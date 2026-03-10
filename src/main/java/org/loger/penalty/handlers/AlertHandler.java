package org.loger.penalty.handlers;

import org.loger.Permissions;
import org.loger.penalty.ActionHandler;
import org.loger.penalty.ActionType;
import org.loger.penalty.PenaltyContext;
import org.loger.util.ColorUtil;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AlertHandler implements ActionHandler {
    private Set<UUID> alertRecipients;
    private final Logger logger;
    private final JavaPlugin plugin;
    private String alertPrefix = "&6[ALERT] &f";
    private boolean consoleAlerts = true;

    public AlertHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void setAlertPrefix(String prefix) {
        this.alertPrefix = prefix != null ? prefix : "&6[ALERT] &f";
    }

    public void setAlertRecipients(Set<UUID> recipients) {
        this.alertRecipients = recipients;
    }

    public void setConsoleAlerts(boolean enabled) {
        this.consoleAlerts = enabled;
    }

    @Override // org.loger.penalty.ActionHandler
    public void handle(String message, PenaltyContext context) {
        if (message == null || message.isEmpty()) {
            return;
        }
        String formattedMessage = ColorUtil.colorize(this.alertPrefix + message);
        if (this.alertRecipients != null) {
            for (UUID uuid : this.alertRecipients) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline() && canReceiveAlerts(player)) {
                    player.sendMessage(formattedMessage);
                }
            }
        } else {
            for (Player player2 : Bukkit.getOnlinePlayers()) {
                if (canReceiveAlerts(player2)) {
                    player2.sendMessage(formattedMessage);
                }
            }
        }
        if (this.consoleAlerts) {
            this.logger.info(ColorUtil.stripColors(formattedMessage));
        }
    }

    private boolean canReceiveAlerts(Player player) {
        return player.hasPermission(Permissions.ALERTS) || player.hasPermission(Permissions.ADMIN);
    }

    @Override // org.loger.penalty.ActionHandler
    public ActionType getActionType() {
        return ActionType.CUSTOM_ALERT;
    }
}
