package com.minhduong.command;

import com.minhduong.data.PlayerDataManager;
import com.minhduong.data.TpaManager;
import com.minhduong.util.Messages;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TpaCommand {

    private static final SuggestionProvider<ServerCommandSource> ONLINE_PLAYERS =
            (ctx, builder) -> {
                ServerPlayerEntity sender = ctx.getSource().getPlayer();
                List<String> names = ctx.getSource().getServer()
                        .getPlayerManager().getPlayerList().stream()
                        .filter(p -> {
                            if (sender != null && p.equals(sender)) return false;
                            return PlayerDataManager.isAuthenticated(p.getName().getString());
                        })
                        .map(p -> p.getName().getString())
                        .toList();
                return CommandSource.suggestMatching(names, builder);
            };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        dispatcher.register(CommandManager.literal("tpa")
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests(ONLINE_PLAYERS)
                        .executes(ctx -> {
                            ServerPlayerEntity sender = ctx.getSource().getPlayer();
                            if (sender == null) return 0;
                            if (!checkAuth(sender)) return 0;

                            String name = StringArgumentType.getString(ctx, "player");
                            ServerPlayerEntity target = ctx.getSource().getServer().getPlayerManager().getPlayer(name);

                            if (target == null) {
                                sender.sendMessage(Messages.error(
                                        "§8[§6§lTPA§8] §cNgười chơi §f" + name + " §ckhông online."));
                                return 0;
                            }

                            if (target.equals(sender)) {
                                sender.sendMessage(Messages.error(
                                        "§8[§6§lTPA§8] §cBạn không thể teleport đến chính mình."));
                                return 0;
                            }

                            if (!PlayerDataManager.isAuthenticated(target.getName().getString())) {
                                sender.sendMessage(Messages.error(
                                        "§8[§6§lTPA§8] §cNgười chơi này chưa đăng nhập."));
                                return 0;
                            }

                            TpaManager.sendRequest(sender.getUuid(), target.getUuid());

                            sender.sendMessage(Messages.info(String.format(
                                    "§8[§6§lTPA§8] §eĐã gửi yêu cầu teleport tới §b%s§e (hết hạn sau §630s§e).",
                                    target.getName().getString())));

                            target.sendMessage(Messages.info("§8§m-----------------------------"));
                            target.sendMessage(Messages.info(String.format(
                                    "§8[§6§lTPA§8] §b%s §emuốn teleport đến bạn.",
                                    sender.getName().getString())));
                            target.sendMessage(Messages.info("§a✔ §7Dùng §f/tpaok §7để chấp nhận"));
                            target.sendMessage(Messages.info("§c✖ §7Dùng §f/tpdeny §7để từ chối"));
                            target.sendMessage(Messages.info("§8§m-----------------------------"));

                            return 1;
                        }))
                .executes(ctx -> {
                    ctx.getSource().sendMessage(Messages.info(
                            "§8[§6§lTPA§8] §7Sử dụng: §f/tpa <tên người chơi>"));
                    return 0;
                })
        );

        dispatcher.register(CommandManager.literal("tpaok").executes(ctx -> {
            ServerPlayerEntity target = ctx.getSource().getPlayer();
            if (target == null) return 0;
            if (!checkAuth(target)) return 0;
            return handleAccept(target);
        }));

        dispatcher.register(CommandManager.literal("tpdeny").executes(ctx -> {
            ServerPlayerEntity target = ctx.getSource().getPlayer();
            if (target == null) return 0;
            if (!checkAuth(target)) return 0;
            return handleDeny(target);
        }));
    }

    public static int handleAccept(ServerPlayerEntity target) {
        UUID senderUuid = TpaManager.getPendingSenderFor(target.getUuid());
        if (senderUuid == null) {
            target.sendMessage(Messages.error(
                    "§8[§6§lTPA§8] §cKhông có yêu cầu teleport nào đang chờ."));
            return 0;
        }

        ServerPlayerEntity sender = target.getEntityWorld().getServer().getPlayerManager().getPlayer(senderUuid);
        TpaManager.clearRequest(senderUuid);

        if (sender == null) {
            target.sendMessage(Messages.error(
                    "§8[§6§lTPA§8] §cNgười gửi đã rời server."));
            return 0;
        }

        String sname = sender.getName().getString();

        sender.teleport(
                target.getEntityWorld(),
                target.getX(), target.getY(), target.getZ(),
                Set.of(),
                target.getYaw(), target.getPitch(),
                true
        );

        target.sendMessage(Messages.success(
                "§8[§6§lTPA§8] §a§b" + sname + " §ađã teleport đến vị trí của bạn."));

        sender.sendMessage(Messages.success(String.format(
                "§8[§6§lTPA§8] §aĐã teleport đến §b%s§a!",
                target.getName().getString())));

        return 1;
    }

    public static int handleDeny(ServerPlayerEntity target) {
        UUID senderUuid = TpaManager.getPendingSenderFor(target.getUuid());
        if (senderUuid == null) {
            target.sendMessage(Messages.error(
                    "§8[§6§lTPA§8] §cKhông có yêu cầu teleport nào đang chờ."));
            return 0;
        }

        ServerPlayerEntity sender = target.getEntityWorld().getServer().getPlayerManager().getPlayer(senderUuid);
        TpaManager.clearRequest(senderUuid);

        target.sendMessage(Messages.success(
                "§8[§6§lTPA§8] §eBạn đã từ chối yêu cầu teleport."));

        if (sender != null)
            sender.sendMessage(Messages.error(
                    "§8[§6§lTPA§8] §c§b" + target.getName().getString()
                            + " §cđã từ chối yêu cầu teleport của bạn."));

        return 1;
    }

    private static boolean checkAuth(ServerPlayerEntity player) {
        if (!PlayerDataManager.isAuthenticated(player.getName().getString())) {
            player.sendMessage(Messages.mustLogin());
            return false;
        }
        return true;
    }
}