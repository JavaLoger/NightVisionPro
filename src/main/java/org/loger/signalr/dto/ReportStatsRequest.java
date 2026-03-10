package org.loger.signalr.dto;

public class ReportStatsRequest {
    public int onlinePlayers;

    public ReportStatsRequest() {
    }

    public ReportStatsRequest(int onlinePlayers) {
        this.onlinePlayers = onlinePlayers;
    }

    public int getOnlinePlayers() {
        return this.onlinePlayers;
    }

    public void setOnlinePlayers(int onlinePlayers) {
        this.onlinePlayers = onlinePlayers;
    }
}
