package org.loger.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public interface SchedulerAdapter {
    ServerType getServerType();

    ScheduledTask runAsync(Runnable runnable);

    ScheduledTask runAsyncDelayed(Runnable runnable, long j);

    ScheduledTask runAsyncRepeating(Runnable runnable, long j, long j2);

    ScheduledTask runEntitySync(Entity entity, Runnable runnable);

    ScheduledTask runEntitySyncDelayed(Entity entity, Runnable runnable, long j);

    ScheduledTask runEntitySyncRepeating(Entity entity, Runnable runnable, long j, long j2);

    ScheduledTask runRegionSync(Location location, Runnable runnable);

    ScheduledTask runRegionSyncDelayed(Location location, Runnable runnable, long j);

    ScheduledTask runRegionSyncRepeating(Location location, Runnable runnable, long j, long j2);

    ScheduledTask runSync(Runnable runnable);

    ScheduledTask runSyncDelayed(Runnable runnable, long j);

    ScheduledTask runSyncRepeating(Runnable runnable, long j, long j2);
}
