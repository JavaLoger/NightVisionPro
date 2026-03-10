package org.loger.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.loger.checks.AICheck;
import org.loger.session.ISessionManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class HitListener extends PacketListenerAbstract {
    private final AICheck aiCheck;
    private final Map<Integer, UUID> playerIdCache;
    private final ISessionManager sessionManager;

    public HitListener(ISessionManager sessionManager, AICheck aiCheck) {
        super(PacketListenerPriority.NORMAL);
        this.playerIdCache = new ConcurrentHashMap();
        this.sessionManager = sessionManager;
        this.aiCheck = aiCheck;
    }

    public void setCurrentTick(int tick) {
        if (tick % 200 == 0) {
            this.playerIdCache.entrySet().removeIf(entry -> {
                return Bukkit.getPlayer((UUID) entry.getValue()) == null;
            });
        }
    }

    public void cachePlayer(Player player) {
        if (player != null) {
            this.playerIdCache.put(Integer.valueOf(player.getEntityId()), player.getUniqueId());
        }
    }

    public void uncachePlayer(Player player) {
        if (player != null) {
            this.playerIdCache.remove(Integer.valueOf(player.getEntityId()));
        }
    }

    public void cacheOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            cachePlayer(player);
        }
    }

    public void cacheEntity(Entity entity) {
        if (entity instanceof Player) {
            cachePlayer((Player) entity);
        }
    }

    public void onPacketReceive(PacketReceiveEvent event) {
        Player attacker;
        try {
            if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
                return;
            }
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK || (attacker = (Player) event.getPlayer()) == null) {
                return;
            }
            int targetId = packet.getEntityId();
            Entity playerById = getPlayerById(targetId);
            if (playerById == null) {
                return;
            }
            if (this.aiCheck != null) {
                this.aiCheck.onAttack(attacker, playerById);
            }
            this.sessionManager.onAttack(attacker);
        } catch (Exception e) {
        }
    }

    private Player getPlayerById(int entityId) {
        UUID uuid = this.playerIdCache.get(Integer.valueOf(entityId));
        if (uuid != null) {
            return Bukkit.getPlayer(uuid);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getEntityId() == entityId) {
                this.playerIdCache.put(Integer.valueOf(entityId), player.getUniqueId());
                return player;
            }
        }
        return null;
    }
}
