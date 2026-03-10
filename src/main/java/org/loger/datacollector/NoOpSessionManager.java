package org.loger.datacollector;

import org.loger.config.Label;
import org.loger.data.DataSession;
import org.loger.session.ISessionManager;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import org.bukkit.entity.Player;

public class NoOpSessionManager implements ISessionManager {
    @Override // org.loger.session.ISessionManager
    public DataSession startSession(Player player, Label label, String comment) {
        return null;
    }

    @Override // org.loger.session.ISessionManager
    public void stopSession(Player player) {
    }

    @Override // org.loger.session.ISessionManager
    public void stopSession(UUID playerId) {
    }

    @Override // org.loger.session.ISessionManager
    public void stopAllSessions() {
    }

    @Override // org.loger.session.ISessionManager
    public boolean hasActiveSession(Player player) {
        return false;
    }

    @Override // org.loger.session.ISessionManager
    public boolean hasActiveSession(UUID playerId) {
        return false;
    }

    @Override // org.loger.session.ISessionManager
    public DataSession getSession(UUID playerId) {
        return null;
    }

    @Override // org.loger.session.ISessionManager
    public DataSession getSession(Player player) {
        return null;
    }

    @Override // org.loger.session.ISessionManager
    public Collection<DataSession> getActiveSessions() {
        return Collections.emptyList();
    }

    @Override // org.loger.session.ISessionManager
    public int getActiveSessionCount() {
        return 0;
    }

    @Override // org.loger.session.ISessionManager
    public String getCurrentSessionFolder() {
        return null;
    }

    @Override // org.loger.session.ISessionManager
    public void onAttack(Player player) {
    }

    @Override // org.loger.session.ISessionManager
    public void onTick(Player player, float yaw, float pitch) {
    }
}
