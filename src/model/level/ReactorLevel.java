package model.level;

import model.item.WasteFactory;
import model.item.WasteItem;

public class ReactorLevel extends FacilityLevel {
    public ReactorLevel() {
        super(5, "Shift 5: Mactan Hazmat Core", 1000, 4.5);
    }

    @Override
    public int[] getStageLayout() {
        return new int[]{ 240, 82,  430, 82,  240, 355,  430, 355 };
    }

    @Override
    public int[] getBeltLayout() {
        return new int[]{ 560, 225, 200, 225 };
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