package org.loger.penalty;

import org.loger.penalty.handlers.AlertHandler;
import org.loger.penalty.handlers.BanHandler;
import org.loger.penalty.handlers.KickHandler;
import org.loger.penalty.handlers.RawHandler;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class PenaltyExecutor {
    private final AlertHandler alertHandler;
    private final Logger logger;
    private final JavaPlugin plugin;
    private final ActionParser parser = new ActionParser();
    private final PlaceholderProcessor placeholders = new PlaceholderProcessor();
    private final Map<ActionType, ActionHandler> handlers = new EnumMap(ActionType.class);

    public PenaltyExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.alertHandler = new AlertHandler(plugin);
        this.handlers.put(ActionType.BAN, new BanHandler(plugin));
        this.handlers.put(ActionType.KICK, new KickHandler(plugin));
        this.handlers.put(ActionType.CUSTOM_ALERT, this.alertHandler);
        this.handlers.put(ActionType.RAW, new RawHandler(plugin));
    }

    public void execute(String rawCommand, PenaltyContext context) {
        if (rawCommand == null || rawCommand.isEmpty()) {
            return;
        }
        ParsedAction action = this.parser.parse(rawCommand);
        String processedCommand = this.placeholders.process(action.getCommand(), context);
        ActionHandler handler = this.handlers.get(action.getType());
        if (handler != null) {
            handler.handle(processedCommand, context);
        } else {
            this.logger.warning("No handler found for action type: " + action.getType());
        }
    }

    public void setAlertPrefix(String prefix) {
        this.alertHandler.setAlertPrefix(prefix);
    }

    public void setAlertRecipients(Set<UUID> recipients) {
        this.alertHandler.setAlertRecipients(recipients);
    }

    public void setConsoleAlerts(boolean enabled) {
        this.alertHandler.setConsoleAlerts(enabled);
    }

    public void setAnimationEnabled(boolean enabled) {
        ActionHandler banHandler = this.handlers.get(ActionType.BAN);
        if (banHandler instanceof BanHandler) {
            ((BanHandler) banHandler).setAnimationEnabled(enabled);
        }
    }

    public ActionParser getParser() {
        return this.parser;
    }

    public PlaceholderProcessor getPlaceholderProcessor() {
        return this.placeholders;
    }

    public void shutdown() {
        ActionHandler banHandler = this.handlers.get(ActionType.BAN);
        if (banHandler instanceof BanHandler) {
            ((BanHandler) banHandler).shutdown();
        }
    }
}
