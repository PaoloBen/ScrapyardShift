package model;

import java.awt.Color;

public class SachetWrapper extends WasteItem {
    public SachetWrapper(double startX, double startY, int lane) {
        super("Chip Wrapper", startX, startY, lane);
    }

    @Override
    public BinType getTargetBin() {
        return BinType.RESIDUAL;
    }

    @Override
    public String getExplanation() {
        return "Multi-layered foil sachets cannot be recycled locally: send to RESIDUAL.";
    }

    @Override
    public Color getRenderColor() {
        return new Color(142, 68, 173);
    }
}