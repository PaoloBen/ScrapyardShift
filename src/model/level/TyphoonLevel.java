package model.level;

import model.item.WasteFactory;
import model.item.WasteItem;

public class TyphoonLevel extends FacilityLevel {
    private boolean surgeActive = false;

    public TyphoonLevel() {
        super(3, "Shift 3: Typhoon Surge", 450, 3.6);
    }

    @Override
    public int[] getStageLayout() {
        return new int[]{ 340, 110,  510, 110,  340, 335,  510, 335 };
    }

    @Override
    public int[] getBeltLayout() {
        return new int[]{ 135, 90, 135, 320 };
    }

    @Override
    public int[] getBayLayout() {
        return new int[]{ 765, 72,  765, 164,  765, 256,  765, 348 };
    }

    public void triggerSurgeEvent() {
        if (!surgeActive) {
            setBeltSpeed(getBeltSpeed() + 1.2);
            surgeActive = true;
        }
    }

    @Override
    public WasteItem spawnItem(double startX, double startY, int lane) {
        return WasteFactory.createItemForTier(3, startX, startY, lane);
    }
}