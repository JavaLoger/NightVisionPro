package org.loger.server;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.common.net.HttpHeaders;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HuggingFaceClient implements IAIClient {
    private static final String INFERENCE_BASE = "https://api-inference.huggingface.co/models/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int TIMEOUT_SECONDS = 30;
    private final String baseUrl;
    private volatile boolean connected = true;
    private final Gson gson;
    private final OkHttpClient httpClient;
    private final String modelId;
    private final String token;

    public HuggingFaceClient(String modelId, String token) {
        String id = modelId == null ? "" : modelId.trim();
        this.modelId = id;
        this.baseUrl = (id.startsWith("http://") || id.startsWith("https://")) ? id : "https://api-inference.huggingface.co/models/" + id;
        this.token = (token == null || token.isEmpty()) ? null : token.trim();
        this.httpClient = new OkHttpClient.Builder().connectTimeout(30L, TimeUnit.SECONDS).readTimeout(30L, TimeUnit.SECONDS).writeTimeout(30L, TimeUnit.SECONDS).build();
        this.gson = new Gson();
    }

    @Override // org.loger.server.IAIClient
    public CompletableFuture<Boolean> connect() {
        this.connected = !this.modelId.isEmpty();
        return CompletableFuture.completedFuture(Boolean.valueOf(this.connected));
    }

    @Override // org.loger.server.IAIClient
    public CompletableFuture<Boolean> connectWithRetry() {
        return connect();
    }

    @Override // org.loger.server.IAIClient
    public CompletableFuture<Void> disconnect() {
        this.connected = false;
        return CompletableFuture.completedFuture(null);
    }

    @Override // org.loger.server.IAIClient
    public CompletableFuture<AIResponse> predict(byte[] playerData, String playerUuid) {
        if (!this.connected || this.modelId.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("HuggingFace client not configured or disconnected"));
        }
        String base64Data = Base64.getEncoder().encodeToString(playerData);
        JsonObject body = new JsonObject();
        JsonObject inputs = new JsonObject();
        inputs.addProperty("data", base64Data);
        inputs.addProperty("player_id", playerUuid != null ? playerUuid : "unknown");
        body.add("inputs", inputs);
        Request.Builder reqBuilder = new Request.Builder().url(this.baseUrl).post(RequestBody.create(body.toString(), JSON));
        if (this.token != null) {
            reqBuilder.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.token);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                Response response = this.httpClient.newCall(reqBuilder.build()).execute();
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    return new AIResponse(0.0d, "HTTP " + response.code() + ": " + responseBody);
                }
                System.out.println("[NightVisionPro DEBUG] Raw HF response: " + responseBody);
                return parseResponse(responseBody);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        });
    }

    private AIResponse parseResponse(String json) {
        try {
            JsonElement root = (JsonElement) this.gson.fromJson(json, JsonElement.class);
            if (root == null) {
                return new AIResponse(0.0d);
            }
            JsonObject obj = null;
            if (root.isJsonArray()) {
                JsonArray arr = root.getAsJsonArray();
                if (arr.size() > 0 && arr.get(0).isJsonObject()) {
                    obj = arr.get(0).getAsJsonObject();
                }
            } else if (root.isJsonObject()) {
                obj = root.getAsJsonObject();
            }
            if (obj == null) {
                return new AIResponse(0.0d);
            }
            if (obj.has("error")) {
                return new AIResponse(0.0d, obj.get("error").getAsString());
            }
            double probability = getDouble(obj, "probability");
            double rotation = getDouble(obj, "rotation");
            double aimbot = getDouble(obj, "aimbot");
            double gcd = getDouble(obj, "gcd");
            double snap = getDouble(obj, "snap");
            double smooth = getDouble(obj, "smooth");
            return new AIResponse(probability, rotation, aimbot, gcd, snap, smooth, null);
        } catch (Exception e) {
            return new AIResponse(0.0d);
        }
    }

    private double getDouble(JsonObject obj, String key) {
        if (obj.has(key)) {
            try {
                return obj.get(key).getAsDouble();
            } catch (Exception e) {
                return 0.0d;
            }
        }
        return 0.0d;
    }

    @Override // org.loger.server.IAIClient
    public boolean isConnected() {
        return this.connected && !this.modelId.isEmpty();
    }

    @Override // org.loger.server.IAIClient
    public boolean isLimitExceeded() {
        return false;
    }

    @Override // org.loger.server.IAIClient
    public String getSessionId() {
        return "hf-" + this.modelId;
    }

    @Override // org.loger.server.IAIClient
    public String getServerAddress() {
        return this.baseUrl;
    }
}
