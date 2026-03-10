package org.loger.checks;

import org.loger.Main;
import org.loger.alert.AlertManager;
import org.loger.compat.WorldGuardCompat;
import org.loger.config.Config;
import org.loger.config.ServerType;
import org.loger.data.AIPlayerData;
import org.loger.data.ProbabilityHistory;
import org.loger.data.TickData;
import org.loger.scheduler.SchedulerAdapter;
import org.loger.scheduler.SchedulerManager;
import org.loger.server.AIClientProvider;
import org.loger.server.AIResponse;
import org.loger.server.FlatBufferSerializer;
import org.loger.server.IAIClient;
import org.loger.violation.ViolationManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class AICheck {
    private static final long CONFIG_CACHE_MS = 2000;
    private final AlertManager alertManager;
    private volatile double cachedAlertThreshold;
    private volatile double cachedAutoGuiThreshold;
    private volatile List<String> cachedAvgParams;
    private volatile double cachedBufferDecrease;
    private volatile double cachedBufferFlag;
    private volatile double cachedBufferMultiplier;
    private volatile double cachedBufferResetOnFlag;
    private final AIClientProvider clientProvider;
    private Config config;
    private final Logger logger;
    private final Main plugin;
    private final ProbabilityHistory probabilityHistory;
    private int sequence;
    private int step;
    private final ViolationManager violationManager;
    private WorldGuardCompat worldGuardCompat;
    private volatile long lastConfigCacheTime = 0;
    private final SchedulerAdapter schedulerAdapter = SchedulerManager.getAdapter();
    private final Map<UUID, AIPlayerData> playerData = new ConcurrentHashMap();

    public AICheck(Main plugin, Config config, AIClientProvider clientProvider, AlertManager alertManager, ViolationManager violationManager, ProbabilityHistory probabilityHistory) {
        this.plugin = plugin;
        this.config = config;
        this.clientProvider = clientProvider;
        this.alertManager = alertManager;
        this.violationManager = violationManager;
        this.probabilityHistory = probabilityHistory;
        this.logger = plugin.getLogger();
        this.sequence = config.getAiSequence();
        this.step = config.getAiStep();
        this.worldGuardCompat = new WorldGuardCompat(plugin.getLogger(), config.isWorldGuardEnabled(), config.getWorldGuardDisabledRegions());
    }

    public void setConfig(Config config) {
        this.config = config;
        this.sequence = config.getAiSequence();
        this.step = config.getAiStep();
        this.worldGuardCompat = new WorldGuardCompat(this.plugin.getLogger(), config.isWorldGuardEnabled(), config.getWorldGuardDisabledRegions());
        refreshConfigCache();
    }

    private void refreshConfigCache() {
        long now = System.currentTimeMillis();
        if (now - this.lastConfigCacheTime < CONFIG_CACHE_MS) {
            return;
        }
        this.cachedBufferMultiplier = this.config.getAiBufferMultiplier();
        this.cachedBufferDecrease = this.config.getAiBufferDecrease();
        this.cachedAlertThreshold = this.config.getAiAlertThreshold();
        this.cachedBufferFlag = this.config.getAiBufferFlag();
        this.cachedBufferResetOnFlag = this.config.getAiBufferResetOnFlag();
        this.cachedAutoGuiThreshold = this.config.getAutoGuiThreshold();
        this.cachedAvgParams = this.config.getAvgParameters();
        this.lastConfigCacheTime = now;
    }

    public void onAttack(Player player, Entity target) {
        if (!this.config.isAiEnabled() || !(target instanceof Player)) {
            return;
        }
        if (this.worldGuardCompat.shouldBypassAICheck(player)) {
            this.plugin.debug("[AI] Skipping attack for " + player.getName() + " - in disabled WorldGuard region");
        } else {
            if (!player.isValid()) {
                return;
            }
            this.schedulerAdapter.runEntitySync(player, () -> {
                AIPlayerData data = getOrCreatePlayerData(player);
                if (!data.isInCombat()) {
                    data.clearBuffer();
                    data.getAimProcessor().reset();
                    this.plugin.debug("[AI] New combat started for " + player.getName() + ", cleared old data");
                }
                data.onAttack();
                this.plugin.debug("[AI] Attack registered for " + player.getName() + ", buffer=" + data.getBufferSize() + "/" + this.sequence);
            });
        }
    }

    public void onTeleport(Player player) {
        if (!this.config.isAiEnabled() || !player.isValid()) {
            return;
        }
        this.schedulerAdapter.runEntitySync(player, () -> {
            AIPlayerData data = this.playerData.get(player.getUniqueId());
            if (data != null) {
                data.onTeleport();
                this.plugin.debug("[AI] Teleport registered for " + player.getName() + ", resetting data");
            }
        });
    }

    public void onTick(Player player) {
        if (!this.config.isAiEnabled() || !isClientAvailable() || !player.isValid()) {
            return;
        }
        this.schedulerAdapter.runEntitySync(player, () -> {
            AIPlayerData data = getOrCreatePlayerData(player);
            data.incrementTicksSinceAttack();
            if (data.getTicksSinceAttack() > this.sequence) {
                if (!data.isPendingRequest() && data.getBufferSize() >= this.sequence) {
                    this.plugin.debug("[AI] Combat ended for " + player.getName() + ", sending final buffer (" + data.getBufferSize() + " ticks)");
                    data.setPendingRequest(true);
                    sendDataToAI(player, data);
                }
                if (!data.isPendingRequest() && data.getTicksSinceAttack() > this.sequence * 2 && data.getBufferSize() > 0) {
                    data.clearBuffer();
                }
                data.resetStepCounter();
            }
        });
    }

    public void onRotationPacket(Player player, float yaw, float pitch) {
        if (!this.config.isAiEnabled() || !isClientAvailable() || !player.isValid()) {
            return;
        }
        this.schedulerAdapter.runEntitySync(player, () -> {
            AIPlayerData data = getOrCreatePlayerData(player);
            if (!data.isInCombat()) {
                return;
            }
            if (this.worldGuardCompat.shouldBypassAICheck(player)) {
                this.plugin.debug("[AI] Skipping rotation for " + player.getName() + " - in disabled WorldGuard region");
                return;
            }
            data.processTick(yaw, pitch);
            data.incrementStepCounter();
            if (data.shouldSendData(this.step, this.sequence)) {
                data.setPendingRequest(true);
                sendDataToAI(player, data);
                data.resetStepCounter();
            }
        });
    }

    private void sendDataToAI(Player player, AIPlayerData data) {
        List<TickData> ticks;
        byte[] serialized;
        List<TickData> ticks2 = data.getTickBuffer();
        if (ticks2.size() < this.sequence) {
            this.plugin.debug("[AI] Not enough ticks for " + player.getName() + ": " + ticks2.size() + "/" + this.sequence);
            return;
        }
        IAIClient client = this.clientProvider.get();
        if (client == null) {
            this.logger.warning("[AI] Client not available, skipping prediction for " + player.getName());
            return;
        }
        this.plugin.debug("[AI] Sending " + ticks2.size() + " ticks for " + player.getName() + " (ticksSinceAttack=" + data.getTicksSinceAttack() + ")");
        if (!this.config.isDebug()) {
            ticks = ticks2;
        } else {
            this.plugin.debug("[AI] === TICK BUFFER START ===");
            int i = 0;
            for (TickData tick : ticks2) {
                this.plugin.debug("[AI] Tick[" + i + "]: dYaw=" + String.format("%.4f", Float.valueOf(tick.deltaYaw)) + ", dPitch=" + String.format("%.4f", Float.valueOf(tick.deltaPitch)) + ", aYaw=" + String.format("%.4f", Float.valueOf(tick.accelYaw)) + ", aPitch=" + String.format("%.4f", Float.valueOf(tick.accelPitch)) + ", jYaw=" + String.format("%.4f", Float.valueOf(tick.jerkYaw)) + ", jPitch=" + String.format("%.4f", Float.valueOf(tick.jerkPitch)) + ", gcdYaw=" + String.format("%.4f", Float.valueOf(tick.gcdErrorYaw)) + ", gcdPitch=" + String.format("%.4f", Float.valueOf(tick.gcdErrorPitch)));
                i++;
                ticks2 = ticks2;
            }
            ticks = ticks2;
            this.plugin.debug("[AI] === TICK BUFFER END ===");
        }
        if (this.config.getServerType() == ServerType.HUGGINGFACE) {
            serialized = FlatBufferSerializer.serializeRaw(ticks);
        } else {
            serialized = FlatBufferSerializer.serialize(ticks);
        }
        UUID playerUuid = player.getUniqueId();
        String playerName = player.getName();
        client.predict(serialized, playerUuid.toString()).thenAccept(response -> {
            processResponse(playerUuid, playerName, data, response);
        }).exceptionally(error -> {
            handleError(playerName, data, error);
            return null;
        });
    }

    private boolean isClientAvailable() {
        return this.clientProvider != null && this.clientProvider.isAvailable();
    }

    private void processResponse(UUID playerUuid, String playerName, AIPlayerData data, AIResponse response) {
        this.schedulerAdapter.runSync(() -> {
            data.setPendingRequest(false);
            data.clearBuffer();
            if (response.getError() != null && response.getError().contains("INVALID_SEQUENCE")) {
                handleInvalidSequence(response.getError());
                return;
            }
            refreshConfigCache();
            double probability = response.getProbability();
            double rot = response.getRotation();
            double aim = response.getAimbot();
            double gcdVal = response.getGcd();
            double snapVal = response.getSnap();
            double smoothVal = response.getSmooth();
            this.plugin.debug("[AI] Response for " + playerName + ": " + response.toString());
            this.probabilityHistory.recordDetection(playerUuid, probability, rot, aim, gcdVal, snapVal, smoothVal);
            data.updateBuffer(probability, this.cachedBufferMultiplier, this.cachedBufferDecrease, this.cachedAlertThreshold);
            data.updateDetectionScores(rot, aim, gcdVal, snapVal, smoothVal);
            int currentVl = this.violationManager.getViolationLevel(playerUuid);
            this.plugin.getGuiManager().addSuspect(playerName, playerUuid, probability, currentVl, rot, aim, gcdVal, snapVal);
            double probability2 = probability;
            if (this.plugin.getCommandHandler() != null) {
                this.plugin.getCommandHandler().sendDopInfo(playerUuid, playerName, probability2, rot, aim, gcdVal, snapVal, smoothVal);
                probability2 = probability2;
            }
            boolean willFlag = data.shouldFlag(this.cachedBufferFlag);
            if (willFlag) {
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    this.violationManager.handleFlag(player, probability2, data.getBuffer());
                } else {
                    this.logger.warning("[AI] Player " + playerName + " went offline before punishment");
                }
                data.resetBuffer(this.cachedBufferResetOnFlag);
                return;
            }
            if (this.alertManager.shouldAlert(probability2)) {
                this.alertManager.sendAlert(playerUuid, playerName, probability2, data.getBuffer(), currentVl);
            }
        });
    }

    private void handleInvalidSequence(String error) {
        int newSequence;
        try {
            String[] parts = error.split(":");
            if (parts.length >= 2 && (newSequence = Integer.parseInt(parts[1].trim())) > 0 && newSequence != this.sequence) {
                this.logger.info("[AI] Updating sequence from " + this.sequence + " to " + newSequence);
                this.sequence = newSequence;
                for (AIPlayerData data : this.playerData.values()) {
                    data.clearBuffer();
                }
            }
        } catch (NumberFormatException e) {
            this.logger.warning("[AI] Failed to parse new sequence from error: " + error);
        }
    }

    private void handleError(String playerName, AIPlayerData data, Throwable error) {
        if (data != null) {
            data.setPendingRequest(false);
        }
        Throwable cause = error.getCause() != null ? error.getCause() : error;
        this.logger.warning("[AI] Error for " + playerName + ": " + cause.getMessage());
    }

    private AIPlayerData getOrCreatePlayerData(Player player) {
        return this.playerData.computeIfAbsent(player.getUniqueId(), uuid -> {
            return new AIPlayerData(uuid, this.sequence);
        });
    }

    public AIPlayerData getPlayerData(UUID playerId) {
        return this.playerData.get(playerId);
    }

    public void handlePlayerQuit(Player player) {
        AIPlayerData data = this.playerData.remove(player.getUniqueId());
        if (data != null) {
            data.fullReset();
        }
    }

    public void clearAll() {
        this.playerData.clear();
    }

    public int getSequence() {
        return this.sequence;
    }

    public int getStep() {
        return this.step;
    }

    public WorldGuardCompat getWorldGuardCompat() {
        return this.worldGuardCompat;
    }
}
