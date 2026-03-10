package org.loger.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import okhttp3.HttpUrl;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Config {
    public static final double DEFAULT_AI_ALERT_THRESHOLD = 0.5d;
    public static final String DEFAULT_AI_API_KEY = "";
    public static final double DEFAULT_AI_BUFFER_DECREASE = 0.25d;
    public static final double DEFAULT_AI_BUFFER_FLAG = 50.0d;
    public static final double DEFAULT_AI_BUFFER_MULTIPLIER = 100.0d;
    public static final double DEFAULT_AI_BUFFER_RESET_ON_FLAG = 25.0d;
    public static final boolean DEFAULT_AI_CONSOLE_ALERTS = true;
    public static final boolean DEFAULT_AI_ENABLED = false;
    public static final double DEFAULT_AI_PUNISHMENT_MIN_PROBABILITY = 0.5d;
    public static final int DEFAULT_AI_SEQUENCE = 40;
    public static final int DEFAULT_AI_STEP = 10;
    public static final int DEFAULT_ALERT_TIME_DELAY = 0;
    public static final String DEFAULT_AUTOSTART_COMMENT = "";
    public static final boolean DEFAULT_AUTOSTART_ENABLED = false;
    public static final String DEFAULT_AUTOSTART_LABEL = "UNLABELED";
    public static final double DEFAULT_AUTO_GUI_THRESHOLD = 0.3d;
    public static final boolean DEFAULT_DEBUG = false;
    public static final boolean DEFAULT_FOLIA_ENABLED = true;
    public static final boolean DEFAULT_FOLIA_ENTITY_SCHEDULER_ENABLED = true;
    public static final boolean DEFAULT_FOLIA_REGION_SCHEDULER_ENABLED = true;
    public static final int DEFAULT_FOLIA_THREAD_POOL_SIZE = 0;
    public static final boolean DEFAULT_GUI_ENABLED = true;
    public static final int DEFAULT_GUI_VL = 1;
    public static final int DEFAULT_HOLOGRAM_DISTANCE = 15;
    public static final boolean DEFAULT_HOLOGRAM_ENABLED = true;
    public static final double DEFAULT_HOLOGRAM_HEIGHT = 2.0d;
    public static final double DEFAULT_HOLOGRAM_LINE_SPACING = 0.25d;
    public static final int DEFAULT_HOLOGRAM_UPDATE_INTERVAL = 2;
    public static final String DEFAULT_LITEBANS_DB_HOST = "localhost";
    public static final String DEFAULT_LITEBANS_DB_NAME = "litebans";
    public static final String DEFAULT_LITEBANS_DB_PASSWORD = "";
    public static final int DEFAULT_LITEBANS_DB_PORT = 3306;
    public static final String DEFAULT_LITEBANS_DB_USERNAME = "";
    public static final boolean DEFAULT_LITEBANS_ENABLED = false;
    public static final int DEFAULT_LITEBANS_LOOKBACK_DAYS = 7;
    public static final String DEFAULT_LITEBANS_TABLE_PREFIX = "litebans_";
    public static final String DEFAULT_MSG_ALERTS_DISABLED = "&eAI alerts disabled";
    public static final String DEFAULT_MSG_ALERTS_ENABLED = "&aAI alerts enabled";
    public static final String DEFAULT_MSG_ALERT_FORMAT = "&f&NVP&7(%player% &f%ping% ⇆&f&NVP&7) &fпровалил &6KillAura &7(%probability%%) &7(&f&NVP&7%vl%VL&7)";
    public static final String DEFAULT_MSG_ALERT_FORMAT_VL = "&f&NVP&7(%player% &f%ping% ⇆&f&NVP&7) &fпровалил &6KillAura &7(%probability%%) &7(&f&NVP&7%vl%VL&7)";
    public static final String DEFAULT_OUTPUT_DIRECTORY = "plugins/NightVisionPro/data";
    public static final String DEFAULT_PREFIX = "&7[&f&NVP&r&7]&f ";
    public static final int DEFAULT_REPORT_STATS_INTERVAL_SECONDS = 30;
    public static final String DEFAULT_SERVER_ADDRESS = "https://api-inference.huggingface.co/models/username/model";
    public static final String DEFAULT_SERVER_TYPE = "signalr";
    public static final String DEFAULT_SYNC_TYPE = "none";
    public static final int DEFAULT_VL_DECAY_AMOUNT = 1;
    public static final boolean DEFAULT_VL_DECAY_ENABLED = true;
    public static final int DEFAULT_VL_DECAY_INTERVAL_SECONDS = 60;
    public static final boolean DEFAULT_WEBHOOK_ENABLED = true;
    public static final int DEFAULT_WEBHOOK_VL = 1;
    public static final boolean DEFAULT_WORLDGUARD_ENABLED = true;
    public static final double HIT_LOCK_THRESHOLD = 5.0d;
    public static final int POST_HIT_TICKS = 3;
    public static final int POST_HIT_TIMEOUT_TICKS = 40;
    public static final int PRE_HIT_TICKS = 5;
    private final double aiAlertThreshold;
    private final String aiApiKey;
    private final double aiBufferDecrease;
    private final double aiBufferFlag;
    private final double aiBufferMultiplier;
    private final double aiBufferResetOnFlag;
    private final boolean aiConsoleAlerts;
    private final boolean aiEnabled;
    private final double aiPunishmentMinProbability;
    private final int aiSequence;
    private final int aiStep;
    private final int alertTimeDelay;
    private final double autoGuiThreshold;
    private final String autostartComment;
    private final boolean autostartEnabled;
    private final String autostartLabel;
    private final List<String> avgParameters;
    private final Map<String, String> commandAliases;
    private final boolean debug;
    private final boolean foliaEnabled;
    private final boolean foliaEntitySchedulerEnabled;
    private final boolean foliaRegionSchedulerEnabled;
    private final int foliaThreadPoolSize;
    private final boolean guiEnabled;
    private final int guiVl;
    private final double hitLockThreshold;
    private final int hologramDistance;
    private final boolean hologramEnabled;
    private final double hologramHeight;
    private final double hologramLineSpacing;
    private final List<String> hologramLines;
    private final int hologramUpdateInterval;
    private final Set<String> liteBansCheatReasons;
    private final String liteBansDbHost;
    private final String liteBansDbName;
    private final String liteBansDbPassword;
    private final int liteBansDbPort;
    private final String liteBansDbUsername;
    private final boolean liteBansEnabled;
    private final int liteBansLookbackDays;
    private final String liteBansTablePrefix;
    private final Map<String, String> messages;
    private final String mysqlDatabase;
    private final String mysqlHost;
    private final String mysqlPassword;
    private final int mysqlPort;
    private final String mysqlTablePrefix;
    private final String mysqlUsername;
    private final String outputDirectory;
    private final int postHitTicks;
    private final int postHitTimeoutTicks;
    private final int preHitTicks;
    private final String prefix;
    private final Map<Integer, String> punishmentCommands;
    private final String redisHost;
    private final String redisPassword;
    private final int redisPort;
    private final String redisPrefix;
    private final int reportStatsIntervalSeconds;
    private final String serverAddress;
    private final ServerType serverType;
    private final List<String> statFormat;
    private final String syncType;
    private final int vlDecayAmount;
    private final boolean vlDecayEnabled;
    private final int vlDecayIntervalSeconds;
    private final boolean webhookEnabled;
    private final int webhookVl;
    private final List<String> worldGuardDisabledRegions;
    private final boolean worldGuardEnabled;
    public static final List<String> DEFAULT_WORLDGUARD_DISABLED_REGIONS = new ArrayList();
    public static final List<String> DEFAULT_HOLOGRAM_LINES = Arrays.asList("&7AVG: %avg%", "%gcd% %rotation% %aimbot% %snap% %smooth%");
    public static final List<String> DEFAULT_AVG_PARAMETERS = Arrays.asList("rotation", "aimbot", "gcd", "snap", "smooth", "rotation_1h", "aimbot_1h", "gcd_1h", "snap_1h", "smooth_1h", "rotation_max", "aimbot_max", "gcd_max", "snap_max", "smooth_max");
    public static final List<String> DEFAULT_STAT_FORMAT = Arrays.asList("&7───────────────────────────────", "&fСтатистика игрока: &#FF0000%player%", "&fТекущая вероятность: %probability%", "&fЗа последний час: %probability_1h%", "&fЗа 6 часов: %probability_6h%", "&fЗа всё время: %probability_all%", "&fVL: &#FF0000%vl%", "&fPing: &7%ping%ms", "&fВремя игры: &7%play_time%", "&7───────────────────────────────");

    public Config() {
        this.debug = false;
        this.preHitTicks = 5;
        this.postHitTicks = 3;
        this.hitLockThreshold = 5.0d;
        this.postHitTimeoutTicks = 40;
        this.outputDirectory = "plugins/NightVisionPro/data";
        this.aiEnabled = false;
        this.aiApiKey = "";
        this.aiAlertThreshold = 0.5d;
        this.aiConsoleAlerts = true;
        this.aiBufferFlag = 50.0d;
        this.aiBufferResetOnFlag = 25.0d;
        this.aiBufferMultiplier = 100.0d;
        this.aiBufferDecrease = 0.25d;
        this.aiSequence = 40;
        this.aiStep = 10;
        this.aiPunishmentMinProbability = 0.5d;
        this.punishmentCommands = new HashMap();
        this.commandAliases = new HashMap();
        this.webhookEnabled = true;
        this.webhookVl = 1;
        this.guiEnabled = true;
        this.guiVl = 1;
        this.autoGuiThreshold = 0.3d;
        this.prefix = DEFAULT_PREFIX;
        this.messages = createDefaultMessages();
        this.liteBansEnabled = false;
        this.liteBansDbHost = DEFAULT_LITEBANS_DB_HOST;
        this.liteBansDbPort = DEFAULT_LITEBANS_DB_PORT;
        this.liteBansDbName = DEFAULT_LITEBANS_DB_NAME;
        this.liteBansDbUsername = "";
        this.liteBansDbPassword = "";
        this.liteBansTablePrefix = DEFAULT_LITEBANS_TABLE_PREFIX;
        this.liteBansLookbackDays = 7;
        this.liteBansCheatReasons = createDefaultCheatReasons();
        this.autostartEnabled = false;
        this.autostartLabel = DEFAULT_AUTOSTART_LABEL;
        this.autostartComment = "";
        this.serverType = ServerType.fromString(DEFAULT_SERVER_TYPE);
        this.serverAddress = DEFAULT_SERVER_ADDRESS;
        this.reportStatsIntervalSeconds = 30;
        this.vlDecayEnabled = true;
        this.vlDecayIntervalSeconds = 60;
        this.vlDecayAmount = 1;
        this.worldGuardEnabled = true;
        this.worldGuardDisabledRegions = new ArrayList(DEFAULT_WORLDGUARD_DISABLED_REGIONS);
        this.foliaEnabled = true;
        this.foliaThreadPoolSize = 0;
        this.foliaEntitySchedulerEnabled = true;
        this.foliaRegionSchedulerEnabled = true;
        this.alertTimeDelay = 0;
        this.hologramEnabled = true;
        this.hologramDistance = 15;
        this.hologramHeight = 2.0d;
        this.hologramUpdateInterval = 2;
        this.hologramLineSpacing = 0.25d;
        this.hologramLines = new ArrayList(DEFAULT_HOLOGRAM_LINES);
        this.avgParameters = new ArrayList(DEFAULT_AVG_PARAMETERS);
        this.statFormat = new ArrayList(DEFAULT_STAT_FORMAT);
        this.syncType = "none";
        this.redisHost = DEFAULT_LITEBANS_DB_HOST;
        this.redisPort = 6379;
        this.redisPassword = "";
        this.redisPrefix = "nvp:";
        this.mysqlHost = DEFAULT_LITEBANS_DB_HOST;
        this.mysqlPort = DEFAULT_LITEBANS_DB_PORT;
        this.mysqlDatabase = "nvp";
        this.mysqlUsername = "root";
        this.mysqlPassword = "";
        this.mysqlTablePrefix = "nvp_";
    }

    private static Set<String> createDefaultCheatReasons() {
        Set<String> reasons = new HashSet<>();
        reasons.add("killaura");
        reasons.add("cheat");
        reasons.add("hack");
        return reasons;
    }

    private static Map<String, String> createDefaultMessages() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("alerts-enabled", DEFAULT_MSG_ALERTS_ENABLED);
        defaults.put("alerts-disabled", DEFAULT_MSG_ALERTS_DISABLED);
        defaults.put("alert-format", "&x&F&B&0&8&0&8(%player% &7%ping% ⇆&x&F&B&0&8&0&8) &7провалил &6KillAura &7(%probability%%) &7(&x&F&B&0&8&0&8%vl%VL&7)");
        defaults.put("alert-format-vl", "&x&F&B&0&8&0&8(%player% &7%ping% ⇆&x&F&B&0&8&0&8) &7провалил &6KillAura &7(%probability%%) &7(&x&F&B&0&8&0&8%vl%VL&7)");
        return defaults;
    }

    public Config(JavaPlugin plugin) {
        this(plugin, null);
    }

    public Config(JavaPlugin plugin, Logger logger) {
        ConfigurationSection cmdSection;
        String msg;
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        this.debug = config.getBoolean("debug", false);
        this.preHitTicks = 5;
        this.postHitTicks = 3;
        this.hitLockThreshold = 5.0d;
        this.postHitTimeoutTicks = 40;
        this.outputDirectory = config.getString("outputDirectory", "plugins/NightVisionPro/data");
        this.aiEnabled = config.getBoolean("detection.enabled", config.getBoolean("ai.enabled", false));
        this.aiApiKey = config.getString("detection.api-key", config.getString("ai.api-key", ""));
        double alertThreshold = config.getDouble("alerts.threshold", config.getDouble("ai.alert.threshold", 0.5d));
        this.aiAlertThreshold = clampThreshold(alertThreshold, "alerts.threshold", logger);
        this.aiConsoleAlerts = config.getBoolean("alerts.console", config.getBoolean("ai.alert.console", true));
        this.aiBufferFlag = config.getDouble("violation.threshold", config.getDouble("ai.buffer.flag", 50.0d));
        this.aiBufferResetOnFlag = config.getDouble("violation.reset-value", config.getDouble("ai.buffer.reset-on-flag", 25.0d));
        this.aiBufferMultiplier = config.getDouble("violation.multiplier", config.getDouble("ai.buffer.multiplier", 100.0d));
        this.aiBufferDecrease = config.getDouble("violation.decay", config.getDouble("ai.buffer.decrease", 0.25d));
        this.aiSequence = config.getInt("detection.sample-size", config.getInt("ai.sequence", 40));
        this.aiStep = config.getInt("detection.sample-interval", config.getInt("ai.step", 10));
        double punishmentMinProb = config.getDouble("penalties.min-probability", config.getDouble("ai.punishment.min-probability", 0.5d));
        this.aiPunishmentMinProbability = clampThreshold(punishmentMinProb, "penalties.min-probability", logger);
        this.webhookEnabled = config.getBoolean("penalties.webhook.enabled", true);
        this.webhookVl = config.getInt("penalties.webhook.vl", 1);
        this.guiEnabled = config.getBoolean("penalties.gui.enabled", true);
        this.guiVl = config.getInt("penalties.gui.vl", 1);
        this.autoGuiThreshold = config.getDouble("penalties.auto-gui-threshold", 0.3d);
        this.punishmentCommands = new HashMap();
        this.commandAliases = new HashMap();
        ConfigurationSection aliasSection = config.getConfigurationSection("commands.aliases");
        if (aliasSection != null) {
            for (String key : aliasSection.getKeys(false)) {
                String alias = aliasSection.getString(key);
                if (alias != null && !alias.isEmpty()) {
                    this.commandAliases.put(key.toLowerCase(), alias.toLowerCase());
                }
            }
        }
        ConfigurationSection cmdSection2 = config.getConfigurationSection("penalties.actions");
        if (cmdSection2 != null) {
            cmdSection = cmdSection2;
        } else {
            cmdSection = config.getConfigurationSection("ai.punishment.commands");
        }
        if (cmdSection != null) {
            for (String key2 : cmdSection.getKeys(false)) {
                try {
                    int vl = Integer.parseInt(key2);
                    String cmd = cmdSection.getString(key2);
                    if (cmd != null && !cmd.isEmpty()) {
                        this.punishmentCommands.put(Integer.valueOf(vl), cmd);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        this.prefix = config.getString("messages.prefix", DEFAULT_PREFIX);
        this.messages = createDefaultMessages();
        ConfigurationSection msgSection = config.getConfigurationSection("messages");
        if (msgSection != null) {
            for (String key3 : msgSection.getKeys(false)) {
                if (!key3.equals("prefix") && (msg = msgSection.getString(key3)) != null) {
                    this.messages.put(key3, msg);
                }
            }
        }
        this.liteBansEnabled = config.getBoolean("litebans.enabled", false);
        this.liteBansDbHost = config.getString("litebans.database.host", DEFAULT_LITEBANS_DB_HOST);
        this.liteBansDbPort = config.getInt("litebans.database.port", DEFAULT_LITEBANS_DB_PORT);
        this.liteBansDbName = config.getString("litebans.database.name", DEFAULT_LITEBANS_DB_NAME);
        this.liteBansDbUsername = config.getString("litebans.database.username", "");
        this.liteBansDbPassword = config.getString("litebans.database.password", "");
        this.liteBansTablePrefix = config.getString("litebans.table-prefix", DEFAULT_LITEBANS_TABLE_PREFIX);
        this.liteBansLookbackDays = config.getInt("litebans.lookback-days", 7);
        this.liteBansCheatReasons = new HashSet();
        List<String> reasonsList = config.getStringList("litebans.cheat-reasons");
        if (reasonsList.isEmpty()) {
            this.liteBansCheatReasons.addAll(createDefaultCheatReasons());
        } else {
            this.liteBansCheatReasons.addAll(reasonsList);
        }
        this.autostartEnabled = config.getBoolean("autostart.enabled", false);
        this.autostartLabel = config.getString("autostart.label", DEFAULT_AUTOSTART_LABEL);
        this.autostartComment = config.getString("autostart.comment", "");
        this.serverType = ServerType.fromString(config.getString("detection.server-type", config.getString("ai.server-type", DEFAULT_SERVER_TYPE)));
        this.serverAddress = config.getString("detection.endpoint", config.getString("ai.server", DEFAULT_SERVER_ADDRESS));
        this.reportStatsIntervalSeconds = 30;
        this.vlDecayEnabled = config.getBoolean("violation.vl-decay.enabled", true);
        this.vlDecayIntervalSeconds = config.getInt("violation.vl-decay.interval", 60);
        this.vlDecayAmount = config.getInt("violation.vl-decay.amount", 1);
        this.worldGuardEnabled = config.getBoolean("detection.worldguard.enabled", true);
        this.worldGuardDisabledRegions = config.getStringList("detection.worldguard.disabled-regions");
        this.foliaEnabled = config.getBoolean("folia.enabled", true);
        this.foliaThreadPoolSize = config.getInt("folia.thread-pool-size", 0);
        this.foliaEntitySchedulerEnabled = config.getBoolean("folia.entity-scheduler.enabled", true);
        this.foliaRegionSchedulerEnabled = config.getBoolean("folia.region-scheduler.enabled", true);
        this.alertTimeDelay = config.getInt("alerts.time-delay", 0);
        this.hologramEnabled = config.getBoolean("holograms.enabled", true);
        this.hologramDistance = config.getInt("holograms.distance", 15);
        this.hologramHeight = config.getDouble("holograms.height", 2.0d);
        this.hologramUpdateInterval = config.getInt("holograms.update-interval", 2);
        this.hologramLineSpacing = config.getDouble("holograms.line-spacing", 0.25d);
        List<String> linesList = config.getStringList("holograms.format");
        this.hologramLines = linesList.isEmpty() ? new ArrayList<>(DEFAULT_HOLOGRAM_LINES) : linesList;
        List<String> avgParamsList = config.getStringList("holograms.avg-parameters");
        this.avgParameters = avgParamsList.isEmpty() ? new ArrayList<>(DEFAULT_AVG_PARAMETERS) : avgParamsList;
        List<String> statFormatList = config.getStringList("stat.format");
        this.statFormat = statFormatList.isEmpty() ? new ArrayList<>(DEFAULT_STAT_FORMAT) : statFormatList;
        this.syncType = config.getString("sync.type", "none");
        this.redisHost = config.getString("sync.redis.host", DEFAULT_LITEBANS_DB_HOST);
        this.redisPort = config.getInt("sync.redis.port", 6379);
        this.redisPassword = config.getString("sync.redis.password", "");
        this.redisPrefix = config.getString("sync.redis.prefix", "nvp:");
        this.mysqlHost = config.getString("sync.mysql.host", DEFAULT_LITEBANS_DB_HOST);
        this.mysqlPort = config.getInt("sync.mysql.port", DEFAULT_LITEBANS_DB_PORT);
        this.mysqlDatabase = config.getString("sync.mysql.database", "nvp");
        this.mysqlUsername = config.getString("sync.mysql.username", "root");
        this.mysqlPassword = config.getString("sync.mysql.password", "");
        this.mysqlTablePrefix = config.getString("sync.mysql.table-prefix", "nvp");
    }

    private double clampThreshold(double value, String configPath, Logger logger) {
        if (value < 0.0d || value > 1.0d) {
            double clamped = Math.max(0.0d, Math.min(1.0d, value));
            if (logger != null) {
                logger.warning("[Config] " + configPath + " value " + value + " is outside valid range [0.0, 1.0], clamped to " + clamped);
            }
            return clamped;
        }
        return value;
    }

    public boolean isDebug() {
        return this.debug;
    }

    public int getPreHitTicks() {
        return this.preHitTicks;
    }

    public int getPostHitTicks() {
        return this.postHitTicks;
    }

    public double getHitLockThreshold() {
        return this.hitLockThreshold;
    }

    public int getPostHitTimeoutTicks() {
        return this.postHitTimeoutTicks;
    }

    public String getOutputDirectory() {
        return this.outputDirectory;
    }

    public boolean isAiEnabled() {
        return this.aiEnabled;
    }

    public String getAiApiKey() {
        return this.aiApiKey;
    }

    public double getAiAlertThreshold() {
        return this.aiAlertThreshold;
    }

    public boolean isAiConsoleAlerts() {
        return this.aiConsoleAlerts;
    }

    public double getAiBufferFlag() {
        return this.aiBufferFlag;
    }

    public double getAiBufferResetOnFlag() {
        return this.aiBufferResetOnFlag;
    }

    public double getAiBufferMultiplier() {
        return this.aiBufferMultiplier;
    }

    public double getAiBufferDecrease() {
        return this.aiBufferDecrease;
    }

    public int getAiSequence() {
        return this.aiSequence;
    }

    public int getAiStep() {
        return this.aiStep;
    }

    public double getAiPunishmentMinProbability() {
        return this.aiPunishmentMinProbability;
    }

    public boolean isWebhookEnabled() {
        return this.webhookEnabled;
    }

    public int getWebhookVl() {
        return this.webhookVl;
    }

    public boolean isGuiEnabled() {
        return this.guiEnabled;
    }

    public int getGuiVl() {
        return this.guiVl;
    }

    public double getAutoGuiThreshold() {
        return this.autoGuiThreshold;
    }

    public String getPunishmentCommand(int vl) {
        return this.punishmentCommands.get(Integer.valueOf(vl));
    }

    public Map<Integer, String> getPunishmentCommands() {
        return this.punishmentCommands;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getMessage(String key) {
        return this.messages.getOrDefault(key, "");
    }

    public String getMessage(String key, String player, double probability, double buffer, int vl) {
        String msg = getMessage(key);
        String playerValue = player != null ? player : "";
        String probValue = String.valueOf((int) (100.0d * probability));
        String bufferValue = String.format("%.1f", Double.valueOf(buffer));
        String vlValue = String.valueOf(vl);
        return msg.replace("%player%", playerValue).replace("%probability%", probValue).replace("%buffer%", bufferValue).replace("%vl%", vlValue);
    }

    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public boolean isLiteBansEnabled() {
        return this.liteBansEnabled;
    }

    public String getLiteBansDbHost() {
        return this.liteBansDbHost;
    }

    public int getLiteBansDbPort() {
        return this.liteBansDbPort;
    }

    public String getLiteBansDbName() {
        return this.liteBansDbName;
    }

    public String getLiteBansDbUsername() {
        return this.liteBansDbUsername;
    }

    public String getLiteBansDbPassword() {
        return this.liteBansDbPassword;
    }

    public String getLiteBansTablePrefix() {
        return this.liteBansTablePrefix;
    }

    public int getLiteBansLookbackDays() {
        return this.liteBansLookbackDays;
    }

    public Set<String> getLiteBansCheatReasons() {
        return this.liteBansCheatReasons;
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

    public ServerType getServerType() {
        return this.serverType;
    }

    public String getServerAddress() {
        return this.serverAddress;
    }

    public int getReportStatsIntervalSeconds() {
        return this.reportStatsIntervalSeconds;
    }

    public String getServerHost() {
        int colonIndex = this.serverAddress.lastIndexOf(58);
        if (colonIndex > 0) {
            return this.serverAddress.substring(0, colonIndex);
        }
        return this.serverAddress;
    }

    public int getServerPort() {
        int colonIndex = this.serverAddress.lastIndexOf(58);
        if (colonIndex <= 0 || colonIndex >= this.serverAddress.length() - 1) {
            return 5000;
        }
        try {
            return Integer.parseInt(this.serverAddress.substring(colonIndex + 1));
        } catch (NumberFormatException e) {
            return 5000;
        }
    }

    public boolean isVlDecayEnabled() {
        return this.vlDecayEnabled;
    }

    public int getVlDecayIntervalSeconds() {
        return this.vlDecayIntervalSeconds;
    }

    public int getVlDecayAmount() {
        return this.vlDecayAmount;
    }

    public boolean isWorldGuardEnabled() {
        return this.worldGuardEnabled;
    }

    public List<String> getWorldGuardDisabledRegions() {
        return this.worldGuardDisabledRegions;
    }

    public boolean isFoliaEnabled() {
        return this.foliaEnabled;
    }

    public int getFoliaThreadPoolSize() {
        return this.foliaThreadPoolSize;
    }

    public boolean isFoliaEntitySchedulerEnabled() {
        return this.foliaEntitySchedulerEnabled;
    }

    public boolean isFoliaRegionSchedulerEnabled() {
        return this.foliaRegionSchedulerEnabled;
    }

    public int getAlertTimeDelay() {
        return this.alertTimeDelay;
    }

    public boolean isHologramEnabled() {
        return this.hologramEnabled;
    }

    public int getHologramDistance() {
        return this.hologramDistance;
    }

    public double getHologramHeight() {
        return this.hologramHeight;
    }

    public int getHologramUpdateInterval() {
        return this.hologramUpdateInterval;
    }

    public double getHologramLineSpacing() {
        return this.hologramLineSpacing;
    }

    public List<String> getHologramLines() {
        return this.hologramLines;
    }

    public List<String> getAvgParameters() {
        return this.avgParameters;
    }

    public List<String> getStatFormat() {
        return this.statFormat;
    }

    public String getCommandAlias(String subCommand) {
        return this.commandAliases.getOrDefault(subCommand.toLowerCase(), subCommand);
    }

    public String getSyncType() {
        return this.syncType;
    }

    public String getRedisHost() {
        return this.redisHost;
    }

    public int getRedisPort() {
        return this.redisPort;
    }

    public String getRedisPassword() {
        return this.redisPassword;
    }

    public String getRedisPrefix() {
        return this.redisPrefix;
    }

    public String getMysqlHost() {
        return this.mysqlHost;
    }

    public int getMysqlPort() {
        return this.mysqlPort;
    }

    public String getMysqlDatabase() {
        return this.mysqlDatabase;
    }

    public String getMysqlUsername() {
        return this.mysqlUsername;
    }

    public String getMysqlPassword() {
        return this.mysqlPassword;
    }

    public String getMysqlTablePrefix() {
        return this.mysqlTablePrefix;
    }

    public boolean isMessageEmpty(String key) {
        String msg = getMessage(key);
        return msg == null || msg.isEmpty() || msg.equals("\"\"") || msg.equals(HttpUrl.PATH_SEGMENT_ENCODE_SET_URI);
    }
}
