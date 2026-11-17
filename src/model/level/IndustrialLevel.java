package model.level;

import model.item.WasteFactory;
import model.item.WasteItem;

public class IndustrialLevel extends FacilityLevel {
    public IndustrialLevel() {
        super(2, "Shift 2: Mandaue Hub", 250, 3.0);
    }

    @Override
    public int[] getStageLayout() {
        return new int[]{ 220, 225,  375, 225,  530, 225,  55, 225 };
    }

    @Override
    public int[] getBeltLayout() {
        return new int[]{ 620, 100, 220, 100 };
    }

    @Override
    public int[] getBayLayout() {
        return new int[]{ 185, 368,  325, 368,  465, 368,  605, 368 };
    }

    @Override
    public WasteItem spawnItem(double startX, double startY, int lane) {
        return WasteFactory.createItemForTier(2, startX, startY, lane);
    }
}