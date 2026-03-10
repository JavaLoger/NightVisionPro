package org.loger.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class BukkitSchedulerAdapter implements SchedulerAdapter {
    private final Plugin plugin;
    private final BukkitScheduler scheduler = Bukkit.getScheduler();

    public BukkitSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runSync(Runnable task) {
        BukkitTask bukkitTask = this.scheduler.runTask(this.plugin, task);
        return new BukkitScheduledTask(bukkitTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runSyncDelayed(Runnable task, long delayTicks) {
        BukkitTask bukkitTask = this.scheduler.runTaskLater(this.plugin, task, delayTicks);
        return new BukkitScheduledTask(bukkitTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runSyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = this.scheduler.runTaskTimer(this.plugin, task, delayTicks, periodTicks);
        return new BukkitScheduledTask(bukkitTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runAsync(Runnable task) {
        BukkitTask bukkitTask = this.scheduler.runTaskAsynchronously(this.plugin, task);
        return new BukkitScheduledTask(bukkitTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runAsyncDelayed(Runnable task, long delayTicks) {
        BukkitTask bukkitTask = this.scheduler.runTaskLaterAsynchronously(this.plugin, task, delayTicks);
        return new BukkitScheduledTask(bukkitTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = this.scheduler.runTaskTimerAsynchronously(this.plugin, task, delayTicks, periodTicks);
        return new BukkitScheduledTask(bukkitTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runEntitySync(Entity entity, Runnable task) {
        return runSync(task);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runEntitySyncDelayed(Entity entity, Runnable task, long delayTicks) {
        return runSyncDelayed(task, delayTicks);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runEntitySyncRepeating(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        return runSyncRepeating(task, delayTicks, periodTicks);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runRegionSync(Location location, Runnable task) {
        return runSync(task);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runRegionSyncDelayed(Location location, Runnable task, long delayTicks) {
        return runSyncDelayed(task, delayTicks);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runRegionSyncRepeating(Location location, Runnable task, long delayTicks, long periodTicks) {
        return runSyncRepeating(task, delayTicks, periodTicks);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ServerType getServerType() {
        return ServerType.BUKKIT;
    }
}
