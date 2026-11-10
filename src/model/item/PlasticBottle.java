package model.item;

import model.station.StationType;

import java.awt.Color;

public class PlasticBottle extends WasteItem {
    private boolean clean;

    public PlasticBottle(double startX, double startY, int lane, boolean clean) {
        super("Plastic Bottle", startX, startY, lane);
        this.clean = clean;
    }

    @Override
    public void applyStation(StationType station) {
        if (station == StationType.WASHER && !clean) {
            this.clean = true;
            setPreprocessed(true); // Awards the +15 preprocessing bonus!
        }
    }

    @Override
    public BinType getTargetBin() {
        return clean ? BinType.RECYCLABLE : BinType.RESIDUAL;
    }

    @Override
    public String getExplanation() {
        return clean
                ? "Clean PET bottles can be baled: send to RECYCLABLE."
                : "Dirty plastic contaminates clean batches! Wash it first, or send to RESIDUAL.";
    }

    @Override
    public String getDisplayTag() {
        return clean ? "Bottle (Clean)" : "Bottle (Dirty)";
    }

    @Override
    public Color getRenderColor() {
        return clean ? new Color(52, 152, 219) : new Color(149, 165, 166);
    }
}