package org.loger.gui;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SyncManager {
    CompletableFuture<Void> addSuspect(SuspectData suspectData);

    CompletableFuture<Void> addSuspectAuto(SuspectData suspectData);

    CompletableFuture<List<SuspectData>> getAllSuspects();

    CompletableFuture<SuspectData> getSuspect(UUID uuid);

    CompletableFuture<Void> removeSuspect(UUID uuid);

    void shutdown();

    CompletableFuture<Void> updateSuspect(SuspectData suspectData);
}
