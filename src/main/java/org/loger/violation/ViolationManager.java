package org.loger.violation;

import org.loger.Main;
import org.loger.alert.AlertManager;
import org.loger.checks.AICheck;
import org.loger.config.Config;
import org.loger.data.AIPlayerData;
import org.loger.gui.GuiManager;
import org.loger.penalty.ActionType;
import org.loger.penalty.PenaltyContext;
import org.loger.penalty.PenaltyExecutor;
import org.loger.scheduler.ScheduledTask;
import org.loger.scheduler.SchedulerManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public class ViolationManager {
    private static final long CONFIG_CACHE_MS = 2000;
    private static final int MAX_KICK_HISTORY = 10;
    private static final long PUNISHMENT_COOLDOWN_MS = 5000;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private AICheck aiCheck;
    private final AlertManager alertManager;
    private volatile List<String> cachedAvgParams;
    private volatile double cachedMinProbability;
    private volatile Map<Integer, String> cachedPunishmentCommands;
    private Config config;
    private ScheduledTask decayTask;
    private GuiManager guiManager;
    private final PenaltyExecutor penaltyExecutor;
    private final Main plugin;
    private volatile long lastConfigUpdate = 0;
    private final Map<UUID, Integer> violationLevels = new ConcurrentHashMap();
    private final LinkedList<KickRecord> kickHistory = new LinkedList<>();
    private final Map<UUID, Long> lastPunishmentTime = new ConcurrentHashMap();
    private final Map<UUID, Integer> lastAlertVl = new ConcurrentHashMap();

    public static class KickRecord {
        private final double buffer;
        private final String command;
        private final String playerName;
        private final double probability;
        private final LocalDateTime time = LocalDateTime.now();
        private final int vl;

        public KickRecord(String playerName, double probability, double buffer, int vl, String command) {
            this.playerName = playerName;
            this.probability = probability;
            this.buffer = buffer;
            this.vl = vl;
            this.command = command;
        }

        public String getPlayerName() {
            return this.playerName;
        }

        public double getProbability() {
            return this.probability;
        }

        public double getBuffer() {
            return this.buffer;
        }

        public int getVl() {
            return this.vl;
        }

        public LocalDateTime getTime() {
            return this.time;
        }

        public String getCommand() {
            return this.command;
        }

        public String getFormattedTime() {
            return this.time.format(ViolationManager.TIME_FORMATTER);
        }
    }

    public ViolationManager(Main plugin, Config config, AlertManager alertManager) {
        this.plugin = plugin;
        this.config = config;
        this.alertManager = alertManager;
        this.penaltyExecutor = new PenaltyExecutor(plugin);
        updatePenaltyExecutorConfig();
        startDecayTask();
    }

    public void setAICheck(AICheck aiCheck) {
        this.aiCheck = aiCheck;
    }

    public void setGuiManager(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    private void startDecayTask() {
        stopDecayTask();
        if (!this.config.isVlDecayEnabled()) {
            return;
        }
        int intervalTicks = this.config.getVlDecayIntervalSeconds() * 20;
        this.decayTask = SchedulerManager.getAdapter().runSyncRepeating(this::processDecay, intervalTicks, intervalTicks);
        this.plugin.debug("[VL] Decay task started with interval " + this.config.getVlDecayIntervalSeconds() + "s");
    }

    private void stopDecayTask() {
        if (this.decayTask != null) {
            this.decayTask.cancel();
            this.decayTask = null;
        }
    }

    private void processDecay() {
        if (this.aiCheck == null) {
            return;
        }
        int decayAmount = this.config.getVlDecayAmount();
        for (Map.Entry<UUID, Integer> entry : this.violationLevels.entrySet()) {
            UUID playerId = entry.getKey();
            AIPlayerData playerData = this.aiCheck.getPlayerData(playerId);
            if (playerData == null || !playerData.isInCombat()) {
                int oldVl = entry.getValue().intValue();
                int newVl = oldVl - decayAmount;
                if (newVl <= 0) {
                    this.violationLevels.remove(playerId);
                    this.plugin.debug("[VL] Decay: removed VL for " + playerId + " (was " + oldVl + ")");
                } else {
                    this.violationLevels.put(playerId, Integer.valueOf(newVl));
                    this.plugin.debug("[VL] Decay: " + playerId + " VL " + oldVl + " -> " + newVl);
                }
            }
        }
    }

    private void updatePenaltyExecutorConfig() {
        this.penaltyExecutor.setConsoleAlerts(this.config.isAiConsoleAlerts());
    }

    public void setConfig(Config config) {
        this.config = config;
        updatePenaltyExecutorConfig();
        startDecayTask();
        refreshConfigCache();
    }

    private void refreshConfigCache() {
        long now = System.currentTimeMillis();
        if (now - this.lastConfigUpdate < CONFIG_CACHE_MS) {
            return;
        }
        this.cachedPunishmentCommands = this.config.getPunishmentCommands();
        this.cachedAvgParams = this.config.getAvgParameters();
        this.cachedMinProbability = this.config.getAiPunishmentMinProbability();
        this.lastConfigUpdate = now;
    }

    public void handleFlag(Player player, double probability, double buffer) {
        UUID uuid;
        refreshConfigCache();
        if (probability < this.cachedMinProbability) {
            return;
        }
        UUID uuid2 = player.getUniqueId();
        long now = System.currentTimeMillis();
        int newVl = incrementViolationLevel(uuid2);
        Integer prevVl = this.lastAlertVl.get(uuid2);
        if (prevVl == null || prevVl.intValue() != newVl) {
            this.alertManager.sendAlert(uuid2, player.getName(), probability, buffer, newVl);
            newVl = newVl;
            this.lastAlertVl.put(uuid2, Integer.valueOf(newVl));
        }
        this.plugin.debug("[AI] " + player.getName() + " flagged - VL: " + newVl + ", Prob: " + String.format("%.2f", Double.valueOf(probability)) + ", Buffer: " + String.format("%.1f", Double.valueOf(buffer)));
        if (this.config.isWebhookEnabled() && newVl % this.config.getWebhookVl() == 0) {
            double avg = calculateAvg(uuid2);
            this.plugin.getWebhookManager().sendAlert(uuid2, player.getName(), probability, newVl, avg);
        }
        if (!this.config.isGuiEnabled() || newVl % this.config.getGuiVl() != 0 || this.guiManager == null) {
            uuid = uuid2;
        } else {
            AIPlayerData playerData = this.aiCheck.getPlayerData(uuid2);
            double rot = playerData != null ? playerData.getLastRotation() : 0.0d;
            double aim = playerData != null ? playerData.getLastAimbot() : 0.0d;
            double g = playerData != null ? playerData.getLastGcd() : 0.0d;
            double sn = playerData != null ? playerData.getLastSnap() : 0.0d;
            this.guiManager.addSuspect(player.getName(), uuid2, probability, newVl, rot, aim, g, sn);
            uuid = uuid2;
        }
        String command = getApplicablePunishmentCommandFast(newVl);
        if (command != null) {
            ActionType actionType = ActionType.fromCommand(command);
            if (actionType.isPunishment()) {
                Long previousTime = this.lastPunishmentTime.get(uuid);
                if (previousTime != null && now - previousTime.longValue() < PUNISHMENT_COOLDOWN_MS) {
                    this.plugin.debug("[AI] " + player.getName() + " punishment on cooldown, skipping " + actionType);
                    return;
                }
                this.lastPunishmentTime.put(uuid, Long.valueOf(now));
            }
            executeCommand(command, player, probability, buffer, newVl);
        }
    }

    public int incrementViolationLevel(UUID playerId) {
        return this.violationLevels.merge(playerId, 1, (v0, v1) -> {
            return Integer.sum(v0, v1);
        }).intValue();
    }

    public int getViolationLevel(UUID playerId) {
        return this.violationLevels.getOrDefault(playerId, 0).intValue();
    }

    public void resetViolationLevel(UUID playerId) {
        this.violationLevels.remove(playerId);
    }

    public String getApplicablePunishmentCommand(int vl) {
        refreshConfigCache();
        return getApplicablePunishmentCommandFast(vl);
    }

    private String getApplicablePunishmentCommandFast(int vl) {
        Map<Integer, String> commands = this.cachedPunishmentCommands;
        if (commands == null || commands.isEmpty() || vl <= 0) {
            return null;
        }
        int bestThreshold = -1;
        Iterator<Integer> it = commands.keySet().iterator();
        while (it.hasNext()) {
            int threshold = it.next().intValue();
            if (threshold > 0 && vl % threshold == 0 && threshold > bestThreshold) {
                bestThreshold = threshold;
            }
        }
        if (bestThreshold > 0) {
            return commands.get(Integer.valueOf(bestThreshold));
        }
        return null;
    }

    public void executeCommand(String command, Player player, double probability, double buffer, int vl) {
        PenaltyContext context = PenaltyContext.builder().playerName(player.getName()).violationLevel(vl).probability(probability).buffer(buffer).build();
        addKickRecord(new KickRecord(player.getName(), probability, buffer, vl, command));
        this.penaltyExecutor.execute(command, context);
    }

    private synchronized void addKickRecord(KickRecord record) {
        this.kickHistory.addFirst(record);
        while (this.kickHistory.size() > 10) {
            this.kickHistory.removeLast();
        }
    }

    public synchronized List<KickRecord> getKickHistory() {
        return Collections.unmodifiableList(new ArrayList(this.kickHistory));
    }

    public synchronized KickRecord getKickRecord(int index) {
        if (index >= 0) {
            if (index < this.kickHistory.size()) {
                return this.kickHistory.get(index);
            }
        }
        return null;
    }

    public synchronized KickRecord removeKickRecord(int index) {
        if (index >= 0) {
            if (index < this.kickHistory.size()) {
                return this.kickHistory.remove(index);
            }
        }
        return null;
    }

    public PenaltyExecutor getPenaltyExecutor() {
        return this.penaltyExecutor;
    }

    public void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        this.lastPunishmentTime.remove(uuid);
        this.lastAlertVl.remove(uuid);
    }

    public void decreaseViolationLevel(UUID playerId, int amount) {
        this.violationLevels.computeIfPresent(playerId, (k, v) -> {
            int newVl = v.intValue() - amount;
            if (newVl <= 0) {
                return null;
            }
            return Integer.valueOf(newVl);
        });
    }

    public void clearAll() {
        this.violationLevels.clear();
        this.lastPunishmentTime.clear();
        this.lastAlertVl.clear();
        synchronized (this) {
            this.kickHistory.clear();
        }
    }

    public void shutdown() {
        stopDecayTask();
        clearAll();
        this.penaltyExecutor.shutdown();
    }

    private double calculateAvg(UUID uuid) {
        AIPlayerData data = this.aiCheck.getPlayerData(uuid);
        if (data == null) {
            return 0.0;
        }
        
        List<String> avgParams = this.config.getAvgParameters();
        double probability = data.getLastProbability();
        double rotation = data.getLastRotation();
        double aimbot = data.getLastAimbot();
        double gcd = data.getLastGcd();
        double snap = data.getLastSnap();
        double smooth = data.getLastSmooth();
        
        return org.loger.util.GcdMath.calculateAvg(probability, rotation, aimbot, gcd, snap, smooth, avgParams);
    }
}
