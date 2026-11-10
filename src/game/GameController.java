package game;

import model.exception.BinFullException;
import model.item.BinType;
import model.item.WasteItem;
import model.level.*;
import model.player.Player;
import model.station.*;
import ui.fx.FloatingText;

import java.awt.Color;
import java.util.*;

public class GameController {
    private final Player player;
    private final List<FacilityLevel> levels;
    private int currentLevelIndex;
    private final Map<BinType, StorageBay> bays;
    private final List<WorkStation> stations;
    private final List<WasteItem> beltItems;
    private final List<WasteItem> thrownItems = new ArrayList<>();
    private final List<FloatingText> popups;
    private final List<String> mistakeLog;

    private String lastFeedback;
    private int spawnTimer;
    private int screenShake;
    private boolean inLockerRoom;
    private boolean gameOver;
    private boolean gameWon;

    public static final int BELT_START_X = 50;
    public static final int BELT_END_X = 560;
    public static final int LANE_Y = 125;

    // Forklift Dispatch Terminal Coordinates
    public static final int FORKLIFT_X = 530;
    public static final int FORKLIFT_Y = 380;

    public GameController(String playerName) {
        this.player = new Player(playerName);
        this.levels = List.of(new BarangayLevel(), new IndustrialLevel(), new TyphoonLevel());
        this.currentLevelIndex = 0;
        this.bays = new EnumMap<>(BinType.class);
        this.stations = new ArrayList<>();
        this.beltItems = new ArrayList<>();
        this.popups = new ArrayList<>();
        this.mistakeLog = new ArrayList<>();

        this.lastFeedback = "Grab trash from the top belt [SPACE], process at bottom stations, & drop in right bays!";
        this.spawnTimer = 0;
        this.screenShake = 0;
        this.inLockerRoom = true; // Starts in Avatar Customization Locker Room!
        this.gameOver = false;
        this.gameWon = false;

        initFacility();
    }

    private void initFacility() {
        int bayY = 80;
        for (BinType type : BinType.values()) {
            bays.put(type, new StorageBay(type, 4, 730, bayY));
            bayY += 95;
        }
        // Bottom row of interactive Overcooked workstations
        stations.add(new WorkStation(StationType.WASHER, 80, 380));
        stations.add(new WorkStation(StationType.DISMANTLER, 230, 380));
        stations.add(new WorkStation(StationType.NEUTRALIZER, 380, 380));
    }

    public void updateTick(double moveDx, double moveDy) {
        popups.removeIf(FloatingText::update);
        if (screenShake > 0) screenShake--;

        if (inLockerRoom || gameOver || gameWon) return;

        // 1. Move Player Avatar (minY is 95 so player can reach the top Biodegradable Bay!)
        player.move(moveDx, moveDy, 40, 700, 95, 450);

        // 1b. Update Thrown / Floor Items
        Iterator<WasteItem> thrownIt = thrownItems.iterator();
        while (thrownIt.hasNext()) {
            WasteItem item = thrownIt.next();
            if (item.isAirborne()) {
                item.updateProjectile();
                double centerX = item.getX() + 48;
                double centerY = item.getY() + 20;

                // Hit a right-side Storage Bay?
                if (centerX >= 715) {
                    BinType targetType = getClosestBayType(centerY);
                    handleRoute(item, bays.get(targetType));
                    thrownIt.remove();
                    continue;
                }

                // Landed inside an empty WorkStation?
                boolean enteredStation = false;
                for (WorkStation station : stations) {
                    if (station.isEmpty() && Math.hypot((station.getX() + 55) - centerX, (station.getY() + 35) - centerY) <= 48) {
                        station.placeItem(item);
                        popups.add(new FloatingText("KOBE!", station.getX() + 25, station.getY() - 10, Color.CYAN));
                        lastFeedback = "Thrown directly into " + station.getType().getLabel() + "!";
                        thrownIt.remove();
                        enteredStation = true;
                        break;
                    }
                }
                if (enteredStation) continue;

                // Keep inside floor bounds if it hits a wall
                item.setPosition(
                        Math.max(30, Math.min(640, item.getX())),
                        Math.max(195, Math.min(420, item.getY()))
                );
            }
        }

        // 2. Tick WorkStations
        for (WorkStation station : stations) {
            if (station.updateTick()) {
                popups.add(new FloatingText("READY!", station.getX() + 25, station.getY() - 15, Color.GREEN));
                lastFeedback = station.getType().getLabel() + " finished processing! Grab item with [SPACE].";
            }
        }

        // 3. Spawn & Move Items on Intake Belt
        FacilityLevel level = getCurrentLevel();
        spawnTimer++;
        int spawnInterval = Math.max(95, 180 - (level.getLevelNumber() * 25));
        if (spawnTimer >= spawnInterval && beltItems.size() < 5) {
            beltItems.add(level.spawnItem(BELT_START_X, LANE_Y, 1));
            spawnTimer = 0;
        }

        Iterator<WasteItem> it = beltItems.iterator();
        while (it.hasNext()) {
            WasteItem item = it.next();
            item.move(level.getBeltSpeed() * 0.45);

            // Item falls into the Incinerator at the end of the belt if not picked up!
            if (item.getX() >= BELT_END_X) {
                player.applyPenalty(5);
                screenShake = 8;
                popups.add(new FloatingText("-5% BURNED!", BELT_END_X - 20, LANE_Y, Color.RED));
                lastFeedback = "INCINERATOR (-5%): You missed " + item.getDisplayTag() + " on the belt!";
                mistakeLog.add("Missed on belt: " + item.getDisplayTag());
                it.remove();
            }
        }

        // 4. Check Win / Loss conditions
        if (player.getComplianceMeter().isDepleted()) {
            gameOver = true;
            lastFeedback = "SHIFT TERMINATED! Compliance reached 0%. Press [R] to Retry.";
        } else if (level.isCleared(player.getScore())) {
            advanceLevel();
        }
    }

    // [SPACE] or [E]: Context-sensitive Grab / Place / Drop / Lever Pull
    public void interact() {
        if (inLockerRoom || gameOver || gameWon) return;

        double px = player.getX();
        double py = player.getY();
        WasteItem held = player.getHeldItem();

        // CASE A: Player is holding an item
        if (held != null) {
            // 1. Standing near the right-side Storage Bays?
            if (px >= 635) {
                BinType targetType = getClosestBayType(py);
                handleRoute(held, bays.get(targetType));
                player.setHeldItem(null);
                return;
            }

            // 2. Standing near a bottom WorkStation?
            for (WorkStation station : stations) {
                if (station.isNear(px, py) && station.isEmpty()) {
                    station.placeItem(held);
                    player.setHeldItem(null);
                    lastFeedback = "Placed " + held.getDisplayTag() + " in " + station.getType().getLabel() + "!";
                    return;
                }
            }

            // 3. Standing near the top Intake Belt? Put item back on belt
            if (py <= 215 && px <= BELT_END_X) {
                held.setPosition(px - 30, LANE_Y);
                beltItems.add(held);
                player.setHeldItem(null);
                lastFeedback = "Placed item back onto conveyor belt.";
                return;
            }

            lastFeedback = "Walk closer to a Machine (bottom) or Sorting Bay (right) to place item!";
            return;
        }

        // CASE B: Player's hands are empty
        // 1. Standing near the Forklift Dispatch Terminal?
        if (Math.hypot((FORKLIFT_X + 45) - px, (FORKLIFT_Y + 30) - py) <= 75) {
            bays.values().forEach(StorageBay::empty);
            popups.add(new FloatingText("BAYS CLEARED!", FORKLIFT_X, FORKLIFT_Y - 10, Color.CYAN));
            lastFeedback = "Forklift dispatched! All 4 sorting bays emptied.";
            return;
        }

        // 2. Standing near a WorkStation with an item inside?
        for (WorkStation station : stations) {
            if (station.isNear(px, py) && !station.isEmpty()) {
                WasteItem item = station.removeItem();
                player.setHeldItem(item);
                lastFeedback = "Picked up " + item.getDisplayTag() + " from " + station.getType().name();
                return;
            }
        }

        // 3. Standing near the Intake Belt? Grab closest item
        if (py <= 220) {
            WasteItem closest = null;
            double bestDist = 85;
            for (WasteItem item : beltItems) {
                double dist = Math.hypot((item.getX() + 45) - px, (item.getY() + 20) - py);
                if (dist < bestDist) {
                    bestDist = dist;
                    closest = item;
                }
            }
            if (closest != null) {
                beltItems.remove(closest);
                player.setHeldItem(closest);
                lastFeedback = "Grabbed " + closest.getDisplayTag() + "! Carry to a station or right-side bay.";
            }
        }

        // 4. Pick up any thrown item lying on the factory floor
        Iterator<WasteItem> floorIt = thrownItems.iterator();
        while (floorIt.hasNext()) {
            WasteItem item = floorIt.next();
            if (!item.isAirborne() && Math.hypot((item.getX() + 48) - px, (item.getY() + 20) - py) <= 75) {
                floorIt.remove();
                player.setHeldItem(item);
                lastFeedback = "Picked up " + item.getDisplayTag() + " off the floor!";
                return;
            }
        }
    }

    // [F]: Speed-boost a workstation OR Throw held item into a bay from medium range
    public void actionKeyF() {
        if (inLockerRoom || gameOver || gameWon) return;

        WasteItem held = player.getHeldItem();
        if (held != null) {
            player.setHeldItem(null);
            held.setPosition(player.getX() - 48, player.getY() - 20);
            held.launch(player.getThrowDx() * 12.5, player.getThrowDy() * 12.5);
            thrownItems.add(held);
            popups.add(new FloatingText("THROW!", player.getX(), player.getY() - 25, Color.WHITE));
            lastFeedback = "Threw " + held.getDisplayTag() + "!";
        } else {
            for (WorkStation station : stations) {
                if (station.isNear(player.getX(), player.getY()) && !station.isEmpty()) {
                    station.boostProcess();
                    popups.add(new FloatingText("BOOST!", station.getX() + 30, station.getY() - 5, Color.YELLOW));
                    return;
                }
            }
        }
    }

    public BinType getClosestBayType(double py) {
        if (py < 155) return BinType.BIODEGRADABLE;
        if (py < 250) return BinType.RECYCLABLE;
        if (py < 345) return BinType.RESIDUAL;
        return BinType.HAZARDOUS;
    }

    private void handleRoute(WasteItem item, StorageBay bay) {
        try {
            bay.accept(item);
            int mult = getCurrentLevel().getLevelNumber();

            if (item.getTargetBin() == bay.getType()) {
                if (item.isPreprocessed()) {
                    int pts = 10 * mult + 15;
                    player.addScore(10 * mult, 15);
                    popups.add(new FloatingText("+" + pts + " PREPROCESSED!", 680, player.getY() - 20, Color.GREEN));
                    lastFeedback = "PERFECT PREPROCESS + SORT! +" + pts + " pts";
                } else {
                    int pts = 10 * mult;
                    player.addScore(pts);
                    popups.add(new FloatingText("+" + pts + " SORTED!", 680, player.getY() - 20, Color.CYAN));
                    lastFeedback = "Correct! Sorted " + item.getDisplayTag() + " into " + bay.getType().name();
                }
            } else {
                player.applyPenalty(item.getPenalty());
                screenShake = 12;
                String err = item.getDisplayTag() + " -> " + bay.getType().name() + ": " + item.getExplanation();
                popups.add(new FloatingText("-" + item.getPenalty() + "% VIOLATION!", 660, player.getY() - 20, Color.RED));
                lastFeedback = "VIOLATION (-" + item.getPenalty() + "%): " + item.getExplanation();
                mistakeLog.add(err);
            }
        } catch (BinFullException e) {
            player.applyPenalty(5);
            screenShake = 10;
            popups.add(new FloatingText("BAY FULL! -5%", 670, player.getY() - 20, Color.ORANGE));
            lastFeedback = "OVERFLOW (-5%): Bay full! Pull the Forklift Lever at bottom-right!";
            mistakeLog.add(e.getMessage());
        }
    }

    private void advanceLevel() {
        if (currentLevelIndex < levels.size() - 1) {
            currentLevelIndex++;
            beltItems.clear();
            stations.forEach(WorkStation::clear);
            bays.values().forEach(StorageBay::empty);
            player.setHeldItem(null);
            if (getCurrentLevel() instanceof TyphoonLevel typhoon) {
                typhoon.triggerSurgeEvent();
            }
            popups.add(new FloatingText("SHIFT PROMOTION!", player.getX() - 40, player.getY() - 30, Color.YELLOW));
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
        this.popups.clear();
        this.mistakeLog.clear();
        this.stations.forEach(WorkStation::clear);
        this.bays.values().forEach(StorageBay::empty);
        this.player.resetScore();
        this.player.resetPosition(400, 270);
        this.player.getComplianceMeter().reset(100);
        this.player.getStreakMeter().reset(0);
        this.gameOver = false;
        this.gameWon = false;
        this.lastFeedback = "Shift restarted! Grab items off the top belt with [SPACE].";
    }

    public Player getPlayer() { return player; }
    public FacilityLevel getCurrentLevel() { return levels.get(currentLevelIndex); }
    public Collection<StorageBay> getBays() { return bays.values(); }
    public List<WorkStation> getStations() { return stations; }
    public List<WasteItem> getBeltItems() { return beltItems; }
    public List<FloatingText> getPopups() { return popups; }
    public List<String> getMistakeLog() { return mistakeLog; }
    public String getLastFeedback() { return lastFeedback; }
    public int getScreenShake() { return screenShake; }
    public boolean isInLockerRoom() { return inLockerRoom; }
    public void setInLockerRoom(boolean inLockerRoom) { this.inLockerRoom = inLockerRoom; }
    public boolean isGameOver() { return gameOver; }
    public boolean isGameWon() { return gameWon; }
    public List<WasteItem> getThrownItems() { return thrownItems; }
}