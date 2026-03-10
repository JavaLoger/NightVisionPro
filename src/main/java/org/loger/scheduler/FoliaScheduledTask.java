package org.loger.scheduler;

public class FoliaScheduledTask implements ScheduledTask {
    private volatile boolean cancelled = false;
    private final io.papermc.paper.threadedregions.scheduler.ScheduledTask task;

    public FoliaScheduledTask(io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
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
