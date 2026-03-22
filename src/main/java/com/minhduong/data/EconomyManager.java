package com.minhduong.data;

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
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyManager {

    private static final Path FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("tryagain").resolve("economy.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final int BASE_TP_COST = 10;

    private static final Map<String, Long> balance = new ConcurrentHashMap<>();
    private static final Map<String, Integer> tpCount = new ConcurrentHashMap<>();
    private static final Map<String, String>  tpDate  = new ConcurrentHashMap<>();
    private static final Set<String> hudDisabled = ConcurrentHashMap.newKeySet();

    public static void init() { load(); TryAgain.LOGGER.info("Economy loaded."); }

    public static long getBalance(String u)   { return balance.getOrDefault(u.toLowerCase(), 0L); }

    public static void addBalance(String u, long amount) {
        balance.merge(u.toLowerCase(), amount, Long::sum); save();
    }

    public static boolean deductBalance(String u, long amount) {
        String key = u.toLowerCase();
        long cur = balance.getOrDefault(key, 0L);
        if (cur < amount) return false;
        balance.put(key, cur - amount);
        save();
        return true;
    }

    public static long getNextTpCost(String u) {
        String key = u.toLowerCase(); refreshDaily(key);
        return (long) BASE_TP_COST * (1L << tpCount.getOrDefault(key, 0));
    }

    public static int getTodayTpCount(String u) {
        String key = u.toLowerCase(); refreshDaily(key);
        return tpCount.getOrDefault(key, 0);
    }

    public static void recordTeleport(String u) {
        String key = u.toLowerCase(); refreshDaily(key);
        tpCount.merge(key, 1, Integer::sum); save();
    }
    public static boolean isHudEnabled(String u)  { return !hudDisabled.contains(u.toLowerCase()); }

    public static void setHudEnabled(String u, boolean enabled) {
        if (enabled) hudDisabled.remove(u.toLowerCase());
        else hudDisabled.add(u.toLowerCase());
        save();
    }

    private static void refreshDaily(String key) {
        String today = LocalDate.now().toString();
        if (!today.equals(tpDate.get(key))) { tpCount.put(key, 0); tpDate.put(key, today); }
    }

    private static void load() {
        if (!Files.exists(FILE)) return;
        try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) return;
            if (root.has("balance"))  root.getAsJsonObject("balance").entrySet().forEach(e -> balance.put(e.getKey(), e.getValue().getAsLong()));
            if (root.has("tpCount"))  root.getAsJsonObject("tpCount").entrySet().forEach(e -> tpCount.put(e.getKey(), e.getValue().getAsInt()));
            if (root.has("tpDate"))   root.getAsJsonObject("tpDate").entrySet().forEach(e -> tpDate.put(e.getKey(), e.getValue().getAsString()));
            if (root.has("hudDisabled")) root.getAsJsonArray("hudDisabled").forEach(e -> hudDisabled.add(e.getAsString()));
        } catch (IOException e) { TryAgain.LOGGER.error("Failed to load economy.", e); }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject root = new JsonObject();
            JsonObject b = new JsonObject(); balance.forEach(b::addProperty); root.add("balance", b);
            JsonObject tc = new JsonObject(); tpCount.forEach(tc::addProperty); root.add("tpCount", tc);
            JsonObject td = new JsonObject(); tpDate.forEach(td::addProperty); root.add("tpDate", td);
            JsonArray hd = new JsonArray();  hudDisabled.forEach(hd::add);     root.add("hudDisabled", hd);
            try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) { GSON.toJson(root, w); }
        } catch (IOException e) { TryAgain.LOGGER.error("" +
                "Failed to save economy.", e); }
    }
}
