package com.minhduong.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.minhduong.TryAgain;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MarketManager {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tryagain").resolve("market.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final int STOCK_NORMAL = 256;
    public static final int STOCK_MAX = 2048;

    private static final Map<Item, Long> BASE = new LinkedHashMap<>();
    private static final Map<String, Integer> stock = new ConcurrentHashMap<>();

    static {
        BASE.put(Items.WHEAT,3L); BASE.put(Items.WHEAT_SEEDS,1L); BASE.put(Items.CARROT,3L);
        BASE.put(Items.POTATO,3L); BASE.put(Items.BAKED_POTATO,5L); BASE.put(Items.BEETROOT,3L);
        BASE.put(Items.BEETROOT_SEEDS,1L); BASE.put(Items.MELON_SLICE,3L); BASE.put(Items.MELON,20L);
        BASE.put(Items.PUMPKIN,15L); BASE.put(Items.SUGAR_CANE,2L); BASE.put(Items.COCOA_BEANS,4L);
        BASE.put(Items.SWEET_BERRIES,3L); BASE.put(Items.GLOW_BERRIES,4L); BASE.put(Items.APPLE,5L);
        BASE.put(Items.BREAD,8L); BASE.put(Items.CAKE,30L); BASE.put(Items.COOKIE,2L);
        BASE.put(Items.PUMPKIN_PIE,10L); BASE.put(Items.MUSHROOM_STEW,12L);
        BASE.put(Items.BROWN_MUSHROOM,3L); BASE.put(Items.RED_MUSHROOM,3L);
        BASE.put(Items.NETHER_WART,5L); BASE.put(Items.KELP,2L);
        BASE.put(Items.DRIED_KELP,1L); BASE.put(Items.BAMBOO,1L);
        BASE.put(Items.PORKCHOP,5L); BASE.put(Items.BEEF,6L); BASE.put(Items.CHICKEN,4L);
        BASE.put(Items.MUTTON,5L); BASE.put(Items.RABBIT,5L); BASE.put(Items.COD,4L);
        BASE.put(Items.SALMON,5L); BASE.put(Items.TROPICAL_FISH,3L);
        BASE.put(Items.COOKED_PORKCHOP,10L); BASE.put(Items.COOKED_BEEF,12L);
        BASE.put(Items.COOKED_CHICKEN,8L); BASE.put(Items.COOKED_MUTTON,10L);
        BASE.put(Items.COOKED_RABBIT,10L); BASE.put(Items.COOKED_COD,8L);
        BASE.put(Items.COOKED_SALMON,10L); BASE.put(Items.RABBIT_STEW,15L);
    }

    public static void init() {
        load();
        TryAgain.LOGGER.info("Loading Market Manager");
    }

    public static boolean isTradeable(Item i) {
        return BASE.containsKey(i);
    }
    public static Set<Item> getAllItems() {
        return Collections.unmodifiableSet(BASE.keySet());
    }
    public static int getStock(Item i) {
        return stock.getOrDefault(itemKey(i), 0);
    }
    public static long getSellPrice(Item i) {
        if (!BASE.containsKey(i)) return -1;
        double f = (double) STOCK_NORMAL / (getStock(i) + STOCK_NORMAL *0.5);
        f = Math.max(0.3, Math.min(2.0, f));
        return Math.max(1L, Math.round(BASE.get(i) * f));
    }

    public static long getBuyPrice(Item i) {
        if (!BASE.containsKey(i)) return -1;
        double f = (double) STOCK_NORMAL / (getStock(i) + STOCK_NORMAL * 0.3);
        f = Math.max(0.5, Math.min(2.5, f)); f *= 1.4;
        return Math.max(2L, Math.round(BASE.get(i) * f));
    }
    public static void addStock(Item i, int amount) {
        String k = itemKey(i); stock.put(k, Math.min(STOCK_MAX, stock.getOrDefault(k,0)+amount)); save();
    }

    public static boolean removeStock(Item i, int amount) {
        String k = itemKey(i); int cur = stock.getOrDefault(k,0);
        if (cur < amount) return false; stock.put(k, cur-amount); save(); return true;
    }

    public static String getPriceTrend(Item i, boolean buying) {
        long cur = buying ? getBuyPrice(i) : getSellPrice(i);
        long base = BASE.getOrDefault(i, 1L);
        long adj  = buying ? Math.round(base*1.4) : base;
        if (cur > adj*1.3) return "§c▲";
        if (cur < adj*0.7) return "§a▼";
        return "§e~";
    }

    private static void load() {
        if (!Files.exists(FILE)) return;
        try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root != null && root.has("stock"))
                root.getAsJsonObject("stock").entrySet().forEach(e -> stock.put(e.getKey(), e.getValue().getAsInt()));
        } catch (IOException e) { TryAgain.LOGGER.error("Failed to load market.", e); }
    }
    public static synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject root = new JsonObject(); JsonObject s = new JsonObject();
            stock.forEach(s::addProperty); root.add("stock", s);
            try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) { GSON.toJson(root, w); }
        } catch (IOException e) { TryAgain.LOGGER.error("Failed to save market.", e); }
    }

    public static String itemKey(Item i) {
        return Registries.ITEM.getId(i).toString();
    }
}
