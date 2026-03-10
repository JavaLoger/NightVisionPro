package org.loger.settings;

import org.loger.config.Config;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginSettings {
    public static final double DEFAULT_ALERT_THRESHOLD = 0.75d;
    public static final String DEFAULT_API_ENDPOINT = "https://api.mlsac.wtf";
    public static final String DEFAULT_API_KEY = "";
    public static final boolean DEFAULT_CONSOLE_ALERTS = true;
    public static final String DEFAULT_CUSTOM_ALERT_PREFIX = "&6[NVP] &f";
    public static final String DEFAULT_DATA_DIR = "plugins/NightVisionPro/data";
    public static final boolean DEFAULT_DEBUG = false;
    public static final boolean DEFAULT_DETECTION_ENABLED = false;
    public static final String DEFAULT_MESSAGE_PREFIX = "&6[NVP] &r";
    public static final double DEFAULT_PENALTY_MIN_PROB = 0.85d;
    public static final int DEFAULT_SAMPLE_INTERVAL = 10;
    public static final int DEFAULT_SAMPLE_SIZE = 40;
    public static final double DEFAULT_VL_DECAY = 0.5d;
    public static final double DEFAULT_VL_MULTIPLIER = 50.0d;
    public static final double DEFAULT_VL_RESET = 1.0d;
    public static final double DEFAULT_VL_THRESHOLD = 3.0d;
    private final double alertThreshold;
    private final String apiEndpoint;
    private final String apiKey;
    private final String autostartComment;
    private final boolean autostartEnabled;
    private final String autostartLabel;
    private final boolean consoleAlerts;
    private final String customAlertPrefix;
    private final String dataDirectory;
    private final boolean debug;
    private final boolean detectionEnabled;
    private final String messagePrefix;
    private final Map<String, String> messages;
    private final Map<Integer, String> penaltyCommands;
    private final double penaltyMinProbability;
    private final int sampleInterval;
    private final int sampleSize;
    private final double vlDecay;
    private final double vlMultiplier;
    private final double vlResetValue;
    private final double vlThreshold;

    public PluginSettings() {
        this.debug = false;
        this.dataDirectory = "plugins/NightVisionPro/data";
        this.detectionEnabled = false;
        this.apiEndpoint = DEFAULT_API_ENDPOINT;
        this.apiKey = "";
        this.sampleSize = 40;
        this.sampleInterval = 10;
        this.alertThreshold = 0.75d;
        this.consoleAlerts = true;
        this.vlThreshold = 3.0d;
        this.vlResetValue = 1.0d;
        this.vlMultiplier = 50.0d;
        this.vlDecay = 0.5d;
        this.penaltyMinProbability = 0.85d;
        this.penaltyCommands = new HashMap();
        this.customAlertPrefix = DEFAULT_CUSTOM_ALERT_PREFIX;
        this.messagePrefix = DEFAULT_MESSAGE_PREFIX;
        this.messages = createDefaultMessages();
        this.autostartEnabled = false;
        this.autostartLabel = Config.DEFAULT_AUTOSTART_LABEL;
        this.autostartComment = "";
    }

    public PluginSettings(JavaPlugin plugin) {
        this(plugin, null);
    }

    public PluginSettings(JavaPlugin plugin, Logger logger) {
        String msg;
        plugin.saveDefaultConfig();
        FileConfiguration cfg = plugin.getConfig();
        this.debug = cfg.getBoolean("debug", false);
        this.dataDirectory = cfg.getString("outputDirectory", "plugins/NightVisionPro/data");
        this.detectionEnabled = cfg.getBoolean("detection.enabled", false);
        this.apiEndpoint = cfg.getString("detection.endpoint", DEFAULT_API_ENDPOINT);
        this.apiKey = cfg.getString("detection.api-key", "");
        this.sampleSize = cfg.getInt("detection.sample-size", 40);
        this.sampleInterval = cfg.getInt("detection.sample-interval", 10);
        double threshold = cfg.getDouble("alerts.threshold", 0.75d);
        this.alertThreshold = clampValue(threshold, 0.0d, 1.0d, "alerts.threshold", logger);
        this.consoleAlerts = cfg.getBoolean("alerts.console", true);
        this.vlThreshold = cfg.getDouble("violation.threshold", 3.0d);
        this.vlResetValue = cfg.getDouble("violation.reset-value", 1.0d);
        this.vlMultiplier = cfg.getDouble("violation.multiplier", 50.0d);
        this.vlDecay = cfg.getDouble("violation.decay", 0.5d);
        double minProb = cfg.getDouble("penalties.min-probability", 0.85d);
        this.penaltyMinProbability = clampValue(minProb, 0.0d, 1.0d, "penalties.min-probability", logger);
        this.customAlertPrefix = cfg.getString("penalties.custom-alert-prefix", DEFAULT_CUSTOM_ALERT_PREFIX);
        this.penaltyCommands = new HashMap();
        ConfigurationSection cmdSection = cfg.getConfigurationSection("penalties.actions");
        if (cmdSection != null) {
            for (String key : cmdSection.getKeys(false)) {
                try {
                    int vl = Integer.parseInt(key);
                    String cmd = cmdSection.getString(key);
                    if (cmd != null && !cmd.isEmpty()) {
                        this.penaltyCommands.put(Integer.valueOf(vl), cmd);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        this.messagePrefix = cfg.getString("messages.prefix", DEFAULT_MESSAGE_PREFIX);
        this.messages = createDefaultMessages();
        ConfigurationSection msgSection = cfg.getConfigurationSection("messages");
        if (msgSection != null) {
            for (String key2 : msgSection.getKeys(false)) {
                if (!key2.equals("prefix") && (msg = msgSection.getString(key2)) != null) {
                    this.messages.put(key2, msg);
                }
            }
        }
        this.autostartEnabled = cfg.getBoolean("autostart.enabled", false);
        this.autostartLabel = cfg.getString("autostart.label", Config.DEFAULT_AUTOSTART_LABEL);
        this.autostartComment = cfg.getString("autostart.comment", "");
    }

    private double clampValue(double value, double min, double max, String path, Logger logger) {
        if (value < min || value > max) {
            double clamped = Math.max(min, Math.min(max, value));
            if (logger != null) {
                logger.warning("[Settings] " + path + " = " + value + " outside range [" + min + ", " + max + "], using " + clamped);
            }
            return clamped;
        }
        return value;
    }

    private static Map<String, String> createDefaultMessages() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("alerts-enabled", "&aAlerts enabled");
        defaults.put("alerts-disabled", "&eAlerts disabled");
        defaults.put("alert-format", "&c{PLAYER} &7| Prob: &e{PROBABILITY} &7| Buffer: &e{BUFFER}");
        defaults.put("alert-format-vl", "&c{PLAYER} &7| Prob: &e{PROBABILITY} &7| Buffer: &e{BUFFER} &7| VL: &c{VL}");
        return defaults;
    }

    public boolean isDebug() {
        return this.debug;
    }

    public String getDataDirectory() {
        return this.dataDirectory;
    }

    public boolean isDetectionEnabled() {
        return this.detectionEnabled;
    }

    public String getApiEndpoint() {
        return this.apiEndpoint;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public int getSampleSize() {
        return this.sampleSize;
    }

    public int getSampleInterval() {
        return this.sampleInterval;
    }

    public double getAlertThreshold() {
        return this.alertThreshold;
    }

    public boolean isConsoleAlerts() {
        return this.consoleAlerts;
    }

    public double getVlThreshold() {
        return this.vlThreshold;
    }

    public double getVlResetValue() {
        return this.vlResetValue;
    }

    public double getVlMultiplier() {
        return this.vlMultiplier;
    }

    public double getVlDecay() {
        return this.vlDecay;
    }

    public double getPenaltyMinProbability() {
        return this.penaltyMinProbability;
    }

    public Map<Integer, String> getPenaltyCommands() {
        return this.penaltyCommands;
    }

    public String getCustomAlertPrefix() {
        return this.customAlertPrefix;
    }

    public String getMessagePrefix() {
        return this.messagePrefix;
    }

    public String getMessage(String key) {
        return this.messages.getOrDefault(key, "");
    }

    public String getMessage(String key, String player, double probability, double buffer, int vl) {
        String probStr;
        String msg = getMessage(key);
        if (key.startsWith("alert-format")) {
            probStr = String.valueOf((int) (100.0d * probability));
        } else {
            probStr = String.format("%.2f", Double.valueOf(probability));
        }
        return msg.replace("{PLAYER}", player != null ? player : "").replace("{PROBABILITY}", probStr).replace("{BUFFER}", String.format("%.1f", Double.valueOf(buffer))).replace("{VL}", String.valueOf(vl)).replace("<player>", player != null ? player : "").replace("<probability>", probStr).replace("<buffer>", String.format("%.1f", Double.valueOf(buffer))).replace("<vl>", String.valueOf(vl));
    }

    public boolean isAutostartEnabled() {
        return this.autostartEnabled;
    }

    public String getAutostartLabel() {
        return this.autostartLabel;
    }

    public String getAutostartComment() {
        return this.autostartComment;
    }
}
