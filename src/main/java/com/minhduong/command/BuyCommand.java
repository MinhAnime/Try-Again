package com.minhduong.command;

import com.minhduong.data.EconomyManager;
import com.minhduong.data.MarketManager;
import com.minhduong.data.PlayerDataManager;
import com.minhduong.util.Messages;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

public class BuyCommand {

    private static final SuggestionProvider<ServerCommandSource> TRADEABLE_ITEMS =
            (ctx, builder) -> {
                List<String> names = MarketManager.getAllItems().stream()
                        .filter(item -> MarketManager.getStock(item) > 0)
                        .map(item -> Registries.ITEM.getId(item).getPath())
                        .toList();
                return CommandSource.suggestMatching(names, builder);
            };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("buy")

                .then(CommandManager.literal("list").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    if (!checkAuth(player)) return 0;
                    return showList(player);
                }))

                .then(CommandManager.argument("item", StringArgumentType.word())
                        .suggests(TRADEABLE_ITEMS)
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null) return 0;
                            if (!checkAuth(player)) return 0;
                            return buy(player, StringArgumentType.getString(ctx, "item"), 1);
                        })
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                                    if (player == null) return 0;
                                    if (!checkAuth(player)) return 0;
                                    return buy(player,
                                            StringArgumentType.getString(ctx, "item"),
                                            IntegerArgumentType.getInteger(ctx, "amount"));
                                })))

                .executes(ctx -> { ctx.getSource().sendMessage(Messages.info("Dùng: /buy <item> [số lượng] | /buy list")); return 0; })
        );
    }

    private static int buy(ServerPlayerEntity player, String itemName, int amount) {
        Item item = resolve(itemName);
        if (item == null) {
            player.sendMessage(Messages.error("Không tìm thấy: '" + itemName + "'. Dùng /buy list.")); return 0;
        }

        int avail = MarketManager.getStock(item);
        if (avail <= 0) {
            player.sendMessage(Messages.error("Nhà cái hết hàng '" + item.getName().getString() + "'.")); return 0;
        }
        if (avail < amount) {
            player.sendMessage(Messages.error(String.format("Nhà cái chỉ còn %d cai.", avail))); return 0;
        }

        long cost = MarketManager.getBuyPrice(item) * amount;
        String un  = player.getName().getString();

        if (EconomyManager.getBalance(un) < cost) {
            player.sendMessage(Messages.error(String.format(
                    "Bịp nhà cái à! Cần %dxu, bạn có %dxu.", cost, EconomyManager.getBalance(un))));
            return 0;
        }

        EconomyManager.deductBalance(un, cost);
        MarketManager.removeStock(item, amount);

        ItemStack toInsert = new ItemStack(item, amount);
        boolean inserted = player.getInventory().insertStack(toInsert);

        if (!inserted) {
            EconomyManager.addBalance(un, cost);
            MarketManager.addStock(item, amount);
            player.sendMessage(Messages.error("Túi đồ đầy rồi! Dọn chỗ cho túi đồ đi."));
            return 0;
        }

        player.sendMessage(Messages.success(String.format(
                "Đã mua %d x %s | -%dxu | Còn lại: %dxu",
                amount, item.getName().getString(), cost, EconomyManager.getBalance(un))));
        player.sendMessage(Messages.info(String.format(
                "Kho còn: %d | %s", MarketManager.getStock(item), MarketManager.getPriceTrend(item, true))));
        return 1;
    }

    private static int showList(ServerPlayerEntity player) {
        player.sendMessage(Messages.info("=== Thị trường tự do (Bán / Mua / Kho) ==="));
        String lastCat = "";
        for (Item item : MarketManager.getAllItems()) {
            String cat = category(item);
            if (!cat.equals(lastCat)) { player.sendMessage(Messages.info("§8--- " + cat + " ---")); lastCat = cat; }
            int s = MarketManager.getStock(item);
            String sc = s==0 ? "§cHết" : s<32 ? "§c"+s : s<MarketManager.STOCK_NORMAL ? "§e"+s : "§a"+s;
            player.sendMessage(net.minecraft.text.Text.literal(String.format(
                    "§f%-20s §aBán:%-4d §eMua:%-4d Kho:%s§r  %s",
                    item.getName().getString(), MarketManager.getSellPrice(item),
                    MarketManager.getBuyPrice(item), sc, MarketManager.getPriceTrend(item, false))));
        }
        player.sendMessage(Messages.info("/buy <item> [số lượng] | /sell"));
        return 1;
    }

    private static Item resolve(String name) {
        try {
            Item item = Registries.ITEM.get(Identifier.of(name.contains(":") ? name : "minecraft:"+name));
            return MarketManager.isTradeable(item) ? item : null;
        } catch (Exception e) { return null; }
    }

    private static String category(Item item) {
        String k = Registries.ITEM.getId(item).getPath();
        if (k.contains("cooked")||k.contains("stew"))
            return "Thịt chín";
        if (k.contains("porkchop")||k.contains("beef")||k.contains("chicken")||k.contains("mutton")
                ||k.contains("rabbit")||k.contains("cod")||k.contains("salmon")||k.contains("fish"))
            return "Thịt sống";
        return "Nông sản";
    }

    private static boolean checkAuth(ServerPlayerEntity player) {
        if (!PlayerDataManager.isAuthenticated(player.getName().getString())) {
            player.sendMessage(Messages.MUST_LOGIN); return false;
        }
        return true;
    }
}