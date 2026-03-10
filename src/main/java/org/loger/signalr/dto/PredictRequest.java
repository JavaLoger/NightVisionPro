package org.loger.signalr.dto;

public class PredictRequest {
    public byte[] playerData;
    public String playerUuid;

    public PredictRequest() {
    }

    public PredictRequest(byte[] playerData, String playerUuid) {
        this.playerData = playerData;
        this.playerUuid = playerUuid;
    }
}
