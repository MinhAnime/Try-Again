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
import java.util.List;
import java.util.Locale;
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

    public static List<String> getSupportedCommands() {
        return SUPPORTED_COMMANDS.stream().sorted().toList();
    }

    public static List<String> getDisabledCommands() {
        return disabledCommands.stream().sorted().toList();
    }

    private static String normalize(String commandName) {
        return commandName.toLowerCase(Locale.ROOT);
    }

    private static void load() {
        disabledCommands.clear();
        if (!Files.exists(FILE)) return;

        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            JsonArray disabledArray = null;

            if (rootElement != null && rootElement.isJsonObject()) {
                JsonObject root = rootElement.getAsJsonObject();
                if (root.has("disabled") && root.get("disabled").isJsonArray()) {
                    disabledArray = root.getAsJsonArray("disabled");
                }
            } else if (rootElement != null && rootElement.isJsonArray()) {
                disabledArray = rootElement.getAsJsonArray();
            }

            if (disabledArray == null) return;

            for (JsonElement element : disabledArray) {
                if (!element.isJsonPrimitive()) continue;
                String normalized = normalize(element.getAsString());
                if (isSupported(normalized)) {
                    disabledCommands.add(normalized);
                }
            }
        } catch (IOException e) {
            TryAgain.LOGGER.error("Failed to load disabled commands.", e);
        }
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());

            JsonObject root = new JsonObject();
            JsonArray disabled = new JsonArray();
            getDisabledCommands().forEach(disabled::add);
            root.add("disabled", disabled);

            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            TryAgain.LOGGER.error("Failed to save disabled commands.", e);
        }
    }
}
