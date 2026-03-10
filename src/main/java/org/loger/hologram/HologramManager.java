package org.loger.hologram;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.loger.Main;
import org.loger.checks.AICheck;
import org.loger.config.Config;
import org.loger.data.AIPlayerData;
import org.loger.data.ProbabilityHistory;
import org.loger.scheduler.ScheduledTask;
import org.loger.scheduler.SchedulerManager;
import org.loger.util.ColorUtil;
import org.loger.violation.ViolationManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class HologramManager {
    private static final long CONFIG_CACHE_MS = 2000;
    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(2147473647);
    private static final long TEXT_CACHE_MS = 200;
    private final AICheck aiCheck;
    private volatile List<String> cachedAvgParams;
    private volatile double cachedHeight;
    private volatile List<String> cachedHologramLines;
    private volatile double cachedLineSpacing;
    private volatile int cachedMaxDistance;
    private volatile int cachedMaxDistanceSq;
    private final Main plugin;
    private final ProbabilityHistory probabilityHistory;
    private ScheduledTask updateTask;
    private final ViolationManager violationManager;
    private final Set<UUID> viewersEnabled = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Map<UUID, int[]>> playerHolograms = new ConcurrentHashMap<>();
    private final Set<Integer> allCreatedEntityIds = ConcurrentHashMap.newKeySet();
    private volatile long lastConfigUpdate = 0;
    private final Map<UUID, CachedHologramTexts> textCache = new ConcurrentHashMap<>();

    private static class CachedHologramTexts {
        final List<Component> components;
        final long timestamp = System.currentTimeMillis();

        CachedHologramTexts(List<String> texts) {
            this.components = new ArrayList<>(texts.size());
            LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
            for (String text : texts) {
                this.components.add(serializer.deserialize(text));
            }
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - this.timestamp > HologramManager.TEXT_CACHE_MS;
        }
    }

    public HologramManager(Main plugin, AICheck aiCheck, ViolationManager violationManager, ProbabilityHistory probabilityHistory) {
        this.plugin = plugin;
        this.aiCheck = aiCheck;
        this.violationManager = violationManager;
        this.probabilityHistory = probabilityHistory;
        refreshConfigCache();
    }

    private void refreshConfigCache() {
        long now = System.currentTimeMillis();
        if (now - this.lastConfigUpdate < CONFIG_CACHE_MS) {
            return;
        }
        Config config = this.plugin.getPluginConfig();
        this.cachedAvgParams = config.getAvgParameters();
        this.cachedHologramLines = config.getHologramLines();
        this.cachedMaxDistance = config.getHologramDistance();
        this.cachedMaxDistanceSq = this.cachedMaxDistance * this.cachedMaxDistance;
        this.cachedHeight = config.getHologramHeight();
        this.cachedLineSpacing = config.getHologramLineSpacing();
        this.lastConfigUpdate = now;
    }

    public void start() {
        Config config = this.plugin.getPluginConfig();
        if (config.isHologramEnabled()) {
            int interval = config.getHologramUpdateInterval();
            this.updateTask = SchedulerManager.getAdapter().runSyncRepeating(this::updateAllHolograms, interval, interval);
        }
    }

    public void stop() {
        if (this.updateTask != null) {
            this.updateTask.cancel();
            this.updateTask = null;
        }
        removeAllHolograms();
        this.textCache.clear();
    }

    public void cleanup() {
        removeAllHolograms();
        destroyAllKnownEntities();
        this.textCache.clear();
    }

    private void destroyAllKnownEntities() {
        if (this.allCreatedEntityIds.isEmpty()) {
            return;
        }
        int[] ids = this.allCreatedEntityIds.stream().mapToInt(Integer::intValue).toArray();
        WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(ids);
        for (Player player : Bukkit.getOnlinePlayers()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroyPacket);
        }
        this.allCreatedEntityIds.clear();
    }

    public boolean toggleHolograms(Player viewer) {
        UUID viewerId = viewer.getUniqueId();
        if (this.viewersEnabled.contains(viewerId)) {
            this.viewersEnabled.remove(viewerId);
            removeHologramsForViewer(viewer);
            return false;
        }
        this.viewersEnabled.add(viewerId);
        createHologramsForViewer(viewer);
        return true;
    }

    public boolean hasHologramsEnabled(Player viewer) {
        return this.viewersEnabled.contains(viewer.getUniqueId());
    }

    private void createHologramsForViewer(Player viewer) {
        refreshConfigCache();
        int maxDistSq = this.cachedMaxDistanceSq;
        Map<UUID, int[]> viewerHolograms = this.playerHolograms.computeIfAbsent(viewer.getUniqueId(), k -> new ConcurrentHashMap<>());
        Location viewerLoc = viewer.getLocation();
        for (Player target : Bukkit.getOnlinePlayers()) {
            UUID targetId = target.getUniqueId();
            if (!targetId.equals(viewer.getUniqueId()) && target.getWorld().equals(viewer.getWorld())) {
                double distanceSq = viewerLoc.distanceSquared(target.getLocation());
                if (distanceSq <= maxDistSq) {
                    CachedHologramTexts cachedTexts = getOrBuildTexts(target);
                    int[] entityIds = createHologramStackCached(viewer, target, cachedTexts);
                    viewerHolograms.put(targetId, entityIds);
                }
            }
        }
    }

    private void updateAllHolograms() {
        cleanupDeadHolograms();
        refreshConfigCache();
        int maxDistSq = this.cachedMaxDistanceSq;
        this.textCache.entrySet().removeIf(e -> e.getValue().isExpired());
        for (UUID viewerId : this.viewersEnabled) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null || !viewer.isOnline()) {
                this.viewersEnabled.remove(viewerId);
            } else {
                Map<UUID, int[]> viewerHolograms = this.playerHolograms.computeIfAbsent(viewerId, k -> new ConcurrentHashMap<>());
                Set<UUID> seenPlayers = new HashSet<>();
                Location viewerLoc = viewer.getLocation();
                for (Player target : Bukkit.getOnlinePlayers()) {
                    UUID targetId = target.getUniqueId();
                    if (!targetId.equals(viewerId)) {
                        if (!target.getWorld().equals(viewer.getWorld())) {
                            removeHologramForTarget(viewer, targetId, viewerHolograms);
                        } else {
                            double distanceSq = viewerLoc.distanceSquared(target.getLocation());
                            if (distanceSq > maxDistSq) {
                                removeHologramForTarget(viewer, targetId, viewerHolograms);
                            } else {
                                seenPlayers.add(targetId);
                                int[] entityIds = viewerHolograms.get(targetId);
                                CachedHologramTexts cachedTexts = getOrBuildTexts(target);
                                if (entityIds == null) {
                                    viewerHolograms.put(targetId, createHologramStackCached(viewer, target, cachedTexts));
                                } else {
                                    updateHologramStackCached(viewer, target, entityIds, cachedTexts);
                                }
                            }
                        }
                    }
                }
                viewerHolograms.entrySet().removeIf(entry -> {
                    if (!seenPlayers.contains(entry.getKey())) {
                        destroyHologramStack(viewer, entry.getValue());
                        return true;
                    }
                    return false;
                });
            }
        }
    }

    private CachedHologramTexts getOrBuildTexts(Player target) {
        UUID targetId = target.getUniqueId();
        CachedHologramTexts cached = this.textCache.get(targetId);
        if (cached != null && !cached.isExpired()) {
            return cached;
        }
        List<String> texts = buildHologramTextsFast(target);
        CachedHologramTexts newCache = new CachedHologramTexts(texts);
        this.textCache.put(targetId, newCache);
        return newCache;
    }

    private void destroyHologramStack(Player viewer, int[] entityIds) {
        if (entityIds == null || entityIds.length == 0) {
            return;
        }
        for (int id : entityIds) {
            this.allCreatedEntityIds.remove(id);
        }
        if (viewer == null || !viewer.isOnline()) {
            return;
        }
        WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(entityIds);
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, destroyPacket);
    }

    private void destroyHologramStackForAll(int[] entityIds) {
        if (entityIds == null || entityIds.length == 0) {
            return;
        }
        for (int id : entityIds) {
            this.allCreatedEntityIds.remove(id);
        }
        WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(entityIds);
        for (Player player : Bukkit.getOnlinePlayers()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroyPacket);
        }
    }

    private void removeHologramForTarget(Player viewer, UUID targetId, Map<UUID, int[]> viewerHolograms) {
        int[] entityIds = viewerHolograms.remove(targetId);
        if (entityIds != null) {
            destroyHologramStack(viewer, entityIds);
        }
    }

    private List<String> buildHologramTextsFast(Player target) {
        UUID targetId = target.getUniqueId();
        AIPlayerData data = this.aiCheck.getPlayerData(targetId);
        double currentProb = data != null ? data.getLastProbability() : 0.0d;
        double rotation = data != null ? data.getLastRotation() : 0.0d;
        double aimbot = data != null ? data.getLastAimbot() : 0.0d;
        double gcd = data != null ? data.getLastGcd() : 0.0d;
        double snap = data != null ? data.getLastSnap() : 0.0d;
        double smooth = data != null ? data.getLastSmooth() : 0.0d;
        double snap2 = snap;
        double snap3 = smooth;
        double currentProb2 = currentProb;
        double rotation2 = rotation;
        double rotation3 = aimbot;
        double aimbot2 = gcd;
        double avg = calculateAvgFast(currentProb2, rotation2, rotation3, aimbot2, snap2, snap3);
        double prob1h = this.probabilityHistory.getProbability1Hour(targetId);
        double prob6h = this.probabilityHistory.getProbability6Hours(targetId);
        double probAll = this.probabilityHistory.getProbabilityAllTime(targetId);
        double rotMax = this.probabilityHistory.getRotationMax1Hour(targetId);
        double aimMax = this.probabilityHistory.getAimbotMax1Hour(targetId);
        double gcdMax = this.probabilityHistory.getGcdMax1Hour(targetId);
        double snapMax = this.probabilityHistory.getSnapMax1Hour(targetId);
        double smoothMax = this.probabilityHistory.getSmoothMax1Hour(targetId);
        int vl = this.violationManager.getViolationLevel(targetId);
        int ping = target.getPing();
        String playerName = target.getName();
        List<String> lines = this.cachedHologramLines;
        int vl2 = lines.size();
        List<String> result = new ArrayList<>(vl2);
        Iterator<String> it = lines.iterator();
        while (it.hasNext()) {
            Iterator<String> it2 = it;
            String lineTemplate = it.next();
            double rotation4 = rotation2;
            if (!lineTemplate.contains("%") && !lineTemplate.contains("{")) {
                result.add(ColorUtil.colorize(lineTemplate));
                it = it2;
                rotation2 = rotation4;
            } else {
                String line = lineTemplate.replace("{PLAYER}", playerName);
                line = replaceProbability(line, "{PROBABILITY}", currentProb2);
                line = replaceProbability(line, "{PROBABILITY_1H}", prob1h);
                line = replaceProbability(line, "{PROBABILITY_6H}", prob6h);
                line = replaceProbability(line, "{PROBABILITY_ALL}", probAll);
                line = line.replace("{AVG}", String.format("%.2f", avg));
                line = line.replace("{ROTATION}", String.format("%.2f", rotation4));
                line = line.replace("{AIMBOT}", String.format("%.2f", rotation3));
                line = line.replace("{GCD}", String.format("%.2f", aimbot2));
                line = line.replace("{SNAP}", String.format("%.2f", snap2));
                line = line.replace("{SMOOTH}", String.format("%.2f", snap3));
                line = line.replace("{ROTATION_1H}", String.format("%.2f", rotMax));
                line = line.replace("{AIMBOT_1H}", String.format("%.2f", aimMax));
                line = line.replace("{GCD_1H}", String.format("%.2f", gcdMax));
                line = line.replace("{SNAP_1H}", String.format("%.2f", snapMax));
                line = line.replace("{SMOOTH_1H}", String.format("%.2f", smoothMax));
                line = replaceVL(line, "{VL}", vl);
                line = replacePing(line, "{PING}", ping);
                result.add(ColorUtil.colorize(line));
                it = it2;
                rotation2 = rotation4;
                currentProb2 = currentProb2;
            }
        }
        return result;
    }

    private double calculateAvgFast(double probability, double rotation, double aimbot, double gcd, double snap, double smooth) {
        List<String> avgParams = this.cachedAvgParams;
        if (avgParams == null || avgParams.isEmpty()) {
            return (rotation + aimbot + gcd + snap + smooth) / 5.0;
        }
        double sum = 0.0;
        int count = 0;
        for (String param : avgParams) {
            String lowerParam = param.toLowerCase();
            switch (lowerParam) {
                case "rotation": sum += rotation; count++; break;
                case "aimbot": sum += aimbot; count++; break;
                case "gcd": sum += gcd; count++; break;
                case "snap": sum += snap; count++; break;
                case "smooth": sum += smooth; count++; break;
                case "probability": sum += probability; count++; break;
            }
        }
        return count > 0 ? sum / count : 0.0;
    }

    private int[] createHologramStackCached(Player viewer, Player target, CachedHologramTexts cached) {
        List<Component> components = cached.components;
        int[] entityIds = new int[components.size()];
        Location loc = target.getLocation();
        double baseY = loc.getY() + this.cachedHeight;
        double x = loc.getX();
        double z = loc.getZ();
        for (int i = 0; i < components.size(); i++) {
            int entityId = ENTITY_ID_COUNTER.getAndDecrement();
            entityIds[i] = entityId;
            this.allCreatedEntityIds.add(entityId);
            double y = (((double) i) * this.cachedLineSpacing) + baseY;
            UUID uuid = UUID.randomUUID();
            com.github.retrooper.packetevents.protocol.world.Location location = new com.github.retrooper.packetevents.protocol.world.Location(new Vector3d(x, y, z), 0.0f, 0.0f);
            WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(entityId, uuid, EntityTypes.ARMOR_STAND, location, 0.0f, 0, null);
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, spawnPacket);
            List<EntityData<?>> metadata = Arrays.asList(
                new EntityData<>(0, EntityDataTypes.BYTE, (byte) 32),
                new EntityData<>(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(components.get(i))),
                new EntityData<>(3, EntityDataTypes.BOOLEAN, true),
                new EntityData<>(5, EntityDataTypes.BOOLEAN, true)
            );
            WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, metadataPacket);
        }
        return entityIds;
    }

    private void updateHologramStackCached(Player viewer, Player target, int[] entityIds, CachedHologramTexts cached) {
        List<Component> components = cached.components;
        Location loc = target.getLocation();
        double baseY = loc.getY() + this.cachedHeight;
        double x = loc.getX();
        double z = loc.getZ();
        int size = Math.min(entityIds.length, components.size());
        for (int i = 0; i < size; i++) {
            double y = (((double) i) * this.cachedLineSpacing) + baseY;
            WrapperPlayServerEntityTeleport teleportPacket = new WrapperPlayServerEntityTeleport(entityIds[i], new com.github.retrooper.packetevents.protocol.world.Location(new Vector3d(x, y, z), 0.0f, 0.0f), false);
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, teleportPacket);
            List<EntityData<?>> metadata = Collections.singletonList(new EntityData<>(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(components.get(i))));
            WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(entityIds[i], metadata);
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, metadataPacket);
        }
    }

    private String replaceProbability(String line, String placeholder, double probability) {
        if (!line.contains(placeholder)) return line;
        String color = ColorUtil.gradientColor(probability);
        String probStr = String.format("%.4f", probability);
        return line.replace(placeholder, color + probStr);
    }

    private String replaceVL(String line, String placeholder, int vl) {
        if (!line.contains(placeholder)) return line;
        String color = ChatColor.GREEN.toString();
        return line.replace(placeholder, color + vl);
    }

    private String replacePing(String line, String placeholder, int ping) {
        if (!line.contains(placeholder)) return line;
        String color = ChatColor.GREEN.toString();
        return line.replace(placeholder, color + ping);
    }

    private void removeHologramsForViewer(Player viewer) {
        Map<UUID, int[]> viewerHolograms = this.playerHolograms.remove(viewer.getUniqueId());
        if (viewerHolograms != null) {
            for (int[] entityIds : viewerHolograms.values()) {
                destroyHologramStack(viewer, entityIds);
            }
        }
    }

    private void removeAllHolograms() {
        for (Map.Entry<UUID, Map<UUID, int[]>> entry : this.playerHolograms.entrySet()) {
            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer != null && viewer.isOnline()) {
                for (int[] entityIds : entry.getValue().values()) {
                    destroyHologramStack(viewer, entityIds);
                }
            }
        }
        this.playerHolograms.clear();
        this.viewersEnabled.clear();
    }

    public void handlePlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();
        this.textCache.remove(playerId);
        this.viewersEnabled.remove(playerId);
        Map<UUID, int[]> viewerHolograms = this.playerHolograms.remove(playerId);
        if (viewerHolograms != null) {
            for (int[] entityIds : viewerHolograms.values()) {
                if (entityIds != null) {
                    for (int id : entityIds) {
                        this.allCreatedEntityIds.remove(id);
                    }
                }
            }
        }
        for (Map.Entry<UUID, Map<UUID, int[]>> entry : this.playerHolograms.entrySet()) {
            int[] entityIds2 = entry.getValue().remove(playerId);
            if (entityIds2 != null) {
                destroyHologramStackForAll(entityIds2);
            }
        }
    }

    public void cleanupDeadHolograms() {
        this.playerHolograms.entrySet().removeIf(entry -> {
            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer == null || !viewer.isOnline()) {
                this.viewersEnabled.remove(entry.getKey());
                return true;
            }
            entry.getValue().entrySet().removeIf(holoEntry -> {
                Player target = Bukkit.getPlayer(holoEntry.getKey());
                if (target == null || !target.isOnline()) {
                    destroyHologramStack(viewer, holoEntry.getValue());
                    return true;
                }
                return false;
            });
            return false;
        });
    }

    public void handlePlayerJoin(Player player) {
        refreshConfigCache();
        int maxDistSq = this.cachedMaxDistanceSq;
        Location playerLoc = player.getLocation();
        UUID playerId = player.getUniqueId();
        this.textCache.remove(playerId);
        for (Map.Entry<UUID, Map<UUID, int[]>> entry : this.playerHolograms.entrySet()) {
            int[] oldEntityIds = entry.getValue().remove(playerId);
            if (oldEntityIds != null) {
                destroyHologramStackForAll(oldEntityIds);
            }
        }
        for (UUID viewerId : this.viewersEnabled) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline() && !viewerId.equals(playerId) && viewer.getWorld().equals(player.getWorld())) {
                double distanceSq = viewer.getLocation().distanceSquared(playerLoc);
                if (distanceSq <= maxDistSq) {
                    Map<UUID, int[]> viewerHolograms = this.playerHolograms.computeIfAbsent(viewerId, k -> new ConcurrentHashMap<>());
                    CachedHologramTexts cachedTexts = getOrBuildTexts(player);
                    int[] entityIds = createHologramStackCached(viewer, player, cachedTexts);
                    viewerHolograms.put(playerId, entityIds);
                }
            }
        }
    }
}
