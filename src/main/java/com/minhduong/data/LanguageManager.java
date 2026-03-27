package com.minhduong.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.minhduong.TryAgain;
import com.minhduong.util.TextFormat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.MutableText;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LanguageManager {
    private static final Path LANG_DIR = FabricLoader.getInstance().getConfigDir().resolve("tryagain").resolve("lang");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Ngôn ngữ mặc định
    private static String currentLang = "vi_vn";
    private static final Map<String, String> translations = new ConcurrentHashMap<>();

    public static void init() {
        try {
            Files.createDirectories(LANG_DIR);

            createDefaultLangFile("vi_vn");
            createDefaultLangFile("en_us");

            load();
        } catch (Exception e) {
            TryAgain.LOGGER.error("Failed to init LanguageManager.", e);
        }
    }

    public static void load() {
        translations.clear();
        Path langFile = LANG_DIR.resolve(currentLang + ".json");

        if (!Files.exists(langFile)) {
            TryAgain.LOGGER.warn("Language file {} not found, falling back to vi_vn", langFile.getFileName());
            langFile = LANG_DIR.resolve("vi_vn.json");
            if (!Files.exists(langFile)) {
                return;
            }
        }

        try (Reader r = Files.newBufferedReader(langFile, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root != null) {
                for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                    translations.put(e.getKey(), e.getValue().getAsString());
                }
            }
            TryAgain.LOGGER.info("Loaded {} translations from {}", translations.size(), langFile.getFileName());
        } catch (IOException e) {
            TryAgain.LOGGER.error("Failed to load language file " + langFile.getFileName(), e);
        }
    }

    public static MutableText getText(String key, Object... args) {
        String template = translations.getOrDefault(key, key);
        for (int i = 0; i < args.length; i++) {
            template = template.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return TextFormat.parse(template);
    }

    private static void createDefaultLangFile(String lang) throws IOException {
        Path file = LANG_DIR.resolve(lang + ".json");
        if (!Files.exists(file)) {
            try (InputStream in = LanguageManager.class
                    .getResourceAsStream("/assets/tryagain/lang/" + lang + ".json")) {
                if (in != null) {
                    Files.copy(in, file);
                }
            }
        }
    }
}
