package model;

import java.awt.Color;

public class GreasyPizzaBox extends WasteItem {
    private boolean degreased;

    public GreasyPizzaBox(double startX, double startY, int lane, boolean degreased) {
        super("Pizza Box", startX, startY, lane);
        this.degreased = degreased;
    }

    @Override
    public void applyStation(StationType station) {
        if (station == StationType.WASHER && !degreased) {
            this.degreased = true;
            setPreprocessed(true);
        }
    }

    @Override
    public BinType getTargetBin() {
        return degreased ? BinType.RECYCLABLE : BinType.RESIDUAL;
    }

    @Override
    public String getExplanation() {
        return degreased
                ? "Clean cardboard fibers can be pulped: send to RECYCLABLE."
                : "Grease ruins paper recycling batches! Scrape/wash first or send to RESIDUAL.";
    }

    @Override
    public String getDisplayTag() {
        return degreased ? "Box (Clean)" : "Box (Greasy)";
    }

    @Override
    public Color getRenderColor() {
        return degreased ? new Color(211, 84, 0) : new Color(127, 140, 141);
    }
}