package com.minhduong.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.minhduong.TryAgain;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private static Path DATA_FILE = FabricLoader.getInstance().getConfigDir().resolve("tryagain").resolve("players.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();


    private static final Map<String, String> accounts = new ConcurrentHashMap<>();
    private static final Set<String> authenticated = ConcurrentHashMap.newKeySet();

    public static void init() {
        try {
            Files.createDirectories(DATA_FILE.getParent());
            load();
            TryAgain.LOGGER.info("[Try Again] Loaded {} accounts", accounts.size());
        } catch (IOException e) {
            TryAgain.LOGGER.error("[Try Again] Failed to init data.", e);
        }
    }
    public static boolean accountExists(String name) {
        return accounts.containsKey(name.toLowerCase());
    }
    public static boolean register(String username, String password) {
        String key = username.toLowerCase();
        if (accounts.containsKey(key)) return false;
        accounts.put(key, password);
        save();
        return true;
    }
    public static boolean verifyPassword(String username, String password) {
        String stored = accounts.get(username.toLowerCase());
        return stored != null && stored.equals(password);
    }
    public static void setAuthenticated(String username, boolean value) {
        if (value) authenticated.add(username.toLowerCase());
        else authenticated.remove(username.toLowerCase());
    }
    public static boolean isAuthenticated(String username) {
        return authenticated.contains(username.toLowerCase());
    }
    public static void clearSession(String username) {
        authenticated.remove(username.toLowerCase());
    }

    public static synchronized void save() {
        try {
            JsonObject root = new JsonObject();
            accounts.forEach(root::addProperty);
            try (Writer w = Files.newBufferedWriter(DATA_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (IOException e) {
            TryAgain.LOGGER.error("[TryAgain] Failed to save accounts.", e);
        }
    }
    private static void load() {
        if (!Files.exists(DATA_FILE)) return;
        try (Reader r = Files.newBufferedReader(DATA_FILE, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) return;
            for (Map.Entry<String, JsonElement> e : root.entrySet())
                accounts.put(e.getKey(), e.getValue().getAsString());
        } catch (IOException e) {
            TryAgain.LOGGER.error("[TryAgain] Failed to load accounts.", e);
        }
    }
}