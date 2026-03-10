package org.loger.penalty;

public interface ActionHandler {
    ActionType getActionType();

    void handle(String str, PenaltyContext penaltyContext);
}
