package com.minhduong.command;

import com.minhduong.data.EconomyManager;
import com.minhduong.data.PlayerDataManager;
import com.minhduong.util.Messages;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class BalanceCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("balance").executes(ctx -> {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player == null) return 0;
            if (!PlayerDataManager.isAuthenticated(player.getName().getString())) {
                player.sendMessage(Messages.MUST_LOGIN); return 0;
            }
            String un = player.getName().getString();
            player.sendMessage(Messages.info("=== Số dư ==="));
            player.sendMessage(Messages.success(EconomyManager.getBalance(un) + " xu"));
            player.sendMessage(Messages.info(String.format(
                    "Teleport hôm nay: %d lần | Chi phí tiếp theo: %dxu",
                    EconomyManager.getTodayTpCount(un), EconomyManager.getNextTpCost(un))));
            return 1;

        }));
    }
}
