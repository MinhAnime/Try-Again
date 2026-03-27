package com.minhduong.command;

import com.minhduong.data.PlayerDataManager;
import com.minhduong.util.Messages;
import com.minhduong.util.SessionManager;
import com.minhduong.util.TokenConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

public class RegisterCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("register")
                .then(CommandManager.argument("password", StringArgumentType.word())
                        .then(CommandManager.argument("confirm", StringArgumentType.word())
                                .then(CommandManager.argument("token", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                                            if (player == null) return 0;
                                            return doRegister(player,
                                                    player.getName().getString(),
                                                    StringArgumentType.getString(ctx, "password"),
                                                    StringArgumentType.getString(ctx, "confirm"),
                                                    StringArgumentType.getString(ctx, "token"));
                                        }))))
                .executes(ctx -> {
                    ctx.getSource().sendMessage(Messages.info("Dùng: /register <mật khẩu> <nhắc lại> <token>"));
                    return 0;
                })
        );
    }
    public static int doRegister(ServerPlayerEntity player, String username, String password, String confirm, String token) {
        if (PlayerDataManager.isAuthenticated(username))
        { player.sendMessage(Messages.alreadyAuth());
            return 0;
        }
        if (PlayerDataManager.accountExists(username))
        { player.sendMessage(Messages.nameTaken());
            return 0;
        }
        if (password.length() < 4)
        { player.sendMessage(Messages.passTooShort());
            return 0;
        }
        if (!password.equals(confirm))
        { player.sendMessage(Messages.passNoMatch());
            return 0;
        }
        TokenConfig.load();
        if (!TokenConfig.consumeToken(token))
        { player.sendMessage(Messages.invalidToken()); return 0; }
        PlayerDataManager.register(username, password);
        PlayerDataManager.setAuthenticated(username, true);
        SessionManager.endSession(player);
        player.sendMessage(Messages.registerSuccess());
        player.changeGameMode(GameMode.SURVIVAL);
        return 1;
    }
}
