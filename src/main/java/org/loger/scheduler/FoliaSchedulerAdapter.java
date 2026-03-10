package org.loger.scheduler;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class FoliaSchedulerAdapter implements SchedulerAdapter {
    private final Plugin plugin;
    private final GlobalRegionScheduler globalScheduler = Bukkit.getGlobalRegionScheduler();
    private final AsyncScheduler asyncScheduler = Bukkit.getAsyncScheduler();
    private final RegionScheduler regionScheduler = Bukkit.getRegionScheduler();

    public FoliaSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runSync(Runnable task) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask = this.globalScheduler.run(this.plugin, scheduledTask -> {
            task.run();
        });
        return new FoliaScheduledTask(foliaTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runSyncDelayed(Runnable task, long delayTicks) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask = this.globalScheduler.runDelayed(this.plugin, scheduledTask -> {
            task.run();
        }, delayTicks);
        return new FoliaScheduledTask(foliaTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runSyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        long adjustedDelay = delayTicks <= 0 ? 1L : delayTicks;
        io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask = this.globalScheduler.runAtFixedRate(this.plugin, scheduledTask -> {
            task.run();
        }, adjustedDelay, periodTicks);
        return new FoliaScheduledTask(foliaTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runAsync(Runnable task) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask = this.asyncScheduler.runNow(this.plugin, scheduledTask -> {
            task.run();
        });
        return new FoliaScheduledTask(foliaTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runAsyncDelayed(Runnable task, long delayTicks) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask = this.asyncScheduler.runDelayed(this.plugin, scheduledTask -> {
            task.run();
        }, 50 * delayTicks, TimeUnit.MILLISECONDS);
        return new FoliaScheduledTask(foliaTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        long delayMs = delayTicks <= 0 ? 50L : delayTicks * 50;
        io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask = this.asyncScheduler.runAtFixedRate(this.plugin, scheduledTask -> {
            task.run();
        }, delayMs, periodTicks * 50, TimeUnit.MILLISECONDS);
        return new FoliaScheduledTask(foliaTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runEntitySync(Entity entity, Runnable task) {
        if (!entity.isValid()) {
            throw new IllegalArgumentException("Entity is not valid");
        }
        io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask = entity.getScheduler().run(this.plugin, scheduledTask -> {
            task.run();
        }, (Runnable) null);
        return new FoliaScheduledTask(foliaTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runEntitySyncDelayed(Entity entity, Runnable task, long delayTicks) {
        if (!entity.isValid()) {
            throw new IllegalArgumentException("Entity is not valid");
        }
        io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask = entity.getScheduler().runDelayed(this.plugin, scheduledTask -> {
            task.run();
        }, (Runnable) null, delayTicks);
        return new FoliaScheduledTask(foliaTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runEntitySyncRepeating(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        if (!entity.isValid()) {
            throw new IllegalArgumentException("Entity is not valid");
        }
        long adjustedDelay = delayTicks <= 0 ? 1L : delayTicks;
        io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask = entity.getScheduler().runAtFixedRate(this.plugin, scheduledTask -> {
            task.run();
        }, (Runnable) null, adjustedDelay, periodTicks);
        return new FoliaScheduledTask(foliaTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runRegionSync(Location location, Runnable task) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Location or world is null");
        }
        io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask = this.regionScheduler.run(this.plugin, location, scheduledTask -> {
            task.run();
        });
        return new FoliaScheduledTask(foliaTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runRegionSyncDelayed(Location location, Runnable task, long delayTicks) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Location or world is null");
        }
        io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask = this.regionScheduler.runDelayed(this.plugin, location, scheduledTask -> {
            task.run();
        }, delayTicks);
        return new FoliaScheduledTask(foliaTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ScheduledTask runRegionSyncRepeating(Location location, Runnable task, long delayTicks, long periodTicks) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Location or world is null");
        }
        long adjustedDelay = delayTicks <= 0 ? 1L : delayTicks;
        io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask = this.regionScheduler.runAtFixedRate(this.plugin, location, scheduledTask -> {
            task.run();
        }, adjustedDelay, periodTicks);
        return new FoliaScheduledTask(foliaTask);
    }

    @Override // org.loger.scheduler.SchedulerAdapter
    public ServerType getServerType() {
        return ServerType.FOLIA;
    }
}
