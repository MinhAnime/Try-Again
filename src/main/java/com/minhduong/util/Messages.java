package com.minhduong.util;

import net.minecraft.text.Text;

public class Messages {
    public static Text error(String msg)   { return Text.literal("§c[TryAgain] §f" + msg); }
    public static Text success(String msg) { return Text.literal("§a[TryAgain] §f" + msg); }
    public static Text info(String msg)    { return Text.literal("§e[TryAgain] §f" + msg); }

    public static final Text MUST_LOGIN        = info("Bạn phải đăng nhập! Dùng: /login <mật khẩu>");
    public static final Text MUST_REGISTER     = info("Chưa có tài khoản! Dùng: /register <mật_khẩu> <nhắc_lại> <token>");
    public static final Text ALREADY_AUTH      = error("Bạn đã đăng nhập rồi.");
    public static final Text LOGIN_SUCCESS     = success("Đăng nhập thành công!");
    public static final Text WRONG_PASSWORD    = error("Sai mật khẩu.");
    public static final Text REGISTER_SUCCESS  = success("Đăng ký thành công! Bạn đã được đăng nhập.");
    public static final Text PASS_NO_MATCH     = error("Hai mật khẩu không khớp.");
    public static final Text NAME_TAKEN        = error("Tên này đã được đăng ký.");
    public static final Text INVALID_TOKEN     = error("Token không hợp lệ.");
    public static final Text PASS_TOO_SHORT    = error("Mật khẩu phải có ít nhất 4 ký tự.");
}