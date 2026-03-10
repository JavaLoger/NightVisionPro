package org.loger.server;

import org.loger.Main;
import org.loger.config.Config;
import org.loger.config.ServerType;
import org.loger.signalr.SignalRClient;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Logger;
import org.bukkit.Bukkit;

public class AIClientProvider {
    private Config config;
    private IAIClient currentClient;
    private final Logger logger;
    private final Main plugin;
    private volatile boolean connecting = false;
    private volatile String clientType = "none";

    public AIClientProvider(Main plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
    }

    public CompletableFuture<Boolean> initialize() {
        if (!this.config.isAiEnabled()) {
            this.plugin.debug("[AI] AI is disabled, skipping client initialization");
            return CompletableFuture.completedFuture(false);
        }
        ServerType serverType = this.config.getServerType();
        String serverAddress = this.config.getServerAddress();
        String apiKey = normalizeApiKey(this.config.getAiApiKey());
        if (serverAddress == null || serverAddress.isEmpty()) {
            this.logger.warning("[AI] Endpoint / server address is not configured!");
            return CompletableFuture.completedFuture(false);
        }
        if (serverType == ServerType.SIGNALR && (apiKey == null || apiKey.isEmpty())) {
            this.logger.warning("[AI] API key is required for SignalR server!");
            return CompletableFuture.completedFuture(false);
        }
        this.logger.info("[AI] Server type: " + serverType + ", endpoint: " + serverAddress);
        this.connecting = true;
        if (serverType == ServerType.HUGGINGFACE) {
            return initializeHuggingFace(serverAddress, apiKey);
        }
        return initializeSignalR(serverAddress, apiKey);
    }

    private CompletableFuture<Boolean> initializeHuggingFace(String modelId, String token) {
        HuggingFaceClient hfClient = new HuggingFaceClient(modelId, token);
        this.currentClient = hfClient;
        this.clientType = "HuggingFace";
        this.logger.info("[HuggingFace] Using model " + modelId + ((token == null || token.isEmpty()) ? " (public)" : " (private)"));
        return hfClient.connect().thenApply(success -> {
            this.connecting = false;
            if (success.booleanValue()) {
                this.logger.info("[HuggingFace] Ready for inference");
            } else {
                this.currentClient = null;
                this.clientType = "none";
            }
            return success;
        }).exceptionally(e -> {
            this.connecting = false;
            this.logger.severe("[HuggingFace] Init error: " + e.getMessage());
            this.currentClient = null;
            this.clientType = "none";
            return false;
        });
    }

    private CompletableFuture<Boolean> initializeSignalR(String serverAddress, String apiKey) {
        SignalRClient signalRClient = new SignalRClient(this.plugin, serverAddress, apiKey, this.config.getReportStatsIntervalSeconds(), () -> {
            return Bukkit.getOnlinePlayers().size();
        }, this.config.isDebug());
        this.currentClient = signalRClient;
        this.clientType = "SignalR";
        this.logger.info("[SignalR] Connecting to " + serverAddress + "...");
        return signalRClient.connectWithRetry().thenApply(success -> {
            this.connecting = false;
            if (success.booleanValue()) {
                this.logger.info("[SignalR] Successfully connected to InferenceServer");
            } else {
                this.logger.warning("[SignalR] Failed to connect to InferenceServer");
                this.currentClient = null;
                this.clientType = "none";
            }
            return success;
        }).exceptionally(e -> {
            this.connecting = false;
            this.logger.severe("[SignalR] Connection error: " + e.getMessage());
            this.currentClient = null;
            this.clientType = "none";
            return false;
        });
    }

    public CompletableFuture<Void> shutdown() {
        if (this.currentClient != null) {
            this.logger.info("[AI] Shutting down " + this.clientType + " client...");
            return this.currentClient.disconnect().thenRun(() -> {
                this.currentClient = null;
                this.clientType = "none";
                this.logger.info("[AI] Client shutdown complete");
            });
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Boolean> reload() {
        return shutdown().thenCompose(v -> {
            return initialize();
        });
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public IAIClient get() {
        return this.currentClient;
    }

    public boolean isAvailable() {
        return this.currentClient != null && this.currentClient.isConnected();
    }

    public boolean isEnabled() {
        return this.config.isAiEnabled();
    }

    public boolean isConnecting() {
        return this.connecting;
    }

    public boolean isLimitExceeded() {
        return this.currentClient != null && this.currentClient.isLimitExceeded();
    }

    public String getClientType() {
        return this.clientType;
    }

    private static String normalizeApiKey(String key) {
        if (key == null) {
            return "";
        }
        String t = key.trim();
        if (t.isEmpty() || "your-api-key".equalsIgnoreCase(t)) {
            return "";
        }
        return t;
    }
}
