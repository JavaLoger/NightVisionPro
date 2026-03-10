package org.loger.listeners;

import org.loger.Main;
import org.loger.Permissions;
import org.loger.alert.AlertManager;
import org.loger.checks.AICheck;
import org.loger.scheduler.SchedulerManager;
import org.loger.session.SessionManager;
import org.loger.violation.ViolationManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerListener implements Listener {
    private final AICheck aiCheck;
    private final AlertManager alertManager;
    private HitListener hitListener;
    private final JavaPlugin plugin;
    private final SessionManager sessionManager;
    private final ViolationManager violationManager;

    public PlayerListener(JavaPlugin plugin, AICheck aiCheck, AlertManager alertManager, ViolationManager violationManager, SessionManager sessionManager) {
        this.plugin = plugin;
        this.aiCheck = aiCheck;
        this.alertManager = alertManager;
        this.violationManager = violationManager;
        this.sessionManager = sessionManager;
    }

    public void setHitListener(HitListener hitListener) {
        this.hitListener = hitListener;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (this.hitListener != null) {
            this.hitListener.cacheEntity(player);
        }
        if (this.plugin instanceof Main) {
            Main main = (Main) this.plugin;
            if (main.getProbabilityHistory() != null) {
                main.getProbabilityHistory().recordJoinTime(player.getUniqueId());
            }
            if (main.getHologramManager() != null) {
                SchedulerManager.getAdapter().runSyncDelayed(() -> {
                    if (player.isOnline()) {
                        main.getHologramManager().handlePlayerJoin(player);
                    }
                }, 5L);
            }
        }
        try {
            SchedulerManager.getAdapter().runSyncDelayed(() -> {
                if (player.isOnline()) {
                    if ((player.hasPermission(Permissions.ALERTS) || player.hasPermission(Permissions.ADMIN)) && (this.plugin instanceof Main)) {
                    }
                }
            }, 20L);
        } catch (Exception e) {
            this.plugin.getLogger().warning("Failed to schedule player join task: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        handlePlayerLeave(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        handlePlayerLeave(event.getPlayer());
    }

    private void handlePlayerLeave(Player player) {
        if (this.hitListener != null) {
            this.hitListener.uncachePlayer(player);
        }
        if (this.aiCheck != null) {
            this.aiCheck.handlePlayerQuit(player);
        }
        if (this.alertManager != null) {
            this.alertManager.handlePlayerQuit(player);
        }
        if (this.violationManager != null) {
            this.violationManager.handlePlayerQuit(player);
        }
        if (this.sessionManager != null) {
            this.sessionManager.removeAimProcessor(player.getUniqueId());
        }
        if (this.plugin instanceof Main) {
            Main main = (Main) this.plugin;
            if (main.getHologramManager() != null) {
                main.getHologramManager().handlePlayerQuit(player);
                SchedulerManager.getAdapter().runSyncDelayed(() -> {
                    main.getHologramManager().cleanupDeadHolograms();
                }, 5L);
            }
        }
    }
}
