package org.loger.data;

import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ProbabilityHistory {
    private static final long DAY_MS = 86400000;
    private static final double DECAY_RATE = 0.98d;
    private static final long DECAY_TIMEOUT_MS = 300000;
    private static final long HOUR_MS = 3600000;
    private static final long MAX_CACHE_MS = 1000;
    private static final long SIX_HOURS_MS = 21600000;
    private final Map<UUID, Deque<DetectionEntry>> historyMap = new ConcurrentHashMap();
    private final Map<UUID, Long> joinTimeMap = new ConcurrentHashMap();
    private final Map<UUID, Long> lastAlertTimeMap = new ConcurrentHashMap();
    private final Map<UUID, PlayerScores> currentScoresMap = new ConcurrentHashMap();
    private final Map<UUID, MaxValuesCache> maxValuesCacheMap = new ConcurrentHashMap();

    public void recordDetection(UUID playerId, double probability, double rotation, double aimbot, double gcd, double snap, double smooth) {
        long now = System.currentTimeMillis();
        Deque<DetectionEntry> history = this.historyMap.computeIfAbsent(playerId, k -> {
            return new ConcurrentLinkedDeque();
        });
        history.addLast(new DetectionEntry(now, probability, rotation, aimbot, gcd, snap, smooth));
        cleanupOldEntries(playerId);
        this.lastAlertTimeMap.put(playerId, Long.valueOf(now));
        PlayerScores scores = this.currentScoresMap.computeIfAbsent(playerId, k2 -> {
            return new PlayerScores();
        });
        scores.update(probability, rotation, aimbot, gcd, snap, smooth);
        this.maxValuesCacheMap.remove(playerId);
    }

    public void recordProbability(UUID playerId, double probability) {
        recordDetection(playerId, probability, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d);
    }

    public void applyDecay(UUID playerId) {
        PlayerScores scores;
        Long lastAlert = this.lastAlertTimeMap.get(playerId);
        if (lastAlert == null) {
            return;
        }
        long timeSinceAlert = System.currentTimeMillis() - lastAlert.longValue();
        if (timeSinceAlert >= DECAY_TIMEOUT_MS && (scores = this.currentScoresMap.get(playerId)) != null) {
            scores.decay(DECAY_RATE);
        }
    }

    public PlayerScores getPlayerScores(UUID playerId) {
        applyDecay(playerId);
        return this.currentScoresMap.computeIfAbsent(playerId, k -> {
            return new PlayerScores();
        });
    }

    public void recordJoinTime(UUID playerId) {
        this.joinTimeMap.putIfAbsent(playerId, Long.valueOf(System.currentTimeMillis()));
    }

    public long getPlayTime(UUID playerId) {
        Long joinTime = this.joinTimeMap.get(playerId);
        if (joinTime == null) {
            return 0L;
        }
        return System.currentTimeMillis() - joinTime.longValue();
    }

    public String getFormattedPlayTime(UUID playerId) {
        long playTimeMs = getPlayTime(playerId);
        long seconds = playTimeMs / MAX_CACHE_MS;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        if (hours > 0) {
            return String.format("%dч %02dм", Long.valueOf(hours), Long.valueOf(minutes % 60));
        }
        if (minutes > 0) {
            return String.format("%dм %02dс", Long.valueOf(minutes), Long.valueOf(seconds % 60));
        }
        return String.format("%dс", Long.valueOf(seconds));
    }

    public double getMaxValue(UUID playerId, long periodMs, String field) {
        Deque<DetectionEntry> history = this.historyMap.get(playerId);
        if (history == null || history.isEmpty()) {
            return 0.0d;
        }
        long cutoff = System.currentTimeMillis() - periodMs;
        double max = 0.0d;
        for (DetectionEntry entry : history) {
            if (entry.timestamp >= cutoff) {
                double value = entry.getValue(field);
                if (value > max) {
                    max = value;
                }
            }
        }
        return max;
    }

    private MaxValuesCache getOrComputeMaxCache(UUID playerId) {
        long ts;
        MaxValuesCache cached = this.maxValuesCacheMap.get(playerId);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.timestamp < MAX_CACHE_MS) {
            return cached;
        }
        Deque<DetectionEntry> history = this.historyMap.get(playerId);
        if (history == null || history.isEmpty()) {
            MaxValuesCache empty = new MaxValuesCache();
            this.maxValuesCacheMap.put(playerId, empty);
            return empty;
        }
        long cutoff1h = now - HOUR_MS;
        long cutoff6h = now - SIX_HOURS_MS;
        long cutoffAll = now - 604800000;
        double maxProb1h = 0.0d;
        double maxProb6h = 0.0d;
        double maxProbAll = 0.0d;
        double maxRot = 0.0d;
        double maxAim = 0.0d;
        double maxGcd = 0.0d;
        double maxSnap = 0.0d;
        double maxSmooth = 0.0d;
        double maxRotAll = 0.0d;
        double maxAimAll = 0.0d;
        double maxGcdAll = 0.0d;
        double maxSnapAll = 0.0d;
        double maxSmoothAll = 0.0d;
        Iterator<DetectionEntry> it = history.iterator();
        while (it.hasNext()) {
            MaxValuesCache cached2 = cached;
            DetectionEntry entry = it.next();
            long now2 = now;
            Iterator<DetectionEntry> it2 = it;
            long ts2 = entry.timestamp;
            if (ts2 < cutoff1h) {
                ts = ts2;
            } else {
                ts = ts2;
                if (entry.probability > maxProb1h) {
                    maxProb1h = entry.probability;
                }
                if (entry.rotation > maxRot) {
                    maxRot = entry.rotation;
                }
                if (entry.aimbot > maxAim) {
                    maxAim = entry.aimbot;
                }
                if (entry.gcd > maxGcd) {
                    maxGcd = entry.gcd;
                }
                if (entry.snap > maxSnap) {
                    maxSnap = entry.snap;
                }
                if (entry.smooth > maxSmooth) {
                    maxSmooth = entry.smooth;
                }
            }
            if (ts >= cutoff6h && entry.probability > maxProb6h) {
                maxProb6h = entry.probability;
            }
            if (ts >= cutoffAll) {
                if (entry.probability > maxProbAll) {
                    maxProbAll = entry.probability;
                }
                if (entry.rotation > maxRotAll) {
                    maxRotAll = entry.rotation;
                }
                if (entry.aimbot > maxAimAll) {
                    maxAimAll = entry.aimbot;
                }
                if (entry.gcd > maxGcdAll) {
                    maxGcdAll = entry.gcd;
                }
                if (entry.snap > maxSnapAll) {
                    maxSnapAll = entry.snap;
                }
                if (entry.smooth > maxSmoothAll) {
                    maxSmoothAll = entry.smooth;
                }
            }
            it = it2;
            cached = cached2;
            now = now2;
        }
        MaxValuesCache result = new MaxValuesCache(now, maxProb1h, maxProb6h, maxProbAll, maxRot, maxAim, maxGcd, maxSnap, maxSmooth, maxRotAll, maxAimAll, maxGcdAll, maxSnapAll, maxSmoothAll);
        this.maxValuesCacheMap.put(playerId, result);
        return result;
    }

    public double getProbability1Hour(UUID playerId) {
        return getOrComputeMaxCache(playerId).prob1h;
    }

    public double getProbability6Hours(UUID playerId) {
        return getOrComputeMaxCache(playerId).prob6h;
    }

    public double getProbabilityAllTime(UUID playerId) {
        return getOrComputeMaxCache(playerId).probAll;
    }

    public double getRotationMax1Hour(UUID playerId) {
        return getOrComputeMaxCache(playerId).rotation;
    }

    public double getAimbotMax1Hour(UUID playerId) {
        return getOrComputeMaxCache(playerId).aimbot;
    }

    public double getGcdMax1Hour(UUID playerId) {
        return getOrComputeMaxCache(playerId).gcd;
    }

    public double getSnapMax1Hour(UUID playerId) {
        return getOrComputeMaxCache(playerId).snap;
    }

    public double getSmoothMax1Hour(UUID playerId) {
        return getOrComputeMaxCache(playerId).smooth;
    }

    public double getRotationMaxAllTime(UUID playerId) {
        return getOrComputeMaxCache(playerId).rotationAll;
    }

    public double getAimbotMaxAllTime(UUID playerId) {
        return getOrComputeMaxCache(playerId).aimbotAll;
    }

    public double getGcdMaxAllTime(UUID playerId) {
        return getOrComputeMaxCache(playerId).gcdAll;
    }

    public double getSnapMaxAllTime(UUID playerId) {
        return getOrComputeMaxCache(playerId).snapAll;
    }

    public double getSmoothMaxAllTime(UUID playerId) {
        return getOrComputeMaxCache(playerId).smoothAll;
    }

    public double getProbabilityForPeriod(UUID playerId, long hours, long days) {
        long periodMs = days > 0 ? days * DAY_MS : 0L;
        if (hours > 0) {
            periodMs += HOUR_MS * hours;
        }
        return periodMs == 0 ? getProbabilityAllTime(playerId) : getMaxValue(playerId, periodMs, "probability");
    }

    public long getTimeSinceLastAlert(UUID playerId) {
        Long lastAlert = this.lastAlertTimeMap.get(playerId);
        if (lastAlert == null) {
            return -1L;
        }
        return (System.currentTimeMillis() - lastAlert.longValue()) / MAX_CACHE_MS;
    }

    private void cleanupOldEntries(UUID playerId) {
        Deque<DetectionEntry> history = this.historyMap.get(playerId);
        if (history == null) {
            return;
        }
        long cutoff = System.currentTimeMillis() - 604800000;
        while (!history.isEmpty() && history.peekFirst().timestamp < cutoff) {
            history.pollFirst();
        }
    }

    public void clearPlayer(UUID playerId) {
        this.historyMap.remove(playerId);
        this.joinTimeMap.remove(playerId);
        this.lastAlertTimeMap.remove(playerId);
        this.currentScoresMap.remove(playerId);
        this.maxValuesCacheMap.remove(playerId);
    }

    public void clearAll() {
        this.historyMap.clear();
        this.joinTimeMap.clear();
        this.lastAlertTimeMap.clear();
        this.currentScoresMap.clear();
        this.maxValuesCacheMap.clear();
    }

    public static class PlayerScores {
        public double probability = 0.0d;
        public double rotation = 0.0d;
        public double aimbot = 0.0d;
        public double gcd = 0.0d;
        public double snap = 0.0d;
        public double smooth = 0.0d;

        public void update(double prob, double rot, double aim, double g, double sn, double sm) {
            this.probability = Math.min(0.9999d, (Math.max(this.probability, prob) * 0.7d) + (prob * 0.3d));
            this.rotation = Math.min(0.9999d, (Math.max(this.rotation, rot) * 0.7d) + (rot * 0.3d));
            this.aimbot = Math.min(0.9999d, (Math.max(this.aimbot, aim) * 0.7d) + (aim * 0.3d));
            this.gcd = Math.min(0.9999d, (Math.max(this.gcd, g) * 0.7d) + (g * 0.3d));
            this.snap = Math.min(0.9999d, (Math.max(this.snap, sn) * 0.7d) + (sn * 0.3d));
            this.smooth = Math.min(0.9999d, (Math.max(this.smooth, sm) * 0.7d) + (0.3d * sm));
            double coreMax = Math.max(this.aimbot, Math.max(this.gcd, this.snap));
            if (coreMax > 0.7d && this.probability > 0.6d) {
                double influence = (this.probability + coreMax) / 2.0d;
                double boost = (influence - 0.5d) * 0.01d;
                this.rotation = Math.min(0.9999d, this.rotation + boost);
                this.smooth = Math.min(0.9999d, this.smooth + boost);
                this.probability = Math.min(0.9999d, this.probability + (boost * 0.5d));
            }
        }

        public void decay(double rate) {
            this.probability *= rate;
            this.rotation *= rate;
            this.aimbot *= rate;
            this.gcd *= rate;
            this.snap *= rate;
            this.smooth *= rate;
        }

        public double getAvg() {
            return (((((this.probability + this.rotation) + this.aimbot) + this.gcd) + this.snap) + this.smooth) / 6.0d;
        }
    }

    private static class DetectionEntry {
        final double aimbot;
        final double gcd;
        final double probability;
        final double rotation;
        final double smooth;
        final double snap;
        final long timestamp;

        DetectionEntry(long timestamp, double probability, double rotation, double aimbot, double gcd, double snap, double smooth) {
            this.timestamp = timestamp;
            this.probability = probability;
            this.rotation = rotation;
            this.aimbot = aimbot;
            this.gcd = gcd;
            this.snap = snap;
            this.smooth = smooth;
        }

        public double getValue(String field) {
            switch (field.toLowerCase()) {
                case "snap":
                    return snap;
                case "gcd":
                    return gcd;
                case "rotation":
                    return rotation;
                case "smooth":
                    return smooth;
                case "probability":
                    return probability;
                case "aimbot":
                    return aimbot;
                default:
                    return 0;
            }
        }
    }

    private static class MaxValuesCache {
        final double aimbot;
        final double aimbotAll;
        final double gcd;
        final double gcdAll;
        final double prob1h;
        final double prob6h;
        final double probAll;
        final double rotation;
        final double rotationAll;
        final double smooth;
        final double smoothAll;
        final double snap;
        final double snapAll;
        final long timestamp;

        MaxValuesCache() {
            this.timestamp = System.currentTimeMillis();
            this.prob1h = 0.0d;
            this.prob6h = 0.0d;
            this.probAll = 0.0d;
            this.rotation = 0.0d;
            this.aimbot = 0.0d;
            this.gcd = 0.0d;
            this.snap = 0.0d;
            this.smooth = 0.0d;
            this.rotationAll = 0.0d;
            this.aimbotAll = 0.0d;
            this.gcdAll = 0.0d;
            this.snapAll = 0.0d;
            this.smoothAll = 0.0d;
        }

        MaxValuesCache(long ts, double p1h, double p6h, double pAll, double rot, double aim, double g, double sn, double sm, double rotAll, double aimAll, double gAll, double snAll, double smAll) {
            this.timestamp = ts;
            this.prob1h = p1h;
            this.prob6h = p6h;
            this.probAll = pAll;
            this.rotation = rot;
            this.aimbot = aim;
            this.gcd = g;
            this.snap = sn;
            this.smooth = sm;
            this.rotationAll = rotAll;
            this.aimbotAll = aimAll;
            this.gcdAll = gAll;
            this.snapAll = snAll;
            this.smoothAll = smAll;
        }
    }
}
