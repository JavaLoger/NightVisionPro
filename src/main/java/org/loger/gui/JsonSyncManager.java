package org.loger.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class JsonSyncManager implements SyncManager {
    private final File dataFile;
    private final Logger logger;
    private final Map<UUID, SuspectData> cache = new ConcurrentHashMap();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean dirty = false;
    private volatile boolean sortDirty = true;
    private volatile List<SuspectData> sortedCache = null;
    private final ScheduledExecutorService saveScheduler = Executors.newSingleThreadScheduledExecutor();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public JsonSyncManager(File dataFolder, Logger logger) {
        this.dataFile = new File(dataFolder, "gui.json");
        this.logger = logger;
        loadData();
        this.saveScheduler.scheduleAtFixedRate(this::saveIfDirty, 30L, 30L, TimeUnit.SECONDS);
    }

    private void loadData() {
        if (!this.dataFile.exists()) {
            return;
        }
        try {
            Reader reader = new FileReader(this.dataFile);
            try {
                Type type = new TypeToken<List<SuspectDataJson>>() { // from class: org.loger.gui.JsonSyncManager.1
                }.getType();
                List<SuspectDataJson> list = (List) this.gson.fromJson(reader, type);
                if (list != null) {
                    for (SuspectDataJson json : list) {
                        SuspectData data = json.toSuspectData();
                        this.cache.put(data.getPlayerId(), data);
                    }
                }
                reader.close();
            } finally {
            }
        } catch (Exception e) {
            this.logger.warning("Failed to load gui.json: " + e.getMessage());
        }
    }

    private void saveData() {
        try {
            this.dataFile.getParentFile().mkdirs();
            List<SuspectDataJson> list = new ArrayList<>();
            for (SuspectData data : this.cache.values()) {
                list.add(SuspectDataJson.fromSuspectData(data));
            }
            Writer writer = new FileWriter(this.dataFile);
            try {
                this.gson.toJson(list, writer);
                writer.close();
                this.dirty = false;
            } finally {
            }
        } catch (Exception e) {
            this.logger.warning("Failed to save gui.json: " + e.getMessage());
        }
    }

    private void saveIfDirty() {
        if (this.dirty) {
            saveData();
        }
    }

    @Override // org.loger.gui.SyncManager
    public CompletableFuture<Void> addSuspect(SuspectData data) {
        return CompletableFuture.runAsync(() -> {
            SuspectData existing = this.cache.get(data.getPlayerId());
            if (existing != null) {
                existing.incrementCount();
                existing.updateData(data.getLastProbability(), data.getVl());
                existing.updateScores(data.getRotation(), data.getAimbot(), data.getGcd(), data.getSnap(), data.getSmooth());
                for (int i = data.getCritValues().size() - 1; i >= 0; i--) {
                    double val = data.getCritValues().get(i).doubleValue();
                    existing.getCritValues().add(0, Double.valueOf(val));
                }
                while (existing.getCritValues().size() > 15) {
                    existing.getCritValues().remove(existing.getCritValues().size() - 1);
                }
            } else {
                data.incrementCount();
                this.cache.put(data.getPlayerId(), data);
            }
            this.dirty = true;
            this.sortDirty = true;
        }, this.executor);
    }

    @Override // org.loger.gui.SyncManager
    public CompletableFuture<Void> addSuspectAuto(SuspectData data) {
        return CompletableFuture.runAsync(() -> {
            SuspectData existing = this.cache.get(data.getPlayerId());
            if (existing != null) {
                existing.updateData(data.getLastProbability(), data.getVl());
                existing.updateScores(data.getRotation(), data.getAimbot(), data.getGcd(), data.getSnap(), data.getSmooth());
                for (int i = data.getCritValues().size() - 1; i >= 0; i--) {
                    double val = data.getCritValues().get(i).doubleValue();
                    existing.getCritValues().add(0, Double.valueOf(val));
                }
                while (existing.getCritValues().size() > 15) {
                    existing.getCritValues().remove(existing.getCritValues().size() - 1);
                }
            } else {
                this.cache.put(data.getPlayerId(), data);
            }
            this.dirty = true;
            this.sortDirty = true;
        }, this.executor);
    }

    @Override // org.loger.gui.SyncManager
    public CompletableFuture<Void> removeSuspect(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            this.cache.remove(playerId);
            this.dirty = true;
            this.sortDirty = true;
        }, this.executor);
    }

    @Override // org.loger.gui.SyncManager
    public CompletableFuture<List<SuspectData>> getAllSuspects() {
        return CompletableFuture.supplyAsync(() -> {
            if (!this.sortDirty && this.sortedCache != null) {
                return this.sortedCache;
            }
            List<SuspectData> list = new ArrayList<>(this.cache.values());
            list.sort((a, b) -> {
                if (a.getActionCount() > 0 && b.getActionCount() == 0) {
                    return -1;
                }
                if (a.getActionCount() == 0 && b.getActionCount() > 0) {
                    return 1;
                }
                if (a.getActionCount() > 0 && b.getActionCount() > 0) {
                    return Integer.compare(b.getActionCount(), a.getActionCount());
                }
                return Double.compare(b.getAvg(), a.getAvg());
            });
            this.sortedCache = list;
            this.sortDirty = false;
            return list;
        }, this.executor);
    }

    @Override // org.loger.gui.SyncManager
    public CompletableFuture<SuspectData> getSuspect(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            return this.cache.get(playerId);
        }, this.executor);
    }

    @Override // org.loger.gui.SyncManager
    public CompletableFuture<Void> updateSuspect(SuspectData data) {
        return CompletableFuture.runAsync(() -> {
            this.cache.put(data.getPlayerId(), data);
            this.dirty = true;
            this.sortDirty = true;
        }, this.executor);
    }

    @Override // org.loger.gui.SyncManager
    public void shutdown() {
        this.saveScheduler.shutdown();
        saveData();
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(5L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
    }

    private static class SuspectDataJson {
        List<Double> aimValues;
        int count;
        List<Double> critValues;
        double lastProbability;
        long lastTime;
        String playerId;
        String playerName;
        String server;
        int vl;

        private SuspectDataJson() {
        }

        static SuspectDataJson fromSuspectData(SuspectData data) {
            SuspectDataJson json = new SuspectDataJson();
            json.playerId = data.getPlayerId().toString();
            json.playerName = data.getPlayerName();
            json.count = data.getCount();
            json.lastProbability = data.getLastProbability();
            json.vl = data.getVl();
            json.lastTime = data.getLastTime();
            json.server = data.getServer();
            json.critValues = new ArrayList(data.getCritValues());
            json.aimValues = new ArrayList(data.getAimValues());
            return json;
        }

        SuspectData toSuspectData() {
            SuspectData data = new SuspectData(UUID.fromString(this.playerId), this.playerName);
            data.setActionCount(this.count);
            data.updateData(this.lastProbability, this.vl);
            data.setServer(this.server);
            if (this.critValues != null) {
                data.getCritValues().clear();
                data.getCritValues().addAll(this.critValues);
            }
            if (this.aimValues != null) {
                data.getAimValues().clear();
                data.getAimValues().addAll(this.aimValues);
            }
            return data;
        }
    }
}
