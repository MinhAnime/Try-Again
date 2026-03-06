package com.minhduong.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class SessionManager {
    private static final Map<UUID, Boolean> loggedInPlayers = new HashMap<>();

    public static void login(UUID uuid) {
        loggedInPlayers.put(uuid, true);
    }

    public static void logout(UUID uuid) {
        loggedInPlayers.remove(uuid);
    }

    public static boolean isLoggedIn(UUID uuid) {
        return loggedInPlayers.getOrDefault(uuid, false);
    }

    public static boolean checkAndNotify(ServerPlayerEntity player) {
        if (!isLoggedIn(player.getUuid())) {
            player.sendMessage(Text.literal("Bạn phải /login trước khi sử dụng lệnh này!"), false);
            return false;
        }
        return true;
    }
}