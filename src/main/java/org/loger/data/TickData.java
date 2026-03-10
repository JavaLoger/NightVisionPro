package org.loger.data;

import java.util.Locale;
import java.util.StringJoiner;

public final class TickData {
    public final float accelPitch;
    public final float accelYaw;
    public final float deltaPitch;
    public final float deltaYaw;
    public final float gcdErrorPitch;
    public final float gcdErrorYaw;
    public final float jerkPitch;
    public final float jerkYaw;

    public TickData(float deltaYaw, float deltaPitch, float accelYaw, float accelPitch, float jerkYaw, float jerkPitch, float gcdErrorYaw, float gcdErrorPitch) {
        this.deltaYaw = deltaYaw;
        this.deltaPitch = deltaPitch;
        this.accelYaw = accelYaw;
        this.accelPitch = accelPitch;
        this.jerkYaw = jerkYaw;
        this.jerkPitch = jerkPitch;
        this.gcdErrorYaw = gcdErrorYaw;
        this.gcdErrorPitch = gcdErrorPitch;
    }

    public static String getHeader() {
        return "is_cheating,delta_yaw,delta_pitch,accel_yaw,accel_pitch,jerk_yaw,jerk_pitch,gcd_error_yaw,gcd_error_pitch";
    }

    public String toCsv(String str) {
        boolean zEqualsIgnoreCase = str.equalsIgnoreCase("CHEAT");
        StringJoiner stringJoiner = new StringJoiner(",");
        stringJoiner.add(String.valueOf(zEqualsIgnoreCase ? 1 : 0));
        stringJoiner.add(String.format(Locale.US, "%.6f", Float.valueOf(this.deltaYaw)));
        stringJoiner.add(String.format(Locale.US, "%.6f", Float.valueOf(this.deltaPitch)));
        stringJoiner.add(String.format(Locale.US, "%.6f", Float.valueOf(this.accelYaw)));
        stringJoiner.add(String.format(Locale.US, "%.6f", Float.valueOf(this.accelPitch)));
        stringJoiner.add(String.format(Locale.US, "%.6f", Float.valueOf(this.jerkYaw)));
        stringJoiner.add(String.format(Locale.US, "%.6f", Float.valueOf(this.jerkPitch)));
        stringJoiner.add(String.format(Locale.US, "%.6f", Float.valueOf(this.gcdErrorYaw)));
        stringJoiner.add(String.format(Locale.US, "%.6f", Float.valueOf(this.gcdErrorPitch)));
        return stringJoiner.toString();
    }

    public String toString() {
        return String.format("TickData[dYaw=%.4f, dPitch=%.4f, aYaw=%.4f, aPitch=%.4f, jYaw=%.4f, jPitch=%.4f, gcdYaw=%.4f, gcdPitch=%.4f]", Float.valueOf(this.deltaYaw), Float.valueOf(this.deltaPitch), Float.valueOf(this.accelYaw), Float.valueOf(this.accelPitch), Float.valueOf(this.jerkYaw), Float.valueOf(this.jerkPitch), Float.valueOf(this.gcdErrorYaw), Float.valueOf(this.gcdErrorPitch));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TickData)) {
            return false;
        }
        TickData other = (TickData) obj;
        return Float.compare(this.deltaYaw, other.deltaYaw) == 0 && Float.compare(this.deltaPitch, other.deltaPitch) == 0 && Float.compare(this.accelYaw, other.accelYaw) == 0 && Float.compare(this.accelPitch, other.accelPitch) == 0 && Float.compare(this.jerkYaw, other.jerkYaw) == 0 && Float.compare(this.jerkPitch, other.jerkPitch) == 0 && Float.compare(this.gcdErrorYaw, other.gcdErrorYaw) == 0 && Float.compare(this.gcdErrorPitch, other.gcdErrorPitch) == 0;
    }

    public int hashCode() {
        int result = Float.hashCode(this.deltaYaw);
        return (((((((((((((result * 31) + Float.hashCode(this.deltaPitch)) * 31) + Float.hashCode(this.accelYaw)) * 31) + Float.hashCode(this.accelPitch)) * 31) + Float.hashCode(this.jerkYaw)) * 31) + Float.hashCode(this.jerkPitch)) * 31) + Float.hashCode(this.gcdErrorYaw)) * 31) + Float.hashCode(this.gcdErrorPitch);
    }
}
