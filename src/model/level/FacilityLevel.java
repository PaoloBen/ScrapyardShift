package model.level;

import model.item.WasteItem;

import java.util.Random;

public abstract class FacilityLevel {
    private final int levelNumber;
    private final String title;
    private final int targetScore;
    private double beltSpeed;
    protected final Random random = new Random();

    protected FacilityLevel(int levelNumber, String title, int targetScore, double beltSpeed) {
        this.levelNumber = levelNumber;
        // Strip any "Shift X: " prefix so levels use pure location names!
        this.title = title != null ? title.replaceFirst("^Shift\\s+\\d+:\\s*", "") : "MRF Facility";
        this.targetScore = targetScore;
        this.beltSpeed = beltSpeed;
    }

    public abstract WasteItem spawnItem(double startX, double startY, int lane);

    // [WasherX, WasherY, DismantlerX, DismantlerY, NeutralizerX, NeutralizerY, ForkliftX, ForkliftY]
    public int[] getStageLayout() {
        return new int[]{ 80, 350,  230, 350,  380, 350,  535, 350 };
    }

    // [StartX, StartY, EndX, EndY]
    public int[] getBeltLayout() {
        return new int[]{ 80, 120, 500, 120 };
    }

    // [BioX, BioY,  RecX, RecY,  ResX, ResY,  HazX, HazY]
    public int[] getBayLayout() {
        return new int[]{ 765, 72,  765, 164,  765, 256,  765, 348 };
    }

    public boolean isCleared(int currentScore) {
        return currentScore >= targetScore;
    }

    // Base quota starts at 16 on Level 1 and increases exponentially across all 20 levels!
    public int getTrashQuota() {
        int raw = (int) Math.round(16.0 * Math.pow(1.16, levelNumber - 1));
        return (raw % 2 == 0) ? raw : raw + 1;
    }

    public boolean isQuotaMet(int transportedTrashCount) {
        return transportedTrashCount >= getTrashQuota();
    }

    public int getLevelNumber() { return levelNumber; }
    public String getTitle() { return title; }
    public int getTargetScore() { return targetScore; }
    public double getBeltSpeed() { return beltSpeed; }
    protected void setBeltSpeed(double beltSpeed) { this.beltSpeed = beltSpeed; }
}