package model.item;

import java.util.Random;

public class WasteFactory {
    private static final Random RANDOM = new Random();

    private WasteFactory() {
        // Utility factory class
    }

    public static WasteItem createItemForTier(int tier, double startX, double startY, int lane) {
        int poolSize = switch (tier) {
            case 1 -> 5;  // Shift 1: Basic items + Washable plastics/boxes
            case 2 -> 8;  // Shift 2: Adds E-Waste & Batteries
            default -> 10; // Shift 3: Full 10-item pool including Syringes & Leaking Chemicals
        };

        int roll = RANDOM.nextInt(poolSize);
        return switch (roll) {
            case 0 -> new BananaPeel(startX, startY, lane);
            case 1 -> new SpoiledMango(startX, startY, lane);
            case 2 -> new AluminumCan(startX, startY, lane);
            case 3 -> new SachetWrapper(startX, startY, lane);
            case 4 -> new PlasticBottle(startX, startY, lane, RANDOM.nextBoolean());
            case 5 -> new GreasyPizzaBox(startX, startY, lane, false);
            case 6 -> new BrokenPhone(startX, startY, lane, false);
            case 7 -> new LithiumBattery(startX, startY, lane);
            case 8 -> new MedicalSyringe(startX, startY, lane);
            default -> new LeakingChemicalJug(startX, startY, lane, false);
        };
    }
}