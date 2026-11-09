package model;

import java.awt.Color;

public class BananaPeel extends WasteItem {
    public BananaPeel(double startX, double startY, int lane) {
        super("Banana Peel", startX, startY, lane);
    }

    @Override
    public BinType getTargetBin() {
        return BinType.BIODEGRADABLE;
    }

    @Override
    public String getExplanation() {
        return "Organic food scraps decompose naturally: send to BIODEGRADABLE.";
    }

    @Override
    public Color getRenderColor() {
        return new Color(241, 196, 15); // Yellow
    }
}