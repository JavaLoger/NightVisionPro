package org.loger.compat;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.loger.scheduler.ScheduledTask;
import org.loger.scheduler.SchedulerManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class EventCompat {
    private static boolean hasServerTickEndEvent;

    public interface TickHandler {
        int getCurrentTick();

        void start();

        void stop();
    }

    static {
        hasServerTickEndEvent = false;
        try {
            Class.forName("com.destroystokyo.paper.event.server.ServerTickEndEvent");
            hasServerTickEndEvent = true;
        } catch (ClassNotFoundException e) {
            hasServerTickEndEvent = false;
        }
    }

    private EventCompat() {
    }

    public static boolean hasServerTickEndEvent() {
        return hasServerTickEndEvent;
    }

    public static TickHandler createTickHandler(JavaPlugin plugin, Runnable onTick) {
        if (hasServerTickEndEvent) {
            return new PaperTickHandler(plugin, onTick);
        }
        return new SpigotTickHandler(plugin, onTick);
    }

    private static class PaperTickHandler implements TickHandler, Listener {
        private final Runnable onTick;
        private final JavaPlugin plugin;
        private int currentTick = 0;
        private boolean running = false;

        PaperTickHandler(JavaPlugin plugin, Runnable onTick) {
            this.plugin = plugin;
            this.onTick = onTick;
        }

        @Override // org.loger.compat.EventCompat.TickHandler
        public void start() {
            if (!this.running) {
                Bukkit.getPluginManager().registerEvents(this, this.plugin);
                this.running = true;
            }
        }

        @Override // org.loger.compat.EventCompat.TickHandler
        public void stop() {
            if (this.running) {
                HandlerList.unregisterAll(this);
                this.running = false;
            }
        }

        @Override // org.loger.compat.EventCompat.TickHandler
        public int getCurrentTick() {
            return this.currentTick;
        }

        @EventHandler
        public void onServerTick(ServerTickEndEvent event) {
            this.currentTick++;
            if (this.onTick != null) {
                this.onTick.run();
            }
        }
    }

    private static class SpigotTickHandler implements TickHandler {
        private int currentTick = 0;
        private final Runnable onTick;
        private final JavaPlugin plugin;
        private ScheduledTask task;

        SpigotTickHandler(JavaPlugin plugin, Runnable onTick) {
            this.plugin = plugin;
            this.onTick = onTick;
        }

        @Override // org.loger.compat.EventCompat.TickHandler
        public void start() {
            if (this.task == null) {
                this.task = SchedulerManager.getAdapter().runSyncRepeating(() -> {
                    this.currentTick++;
                    if (this.onTick != null) {
                        this.onTick.run();
                    }
                }, 0L, 1L);
            }
        }

        @Override // org.loger.compat.EventCompat.TickHandler
        public void stop() {
            if (this.task != null) {
                this.task.cancel();
                this.task = null;
            }
        }

        @Override // org.loger.compat.EventCompat.TickHandler
        public int getCurrentTick() {
            return this.currentTick;
        }
    }
}
