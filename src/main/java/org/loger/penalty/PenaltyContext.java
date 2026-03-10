package org.loger.penalty;

public class PenaltyContext {
    private final double buffer;
    private final String playerName;
    private final double probability;
    private final int violationLevel;

    public PenaltyContext(String playerName, int violationLevel, double probability, double buffer) {
        this.playerName = playerName != null ? playerName : "";
        this.violationLevel = violationLevel;
        this.probability = probability;
        this.buffer = buffer;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public int getViolationLevel() {
        return this.violationLevel;
    }

    public double getProbability() {
        return this.probability;
    }

    public double getBuffer() {
        return this.buffer;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String playerName = "";
        private int violationLevel = 0;
        private double probability = 0.0d;
        private double buffer = 0.0d;

        public Builder playerName(String name) {
            this.playerName = name;
            return this;
        }

        public Builder violationLevel(int vl) {
            this.violationLevel = vl;
            return this;
        }

        public Builder probability(double prob) {
            this.probability = prob;
            return this;
        }

        public Builder buffer(double buf) {
            this.buffer = buf;
            return this;
        }

        public PenaltyContext build() {
            return new PenaltyContext(this.playerName, this.violationLevel, this.probability, this.buffer);
        }
    }
}
