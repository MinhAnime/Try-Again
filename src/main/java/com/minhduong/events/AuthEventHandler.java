package com.minhduong.events;

import com.minhduong.data.PlayerDataManager;
import com.minhduong.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.*;

public class AuthEventHandler {

    private static final long REMINDER_MS = 10_000;
    private static final Map<UUID, Long> lastReminder = new HashMap<>();
    private static int  authTick = 0; // kiểm tra auth mỗi 1 giây (20 tick)
    private static int  hudTick  = 0; // cập nhật HUD mỗi 2 giây (40 tick)
    private static boolean tickRegistered = false;

    // ─── Player join ──────────────────────────────────────────────────────────

    public static void onPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        String username = player.getName().getString();

        TokenConfig.load();
        SessionManager.startSession(player);
        freezePlayer(player);
        registerTickHandler(server);


        // Java → nhắc gõ lệnh
        if (PlayerDataManager.accountExists(username)) {
            player.sendMessage(Messages.MUST_LOGIN);
        } else {
            player.sendMessage(Messages.info("Chào mừng! Dùng: /register <mật khẩu> <xác nhận mật khẩu> <token>"));
        }
    }

    // ─── Player leave ─────────────────────────────────────────────────────────

    public static void onPlayerLeave(ServerPlayNetworkHandler handler, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        PlayerDataManager.clearSession(player.getName().getString());
        SessionManager.endSession(player);
        lastReminder.remove(player.getUuid());
    }

    // ─── Tick handler ─────────────────────────────────────────────────────────

    private static void registerTickHandler(MinecraftServer server) {
        if (tickRegistered) return;
        tickRegistered = true;

        ServerTickEvents.END_SERVER_TICK.register(s -> {
            authTick++;
            hudTick++;

            // Auth check: mỗi giây
            if (authTick >= 20) {
                authTick = 0;
                List<ServerPlayerEntity> toKick = new ArrayList<>();

                for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
                    String uname = p.getName().getString();
                    if (PlayerDataManager.isAuthenticated(uname)) continue;

                    if (SessionManager.isExpired(p)) {
                        toKick.add(p); continue;
                    }


                    freezePlayer(p);
                }

                toKick.forEach(p -> p.networkHandler.disconnect(
                        Text.literal("§cVào treo máy à. Cút ngay ra.")));
            }

        });
    }

    // ─── Đóng băng vị trí ─────────────────────────────────────────────────────

    private static void freezePlayer(ServerPlayerEntity player) {
        Vec3d pos = SessionManager.getJoinPos(player);
        player.changeGameMode(GameMode.SPECTATOR);
        player.teleport(
                player.getEntityWorld(),
                pos.x, pos.y, pos.z,
                Set.of(),
                player.getYaw(),
                player.getPitch(),
                true
        );
    }
}
