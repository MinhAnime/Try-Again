package com.minhduong.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.minhduong.TryAgain;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TokenConfig {
    private static final Path CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("tryagain").resolve("token.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final List<String> tokens = new CopyOnWriteArrayList<>();

    public static void load() {
        try {
            if (!Files.exists(CONFIG_FILE)) {
                saveDefault();
                TryAgain.LOGGER.warn("Created default token.json file. Please change your token in token.json file");
                return;
            }
            try (Reader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
                JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                tokens.clear();
                if (obj != null && obj.has("tokens")) {
                    obj.getAsJsonArray("tokens").forEach(e -> tokens.add(e.getAsString()));
                }
            }
        }
        catch (IOException e) {
            TryAgain.LOGGER.error("Failed to load token file!", e);
        }
    }
    public static synchronized boolean consumeToken(String token) {
        if (!tokens.contains(token)) return false;
        tokens.remove(token);
        saveCurrentToken();
        TryAgain.LOGGER.info("Token '{}' is used and removed.", token);
        return true;
    }
    public static int remainingCount() {
        return tokens.size();
    }
    public static void saveDefault() throws IOException {
        Files.createDirectories(CONFIG_FILE.getParent());
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();
        arr.add("a"); arr.add("b"); arr.add("c"); arr.add("d"); arr.add("e");
        obj.add("tokens", arr);
        try (Writer w = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
            GSON.toJson(obj, w);
        }
    }
    public static synchronized void saveCurrentToken() {
        try {
            JsonObject obj = new JsonObject();
            JsonArray arr = new JsonArray();
            tokens.forEach(arr::add);
            obj.add("tokens", arr);
            try (Writer w = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(obj, w);
            }
        }
        catch (IOException e) {
            TryAgain.LOGGER.error("Failed to save token!", e);
        }
    }

}
