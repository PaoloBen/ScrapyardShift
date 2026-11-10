package model.item;

import model.station.StationType;

import java.awt.Color;

public class LeakingChemicalJug extends WasteItem {
    private boolean sealed;

    public LeakingChemicalJug(double startX, double startY, int lane, boolean sealed) {
        super("Chemical Jug", startX, startY, lane);
        this.sealed = sealed;
    }

    @Override
    public void applyStation(StationType station) {
        if (station == StationType.NEUTRALIZER && !sealed) {
            this.sealed = true;
            setPreprocessed(true);
        }
    }

    @Override
    public BinType getTargetBin() {
        return BinType.HAZARDOUS;
    }

    @Override
    public int getPenalty() {
        return sealed ? 15 : 25;
    }

    @Override
    public String getExplanation() {
        return "Corrosive industrial chemicals must be sealed [E] and routed to HAZARDOUS.";
    }

    @Override
    public String getDisplayTag() {
        return sealed ? "Chem (Sealed)" : "Chem (Leaking!)";
    }

    @Override
    public Color getRenderColor() {
        return sealed ? new Color(39, 174, 96) : new Color(231, 76, 60);
    }
}