package org.loger.signalr;

import org.loger.config.Config;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class SignalREndpointConfig {
    private static final String DEFAULT_HUB = "/v1/beta";
    private static final Map<String, String> DEFAULT_METHODS = Map.of("connect", "Connect", "heartbeat", "Heartbeat", "reportStats", "ReportStats", "predict", "Predict");
    private static final String DEFAULT_TRANSPORT = "WebSockets";
    private final String hub;
    private final Map<String, String> methods;
    private final String transport;

    private SignalREndpointConfig(String hub, String transport, Map<String, String> methods) {
        this.hub = hub != null ? hub : DEFAULT_HUB;
        this.transport = transport != null ? transport : DEFAULT_TRANSPORT;
        this.methods = methods != null ? methods : new HashMap<>(DEFAULT_METHODS);
    }

    public static SignalREndpointConfig defaults() {
        return new SignalREndpointConfig(DEFAULT_HUB, DEFAULT_TRANSPORT, new HashMap(DEFAULT_METHODS));
    }

    public static SignalREndpointConfig fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return defaults();
        }
        try {
            Gson gson = new Gson();
            JsonObject root = (JsonObject) gson.fromJson(json, JsonObject.class);
            String hub = root.has("hub") ? root.get("hub").getAsString() : DEFAULT_HUB;
            String transport = root.has("transport") ? root.get("transport").getAsString() : DEFAULT_TRANSPORT;
            Map<String, String> methods = new HashMap<>(DEFAULT_METHODS);
            if (root.has("methods") && root.get("methods").isJsonObject()) {
                JsonObject methodsObj = root.getAsJsonObject("methods");
                for (String key : methodsObj.entrySet().stream().map(e -> {
                    return (String) e.getKey();
                }).toList()) {
                    methods.put(key, methodsObj.get(key).getAsString());
                }
            }
            return new SignalREndpointConfig(hub, transport, methods);
        } catch (JsonSyntaxException e2) {
            return defaults();
        }
    }

    public String toJson() {
        Gson gson = new Gson();
        JsonObject root = new JsonObject();
        root.addProperty("hub", this.hub);
        root.addProperty("transport", this.transport);
        JsonObject methodsObj = new JsonObject();
        for (Map.Entry<String, String> entry : this.methods.entrySet()) {
            methodsObj.addProperty(entry.getKey(), entry.getValue());
        }
        root.add("methods", methodsObj);
        return gson.toJson((JsonElement) root);
    }

    public String getHubUrl(String serverAddress) {
        String baseUrl = normalizeServerAddress(serverAddress);
        return baseUrl + this.hub;
    }

    public String getMethodName(String methodKey) {
        return this.methods.getOrDefault(methodKey, DEFAULT_METHODS.get(methodKey));
    }

    private String normalizeServerAddress(String serverAddress) {
        if (serverAddress == null || serverAddress.isEmpty()) {
            return Config.DEFAULT_SERVER_ADDRESS;
        }
        String address = serverAddress.trim();
        if (address.startsWith("http://") || address.startsWith("https://")) {
            return address.endsWith("/") ? address.substring(0, address.length() - 1) : address;
        }
        return "https://" + address;
    }

    public String getHub() {
        return this.hub;
    }

    public String getTransport() {
        return this.transport;
    }

    public Map<String, String> getMethods() {
        return new HashMap(this.methods);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SignalREndpointConfig other = (SignalREndpointConfig) obj;
        if (this.hub.equals(other.hub) && this.transport.equals(other.transport) && this.methods.equals(other.methods)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = this.hub.hashCode();
        return (((result * 31) + this.transport.hashCode()) * 31) + this.methods.hashCode();
    }

    public String toString() {
        return "SignalREndpointConfig{hub='" + this.hub + "', transport='" + this.transport + "', methods=" + this.methods + "}";
    }
}
