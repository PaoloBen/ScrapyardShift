package model.level;

import model.item.WasteFactory;
import model.item.WasteItem;

public class BarangayLevel extends FacilityLevel {
    public BarangayLevel() {
        super(1, "Community MRF", 100, 2.2);
    }

    @Override
    public WasteItem spawnItem(double startX, double startY, int lane) {
        return WasteFactory.createItemForTier(1, startX, startY, lane);
    }
}