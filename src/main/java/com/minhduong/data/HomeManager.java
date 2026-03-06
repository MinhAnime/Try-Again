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
    private static final Path HOMES_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("tryagain").resolve("homes.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, HomeData> homes = new ConcurrentHashMap<>();
    public static void init() {
        load();
        TryAgain.LOGGER.info("[TryAgain] Loaded {} home(s).", homes.size());
    }
    public static boolean hasHome(String username) {
        return homes.containsKey(username.toLowerCase());
    }
    public static Optional<HomeData> getHome(String username) {
        return Optional.ofNullable(homes.get(username.toLowerCase()));
    }
    public static void setHome(String username, HomeData home) {
        homes.put(username.toLowerCase(), home);
        save();
    }
    public static boolean deleteHome(String username) {
        boolean had = homes.remove(username.toLowerCase()) != null;
        if (had) save();
        return had;
    }
    private static void load() {
        if (!Files.exists(HOMES_FILE)) return;
        try (Reader r = Files.newBufferedReader(HOMES_FILE, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) return;
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                homes.put(e.getKey().toLowerCase(), new HomeData(
                        o.get("worldId").getAsString(),
                        o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble()
                ));
            }
        } catch (IOException e) {
            TryAgain.LOGGER.error("[AuthMod] Failed to load homes.", e);
        }
    }
    public static synchronized void save() {
        try {
            Files.createDirectories(HOMES_FILE.getParent());
            JsonObject root = new JsonObject();
            homes.forEach((k, h) -> {
                JsonObject o = new JsonObject();
                o.addProperty("worldId", h.getWorldId());
                o.addProperty("x", h.getX());
                o.addProperty("y", h.getY());
                o.addProperty("z", h.getZ());
                root.add(k, o);
            });
            try (Writer w = Files.newBufferedWriter(HOMES_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (IOException e) {
            TryAgain.LOGGER.error("[AuthMod] Failed to save homes.", e);
        }
    }
}
