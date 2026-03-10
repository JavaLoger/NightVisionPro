package org.loger.gui;

import org.loger.Main;
import org.loger.config.Config;
import org.loger.data.ProbabilityHistory;
import org.loger.gui.MenuConfig;
import org.loger.scheduler.SchedulerManager;
import org.loger.util.ColorUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class GuiManager implements Listener {
    private static final long AVG_PARAMS_CACHE_MS = 5000;
    private volatile List<String> cachedAvgParams;
    private final MenuConfig menuConfig;
    private final Main plugin;
    private final SyncManager syncManager;
    private final Map<UUID, Integer> playerPages = new ConcurrentHashMap();
    private String serverName = "default";
    private volatile long lastAvgParamsUpdate = 0;
    private final Map<UUID, ItemStack> skullCache = new ConcurrentHashMap();
    private final Set<UUID> loadingProfiles = ConcurrentHashMap.newKeySet();
    private final ExecutorService textureLoader = Executors.newFixedThreadPool(2);
    private final Map<UUID, Map<Integer, String>> viewerSlotMap = new ConcurrentHashMap();

    public GuiManager(Main plugin) {
        this.plugin = plugin;
        this.menuConfig = new MenuConfig(plugin);
        Config config = plugin.getPluginConfig();
        String syncType = config.getSyncType();
        if ("redis".equalsIgnoreCase(syncType)) {
            this.syncManager = new JsonSyncManager(plugin.getDataFolder(), plugin.getLogger());
            plugin.getLogger().info("[GUI] Redis sync not implemented, using local JSON storage");
        } else if ("mysql".equalsIgnoreCase(syncType)) {
            this.syncManager = new JsonSyncManager(plugin.getDataFolder(), plugin.getLogger());
            plugin.getLogger().info("[GUI] MySQL sync not implemented, using local JSON storage");
        } else {
            this.syncManager = new JsonSyncManager(plugin.getDataFolder(), plugin.getLogger());
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void addSuspect(String playerName, UUID playerId, double probability, int vl, double rot, double aim, double g, double sn) {
        this.syncManager.getSuspect(playerId).thenAccept(existing -> {
            SuspectData data;
            if (existing != null) {
                data = existing;
            } else {
                data = new SuspectData(playerId, playerName);
            }
            data.updateData(probability, vl);
            data.addCritValue(rot, aim, g, sn);
            data.setServer(this.serverName);
            fillHistoryData(data, playerId);
            if (existing != null) {
                this.syncManager.updateSuspect(data);
            } else {
                this.syncManager.addSuspect(data);
            }
            cacheSkullAsync(playerId);
        });
    }

    public void addSuspectAuto(String playerName, UUID playerId, double probability, int vl, double rot, double aim, double g, double sn) {
        addSuspect(playerName, playerId, probability, vl, rot, aim, g, sn);
    }

    private void fillHistoryData(SuspectData data, UUID playerId) {
        ProbabilityHistory history = this.plugin.getProbabilityHistory();
        if (history == null) {
            return;
        }
        data.updateProbabilityHistory(history.getProbability1Hour(playerId), history.getProbability6Hours(playerId), history.getProbabilityAllTime(playerId));
        ProbabilityHistory.PlayerScores scores = history.getPlayerScores(playerId);
        data.updateScores(scores.rotation, scores.aimbot, scores.gcd, scores.snap, scores.smooth);
        data.updateMaxScores(history.getRotationMax1Hour(playerId), history.getAimbotMax1Hour(playerId), history.getGcdMax1Hour(playerId), history.getSnapMax1Hour(playerId), history.getSmoothMax1Hour(playerId));
        data.updateMaxScoresAll(history.getRotationMaxAllTime(playerId), history.getAimbotMaxAllTime(playerId), history.getGcdMaxAllTime(playerId), history.getSnapMaxAllTime(playerId), history.getSmoothMaxAllTime(playerId));
    }

    private void cacheSkullAsync(UUID playerId) {
        if (!this.skullCache.containsKey(playerId) && this.loadingProfiles.add(playerId)) {
            this.textureLoader.submit(() -> {
                try {
                    try {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta meta = (SkullMeta) skull.getItemMeta();
                        if (meta != null) {
                            meta.setOwningPlayer(offlinePlayer);
                            skull.setItemMeta(meta);
                            this.skullCache.put(playerId, skull);
                        }
                    } catch (Exception e) {
                        this.plugin.getLogger().fine("[GUI] Failed to cache skull for " + playerId + ": " + e.getMessage());
                    }
                } finally {
                    this.loadingProfiles.remove(playerId);
                }
            });
        }
    }

    public void openGui(Player player) {
        openGui(player, 0);
    }

    public void openGui(Player player, int page) {
        this.playerPages.put(player.getUniqueId(), Integer.valueOf(page));
        this.syncManager.getAllSuspects().thenAccept(allSuspects -> {
            try {
                List<SuspectData> suspects = new ArrayList<>();
                Iterator it = allSuspects.iterator();
                while (it.hasNext()) {
                    SuspectData s = (SuspectData) it.next();
                    if (s.getCritValues().size() >= 5) {
                        suspects.add(s);
                    }
                }
                List<Integer> playerSlots = this.menuConfig.getPlayerSlots();
                int itemsPerPage = playerSlots.size();
                int startIndex = page * itemsPerPage;
                int endIndex = Math.min(startIndex + itemsPerPage, suspects.size());
                List<String> avgParams = getAvgParams();
                List<HeadData> headDataList = new ArrayList<>(endIndex - startIndex);
                for (int i = startIndex; i < endIndex; i++) {
                    headDataList.add(prepareHeadData(suspects.get(i), avgParams));
                }
                int i2 = endIndex + itemsPerPage;
                int nextEnd = Math.min(i2, suspects.size());
                for (int i3 = endIndex; i3 < nextEnd; i3++) {
                    cacheSkullAsync(suspects.get(i3).getPlayerId());
                }
                Map<Integer, String> slotNames = new HashMap<>();
                for (int i4 = 0; i4 < headDataList.size() && i4 < playerSlots.size(); i4++) {
                    slotNames.put(playerSlots.get(i4), headDataList.get(i4).playerName);
                }
                SchedulerManager.getAdapter().runSync(() -> {
                    try {
                        if (player.isOnline()) {
                            Inventory inv = Bukkit.createInventory(new GuiHolder(), this.menuConfig.getSize(), ColorUtil.colorize(this.menuConfig.getMenuTitle()));
                            for (MenuConfig.MenuItem item : this.menuConfig.getDecorationItems().values()) {
                                if (item.material != null) {
                                    ItemStack is = new ItemStack(item.material);
                                    ItemMeta meta = is.getItemMeta();
                                    if (meta != null) {
                                        meta.setDisplayName(ColorUtil.colorize(item.displayName));
                                        is.setItemMeta(meta);
                                    }
                                    Iterator<Integer> it2 = item.slots.iterator();
                                    while (it2.hasNext()) {
                                        int slot = it2.next().intValue();
                                        if (slot < inv.getSize()) {
                                            inv.setItem(slot, is);
                                        }
                                    }
                                }
                            }
                            for (int i5 = 0; i5 < headDataList.size() && i5 < playerSlots.size(); i5++) {
                                inv.setItem(((Integer) playerSlots.get(i5)).intValue(), createHead((HeadData) headDataList.get(i5)));
                            }
                            inv.setItem(this.menuConfig.getPrevPageSlot(), navItem(this.menuConfig.getPrevPageMaterial(), this.menuConfig.getPrevPageName()));
                            inv.setItem(this.menuConfig.getNextPageSlot(), navItem(this.menuConfig.getNextPageMaterial(), this.menuConfig.getNextPageName()));
                            this.viewerSlotMap.put(player.getUniqueId(), slotNames);
                            player.openInventory(inv);
                        }
                    } catch (Exception e) {
                        this.plugin.getLogger().warning("[GUI] Error opening GUI: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                this.plugin.getLogger().warning("[GUI] Error preparing GUI data: " + e.getMessage());
            }
        });
    }

    private ItemStack navItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat != null ? mat : Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> getAvgParams() {
        long now = System.currentTimeMillis();
        if (this.cachedAvgParams == null || now - this.lastAvgParamsUpdate > AVG_PARAMS_CACHE_MS) {
            this.cachedAvgParams = this.plugin.getPluginConfig().getAvgParameters();
            this.lastAvgParamsUpdate = now;
        }
        return this.cachedAvgParams;
    }

    private static class HeadData {
        final int amount;
        final String displayName;
        final List<String> lore;
        final UUID playerId;
        final String playerName;

        HeadData(UUID playerId, String playerName, int amount, String displayName, List<String> lore) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.amount = amount;
            this.displayName = displayName;
            this.lore = lore;
        }
    }

    private HeadData prepareHeadData(SuspectData suspect, List<String> avgParams) {
        int amount = Math.min(Math.max(1, suspect.getCount()), 64);
        double avg = suspect.getAvg(avgParams);
        String progressBar = buildProgressBar(avg);
        GuiManager guiManager = this;
        String displayName = ColorUtil.colorize(guiManager.replacePlaceholders(this.menuConfig.getPlayerHeadDisplayName(), suspect, progressBar, avg));
        List<String> loreTpl = guiManager.menuConfig.getPlayerHeadLore();
        List<String> lore = new ArrayList<>(loreTpl.size());
        for (String line : loreTpl) {
            lore.add(ColorUtil.colorize(guiManager.replacePlaceholders(line, suspect, progressBar, avg)));
            guiManager = this;
        }
        return new HeadData(suspect.getPlayerId(), suspect.getPlayerName(), amount, displayName, lore);
    }

    private ItemStack createHead(HeadData data) {
        Player online = Bukkit.getPlayer(data.playerId);
        if (online != null) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD, data.amount);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(online);
                meta.setDisplayName(data.displayName);
                meta.setLore(data.lore);
                head.setItemMeta(meta);
            }
            return head;
        }
        ItemStack cached = this.skullCache.get(data.playerId);
        if (cached != null) {
            ItemStack head2 = cached.clone();
            head2.setAmount(data.amount);
            SkullMeta meta2 = (SkullMeta) head2.getItemMeta();
            if (meta2 != null) {
                meta2.setDisplayName(data.displayName);
                meta2.setLore(data.lore);
                head2.setItemMeta(meta2);
            }
            return head2;
        }
        cacheSkullAsync(data.playerId);
        ItemStack head3 = new ItemStack(Material.PLAYER_HEAD, data.amount);
        SkullMeta meta3 = (SkullMeta) head3.getItemMeta();
        if (meta3 != null) {
            meta3.setDisplayName(data.displayName);
            meta3.setLore(data.lore);
            head3.setItemMeta(meta3);
        }
        return head3;
    }

    private String replacePlaceholders(String text, SuspectData suspect, String progressBar, double avg) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String result = text.replace("{PLAYER}", suspect.getPlayerName()).replace("{PROGRESS}", progressBar).replace("{PERCENT}", suspect.getPercent()).replace("{COUNT}", String.valueOf(suspect.getCount())).replace("{PROBABILITY}", colorProb(suspect.getLastProbability())).replace("{PROBABILITY_1H}", colorProb(suspect.getProbability1h())).replace("{PROBABILITY_6H}", colorProb(suspect.getProbability6h())).replace("{PROBABILITY_ALL}", colorProb(suspect.getProbabilityAll())).replace("{AVG}", colorProb(avg)).replace("{ROTATION}", colorProb(suspect.getRotation())).replace("{AIMBOT}", colorProb(suspect.getAimbot())).replace("{GCD}", colorProb(suspect.getGcd())).replace("{SNAP}", colorProb(suspect.getSnap())).replace("{SMOOTH}", colorProb(suspect.getSmooth())).replace("{ROTATION_1H}", colorProb(suspect.getRotationMax())).replace("{AIMBOT_1H}", colorProb(suspect.getAimbotMax())).replace("{GCD_1H}", colorProb(suspect.getGcdMax())).replace("{SNAP_1H}", colorProb(suspect.getSnapMax())).replace("{SMOOTH_1H}", colorProb(suspect.getSmoothMax())).replace("{ROTATION_MAX}", colorProb(suspect.getRotationMaxAll())).replace("{AIMBOT_MAX}", colorProb(suspect.getAimbotMaxAll())).replace("{GCD_MAX}", colorProb(suspect.getGcdMaxAll())).replace("{SNAP_MAX}", colorProb(suspect.getSnapMaxAll())).replace("{SMOOTH_MAX}", colorProb(suspect.getSmoothMaxAll())).replace("{VL}", colorVL(suspect.getVl())).replace("{TIME}", suspect.getFormattedTime()).replace("{SERVER}", suspect.getServer());
        List<Double> crits = suspect.getCritValues();
        int i = 1;
        while (i <= 15) {
            String placeholder = "%crit" + i + "%";
            if (result.contains(placeholder)) {
                String val = i <= crits.size() ? colorProb(crits.get(i - 1).doubleValue()) : "";
                result = result.replace(placeholder, val);
            }
            i++;
        }
        List<Double> aims = suspect.getAimValues();
        int i2 = 1;
        while (i2 <= 15) {
            String placeholder2 = "%aim" + i2 + "%";
            if (result.contains(placeholder2)) {
                String val2 = i2 <= aims.size() ? colorProb(aims.get(i2 - 1).doubleValue()) : "";
                result = result.replace(placeholder2, val2);
            }
            i2++;
        }
        return result;
    }

    private String colorProb(double prob) {
        return ColorUtil.gradientValue(prob);
    }

    private String colorVL(int vl) {
        double normalized = Math.min(((double) vl) / 20.0d, 1.0d);
        return ColorUtil.gradientColor(normalized) + vl;
    }

    private String buildProgressBar(double avg) {
        int length = this.menuConfig.getProgressBarLength();
        int filledCount = (int) (((double) length) * avg);
        String filled = this.menuConfig.getProgressBarFilled();
        String empty = this.menuConfig.getProgressBarEmpty();
        String emptyColor = this.menuConfig.getProgressBarEmptyColor();
        StringBuilder sb = new StringBuilder((length * 2) + 16);
        sb.append(ColorUtil.gradientColor(avg));
        for (int i = 0; i < filledCount; i++) {
            sb.append(filled);
        }
        sb.append(emptyColor);
        for (int i2 = filledCount; i2 < length; i2++) {
            sb.append(empty);
        }
        return sb.toString();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String suspectName;
        ItemStack clicked;
        if ((event.getInventory().getHolder() instanceof GuiHolder) && (event.getWhoClicked() instanceof Player)) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();
            if (slot == this.menuConfig.getPrevPageSlot()) {
                int currentPage = this.playerPages.getOrDefault(player.getUniqueId(), 0).intValue();
                if (currentPage > 0) {
                    openGui(player, currentPage - 1);
                    return;
                }
                return;
            }
            if (slot == this.menuConfig.getNextPageSlot()) {
                int currentPage2 = this.playerPages.getOrDefault(player.getUniqueId(), 0).intValue();
                this.syncManager.getAllSuspects().thenAccept(allSuspects -> {
                    long filteredCount = allSuspects.stream().filter(s -> {
                        return s.getCritValues().size() >= 5;
                    }).count();
                    int itemsPerPage = this.menuConfig.getPlayerSlots().size();
                    int totalPages = Math.max(1, (int) Math.ceil(filteredCount / ((double) itemsPerPage)));
                    if (currentPage2 < totalPages - 1) {
                        SchedulerManager.getAdapter().runSync(() -> {
                            openGui(player, currentPage2 + 1);
                        });
                    }
                });
                return;
            }
            Map<Integer, String> slotMap = this.viewerSlotMap.get(player.getUniqueId());
            if (slotMap == null || (suspectName = slotMap.get(Integer.valueOf(slot))) == null || (clicked = event.getCurrentItem()) == null || clicked.getType() != Material.PLAYER_HEAD) {
                return;
            }
            player.closeInventory();
            player.performCommand("spec " + suspectName);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder) {
            UUID viewerId = event.getPlayer().getUniqueId();
            this.playerPages.remove(viewerId);
            this.viewerSlotMap.remove(viewerId);
        }
    }

    public void shutdown() {
        this.syncManager.shutdown();
        this.textureLoader.shutdown();
        this.skullCache.clear();
        this.loadingProfiles.clear();
        this.viewerSlotMap.clear();
    }

    public void reload() {
        this.menuConfig.load();
    }

    public SyncManager getSyncManager() {
        return this.syncManager;
    }

    public static class GuiHolder implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }
}
