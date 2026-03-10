package org.loger.config;

public enum ServerType {
    SIGNALR,
    HUGGINGFACE;

    public static ServerType fromString(String value) {
        if (value == null || value.isEmpty()) {
            return SIGNALR;
        }
        try {
            return value.toUpperCase().startsWith("HUGGING") ? HUGGINGFACE : SIGNALR;
        } catch (Exception e) {
            return SIGNALR;
        }
    }
}
