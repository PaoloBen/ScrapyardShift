package model;

import java.awt.Color;

public class BrokenPhone extends WasteItem {
    private boolean dismantled;

    public BrokenPhone(double startX, double startY, int lane, boolean dismantled) {
        super("Broken Phone", startX, startY, lane);
        this.dismantled = dismantled;
    }

    @Override
    public void applyStation(StationType station) {
        if (station == StationType.DISMANTLER && !dismantled) {
            this.dismantled = true;
            setPreprocessed(true);
        }
    }

    @Override
    public BinType getTargetBin() {
        // Intact phone has a dangerous battery inside -> HAZARDOUS
        // Once dismantled, the metal/plastic frame goes to -> RECYCLABLE
        return dismantled ? BinType.RECYCLABLE : BinType.HAZARDOUS;
    }

    @Override
    public int getPenalty() {
        return dismantled ? 10 : 20;
    }

    @Override
    public String getExplanation() {
        return dismantled
                ? "Dismantled phone casings are safe metal/plastic: send to RECYCLABLE."
                : "Intact e-waste contains explosive lithium batteries! Dismantle first or send to HAZARDOUS.";
    }

    @Override
    public String getDisplayTag() {
        return dismantled ? "Phone (Frame)" : "Phone (Intact)";
    }

    @Override
    public Color getRenderColor() {
        return dismantled ? new Color(46, 204, 113) : new Color(230, 126, 34);
    }
}