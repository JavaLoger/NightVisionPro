package org.loger.compat;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import kotlin.jvm.internal.IntCompanionObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.slf4j.Marker;

public class WorldGuardCompat {
    private final Map<String, Set<String>> disabledRegions = new HashMap();
    private final boolean enabled;
    private final Logger logger;
    private boolean worldGuardAvailable;

    public WorldGuardCompat(Logger logger, boolean enabled, List<String> disabledRegionsList) {
        this.logger = logger;
        this.enabled = enabled;
        parseDisabledRegions(disabledRegionsList);
        checkWorldGuardAvailability();
    }

    private void parseDisabledRegions(List<String> regionsList) {
        for (String entry : regionsList) {
            String[] parts = entry.split(":", 2);
            if (parts.length == 2) {
                String worldName = parts[0].toLowerCase();
                String regionName = parts[1].toLowerCase();
                this.disabledRegions.computeIfAbsent(worldName, k -> {
                    return new HashSet();
                }).add(regionName);
            } else {
                this.disabledRegions.computeIfAbsent(Marker.ANY_MARKER, k2 -> {
                    return new HashSet();
                }).add(entry.toLowerCase());
            }
        }
    }

    private void checkWorldGuardAvailability() {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
                this.worldGuardAvailable = true;
                this.logger.info("[WorldGuard] Integration enabled");
            } else {
                this.worldGuardAvailable = false;
                if (this.enabled) {
                    this.logger.warning("[WorldGuard] Plugin not found, region checking disabled");
                }
            }
        } catch (ClassNotFoundException e) {
            this.worldGuardAvailable = false;
            if (this.enabled) {
                this.logger.warning("[WorldGuard] Not available, region checking disabled");
            }
        }
    }

    public boolean shouldBypassAICheck(Player player) {
        if (!this.enabled || !this.worldGuardAvailable) {
            return false;
        }
        return shouldBypassAtLocation(player.getLocation());
    }

    public boolean shouldBypassAtLocation(Location location) {
        World world;
        if (!this.enabled || !this.worldGuardAvailable || (world = location.getWorld()) == null) {
            return false;
        }
        String worldName = world.getName().toLowerCase();
        Set<String> worldDisabled = this.disabledRegions.getOrDefault(worldName, Collections.emptySet());
        Set<String> globalDisabled = this.disabledRegions.getOrDefault(Marker.ANY_MARKER, Collections.emptySet());
        if (worldDisabled.isEmpty() && globalDisabled.isEmpty()) {
            return false;
        }
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
            if (regionManager == null) {
                return false;
            }
            ApplicableRegionSet regions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
            ProtectedRegion highestPriorityRegion = null;
            int highestPriority = IntCompanionObject.MIN_VALUE;
            for (ProtectedRegion region : regions) {
                if (region.getPriority() > highestPriority) {
                    int highestPriority2 = region.getPriority();
                    highestPriorityRegion = region;
                    highestPriority = highestPriority2;
                }
            }
            if (highestPriorityRegion != null) {
                String regionId = highestPriorityRegion.getId().toLowerCase();
                if (!worldDisabled.contains(regionId)) {
                    if (!globalDisabled.contains(regionId)) {
                        return false;
                    }
                }
                return true;
            }
        } catch (Exception e) {
            this.logger.warning("[WorldGuard] Error checking regions: " + e.getMessage());
        }
        return false;
    }

    public List<String> getRegionsAtPlayer(Player player) {
        Location location;
        World world;
        List<String> result = new ArrayList<>();
        if (!this.worldGuardAvailable || (world = (location = player.getLocation()).getWorld()) == null) {
            return result;
        }
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
            if (regionManager == null) {
                return result;
            }
            ApplicableRegionSet regions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
            for (ProtectedRegion region : regions) {
                result.add(region.getId() + " (priority: " + region.getPriority() + ")");
            }
        } catch (Exception e) {
            this.logger.warning("[WorldGuard] Error getting regions: " + e.getMessage());
        }
        return result;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isWorldGuardAvailable() {
        return this.worldGuardAvailable;
    }

    public Map<String, Set<String>> getDisabledRegions() {
        return Collections.unmodifiableMap(this.disabledRegions);
    }
}
