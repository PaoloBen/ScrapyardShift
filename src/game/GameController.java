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
    private final model.bot.CleanerBot cleanerBot = new model.bot.CleanerBot(640, 270);
    private final model.shop.UpgradeShop shop = new model.shop.UpgradeShop();

    private String lastFeedback;
    private int spawnTimer;
    private int screenShake;
    private boolean inLockerRoom;
    private boolean gameOver;
    private boolean gameWon;

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
    private boolean shiftVictoryPending = false;

    public GameController(String playerName) {
        this.player = new Player(playerName);
        this.levels = List.of(
                new BarangayLevel(),
                new IndustrialLevel(),
                new TyphoonLevel(),
                new HarborLevel(),
                new ReactorLevel()
        );
        this.currentLevelIndex = 0;
        this.bays = new EnumMap<>(BinType.class);
        this.stations = new ArrayList<>();
        this.beltItems = new ArrayList<>();
        this.popups = new ArrayList<>();
        this.mistakeLog = new ArrayList<>();

        this.lastFeedback = "Grab trash from the belt [SPACE/Click], process at machines, & sort into bins!";
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

    public void updateTick(double moveDx, double moveDy) {
        popups.removeIf(FloatingText::update);
        if (screenShake > 0) screenShake--;

        if (inLockerRoom || gameOver || gameWon || shiftVictoryPending) return;

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
        if (cleanerBot.getLevel() > 0 && forkliftState == ForkliftState.IDLE
                && isNearForklift(cleanerBot.getX(), cleanerBot.getY(), 130) && hasAnyFullBin()) {
            tryDispatchForklift();
        }

        // Update Thrown / Floor Items
        Iterator<WasteItem> thrownIt = thrownItems.iterator();
        while (thrownIt.hasNext()) {
            WasteItem item = thrownIt.next();
            if (item.isAirborne()) {
                item.updateProjectile();
                double centerX = item.getX() + 23;
                double centerY = item.getY() + 23;

                StorageBay hitBay = getNearbyBay(centerX, centerY, 54);
                if (hitBay != null && !hitBay.isBeingHauled()) {
                    handleRoute(item, hitBay);
                    thrownIt.remove();
                    continue;
                }

                boolean handledByStation = false;
                for (WorkStation station : stations) {
                    if (station.isEmpty() && Math.hypot((station.getX() + 42) - centerX, (station.getY() + 36) - centerY) <= 46) {
                        if (tryLoadItemIntoStation(item, station)) {
                            thrownIt.remove();
                        } else {
                            // Machine is broken -> bounce onto open floor in front of it
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

        // Tick WorkStations (including wrong-item damage & breakdown check!)
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
                    popups.add(new FloatingText(station.getType().name() + " READY!", station.getX() + 5, station.getY() - 15, Color.GREEN));
                    lastFeedback = station.getType().getLabel() + " finished processing! Grab item with [SPACE/Click].";
                }
            }
        }

        // Spawn & Move Items along 4-Way Rotatable Conveyor Belt
        FacilityLevel level = getCurrentLevel();
        spawnTimer++;
        int spawnInterval = Math.max(95, 180 - (level.getLevelNumber() * 25));
        if (spawnTimer >= spawnInterval && beltItems.size() < 5) {
            beltItems.add(level.spawnItem(beltStartX, beltStartY, 1));
            spawnTimer = 0;
        }

        double bdx = beltEndX - beltStartX;
        double bdy = beltEndY - beltStartY;
        double bLen = Math.max(1.0, Math.hypot(bdx, bdy));
        double stepVx = (bdx / bLen) * (level.getBeltSpeed() * 0.45);
        double stepVy = (bdy / bLen) * (level.getBeltSpeed() * 0.45);

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
            lastFeedback = "SHIFT TERMINATED! Compliance reached 0%.";
        } else if (level.isQuotaMet(transportedTrash) && forkliftState == ForkliftState.IDLE && !shiftVictoryPending) {
            shiftVictoryPending = true;
            lastEarnedShiftCredits = player.bankPendingCredits();
            model.save.SaveManager.save(this);
            beltItems.clear();
            thrownItems.clear();
            player.setHeldItem(null);
            lastFeedback = "FORKLIFT QUOTA MET! Earned +$" + lastEarnedShiftCredits + " Credits!";
        }
    }

    // Computes an unblocked pickup/dropoff pose in the open lane beside or above the target bin!
    private double[] getForkliftBayApproach(StorageBay bay) {
        if (bay.getX() >= 700) {
            // Right-wall bins: approach from the open floor to the left of the bin
            return new double[]{ bay.getX() - 96, bay.getY() };
        } else {
            // Bottom-row bins (Shift 2): approach from the open highway lane above the bin row!
            return new double[]{ bay.getX() - 18, bay.getY() - 78 };
        }
    }

    private void updateForkliftAnimation() {
        if (forkliftState == ForkliftState.IDLE) return;
        double fkSpeed = 5.2;

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
                // Keep avoidObstacles = true so the Forklift routes around all other bins and machines!
                if (driveForkliftWithPathfinding(targetX, targetY, fkSpeed * 1.15, true)) {
                    int hauled = activeHaulBay.getFillLevel();
                    activeHaulBay.empty();
                    this.transportedTrash += hauled;
                    this.player.addPendingCredits(25);
                    this.carryingFullBin = false;

                    int quota = getCurrentLevel().getTrashQuota();
                    popups.add(new FloatingText("+" + hauled + " TRANSPORTED! (" + transportedTrash + "/" + quota + ")", 660, Math.max(80, (int) forkliftY), Color.GREEN));
                    lastFeedback = "Forklift transported " + hauled + " items! Returning empty " + activeHaulBay.getType().name() + " bin...";
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

    // Obstacle-Avoiding Forklift Navigation (Uses Grid BFS across all states so it NEVER noclips through bins or machines!)
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
        // Once past the right-wall bins (fx > 830), the off-screen exit ramp is completely clear
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
                // While approaching activeHaulBay, allow getting close enough to its front edge to lift it,
                // while keeping a full solid collision barrier around all other bins!
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

    // Loads item into machine (even if wrong item, so it plays the processing animation before damaging!)
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
        lastFeedback = "Threw " + held.getDisplayTag() + " (" + (int) (chargeRatio * 100) + "% power)!";
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
                lastFeedback = "Picked up processed " + item.getDisplayTag() + "!";
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
            lastFeedback = "Picked up " + closestBelt.getDisplayTag() + "! Tap to Place or Hold to Power-Throw!";
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
                if (item.isPreprocessed()) {
                    int pts = 10 * mult + 15;
                    player.addScore(10 * mult, 15);
                    popups.add(new FloatingText("+" + pts + " PREPROCESSED!", bay.getX() - 20, bay.getY() - 10, Color.GREEN));
                    lastFeedback = "PERFECT PREPROCESS + SORT! +" + pts + " pts";
                } else {
                    int pts = 10 * mult;
                    player.addScore(pts);
                    popups.add(new FloatingText("+" + pts + " SORTED!", bay.getX() - 10, bay.getY() - 10, Color.CYAN));
                    lastFeedback = "Correct! Sorted " + item.getDisplayTag() + " into " + bay.getType().name();
                }
            } else {
                player.applyPenalty(item.getPenalty());
                screenShake = 12;
                String err = item.getDisplayTag() + " -> " + bay.getType().name() + ": " + item.getExplanation();
                popups.add(new FloatingText("-" + item.getPenalty() + "% WRONG BIN!", bay.getX() - 20, bay.getY() - 10, Color.RED));
                lastFeedback = "VIOLATION (-" + item.getPenalty() + "%): " + item.getExplanation();
                mistakeLog.add(err);
            }
        } catch (BinFullException e) {
            player.applyPenalty(5);
            screenShake = 10;
            popups.add(new FloatingText("BIN FULL! -5%", bay.getX() - 10, bay.getY() - 10, Color.ORANGE));
            lastFeedback = "OVERFLOW (-5%): Bin full! Activate the Forklift to haul full bins!";
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
            lastFeedback = "Forklift Idle: Fill at least one bin completely before transporting!";
            return false;
        }

        this.activeHaulBay = nextFull;
        this.forkliftState = ForkliftState.TO_FULL_BIN;
        popups.add(new FloatingText("FORKLIFT DISPATCHED!", (int) forkliftX - 10, (int) forkliftY - 12, Color.CYAN));
        lastFeedback = "Forklift en route to haul full " + nextFull.getType().name() + " bin!";
        return true;
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
            currentLevelIndex++;
            this.transportedTrash = 0;
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
            lastFeedback = "WELCOME TO " + getCurrentLevel().getTitle().toUpperCase() + "! Shop upgrades applied!";
        } else {
            gameWon = true;
            lastFeedback = "ALL 5 SHIFTS CLEARED! Final Score: " + player.getScore();
        }
    }

    public void loadSavedProgress(int savedLevelIdx, int savedScore) {
        this.currentLevelIndex = Math.max(0, Math.min(levels.size() - 1, savedLevelIdx));
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
        this.player.resetUpgrades();
        this.player.clearPendingCredits();
        this.player.getCosmetics().resetDefaults();
        this.cleanerBot.setLevel(0);
        this.shop.resetAll(this.player, this.bays.values());
        resetGame();
    }

    public void resetGame() {
        this.currentLevelIndex = 0;
        this.transportedTrash = 0;
        this.lastEarnedShiftCredits = 0;
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
        this.lastFeedback = "Shift restarted! Tap [SPACE/Click] to Place or Hold to Power-Throw!";
    }

    public model.bot.CleanerBot getCleanerBot() { return cleanerBot; }
    public model.shop.UpgradeShop getShop() { return shop; }
    public Player getPlayer() { return player; }
    public FacilityLevel getCurrentLevel() { return levels.get(currentLevelIndex); }
    public int getCurrentLevelIndex() { return currentLevelIndex; }
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