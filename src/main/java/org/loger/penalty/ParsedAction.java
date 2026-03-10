package org.loger.penalty;

public class ParsedAction {
    private final String command;
    private final ActionType type;

    public ParsedAction(ActionType type, String command) {
        this.type = type != null ? type : ActionType.RAW;
        this.command = command != null ? command : "";
    }

    public ActionType getType() {
        return this.type;
    }

    public String getCommand() {
        return this.command;
    }

    public boolean hasCommand() {
        return (this.command == null || this.command.isEmpty()) ? false : true;
    }

    public String toString() {
        return "ParsedAction{type=" + this.type + ", command='" + this.command + "'}";
    }
}
