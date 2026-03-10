package org.loger.scheduler;

import org.bukkit.plugin.Plugin;

public class SchedulerManager {
    private static SchedulerAdapter adapter;
    private static boolean initialized = false;
    private static ServerType serverType;

    public static void initialize(Plugin plugin) {
        if (initialized) {
            throw new IllegalStateException("SchedulerManager is already initialized");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        try {
            serverType = detectServerType();
            if (serverType == ServerType.FOLIA) {
                adapter = new FoliaSchedulerAdapter(plugin);
            } else {
                adapter = new BukkitSchedulerAdapter(plugin);
            }
            initialized = true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SchedulerManager", e);
        }
    }

    public static SchedulerAdapter getAdapter() {
        if (!initialized || adapter == null) {
            throw new IllegalStateException("SchedulerManager has not been initialized. Call initialize(plugin) first.");
        }
        return adapter;
    }

    public static ServerType getServerType() {
        if (!initialized) {
            throw new IllegalStateException("SchedulerManager has not been initialized. Call initialize(plugin) first.");
        }
        return serverType;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    private static ServerType detectServerType() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            return ServerType.FOLIA;
        } catch (ClassNotFoundException e) {
            return ServerType.BUKKIT;
        }
    }

    static void reset() {
        adapter = null;
        serverType = null;
        initialized = false;
    }
}
