package org.loger.util;

public final class GcdMath {
    public static final double MINIMUM_DIVISOR = ((Math.pow(0.20000000298023224d, 3.0d) * 8.0d) * 0.15d) - 0.001d;

    private GcdMath() {
    }

    public static float gcd(float a, float b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b > MINIMUM_DIVISOR) {
            float remainder = a % b;
            a = b;
            b = remainder;
        }
        return a;
    }

    public static double calculateAvg(double probability, double rotation, double aimbot, double gcd, double snap, double smooth, java.util.List<String> avgParams) {
        if (avgParams == null || avgParams.isEmpty()) {
            return (probability + rotation + aimbot + gcd + snap + smooth) / 5.0;
        }
        
        double sum = 0;
        int count = 0;
        
        for (String param : avgParams) {
            switch (param.toLowerCase()) {
                case "snap":
                    sum += snap;
                    count++;
                    break;
                case "gcd":
                    sum += gcd;
                    count++;
                    break;
                case "rotation":
                    sum += rotation;
                    count++;
                    break;
                case "smooth":
                    sum += smooth;
                    count++;
                    break;
                case "probability":
                    sum += probability;
                    count++;
                    break;
                case "aimbot":
                    sum += aimbot;
                    count++;
                    break;
            }
        }
        
        return count > 0 ? sum / count : 0;
    }
}
