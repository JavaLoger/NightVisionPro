package org.loger.session;

import org.loger.Main;
import org.loger.config.Label;
import org.loger.data.DataSession;
import org.loger.util.AimProcessor;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.entity.Player;

public class SessionManager implements ISessionManager {
    private final Main plugin;
    private volatile String currentSessionFolder = null;
    private final Map<UUID, DataSession> activeSessions = new ConcurrentHashMap();
    private final Map<UUID, AimProcessor> playerAimProcessors = new ConcurrentHashMap();

    public SessionManager(Main plugin) {
        this.plugin = plugin;
    }

    public AimProcessor getOrCreateAimProcessor(UUID playerId) {
        return this.playerAimProcessors.computeIfAbsent(playerId, id -> {
            return new AimProcessor();
        });
    }

    public void removeAimProcessor(UUID playerId) {
        this.playerAimProcessors.remove(playerId);
    }

    @Override // org.loger.session.ISessionManager
    public DataSession startSession(Player player, Label label, String comment) {
        UUID playerId = player.getUniqueId();
        if (hasActiveSession(player)) {
            stopSession(player);
        }
        AimProcessor aimProcessor = getOrCreateAimProcessor(playerId);
        DataSession session = new DataSession(playerId, player.getName(), label, comment, aimProcessor);
        this.activeSessions.put(playerId, session);
        this.plugin.getLogger().info("Started data collection for " + player.getName() + " [" + label + "]");
        return session;
    }

    @Override // org.loger.session.ISessionManager
    public void stopSession(Player player) {
        stopSession(player.getUniqueId());
    }

    @Override // org.loger.session.ISessionManager
    public void stopSession(UUID playerId) {
        DataSession session = this.activeSessions.remove(playerId);
        if (session != null) {
            try {
                session.saveAndClose(this.plugin, session.getLabel().name().toLowerCase());
            } catch (IOException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to save session for " + session.getPlayerName(), (Throwable) e);
            }
        }
    }

    @Override // org.loger.session.ISessionManager
    public void stopAllSessions() {
        for (UUID playerId : (UUID[]) this.activeSessions.keySet().toArray(new UUID[0])) {
            stopSession(playerId);
        }
        this.currentSessionFolder = null;
    }

    @Override // org.loger.session.ISessionManager
    public boolean hasActiveSession(Player player) {
        return hasActiveSession(player.getUniqueId());
    }

    @Override // org.loger.session.ISessionManager
    public boolean hasActiveSession(UUID playerId) {
        return this.activeSessions.containsKey(playerId);
    }

    @Override // org.loger.session.ISessionManager
    public DataSession getSession(UUID playerId) {
        return this.activeSessions.get(playerId);
    }

    @Override // org.loger.session.ISessionManager
    public DataSession getSession(Player player) {
        return getSession(player.getUniqueId());
    }

    @Override // org.loger.session.ISessionManager
    public Collection<DataSession> getActiveSessions() {
        return Collections.unmodifiableCollection(this.activeSessions.values());
    }

    @Override // org.loger.session.ISessionManager
    public int getActiveSessionCount() {
        return this.activeSessions.size();
    }

    @Override // org.loger.session.ISessionManager
    public String getCurrentSessionFolder() {
        return this.currentSessionFolder;
    }

    @Override // org.loger.session.ISessionManager
    public void onAttack(Player player) {
        DataSession session = this.activeSessions.get(player.getUniqueId());
        if (session != null) {
            session.onAttack();
        }
    }

    @Override // org.loger.session.ISessionManager
    public void onTick(Player player, float yaw, float pitch) {
        DataSession session = this.activeSessions.get(player.getUniqueId());
        if (session != null) {
            session.processTick(yaw, pitch);
        }
    }
}
