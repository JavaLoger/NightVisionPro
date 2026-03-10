package org.loger.signalr;

import org.loger.signalr.HubErrorParser;
import org.loger.signalr.dto.ConnectRequest;
import org.loger.signalr.dto.ConnectResponse;
import org.loger.signalr.dto.HeartbeatResponse;
import org.loger.signalr.dto.PredictRequest;
import org.loger.signalr.dto.PredictResponse;
import org.loger.signalr.dto.ReportStatsRequest;
import org.loger.signalr.dto.ReportStatsResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gson.Gson;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;
import com.microsoft.signalr.TransportEnum;
import com.microsoft.signalr.messagepack.MessagePackHubProtocol;

public class SignalRSessionManager {
    private static final long KEEP_ALIVE_INTERVAL_MS = 15000;
    private static final long SERVER_TIMEOUT_MS = 60000;
    private final boolean debug;
    private final SignalREndpointConfig endpointConfig;
    private HubConnection hubConnection;
    private final String hubUrl;
    private volatile long lastServerTime;
    private final Logger logger;
    private Consumer<Throwable> onDisconnectedCallback;
    private volatile String sessionId;
    private volatile boolean sessionValid = false;

    public SignalRSessionManager(String serverAddress, SignalREndpointConfig endpointConfig, Logger logger, boolean debug) {
        this.logger = logger;
        this.endpointConfig = endpointConfig;
        this.hubUrl = endpointConfig.getHubUrl(serverAddress);
        this.debug = debug;
    }

    public void setOnDisconnectedCallback(Consumer<Throwable> callback) {
        this.onDisconnectedCallback = callback;
    }

    public void initialize() {
        this.hubConnection = HubConnectionBuilder.create(this.hubUrl).setHttpClientBuilderCallback(builder -> {
            builder.addInterceptor(new SignalRNegotiateInterceptor(this.logger, this.debug));
        }).withTransport(TransportEnum.WEBSOCKETS).withHubProtocol(new MessagePackHubProtocol()).withServerTimeout(SERVER_TIMEOUT_MS).withKeepAliveInterval(KEEP_ALIVE_INTERVAL_MS).build();
        this.hubConnection.onClosed(exception -> {
            this.sessionValid = false;
            if (exception != null) {
                this.logger.warning("[SignalR] Connection closed with error: " + exception.getMessage());
            } else {
                this.logger.info("[SignalR] Connection closed");
            }
            if (this.onDisconnectedCallback != null) {
                this.onDisconnectedCallback.accept(exception);
            }
        });
    }

    public CompletableFuture<Void> startConnection() {
        if (this.hubConnection == null) {
            initialize();
        }
        return CompletableFuture.runAsync(() -> {
            try {
                this.hubConnection.start().blockingAwait();
                this.logger.info("[SignalR] WebSocket connection established to " + this.hubUrl);
            } catch (Exception e) {
                throw new RuntimeException("Failed to start SignalR connection: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<String> createSession(String apiKey, String pluginHash) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String methodName = this.endpointConfig.getMethodName("connect");
                if (this.debug) {
                    this.logger.info("[SignalR] Calling " + methodName + " with api_key=" + apiKey.substring(0, Math.min(8, apiKey.length())) + "..., plugin_hash=" + pluginHash);
                }
                ConnectRequest request = new ConnectRequest(apiKey, pluginHash);
                ConnectResponse response = (ConnectResponse) this.hubConnection.invoke(ConnectResponse.class, methodName, request).blockingGet();
                if (response == null || response.sessionId == null || response.sessionId.isEmpty()) {
                    throw new SessionException("Empty session ID received");
                }
                this.sessionId = response.sessionId;
                this.lastServerTime = response.serverTime;
                this.sessionValid = true;
                this.logger.info("[SignalR] Session created: " + this.sessionId.substring(0, Math.min(8, this.sessionId.length())) + "...");
                return this.sessionId;
            } catch (Exception e) {
                this.sessionValid = false;
                String errorMsg = e.getMessage();
                HubErrorParser.HubError hubError = HubErrorParser.parse(errorMsg);
                if (HubErrorParser.AUTH_FAILED.equals(hubError.getCode())) {
                    this.logger.severe("[SignalR] Authentication failed: " + hubError.getMessage());
                    throw new AuthenticationException(hubError.getMessage());
                }
                this.logger.log(Level.SEVERE, "[SignalR] Session creation failed: " + errorMsg, (Throwable) e);
                throw new SessionException("Session creation failed: " + errorMsg, e);
            }
        });
    }

    public CompletableFuture<HeartbeatResult> sendHeartbeat() {
        if (!isSessionValid()) {
            return CompletableFuture.completedFuture(new HeartbeatResult(false, 0L, "No active session"));
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                String methodName = this.endpointConfig.getMethodName("heartbeat");
                HeartbeatResponse response = (HeartbeatResponse) this.hubConnection.invoke(HeartbeatResponse.class, methodName, new Object[0]).blockingGet();
                long serverTime = response != null ? response.serverTime : 0L;
                this.lastServerTime = serverTime;
                return new HeartbeatResult(true, serverTime, null);
            } catch (Exception e) {
                HubErrorParser.HubError hubError = HubErrorParser.parse(e.getMessage());
                if (HubErrorParser.NOT_AUTHENTICATED.equals(hubError.getCode())) {
                    this.sessionValid = false;
                    return new HeartbeatResult(false, 0L, "Session expired or invalid");
                }
                return new HeartbeatResult(false, 0L, "Heartbeat error: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<ReportStatsResult> reportStats(int onlinePlayers) {
        if (!isSessionValid()) {
            this.logger.warning("[SignalR] Cannot call ReportStats - no active session");
            return CompletableFuture.completedFuture(new ReportStatsResult(false, false, 0, "No active session"));
        }
        HubConnectionState state = this.hubConnection.getConnectionState();
        if (state != HubConnectionState.CONNECTED) {
            this.logger.severe("[SignalR] Cannot call ReportStats - not connected! State: " + state);
            return CompletableFuture.completedFuture(new ReportStatsResult(false, false, 0, "Not connected, state: " + state));
        }
        return CompletableFuture.supplyAsync(() -> {
            boolean limitExceeded;
            try {
                String methodName = this.endpointConfig.getMethodName("reportStats");
                ReportStatsRequest request = new ReportStatsRequest(onlinePlayers);
                this.logger.info("[SignalR] Connection state OK, calling ReportStats...");
                this.logger.info("[SignalR] Preparing to call " + methodName + " with onlinePlayers=" + onlinePlayers);
                if (this.debug) {
                    try {
                        Gson gson = new Gson();
                        String json = gson.toJson(request);
                        this.logger.info("[SignalR] ReportStats request JSON: " + json);
                    } catch (Exception jsonEx) {
                        this.logger.warning("[SignalR] Failed to serialize request to JSON: " + jsonEx.getMessage());
                    }
                }
                this.logger.info("[SignalR] ReportStats invoked, waiting for response...");
                try {
                    ReportStatsResponse response = (ReportStatsResponse) this.hubConnection.invoke(ReportStatsResponse.class, methodName, request).timeout(5L, TimeUnit.SECONDS).toFuture().get(5L, TimeUnit.SECONDS);
                    this.logger.info("[SignalR] ReportStats completed successfully!");
                    if (response == null || !response.limitExceeded) {
                        limitExceeded = false;
                    } else {
                        limitExceeded = true;
                    }
                    int maxOnline = response != null ? response.maxOnline : 0;
                    if (this.debug) {
                        this.logger.info("[SignalR] ReportStats response: limitExceeded=" + limitExceeded + ", maxOnline=" + maxOnline);
                    }
                    return new ReportStatsResult(true, limitExceeded, maxOnline, null);
                } catch (ExecutionException execEx) {
                    Throwable cause = execEx.getCause();
                    if (cause != null) {
                        throw cause;
                    }
                    throw execEx;
                } catch (TimeoutException timeoutEx) {
                    this.logger.severe("[SignalR] ReportStats timeout after 5 seconds");
                    this.logger.severe("[SignalR] Exception type: " + timeoutEx.getClass().getName());
                    this.logger.severe("[SignalR] Exception message: " + timeoutEx.getMessage());
                    return new ReportStatsResult(false, false, 0, "Timeout after 5 seconds");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                this.logger.severe("[SignalR] ReportStats interrupted");
                this.logger.severe("[SignalR] Exception type: " + e.getClass().getName());
                return new ReportStatsResult(false, false, 0, "Interrupted");
            } catch (Throwable e2) {
                this.logger.severe("[SignalR] ReportStats failed with exception");
                this.logger.severe("[SignalR] Exception type: " + e2.getClass().getName());
                this.logger.severe("[SignalR] Exception message: " + e2.getMessage());
                HubErrorParser.HubError hubError = HubErrorParser.parse(e2.getMessage());
                this.logger.severe("[SignalR] Parsed error code: " + hubError.getCode());
                this.logger.severe("[SignalR] Parsed error message: " + hubError.getMessage());
                if (HubErrorParser.NOT_AUTHENTICATED.equals(hubError.getCode())) {
                    this.sessionValid = false;
                    this.logger.warning("[SignalR] Session invalidated due to NOT_AUTHENTICATED error");
                }
                return new ReportStatsResult(false, false, 0, hubError.getMessage());
            }
        });
    }

    public CompletableFuture<PredictResult> predict(byte[] playerData, String playerUuid) {
        if (!isSessionValid()) {
            return CompletableFuture.completedFuture(new PredictResult(false, 0.0f, 0L, HubErrorParser.NOT_AUTHENTICATED, "No active session"));
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                String methodName = this.endpointConfig.getMethodName("predict");
                PredictRequest request = new PredictRequest(playerData, playerUuid);
                PredictResponse response = (PredictResponse) this.hubConnection.invoke(PredictResponse.class, methodName, request).blockingGet();
                if (response == null) {
                    return new PredictResult(false, 0.0f, 0L, "INVALID_RESPONSE", "Null response from server");
                }
                if (!Float.isNaN(response.probability) && !Float.isInfinite(response.probability)) {
                    float probability = Math.max(0.0f, Math.min(1.0f, response.probability));
                    return new PredictResult(true, probability, response.inferenceTimeMs, null, null);
                }
                return new PredictResult(false, 0.0f, 0L, HubErrorParser.INVALID_DATA, "Server returned invalid probability: " + response.probability);
            } catch (Exception e) {
                HubErrorParser.HubError hubError = HubErrorParser.parse(e.getMessage());
                String errorCode = hubError.getCode();
                if (HubErrorParser.NOT_AUTHENTICATED.equals(errorCode)) {
                    this.sessionValid = false;
                }
                return new PredictResult(false, 0.0f, 0L, errorCode, hubError.getMessage());
            }
        });
    }

    public CompletableFuture<Void> closeSession() {
        return CompletableFuture.runAsync(() -> {
            try {
                try {
                    if (this.hubConnection != null && this.hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
                        this.hubConnection.stop().blockingAwait();
                    }
                } catch (Exception e) {
                    this.logger.warning("[SignalR] Error closing connection: " + e.getMessage());
                }
            } finally {
                this.sessionId = null;
                this.sessionValid = false;
            }
        });
    }

    public boolean isSessionValid() {
        return this.sessionValid && this.sessionId != null && this.hubConnection != null && this.hubConnection.getConnectionState() == HubConnectionState.CONNECTED;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public long getLastServerTime() {
        return this.lastServerTime;
    }

    public HubConnectionState getConnectionState() {
        return this.hubConnection != null ? this.hubConnection.getConnectionState() : HubConnectionState.DISCONNECTED;
    }

    public void invalidateSession() {
        this.sessionValid = false;
    }

    public static class HeartbeatResult {
        private final String error;
        private final long serverTime;
        private final boolean success;

        public HeartbeatResult(boolean success, long serverTime, String error) {
            this.success = success;
            this.serverTime = serverTime;
            this.error = error;
        }

        public boolean isSuccess() {
            return this.success;
        }

        public long getServerTime() {
            return this.serverTime;
        }

        public String getError() {
            return this.error;
        }
    }

    public static class ReportStatsResult {
        private final String error;
        private final boolean limitExceeded;
        private final int maxOnline;
        private final boolean success;

        public ReportStatsResult(boolean success, boolean limitExceeded, int maxOnline, String error) {
            this.success = success;
            this.limitExceeded = limitExceeded;
            this.maxOnline = maxOnline;
            this.error = error;
        }

        public boolean isSuccess() {
            return this.success;
        }

        public boolean isLimitExceeded() {
            return this.limitExceeded;
        }

        public int getMaxOnline() {
            return this.maxOnline;
        }

        public String getError() {
            return this.error;
        }
    }

    public static class PredictResult {
        private final String errorCode;
        private final String errorMessage;
        private final long inferenceTimeMs;
        private final float probability;
        private final boolean success;

        public PredictResult(boolean success, float probability, long inferenceTimeMs, String errorCode, String errorMessage) {
            this.success = success;
            this.probability = probability;
            this.inferenceTimeMs = inferenceTimeMs;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return this.success;
        }

        public float getProbability() {
            return this.probability;
        }

        public long getInferenceTimeMs() {
            return this.inferenceTimeMs;
        }

        public String getErrorCode() {
            return this.errorCode;
        }

        public String getErrorMessage() {
            return this.errorMessage;
        }
    }

    public static class SessionException extends RuntimeException {
        public SessionException(String message) {
            super(message);
        }

        public SessionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class AuthenticationException extends SessionException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}
