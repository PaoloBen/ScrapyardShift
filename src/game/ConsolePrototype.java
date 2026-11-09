package game;

import model.*;
import java.util.*;

public class ConsolePrototype {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Player player = new Player("Operator Paul");

        Map<BinType, StorageBay> bays = new EnumMap<>(BinType.class);
        for (BinType type : BinType.values()) {
            bays.put(type, new StorageBay(type, 2, 0, 0)); // Small capacity of 2 to test BinFullException!
        }

        List<WasteItem> testBelt = new ArrayList<>();
        testBelt.add(new BananaPeel(0, 0, 0));
        testBelt.add(new PlasticBottle(0, 0, 0, false)); // Dirty bottle!
        testBelt.add(new BrokenPhone(0, 0, 0, false));   // Intact phone!
        testBelt.add(new LithiumBattery(0, 0, 0));

        System.out.println("=== SCRAPYARD SHIFT: CONSOLE PROTOTYPE ===");

        for (WasteItem item : testBelt) {
            if (player.getComplianceMeter().isDepleted()) break;

            System.out.println("\n-------------------------------------------");
            System.out.println("Score: " + player.getScore() + " | Streak: " + player.getStreakMeter().getValue()
                    + " | Compliance: " + player.getComplianceMeter().getValue() + "%");
            System.out.println("INCOMING ITEM: " + item.getDisplayTag());
            System.out.println("Actions: [W]ash | [D]ismantle | [F]orklift Empty | [S]kip to Routing");
            System.out.print("Preprocess action: ");
            String action = scanner.nextLine().trim().toUpperCase();

            if (action.equals("W")) {
                item.applyStation(StationType.WASHER);
                System.out.println("-> Washed! Item is now: " + item.getDisplayTag());
            } else if (action.equals("D")) {
                item.applyStation(StationType.DISMANTLER);
                System.out.println("-> Dismantled! Item is now: " + item.getDisplayTag());
            } else if (action.equals("F")) {
                bays.values().forEach(StorageBay::empty);
                System.out.println("-> Forklift emptied all bays!");
            }

            System.out.println("Route to Bay: [1] BIODEGRADABLE  [2] RECYCLABLE  [3] RESIDUAL  [4] HAZARDOUS");
            System.out.print("Select Bay (1-4): ");
            int choice = Integer.parseInt(scanner.nextLine().trim());
            BinType chosenType = BinType.values()[Math.max(0, Math.min(3, choice - 1))];
            StorageBay targetBay = bays.get(chosenType);

            try {
                targetBay.accept(item);
                if (item.getTargetBin() == chosenType) {
                    if (item.isPreprocessed()) {
                        player.addScore(10, 15);
                        System.out.println("CORRECT + PREPROCESSED BONUS! (+25 + streak)");
                    } else {
                        player.addScore(10);
                        System.out.println("CORRECT SORT! (+10 + streak)");
                    }
                } else {
                    player.applyPenalty(item.getPenalty());
                    System.out.println("WRONG BAY! -" + item.getPenalty() + "% Compliance.");
                    System.out.println("Reason: " + item.getExplanation());
                }
            } catch (BinFullException e) {
                player.applyPenalty(5);
                System.out.println("BAY OVERFLOW! -5% Compliance. " + e.getMessage());
            }
        }

        System.out.println("\n=== SHIFT COMPLETE | Final Score: " + player.getScore() + " ===");
    }
}