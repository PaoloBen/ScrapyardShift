package model;

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

    public boolean isCleared(int currentScore) {
        return currentScore >= targetScore;
    }

    public int getLevelNumber() { return levelNumber; }
    public String getTitle() { return title; }
    public int getTargetScore() { return targetScore; }
    public double getBeltSpeed() { return beltSpeed; }
    protected void setBeltSpeed(double beltSpeed) { this.beltSpeed = beltSpeed; }
}