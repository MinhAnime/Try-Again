package com.minhduong.command;

import com.minhduong.data.EconomyManager;
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
                                sender.sendMessage(Messages.error("'" + name + "' Không online.")); return 0;
                            }
                            if (target.equals(sender)) {
                                sender.sendMessage(Messages.error("Không thể teleport đến chính mình.")); return 0;
                            }
                            if (!PlayerDataManager.isAuthenticated(target.getName().getString())) {
                                sender.sendMessage(Messages.error("Người chơi chưa đăng nhập.")); return 0;
                            }

                            long cost = EconomyManager.getNextTpCost(sender.getName().getString());
                            if (EconomyManager.getBalance(sender.getName().getString()) < cost) {
                                sender.sendMessage(Messages.error(String.format(
                                        "Thiếu tiền mà đòi tele à? Nạp vip vào! Cần %dxu, bạn có %dxu.",
                                        cost, EconomyManager.getBalance(sender.getName().getString()))));
                                return 0;
                            }

                            TpaManager.sendRequest(sender.getUuid(), target.getUuid());
                            sender.sendMessage(Messages.info(String.format(
                                    "Đã gửi yêu cầu đến %s. Chi phí: %dxu. Hết hạn sau 30 giây.",
                                    target.getName().getString(), cost)));
                            target.sendMessage(Messages.info(String.format(
                                    "%s muốn teleport đến bạn! Dùng /tpaok hoặc /tpdeny",
                                    sender.getName().getString())));
                            return 1;
                        }))
                .executes(ctx -> { ctx.getSource().sendMessage(Messages.info("Dùng: /tpa <tên người chơi>")); return 0; })
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
            target.sendMessage(Messages.error("Không có yêu cầu teleport nào đang chờ.")); return 0;
        }

        ServerPlayerEntity sender = target.getEntityWorld().getServer().getPlayerManager().getPlayer(senderUuid);
        TpaManager.clearRequest(senderUuid);

        if (sender == null) {
            target.sendMessage(Messages.error("Người gửi đã thoát server.")); return 0;
        }

        String sname = sender.getName().getString();
        long cost = EconomyManager.getNextTpCost(sname);

        if (!EconomyManager.deductBalance(sname, cost)) {
            sender.sendMessage(Messages.error(String.format("Không đủ xu! Cần %dxu.", cost)));
            target.sendMessage(Messages.error("Người kia không đủ xu."));
            return 0;
        }

        EconomyManager.recordTeleport(sname);

        sender.teleport(target.getEntityWorld(),

                target.getX(), target.getY(), target.getZ(), Set.of(), target.getYaw(), target.getPitch(), true);

        target.sendMessage(Messages.success(sname + " đã được teleport đến bạn."));
        sender.sendMessage(Messages.success(String.format(
                "Đã teleport đến %s! - %dxu. Còn lại: %dxu.",
                target.getName().getString(), cost, EconomyManager.getBalance(sname))));
        sender.sendMessage(Messages.info(String.format(
                "Lần %d hôm nay. Tiếp theo: %dxu.",
                EconomyManager.getTodayTpCount(sname), EconomyManager.getNextTpCost(sname))));
        return 1;
    }

    public static int handleDeny(ServerPlayerEntity target) {
        UUID senderUuid = TpaManager.getPendingSenderFor(target.getUuid());
        if (senderUuid == null) {
            target.sendMessage(Messages.error("Không có yêu cầu teleport nào đang chờ.")); return 0;
        }
        ServerPlayerEntity sender = target.getEntityWorld().getServer().getPlayerManager().getPlayer(senderUuid);
        TpaManager.clearRequest(senderUuid);
        target.sendMessage(Messages.success("Đã từ chối yêu cầu teleport."));
        if (sender != null)
            sender.sendMessage(Messages.error(target.getName().getString() + " đã từ chối yêu cầu của bạn."));
        return 1;
    }

    private static boolean checkAuth(ServerPlayerEntity player) {
        if (!PlayerDataManager.isAuthenticated(player.getName().getString())) {
            player.sendMessage(Messages.MUST_LOGIN); return false;
        }
        return true;
    }
}
