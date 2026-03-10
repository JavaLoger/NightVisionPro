package org.loger.penalty.handlers;

import org.loger.penalty.ActionHandler;
import org.loger.penalty.ActionType;
import org.loger.penalty.BanAnimation;
import org.loger.penalty.PenaltyContext;
import org.loger.scheduler.SchedulerManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class BanHandler implements ActionHandler {
    private final BanAnimation animation;
    private boolean animationEnabled = true;

    public BanHandler(JavaPlugin plugin) {
        this.animation = new BanAnimation(plugin);
    }

    @Override // org.loger.penalty.ActionHandler
    public void handle(String command, PenaltyContext context) {
        if (command == null || command.isEmpty()) {
            return;
        }
        Player player = null;
        if (context != null && context.getPlayerName() != null) {
            player = Bukkit.getPlayer(context.getPlayerName());
        }
        if (!this.animationEnabled || player == null || !player.isOnline()) {
            SchedulerManager.getAdapter().runSync(() -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            });
        } else {
            this.animation.playAnimation(player, command, context);
        }
    }

    public void setAnimationEnabled(boolean enabled) {
        this.animationEnabled = enabled;
    }

    public boolean isAnimationEnabled() {
        return this.animationEnabled;
    }

    public BanAnimation getAnimation() {
        return this.animation;
    }

    public void shutdown() {
        this.animation.shutdown();
    }

    @Override // org.loger.penalty.ActionHandler
    public ActionType getActionType() {
        return ActionType.BAN;
    }
}
