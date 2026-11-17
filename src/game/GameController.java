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
    private int maxUnlockedLevel = 0;
    private int justUnlockedLevel = -1;
    private boolean activeRun = false;

    private final Map<BinType, StorageBay> bays;
    private final List<WorkStation> stations;
    private final List<WasteItem> beltItems;
    private final List<WasteItem> thrownItems = new ArrayList<>();
    private final List<FloatingText> popups;
    private final List<String> mistakeLog;
    private final model.bot.CleanerBot cleanerBot = new model.bot.CleanerBot(640, 270);
    private final model.shop.UpgradeShop shop = new model.shop.UpgradeShop();

    private String lastFeedback;
    private int spawnTimer;
    private int screenShake;
    private boolean inLockerRoom;
    private boolean gameOver;
    private boolean gameWon;

    // 5-Minute Shift Timer (300 seconds * 60 ticks/sec = 18,000 ticks)
    public static final int SHIFT_DURATION_TICKS = 300 * 60;
    private int shiftTimerTicks = SHIFT_DURATION_TICKS;

    public static final int BELT_START_X = 80;
    public static final int BELT_END_X = 500;
    public static final int LANE_Y = 120;

    private enum ForkliftState { IDLE, TO_FULL_BIN, HAUL_OFFSCREEN, RETURN_EMPTY_BIN, RETURN_HOME }
    private ForkliftState forkliftState = ForkliftState.IDLE;
    private StorageBay activeHaulBay = null;
    private boolean carryingFullBin = false;
    private boolean forkliftFacingLeft = false;
    private double forkliftX = 535;
    private double forkliftY = 350;
    private int forkliftHomeX = 535;
    private int forkliftHomeY = 350;

    private int beltStartX = 80;
    private int beltStartY = 120;
    private int beltEndX = 500;
    private int beltEndY = 120;
    private int transportedTrash = 0;
    private int lastEarnedShiftCredits = 0;
    private double stationOverclockAccum = 0.0;
    private boolean shiftVictoryPending = false;

    public GameController(String playerName) {
        this.player = new Player(playerName);
        this.levels = List.of(
                new BarangayLevel(),                                               // 1
                new IndustrialLevel(),                                             // 2
                new TyphoonLevel(),                                                // 3
                new HarborLevel(),                                                 // 4
                new ReactorLevel(),                                                // 5
                new DynamicFacilityLevel(6,  "Eco-Sorting Station",    2.3, 5),    // 6
                new DynamicFacilityLevel(7,  "Coastal Logistics Depot",2.5, 6),    // 7
                new DynamicFacilityLevel(8,  "Tech Park E-Waste Hub",  2.6, 7),    // 8
                new DynamicFacilityLevel(9,  "Central Sorting Yard",   2.8, 0),    // 9
                new DynamicFacilityLevel(10, "Drydock Shipyard MRF",   2.9, 1),    // 10
                new DynamicFacilityLevel(11, "Heavy Smelting Foundry", 3.1, 2),    // 11
                new DynamicFacilityLevel(12, "Hydro Reclamation Basin",3.2, 3),    // 12
                new DynamicFacilityLevel(13, "Offshore Reef Terminal", 3.4, 4),    // 13
                new DynamicFacilityLevel(14, "Lighthouse Point MRF",   3.5, 5),    // 14
                new DynamicFacilityLevel(15, "Thermal Power Plant",    3.7, 6),    // 15
                new DynamicFacilityLevel(16, "Automated Steelworks",   3.8, 7),    // 16
                new DynamicFacilityLevel(17, "Timber & Scrap Mill",    4.0, 1),    // 17
                new DynamicFacilityLevel(18, "Copper Ore Refinery",    4.2, 2),    // 18
                new DynamicFacilityLevel(19, "Highland Bio-Dome",      4.4, 3),    // 19
                new DynamicFacilityLevel(20, "Mega-Nexus Core",        4.6, 4)     // 20
        );
        this.currentLevelIndex = 0;
        this.bays = new EnumMap<>(BinType.class);
        this.stations = new ArrayList<>();
        this.beltItems = new ArrayList<>();
        this.popups = new ArrayList<>();
        this.mistakeLog = new ArrayList<>();

        this.lastFeedback = "Grab trash from the belt [SPACE/Click], process at machines, & walk to bins to sort!";
        this.spawnTimer = 0;
        this.screenShake = 0;
        this.inLockerRoom = true;
        this.gameOver = false;
        this.gameWon = false;

        initFacility();
        model.save.SaveManager.load(this);
    }

    private void initFacility() {
        int bayY = 72;
        for (BinType type : BinType.values()) {
            bays.put(type, new StorageBay(type, 4, 765, bayY));
            bayY += 92;
        }
        stations.add(new WorkStation(StationType.WASHER, 80, 350));
        stations.add(new WorkStation(StationType.DISMANTLER, 230, 350));
        stations.add(new WorkStation(StationType.NEUTRALIZER, 380, 350));
    }

    // Returns current Speed Stage (1 to 4) based on the 5-minute countdown timer!
    public int getTimerSpeedStage() {
        float elapsedRatio = 1.0f - ((float) shiftTimerTicks / SHIFT_DURATION_TICKS);
        if (elapsedRatio < 0.25f) return 1; // 5:00 - 3:45 (Stage 1)
        if (elapsedRatio < 0.50f) return 2; // 3:45 - 2:30 (Stage 2)
        if (elapsedRatio < 0.75f) return 3; // 2:30 - 1:15 (Stage 3)
        return 4;                           // 1:15 - 0:00 (Stage 4 Overdrive!)
    }

    public double getStageSpeedMultiplier() {
        return switch (getTimerSpeedStage()) {
            case 1 -> 1.00;
            case 2 -> 1.30;
            case 3 -> 1.65;
            default -> 2.10;
        };
    }

    public String getFormattedTimeRemaining() {
        int totalSec = Math.max(0, (int) Math.ceil(shiftTimerTicks / 60.0));
        int mins = totalSec / 60;
        int secs = totalSec % 60;
        return String.format("%d:%02d", mins, secs);
    }

    public void updateTick(double moveDx, double moveDy) {
        popups.removeIf(FloatingText::update);
        if (screenShake > 0) screenShake--;

        if (inLockerRoom || gameOver || gameWon || shiftVictoryPending) return;

        // Tick 5-Minute Shift Countdown Timer
        int prevStage = getTimerSpeedStage();
        if (shiftTimerTicks > 0) {
            shiftTimerTicks--;
        }
        int newStage = getTimerSpeedStage();
        if (newStage > prevStage) {
            popups.add(new FloatingText("SPEED STAGE " + newStage + "! BELT ACCELERATING!", 320, 95, Color.ORANGE));
            lastFeedback = "TIMER STAGE " + newStage + "/4: Conveyor belt & music speed increased!";
        }

        if (shiftTimerTicks <= 0) {
            gameOver = true;
            player.clearPendingCredits();
            mistakeLog.add("Time Expired: Failed to transport " + getCurrentLevel().getTrashQuota() + " items within 5:00.");
            lastFeedback = "TIME EXPIRED (0:00)! Quota was not reached in time.";
            return;
        }

        movePlayerWithCollisions(moveDx, moveDy);
        updateForkliftAnimation();

        boolean horizontalBelt = Math.abs(beltEndX - beltStartX) >= Math.abs(beltEndY - beltStartY);
        WasteItem botDelivered = cleanerBot.updateAI(
                thrownItems, beltItems, stations, bays,
                horizontalBelt, (int) forkliftX, (int) forkliftY,
                (bx, by) -> isPointBlocked(bx, by, 12)
        );
        if (botDelivered != null) {
            handleRoute(botDelivered, bays.get(botDelivered.getTargetBin()));
        }
        if (cleanerBot.canUseForklift() && forkliftState == ForkliftState.IDLE
                && isNearForklift(cleanerBot.getX(), cleanerBot.getY(), 130) && hasAnyFullBin()) {
            tryDispatchForklift();
        }

        // Update Thrown / Floor Items (Throwing into machines is allowed; throwing into bins is DISABLED!)
        Iterator<WasteItem> thrownIt = thrownItems.iterator();
        while (thrownIt.hasNext()) {
            WasteItem item = thrownIt.next();
            if (item.isAirborne()) {
                item.updateProjectile();
                double centerX = item.getX() + 23;
                double centerY = item.getY() + 23;

                boolean handledByStation = false;
                for (WorkStation station : stations) {
                    if (station.isEmpty() && Math.hypot((station.getX() + 42) - centerX, (station.getY() + 36) - centerY) <= 46) {
                        if (tryLoadItemIntoStation(item, station)) {
                            thrownIt.remove();
                        } else {
                            item.setPosition(station.getX() + 18, Math.min(430, station.getY() + 78));
                        }
                        handledByStation = true;
                        break;
                    }
                }
                if (handledByStation) continue;

                item.setPosition(
                        Math.max(18, Math.min(835, item.getX())),
                        Math.max(52, Math.min(440, item.getY()))
                );
            } else {
                double cx = item.getX() + 23;
                double cy = item.getY() + 23;
                if (isPointBlocked(cx, cy, 14)) {
                    boolean freed = false;
                    for (int r = 12; r <= 110 && !freed; r += 12) {
                        for (int a = 0; a < 8; a++) {
                            double ang = a * (Math.PI / 4.0);
                            double nx = Math.max(30, Math.min(840, cx + Math.cos(ang) * r));
                            double ny = Math.max(60, Math.min(450, cy + Math.sin(ang) * r));
                            if (!isPointBlocked(nx, ny, 14)) {
                                item.setPosition(nx - 23, ny - 23);
                                freed = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Tick WorkStations (Base 1.0x speed + gentle +8% speed per Turbo Overclock level, max 1.8x at Lv.10)
        int stationTicks = 1;
        stationOverclockAccum += shop.getStationSpeedBonus() * 0.08;
        if (stationOverclockAccum >= 1.0) {
            stationTicks++;
            stationOverclockAccum -= 1.0;
        }
        for (int t = 0; t < stationTicks; t++) {
            for (WorkStation station : stations) {
                if (station.updateTick()) {
                    if (station.consumeWrongItemFinished()) {
                        WasteItem ejected = station.removeItem();
                        if (ejected != null) {
                            ejected.setPosition(station.getX() + 18, Math.min(435, station.getY() + 78));
                            thrownItems.add(ejected);
                        }
                        player.applyPenalty(5);
                        screenShake = 12;
                        if (station.isBroken()) {
                            popups.add(new FloatingText(station.getType().name() + " BROKEN! [REPAIR]", station.getX() - 20, station.getY() - 15, Color.RED));
                            lastFeedback = station.getType().getLabel() + " BROKE (0% HP)! Walk up & press [SPACE/Click] to Repair!";
                        } else {
                            popups.add(new FloatingText("WRONG ITEM! -50% HP", station.getX() - 10, station.getY() - 15, Color.ORANGE));
                            lastFeedback = "Wrong item damaged " + station.getType().getLabel() + " (HP: " + station.getHealth() + "%)!";
                        }
                    } else {
                        if (station.getStoredItem() != null) {
                            station.getStoredItem().markPersonallyProcessed();
                        }
                        popups.add(new FloatingText(station.getType().name() + " READY! (4x HAUL)", station.getX() - 10, station.getY() - 15, Color.GREEN));
                        lastFeedback = station.getType().getLabel() + " finished! Personally processed items yield 4x Haul value!";
                    }
                }
            }
        }

        // Spawn & Move Items along 4-Way Rotatable Conveyor Belt (Scaled by Timer Speed Stage!)
        FacilityLevel level = getCurrentLevel();
        double stageMult = getStageSpeedMultiplier();
        spawnTimer++;
        int baseInterval = Math.max(70, 175 - (level.getLevelNumber() * 5));
        int spawnInterval = Math.max(42, (int) (baseInterval / stageMult));
        if (spawnTimer >= spawnInterval && beltItems.size() < 6) {
            beltItems.add(level.spawnItem(beltStartX, beltStartY, 1));
            spawnTimer = 0;
        }

        double bdx = beltEndX - beltStartX;
        double bdy = beltEndY - beltStartY;
        double bLen = Math.max(1.0, Math.hypot(bdx, bdy));
        double effectiveBeltSpeed = level.getBeltSpeed() * 0.42 * stageMult;
        double stepVx = (bdx / bLen) * effectiveBeltSpeed;
        double stepVy = (bdy / bLen) * effectiveBeltSpeed;

        Iterator<WasteItem> it = beltItems.iterator();
        while (it.hasNext()) {
            WasteItem item = it.next();
            item.moveVector(stepVx, stepVy);

            if (Math.hypot(item.getX() - beltEndX, item.getY() - beltEndY) <= 18) {
                player.applyPenalty(5);
                screenShake = 8;
                popups.add(new FloatingText("-5% BURNED!", beltEndX, beltEndY, Color.RED));
                lastFeedback = "INCINERATOR (-5%): You missed " + item.getDisplayTag() + " on the belt!";
                mistakeLog.add("Missed on belt: " + item.getDisplayTag());
                it.remove();
            }
        }

        // Check Win / Loss conditions
        if (player.getComplianceMeter().isDepleted()) {
            gameOver = true;
            player.clearPendingCredits();
            lastFeedback = "FACILITY SHUTDOWN! Compliance reached 0%.";
        } else if (level.isQuotaMet(transportedTrash) && forkliftState == ForkliftState.IDLE && !shiftVictoryPending) {
            shiftVictoryPending = true;
            activeRun = true;
            lastEarnedShiftCredits = player.bankPendingCredits();
            if (currentLevelIndex >= maxUnlockedLevel && currentLevelIndex < levels.size() - 1) {
                maxUnlockedLevel = currentLevelIndex + 1;
                justUnlockedLevel = maxUnlockedLevel;
            }
            model.save.SaveManager.save(this);
            beltItems.clear();
            thrownItems.clear();
            player.setHeldItem(null);
            lastFeedback = level.getTitle().toUpperCase() + " QUOTA MET! Earned +$" + lastEarnedShiftCredits + "!";
        }
    }

    private double[] getForkliftBayApproach(StorageBay bay) {
        if (bay.getX() >= 700) {
            return new double[]{ bay.getX() - 96, bay.getY() };
        } else {
            return new double[]{ bay.getX() - 18, bay.getY() - 78 };
        }
    }

    private void updateForkliftAnimation() {
        if (forkliftState == ForkliftState.IDLE) return;
        double fkSpeed = 5.4;

        switch (forkliftState) {
            case TO_FULL_BIN -> {
                if (activeHaulBay == null) {
                    forkliftState = ForkliftState.RETURN_HOME;
                    return;
                }
                double[] approach = getForkliftBayApproach(activeHaulBay);
                if (driveForkliftWithPathfinding(approach[0], approach[1], fkSpeed, true)) {
                    activeHaulBay.setBeingHauled(true);
                    carryingFullBin = true;
                    forkliftState = ForkliftState.HAUL_OFFSCREEN;
                    popups.add(new FloatingText("HAULING BIN...", activeHaulBay.getX() - 20, activeHaulBay.getY() - 10, Color.YELLOW));
                }
            }
            case HAUL_OFFSCREEN -> {
                double[] approach = getForkliftBayApproach(activeHaulBay);
                double targetX = 935;
                double targetY = approach[1];
                if (driveForkliftWithPathfinding(targetX, targetY, fkSpeed * 1.15, true)) {
                    int hauled = activeHaulBay.getValidHaulUnits();
                    int wrongIgnored = activeHaulBay.getWrongItemCount();
                    activeHaulBay.empty();
                    this.transportedTrash += hauled;
                    this.player.addPendingCredits(hauled * 15 * getCurrentLevel().getLevelNumber());
                    this.carryingFullBin = false;

                    int quota = getCurrentLevel().getTrashQuota();
                    String wrongNote = wrongIgnored > 0 ? " (" + wrongIgnored + " wrong ignored!)" : "";
                    popups.add(new FloatingText("+" + hauled + " HAULED! (" + transportedTrash + "/" + quota + ")", 640, Math.max(80, (int) forkliftY), hauled > 0 ? Color.GREEN : Color.ORANGE));
                    lastFeedback = "Forklift hauled +" + hauled + " valid units" + wrongNote + "! Returning empty " + activeHaulBay.getType().name() + " bin...";
                    forkliftState = ForkliftState.RETURN_EMPTY_BIN;
                }
            }
            case RETURN_EMPTY_BIN -> {
                double[] approach = getForkliftBayApproach(activeHaulBay);
                if (driveForkliftWithPathfinding(approach[0], approach[1], fkSpeed, true)) {
                    activeHaulBay.setBeingHauled(false);
                    activeHaulBay = findNextFullBin();
                    if (activeHaulBay != null) {
                        forkliftState = ForkliftState.TO_FULL_BIN;
                    } else {
                        forkliftState = ForkliftState.RETURN_HOME;
                    }
                }
            }
            case RETURN_HOME -> {
                if (driveForkliftWithPathfinding(forkliftHomeX, forkliftHomeY, fkSpeed, true)) {
                    forkliftFacingLeft = false;
                    forkliftState = ForkliftState.IDLE;
                }
            }
        }
    }

    private boolean driveForkliftWithPathfinding(double tx, double ty, double speed, boolean avoidObstacles) {
        double directDist = Math.hypot(tx - this.forkliftX, ty - this.forkliftY);
        if (directDist <= speed + 4) {
            this.forkliftX = tx;
            this.forkliftY = ty;
            return true;
        }

        double waypointX = tx;
        double waypointY = ty;

        if (avoidObstacles && this.forkliftX < 820 && isForkliftLineBlocked(this.forkliftX, this.forkliftY, tx, ty)) {
            double[] bfsWp = computeForkliftBfsWaypoint(Math.min(820, tx), ty);
            if (bfsWp != null) {
                waypointX = bfsWp[0];
                waypointY = bfsWp[1];
            }
        }

        double dx = waypointX - this.forkliftX;
        double dy = waypointY - this.forkliftY;
        if (Math.abs(dx) > 1.2) {
            this.forkliftFacingLeft = (dx < 0);
        }

        double baseAng = Math.atan2(dy, dx);
        double[] angles = {
                0, 0.35, -0.35, 0.70, -0.70, 1.05, -1.05,
                1.40, -1.40, 1.75, -1.75, 2.15, -2.15
        };
        for (double off : angles) {
            double ang = baseAng + off;
            double nx = this.forkliftX + Math.cos(ang) * speed;
            double ny = Math.max(55, Math.min(435, this.forkliftY + Math.sin(ang) * speed));
            if (!avoidObstacles || !isForkliftBlocked(nx, ny)) {
                this.forkliftX = nx;
                this.forkliftY = ny;
                return false;
            }
        }
        return false;
    }

    private boolean isForkliftBlocked(double fx, double fy) {
        if (fx > 830) return false;

        double cx = fx + 46;
        double cy = fy + 36;
        double r = 24;

        boolean horizontal = Math.abs(beltEndX - beltStartX) >= Math.abs(beltEndY - beltStartY);
        if (horizontal) {
            double minX = Math.min(beltStartX, beltEndX) - 6;
            double maxX = Math.max(beltStartX, beltEndX) + 50;
            if (circleIntersectsRect(cx, cy, r, minX, beltStartY - 8, maxX - minX, 56)) return true;
        } else {
            double minY = Math.min(beltStartY, beltEndY) - 6;
            double maxY = Math.max(beltStartY, beltEndY) + 50;
            if (circleIntersectsRect(cx, cy, r, beltStartX - 8, minY, 56, maxY - minY)) return true;
        }

        if (circleIntersectsRect(cx, cy, r, beltEndX - 14, beltEndY - 10, 74, 66)) return true;

        for (WorkStation ws : stations) {
            if (circleIntersectsRect(cx, cy, r, ws.getX() + 6, ws.getY() + 8, 72, 54)) return true;
        }

        for (StorageBay bay : bays.values()) {
            if (!bay.isBeingHauled()) {
                if (bay == activeHaulBay && (forkliftState == ForkliftState.TO_FULL_BIN || forkliftState == ForkliftState.RETURN_EMPTY_BIN)) {
                    if (circleIntersectsRect(cx, cy, 12, bay.getX() + 14, bay.getY() + 16, 60, 44)) return true;
                } else {
                    if (circleIntersectsRect(cx, cy, r, bay.getX() + 4, bay.getY() + 8, 80, 58)) return true;
                }
            }
        }
        return false;
    }

    private boolean isForkliftLineBlocked(double x1, double y1, double x2, double y2) {
        double dist = Math.hypot(x2 - x1, y2 - y1);
        int steps = Math.max(2, (int) (dist / 16.0));
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            if (isForkliftBlocked(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t)) return true;
        }
        return false;
    }

    private double[] computeForkliftBfsWaypoint(double targetX, double targetY) {
        int cols = 45, rows = 26, cell = 20;
        int sCol = Math.max(1, Math.min(cols - 2, (int) (this.forkliftX / cell)));
        int sRow = Math.max(2, Math.min(rows - 2, (int) (this.forkliftY / cell)));
        int gCol = Math.max(1, Math.min(cols - 2, (int) (targetX / cell)));
        int gRow = Math.max(2, Math.min(rows - 2, (int) (targetY / cell)));
        if (sCol == gCol && sRow == gRow) return null;

        boolean[][] vis = new boolean[rows][cols];
        int[][] pR = new int[rows][cols];
        int[][] pC = new int[rows][cols];
        Queue<int[]> q = new ArrayDeque<>();
        q.add(new int[]{ sRow, sCol });
        vis[sRow][sCol] = true;

        int[] dR = { -1, 1, 0, 0 };
        int[] dC = { 0, 0, -1, 1 };
        int bR = sRow, bC = sCol;
        double bDist = Double.MAX_VALUE;

        while (!q.isEmpty()) {
            int[] curr = q.poll();
            int r = curr[0], c = curr[1];
            if (r == gRow && c == gCol) { bR = r; bC = c; break; }
            double d = Math.hypot(c - gCol, r - gRow);
            if (!(r == sRow && c == sCol) && d < bDist) { bDist = d; bR = r; bC = c; }

            for (int i = 0; i < 4; i++) {
                int nr = r + dR[i], nc = c + dC[i];
                if (nr < 2 || nr >= rows - 1 || nc < 1 || nc >= cols - 1 || vis[nr][nc]) continue;
                if (isForkliftBlocked(nc * cell, nr * cell)) continue;
                vis[nr][nc] = true;
                pR[nr][nc] = r;
                pC[nr][nc] = c;
                q.add(new int[]{ nr, nc });
            }
        }

        int trR = bR, trC = bC, stepR = trR, stepC = trC;
        while (!(trR == sRow && trC == sCol)) {
            int pr = pR[trR][trC], pc = pC[trR][trC];
            if (pr == sRow && pc == sCol) break;
            stepR = trR; stepC = trC;
            trR = pr; trC = pc;
        }
        return new double[]{ stepC * cell, stepR * cell };
    }

    private StorageBay findNextFullBin() {
        for (StorageBay bay : bays.values()) {
            if (bay.isFull() && !bay.isBeingHauled()) {
                return bay;
            }
        }
        return null;
    }

    private boolean tryLoadItemIntoStation(WasteItem item, WorkStation station) {
        if (station.isBroken()) {
            popups.add(new FloatingText("MACHINE BROKEN! [REPAIR]", station.getX() - 15, station.getY() - 12, Color.RED));
            lastFeedback = station.getType().getLabel() + " is BROKEN! Walk up empty-handed & press [SPACE/Click] to Repair!";
            return false;
        }
        StationType req = item.getRequiredStation();
        if (req == station.getType()) {
            station.placeItem(item);
            popups.add(new FloatingText("LOADED " + station.getType().name(), station.getX(), station.getY() - 10, Color.CYAN));
            lastFeedback = "Placed " + item.getDisplayTag() + " in " + station.getType().getLabel() + "!";
            return true;
        } else {
            station.placeWrongItem(item);
            popups.add(new FloatingText("WRONG ITEM LOADING...", station.getX() - 15, station.getY() - 12, Color.ORANGE));
            lastFeedback = "WARNING: " + item.getDisplayTag() + " does not belong in " + station.getType().getLabel() + "!";
            return true;
        }
    }

    private void movePlayerWithCollisions(double moveDx, double moveDy) {
        double oldX = player.getX();
        double oldY = player.getY();

        if (isPointBlocked(oldX, oldY, 15)) {
            nudgePlayerOutOfObstacles();
            oldX = player.getX();
            oldY = player.getY();
        }

        player.move(moveDx, moveDy, 30, 865, 68, 465);
        double targetX = player.getX();
        double targetY = player.getY();

        double finalX = oldX;
        double finalY = oldY;

        if (!isPointBlocked(targetX, oldY, 15)) {
            finalX = targetX;
        }
        if (!isPointBlocked(finalX, targetY, 15)) {
            finalY = targetY;
        }

        player.setCoordinates(finalX, finalY);
    }

    private void nudgePlayerOutOfObstacles() {
        double px = player.getX();
        double py = player.getY();
        for (int r = 15; r <= 150; r += 15) {
            for (int a = 0; a < 8; a++) {
                double ang = a * (Math.PI / 4.0);
                double nx = Math.max(40, Math.min(735, px + Math.cos(ang) * r));
                double ny = Math.max(75, Math.min(435, py + Math.sin(ang) * r));
                if (!isPointBlocked(nx, ny, 16)) {
                    player.setCoordinates(nx, ny);
                    return;
                }
            }
        }
    }

    public boolean isPointBlocked(double cx, double cy, double radius) {
        boolean horizontal = Math.abs(beltEndX - beltStartX) >= Math.abs(beltEndY - beltStartY);
        if (horizontal) {
            double minX = Math.min(beltStartX, beltEndX) - 6;
            double maxX = Math.max(beltStartX, beltEndX) + 50;
            double minY = beltStartY - 8;
            double maxY = beltStartY + 48;
            if (circleIntersectsRect(cx, cy, radius, minX, minY, maxX - minX, maxY - minY)) return true;
        } else {
            double minX = beltStartX - 8;
            double maxX = beltStartX + 48;
            double minY = Math.min(beltStartY, beltEndY) - 6;
            double maxY = Math.max(beltStartY, beltEndY) + 50;
            if (circleIntersectsRect(cx, cy, radius, minX, minY, maxX - minX, maxY - minY)) return true;
        }

        if (circleIntersectsRect(cx, cy, radius, beltEndX - 14, beltEndY - 10, 74, 66)) return true;

        for (WorkStation ws : stations) {
            if (circleIntersectsRect(cx, cy, radius, ws.getX() + 4, ws.getY() + 6, 76, 62)) return true;
        }

        if (forkliftState == ForkliftState.IDLE) {
            if (circleIntersectsRect(cx, cy, radius, forkliftX + 6, forkliftY + 8, 80, 58)) return true;
        }

        for (StorageBay bay : bays.values()) {
            if (!bay.isBeingHauled() && circleIntersectsRect(cx, cy, radius, bay.getX() + 4, bay.getY() + 8, 80, 60)) {
                return true;
            }
        }

        return false;
    }

    private boolean circleIntersectsRect(double cx, double cy, double r, double rx, double ry, double rw, double rh) {
        double closestX = Math.max(rx, Math.min(cx, rx + rw));
        double closestY = Math.max(ry, Math.min(cy, ry + rh));
        return Math.hypot(cx - closestX, cy - closestY) < r;
    }

    public boolean canPlaceHeldItemDirectly() {
        WasteItem held = player.getHeldItem();
        if (held == null) return false;
        double px = player.getX();
        double py = player.getY();
        StorageBay nearBay = getNearbyBay(px, py, 96);
        if (nearBay != null && !nearBay.isBeingHauled()) return true;
        WorkStation nearStation = getNearbyStation(px, py, 92);
        return nearStation != null && nearStation.isEmpty() && !nearStation.isBroken();
    }

    public void throwHeldItem(float chargeRatio) {
        if (inLockerRoom || gameOver || gameWon || shiftVictoryPending) return;
        WasteItem held = player.getHeldItem();
        if (held == null) return;

        double px = player.getX();
        double py = player.getY();
        player.setHeldItem(null);
        held.setPosition(px - 23, py - 23);

        double speed = 10.5 + (Math.max(0f, Math.min(1f, chargeRatio)) * 11.5);
        held.launch(player.getThrowDx() * speed, player.getThrowDy() * speed);
        thrownItems.add(held);

        String tag = (chargeRatio >= 0.7f) ? "POWER THROW!" : "THROW!";
        popups.add(new FloatingText(tag, px - 25, py - 28, chargeRatio >= 0.7f ? Color.ORANGE : Color.WHITE));
        lastFeedback = "Threw " + held.getDisplayTag() + "! (Note: Bins require walk-up drop-off!)";
    }

    public void interact() {
        if (inLockerRoom || gameOver || gameWon || shiftVictoryPending) return;

        double px = player.getX();
        double py = player.getY();
        WasteItem held = player.getHeldItem();

        if (held != null) {
            StorageBay nearBay = getNearbyBay(px, py, 96);
            if (nearBay != null && !nearBay.isBeingHauled()) {
                handleRoute(held, nearBay);
                player.setHeldItem(null);
                return;
            }

            WorkStation nearStation = getNearbyStation(px, py, 92);
            if (nearStation != null && nearStation.isEmpty()) {
                if (tryLoadItemIntoStation(held, nearStation)) {
                    player.setHeldItem(null);
                }
                return;
            }

            throwHeldItem(0.25f);
            return;
        }

        WasteItem closestFloor = null;
        double bestFloorDist = 95;
        for (WasteItem item : thrownItems) {
            if (!item.isAirborne()) {
                double dist = Math.hypot((item.getX() + 23) - px, (item.getY() + 23) - py);
                if (dist < bestFloorDist) {
                    bestFloorDist = dist;
                    closestFloor = item;
                }
            }
        }
        if (closestFloor != null) {
            thrownItems.remove(closestFloor);
            player.setHeldItem(closestFloor);
            popups.add(new FloatingText("Picked up: " + closestFloor.getDisplayTag(), px - 35, py - 28, Color.YELLOW));
            lastFeedback = "Picked up " + closestFloor.getDisplayTag() + " off the floor!";
            return;
        }

        if (isNearForklift(px, py, 92) && hasAnyFullBin()) {
            tryDispatchForklift();
            return;
        }

        WorkStation nearStation = getNearbyStation(px, py, 92);
        if (nearStation != null && !nearStation.isEmpty()) {
            if (nearStation.isFinished()) {
                WasteItem item = nearStation.removeItem();
                player.setHeldItem(item);
                popups.add(new FloatingText("Picked up: " + item.getDisplayTag(), px - 35, py - 28, Color.YELLOW));
                lastFeedback = "Picked up processed " + item.getDisplayTag() + "! Walk to the matching Bin to drop off.";
            } else if (!nearStation.isWrongItemProcessing()) {
                nearStation.boostProcess();
                popups.add(new FloatingText("BOOST!", nearStation.getX() + 20, nearStation.getY() - 8, Color.YELLOW));
            }
            return;
        }

        WasteItem closestBelt = null;
        double bestBeltDist = 92;
        for (WasteItem item : beltItems) {
            double dist = Math.hypot((item.getX() + 23) - px, (item.getY() + 23) - py);
            if (dist < bestBeltDist) {
                bestBeltDist = dist;
                closestBelt = item;
            }
        }
        if (closestBelt != null) {
            beltItems.remove(closestBelt);
            closestBelt.clearSpawnDrop();
            player.setHeldItem(closestBelt);
            popups.add(new FloatingText("Picked up: " + closestBelt.getDisplayTag(), px - 35, py - 28, Color.YELLOW));
            lastFeedback = "Picked up " + closestBelt.getDisplayTag() + "! Throw into Machines or walk to Bins!";
            return;
        }

        if (isNearForklift(px, py, 92)) {
            tryDispatchForklift();
        }
    }

    public void actionKeyF() {
        interact();
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
                if (item.isPersonallyProcessed()) {
                    int pts = (10 * mult) * 4;
                    player.addScore(pts, 15);
                    popups.add(new FloatingText("+" + pts + " (4x HAUL VALUE!)", bay.getX() - 25, bay.getY() - 10, Color.GREEN));
                    lastFeedback = "PERSONALLY PROCESSED! Counts as 4x Haul Value in " + bay.getType().name() + "!";
                } else {
                    int pts = 10 * mult;
                    player.addScore(pts);
                    popups.add(new FloatingText("+" + pts + " SORTED (1x)", bay.getX() - 10, bay.getY() - 10, Color.CYAN));
                    lastFeedback = "Sorted " + item.getDisplayTag() + " into " + bay.getType().name() + " (1x Haul Value).";
                }
            } else {
                player.applyPenalty(item.getPenalty());
                screenShake = 12;
                String err = item.getDisplayTag() + " -> " + bay.getType().name() + ": " + item.getExplanation();
                popups.add(new FloatingText("-" + item.getPenalty() + "% WRONG BIN! (0x HAUL)", bay.getX() - 25, bay.getY() - 10, Color.RED));
                lastFeedback = "WRONG BIN (-" + item.getPenalty() + "%): Takes up 1 bin slot, but yields 0 Haul when transported!";
                mistakeLog.add(err);
            }
        } catch (BinFullException e) {
            player.applyPenalty(5);
            screenShake = 10;
            popups.add(new FloatingText("BIN FULL! -5%", bay.getX() - 10, bay.getY() - 10, Color.ORANGE));
            lastFeedback = "OVERFLOW (-5%): Bin full! Wake up the Forklift driver to haul full bins!";
            mistakeLog.add(e.getMessage());
        }
    }

    public StorageBay getNearbyBay(double x, double y, double maxDist) {
        StorageBay best = null;
        double bestDist = maxDist;
        for (StorageBay bay : bays.values()) {
            if (bay.isBeingHauled()) continue;
            double dist = Math.hypot((bay.getX() + 44) - x, (bay.getY() + 36) - y);
            if (dist < bestDist) {
                bestDist = dist;
                best = bay;
            }
        }
        return best;
    }

    public WorkStation getNearbyStation(double x, double y, double maxDist) {
        WorkStation best = null;
        double bestDist = maxDist;
        for (WorkStation ws : stations) {
            double dist = Math.hypot((ws.getX() + 42) - x, (ws.getY() + 36) - y);
            if (dist < bestDist) {
                bestDist = dist;
                best = ws;
            }
        }
        return best;
    }

    public boolean isNearForklift(double x, double y, double maxDist) {
        return Math.hypot((forkliftX + 46) - x, (forkliftY + 36) - y) <= maxDist;
    }

    public boolean hasAnyFullBin() {
        for (StorageBay bay : bays.values()) {
            if (bay.isFull() && !bay.isBeingHauled()) return true;
        }
        return false;
    }

    public boolean tryDispatchForklift() {
        if (forkliftState != ForkliftState.IDLE) {
            lastFeedback = "Forklift is already hauling bins!";
            return false;
        }
        StorageBay nextFull = findNextFullBin();
        if (nextFull == null) {
            popups.add(new FloatingText("NO FULL BINS!", (int) forkliftX, (int) forkliftY - 12, Color.ORANGE));
            lastFeedback = "Forklift Driver Sleeping: Fill at least one bin completely before waking him!";
            return false;
        }

        this.activeHaulBay = nextFull;
        this.forkliftState = ForkliftState.TO_FULL_BIN;
        popups.add(new FloatingText("DRIVER AWAKE! HAULING!", (int) forkliftX - 15, (int) forkliftY - 12, Color.CYAN));
        lastFeedback = "Forklift driver woke up! En route to haul full " + nextFull.getType().name() + " bin!";
        return true;
    }

    public void selectLevel(int index) {
        int clamped = Math.max(0, Math.min(levels.size() - 1, Math.min(maxUnlockedLevel, index)));
        this.currentLevelIndex = clamped;
        this.activeRun = true;
        this.transportedTrash = 0;
        this.shiftTimerTicks = SHIFT_DURATION_TICKS;
        this.spawnTimer = 0;
        this.beltItems.clear();
        this.thrownItems.clear();
        this.popups.clear();
        this.mistakeLog.clear();
        this.stations.forEach(ws -> { ws.clear(); ws.repairFull(); });
        this.bays.values().forEach(StorageBay::empty);
        this.cleanerBot.reset();
        this.player.setHeldItem(null);
        this.player.clearPendingCredits();
        this.player.getComplianceMeter().reset(100);
        this.player.getStreakMeter().reset(0);
        this.shiftVictoryPending = false;
        this.gameOver = false;
        this.gameWon = false;
        this.shop.applyPendingUpgrades(player, cleanerBot, bays.values());
        applyStageLayout();
        this.player.resetPosition(400, 260);
        nudgePlayerOutOfObstacles();
        model.save.SaveManager.save(this);
        this.lastFeedback = "Deployed to " + getCurrentLevel().getTitle() + "! Complete quota before 5:00 expires!";
    }

    public void applyStageLayout() {
        FacilityLevel lvl = getCurrentLevel();
        int[] st = lvl.getStageLayout();
        if (stations.size() >= 3) {
            stations.get(0).setPosition(st[0], st[1]);
            stations.get(1).setPosition(st[2], st[3]);
            stations.get(2).setPosition(st[4], st[5]);
        }
        this.forkliftHomeX = st[6];
        this.forkliftHomeY = st[7];
        this.forkliftX = forkliftHomeX;
        this.forkliftY = forkliftHomeY;
        this.forkliftState = ForkliftState.IDLE;
        this.activeHaulBay = null;
        this.carryingFullBin = false;
        this.forkliftFacingLeft = false;

        int[] belt = lvl.getBeltLayout();
        this.beltStartX = belt[0];
        this.beltStartY = belt[1];
        this.beltEndX = belt[2];
        this.beltEndY = belt[3];

        int[] bayCoords = lvl.getBayLayout();
        BinType[] types = BinType.values();
        for (int i = 0; i < types.length; i++) {
            StorageBay b = bays.get(types[i]);
            if (b != null) {
                b.setBeingHauled(false);
                b.setPosition(bayCoords[i * 2], bayCoords[i * 2 + 1]);
            }
        }
    }

    public void completeShiftPromotion() {
        shiftVictoryPending = false;
        shop.applyPendingUpgrades(player, cleanerBot, bays.values());
        stations.forEach(WorkStation::repairFull);

        if (currentLevelIndex < levels.size() - 1) {
            if (currentLevelIndex >= maxUnlockedLevel) {
                maxUnlockedLevel = currentLevelIndex + 1;
            }
            currentLevelIndex++;
            this.transportedTrash = 0;
            this.shiftTimerTicks = SHIFT_DURATION_TICKS;
            beltItems.clear();
            thrownItems.clear();
            stations.forEach(WorkStation::clear);
            bays.values().forEach(StorageBay::empty);
            player.setHeldItem(null);
            cleanerBot.reset();
            applyStageLayout();
            nudgePlayerOutOfObstacles();
            if (getCurrentLevel() instanceof TyphoonLevel typhoon) {
                typhoon.triggerSurgeEvent();
            }
            model.save.SaveManager.save(this);
            lastFeedback = "WELCOME TO " + getCurrentLevel().getTitle().toUpperCase() + "!";
        } else {
            gameWon = true;
            lastFeedback = "ALL 20 MRF FACILITIES CLEARED! Final Score: " + player.getScore();
        }
    }

    public void loadSavedProgress(int savedLevelIdx, int savedScore) {
        this.currentLevelIndex = Math.max(0, Math.min(levels.size() - 1, savedLevelIdx));
        this.maxUnlockedLevel = Math.max(this.maxUnlockedLevel, this.currentLevelIndex);
        this.shiftTimerTicks = SHIFT_DURATION_TICKS;
        this.player.resetScore();
        this.player.clearPendingCredits();
        if (savedScore > 0) this.player.addScore(savedScore);
        this.player.clearPendingCredits();
        this.player.getStreakMeter().reset(0);
        this.beltItems.clear();
        this.thrownItems.clear();
        this.stations.forEach(ws -> { ws.clear(); ws.repairFull(); });
        this.bays.values().forEach(StorageBay::empty);
        this.shiftVictoryPending = false;
        applyStageLayout();
        nudgePlayerOutOfObstacles();
    }

    public void resetToBrandNewSave() {
        this.currentLevelIndex = 0;
        this.maxUnlockedLevel = 0;
        this.justUnlockedLevel = -1;
        this.activeRun = false;
        this.player.resetUpgrades();
        this.player.resetScore();
        this.player.clearPendingCredits();
        this.player.getCosmetics().resetDefaults();
        this.cleanerBot.setLevel(0);
        this.shop.resetAll(this.player, this.bays.values());
        resetGame();
        this.activeRun = false;
    }

    public void resetGame() {
        // Keeps currentLevelIndex as-is so restarting only resets the current facility!
        this.transportedTrash = 0;
        this.lastEarnedShiftCredits = 0;
        this.shiftTimerTicks = SHIFT_DURATION_TICKS;
        this.player.clearPendingCredits();
        this.shop.applyPendingUpgrades(player, cleanerBot, bays.values());
        applyStageLayout();
        this.spawnTimer = 0;
        this.beltItems.clear();
        this.thrownItems.clear();
        this.popups.clear();
        this.mistakeLog.clear();
        this.stations.forEach(ws -> { ws.clear(); ws.repairFull(); });
        this.bays.values().forEach(StorageBay::empty);
        this.cleanerBot.reset();
        this.player.setHeldItem(null);
        this.player.resetScore();
        this.player.resetPosition(400, 260);
        nudgePlayerOutOfObstacles();
        this.player.getComplianceMeter().reset(100);
        this.player.getStreakMeter().reset(0);
        this.shiftVictoryPending = false;
        this.gameOver = false;
        this.gameWon = false;
        this.lastFeedback = "Facility restarted! Walk to Bins to sort or Hold [SPACE/Click] to throw into Machines!";
    }

    public model.bot.CleanerBot getCleanerBot() { return cleanerBot; }
    public model.shop.UpgradeShop getShop() { return shop; }
    public Player getPlayer() { return player; }
    public List<FacilityLevel> getLevels() { return levels; }
    public FacilityLevel getCurrentLevel() { return levels.get(currentLevelIndex); }
    public int getCurrentLevelIndex() { return currentLevelIndex; }
    public int getMaxUnlockedLevel() { return maxUnlockedLevel; }
    public void setMaxUnlockedLevel(int max) { this.maxUnlockedLevel = Math.max(0, Math.min(levels.size() - 1, max)); }
    public int consumeJustUnlockedLevel() {
        int val = justUnlockedLevel;
        justUnlockedLevel = -1;
        return val;
    }
    public boolean hasActiveRun() { return activeRun; }
    public void setHasActiveRun(boolean active) { this.activeRun = active; }
    public Collection<StorageBay> getBays() { return bays.values(); }
    public List<WorkStation> getStations() { return stations; }
    public List<WasteItem> getBeltItems() { return beltItems; }
    public List<FloatingText> getPopups() { return popups; }
    public List<String> getMistakeLog() { return mistakeLog; }
    public String getLastFeedback() { return lastFeedback; }
    public int getScreenShake() { return screenShake; }
    public int getForkliftX() { return (int) forkliftX; }
    public int getForkliftY() { return (int) forkliftY; }
    public boolean isForkliftFacingLeft() { return forkliftFacingLeft; }
    public StorageBay getActiveHaulBay() { return activeHaulBay; }
    public boolean isCarryingFullBin() { return carryingFullBin; }
    public boolean isForkliftBusy() { return forkliftState != ForkliftState.IDLE; }
    public int getBeltStartX() { return beltStartX; }
    public int getBeltStartY() { return beltStartY; }
    public int getBeltEndX() { return beltEndX; }
    public int getBeltEndY() { return beltEndY; }
    public int getTransportedTrash() { return transportedTrash; }
    public void setTransportedTrash(int count) { this.transportedTrash = Math.max(0, count); }
    public int getLastEarnedShiftCredits() { return lastEarnedShiftCredits; }
    public boolean isShiftVictoryPending() { return shiftVictoryPending; }
    public void clearShiftVictoryPending() { this.shiftVictoryPending = false; }
    public boolean isInLockerRoom() { return inLockerRoom; }
    public void setInLockerRoom(boolean inLockerRoom) { this.inLockerRoom = inLockerRoom; }
    public boolean isGameOver() { return gameOver; }
    public boolean isGameWon() { return gameWon; }
    public List<WasteItem> getThrownItems() { return thrownItems; }
}