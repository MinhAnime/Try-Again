package com.minhduong.command;

import com.minhduong.data.HomeData;
import com.minhduong.data.HomeManager;
import com.minhduong.data.PlayerDataManager;
import com.minhduong.util.Messages;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Set;


public class HomeCommand {
    public static void register(CommandDispatcher<ServerCommandSource>  dispatcher) {
        //sethome
        dispatcher.register(CommandManager.literal("sethome")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    if (!checkAuth(player)) return 0;
                    Vec3d pos = player.getEntityPos();
                    String world = player.getEntityWorld().getRegistryKey().getValue().toString();
                    HomeManager.setHome(player.getName().getString(), new HomeData(world, pos.x, pos.y, pos.z));
                    player.sendMessage(Messages.success(String.format("Đã lưu home tại (%.1f, %.1f, %.1f) - %s", pos.x, pos.y, pos.z, friendly(world))));
                    return 1;
                })
        );
        //delhome
        dispatcher.register(
                CommandManager.literal("delhome").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    if (!checkAuth(player)) return 0;
                    if (!HomeManager.hasHome(player.getName().getString())) {
                        player.sendMessage(Messages.error("Bạn chưa có nhà nào."));
                        return 0;
                    }
                    HomeManager.deleteHome(player.getName().getString());
                    player.sendMessage(Messages.success("Đã xóa home."));
                    return 1;
                })
        );
        //home
        dispatcher.register(
                CommandManager.literal("home").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    if (!checkAuth(player)) return 0;
                    HomeData home = HomeManager.getHome(player.getName().getString()).orElse(null);
                    if (home == null) {
                        player.sendMessage(Messages.error("Bạn chưa có nhà nào. Dùng /sethome để lưu nhà"));
                        return 0;
                    }
                    ServerWorld target = null;
                    for (ServerWorld w : player.getEntityWorld().getServer().getWorlds())
                        if (w.getRegistryKey().getValue().toString().equals(home.getWorldId())){
                            target = w;
                            break;
                        }
                    if (target == null) {
                        player.sendMessage(Messages.error("Không tìm thấy thế giới của home. Có thể nó đã bị xóa."));
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
                    player.sendMessage(Messages.success(String.format(
                            "Đã về home (%.1f, %.1f, %.1f) - %s",
                            home.getX(), home.getY(), home.getZ(), friendly(home.getWorldId()))));
                    return 1;
                })
        );

    }
    private static boolean checkAuth(ServerPlayerEntity player) {
        if (!PlayerDataManager.isAuthenticated(player.getName().getString())) {
            player.sendMessage(Messages.MUST_LOGIN);
            return false;
        }
        return true;
    }
    private static String friendly(String id) {
        return switch (id) {
            case "minecraft:overworld"  -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end"    -> "The End";
            default -> id;
        };
    }
}
