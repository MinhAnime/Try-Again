package com.minhduong.util;

import com.minhduong.data.EconomyManager;
import com.minhduong.data.MarketManager;
import net.minecraft.item.Item;
import net.minecraft.scoreboard.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HudManager {

    private static final String OBJECTIVE_NAME = "tryagain_hud";
    private static final String DISPLAY_TITLE  = "§6§lTryAgain";
    private static final int    MARKET_ROWS    = 6;


    public static boolean toggle(ServerPlayerEntity player) {
        String uname = player.getName().getString();
        boolean newState = !EconomyManager.isHudEnabled(uname);
        EconomyManager.setHudEnabled(uname, newState);
        return newState;
    }
    public static boolean isEnabled(ServerPlayerEntity player) {
        return EconomyManager.isHudEnabled(player.getName().getString());
    }

    public static void update(ServerPlayerEntity player) {
        if (player.isRemoved()) return;
        if (!isEnabled(player)) return;
        Scoreboard sb = player.getEntityWorld().getScoreboard();

        ScoreboardObjective obj = sb.getNullableObjective(OBJECTIVE_NAME);
        if (obj == null) {
            obj = sb.addObjective(OBJECTIVE_NAME, ScoreboardCriterion.DUMMY,
                    Text.literal(DISPLAY_TITLE), ScoreboardCriterion.RenderType.INTEGER, true, null);
        }

        sb.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, obj);
        clearObjective(sb, obj);

        List<String> lines = buildLines(player);
        int score = lines.size();
        for (String line : lines)
            sb.getOrCreateScore(ScoreHolder.fromName(line), obj).setScore(score--);
    }

    public static void remove(ServerPlayerEntity player) {
        Scoreboard sb = player.getEntityWorld().getScoreboard();
        ScoreboardObjective obj = sb.getNullableObjective(OBJECTIVE_NAME);
        if (obj != null) sb.removeObjective(obj);
    }

    private static List<String> buildLines(ServerPlayerEntity player) {

        String uname = player.getName().getString();
        List<String> lines = new ArrayList<>();

        // ===== HEADER =====
        lines.add("§8§m━━━━━━━━━━━━━━━━");
        lines.add("§6§l  ✦ SERVER MARKET ✦");
        lines.add("§8§m━━━━━━━━━━━━━━━━");

        // ===== PLAYER INFO =====
        lines.add("§7👤 §fNgười chơi: §e" + uname);
        lines.add("§7💰 §fSố dư: §a§l"
                + EconomyManager.getBalance(uname) + "§r§a xu");

        lines.add(String.format(
                "§7📍 §fTP tiếp: §6%dxu §8• §7Lần §e%d",
                EconomyManager.getNextTpCost(uname),
                EconomyManager.getTodayTpCount(uname) + 1
        ));

        lines.add(" ");

        // ===== MARKET HEADER =====
        lines.add("§b§l Thị Trường");
        lines.add("§8────────────────");
        lines.add("§7Vật phẩm   §aBán §eMua §fKho");

        // ===== ITEMS =====
        for (Item item : getSortedItems()) {

            int stock = MarketManager.getStock(item);

            String stockColor =
                    (stock <= 0) ? "§4" :
                            (stock < 32) ? "§c" :
                                    (stock < MarketManager.STOCK_NORMAL) ? "§e" : "§a";

            lines.add(String.format(
                    "§f%-10s §a%3d §e%3d %s%4d§r %s",
                    shortName(item),
                    MarketManager.getSellPrice(item),
                    MarketManager.getBuyPrice(item),
                    stockColor,
                    stock,
                    MarketManager.getPriceTrend(item, false)
            ));
        }

        // ===== FOOTER =====
        lines.add("§8§m━━━━━━━━━━━━━━━━");

        return lines;
    }

    private static List<Item> getSortedItems() {
        List<Item> items = new ArrayList<>(MarketManager.getAllItems());
        items.sort(Comparator.comparingInt(MarketManager::getStock));
        return items.size() > MARKET_ROWS ? items.subList(0, MARKET_ROWS) : items;
    }

    private static String shortName(Item item) {
        String n = item.getName().getString().replace("Cooked ", "~").replace("Raw ", "");
        return n.length() > 11 ? n.substring(0, 10) + "." : n;
    }

    private static void clearObjective(Scoreboard sb, ScoreboardObjective obj) {
        for (ScoreHolder h : new ArrayList<>(sb.getKnownScoreHolders()))
            if (sb.getScore(h, obj) != null) sb.removeScore(h, obj);
    }
}
