package org.loger.alert;

import org.loger.Main;
import org.loger.Permissions;
import org.loger.config.Config;
import org.loger.config.MessageManager;
import org.loger.scheduler.SchedulerAdapter;
import org.loger.scheduler.SchedulerManager;
import org.loger.util.ColorUtil;
import org.loger.webhook.WebhookManager;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class AlertManager {
    private Config config;
    private final Logger logger;
    private final Main plugin;
    private final Map<UUID, Long> lastAlertTime = new ConcurrentHashMap();
    private final Set<UUID> alertsDisabled = new CopyOnWriteArraySet();
    private final SchedulerAdapter scheduler = SchedulerManager.getAdapter();

    public AlertManager(Main plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
    }

    private MessageManager messages() {
        return this.plugin.getMessageManager();
    }

    private String getPrefix() {
        return ColorUtil.colorize(messages().getPrefix());
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public boolean toggleAlerts(Player player) {
        UUID uuid = player.getUniqueId();
        if (this.alertsDisabled.contains(uuid)) {
            this.alertsDisabled.remove(uuid);
            String msg = ColorUtil.colorize(messages().getMessage("alerts-enabled"));
            player.sendMessage(getPrefix() + msg);
            return true;
        }
        this.alertsDisabled.add(uuid);
        String msg2 = ColorUtil.colorize(messages().getMessage("alerts-disabled"));
        player.sendMessage(getPrefix() + msg2);
        return false;
    }

    public void enableAlerts(Player player) {
        this.alertsDisabled.remove(player.getUniqueId());
    }

    public void disableAlerts(Player player) {
        this.alertsDisabled.add(player.getUniqueId());
    }

    public boolean hasAlertsEnabled(Player player) {
        return !this.alertsDisabled.contains(player.getUniqueId());
    }

    private boolean canReceiveAlerts(Player player) {
        if (this.alertsDisabled.contains(player.getUniqueId())) {
            return false;
        }
        return player.hasPermission(Permissions.ALERTS) || player.hasPermission(Permissions.ADMIN);
    }

    public void sendAlert(UUID suspectId, String suspectName, double probability, double buffer, int vl) {
        if (canSendAlert(suspectId)) {
            Player suspect = suspectId != null ? Bukkit.getPlayer(suspectId) : null;
            int ping = suspect != null ? suspect.getPing() : 0;
            String template = messages().getMessage("alert-format", suspectName, probability, buffer, vl);
            if (messages().isMessageEmpty("alert-format")) {
                return;
            }
            String formattedMsg = template.replace("{PING}", String.valueOf(ping)).replace("%ping%", String.valueOf(ping));
            String message = ColorUtil.colorize(formattedMsg);
            this.scheduler.runSync(() -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (canReceiveAlerts(player)) {
                        player.sendMessage(message);
                    }
                }
                if (this.config.isAiConsoleAlerts()) {
                    this.logger.info(ColorUtil.stripColors(message));
                }
            });
            WebhookManager webhookManager = this.plugin.getWebhookManager();
            if (webhookManager != null) {
                webhookManager.sendAlert(suspectId, suspectName, probability, vl, buffer);
            }
            recordAlertTime(suspectId);
        }
    }

    public void sendAlert(String suspectName, double probability, double buffer) {
        sendAlert(null, suspectName, probability, buffer, 0);
    }

    public void sendAlert(String suspectName, double probability, double buffer, int vl) {
        sendAlert(null, suspectName, probability, buffer, vl);
    }

    private boolean canSendAlert(UUID suspectId) {
        int delayTicks;
        Long lastTime;
        if (suspectId == null || (delayTicks = this.config.getAlertTimeDelay()) <= 0 || (lastTime = this.lastAlertTime.get(suspectId)) == null) {
            return true;
        }
        long delayMs = ((long) delayTicks) * 50;
        return System.currentTimeMillis() - lastTime.longValue() >= delayMs;
    }

    private void recordAlertTime(UUID suspectId) {
        if (suspectId != null) {
            this.lastAlertTime.put(suspectId, Long.valueOf(System.currentTimeMillis()));
        }
    }

    public void handlePlayerQuit(Player player) {
    }

    public boolean shouldAlert(double probability) {
        return probability >= this.config.getAiAlertThreshold();
    }

    public double getAlertThreshold() {
        return this.config.getAiAlertThreshold();
    }
}
