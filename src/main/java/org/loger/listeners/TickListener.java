package org.loger.listeners;

import org.loger.checks.AICheck;
import org.loger.compat.EventCompat;
import org.loger.session.ISessionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TickListener {
    private final AICheck aiCheck;
    private HitListener hitListener;
    private final ISessionManager sessionManager;
    private final EventCompat.TickHandler tickHandler;

    public TickListener(JavaPlugin plugin, ISessionManager sessionManager, AICheck aiCheck) {
        this.sessionManager = sessionManager;
        this.aiCheck = aiCheck;
        this.tickHandler = EventCompat.createTickHandler(plugin, this::onTick);
    }

    public void start() {
        this.tickHandler.start();
    }

    public void stop() {
        this.tickHandler.stop();
    }

    public void setHitListener(HitListener hitListener) {
        this.hitListener = hitListener;
    }

    private void onTick() {
        int currentTick = this.tickHandler.getCurrentTick();
        if (this.hitListener != null) {
            this.hitListener.setCurrentTick(currentTick);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (this.aiCheck != null) {
                this.aiCheck.onTick(player);
            }
        }
    }

    public int getCurrentTick() {
        return this.tickHandler.getCurrentTick();
    }
}
