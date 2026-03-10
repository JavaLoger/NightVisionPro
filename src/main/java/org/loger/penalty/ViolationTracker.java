package org.loger.penalty;

import org.loger.alert.AlertManager;
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
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ViolationTracker {
    private static final int MAX_PENALTY_HISTORY = 10;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final AlertManager alertManager;
    private final PenaltyExecutor executor;
    private final Logger logger;
    private final JavaPlugin plugin;
    private double minProbability = 0.85d;
    private Map<Integer, String> penaltyCommands = new ConcurrentHashMap();
    private final Map<UUID, Integer> levels = new ConcurrentHashMap();
    private final LinkedList<PenaltyRecord> history = new LinkedList<>();

    public static class PenaltyRecord {
        private final ActionType actionType;
        private final String command;
        private final String playerName;
        private final double probability;
        private final LocalDateTime timestamp = LocalDateTime.now();
        private final int violationLevel;

        public PenaltyRecord(String playerName, ActionType actionType, int vl, double probability, String command) {
            this.playerName = playerName;
            this.actionType = actionType;
            this.violationLevel = vl;
            this.probability = probability;
            this.command = command;
        }

        public String getPlayerName() {
            return this.playerName;
        }

        public ActionType getActionType() {
            return this.actionType;
        }

        public int getViolationLevel() {
            return this.violationLevel;
        }

        public double getProbability() {
            return this.probability;
        }

        public LocalDateTime getTimestamp() {
            return this.timestamp;
        }

        public String getCommand() {
            return this.command;
        }

        public String getFormattedTime() {
            return this.timestamp.format(ViolationTracker.TIME_FORMAT);
        }
    }

    public ViolationTracker(JavaPlugin plugin, AlertManager alertManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.alertManager = alertManager;
        this.executor = new PenaltyExecutor(plugin);
    }

    public void recordViolation(Player player, double probability, double buffer) {
        if (probability < this.minProbability) {
            return;
        }
        UUID uuid = player.getUniqueId();
        int newLevel = incrementLevel(uuid);
        this.alertManager.sendAlert(player.getName(), probability, buffer, newLevel);
        this.logger.info("[Penalty] " + player.getName() + " - VL: " + newLevel + ", Prob: " + String.format("%.2f", Double.valueOf(probability)) + ", Buffer: " + String.format("%.1f", Double.valueOf(buffer)));
        String command = findPenaltyCommand(newLevel);
        if (command != null) {
            executePenalty(command, player, probability, buffer, newLevel);
        }
    }

    public int incrementLevel(UUID playerId) {
        return this.levels.merge(playerId, 1, (v0, v1) -> {
            return Integer.sum(v0, v1);
        }).intValue();
    }

    public int getLevel(UUID playerId) {
        return this.levels.getOrDefault(playerId, 0).intValue();
    }

    public void resetLevel(UUID playerId) {
        this.levels.remove(playerId);
    }

    public void decreaseLevel(UUID playerId, int amount) {
        this.levels.computeIfPresent(playerId, (k, v) -> {
            int newLevel = v.intValue() - amount;
            if (newLevel <= 0) {
                return null;
            }
            return Integer.valueOf(newLevel);
        });
    }

    public String findPenaltyCommand(int vl) {
        if (this.penaltyCommands.isEmpty()) {
            return null;
        }
        if (this.penaltyCommands.containsKey(Integer.valueOf(vl))) {
            return this.penaltyCommands.get(Integer.valueOf(vl));
        }
        int maxThreshold = -1;
        int applicableThreshold = -1;
        Iterator<Integer> it = this.penaltyCommands.keySet().iterator();
        while (it.hasNext()) {
            int threshold = it.next().intValue();
            if (threshold > maxThreshold) {
                maxThreshold = threshold;
            }
            if (threshold <= vl && threshold > applicableThreshold) {
                applicableThreshold = threshold;
            }
        }
        if (applicableThreshold == -1 && vl > maxThreshold) {
            return this.penaltyCommands.get(Integer.valueOf(maxThreshold));
        }
        if (applicableThreshold > 0) {
            return this.penaltyCommands.get(Integer.valueOf(applicableThreshold));
        }
        return null;
    }

    private void executePenalty(String command, Player player, double probability, double buffer, int vl) {
        PenaltyContext context = PenaltyContext.builder().playerName(player.getName()).violationLevel(vl).probability(probability).buffer(buffer).build();
        ActionType type = ActionType.fromCommand(command);
        addToHistory(new PenaltyRecord(player.getName(), type, vl, probability, command));
        this.executor.execute(command, context);
    }

    private synchronized void addToHistory(PenaltyRecord record) {
        this.history.addFirst(record);
        while (this.history.size() > 10) {
            this.history.removeLast();
        }
    }

    public synchronized List<PenaltyRecord> getHistory() {
        return Collections.unmodifiableList(new ArrayList(this.history));
    }

    public void handlePlayerQuit(Player player) {
    }

    public void clearAll() {
        this.levels.clear();
        synchronized (this) {
            this.history.clear();
        }
    }

    public void setMinProbability(double minProb) {
        this.minProbability = minProb;
    }

    public void setPenaltyCommands(Map<Integer, String> commands) {
        this.penaltyCommands.clear();
        if (commands != null) {
            this.penaltyCommands.putAll(commands);
        }
    }

    public void setAlertPrefix(String prefix) {
        this.executor.setAlertPrefix(prefix);
    }

    public void setConsoleAlerts(boolean enabled) {
        this.executor.setConsoleAlerts(enabled);
    }

    public PenaltyExecutor getExecutor() {
        return this.executor;
    }
}
