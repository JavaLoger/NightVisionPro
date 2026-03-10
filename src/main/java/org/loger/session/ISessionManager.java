package org.loger.session;

import org.loger.config.Label;
import org.loger.data.DataSession;
import java.util.Collection;
import java.util.UUID;
import org.bukkit.entity.Player;

public interface ISessionManager {
    int getActiveSessionCount();

    Collection<DataSession> getActiveSessions();

    String getCurrentSessionFolder();

    DataSession getSession(UUID uuid);

    DataSession getSession(Player player);

    boolean hasActiveSession(UUID uuid);

    boolean hasActiveSession(Player player);

    void onAttack(Player player);

    void onTick(Player player, float f, float f2);

    DataSession startSession(Player player, Label label, String str);

    void stopAllSessions();

    void stopSession(UUID uuid);

    void stopSession(Player player);
}
