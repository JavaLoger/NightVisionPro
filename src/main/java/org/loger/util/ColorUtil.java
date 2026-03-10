package org.loger.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kotlin.KotlinVersion;
import kotlin.text.Typography;
import net.md_5.bungee.api.ChatColor;

public class ColorUtil {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes(Typography.amp, buffer.toString());
    }

    public static String stripColors(String message) {
        if (message == null) {
            return null;
        }
        String stripped = HEX_PATTERN.matcher(message).replaceAll("");
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes(Typography.amp, stripped));
    }

    public static String gradientColor(double value) {
        int g;
        int r;
        double value2 = Math.max(0.0d, Math.min(1.0d, value));
        if (value2 > 0.5d) {
            g = (int) ((1.0d - ((value2 - 0.5d) * 2.0d)) * 255.0d);
            r = 255;
        } else {
            r = (int) (2.0d * value2 * 255.0d);
            g = 255;
        }
        return String.format("&#%02X%02X00", Integer.valueOf(r), Integer.valueOf(g));
    }

    public static String gradientValue(double value) {
        return gradientColor(value) + String.format("%.4f", Double.valueOf(value));
    }

    public static String gradientValue(double value, String format) {
        return gradientColor(value) + String.format(format, Double.valueOf(value));
    }
}
