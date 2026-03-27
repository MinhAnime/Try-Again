package com.minhduong.command;

import com.minhduong.data.PlayerDataManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class HelpCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tryagain").executes(ctx -> {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player == null) return 0;

            boolean authed = PlayerDataManager.isAuthenticated(player.getName().getString());
            boolean exists = PlayerDataManager.accountExists(player.getName().getString());

            send(player, "§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            send(player, "§6§l        TRY AGAIN §7• §fHướng dẫn");
            send(player, "§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            if (!authed) {
                send(player, "§e§l🔐 XÁC THỰC");
                if (exists) {
                    send(player, " §6▶ §f/login <mật_khẩu> §8- §7Đăng nhập vào server");
                } else {
                    send(player, " §6▶ §f/register <mật_khẩu> <nhắc_lại> <token>");
                    send(player, "     §8↳ §7Tạo tài khoản mới");
                }
                send(player, "");
                send(player, "§7Đăng nhập để mở khóa toàn bộ lệnh.");
                send(player, "§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                return 1;
            }

            // ===== ĐÃ ĐĂNG NHẬP =====

            send(player, "§e§l🔐 XÁC THỰC");
            send(player, " §6▶ §f/login <mật_khẩu> §8- §7Đăng nhập");
            send(player, " §6▶ §f/register <mật_khẩu> <nhắc_lại> <token>");
            send(player, "     §8↳ §7Tạo tài khoản");
            send(player, "");

            send(player, "§e§l🏠 HOME");
            send(player, " §6▶ §f/sethome <tên> §8- §7Lưu vị trí hiện tại");
            send(player, " §6▶ §f/home §8- §7Dịch chuyển về nhà (đa thế giới)");
            send(player, " §6▶ §f/delhome <tên> §8- §7Xóa vị trí home");
            send(player, " §6▶ §f/lshome §8- §7Xem danh sách home đã lưu");
            send(player, "");

            send(player, "§e§l✨ TELEPORT");
            send(player, " §6▶ §f/tpa <tên> §8- §7Gửi yêu cầu teleport");
            send(player, " §6▶ §f/tpaok §8- §7Chấp nhận teleport");
            send(player, " §6▶ §f/tpdeny §8- §7Từ chối teleport");
            send(player, "");

            send(player, "§8Gõ §f/tryagain §8bất cứ lúc nào để xem lại menu.");
            send(player, "§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            return 1;
        }));
    }

    private static void send(ServerPlayerEntity player, String msg) {
        player.sendMessage(msg.isEmpty() ? Text.literal(" ") : Text.literal(msg));
    }
}