package org.loger.util;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class RunningMode {
    private static final double THRESHOLD = 0.001d;
    private final Queue<Double> addList;
    private final int maxSize;
    private final Map<Double, Integer> popularityMap;

    public RunningMode(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("There's no mode to a size 0 or negative list!");
        }
        this.addList = new ArrayDeque(maxSize);
        this.popularityMap = new HashMap();
        this.maxSize = maxSize;
    }

    public int size() {
        return this.addList.size();
    }

    public int getMaxSize() {
        return this.maxSize;
    }

    public void add(double value) {
        pop();
        for (Map.Entry<Double, Integer> entry : this.popularityMap.entrySet()) {
            if (Math.abs(entry.getKey().doubleValue() - value) < THRESHOLD) {
                entry.setValue(Integer.valueOf(entry.getValue().intValue() + 1));
                this.addList.add(entry.getKey());
                return;
            }
        }
        this.popularityMap.put(Double.valueOf(value), 1);
        this.addList.add(Double.valueOf(value));
    }

    private void pop() {
        Double type;
        Integer popularity;
        if (this.addList.size() >= this.maxSize && (type = this.addList.poll()) != null && (popularity = this.popularityMap.get(type)) != null) {
            if (popularity.intValue() == 1) {
                this.popularityMap.remove(type);
            } else {
                this.popularityMap.put(type, Integer.valueOf(popularity.intValue() - 1));
            }
        }
    }

    public Pair<Double, Integer> getMode() {
        int max = 0;
        Double mostPopular = null;
        for (Map.Entry<Double, Integer> entry : this.popularityMap.entrySet()) {
            if (entry.getValue().intValue() > max) {
                max = entry.getValue().intValue();
                Double mostPopular2 = entry.getKey();
                mostPopular = mostPopular2;
            }
        }
        return new Pair<>(mostPopular, Integer.valueOf(max));
    }

    public void clear() {
        this.addList.clear();
        this.popularityMap.clear();
    }
}
