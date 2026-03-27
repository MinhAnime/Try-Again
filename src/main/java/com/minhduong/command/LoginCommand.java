package com.minhduong.command;

import com.minhduong.data.PlayerDataManager;
import com.minhduong.util.Messages;
import com.minhduong.util.SessionManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

public class LoginCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("login")
                .then(CommandManager.argument("password", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null) return 0;
                            String username = player.getName().getString();
                            String password = StringArgumentType.getString(ctx, "password");

                            if (PlayerDataManager.isAuthenticated(username)) {
                                player.sendMessage(Messages.alreadyAuth()); return 0;
                            }
                            if (!PlayerDataManager.accountExists(username)) {
                                player.sendMessage(Messages.mustRegister()); return 0;
                            }
                            if (!PlayerDataManager.verifyPassword(username, password)) {
                                player.sendMessage(Messages.wrongPassword()); return 0;
                            }

                            PlayerDataManager.setAuthenticated(username, true);
                            SessionManager.endSession(player);
                            player.changeGameMode(GameMode.SURVIVAL);
                            player.sendMessage(Messages.loginSuccess());
                            return 1;
                        }))
                .executes(ctx -> {
                    ctx.getSource().sendMessage(Messages.info("Dùng: /login <mật khẩu>"));
                    return 0;
                })
        );
    }
}
