package org.loger.server;

public class AIResponse {
    private final double aimbot;
    private final String error;
    private final double gcd;
    private final double probability;
    private final double rotation;
    private final double smooth;
    private final double snap;

    public AIResponse(double probability) {
        this(probability, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, null);
    }

    public AIResponse(double probability, String error) {
        this(probability, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, error);
    }

    public AIResponse(double probability, double rotation, double aimbot, double gcd, double snap, double smooth, String error) {
        this.probability = probability;
        this.rotation = rotation;
        this.aimbot = aimbot;
        this.gcd = gcd;
        this.snap = snap;
        this.smooth = smooth;
        this.error = error;
    }

    public double getProbability() {
        return this.probability;
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

    public String getError() {
        return this.error;
    }

    public boolean hasError() {
        return (this.error == null || this.error.isEmpty()) ? false : true;
    }

    public static AIResponse fromJson(String json) {
        int colonIndex;
        int start;
        int end;
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            String trimmed = json.trim();
            int errorIndex = trimmed.indexOf("\"error\"");
            if (errorIndex != -1 && (colonIndex = trimmed.indexOf(58, errorIndex)) != -1 && (start = trimmed.indexOf(34, colonIndex + 1)) != -1 && (end = trimmed.indexOf(34, start + 1)) != -1) {
                String errorMsg = trimmed.substring(start + 1, end);
                return new AIResponse(0.0d, errorMsg);
            }
            double probability = parseDouble(trimmed, "probability");
            double rotation = parseDouble(trimmed, "rotation");
            double aimbot = parseDouble(trimmed, "aimbot");
            double gcd = parseDouble(trimmed, "gcd");
            double snap = parseDouble(trimmed, "snap");
            double smooth = parseDouble(trimmed, "smooth");
            return new AIResponse(probability, rotation, aimbot, gcd, snap, smooth, null);
        } catch (Exception e) {
            return null;
        }
    }

    private static double parseDouble(String json, String key) {
        int colonIndex;
        char c;
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1 || (colonIndex = json.indexOf(58, keyIndex)) == -1) {
            return 0.0d;
        }
        int start = colonIndex + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < json.length() && (c = json.charAt(end)) != ',' && c != '}' && !Character.isWhitespace(c)) {
            end++;
        }
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0.0d;
        }
    }

    public String toJson() {
        return String.format("{\"probability\":%.4f,\"rotation\":%.4f,\"aimbot\":%.4f,\"gcd\":%.4f,\"snap\":%.4f,\"smooth\":%.4f}", Double.valueOf(this.probability), Double.valueOf(this.rotation), Double.valueOf(this.aimbot), Double.valueOf(this.gcd), Double.valueOf(this.snap), Double.valueOf(this.smooth));
    }

    public String toString() {
        return String.format("AIResponse{prob=%.4f,rot=%.4f,aim=%.4f,gcd=%.4f,snap=%.4f,smooth=%.4f}", Double.valueOf(this.probability), Double.valueOf(this.rotation), Double.valueOf(this.aimbot), Double.valueOf(this.gcd), Double.valueOf(this.snap), Double.valueOf(this.smooth));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AIResponse that = (AIResponse) obj;
        if (Double.compare(that.probability, this.probability) == 0) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        long temp = Double.doubleToLongBits(this.probability);
        return (int) ((temp >>> 32) ^ temp);
    }
}
