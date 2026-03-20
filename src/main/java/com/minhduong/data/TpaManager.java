package com.minhduong.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaManager {
    public static final long EXPIRE_MS = 30_000;
    private static final Map<UUID, UUID> requests = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> requestTime = new ConcurrentHashMap<>();
    public static void sendRequest(UUID sender, UUID target) {
        requests.put(sender, target);
        requestTime.put(sender, System.currentTimeMillis());
    }
    public static UUID getPendingSenderFor(UUID target) {
        for (Map.Entry<UUID, UUID> e : requests.entrySet()) {
            if (e.getValue().equals(target)) {
                UUID s = e.getKey();
                if (isExpired(s)) {
                    clearRequest(s);
                    return null;
                }
                return s;
            }
        }
        return null;
    }
    public static boolean hasOutgoingRequest(UUID sender) {
        if (!requests.containsKey(sender)) return false;
        if (isExpired(sender)) {
            clearRequest(sender);
            return false;
        }
        return true;
    }

    public static void clearRequest(UUID sender) { requests.remove(sender);
        requestTime.remove(sender);
    }

    public static void clearAll(UUID uuid) {
        clearRequest(uuid);
        requests.entrySet().removeIf(e -> e.getValue().equals(uuid));
    }

    private static boolean isExpired(UUID s) {
        Long t = requestTime.get(s);
        return t == null || (System.currentTimeMillis() - t) > EXPIRE_MS;
    }
}
