package org.loger.signalr;

import org.loger.scheduler.SchedulerManager;
import org.loger.server.AIResponse;
import org.loger.server.IAIClient;
import org.loger.signalr.SignalRSessionManager;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.microsoft.signalr.HubConnectionState;
import org.bukkit.plugin.java.JavaPlugin;

public class SignalRClient implements IAIClient {
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final int MAX_RECONNECT_ATTEMPTS = Integer.MAX_VALUE;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RECONNECT_INTERVAL_MS = 10000;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
    private final String apiKey;
    private final boolean debug;
    private SignalREndpointConfig endpointConfig;
    private SignalRHeartbeatScheduler heartbeatScheduler;
    private final Logger logger;
    private final IntSupplier onlinePlayersSupplier;
    private final JavaPlugin plugin;
    private String pluginHash;
    private final int reportStatsIntervalSeconds;
    private SignalRReportStatsScheduler reportStatsScheduler;
    private final String serverAddress;
    private SignalRSessionManager sessionManager;
    private volatile boolean connected = false;
    private volatile boolean autoReconnectEnabled = true;
    private volatile boolean shuttingDown = false;
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final AtomicInteger autoReconnectAttempts = new AtomicInteger(0);
    private volatile CompletableFuture<Boolean> connectionFuture = null;

    public SignalRClient(JavaPlugin plugin, String serverAddress, String apiKey, int reportStatsIntervalSeconds, IntSupplier onlinePlayersSupplier, boolean debug) {
        this.plugin = plugin;
        this.serverAddress = serverAddress;
        this.apiKey = apiKey;
        this.reportStatsIntervalSeconds = reportStatsIntervalSeconds;
        this.logger = plugin.getLogger();
        this.onlinePlayersSupplier = onlinePlayersSupplier;
        this.debug = debug;
    }

    @Override
    public synchronized CompletableFuture<Boolean> connect() {
        if (this.connectionFuture != null && !this.connectionFuture.isDone()) {
            return this.connectionFuture;
        }
        this.connectionFuture = CompletableFuture.supplyAsync(() -> {
            try {
                try {
                    if (this.pluginHash == null || this.pluginHash.isEmpty()) {
                        this.pluginHash = PluginHashCalculator.calculatePluginHash(this.plugin);
                    }
                    if (this.pluginHash.isEmpty()) {
                        this.logger.warning("[SignalR] Plugin hash calculation failed, using empty hash");
                    }
                    if (this.endpointConfig == null) {
                        SignalREndpointConfigLoader configLoader = new SignalREndpointConfigLoader(this.logger);
                        try {
                            this.endpointConfig = configLoader.loadSync(this.serverAddress);
                        } catch (Exception e) {
                            this.logger.warning("[SignalR] Failed to load endpoint config, using defaults: " + e.getMessage());
                            this.endpointConfig = SignalREndpointConfig.defaults();
                        }
                    }
                    if (this.sessionManager == null) {
                        this.sessionManager = new SignalRSessionManager(this.serverAddress, this.endpointConfig, this.logger, this.debug);
                        this.sessionManager.initialize();
                        this.sessionManager.setOnDisconnectedCallback(this::handleDisconnection);
                    }
                    if (this.sessionManager.getConnectionState() != HubConnectionState.CONNECTED) {
                        this.sessionManager.startConnection().join();
                    }
                    String sessionId = this.sessionManager.createSession(this.apiKey, this.pluginHash).join();
                    if (sessionId == null || sessionId.isEmpty()) {
                        throw new RuntimeException("Failed to create session");
                    }
                    if (this.reportStatsScheduler == null) {
                        this.reportStatsScheduler = new SignalRReportStatsScheduler(this.plugin, this.sessionManager, this.onlinePlayersSupplier);
                        this.reportStatsScheduler.setOnLimitExceededCallback(() -> {
                            this.logger.warning("[SignalR] Online limit exceeded - Predict blocked");
                        });
                        this.reportStatsScheduler.setOnLimitClearedCallback(() -> {
                            this.logger.info("[SignalR] Online limit cleared - Predict enabled");
                        });
                        this.reportStatsScheduler.setOnSessionExpiredCallback(this::handleSessionExpired);
                    }
                    this.reportStatsScheduler.start(this.reportStatsIntervalSeconds);
                    if (this.heartbeatScheduler == null) {
                        this.heartbeatScheduler = new SignalRHeartbeatScheduler(this.plugin, this.sessionManager);
                        this.heartbeatScheduler.setOnSessionExpiredCallback(this::handleSessionExpired);
                    }
                    this.heartbeatScheduler.start();
                    this.connected = true;
                    this.reconnectAttempts.set(0);
                    this.autoReconnectAttempts.set(0);
                    this.logger.info("[SignalR] Connected to " + this.serverAddress);
                    return true;
                } catch (Exception e2) {
                    this.logger.log(Level.SEVERE, "[SignalR] Connection failed: " + e2.getMessage());
                    this.connected = false;
                    return false;
                }
            } catch (SignalRSessionManager.AuthenticationException e3) {
                this.logger.severe("[SignalR] Authentication failed: " + e3.getMessage());
                this.connected = false;
                return false;
            }
        });
        return this.connectionFuture;
    }

    @Override
    public CompletableFuture<Boolean> connectWithRetry() {
        return connectWithRetry(0);
    }

    private CompletableFuture<Boolean> connectWithRetry(int attempt) {
        if (attempt >= 3) {
            this.logger.severe("[SignalR] Max retry attempts reached, giving up");
            return CompletableFuture.completedFuture(false);
        }
        return connect().thenCompose(success -> {
            if (success.booleanValue()) {
                return CompletableFuture.completedFuture(true);
            }
            long backoffMs = calculateBackoff(attempt);
            this.logger.info("[SignalR] Retrying connection in " + backoffMs + "ms (attempt " + (attempt + 1) + "/3)");
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            SchedulerManager.getAdapter().runAsyncDelayed(() -> {
                connectWithRetry(attempt + 1).thenAccept(future::complete);
            }, backoffMs / 50);
            return future;
        });
    }

    public static long calculateBackoff(int attempt) {
        return (1 << attempt) * INITIAL_BACKOFF_MS;
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        this.shuttingDown = true;
        this.autoReconnectEnabled = false;
        return CompletableFuture.runAsync(() -> {
            try {
                if (this.heartbeatScheduler != null) {
                    this.heartbeatScheduler.stop();
                }
                if (this.reportStatsScheduler != null) {
                    this.reportStatsScheduler.stop();
                }
                if (this.sessionManager != null) {
                    try {
                        this.sessionManager.closeSession().get(5L, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        this.logger.warning("[SignalR] Error closing session: " + e.getMessage());
                    }
                }
                this.connected = false;
                this.logger.info("[SignalR] Disconnected from server");
            } catch (Exception e2) {
                this.logger.log(Level.WARNING, "[SignalR] Error during disconnect", (Throwable) e2);
            }
        });
    }

    private void handleDisconnection(Throwable exception) {
        this.connected = false;
        if (this.shuttingDown || !this.autoReconnectEnabled) {
            return;
        }
        if (exception != null) {
            this.logger.warning("[SignalR] Connection lost: " + exception.getMessage());
        } else {
            this.logger.warning("[SignalR] Connection lost unexpectedly");
        }
        if (this.reportStatsScheduler != null) {
            this.reportStatsScheduler.stop();
        }
        if (this.heartbeatScheduler != null) {
            this.heartbeatScheduler.stop();
        }
        scheduleReconnect();
    }

    private void handleSessionExpired() {
        if (this.shuttingDown || !this.autoReconnectEnabled) {
            return;
        }
        this.logger.info("[SignalR] Re-authenticating session...");
        SchedulerManager.getAdapter().runAsync(() -> {
            try {
                String newSessionId = this.sessionManager.createSession(this.apiKey, this.pluginHash).join();
                if (newSessionId != null && !newSessionId.isEmpty()) {
                    this.logger.info("[SignalR] Session re-authenticated successfully");
                } else {
                    this.logger.warning("[SignalR] Session re-authentication failed, scheduling reconnect");
                    scheduleReconnect();
                }
            } catch (Exception e) {
                this.logger.warning("[SignalR] Session re-authentication error: " + e.getMessage());
                scheduleReconnect();
            }
        });
    }

    private void scheduleReconnect() {
        int attempt = this.autoReconnectAttempts.incrementAndGet();
        long delaySeconds = RECONNECT_INTERVAL_MS / INITIAL_BACKOFF_MS;
        this.logger.info("[SignalR] Scheduling reconnect attempt " + attempt + " in " + delaySeconds + " seconds...");
        long delayTicks = RECONNECT_INTERVAL_MS / 50;
        SchedulerManager.getAdapter().runAsyncDelayed(this::attemptReconnect, delayTicks);
    }

    private void attemptReconnect() {
        if (this.shuttingDown || !this.autoReconnectEnabled) {
            return;
        }
        if (this.connected) {
            this.logger.info("[SignalR] Already connected, skipping reconnect");
            this.autoReconnectAttempts.set(0);
        } else {
            int attempt = this.autoReconnectAttempts.get();
            this.logger.info("[SignalR] Attempting reconnect (" + attempt + "/2147483647)...");
            connect().thenAccept(success -> {
                if (success.booleanValue()) {
                    this.logger.info("[SignalR] Reconnected successfully after " + attempt + " attempt(s)");
                    this.autoReconnectAttempts.set(0);
                } else {
                    this.logger.warning("[SignalR] Reconnect attempt " + attempt + " failed");
                    scheduleReconnect();
                }
            }).exceptionally(ex -> {
                this.logger.warning("[SignalR] Reconnect attempt " + attempt + " failed: " + ex.getMessage());
                scheduleReconnect();
                return null;
            });
        }
    }

    private long calculateReconnectDelay(int attempt) {
        return RECONNECT_INTERVAL_MS;
    }

    public void setAutoReconnectEnabled(boolean enabled) {
        this.autoReconnectEnabled = enabled;
    }

    public boolean isAutoReconnectEnabled() {
        return this.autoReconnectEnabled;
    }

    public CompletableFuture<Boolean> reconnect() {
        this.autoReconnectAttempts.set(0);
        return disconnect().thenCompose(v -> {
            this.shuttingDown = false;
            this.autoReconnectEnabled = true;
            return connect();
        });
    }

    @Override
    public CompletableFuture<AIResponse> predict(byte[] playerData, String playerUuid) {
        if (!isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not connected to SignalR server"));
        }
        if (this.reportStatsScheduler != null && this.reportStatsScheduler.isLimitExceeded()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Online limit exceeded, Predict blocked"));
        }
        return this.sessionManager.predict(playerData, playerUuid).thenCompose(result -> {
            if (result.isSuccess()) {
                return CompletableFuture.completedFuture(new AIResponse(result.getProbability()));
            }
            String errorCode = result.getErrorCode();
            if (HubErrorParser.requiresReportStats(errorCode)) {
                this.logger.warning("[SignalR] Predict failed: " + errorCode + ", calling ReportStats...");
                return handleStatsRequiredAndRetry(playerData, playerUuid);
            }
            if (HubErrorParser.LIMIT_EXCEEDED.equals(errorCode)) {
                this.logger.warning("[SignalR] Predict failed: Online limit exceeded");
                if (this.reportStatsScheduler != null) {
                    this.reportStatsScheduler.setLimitExceeded(true);
                }
                return CompletableFuture.failedFuture(new RuntimeException("Online limit exceeded"));
            }
            if (HubErrorParser.NOT_AUTHENTICATED.equals(errorCode)) {
                this.logger.warning("[SignalR] Session expired during prediction, attempting reconnect");
                return handleUnauthenticatedAndRetry(playerData, playerUuid);
            }
            return CompletableFuture.failedFuture(new RuntimeException(errorCode + ": " + result.getErrorMessage()));
        });
    }

    private CompletableFuture<AIResponse> handleStatsRequiredAndRetry(byte[] playerData, String playerUuid) {
        if (this.reportStatsScheduler == null) {
            return CompletableFuture.failedFuture(new RuntimeException("ReportStats scheduler not initialized"));
        }
        return this.reportStatsScheduler.reportNow().thenCompose(statsResult -> {
            if (!statsResult.isSuccess()) {
                return CompletableFuture.failedFuture(new RuntimeException("ReportStats failed: " + statsResult.getError()));
            }
            if (statsResult.isLimitExceeded()) {
                return CompletableFuture.failedFuture(new RuntimeException("Online limit exceeded after ReportStats"));
            }
            return this.sessionManager.predict(playerData, playerUuid).thenApply(result -> {
                if (result.isSuccess()) {
                    return new AIResponse(result.getProbability());
                }
                throw new RuntimeException(result.getErrorCode() + ": " + result.getErrorMessage());
            });
        });
    }

    private CompletableFuture<AIResponse> handleUnauthenticatedAndRetry(byte[] playerData, String playerUuid) {
        return this.sessionManager.createSession(this.apiKey, this.pluginHash).thenCompose(sessionId -> {
            return this.sessionManager.predict(playerData, playerUuid).thenApply(result -> {
                if (result.isSuccess()) {
                    return new AIResponse(result.getProbability());
                }
                throw new RuntimeException(result.getErrorCode() + ": " + result.getErrorMessage());
            });
        });
    }

    @Override
    public boolean isConnected() {
        return this.connected && this.sessionManager != null && this.sessionManager.isSessionValid();
    }

    @Override
    public boolean isLimitExceeded() {
        return this.reportStatsScheduler != null && this.reportStatsScheduler.isLimitExceeded();
    }

    @Override
    public String getSessionId() {
        if (this.sessionManager != null) {
            return this.sessionManager.getSessionId();
        }
        return null;
    }

    @Override
    public String getServerAddress() {
        return this.serverAddress;
    }

    public SignalREndpointConfig getEndpointConfig() {
        return this.endpointConfig;
    }
}
