package org.loger.config;

public enum Label {
    CHEAT,
    LEGIT,
    UNLABELED;

    public static Label fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
