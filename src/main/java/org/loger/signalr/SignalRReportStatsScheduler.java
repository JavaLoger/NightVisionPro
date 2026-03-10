package org.loger.signalr;

import org.loger.scheduler.ScheduledTask;
import org.loger.scheduler.SchedulerManager;
import org.loger.signalr.SignalRSessionManager;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class SignalRReportStatsScheduler {
    private final Logger logger;
    private Runnable onLimitClearedCallback;
    private Runnable onLimitExceededCallback;
    private Runnable onSessionExpiredCallback;
    private final IntSupplier onlinePlayersSupplier;
    private final JavaPlugin plugin;
    private ScheduledTask scheduledTask;
    private final SignalRSessionManager sessionManager;
    private volatile boolean limitExceeded = false;
    private volatile int maxOnline = 0;

    public SignalRReportStatsScheduler(JavaPlugin plugin, SignalRSessionManager sessionManager, IntSupplier onlinePlayersSupplier) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.onlinePlayersSupplier = onlinePlayersSupplier;
        this.logger = plugin.getLogger();
    }

    public void start(int intervalSeconds) {
        if (this.scheduledTask != null) {
            stop();
        }
        long intervalTicks = ((long) intervalSeconds) * 20;
        this.scheduledTask = SchedulerManager.getAdapter().runAsyncRepeating(() -> {
            reportNow();
        }, intervalTicks, intervalTicks);
        this.logger.info("[SignalR] ReportStats scheduler started (interval: " + intervalSeconds + "s)");
        SchedulerManager.getAdapter().runAsyncDelayed(this::reportNow, 20L);
    }

    public void stop() {
        if (this.scheduledTask != null) {
            this.scheduledTask.cancel();
            this.scheduledTask = null;
            this.logger.info("[SignalR] ReportStats scheduler stopped");
        }
    }

    public CompletableFuture<SignalRSessionManager.ReportStatsResult> reportNow() {
        if (!this.sessionManager.isSessionValid()) {
            return CompletableFuture.completedFuture(new SignalRSessionManager.ReportStatsResult(false, false, 0, "No active session"));
        }
        int onlinePlayers = this.onlinePlayersSupplier.getAsInt();
        return this.sessionManager.reportStats(onlinePlayers).thenApply(result -> {
            if (result.isSuccess()) {
                boolean wasLimitExceeded = this.limitExceeded;
                this.limitExceeded = result.isLimitExceeded();
                this.maxOnline = result.getMaxOnline();
                if (!wasLimitExceeded && this.limitExceeded) {
                    this.logger.warning("[SignalR] Online limit exceeded (" + onlinePlayers + "/" + this.maxOnline + ") - Predict blocked");
                    if (this.onLimitExceededCallback != null) {
                        this.onLimitExceededCallback.run();
                    }
                } else if (wasLimitExceeded && !this.limitExceeded) {
                    this.logger.info("[SignalR] Online limit cleared - Predict enabled");
                    if (this.onLimitClearedCallback != null) {
                        this.onLimitClearedCallback.run();
                    }
                }
            } else {
                String error = result.getError();
                if (error != null && error.contains(HubErrorParser.NOT_AUTHENTICATED)) {
                    this.logger.warning("[SignalR] ReportStats failed: " + error);
                    this.logger.info("[SignalR] Session expired, triggering re-authentication...");
                    if (this.onSessionExpiredCallback != null) {
                        this.onSessionExpiredCallback.run();
                    }
                }
            }
            return result;
        });
    }

    public boolean isLimitExceeded() {
        return this.limitExceeded;
    }

    public void setLimitExceeded(boolean exceeded) {
        boolean wasLimitExceeded = this.limitExceeded;
        this.limitExceeded = exceeded;
        if (!wasLimitExceeded && exceeded && this.onLimitExceededCallback != null) {
            this.onLimitExceededCallback.run();
        } else if (wasLimitExceeded && !exceeded && this.onLimitClearedCallback != null) {
            this.onLimitClearedCallback.run();
        }
    }

    public int getMaxOnline() {
        return this.maxOnline;
    }

    public void setOnLimitExceededCallback(Runnable callback) {
        this.onLimitExceededCallback = callback;
    }

    public void setOnLimitClearedCallback(Runnable callback) {
        this.onLimitClearedCallback = callback;
    }

    public void setOnSessionExpiredCallback(Runnable callback) {
        this.onSessionExpiredCallback = callback;
    }

    public boolean isRunning() {
        return (this.scheduledTask == null || this.scheduledTask.isCancelled()) ? false : true;
    }
}
