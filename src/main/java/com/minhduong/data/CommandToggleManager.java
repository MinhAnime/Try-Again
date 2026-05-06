package com.minhduong.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minhduong.TryAgain;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CommandToggleManager {
    private static final Path FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("tryagain").resolve("disabled_commands.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Set<String> SUPPORTED_COMMANDS = Set.of(
            "sethome", "home", "delhome", "lshome"
    );
    private static final Set<String> disabledCommands = ConcurrentHashMap.newKeySet();
    private static final Map<String, Set<String>> commandAllowedPlayers = new ConcurrentHashMap<>();

    public static void init() {
        load();
        TryAgain.LOGGER.info("Loaded {} disabled command(s).", disabledCommands.size());
    }

    public static boolean isSupported(String commandName) {
        return SUPPORTED_COMMANDS.contains(normalize(commandName));
    }

    public static boolean isDisabled(String commandName) {
        return disabledCommands.contains(normalize(commandName));
    }

    public static boolean isAllowedPlayer(String commandName, String playerName) {
        Set<String> allowedPlayers = commandAllowedPlayers.get(normalize(commandName));
        if (allowedPlayers == null) return false;
        return allowedPlayers.contains(normalizePlayer(playerName));
    }

    public static boolean disableCommand(String commandName) {
        String normalized = normalize(commandName);
        if (!isSupported(normalized)) return false;
        boolean changed = disabledCommands.add(normalized);
        if (changed) save();
        return changed;
    }

    public static boolean enableCommand(String commandName) {
        String normalized = normalize(commandName);
        boolean changed = disabledCommands.remove(normalized);
        if (changed) save();
        return changed;
    }

    public static boolean allowPlayer(String commandName, String playerName) {
        String normalizedCommand = normalize(commandName);
        if (!isSupported(normalizedCommand)) return false;

        String normalizedPlayer = normalizePlayer(playerName);
        if (normalizedPlayer.isEmpty()) return false;

        Set<String> allowedPlayers = commandAllowedPlayers.computeIfAbsent(
                normalizedCommand,
                ignored -> ConcurrentHashMap.newKeySet()
        );

        boolean changed = allowedPlayers.add(normalizedPlayer);
        if (changed) save();
        return changed;
    }

    public static boolean unallowPlayer(String commandName, String playerName) {
        String normalizedCommand = normalize(commandName);
        String normalizedPlayer = normalizePlayer(playerName);

        Set<String> allowedPlayers = commandAllowedPlayers.get(normalizedCommand);
        if (allowedPlayers == null) return false;

        boolean changed = allowedPlayers.remove(normalizedPlayer);
        if (allowedPlayers.isEmpty()) {
            commandAllowedPlayers.remove(normalizedCommand);
        }

        if (changed) save();
        return changed;
    }

    public static List<String> getSupportedCommands() {
        return SUPPORTED_COMMANDS.stream().sorted().toList();
    }

    public static List<String> getDisabledCommands() {
        return disabledCommands.stream().sorted().toList();
    }

    public static List<String> getAllowedPlayers(String commandName) {
        Set<String> allowedPlayers = commandAllowedPlayers.get(normalize(commandName));
        if (allowedPlayers == null || allowedPlayers.isEmpty()) return List.of();
        return allowedPlayers.stream().sorted().toList();
    }

    public static Map<String, List<String>> getAllowedPlayersByCommand() {
        Map<String, List<String>> result = new ConcurrentHashMap<>();
        for (String commandName : getSupportedCommands()) {
            List<String> allowed = getAllowedPlayers(commandName);
            if (!allowed.isEmpty()) {
                result.put(commandName, allowed);
            }
        }
        return result;
    }

    private static String normalize(String commandName) {
        return commandName.toLowerCase(Locale.ROOT);
    }

    private static String normalizePlayer(String playerName) {
        return playerName.toLowerCase(Locale.ROOT).trim();
    }

    private static void load() {
        disabledCommands.clear();
        commandAllowedPlayers.clear();
        if (!Files.exists(FILE)) return;

        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);

            if (rootElement != null && rootElement.isJsonObject()) {
                JsonObject root = rootElement.getAsJsonObject();
                readDisabledArray(root.has("disabled") ? root.get("disabled") : null);

                if (root.has("overrides") && root.get("overrides").isJsonObject()) {
                    JsonObject overrides = root.getAsJsonObject("overrides");
                    for (Map.Entry<String, JsonElement> entry : overrides.entrySet()) {
                        String commandName = normalize(entry.getKey());
                        if (!isSupported(commandName)) continue;
                        if (!entry.getValue().isJsonArray()) continue;

                        Set<String> allowedPlayers = ConcurrentHashMap.newKeySet();
                        for (JsonElement playerElement : entry.getValue().getAsJsonArray()) {
                            if (!playerElement.isJsonPrimitive()) continue;
                            String normalizedPlayer = normalizePlayer(playerElement.getAsString());
                            if (!normalizedPlayer.isEmpty()) {
                                allowedPlayers.add(normalizedPlayer);
                            }
                        }

                        if (!allowedPlayers.isEmpty()) {
                            commandAllowedPlayers.put(commandName, allowedPlayers);
                        }
                    }
                }
            } else if (rootElement != null && rootElement.isJsonArray()) {
                readDisabledArray(rootElement);
            }
        } catch (IOException e) {
            TryAgain.LOGGER.error("Failed to load disabled commands.", e);
        }
    }

    private static void readDisabledArray(JsonElement disabledElement) {
        if (disabledElement == null || !disabledElement.isJsonArray()) return;

        for (JsonElement element : disabledElement.getAsJsonArray()) {
            if (!element.isJsonPrimitive()) continue;
            String normalized = normalize(element.getAsString());
            if (isSupported(normalized)) {
                disabledCommands.add(normalized);
            }
        }
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());

            JsonObject root = new JsonObject();
            JsonArray disabled = new JsonArray();
            getDisabledCommands().forEach(disabled::add);
            root.add("disabled", disabled);

            JsonObject overrides = new JsonObject();
            for (String commandName : getSupportedCommands()) {
                List<String> allowedPlayers = new ArrayList<>(getAllowedPlayers(commandName));
                if (allowedPlayers.isEmpty()) continue;

                JsonArray players = new JsonArray();
                allowedPlayers.forEach(players::add);
                overrides.add(commandName, players);
            }

            if (!overrides.isEmpty()) {
                root.add("overrides", overrides);
            }

            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            TryAgain.LOGGER.error("Failed to save disabled commands.", e);
        }
    }
}
