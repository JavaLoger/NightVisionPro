package org.loger.signalr;

import org.loger.scheduler.ScheduledTask;
import org.loger.scheduler.SchedulerManager;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class SignalRHeartbeatScheduler {
    private static final int DEFAULT_INTERVAL_SECONDS = 30;
    private final Logger logger;
    private Runnable onSessionExpiredCallback;
    private final JavaPlugin plugin;
    private ScheduledTask scheduledTask;
    private final SignalRSessionManager sessionManager;

    public SignalRHeartbeatScheduler(JavaPlugin plugin, SignalRSessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.logger = plugin.getLogger();
    }

    public void start() {
        start(30);
    }

    public void start(int intervalSeconds) {
        if (this.scheduledTask != null) {
            stop();
        }
        long intervalTicks = ((long) intervalSeconds) * 20;
        this.scheduledTask = SchedulerManager.getAdapter().runAsyncRepeating(this::sendHeartbeat, intervalTicks, intervalTicks);
        this.logger.info("[SignalR] Heartbeat scheduler started (interval: " + intervalSeconds + "s)");
    }

    public void stop() {
        if (this.scheduledTask != null) {
            this.scheduledTask.cancel();
            this.scheduledTask = null;
            this.logger.info("[SignalR] Heartbeat scheduler stopped");
        }
    }

    private void sendHeartbeat() {
        if (!this.sessionManager.isSessionValid()) {
            return;
        }
        this.sessionManager.sendHeartbeat().thenAccept(result -> {
            String error;
            if (result.isSuccess() || (error = result.getError()) == null) {
                return;
            }
            if (error.contains("expired") || error.contains("invalid")) {
                this.logger.warning("[SignalR] Heartbeat failed: " + error);
                if (this.onSessionExpiredCallback != null) {
                    this.onSessionExpiredCallback.run();
                }
            }
        });
    }

    public void setOnSessionExpiredCallback(Runnable callback) {
        this.onSessionExpiredCallback = callback;
    }

    public boolean isRunning() {
        return (this.scheduledTask == null || this.scheduledTask.isCancelled()) ? false : true;
    }
}
