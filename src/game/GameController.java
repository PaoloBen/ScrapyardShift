package game;

import model.*;
import java.util.*;

public class GameController {
    private final Player player;
    private final List<FacilityLevel> levels;
    private int currentLevelIndex;
    private final Map<BinType, StorageBay> bays;
    private final List<WorkStation> stations;
    private final List<WasteItem> beltItems;
    private final List<String> mistakeLog;

    private BinType activeDiverter;
    private String lastFeedback;
    private int spawnTimer;
    private boolean gameOver;
    private boolean gameWon;

    public static final int BELT_START_X = 60;
    public static final int BELT_END_X = 680;
    public static final int LANE_Y = 230;

    public GameController(String playerName) {
        this.player = new Player(playerName);
        this.levels = List.of(new BarangayLevel(), new IndustrialLevel(), new TyphoonLevel());
        this.currentLevelIndex = 0;
        this.bays = new EnumMap<>(BinType.class);
        this.stations = new ArrayList<>();
        this.beltItems = new ArrayList<>();
        this.mistakeLog = new ArrayList<>();

        this.activeDiverter = BinType.BIODEGRADABLE;
        this.lastFeedback = "Use [Q] Wash, [W] Dismantle, [E] Seal | [1-4] Switch Track | [SPACE] Forklift";
        this.spawnTimer = 0;
        this.gameOver = false;
        this.gameWon = false;

        initFacility();
    }

    private void initFacility() {
        int bayY = 80;
        for (BinType type : BinType.values()) {
            bays.put(type, new StorageBay(type, 4, 720, bayY));
            bayY += 95;
        }
        // Place three inline stations along the conveyor belt
        stations.add(new WorkStation(StationType.WASHER, 220, 1));
        stations.add(new WorkStation(StationType.DISMANTLER, 370, 1));
        stations.add(new WorkStation(StationType.NEUTRALIZER, 520, 1));
    }

    public void updateTick() {
        if (gameOver || gameWon) return;

        FacilityLevel level = getCurrentLevel();

        // Spawn new waste item periodically
        spawnTimer++;
        int spawnInterval = Math.max(55, 130 - (level.getLevelNumber() * 25));
        if (spawnTimer >= spawnInterval) {
            beltItems.add(level.spawnItem(BELT_START_X, LANE_Y, 1));
            spawnTimer = 0;
        }

        // Move items and check if they reach the diverter gate at BELT_END_X
        Iterator<WasteItem> it = beltItems.iterator();
        while (it.hasNext()) {
            WasteItem item = it.next();
            item.move(level.getBeltSpeed());

            if (item.getX() >= BELT_END_X) {
                handleRoute(item, bays.get(activeDiverter));
                it.remove();
            }
        }

        // Check level progression or defeat
        if (player.getComplianceMeter().isDepleted()) {
            gameOver = true;
            lastFeedback = "SHIFT TERMINATED! Compliance reached 0%. Press [R] to Retry.";
        } else if (level.isCleared(player.getScore())) {
            advanceLevel();
        }
    }

    public void triggerStation(StationType type) {
        if (gameOver || gameWon) return;
        for (WorkStation station : stations) {
            if (station.getType() == type) {
                for (WasteItem item : beltItems) {
                    station.process(item);
                }
                lastFeedback = "Activated " + type.getLabel() + "!";
            }
        }
    }

    public void setActiveDiverter(BinType type) {
        this.activeDiverter = type;
    }

    public void dispatchForklift() {
        bays.values().forEach(StorageBay::empty);
        lastFeedback = "Forklift dispatched! All storage bays emptied.";
    }

    private void handleRoute(WasteItem item, StorageBay bay) {
        try {
            bay.accept(item);
            int levelMultiplier = getCurrentLevel().getLevelNumber();

            if (item.getTargetBin() == bay.getType()) {
                if (item.isPreprocessed()) {
                    player.addScore(10 * levelMultiplier, 15);
                    lastFeedback = "PERFECT PREPROCESS + SORT! +" + (10 * levelMultiplier + 15) + " pts";
                } else {
                    player.addScore(10 * levelMultiplier);
                    lastFeedback = "Correct! Routed " + item.getDisplayTag() + " to " + bay.getType().name();
                }
            } else {
                player.applyPenalty(item.getPenalty());
                String err = item.getDisplayTag() + " -> " + bay.getType().name() + ": " + item.getExplanation();
                lastFeedback = "VIOLATION (-" + item.getPenalty() + "%): " + item.getExplanation();
                mistakeLog.add(err);
            }
        } catch (BinFullException e) {
            player.applyPenalty(5);
            lastFeedback = "OVERFLOW (-5%): " + e.getMessage();
            mistakeLog.add(e.getMessage());
        }
    }

    private void advanceLevel() {
        if (currentLevelIndex < levels.size() - 1) {
            currentLevelIndex++;
            beltItems.clear();
            bays.values().forEach(StorageBay::empty);
            if (getCurrentLevel() instanceof TyphoonLevel typhoon) {
                typhoon.triggerSurgeEvent();
            }
            lastFeedback = "PROMOTED TO " + getCurrentLevel().getTitle().toUpperCase() + "!";
        } else {
            gameWon = true;
            lastFeedback = "ALL SHIFTS CLEARED! Final Score: " + player.getScore() + " | Press [R] to Play Again.";
        }
    }

    public void resetGame() {
        this.currentLevelIndex = 0;
        this.spawnTimer = 0;
        this.beltItems.clear();
        this.mistakeLog.clear();
        this.bays.values().forEach(StorageBay::empty);
        this.player.resetScore();
        this.player.getComplianceMeter().reset(100);
        this.player.getStreakMeter().reset(0);
        this.gameOver = false;
        this.gameWon = false;
        this.lastFeedback = "Shift restarted! Watch the belt.";
    }

    public Player getPlayer() { return player; }
    public FacilityLevel getCurrentLevel() { return levels.get(currentLevelIndex); }
    public Collection<StorageBay> getBays() { return bays.values(); }
    public List<WorkStation> getStations() { return stations; }
    public List<WasteItem> getBeltItems() { return beltItems; }
    public List<String> getMistakeLog() { return mistakeLog; }
    public BinType getActiveDiverter() { return activeDiverter; }
    public String getLastFeedback() { return lastFeedback; }
    public boolean isGameOver() { return gameOver; }
    public boolean isGameWon() { return gameWon; }
}