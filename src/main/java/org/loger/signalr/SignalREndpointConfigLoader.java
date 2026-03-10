package org.loger.signalr;

import org.loger.settings.PluginSettings;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.net.HttpHeaders;

public class SignalREndpointConfigLoader {
    private static final String CONFIG_PATH = "/api/config/signalr";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;
    private final Logger logger;

    public SignalREndpointConfigLoader(Logger logger) {
        this.logger = logger;
    }

    public SignalREndpointConfig loadSync(String serverAddress) {
        try {
            String configUrl = buildConfigUrl(serverAddress);
            String json = fetchJson(configUrl);
            SignalREndpointConfig config = SignalREndpointConfig.fromJson(json);
            this.logger.info("[SignalR] Loaded endpoint config: hub=" + config.getHub());
            return config;
        } catch (Exception e) {
            this.logger.log(Level.WARNING, "[SignalR] Failed to load endpoint config, using defaults: " + e.getMessage());
            return SignalREndpointConfig.defaults();
        }
    }

    public CompletableFuture<SignalREndpointConfig> loadAsync(String serverAddress) {
        return CompletableFuture.supplyAsync(() -> {
            return loadSync(serverAddress);
        });
    }

    private String buildConfigUrl(String serverAddress) {
        String baseUrl = normalizeServerAddress(serverAddress);
        return baseUrl + "/api/config/signalr";
    }

    private String normalizeServerAddress(String serverAddress) {
        if (serverAddress == null || serverAddress.isEmpty()) {
            return PluginSettings.DEFAULT_API_ENDPOINT;
        }
        String address = serverAddress.trim();
        if (address.startsWith("http://") || address.startsWith("https://")) {
            return address.endsWith("/") ? address.substring(0, address.length() - 1) : address;
        }
        return "https://" + address;
    }

    private String fetchJson(String urlString) throws Exception {
        int responseCode;
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty(HttpHeaders.ACCEPT, "application/json");
            responseCode = connection.getResponseCode();
        } finally {
        }
        if (responseCode != 200) {
            throw new RuntimeException("HTTP error: " + responseCode);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        try {
            StringBuilder response = new StringBuilder();
            while (true) {
                String line = reader.readLine();
                if (line != null) {
                    response.append(line);
                } else {
                    String string = response.toString();
                    reader.close();
                    return string;
                }
                connection.disconnect();
            }
        } finally {
        }
    }
}
