package org.loger.scheduler;

import org.bukkit.scheduler.BukkitTask;

public class BukkitScheduledTask implements ScheduledTask {
    private volatile boolean cancelled = false;
    private final BukkitTask task;

    public BukkitScheduledTask(BukkitTask task) {
        this.task = task;
    }

    @Override // org.loger.scheduler.ScheduledTask
    public void cancel() {
        if (!this.cancelled) {
            this.cancelled = true;
            this.task.cancel();
        }
    }

    @Override // org.loger.scheduler.ScheduledTask
    public boolean isCancelled() {
        return this.cancelled || this.task.isCancelled();
    }

    @Override // org.loger.scheduler.ScheduledTask
    public boolean isRunning() {
        return !isCancelled();
    }
}
