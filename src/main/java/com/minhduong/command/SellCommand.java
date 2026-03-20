package com.minhduong.command;

import com.minhduong.data.EconomyManager;
import com.minhduong.data.MarketManager;
import com.minhduong.data.PlayerDataManager;
import com.minhduong.util.Messages;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class SellCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sell")
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null) return 0;
                            if (!PlayerDataManager.isAuthenticated(player.getName().getString())) {
                                player.sendMessage(Messages.MUST_LOGIN); return 0;
                            }
                            return sellHand(player, IntegerArgumentType.getInteger(ctx, "amount"));
                        }))
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    if (!PlayerDataManager.isAuthenticated(player.getName().getString())) {
                        player.sendMessage(Messages.MUST_LOGIN); return 0;
                    }
                    return sellHand(player, -1);
                })
        );
    }

    private static int sellHand(ServerPlayerEntity player, int amount) {
        ItemStack held = player.getMainHandStack();
        if (held.isEmpty()) { player.sendMessage(Messages.error("Bạn đang cầm tay không.")); return 0; }

        Item item = held.getItem();
        if (!MarketManager.isTradeable(item)) {
            player.sendMessage(Messages.error(
                    "'" + held.getName().getString() + "' không có trong danh sách. Dùng /buy list."));
            return 0;
        }

        int stackSize = held.getCount();

        int sellCount;
        if (amount == -1) {
            sellCount = stackSize;
        } else if (amount > stackSize) {
            player.sendMessage(Messages.error(String.format(
                    "Bạn chỉ có %d cái nhưng muốn bán %d cái, lừa cả nhà cái à?", stackSize, amount)));
            return 0;
        } else {
            sellCount = amount;
        }

        long priceEach = MarketManager.getSellPrice(item);
        long total     = priceEach * sellCount;
        String uname   = player.getName().getString();

        held.setCount(stackSize - sellCount);
        MarketManager.addStock(item, sellCount);
        EconomyManager.addBalance(uname, total);

        long   nextPrice = MarketManager.getSellPrice(item);
        int    newStock  = MarketManager.getStock(item);
        String trend     = MarketManager.getPriceTrend(item, false);

        player.sendMessage(Messages.success(String.format(
                "Đã bán %d x %s | +%dxu/cái | Nhận: +%dxu | Số dư: %dxu",
                sellCount, held.getName().getString(), priceEach, total,
                EconomyManager.getBalance(uname))));

        if (sellCount < stackSize) {
            player.sendMessage(Messages.info(String.format(
                    "Còn lại trong tay: %d cái.", stackSize - sellCount)));
        }

        if (nextPrice < priceEach) {
            player.sendMessage(Messages.info(String.format(
                    "Kho tăng lên %d. Giá bán tiếp theo: %dxu %s", newStock, nextPrice, trend)));
        } else {
            player.sendMessage(Messages.info(String.format("Kho: %d | Giá: %s", newStock, trend)));
        }
        return 1;
    }
}