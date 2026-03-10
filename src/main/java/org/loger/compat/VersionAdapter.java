package org.loger.compat;

import java.util.logging.Logger;
import org.bukkit.Bukkit;

public final class VersionAdapter {
    private static VersionAdapter instance;
    private boolean debugEnabled;
    private final boolean isPaper;
    private final Logger logger;
    private final String rawVersion;
    private final ServerVersion version;

    private VersionAdapter(Logger logger) {
        this.debugEnabled = false;
        this.logger = logger;
        this.rawVersion = Bukkit.getBukkitVersion();
        this.version = detectVersion();
        this.isPaper = detectPaper();
    }

    VersionAdapter(Logger logger, ServerVersion version, boolean isPaper) {
        this.debugEnabled = false;
        this.logger = logger;
        this.version = version;
        this.isPaper = isPaper;
        this.rawVersion = "test";
    }

    public static void init(Logger logger) {
        if (instance == null) {
            instance = new VersionAdapter(logger);
        }
    }

    public static VersionAdapter get() {
        if (instance == null) {
            throw new IllegalStateException("VersionAdapter not initialized. Call init() first.");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    static void reset() {
        instance = null;
    }

    public ServerVersion getVersion() {
        return this.version;
    }

    public boolean isPaper() {
        return this.isPaper;
    }

    public String getRawVersion() {
        return this.rawVersion;
    }

    public boolean isDebugEnabled() {
        return this.debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public boolean isAtLeast(ServerVersion v) {
        return this.version.isAtLeast(v);
    }

    public boolean isBelow(ServerVersion v) {
        return this.version.isBelow(v);
    }

    public boolean isBetween(ServerVersion min, ServerVersion max) {
        return this.version.isBetween(min, max);
    }

    private ServerVersion detectVersion() {
        try {
            String bukkitVersion = Bukkit.getBukkitVersion();
            ServerVersion detected = ServerVersion.fromString(bukkitVersion);
            if (detected == ServerVersion.UNKNOWN) {
                if (this.logger != null) {
                    this.logger.warning("Could not detect server version from: " + bukkitVersion);
                    this.logger.warning("Defaulting to minimum compatibility mode (1.16.5)");
                }
                return ServerVersion.V1_16_5;
            }
            return detected;
        } catch (Exception e) {
            if (this.logger != null) {
                this.logger.warning("Failed to detect server version: " + e.getMessage());
                this.logger.warning("Defaulting to minimum compatibility mode (1.16.5)");
            }
            return ServerVersion.V1_16_5;
        }
    }

    private boolean detectPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("io.papermc.paper.configuration.Configuration");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }

    public void logCompatibilityInfo() {
        if (this.logger == null) {
            return;
        }
        this.logger.info("=== NightVisionPro Version Compatibility ===");
        this.logger.info("Server version: " + this.version + " (raw: " + this.rawVersion + ")");
        this.logger.info("Server type: " + (this.isPaper ? "Paper" : "Spigot/Bukkit"));
        this.logger.info("Compatibility mode: " + getCompatibilityMode());
        if (isAtLeast(ServerVersion.V1_20_5)) {
            this.logger.info("Using modern particle/effect names (1.20.5+)");
        } else {
            this.logger.info("Using legacy particle/effect names (pre-1.20.5)");
        }
        if (!this.isPaper) {
            this.logger.info("Paper events not available - using scheduler fallbacks");
        }
    }

    public String getCompatibilityMode() {
        if (this.version.isAtLeast(ServerVersion.V1_20_5)) {
            return "Modern (1.20.5+)";
        }
        if (this.version.isAtLeast(ServerVersion.V1_17)) {
            return "Legacy-Modern (1.17-1.20.4)";
        }
        return "Legacy (1.16.x)";
    }
}
