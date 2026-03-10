package org.loger.penalty;

import org.loger.compat.ParticleCompat;
import org.loger.scheduler.ScheduledTask;
import org.loger.scheduler.SchedulerManager;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BanAnimation implements Listener {
    private static final int ANIMATION_END_TICK = 80;
    private static final int LEVITATION_DURATION = 90;
    private static final int STROBE_INTERVAL = 4;
    private final Set<UUID> animatingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> pendingBans = new ConcurrentHashMap();
    private final JavaPlugin plugin;

    public BanAnimation(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void playAnimation(Player player, String banCommand, PenaltyContext context) {
        if (player == null) {
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            SchedulerManager.getAdapter().runSync(() -> {
                playAnimation(player, banCommand, context);
            });
            return;
        }
        if (!player.isOnline()) {
            executeBanCommand(banCommand);
            return;
        }
        UUID playerId = player.getUniqueId();
        if (this.animatingPlayers.contains(playerId)) {
            return;
        }
        this.animatingPlayers.add(playerId);
        this.pendingBans.put(playerId, banCommand);
        Location origin = player.getLocation().clone();
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, LEVITATION_DURATION, 1, false, false, false));
        player.setAllowFlight(false);
        player.setGliding(false);
        player.setFlying(false);
        player.setSwimming(false);
        int[] ticks = {0};
        ScheduledTask[] taskRef = new ScheduledTask[1];
        taskRef[0] = SchedulerManager.getAdapter().runSyncRepeating(() -> {
            try {
                if (!player.isOnline()) {
                    taskRef[0].cancel();
                    return;
                }
                if (ticks[0] >= ANIMATION_END_TICK) {
                    finishAnimation(player, banCommand);
                    taskRef[0].cancel();
                    return;
                }
                Location loc = player.getLocation();
                if (Math.abs(loc.getX() - origin.getX()) > 0.5d || Math.abs(loc.getZ() - origin.getZ()) > 0.5d) {
                    loc.setX(origin.getX());
                    loc.setZ(origin.getZ());
                    player.teleport(loc);
                }
                if (ticks[0] % 4 == 0) {
                    spawnStrobe(player);
                }
                ticks[0] = ticks[0] + 1;
            } catch (Exception e) {
                this.plugin.getLogger().severe("Ban animation error: " + e.getMessage());
                taskRef[0].cancel();
                cleanup(player, banCommand);
            }
        }, 0L, 1L);
    }

    private void spawnStrobe(Player player) {
        Location loc = player.getLocation().add(0.0d, 1.0d, 0.0d);
        Particle flash = ParticleCompat.getParticle("FLASH");
        if (flash != null) {
            ParticleCompat.spawnParticle(player.getWorld(), flash, loc, 1, 0.0d, 0.0d, 0.0d, 0.0d);
            return;
        }
        Particle endRod = ParticleCompat.getParticle("END_ROD");
        if (endRod != null) {
            ParticleCompat.spawnParticle(player.getWorld(), endRod, loc, 15, 0.5d, 0.5d, 0.5d, 0.05d);
        }
    }

    private void finishAnimation(Player player, String banCommand) {
        UUID playerId = player.getUniqueId();
        this.animatingPlayers.remove(playerId);
        this.pendingBans.remove(playerId);
        player.removePotionEffect(PotionEffectType.LEVITATION);
        Location loc = player.getLocation().add(0.0d, 2.0d, 0.0d);
        player.teleport(loc);
        spawnExplosion(player.getWorld(), loc);
        try {
            player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
        } catch (Exception e) {
        }
        player.setHealth(0.0d);
        SchedulerManager.getAdapter().runSyncDelayed(() -> {
            executeBanCommand(banCommand);
        }, 5L);
    }

    private void spawnExplosion(World world, Location center) {
        Particle explosion;
        Particle smoke;
        Particle explosion2 = ParticleCompat.getParticle("EXPLOSION_LARGE");
        if (explosion2 != null) {
            explosion = explosion2;
        } else {
            explosion = ParticleCompat.getParticle("EXPLOSION");
        }
        if (explosion != null) {
            ParticleCompat.spawnParticle(world, explosion, center, 3, 0.5d, 0.5d, 0.5d, 0.0d);
        }
        Particle flash = ParticleCompat.getParticle("FLASH");
        if (flash != null) {
            for (int i = -2; i <= 2; i++) {
                ParticleCompat.spawnParticle(world, flash, center.clone().add(i, 0.0d, 0.0d), 3, 0.0d, 0.0d, 0.0d, 0.0d);
                ParticleCompat.spawnParticle(world, flash, center.clone().add(0.0d, 0.0d, i), 3, 0.0d, 0.0d, 0.0d, 0.0d);
            }
        }
        Particle smoke2 = ParticleCompat.getParticle("SMOKE_LARGE");
        if (smoke2 != null) {
            smoke = smoke2;
        } else {
            smoke = ParticleCompat.getParticle("SMOKE");
        }
        if (smoke != null) {
            ParticleCompat.spawnParticle(world, smoke, center, 30, 1.0d, 1.0d, 1.0d, 0.1d);
        }
    }

    private void cleanup(Player player, String banCommand) {
        UUID playerId = player.getUniqueId();
        this.animatingPlayers.remove(playerId);
        this.pendingBans.remove(playerId);
        if (player.isOnline()) {
            player.removePotionEffect(PotionEffectType.LEVITATION);
            player.setHealth(0.0d);
        }
        executeBanCommand(banCommand);
    }

    private void executeBanCommand(String command) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else {
            SchedulerManager.getAdapter().runSync(() -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            });
        }
    }

    public boolean isAnimating(UUID playerId) {
        return this.animatingPlayers.contains(playerId);
    }

    public boolean isAnimating(Player player) {
        return player != null && this.animatingPlayers.contains(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (this.animatingPlayers.contains(playerId)) {
            String banCommand = this.pendingBans.get(playerId);
            this.animatingPlayers.remove(playerId);
            this.pendingBans.remove(playerId);
            if (banCommand != null) {
                SchedulerManager.getAdapter().runSyncDelayed(() -> {
                    executeBanCommand(banCommand);
                }, 1L);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (isAnimating(event.getPlayer()) && event.getTo() != null) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                Location to = event.getTo().clone();
                to.setX(event.getFrom().getX());
                to.setZ(event.getFrom().getZ());
                event.setTo(to);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (isAnimating(event.getPlayer()) && event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isAnimating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (isAnimating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isAnimating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if ((event.getWhoClicked() instanceof Player) && isAnimating((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if ((event.getPlayer() instanceof Player) && isAnimating((Player) event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isAnimating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isAnimating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if ((event.getDamager() instanceof Player) && isAnimating((Player) event.getDamager())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemPickup(EntityPickupItemEvent event) {
        if ((event.getEntity() instanceof Player) && isAnimating((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
        this.animatingPlayers.clear();
        this.pendingBans.clear();
    }
}
