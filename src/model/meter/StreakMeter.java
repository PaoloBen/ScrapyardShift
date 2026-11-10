package model.meter;

public class StreakMeter extends Meter {
    public StreakMeter() {
        super("Efficiency Streak", 0, 0, 50);
    }

    @Override
    public boolean isCritical() {
        return getValue() >= 5; // High streak active!
    }

    public int getBonus() {
        return Math.min(10, getValue() * 2); // +2 per streak, up to +10
    }
}