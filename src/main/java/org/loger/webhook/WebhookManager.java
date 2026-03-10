package org.loger.webhook;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.google.common.net.HttpHeaders;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class WebhookManager {
    private String discordEmbedColor;
    private String discordEmbedTitle;
    private boolean discordEnabled;
    private boolean discordTimestamp;
    private String discordWebhookUrl;
    private boolean enabled;
    private double minProbability;
    private final JavaPlugin plugin;
    private String serverName;
    private List<String> violationContent;
    private FileConfiguration webhooksConfig;
    private File webhooksFile;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy");

    public WebhookManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        this.webhooksFile = new File(this.plugin.getDataFolder(), "webhooks.yml");
        if (!this.webhooksFile.exists()) {
            this.plugin.saveResource("webhooks.yml", false);
        }
        this.webhooksConfig = YamlConfiguration.loadConfiguration(this.webhooksFile);
        this.enabled = this.webhooksConfig.getBoolean("enabled", false);
        this.serverName = this.webhooksConfig.getString("server-name", HttpHeaders.SERVER);
        this.minProbability = this.webhooksConfig.getDouble("min-probability", 0.75d);
        this.discordEnabled = this.webhooksConfig.getBoolean("enabled", false);
        this.discordWebhookUrl = this.webhooksConfig.getString("url", "");
        this.discordEmbedTitle = this.webhooksConfig.getString("embed-title", "**Новое нарушение**");
        this.discordEmbedColor = this.webhooksConfig.getString("embed-color", "#00FFFF");
        this.discordTimestamp = this.webhooksConfig.getBoolean("timestamp", true);
        this.violationContent = this.webhooksConfig.getStringList("violation-content");
    }

    public void reload() {
        loadConfig();
    }

    public void shutdown() {
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(5L, TimeUnit.SECONDS)) {
                this.executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
        }
    }

    public void sendAlert(UUID playerId, String playerName, double probability, int vl, double avg) {
        if (!this.enabled || probability < this.minProbability || !isDiscordEnabled()) {
            return;
        }
        String time = this.dateFormat.format(new Date());
        if (this.discordEnabled && !this.discordWebhookUrl.isEmpty()) {
            this.executor.submit(() -> {
                sendDiscordWebhook(playerName, probability, vl, avg, time);
            });
        }
    }

    private void sendDiscordWebhook(String player, double probability, int vl, double avg, String time) {
        try {
            URL url = new URL(this.discordWebhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            String json = buildDiscordJson(player, probability, vl, avg, time);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200 && responseCode != 204) {
                this.plugin.getLogger().warning("[Webhook] Discord returned code " + responseCode);
            }
            conn.disconnect();
        } catch (Exception e) {
            this.plugin.getLogger().warning("[Webhook] Discord error: " + e.getMessage());
        }
    }

    private String buildDiscordJson(String player, double probability, int vl, double avg, String time) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"embeds\":[{");
        json.append("\"title\":\"").append(escapeJson(replacePlaceholders(this.discordEmbedTitle, player, probability, vl, avg, time))).append("\",");
        if (!this.violationContent.isEmpty()) {
            StringBuilder description = new StringBuilder();
            for (int i = 0; i < this.violationContent.size(); i++) {
                String line = replacePlaceholders(this.violationContent.get(i), player, probability, vl, avg, time);
                description.append(line);
                if (i < this.violationContent.size() - 1) {
                    description.append("\n");
                }
            }
            json.append("\"description\":\"").append(escapeJson(description.toString())).append("\",");
        }
        try {
            int color = Integer.parseInt(this.discordEmbedColor.replace("#", ""), 16);
            json.append("\"color\":").append(color).append(",");
        } catch (NumberFormatException e) {
            json.append("\"color\":65535,");
        }
        if (this.discordTimestamp) {
            json.append("\"timestamp\":\"").append(Instant.now().toString()).append("\",");
        }
        if (json.charAt(json.length() - 1) == ',') {
            json.deleteCharAt(json.length() - 1);
        }
        json.append("}]}");
        return json.toString();
    }

    private String replacePlaceholders(String text, String player, double probability, int vl, double avg, String time) {
        String probPercent = String.valueOf((int) (100.0d * probability));
        return text.replace("{PLAYER}", player).replace("{PROBABILITY}", probPercent).replace("{VL}", String.valueOf(vl)).replace("{AVG}", String.format("%.2f", Double.valueOf(avg))).replace("{SERVER}", this.serverName).replace("{TIME}", time);
    }

    private String escapeJson(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isDiscordEnabled() {
        return this.discordEnabled && !this.discordWebhookUrl.isEmpty();
    }
}
