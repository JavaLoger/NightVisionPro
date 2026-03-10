package org.loger.penalty.handlers;

import org.loger.penalty.ActionHandler;
import org.loger.penalty.ActionType;
import org.loger.penalty.PenaltyContext;
import org.loger.scheduler.SchedulerManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class KickHandler implements ActionHandler {
    private final JavaPlugin plugin;

    public KickHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override // org.loger.penalty.ActionHandler
    public void handle(String command, PenaltyContext context) {
        if (command == null || command.isEmpty()) {
            return;
        }
        SchedulerManager.getAdapter().runSync(() -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        });
    }

    @Override // org.loger.penalty.ActionHandler
    public ActionType getActionType() {
        return ActionType.KICK;
    }
}
