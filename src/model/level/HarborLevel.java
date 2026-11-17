package model.level;

import model.item.WasteFactory;
import model.item.WasteItem;

public class HarborLevel extends FacilityLevel {
    public HarborLevel() {
        super(4, "Harbor Pier Terminal", 700, 4.0);
    }

    @Override
    public int[] getStageLayout() {
        return new int[]{ 65, 90,  65, 210,  65, 330,  235, 330 };
    }

    @Override
    public int[] getBeltLayout() {
        return new int[]{ 505, 360, 505, 140 };
    }

    @Override
    public int[] getBayLayout() {
        return new int[]{ 765, 72,  765, 164,  765, 256,  765, 348 };
    }

    @Override
    public WasteItem spawnItem(double startX, double startY, int lane) {
        return WasteFactory.createItemForTier(3, startX, startY, lane);
    }
}