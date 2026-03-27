package com.minhduong.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextFormat {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(&#([A-Fa-f0-9]{6}))|(<#([A-Fa-f0-9]{6})>)|(&([0-9a-fk-or]))");

    public static MutableText parse(String text) {
        if (text == null || text.isEmpty()) return Text.empty();
        
        MutableText result = Text.empty();
        Style currentStyle = Style.EMPTY;
        
        int cursor = 0;
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        
        while (matcher.find()) {
            if (matcher.start() > cursor) {
                result.append(Text.literal(text.substring(cursor, matcher.start())).setStyle(currentStyle));
            }
            if (matcher.group(2) != null) { // &#RRGGBB
                currentStyle = currentStyle.withColor(TextColor.fromRgb(Integer.parseInt(matcher.group(2), 16)));
            } else if (matcher.group(4) != null) { // <#RRGGBB>
                currentStyle = currentStyle.withColor(TextColor.fromRgb(Integer.parseInt(matcher.group(4), 16)));
            } else if (matcher.group(6) != null) { // &c
                char code = matcher.group(6).charAt(0);
                Formatting formatting = byCode(code);
                if (formatting != null) {
                    if (formatting.isColor()) {
                        // Reset other styles if color is specified (vanilla behavior)
                        currentStyle = currentStyle.withColor(formatting).withBold(false).withItalic(false).withUnderline(false).withStrikethrough(false).withObfuscated(false);
                    } else if (formatting == Formatting.BOLD) {
                        currentStyle = currentStyle.withBold(true);
                    } else if (formatting == Formatting.ITALIC) {
                        currentStyle = currentStyle.withItalic(true);
                    } else if (formatting == Formatting.UNDERLINE) {
                        currentStyle = currentStyle.withUnderline(true);
                    } else if (formatting == Formatting.STRIKETHROUGH) {
                        currentStyle = currentStyle.withStrikethrough(true);
                    } else if (formatting == Formatting.OBFUSCATED) {
                        currentStyle = currentStyle.withObfuscated(true);
                    } else if (formatting == Formatting.RESET) {
                        currentStyle = Style.EMPTY;
                    }
                }
            }
            cursor = matcher.end();
        }
        
        if (cursor < text.length()) {
            result.append(Text.literal(text.substring(cursor)).setStyle(currentStyle));
        }
        
        return result;
    }

    private static Formatting byCode(char code) {
        for (Formatting f : Formatting.values()) {
            if (f.getCode() == code) return f;
        }
        return null;
    }
}
