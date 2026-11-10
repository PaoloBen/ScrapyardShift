package model.item;

import java.awt.Color;

public class AluminumCan extends WasteItem {
    public AluminumCan(double startX, double startY, int lane) {
        super("Soda Can", startX, startY, lane);
    }

    @Override
    public BinType getTargetBin() {
        return BinType.RECYCLABLE;
    }

    @Override
    public String getExplanation() {
        return "Aluminum cans are 100% infinitely recyclable: send to RECYCLABLE.";
    }

    @Override
    public Color getRenderColor() {
        return new Color(41, 128, 185);
    }
}