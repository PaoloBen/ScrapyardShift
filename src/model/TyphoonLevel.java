package model;

public class TyphoonLevel extends FacilityLevel {
    private boolean surgeActive = false;

    public TyphoonLevel() {
        super(3, "Shift 3: Typhoon Cleanup Surge", 450, 4.2);
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