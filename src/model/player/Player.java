package model.player;

import model.meter.ComplianceMeter;
import model.meter.StreakMeter;

public class Player {
    private final String name;
    private int score;
    private final ComplianceMeter complianceMeter;
    private final StreakMeter streakMeter;

    public Player(String name) {
        this.name = name;
        this.score = 0;
        this.complianceMeter = new ComplianceMeter();
        this.streakMeter = new StreakMeter();
    }

    // Overloaded method 1: basic score addition
    public void addScore(int basePoints) {
        this.score += basePoints + streakMeter.getBonus();
        this.streakMeter.change(1);
    }

    // Overloaded method 2: score addition with preprocessing bonus
    public void addScore(int basePoints, int preprocessBonus) {
        this.score += basePoints + preprocessBonus + streakMeter.getBonus();
        this.streakMeter.change(1);
    }

    public void applyPenalty(int penaltyAmount) {
        this.complianceMeter.change(-penaltyAmount);
        this.streakMeter.reset(0);
    }

    public void resetScore() {
        this.score = 0;
    }

    public String getName() { return name; }
    public int getScore() { return score; }
    public ComplianceMeter getComplianceMeter() { return complianceMeter; }
    public StreakMeter getStreakMeter() { return streakMeter; }
}