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
        this.title = title;
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
        return new int[]{ 80, 120, 500, 120 }; // Shift 1: Left-to-Right
    }

    // [BioX, BioY,  RecX, RecY,  ResX, ResY,  HazX, HazY]
    public int[] getBayLayout() {
        return new int[]{ 765, 72,  765, 164,  765, 256,  765, 348 };
    }

    public boolean isCleared(int currentScore) {
        return currentScore >= targetScore;
    }

    // Number of trash items that must be cleared from FULL bins via the Forklift to finish the shift!
    public int getTrashQuota() {
        return 4 + (levelNumber * 4);
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