package com.minhduong.data;

import com.google.gson.*;
import com.minhduong.TryAgain;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HomeManager {

    public static final int MAX_HOMES = 5;

    private static final Path FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("tryagain").resolve("homes.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** username.toLowerCase() → (homeName.toLowerCase() → HomeData) */
    private static final Map<String, Map<String, HomeData>> homes = new ConcurrentHashMap<>();

    public static void init() {
        load();
        TryAgain.LOGGER.info("Loaded homes for {} player(s).", homes.size());
    }

    // ─── Query ────────────────────────────────────────────────────────────────

    public static int getHomeCount(String username) {
        return getPlayerHomes(username).size();
    }

    public static boolean hasHome(String username, String homeName) {
        return getPlayerHomes(username).containsKey(homeName.toLowerCase());
    }

    public static Optional<HomeData> getHome(String username, String homeName) {
        return Optional.ofNullable(getPlayerHomes(username).get(homeName.toLowerCase()));
    }

    public static List<HomeData> getHomes(String username) {
        return new ArrayList<>(getPlayerHomes(username).values())
                .stream()
                .sorted(Comparator.comparing(HomeData::getName))
                .toList();
    }

    public static List<String> getHomeNames(String username) {
        return getHomes(username).stream().map(HomeData::getName).toList();
    }

    // ─── Mutate ───────────────────────────────────────────────────────────────

    public static boolean setHome(String username, HomeData home) {
        Map<String, HomeData> m = getPlayerHomes(username);
        boolean isNew = !m.containsKey(home.getName().toLowerCase());
        if (isNew && m.size() >= MAX_HOMES) return false;
        m.put(home.getName().toLowerCase(), home);
        homes.put(username.toLowerCase(), m);
        save();
        return true;
    }

    public static boolean deleteHome(String username, String homeName) {
        Map<String, HomeData> m = getPlayerHomes(username);
        boolean removed = m.remove(homeName.toLowerCase()) != null;
        if (removed) save();
        return removed;
    }

    // ─── IO ───────────────────────────────────────────────────────────────────

    private static Map<String, HomeData> getPlayerHomes(String username) {
        return homes.computeIfAbsent(username.toLowerCase(), k -> new ConcurrentHashMap<>());
    }

    private static void load() {
        if (!Files.exists(FILE)) return;
        try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) return;
            for (Map.Entry<String, JsonElement> userEntry : root.entrySet()) {
                Map<String, HomeData> userHomes = new ConcurrentHashMap<>();
                JsonObject userObj = userEntry.getValue().getAsJsonObject();
                for (Map.Entry<String, JsonElement> homeEntry : userObj.entrySet()) {
                    JsonObject o = homeEntry.getValue().getAsJsonObject();
                    String name = o.has("name") ? o.get("name").getAsString() : homeEntry.getKey();
                    userHomes.put(homeEntry.getKey(), new HomeData(
                            name,
                            o.get("worldId").getAsString(),
                            o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble()
                    ));
                }
                homes.put(userEntry.getKey().toLowerCase(), userHomes);
            }
        } catch (IOException e) { TryAgain.LOGGER.error("Failed to load homes.", e); }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject root = new JsonObject();
            homes.forEach((user, userHomes) -> {
                JsonObject userObj = new JsonObject();
                userHomes.forEach((hkey, h) -> {
                    JsonObject o = new JsonObject();
                    o.addProperty("name",    h.getName());
                    o.addProperty("worldId", h.getWorldId());
                    o.addProperty("x",       h.getX());
                    o.addProperty("y",       h.getY());
                    o.addProperty("z",       h.getZ());
                    userObj.add(hkey, o);
                });
                root.add(user, userObj);
            });
            try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) { GSON.toJson(root, w); }
        } catch (IOException e) { TryAgain.LOGGER.error("Failed to save homes.", e); }
    }
}