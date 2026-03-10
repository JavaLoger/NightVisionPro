package org.loger.util;

import org.loger.data.TickData;

public class AimProcessor {
    private static final float MAX_DELTA_FOR_GCD = 5.0f;
    private static final int SIGNIFICANT_SAMPLES_THRESHOLD = 15;
    private static final int TOTAL_SAMPLES_THRESHOLD = 80;
    private float currentPitchAccel;
    private float currentYawAccel;
    private boolean hasLastRotation;
    private float lastDeltaPitch;
    private float lastDeltaYaw;
    private float lastPitch;
    private float lastPitchAccel;
    private float lastXRot;
    private float lastYRot;
    private float lastYaw;
    private float lastYawAccel;
    private double modeX;
    private double modeY;
    private final RunningMode xRotMode;
    private final RunningMode yRotMode;

    public AimProcessor() {
        this(TOTAL_SAMPLES_THRESHOLD);
    }

    public AimProcessor(int modeSize) {
        this.xRotMode = new RunningMode(modeSize);
        this.yRotMode = new RunningMode(modeSize);
        reset();
    }

    public void reset() {
        this.lastYaw = 0.0f;
        this.lastPitch = 0.0f;
        this.lastXRot = 0.0f;
        this.lastYRot = 0.0f;
        this.lastDeltaYaw = 0.0f;
        this.lastDeltaPitch = 0.0f;
        this.lastYawAccel = 0.0f;
        this.lastPitchAccel = 0.0f;
        this.currentYawAccel = 0.0f;
        this.currentPitchAccel = 0.0f;
        this.modeX = 0.0d;
        this.modeY = 0.0d;
        this.hasLastRotation = false;
        this.xRotMode.clear();
        this.yRotMode.clear();
    }

    public TickData process(float yaw, float pitch) {
        float deltaYaw = this.hasLastRotation ? normalizeAngle(yaw - this.lastYaw) : 0.0f;
        float deltaPitch = this.hasLastRotation ? pitch - this.lastPitch : 0.0f;
        float deltaYawAbs = Math.abs(deltaYaw);
        float deltaPitchAbs = Math.abs(deltaPitch);
        this.lastYawAccel = this.currentYawAccel;
        this.lastPitchAccel = this.currentPitchAccel;
        this.currentYawAccel = deltaYawAbs - Math.abs(this.lastDeltaYaw);
        this.currentPitchAccel = deltaPitchAbs - Math.abs(this.lastDeltaPitch);
        float jerkYaw = this.currentYawAccel - this.lastYawAccel;
        float jerkPitch = this.currentPitchAccel - this.lastPitchAccel;
        if (this.hasLastRotation) {
            double divisorX = GcdMath.gcd(deltaYawAbs, this.lastXRot);
            if (deltaYawAbs > 0.0f && deltaYawAbs < MAX_DELTA_FOR_GCD && divisorX > GcdMath.MINIMUM_DIVISOR) {
                this.xRotMode.add(divisorX);
                this.lastXRot = deltaYawAbs;
            }
            double divisorY = GcdMath.gcd(deltaPitchAbs, this.lastYRot);
            if (deltaPitchAbs > 0.0f && deltaPitchAbs < MAX_DELTA_FOR_GCD && divisorY > GcdMath.MINIMUM_DIVISOR) {
                this.yRotMode.add(divisorY);
                this.lastYRot = deltaPitchAbs;
            }
            updateModes();
        }
        float gcdErrorYaw = calculateGcdError(deltaYaw, this.modeX);
        float gcdErrorPitch = calculateGcdError(deltaPitch, this.modeY);
        this.lastYaw = yaw;
        this.lastPitch = pitch;
        this.lastDeltaYaw = deltaYaw;
        this.lastDeltaPitch = deltaPitch;
        this.hasLastRotation = true;
        return new TickData(deltaYaw, deltaPitch, this.currentYawAccel, this.currentPitchAccel, jerkYaw, jerkPitch, gcdErrorYaw, gcdErrorPitch);
    }

    private float normalizeAngle(float angle) {
        while (angle > 180.0f) {
            angle -= 360.0f;
        }
        while (angle < -180.0f) {
            angle += 360.0f;
        }
        return angle;
    }

    private void updateModes() {
        if (this.xRotMode.size() > 15) {
            Pair<Double, Integer> modeResult = this.xRotMode.getMode();
            if (modeResult.first() != null && modeResult.second().intValue() > 15) {
                this.modeX = modeResult.first().doubleValue();
            }
        }
        if (this.yRotMode.size() > 15) {
            Pair<Double, Integer> modeResult2 = this.yRotMode.getMode();
            if (modeResult2.first() != null && modeResult2.second().intValue() > 15) {
                this.modeY = modeResult2.first().doubleValue();
            }
        }
    }

    private float calculateGcdError(float delta, double mode) {
        if (mode == 0.0d) {
            return 0.0f;
        }
        double absDelta = Math.abs(delta);
        double remainder = absDelta % mode;
        double error = Math.min(remainder, mode - remainder);
        return (float) error;
    }

    public double getModeX() {
        return this.modeX;
    }

    public double getModeY() {
        return this.modeY;
    }

    public RunningMode getXRotMode() {
        return this.xRotMode;
    }

    public RunningMode getYRotMode() {
        return this.yRotMode;
    }

    public float getCurrentYawAccel() {
        return this.currentYawAccel;
    }

    public float getCurrentPitchAccel() {
        return this.currentPitchAccel;
    }

    public float getLastYawAccel() {
        return this.lastYawAccel;
    }

    public float getLastPitchAccel() {
        return this.lastPitchAccel;
    }
}
