package com.minhduong.command;

import com.minhduong.data.HomeData;
import com.minhduong.data.HomeManager;
import com.minhduong.data.PlayerDataManager;
import com.minhduong.util.HomeTextBuilder;
import com.minhduong.util.Messages;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Set;

public class HomeCommand {

    private static final SuggestionProvider<ServerCommandSource> OWN_HOMES =
            (ctx, builder) -> {
                ServerPlayerEntity player = ctx.getSource().getPlayer();
                if (player == null) return builder.buildFuture();
                List<String> names =
                        HomeManager.getHomeNames(player.getName().getString());
                return CommandSource.suggestMatching(names, builder);
            };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        // /sethome
        dispatcher.register(CommandManager.literal("sethome")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null || !checkAuth(player)) return 0;

                            return doSetHome(
                                    player,
                                    StringArgumentType.getString(ctx, "name"));
                        })));

        // /home
        dispatcher.register(CommandManager.literal("home")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(OWN_HOMES)
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null || !checkAuth(player)) return 0;

                            return doGoHome(
                                    player,
                                    StringArgumentType.getString(ctx, "name"));
                        })));

        // /delhome
        dispatcher.register(CommandManager.literal("delhome")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(OWN_HOMES)
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null || !checkAuth(player)) return 0;

                            return doDelHome(
                                    player,
                                    StringArgumentType.getString(ctx, "name"));
                        })));

        // /lshome
        dispatcher.register(CommandManager.literal("lshome")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null || !checkAuth(player)) return 0;
                    return listHomes(player);
                }));
    }

    // ───────────────── ACTIONS ─────────────────

    private static int doSetHome(ServerPlayerEntity player, String name) {

        String username = player.getName().getString();

        if (HomeManager.hasHome(username, name)) {
            player.sendMessage(Messages.error("Home '" + name + "' đã tồn tại!"));
            return 0;
        }

        String world = player.getEntityWorld().getRegistryKey().getValue().toString();

        HomeData home = new HomeData(
                name,
                world,
                player.getX(),
                player.getY(),
                player.getZ()
        );

        boolean ok = HomeManager.setHome(username, home);

        if (!ok) {
            player.sendMessage(Messages.error(
                    "Đã đạt giới hạn home (" + HomeManager.MAX_HOMES + ")"));
            return 0;
        }

        player.sendMessage(Text.literal(String.format(
                "§a✔ §fĐã lưu home §e'%s' §8(%.0f %.0f %.0f)",
                name,
                player.getX(),
                player.getY(),
                player.getZ()
        )));

        return 1;
    }

    private static int doGoHome(ServerPlayerEntity player, String name) {

        HomeData home = HomeManager
                .getHome(player.getName().getString(), name)
                .orElse(null);

        if (home == null) {
            player.sendMessage(Messages.error(
                    "Không tìm thấy home '" + name + "'"));
            return 0;
        }

        ServerWorld target = null;

        for (ServerWorld w : player.getEntityWorld().getServer().getWorlds()) {
            if (w.getRegistryKey().getValue()
                    .toString().equals(home.getWorldId())) {
                target = w;
                break;
            }
        }

        if (target == null) {
            player.sendMessage(Messages.error("World không tồn tại."));
            return 0;
        }

        player.teleport(
                target,
                home.getX(),
                home.getY(),
                home.getZ(),
                Set.of(),
                player.getYaw(),
                player.getPitch(),
                true
        );

        player.sendMessage(Text.literal(
                "§a⬤ §fĐã dịch chuyển tới §e" + home.getName()));

        return 1;
    }

    private static int doDelHome(ServerPlayerEntity player, String name) {

        boolean removed =
                HomeManager.deleteHome(player.getName().getString(), name);

        if (!removed) {
            player.sendMessage(Messages.error(
                    "Không tìm thấy home '" + name + "'"));
            return 0;
        }

        player.sendMessage(Text.literal(
                "§c✖ §fĐã xoá home §e'" + name + "'"));

        return 1;
    }

    private static int listHomes(ServerPlayerEntity player) {

        List<HomeData> list = new java.util.ArrayList<>(
                HomeManager.getHomes(player.getName().getString()));

        if (list.isEmpty()) {
            player.sendMessage(Messages.info(
                    "§7Bạn chưa có home nào. Dùng §6/sethome <tên>"));
            return 0;
        }

        // sort alphabet
        list.sort((a, b) ->
                a.getName().compareToIgnoreCase(b.getName()));

        player.sendMessage(HomeTextBuilder.divider());
        player.sendMessage(HomeTextBuilder.header("DANH SÁCH HOME"));

        player.sendMessage(Text.literal(String.format(
                " §7Bạn có §e%d§7/§e%d §7home",
                list.size(),
                HomeManager.MAX_HOMES
        )));

        player.sendMessage(Text.literal(""));

        for (HomeData h : list) {
            player.sendMessage(HomeTextBuilder.homeLine(h));
        }

        player.sendMessage(HomeTextBuilder.divider());

        return 1;
    }

    // ───────────────── HELPER ─────────────────

    private static boolean checkAuth(ServerPlayerEntity player) {
        if (!PlayerDataManager.isAuthenticated(
                player.getName().getString())) {
            player.sendMessage(Messages.mustLogin());
            return false;
        }
        return true;
    }
}