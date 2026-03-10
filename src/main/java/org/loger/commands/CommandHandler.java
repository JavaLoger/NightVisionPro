package org.loger.commands;

import org.loger.Main;
import org.loger.Permissions;
import org.loger.alert.AlertManager;
import org.loger.checks.AICheck;
import org.loger.config.Config;
import org.loger.config.Label;
import org.loger.data.AIPlayerData;
import org.loger.data.DataSession;
import org.loger.data.ProbabilityHistory;
import org.loger.gui.GuiManager;
import org.loger.hologram.HologramManager;
import org.loger.scheduler.ScheduledTask;
import org.loger.scheduler.SchedulerManager;
import org.loger.session.ISessionManager;
import org.loger.util.ColorUtil;
import org.loger.violation.ViolationManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private static final UUID ALL_PLAYERS = new UUID(0, 0);
    private final AICheck aiCheck;
    private final AlertManager alertManager;
    private GuiManager guiManager;
    private HologramManager hologramManager;
    private final Main plugin;
    private ProbabilityHistory probabilityHistory;
    private final ISessionManager sessionManager;
    private final Map<UUID, UUID> probTracking = new ConcurrentHashMap();
    private final Map<UUID, ScheduledTask> probTasks = new ConcurrentHashMap();
    private final Map<UUID, UUID> dopInfoTracking = new ConcurrentHashMap();

    public CommandHandler(ISessionManager sessionManager, AlertManager alertManager, AICheck aiCheck, Main plugin) {
        this.sessionManager = sessionManager;
        this.alertManager = alertManager;
        this.aiCheck = aiCheck;
        this.plugin = plugin;
    }

    public void setHologramManager(HologramManager hologramManager) {
        this.hologramManager = hologramManager;
    }

    public void setProbabilityHistory(ProbabilityHistory probabilityHistory) {
        this.probabilityHistory = probabilityHistory;
    }

    public void setGuiManager(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    private Config getConfig() {
        return this.plugin.getPluginConfig();
    }

    private String getPrefix() {
        return ColorUtil.colorize(this.plugin.getMessageManager().getPrefix());
    }

    private String msg(String key) {
        return ColorUtil.colorize(this.plugin.getMessageManager().getMessage(key));
    }

    private String msg(String key, String... replacements) {
        return ColorUtil.colorize(this.plugin.getMessageManager().getMessage(key, replacements));
    }

    private void sendMsg(CommandSender sender, String key) {
        if (this.plugin.getMessageManager().isMessageEmpty(key)) {
            return;
        }
        sender.sendMessage(getPrefix() + msg(key));
    }

    private void sendMsg(CommandSender sender, String key, String... replacements) {
        if (this.plugin.getMessageManager().isMessageEmpty(key)) {
            return;
        }
        sender.sendMessage(getPrefix() + msg(key, replacements));
    }

    public boolean onCommand(CommandSender r6, Command r7, String r8, String[] r9) {
        if (r9.length == 0) {
            sendUsage(r6);
            return true;
        }

        String subCommand = r9[0].toLowerCase();
        
        if (matchesAlias(subCommand, "alerts")) {
            return handleAlerts(r6);
        } else if (matchesAlias(subCommand, "hologram")) {
            return handleHologram(r6);
        } else if (matchesAlias(subCommand, "gui")) {
            return handleGui(r6);
        } else if (matchesAlias(subCommand, "reload")) {
            return handleReload(r6);
        } else if (matchesAlias(subCommand, "stat")) {
            return handleStat(r6, r9);
        } else if (matchesAlias(subCommand, "prob")) {
            return handleProb(r6, r9);
        } else if (matchesAlias(subCommand, "kicklist")) {
            return handleKickList(r6);
        } else if (matchesAlias(subCommand, "dataset")) {
            return handleDataset(r6, r9);
        } else if (matchesAlias(subCommand, "dopinfo")) {
            return handleDopInfo(r6, r9);
        }

        sendUsage(r6);
        return true;
    }

    private boolean handleAlerts(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMsg(sender, "players-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(Permissions.ALERTS)) {
            sendMsg(sender, "no-permission");
            return true;
        }
        this.alertManager.toggleAlerts(player);
        return true;
    }

    private boolean handleHologram(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMsg(sender, "players-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(Permissions.HOLOGRAM)) {
            sendMsg(sender, "no-permission");
            return true;
        }
        if (this.hologramManager == null) {
            player.sendMessage(getPrefix() + ColorUtil.colorize("&cГолограммы не инициализированы"));
            return true;
        }
        boolean enabled = this.hologramManager.toggleHolograms(player);
        if (enabled) {
            sendMsg(sender, "hologram-enabled");
        } else {
            sendMsg(sender, "hologram-disabled");
        }
        return true;
    }

    private boolean handleGui(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMsg(sender, "players-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(Permissions.ADMIN)) {
            sendMsg(sender, "no-permission");
            return true;
        }
        if (this.guiManager == null) {
            player.sendMessage(getPrefix() + ColorUtil.colorize("&cGUI менеджер не инициализирован"));
            return true;
        }
        this.guiManager.openGui(player);
        return true;
    }

    private boolean handleDopInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMsg(sender, "players-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(Permissions.ADMIN)) {
            sendMsg(sender, "no-permission");
            return true;
        }
        UUID viewerId = player.getUniqueId();
        if (this.dopInfoTracking.containsKey(viewerId)) {
            this.dopInfoTracking.remove(viewerId);
            sendMsg(sender, "dopinfo-disabled");
            return true;
        }
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sendMsg(sender, "player-not-found", "{PLAYER}", args[1]);
                return true;
            }
            this.dopInfoTracking.put(viewerId, target.getUniqueId());
            sendMsg(sender, "dopinfo-enabled-player", "{PLAYER}", target.getName());
        } else {
            this.dopInfoTracking.put(viewerId, ALL_PLAYERS);
            sendMsg(sender, "dopinfo-enabled");
        }
        return true;
    }

    public void sendDopInfo(UUID targetId, String targetName, double probability, double rotation, double aimbot, double gcd, double snap, double smooth) {
        for (Map.Entry<UUID, UUID> entry : this.dopInfoTracking.entrySet()) {
            UUID viewerId = entry.getKey();
            UUID watchingTarget = entry.getValue();
            if (ALL_PLAYERS.equals(watchingTarget) || watchingTarget.equals(targetId)) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer == null || !viewer.isOnline()) {
                    this.dopInfoTracking.remove(viewerId);
                } else {
                    String msg = ColorUtil.colorize("&7[&fDopInfo&7] &f" + targetName + " &7| " + ColorUtil.gradientColor(rotation) + "ROT:" + String.format("%.2f", Double.valueOf(rotation)) + " " + ColorUtil.gradientColor(aimbot) + "AIM:" + String.format("%.2f", Double.valueOf(aimbot)) + " " + ColorUtil.gradientColor(gcd) + "GCD:" + String.format("%.2f", Double.valueOf(gcd)) + " " + ColorUtil.gradientColor(snap) + "SNAP:" + String.format("%.2f", Double.valueOf(snap)) + " " + ColorUtil.gradientColor(smooth) + "SMOOTH:" + String.format("%.2f", Double.valueOf(smooth)) + " &7| " + ColorUtil.gradientColor(probability) + "PROB:" + String.format("%.2f", Double.valueOf(probability)));
                    viewer.sendMessage(msg);
                }
            }
        }
    }

    private boolean handleStat(CommandSender sender, String[] args) {
        double probability1Hour;
        double probability6Hours;
        double probabilityAllTime;
        String playTime;
        double d;
        if (!sender.hasPermission(Permissions.STAT)) {
            sendMsg(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            sendMsg(sender, "stat-usage");
            return true;
        }
        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sendMsg(sender, "player-not-found", "%player%", playerName);
            return true;
        }
        long hours = 0;
        int i = 2;
        long days = 0;
        while (i < args.length) {
            if (args[i].equals("-d") && i + 1 < args.length) {
                try {
                    long days2 = Long.parseLong(args[i + 1]);
                    i++;
                    days = days2;
                } catch (NumberFormatException e) {
                }
            } else if (args[i].equals("-h") && i + 1 < args.length) {
                try {
                    long hours2 = Long.parseLong(args[i + 1]);
                    i++;
                    hours = hours2;
                } catch (NumberFormatException e2) {
                }
            }
            i++;
        }
        UUID targetId = target.getUniqueId();
        AIPlayerData data = this.aiCheck.getPlayerData(targetId);
        double probPeriod = 0.0d;
        double currentProb = data != null ? data.getLastProbability() : 0.0d;
        if (this.probabilityHistory != null) {
            probability1Hour = this.probabilityHistory.getProbability1Hour(targetId);
        } else {
            probability1Hour = 0.0d;
        }
        double prob1h = probability1Hour;
        if (this.probabilityHistory != null) {
            probability6Hours = this.probabilityHistory.getProbability6Hours(targetId);
        } else {
            probability6Hours = 0.0d;
        }
        double prob6h = probability6Hours;
        if (this.probabilityHistory != null) {
            probabilityAllTime = this.probabilityHistory.getProbabilityAllTime(targetId);
        } else {
            probabilityAllTime = 0.0d;
        }
        double probAll = probabilityAllTime;
        if (this.probabilityHistory != null) {
            probPeriod = this.probabilityHistory.getProbabilityForPeriod(targetId, hours, days);
        }
        int vl = this.plugin.getViolationManager().getViolationLevel(targetId);
        int ping = target.getPing();
        String playTime2 = target.getAddress() != null ? target.getAddress().getAddress().getHostAddress() : "N/A";
        String playTime3 = this.probabilityHistory != null ? this.probabilityHistory.getFormattedPlayTime(targetId) : "N/A";
        for (String lineTemplate : getConfig().getStatFormat()) {
            AIPlayerData data2 = data;
            String strReplace = lineTemplate.replace("%player%", target.getName());
            String lineTemplate2 = getVLColor(vl);
            int vl2 = vl;
            String strReplace2 = strReplace.replace("%vl%", lineTemplate2 + vl).replace("%ping%", getPingColor(ping) + ping);
            String ip = playTime2;
            String line = strReplace2.replace("%ip%", ip).replace("%play_time%", playTime3);
            if (days > 0 || hours > 0) {
                playTime = playTime3;
                d = probPeriod;
            } else {
                playTime = playTime3;
                d = currentProb;
            }
            sender.sendMessage(ColorUtil.colorize(replaceProbabilityWithColor(replaceProbabilityWithColor(replaceProbabilityWithColor(replaceProbabilityWithColor(line, "%probability%", d), "%probability_1h%", prob1h), "%probability_6h%", prob6h), "%probability_all%", probAll)));
            playTime3 = playTime;
            data = data2;
            vl = vl2;
            playTime2 = ip;
        }
        return true;
    }

    private String replaceProbabilityWithColor(String line, String placeholder, double probability) {
        if (!line.contains(placeholder)) {
            return line;
        }
        String color = getProbabilityColor(probability);
        String probStr = String.format("%.2f", Double.valueOf(probability));
        return line.replace(placeholder, color + probStr);
    }

    private String getProbabilityColor(double probability) {
        return probability >= 0.8d ? "&#FF0000" : probability >= 0.6d ? "&#FF7F00" : probability >= 0.4d ? "&#FFFF00" : "&#00FF00";
    }

    private String getVLColor(int vl) {
        return vl > 10 ? "&#FF0000" : vl > 5 ? "&#FF7F00" : vl > 3 ? "&#FFFF00" : "&#00FF00";
    }

    private String getPingColor(int ping) {
        return ping >= 300 ? "&#FF0000" : ping >= 150 ? "&#FF7F00" : ping >= 70 ? "&#FFFF00" : "&#00FF00";
    }

    private boolean handleDataset(CommandSender r7, String[] r8) {
        if (!r7.hasPermission(Permissions.DATASET)) {
            sendMsg(r7, "no-permission");
            return true;
        }
        if (r8.length < 2) {
            r7.sendMessage(getPrefix() + ColorUtil.colorize("&fИспользование: /nvp dataset <start|stop|status>"));
            return true;
        }
        String sub = r8[1].toLowerCase();
        if (sub.equals("start")) {
            return handleDatasetStart(r7, r8);
        } else if (sub.equals("stop")) {
            return handleDatasetStop(r7, r8);
        } else if (sub.equals("status")) {
            return handleDataStatus(r7);
        }
        r7.sendMessage(getPrefix() + ColorUtil.colorize("&fИспользование: /nvp dataset <start|stop|status>"));
        return true;
    }

    private boolean handleDatasetStart(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(getPrefix() + ColorUtil.colorize("&fИспользование: /nvp dataset start <ник> <CHEAT|LEGIT|UNLABELED>"));
            return true;
        }
        String playerName = args[1];
        String labelStr = args[2];
        Label sessionLabel = Label.fromString(labelStr);
        if (sessionLabel == null) {
            sendMsg(sender, "invalid-label", "{LABEL}", labelStr);
            sendMsg(sender, "valid-labels");
            return true;
        }
        String comment = parseComment(args, 3);
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sendMsg(sender, "player-not-found", "{PLAYER}", playerName);
            return true;
        }
        this.sessionManager.startSession(player, sessionLabel, comment);
        sendMsg(sender, "session-started", "{LABEL}", sessionLabel.name(), "{PLAYER}", player.getName());
        return true;
    }

    private boolean handleDatasetStop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(getPrefix() + ColorUtil.colorize("&fИспользование: /nvp dataset stop <ник>"));
            return true;
        }
        String playerName = args[1];
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sendMsg(sender, "player-not-found", "%player%", playerName);
            return true;
        }
        if (!this.sessionManager.hasActiveSession(player)) {
            sendMsg(sender, "no-sessions-to-stop");
            return true;
        }
        this.sessionManager.stopSession(player);
        sendMsg(sender, "session-stopped", "%player%", player.getName());
        return true;
    }

    private boolean handleProb(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMsg(sender, "players-only");
            return true;
        }
        Player admin = (Player) sender;
        if (!admin.hasPermission(Permissions.PROB)) {
            sendMsg(sender, "no-permission");
            return true;
        }
        if (this.probTracking.containsKey(admin.getUniqueId())) {
            stopTracking(admin);
            sendMsg(sender, "tracking-stopped");
            return true;
        }
        if (args.length < 2) {
            sendMsg(sender, "prob-usage");
            return true;
        }
        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sendMsg(sender, "player-not-found", "%player%", playerName);
            return true;
        }
        startTracking(admin, target);
        sendMsg(sender, "tracking-started", "%player%", target.getName());
        return true;
    }

    private void startTracking(Player admin, Player target) {
        UUID adminId = admin.getUniqueId();
        UUID targetId = target.getUniqueId();
        stopTracking(admin);
        this.probTracking.put(adminId, targetId);
        ScheduledTask task = SchedulerManager.getAdapter().runSyncRepeating(() -> {
            String message;
            Player adminPlayer = Bukkit.getPlayer(adminId);
            Player targetPlayer = Bukkit.getPlayer(targetId);
            if (adminPlayer == null || !adminPlayer.isOnline()) {
                stopTracking(adminId);
                return;
            }
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                sendActionBar(adminPlayer, msg("player-offline"));
                stopTracking(adminId);
                return;
            }
            AIPlayerData data = this.aiCheck.getPlayerData(targetId);
            if (data == null) {
                message = ColorUtil.colorize("&7" + targetPlayer.getName() + ": &eНет данных");
            } else {
                double prob = data.getLastProbability();
                double buffer = data.getBuffer();
                int vl = this.plugin.getViolationManager().getViolationLevel(targetId);
                message = ColorUtil.colorize(this.plugin.getMessageManager().getMessage("actionbar-format", targetPlayer.getName(), prob, buffer, vl));
            }
            sendActionBar(adminPlayer, message);
        }, 0L, 10L);
        this.probTasks.put(adminId, task);
    }

    private void stopTracking(Player admin) {
        stopTracking(admin.getUniqueId());
    }

    private void stopTracking(UUID adminId) {
        this.probTracking.remove(adminId);
        ScheduledTask task = this.probTasks.remove(adminId);
        if (task != null) {
            task.cancel();
        }
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission(Permissions.RELOAD)) {
            sendMsg(sender, "no-permission");
            return true;
        }
        this.plugin.reloadPluginConfig();
        sendMsg(sender, "config-reloaded");
        return true;
    }

    private boolean handleKickList(CommandSender sender) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sendMsg(sender, "no-permission");
            return true;
        }
        List<ViolationManager.KickRecord> kicks = this.plugin.getViolationManager().getKickHistory();
        if (kicks.isEmpty()) {
            sender.sendMessage(getPrefix() + ColorUtil.colorize("&7Нет киков от AI античита"));
            return true;
        }
        sender.sendMessage(getPrefix() + ColorUtil.colorize("&6Последние кики от AI античита:"));
        sender.sendMessage(ColorUtil.colorize("&7─────────────────────────────────"));
        int index = 1;
        for (ViolationManager.KickRecord kick : kicks) {
            sender.sendMessage(ColorUtil.colorize(String.format("&e%d. &f%s &7[&c%s&7] &8- &bProb: &f%.2f &8| &bBuf: &f%.1f &8| &bVL: &f%d", Integer.valueOf(index), kick.getPlayerName(), kick.getFormattedTime(), Double.valueOf(kick.getProbability()), Double.valueOf(kick.getBuffer()), Integer.valueOf(kick.getVl()))));
            index++;
        }
        sender.sendMessage(ColorUtil.colorize("&7─────────────────────────────────"));
        return true;
    }

    private boolean handleDataStatus(CommandSender sender) {
        if (!sender.hasPermission(Permissions.ADMIN) && !sender.hasPermission(Permissions.DATASET)) {
            sendMsg(sender, "no-permission");
            return true;
        }
        int activeSessions = this.sessionManager.getActiveSessionCount();
        sender.sendMessage(getPrefix() + msg("data-status-header"));
        sendMsg(sender, "active-sessions", "%count%", String.valueOf(activeSessions));
        if (activeSessions > 0) {
            sender.sendMessage(ColorUtil.colorize("&7Игроки собирающие данные:"));
            for (DataSession session : this.sessionManager.getActiveSessions()) {
                Player player = Bukkit.getPlayer(session.getUuid());
                String playerName = player != null ? player.getName() : session.getPlayerName();
                String sessionLabel = session.getLabel().name();
                String comment = session.getComment();
                boolean inCombat = session.isInCombat();
                int tickCount = session.getTickCount();
                sender.sendMessage(ColorUtil.colorize("&b  " + playerName + "&7 [&e" + sessionLabel + "&7]" + (comment.isEmpty() ? "" : " \"" + comment + "\"")));
                sender.sendMessage(ColorUtil.colorize("&7    Тики: &a" + tickCount + "&7 | В бою: " + (inCombat ? "&aДа" : "&cНет")));
            }
        } else {
            sendMsg(sender, "no-active-sessions");
            sendMsg(sender, "start-hint");
        }
        return true;
    }

    private String parseComment(String[] args, int startIndex) {
        if (startIndex >= args.length) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(args[i]);
        }
        String comment = sb.toString();
        if (comment.startsWith("\"") && comment.endsWith("\"") && comment.length() >= 2) {
            comment = comment.substring(1, comment.length() - 1);
        } else if (comment.startsWith("\"")) {
            comment = comment.substring(1);
        }
        return comment.trim();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ColorUtil.colorize("&7&m----------------------------------------"));
        sender.sendMessage(ColorUtil.colorize("&x&F&B&0&8&0&8&lNEO&f&lNVP &7- &fКоманды:"));
        sender.sendMessage(ColorUtil.colorize(" &x&F&B&0&8&0&8/nvp alerts &7- &fВкл/Выкл уведомления"));
        sender.sendMessage(ColorUtil.colorize(" &x&F&B&0&8&0&8/nvp stat <игрок> &7- &fСтатистика игрока"));
        sender.sendMessage(ColorUtil.colorize(" &x&F&B&0&8&0&8/nvp reload &7- &fПерезагрузить конфиг"));
        sender.sendMessage(ColorUtil.colorize(" &x&F&B&0&8&0&8/nvp gui &7- &fОткрыть GUI"));
        sender.sendMessage(ColorUtil.colorize("&7&m----------------------------------------"));
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        Config config = getConfig();
        if (args.length == 1) {
            List<String> commands = Arrays.asList(config.getCommandAlias("alerts"), config.getCommandAlias("prob"), config.getCommandAlias("reload"), config.getCommandAlias("kicklist"), config.getCommandAlias("hologram"), config.getCommandAlias("stat"), config.getCommandAlias("dataset"), config.getCommandAlias("gui"), config.getCommandAlias("dopinfo"));
            completions.addAll(filterStartsWith(commands, args[0]));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (matchesAlias(subCommand, "prob") || matchesAlias(subCommand, "stat") || matchesAlias(subCommand, "dopinfo")) {
                completions.addAll(filterStartsWith(getOnlinePlayerNames(), args[1]));
            } else if (matchesAlias(subCommand, "dataset")) {
                completions.addAll(filterStartsWith(Arrays.asList("start", "stop", "status"), args[1]));
            }
        } else if (args.length == 3) {
            String subCommand2 = args[0].toLowerCase();
            if (matchesAlias(subCommand2, "dataset")) {
                String datasetCmd = args[1].toLowerCase();
                if (datasetCmd.equals("start") || datasetCmd.equals("stop")) {
                    completions.addAll(filterStartsWith(getOnlinePlayerNames(), args[2]));
                }
            } else if (matchesAlias(subCommand2, "stat")) {
                completions.addAll(Arrays.asList("-d", "-h"));
            }
        } else if (args.length == 4) {
            String subCommand3 = args[0].toLowerCase();
            if (matchesAlias(subCommand3, "dataset") && args[1].equalsIgnoreCase("start")) {
                List<String> labels = (List) Arrays.stream(Label.values()).map((v0) -> {
                    return v0.name();
                }).collect(Collectors.toList());
                completions.addAll(filterStartsWith(labels, args[3]));
            } else if (matchesAlias(subCommand3, "stat")) {
                completions.addAll(Arrays.asList("1", "7", "30"));
            }
        }
        return completions;
    }

    private List<String> getOnlinePlayerNames() {
        return (List) Bukkit.getOnlinePlayers().stream().map((v0) -> {
            return v0.getName();
        }).collect(Collectors.toList());
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        String lowerPrefix = prefix.toLowerCase();
        return (List) options.stream().filter(option -> {
            return option.toLowerCase().startsWith(lowerPrefix);
        }).collect(Collectors.toList());
    }

    private boolean matchesAlias(String input, String internalName) {
        if (input.equalsIgnoreCase(internalName)) {
            return true;
        }
        String alias = getConfig().getCommandAlias(internalName);
        return input.equalsIgnoreCase(alias);
    }

    public void cleanup() {
        for (ScheduledTask task : this.probTasks.values()) {
            task.cancel();
        }
        this.probTasks.clear();
        this.probTracking.clear();
    }
}
