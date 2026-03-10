package org.loger.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class FeatureCalculator {
    public double calculateDelta(double current, double previous) {
        double delta = current - previous;
        return normalizeAngle(delta);
    }

    public double calculateAcceleration(double currentDelta, double previousDelta) {
        return currentDelta - previousDelta;
    }

    public double calculateJerk(double currentAccel, double previousAccel) {
        return currentAccel - previousAccel;
    }

    public double calculateAngleToTarget(Player player, Entity target) {
        if (target == null || !target.isValid()) {
            return 0.0d;
        }
        Location playerEyeLocation = player.getEyeLocation();
        Location targetLocation = target.getLocation();
        Location targetLocation2 = targetLocation.add(0.0d, target.getHeight() / 2.0d, 0.0d);
        Vector lookDirection = playerEyeLocation.getDirection().normalize();
        Vector toTarget = targetLocation2.toVector().subtract(playerEyeLocation.toVector()).normalize();
        double dotProduct = lookDirection.dot(toTarget);
        return Math.toDegrees(Math.acos(Math.max(-1.0d, Math.min(1.0d, dotProduct))));
    }

    public double calculateAngleReductionSpeed(double currentAngle, double previousAngle) {
        return currentAngle - previousAngle;
    }

    public double calculateStandardDeviation(double[] values) {
        if (values == null || values.length == 0) {
            return 0.0d;
        }
        int n = values.length;
        double sum = 0.0d;
        for (double value : values) {
            sum += value;
        }
        double mean = sum / ((double) n);
        double varianceSum = 0.0d;
        for (double value2 : values) {
            double diff = value2 - mean;
            varianceSum += diff * diff;
        }
        double variance = varianceSum / ((double) n);
        return Math.sqrt(variance);
    }

    public double normalizeAngle(double angle) {
        double angle2 = angle % 360.0d;
        if (angle2 > 180.0d) {
            return angle2 - 360.0d;
        }
        if (angle2 < -180.0d) {
            return angle2 + 360.0d;
        }
        return angle2;
    }
}
