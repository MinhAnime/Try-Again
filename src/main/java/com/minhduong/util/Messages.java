package com.minhduong.util;

import com.minhduong.data.LanguageManager;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class Messages {
    public static MutableText error(String key, Object... args) {
        return Text.empty().append(LanguageManager.getText("prefix_error")).append(LanguageManager.getText(key, args));
    }
    public static MutableText success(String key, Object... args) {
        return Text.empty().append(LanguageManager.getText("prefix_success")).append(LanguageManager.getText(key, args));
    }
    public static MutableText info(String key, Object... args) {
        return Text.empty().append(LanguageManager.getText("prefix_info")).append(LanguageManager.getText(key, args));
    }

    public static MutableText get(String key, Object... args) {
        return LanguageManager.getText(key, args);
    }

    public static MutableText mustLogin()        { return info("must_login"); }
    public static MutableText mustRegister()     { return info("must_register"); }
    public static MutableText alreadyAuth()      { return error("already_auth"); }
    public static MutableText loginSuccess()     { return success("login_success"); }
    public static MutableText wrongPassword()    { return error("wrong_password"); }
    public static MutableText registerSuccess()  { return success("register_success"); }
    public static MutableText passNoMatch()      { return error("pass_no_match"); }
    public static MutableText nameTaken()        { return error("name_taken"); }
    public static MutableText invalidToken()     { return error("invalid_token"); }
    public static MutableText passTooShort()     { return error("pass_too_short"); }
}