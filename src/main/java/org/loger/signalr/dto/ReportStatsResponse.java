package org.loger.signalr.dto;

public class ReportStatsResponse {
    public boolean limitExceeded;
    public int maxOnline;

    public boolean isLimitExceeded() {
        return this.limitExceeded;
    }

    public void setLimitExceeded(boolean limitExceeded) {
        this.limitExceeded = limitExceeded;
    }

    public int getMaxOnline() {
        return this.maxOnline;
    }

    public void setMaxOnline(int maxOnline) {
        this.maxOnline = maxOnline;
    }
}
