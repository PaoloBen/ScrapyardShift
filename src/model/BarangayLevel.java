package model;

public class BarangayLevel extends FacilityLevel {
    public BarangayLevel() {
        super(1, "Shift 1: Barangay MRF", 100, 2.2);
    }

    @Override
    public WasteItem spawnItem(double startX, double startY, int lane) {
        return WasteFactory.createItemForTier(1, startX, startY, lane);
    }
}