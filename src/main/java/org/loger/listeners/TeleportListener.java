package org.loger.listeners;

import org.loger.checks.AICheck;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportListener implements Listener {
    private final AICheck aiCheck;

    public TeleportListener(AICheck aiCheck) {
        this.aiCheck = aiCheck;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        if (this.aiCheck != null) {
            this.aiCheck.onTeleport(event.getPlayer());
        }
    }
}
