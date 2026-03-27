package com.minhduong.data;

public class HomeData {
    private final String name;
    private final String worldId;
    private final double x, y, z;

    public HomeData(String name, String worldId, double x, double y, double z) {
        this.name    = name;
        this.worldId = worldId;
        this.x = x; this.y = y; this.z = z;
    }

    public String getName()    { return name; }
    public String getWorldId() { return worldId; }
    public double getX()       { return x; }
    public double getY()       { return y; }
    public double getZ()       { return z; }

    public String friendlyWorld() {
        return switch (worldId) {
            case "minecraft:overworld"  -> "Overworld";
            case "minecraft:the_nether" -> "The Nether";
            case "minecraft:the_end"    -> "The End";
            default -> worldId;
        };
    }
}
