package org.loger.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import okhttp3.HttpUrl;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class MessageManager {
    public static final String DEFAULT_PREFIX = "&7[&x&F&B&0&8&0&8&lNEO&f&lNVP&7] &f";
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private final JavaPlugin plugin;
    private String prefix;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }
        this.messagesFile = new File(this.plugin.getDataFolder(), "messages.yml");
        if (!this.messagesFile.exists()) {
            this.plugin.saveResource("messages.yml", false);
            this.plugin.getLogger().info("[Messages] Created messages.yml");
        }
        this.messagesConfig = YamlConfiguration.loadConfiguration(this.messagesFile);
        InputStream defaultStream = this.plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            this.messagesConfig.setDefaults(defaultConfig);
            this.messagesConfig.options().copyDefaults(true);
            try {
                this.messagesConfig.save(this.messagesFile);
            } catch (IOException e) {
                this.plugin.getLogger().warning("[Messages] Could not save defaults: " + e.getMessage());
            }
        }
        this.prefix = this.messagesConfig.getString("prefix", DEFAULT_PREFIX);
        this.plugin.getLogger().info("[Messages] Loaded " + this.messagesConfig.getKeys(false).size() + " messages");
    }

    public void reload() {
        loadMessages();
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getMessage(String key) {
        return this.messagesConfig.getString(key, "");
    }

    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public String getMessage(String key, String player, double probability, double buffer, int vl) {
        String msg = getMessage(key);
        String playerValue = player != null ? player : "";
        String probValue = String.valueOf((int) (100.0d * probability));
        String bufferValue = String.format("%.1f", Double.valueOf(buffer));
        String vlValue = String.valueOf(vl);
        return msg.replace("%player%", playerValue).replace("{PLAYER}", playerValue).replace("%probability%", probValue).replace("{PROBABILITY}", probValue).replace("%buffer%", bufferValue).replace("{BUFFER}", bufferValue).replace("%vl%", vlValue).replace("{VL}", vlValue);
    }

    public boolean isMessageEmpty(String key) {
        String msg = getMessage(key);
        return msg == null || msg.isEmpty() || msg.equals("\"\"") || msg.equals(HttpUrl.PATH_SEGMENT_ENCODE_SET_URI);
    }

    public void saveMessages() {
        try {
            this.messagesConfig.save(this.messagesFile);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not save messages.yml: " + e.getMessage());
        }
    }
}
