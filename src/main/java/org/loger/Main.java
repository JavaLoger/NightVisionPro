package org.loger;

import com.github.retrooper.packetevents.PacketEvents;
import org.loger.alert.AlertManager;
import org.loger.checks.AICheck;
import org.loger.commands.CommandHandler;
import org.loger.compat.VersionAdapter;
import org.loger.config.Config;
import org.loger.config.MessageManager;
import org.loger.data.ProbabilityHistory;
import org.loger.datacollector.DataCollectorFactory;
import org.loger.gui.GuiManager;
import org.loger.hologram.HologramManager;
import org.loger.listeners.HitListener;
import org.loger.listeners.PlayerListener;
import org.loger.listeners.RotationListener;
import org.loger.listeners.TeleportListener;
import org.loger.listeners.TickListener;
import org.loger.scheduler.SchedulerManager;
import org.loger.server.AIClientProvider;
import org.loger.session.ISessionManager;
import org.loger.session.SessionManager;
import org.loger.util.FeatureCalculator;
import org.loger.violation.ViolationManager;
import org.loger.webhook.WebhookManager;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import java.io.File;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private AICheck aiCheck;
    private AIClientProvider aiClientProvider;
    private AlertManager alertManager;
    private CommandHandler commandHandler;
    private Config config;
    private FeatureCalculator featureCalculator;
    private GuiManager guiManager;
    private HitListener hitListener;
    private HologramManager hologramManager;
    private MessageManager messageManager;
    private PlayerListener playerListener;
    private ProbabilityHistory probabilityHistory;
    private RotationListener rotationListener;
    private ISessionManager sessionManager;
    private TeleportListener teleportListener;
    private TickListener tickListener;
    private ViolationManager violationManager;
    private WebhookManager webhookManager;

    public void onLoad() {
        VersionAdapter.init(getLogger());
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().reEncodeByDefault(false).checkForUpdates(false).bStats(false).debug(false);
        PacketEvents.getAPI().load();
    }

    public void onEnable() {
        File file;
        try {
            SchedulerManager.initialize(this);
            getLogger().info("SchedulerManager initialized for " + SchedulerManager.getServerType());
            PacketEvents.getAPI().init();
            VersionAdapter.get().logCompatibilityInfo();
            saveDefaultConfig();
            saveResource("menu.yml", false);
            this.config = new Config(this, getLogger());
            this.messageManager = new MessageManager(this);
            this.webhookManager = new WebhookManager(this);
            String outDir = this.config.getOutputDirectory();
            if (outDir == null || outDir.isEmpty()) {
                outDir = "data";
            }
            String outDir2 = outDir;
            if (new File(outDir2).isAbsolute()) {
                file = new File(outDir2);
            } else {
                file = new File(getDataFolder(), outDir2);
            }
            File outputDir = file;
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            this.featureCalculator = new FeatureCalculator();
            this.sessionManager = DataCollectorFactory.createSessionManager(this);
            this.aiClientProvider = new AIClientProvider(this, this.config);
            this.alertManager = new AlertManager(this, this.config);
            this.probabilityHistory = new ProbabilityHistory();
            this.violationManager = new ViolationManager(this, this.config, this.alertManager);
            this.aiCheck = new AICheck(this, this.config, this.aiClientProvider, this.alertManager, this.violationManager, this.probabilityHistory);
            this.violationManager.setAICheck(this.aiCheck);
            this.hologramManager = new HologramManager(this, this.aiCheck, this.violationManager, this.probabilityHistory);
            this.guiManager = new GuiManager(this);
            this.violationManager.setGuiManager(this.guiManager);
            if (this.config.isAiEnabled()) {
                this.aiClientProvider.initialize().thenAccept(success -> {
                    if (success.booleanValue()) {
                        getLogger().info("[AI] Connected to " + this.config.getServerAddress());
                    } else {
                        getLogger().warning("[AI] Failed to connect to inference server");
                    }
                });
            }
            this.tickListener = new TickListener(this, this.sessionManager, this.aiCheck);
            this.hitListener = new HitListener(this.sessionManager, this.aiCheck);
            this.rotationListener = new RotationListener(this.sessionManager, this.aiCheck);
            this.playerListener = new PlayerListener(this, this.aiCheck, this.alertManager, this.violationManager, this.sessionManager instanceof SessionManager ? (SessionManager) this.sessionManager : null);
            this.teleportListener = new TeleportListener(this.aiCheck);
            this.tickListener.setHitListener(this.hitListener);
            this.playerListener.setHitListener(this.hitListener);
            this.hitListener.cacheOnlinePlayers();
            this.tickListener.start();
            for (Player player : Bukkit.getOnlinePlayers()) {
                this.probabilityHistory.recordJoinTime(player.getUniqueId());
            }
            getServer().getPluginManager().registerEvents(this.playerListener, this);
            getServer().getPluginManager().registerEvents(this.teleportListener, this);
            PacketEvents.getAPI().getEventManager().registerListener(this.hitListener);
            PacketEvents.getAPI().getEventManager().registerListener(this.rotationListener);
            this.commandHandler = new CommandHandler(this.sessionManager, this.alertManager, this.aiCheck, this);
            this.commandHandler.setHologramManager(this.hologramManager);
            this.commandHandler.setProbabilityHistory(this.probabilityHistory);
            this.commandHandler.setGuiManager(this.guiManager);
            PluginCommand command = getCommand("nvp");
            if (command != null) {
                command.setExecutor(this.commandHandler);
                command.setTabCompleter(this.commandHandler);
            }
            this.hologramManager.start();
            getLogger().info("NightVisionPro enabled successfully!");
            getLogger().info("Data collector: ENABLED (output: " + this.config.getOutputDirectory() + ")");
            if (this.config.isAiEnabled()) {
                getLogger().info("AI detection: ENABLED (threshold: " + this.config.getAiAlertThreshold() + ")");
            } else {
                getLogger().info("AI detection: DISABLED");
            }
        } catch (Exception e) {
            getLogger().severe("Failed to initialize SchedulerManager: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public void onDisable() {
        if (this.webhookManager != null) {
            this.webhookManager.shutdown();
        }
        if (this.guiManager != null) {
            this.guiManager.shutdown();
        }
        if (this.hologramManager != null) {
            this.hologramManager.cleanup();
            this.hologramManager.stop();
        }
        if (this.tickListener != null) {
            this.tickListener.stop();
        }
        if (this.sessionManager != null) {
            getLogger().info("Stopping all active sessions...");
            this.sessionManager.stopAllSessions();
        }
        if (this.aiCheck != null) {
            this.aiCheck.clearAll();
        }
        if (this.violationManager != null) {
            this.violationManager.shutdown();
        }
        if (this.commandHandler != null) {
            this.commandHandler.cleanup();
        }
        if (this.aiClientProvider != null && this.aiClientProvider.isAvailable()) {
            getLogger().info("Shutting down SignalR client...");
            try {
                this.aiClientProvider.shutdown().get(5L, TimeUnit.SECONDS);
            } catch (Exception e) {
                getLogger().warning("Error shutting down SignalR client: " + e.getMessage());
            }
        }
        PacketEvents.getAPI().terminate();
        getLogger().info("NightVisionPro disabled successfully!");
    }

    public void reloadPluginConfig() {
        SchedulerManager.getAdapter().runSync(() -> {
            try {
                if (this.hologramManager != null) {
                    this.hologramManager.cleanup();
                }
                reloadConfig();
                this.config = new Config(this, getLogger());
                if (this.messageManager != null) {
                    this.messageManager.reload();
                }
                if (this.webhookManager != null) {
                    this.webhookManager.reload();
                }
                this.alertManager.setConfig(this.config);
                this.violationManager.setConfig(this.config);
                this.aiCheck.setConfig(this.config);
                if (this.hologramManager != null) {
                    this.hologramManager.stop();
                    this.hologramManager.start();
                }
                if (this.guiManager != null) {
                    this.guiManager.reload();
                }
                if (this.aiClientProvider != null) {
                    this.aiClientProvider.setConfig(this.config);
                    if (this.config.isAiEnabled()) {
                        this.aiClientProvider.reload().thenAccept(success -> {
                            if (success.booleanValue()) {
                                getLogger().info("[AI] Reconnected to " + this.config.getServerAddress());
                            }
                        });
                    } else {
                        this.aiClientProvider.shutdown();
                    }
                }
                getLogger().info("Configuration reloaded!");
            } catch (Exception e) {
                getLogger().severe("Failed to reload configuration: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public Config getPluginConfig() {
        return this.config;
    }

    public ISessionManager getSessionManager() {
        return this.sessionManager;
    }

    public FeatureCalculator getFeatureCalculator() {
        return this.featureCalculator;
    }

    public AICheck getAiCheck() {
        return this.aiCheck;
    }

    public AlertManager getAlertManager() {
        return this.alertManager;
    }

    public ViolationManager getViolationManager() {
        return this.violationManager;
    }

    public AIClientProvider getAiClientProvider() {
        return this.aiClientProvider;
    }

    public ProbabilityHistory getProbabilityHistory() {
        return this.probabilityHistory;
    }

    public HologramManager getHologramManager() {
        return this.hologramManager;
    }

    public GuiManager getGuiManager() {
        return this.guiManager;
    }

    public CommandHandler getCommandHandler() {
        return this.commandHandler;
    }

    public MessageManager getMessageManager() {
        return this.messageManager;
    }

    public WebhookManager getWebhookManager() {
        return this.webhookManager;
    }

    public void debug(String message) {
        if (this.config != null && this.config.isDebug()) {
            getLogger().info("[Debug] " + message);
        }
    }
}
