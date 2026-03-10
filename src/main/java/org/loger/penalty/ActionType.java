package org.loger.penalty;

public enum ActionType {
    BAN("%BAN%"),
    KICK("%KICK%"),
    CUSTOM_ALERT("%CUSTOM_ALERT%"),
    RAW(null);

    private final String prefix;

    ActionType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public static ActionType fromCommand(String command) {
        if (command == null || command.isEmpty()) {
            return RAW;
        }
        String trimmed = command.trim();
        for (ActionType type : values()) {
            if (type.prefix != null && trimmed.startsWith(type.prefix)) {
                return type;
            }
        }
        return RAW;
    }

    public String stripPrefix(String command) {
        if (command == null || this.prefix == null) {
            return command != null ? command.trim() : "";
        }
        String trimmed = command.trim();
        if (trimmed.startsWith(this.prefix)) {
            return trimmed.substring(this.prefix.length()).trim();
        }
        return trimmed;
    }

    public boolean isConsoleCommand() {
        return this == BAN || this == KICK || this == RAW;
    }

    public boolean isPunishment() {
        return this == BAN || this == KICK;
    }
}
