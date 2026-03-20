package com.minhduong.command;

import com.minhduong.data.PlayerDataManager;
import com.minhduong.util.HudManager;
import com.minhduong.util.Messages;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class HudCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("hud").executes(ctx -> {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player == null) return 0;
            if (!PlayerDataManager.isAuthenticated(player.getName().getString())) {
                player.sendMessage(Messages.MUST_LOGIN); return 0;
            }

            boolean nowEnabled = HudManager.toggle(player);
            if (nowEnabled) {
                player.sendMessage(Messages.success("HUD đã bật."));
                HudManager.update(player);
            } else {
                player.sendMessage(Messages.info("HUD đã tắt."));
                HudManager.remove(player);
            }
            return 1;
        }));
    }
}
