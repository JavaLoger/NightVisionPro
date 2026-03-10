package org.loger.util;

public class BufferCalculator {
    private static final double LOW_PROBABILITY_THRESHOLD = 0.1d;

    public static double calculateBufferIncrease(double probability, double multiplier, double threshold) {
        if (probability <= threshold) {
            return 0.0d;
        }
        return (probability - threshold) * multiplier;
    }

    public static double calculateBufferDecrease(double currentBuffer, double decreaseAmount) {
        return Math.max(0.0d, currentBuffer - decreaseAmount);
    }

    public static double updateBuffer(double currentBuffer, double probability, double multiplier, double decreaseAmount, double threshold) {
        if (probability > threshold) {
            return calculateBufferIncrease(probability, multiplier, threshold) + currentBuffer;
        }
        return probability < LOW_PROBABILITY_THRESHOLD ? calculateBufferDecrease(currentBuffer, decreaseAmount) : currentBuffer;
    }

    public static double updateBuffer(double currentBuffer, double probability, double multiplier, double decreaseAmount) {
        return updateBuffer(currentBuffer, probability, multiplier, decreaseAmount, 0.5d);
    }

    public static boolean shouldFlag(double buffer, double flagThreshold) {
        return buffer >= flagThreshold;
    }

    public static double resetBuffer(double resetValue) {
        return Math.max(0.0d, resetValue);
    }
}
