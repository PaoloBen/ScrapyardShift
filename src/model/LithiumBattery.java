package model;

import java.awt.Color;

public class LithiumBattery extends WasteItem {
    private boolean neutralized;

    public LithiumBattery(double startX, double startY, int lane) {
        super("Lithium Battery", startX, startY, lane);
        this.neutralized = false;
    }

    @Override
    public void applyStation(StationType station) {
        if (station == StationType.NEUTRALIZER && !neutralized) {
            this.neutralized = true;
            setPreprocessed(true);
        }
    }

    @Override
    public BinType getTargetBin() {
        return BinType.HAZARDOUS;
    }

    @Override
    public int getPenalty() {
        return 20; // Heavy penalty for misrouting hazardous waste!
    }

    @Override
    public String getExplanation() {
        return "Lithium batteries cause toxic fires in standard bins! Always send to HAZARDOUS.";
    }

    @Override
    public String getDisplayTag() {
        return neutralized ? "Battery (Sealed)" : "Battery (Live!)";
    }

    @Override
    public Color getRenderColor() {
        return new Color(231, 76, 60); // Red
    }
}