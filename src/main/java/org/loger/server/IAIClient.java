package org.loger.server;

import java.util.concurrent.CompletableFuture;

public interface IAIClient {
    CompletableFuture<Boolean> connect();

    CompletableFuture<Boolean> connectWithRetry();

    CompletableFuture<Void> disconnect();

    String getServerAddress();

    String getSessionId();

    boolean isConnected();

    boolean isLimitExceeded();

    CompletableFuture<AIResponse> predict(byte[] bArr, String str);
}
