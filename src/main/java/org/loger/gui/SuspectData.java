package org.loger.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class SuspectData {
    private static final int MAX_CRIT_VALUES = 15;
    private final UUID playerId;
    private final String playerName;
    private final List<Double> critValues = new ArrayList();
    private final List<Double> aimValues = new ArrayList();
    private int actionCount = 0;
    private double lastProbability = 0.0d;
    private double probability1h = 0.0d;
    private double probability6h = 0.0d;
    private double probabilityAll = 0.0d;
    private double rotation = 0.0d;
    private double aimbot = 0.0d;
    private double gcd = 0.0d;
    private double snap = 0.0d;
    private double smooth = 0.0d;
    private double rotationMax = 0.0d;
    private double aimbotMax = 0.0d;
    private double gcdMax = 0.0d;
    private double snapMax = 0.0d;
    private double smoothMax = 0.0d;
    private double rotationMaxAll = 0.0d;
    private double aimbotMaxAll = 0.0d;
    private double gcdMaxAll = 0.0d;
    private double snapMaxAll = 0.0d;
    private double smoothMaxAll = 0.0d;
    private int vl = 0;
    private long lastTime = System.currentTimeMillis();
    private String server = "default";

    public SuspectData(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public List<Double> getCritValues() {
        return this.critValues;
    }

    public List<Double> getAimValues() {
        return this.aimValues;
    }

    public void addCritValue(double rotation, double aimbot, double gcd, double snap) {
        double avg = (((rotation + aimbot) + gcd) + snap) / 4.0d;
        this.critValues.add(0, Double.valueOf(avg));
        if (this.critValues.size() > 15) {
            this.critValues.remove(this.critValues.size() - 1);
        }
        double aimAvg = ((aimbot + gcd) + snap) / 3.0d;
        this.aimValues.add(0, Double.valueOf(aimAvg));
        if (this.aimValues.size() > 15) {
            this.aimValues.remove(this.aimValues.size() - 1);
        }
        updateAverageFromCrits();
    }

    private void updateAverageFromCrits() {
        if (this.critValues.isEmpty()) {
            return;
        }
        double sum = 0.0d;
        Iterator<Double> it = this.critValues.iterator();
        while (it.hasNext()) {
            double val = it.next().doubleValue();
            sum += val;
        }
        this.lastProbability = sum / ((double) this.critValues.size());
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public int getCount() {
        return this.actionCount;
    }

    public int getActionCount() {
        return this.actionCount;
    }

    public double getLastProbability() {
        return this.lastProbability;
    }

    public double getProbability1h() {
        return this.probability1h;
    }

    public double getProbability6h() {
        return this.probability6h;
    }

    public double getProbabilityAll() {
        return this.probabilityAll;
    }

    public double getRotation() {
        return this.rotation;
    }

    public double getAimbot() {
        return this.aimbot;
    }

    public double getGcd() {
        return this.gcd;
    }

    public double getSnap() {
        return this.snap;
    }

    public double getSmooth() {
        return this.smooth;
    }

    public double getRotationMax() {
        return this.rotationMax;
    }

    public double getAimbotMax() {
        return this.aimbotMax;
    }

    public double getGcdMax() {
        return this.gcdMax;
    }

    public double getSnapMax() {
        return this.snapMax;
    }

    public double getSmoothMax() {
        return this.smoothMax;
    }

    public double getRotationMaxAll() {
        return this.rotationMaxAll;
    }

    public double getAimbotMaxAll() {
        return this.aimbotMaxAll;
    }

    public double getGcdMaxAll() {
        return this.gcdMaxAll;
    }

    public double getSnapMaxAll() {
        return this.snapMaxAll;
    }

    public double getSmoothMaxAll() {
        return this.smoothMaxAll;
    }

    public int getVl() {
        return this.vl;
    }

    public long getLastTime() {
        return this.lastTime;
    }

    public String getServer() {
        return this.server;
    }

    public double getAvg() {
        if (!this.critValues.isEmpty()) {
            double sum = 0.0d;
            Iterator<Double> it = this.critValues.iterator();
            while (it.hasNext()) {
                double val = it.next().doubleValue();
                sum += val;
            }
            return sum / ((double) this.critValues.size());
        }
        double sum2 = this.rotation;
        return ((((((((((((((sum2 + this.aimbot) + this.gcd) + this.snap) + this.smooth) + this.rotationMax) + this.aimbotMax) + this.gcdMax) + this.snapMax) + this.smoothMax) + this.rotationMaxAll) + this.aimbotMaxAll) + this.gcdMaxAll) + this.snapMaxAll) + this.smoothMaxAll) / 15.0d;
    }

    public double getAvg(java.util.List<String> avgParams) {
        return org.loger.util.GcdMath.calculateAvg(this.lastProbability, this.rotation, this.aimbot, this.gcd, this.snap, this.smooth, avgParams);
    }

    public void incrementCount() {
        this.actionCount++;
        this.lastTime = System.currentTimeMillis();
    }

    public void setActionCount(int count) {
        this.actionCount = count;
    }

    public void updateData(double probability, int vl) {
        this.lastProbability = probability;
        this.vl = vl;
        this.lastTime = System.currentTimeMillis();
    }

    public void updateProbabilityHistory(double prob1h, double prob6h, double probAll) {
        this.probability1h = prob1h;
        this.probability6h = prob6h;
        this.probabilityAll = probAll;
    }

    public void updateScores(double rot, double aim, double g, double sn, double sm) {
        this.rotation = rot;
        this.aimbot = aim;
        this.gcd = g;
        this.snap = sn;
        this.smooth = sm;
    }

    public void updateMaxScores(double rotMax, double aimMax, double gMax, double snMax, double smMax) {
        this.rotationMax = rotMax;
        this.aimbotMax = aimMax;
        this.gcdMax = gMax;
        this.snapMax = snMax;
        this.smoothMax = smMax;
    }

    public void updateMaxScoresAll(double rot, double aim, double g, double sn, double sm) {
        this.rotationMaxAll = rot;
        this.aimbotMaxAll = aim;
        this.gcdMaxAll = g;
        this.snapMaxAll = sn;
        this.smoothMaxAll = sm;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getFormattedTime() {
        long diff = System.currentTimeMillis() - this.lastTime;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        if (hours > 0) {
            return hours + " ч. назад";
        }
        if (minutes > 0) {
            return minutes + " мин. назад";
        }
        return seconds + " сек. назад";
    }

    public String getProgressBar(int length, String filled, String empty) {
        int filledCount = (int) (this.lastProbability * ((double) length));
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < length) {
            sb.append(i < filledCount ? filled : empty);
            i++;
        }
        return sb.toString();
    }

    public String getPercent() {
        return String.format("%.0f%%", Double.valueOf(getAvg() * 100.0d));
    }
}
