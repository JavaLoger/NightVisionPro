package org.loger.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.loger.checks.AICheck;
import org.loger.session.ISessionManager;
import org.bukkit.entity.Player;

public class RotationListener extends PacketListenerAbstract {
    private final AICheck aiCheck;
    private final ISessionManager sessionManager;

    public RotationListener(ISessionManager sessionManager, AICheck aiCheck) {
        super(PacketListenerPriority.NORMAL);
        this.sessionManager = sessionManager;
        this.aiCheck = aiCheck;
    }

    public void onPacketReceive(PacketReceiveEvent event) {
        Player player;
        try {
            if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) || (player = (Player) event.getPlayer()) == null) {
                return;
            }
            WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(event);
            if (!packet.hasRotationChanged()) {
                return;
            }
            float yaw = packet.getLocation().getYaw();
            float pitch = packet.getLocation().getPitch();
            if (this.aiCheck != null) {
                this.aiCheck.onRotationPacket(player, yaw, pitch);
            }
            if (this.sessionManager.hasActiveSession(player)) {
                this.sessionManager.onTick(player, yaw, pitch);
            }
        } catch (Exception e) {
        }
    }
}
