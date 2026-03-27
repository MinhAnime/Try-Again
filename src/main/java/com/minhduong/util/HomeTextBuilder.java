package com.minhduong.util;

import com.minhduong.data.HomeData;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class HomeTextBuilder {

    // divider line
    public static Text divider() {
        return Text.literal("§8§m--------------------------------");
    }

    // header
    public static Text header(String title) {
        return Text.literal("§6§l✦ §e" + title + " §6§l✦");
    }

    // world icon
    private static String worldIcon(String world) {
        if (world.contains("the_nether")) return "🔥";
        if (world.contains("the_end")) return "☁";
        return "🌍";
    }

    // home line (NO CLICK / NO HOVER)
    public static Text homeLine(HomeData home) {

        String icon = worldIcon(home.getWorldId());

        MutableText line = Text.literal(String.format(
                " §6▸ %s §e%-12s §7(%.0f, %.0f, %.0f) §8- §f%s",
                icon,
                home.getName(),
                home.getX(),
                home.getY(),
                home.getZ(),
                home.friendlyWorld()
        ));

        return line;
    }
}