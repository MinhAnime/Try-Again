package com.minhduong.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class SessionManager {
    public static final int TIMEOUT_SECONDS = 60;
    private static final Map<UUID, Long> joinTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3d> joinPos = new ConcurrentHashMap<>();

    public static void startSession(ServerPlayerEntity player) {
        joinTime.put(player.getUuid(), System.currentTimeMillis());
        joinPos.put(player.getUuid(), player.getEntityPos());
    }
    public static void endSession(ServerPlayerEntity player) {
        joinTime.remove(player.getUuid());
        joinPos.remove(player.getUuid());
    }
    public static boolean isExpired(ServerPlayerEntity player) {
        Long t = joinTime.get(player.getUuid());
        return t != null && (System.currentTimeMillis() - t) > (long) TIMEOUT_SECONDS * 1000;
    }

    public static Vec3d getJoinPos(ServerPlayerEntity player) {
        return joinPos.getOrDefault(player.getUuid(), player.getEntityPos());
    }
}