package model;

import java.awt.Color;

public class SpoiledMango extends WasteItem {
    public SpoiledMango(double startX, double startY, int lane) {
        super("Spoiled Mango", startX, startY, lane);
    }

    @Override
    public BinType getTargetBin() {
        return BinType.BIODEGRADABLE;
    }

    @Override
    public String getExplanation() {
        return "Rotten fruit turns into nutrient-rich compost: send to BIODEGRADABLE.";
    }

    @Override
    public Color getRenderColor() {
        return new Color(243, 156, 18);
    }
}