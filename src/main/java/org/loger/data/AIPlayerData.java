package org.loger.data;

import org.loger.util.AimProcessor;
import org.loger.util.BufferCalculator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AIPlayerData {
    private final AimProcessor aimProcessor;
    private volatile double buffer;
    private volatile double lastAimbot;
    private volatile double lastGcd;
    private volatile double lastProbability;
    private volatile double lastRotation;
    private volatile double lastSmooth;
    private volatile double lastSnap;
    private final ReentrantReadWriteLock lock;
    private volatile boolean pendingRequest;
    private final UUID playerId;
    private final int sequence;
    private final Deque<TickData> tickBuffer;
    private int ticksSinceAttack;
    private int ticksStep;

    public AIPlayerData(UUID playerId) {
        this(playerId, 40);
    }

    public AIPlayerData(UUID playerId, int sequence) {
        this.lock = new ReentrantReadWriteLock();
        this.playerId = playerId;
        this.sequence = sequence;
        this.aimProcessor = new AimProcessor();
        this.tickBuffer = new ArrayDeque(sequence);
        this.ticksSinceAttack = sequence + 1;
        this.ticksStep = 0;
        this.buffer = 0.0d;
        this.lastProbability = 0.0d;
        this.lastRotation = 0.0d;
        this.lastAimbot = 0.0d;
        this.lastGcd = 0.0d;
        this.lastSnap = 0.0d;
        this.lastSmooth = 0.0d;
        this.pendingRequest = false;
    }

    public TickData processTick(float yaw, float pitch) {
        TickData tickData = this.aimProcessor.process(yaw, pitch);
        this.lock.writeLock().lock();
        try {
            if (this.tickBuffer.size() >= this.sequence) {
                this.tickBuffer.pollFirst();
            }
            this.tickBuffer.addLast(tickData);
            return tickData;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public void onAttack() {
        this.lock.writeLock().lock();
        try {
            this.ticksSinceAttack = 0;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public void onTeleport() {
        this.lock.writeLock().lock();
        try {
            this.aimProcessor.reset();
            clearBuffer();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public void incrementTicksSinceAttack() {
        this.lock.writeLock().lock();
        try {
            if (this.ticksSinceAttack <= this.sequence + 1) {
                this.ticksSinceAttack++;
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public void incrementStepCounter() {
        this.lock.writeLock().lock();
        try {
            this.ticksStep++;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Deprecated
    public void onTick() {
        this.lock.writeLock().lock();
        try {
            this.ticksSinceAttack++;
            this.ticksStep++;
            if (this.ticksSinceAttack > this.sequence) {
                clearBuffer();
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public void setPendingRequest(boolean pending) {
        this.lock.writeLock().lock();
        try {
            this.pendingRequest = pending;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public boolean isPendingRequest() {
        this.lock.readLock().lock();
        try {
            return this.pendingRequest;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public void resetStepCounter() {
        this.lock.writeLock().lock();
        try {
            this.ticksStep = 0;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public List<TickData> getTickBuffer() {
        this.lock.readLock().lock();
        try {
            return new ArrayList(this.tickBuffer);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public boolean shouldSendData(int step, int sequence) {
        this.lock.readLock().lock();
        try {
            return !pendingRequest && ticksStep >= step && tickBuffer.size() >= sequence && ticksSinceAttack <= sequence;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public void clearBuffer() {
        this.lock.writeLock().lock();
        try {
            this.tickBuffer.clear();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public void fullReset() {
        this.lock.writeLock().lock();
        try {
            this.tickBuffer.clear();
            this.aimProcessor.reset();
            this.pendingRequest = false;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public boolean isInCombat() {
        this.lock.readLock().lock();
        try {
            return this.ticksSinceAttack <= this.sequence;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public int getBufferSize() {
        this.lock.readLock().lock();
        try {
            return this.tickBuffer.size();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public int getSequence() {
        return this.sequence;
    }

    public int getTicksSinceAttack() {
        this.lock.readLock().lock();
        try {
            return this.ticksSinceAttack;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public void updateBuffer(double probability, double multiplier, double decreaseAmount, double threshold) {
        this.lock.writeLock().lock();
        try {
            this.lastProbability = probability;
            this.buffer = BufferCalculator.updateBuffer(this.buffer, probability, multiplier, decreaseAmount, threshold);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public void updateDetectionScores(double rotation, double aimbot, double gcd, double snap, double smooth) {
        this.lock.writeLock().lock();
        try {
            this.lastRotation = rotation;
            this.lastAimbot = aimbot;
            this.lastGcd = gcd;
            this.lastSnap = snap;
            this.lastSmooth = smooth;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public boolean shouldFlag(double flagThreshold) {
        this.lock.readLock().lock();
        try {
            return BufferCalculator.shouldFlag(this.buffer, flagThreshold);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public void resetBuffer(double resetValue) {
        this.lock.writeLock().lock();
        try {
            this.buffer = BufferCalculator.resetBuffer(resetValue);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public double getBuffer() {
        this.lock.readLock().lock();
        try {
            return this.buffer;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public double getLastProbability() {
        return this.lastProbability;
    }

    public double getLastRotation() {
        return this.lastRotation;
    }

    public double getLastAimbot() {
        return this.lastAimbot;
    }

    public double getLastGcd() {
        return this.lastGcd;
    }

    public double getLastSnap() {
        return this.lastSnap;
    }

    public double getLastSmooth() {
        return this.lastSmooth;
    }

    public AimProcessor getAimProcessor() {
        return this.aimProcessor;
    }
}
