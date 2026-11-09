package model;

public class IndustrialLevel extends FacilityLevel {
    public IndustrialLevel() {
        super(2, "Shift 2: Mandaue Industrial Hub", 250, 3.2);
    }

    @Override
    public WasteItem spawnItem(double startX, double startY, int lane) {
        return WasteFactory.createItemForTier(2, startX, startY, lane);
    }
}