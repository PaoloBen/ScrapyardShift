package model.item;

import java.awt.Color;

public class MedicalSyringe extends WasteItem {
    public MedicalSyringe(double startX, double startY, int lane) {
        super("Used Syringe", startX, startY, lane);
    }

    @Override
    public BinType getTargetBin() {
        return BinType.HAZARDOUS;
    }

    @Override
    public int getPenalty() {
        return 20;
    }

    @Override
    public String getExplanation() {
        return "Biohazard sharps risk needle-stick injuries! Must go to HAZARDOUS.";
    }

    @Override
    public Color getRenderColor() {
        return new Color(192, 57, 43);
    }
}