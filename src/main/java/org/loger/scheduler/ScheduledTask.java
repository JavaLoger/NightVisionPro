package org.loger.scheduler;

public interface ScheduledTask {
    void cancel();

    boolean isCancelled();

    boolean isRunning();
}
