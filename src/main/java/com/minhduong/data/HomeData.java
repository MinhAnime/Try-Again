package com.minhduong.data;

public class HomeData {
    private final String worldId;
    private final double x, y, z;

    public HomeData(String worldId, double x, double y, double z) {
        this.worldId = worldId;
        this.x = x; this.y = y; this.z = z;
    }

    public String getWorldId() { return worldId; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
}
