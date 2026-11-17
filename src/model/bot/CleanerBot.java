package model.bot;

import model.contract.Movable;
import model.item.BinType;
import model.item.WasteItem;
import model.station.StationType;
import model.station.StorageBay;
import model.station.WorkStation;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class CleanerBot implements Movable {

    @FunctionalInterface
    public interface CollisionSensor {
        boolean isBlocked(double x, double y);
    }

    private static final int CELL_SIZE = 20;
    private static final int GRID_COLS = 45; // Full 900px width
    private static final int GRID_ROWS = 26; // Full 520px height

    private double x;
    private double y;
    private final double homeX;
    private final double homeY;
    private double facingDx = 0;
    private double facingDy = 1;
    private boolean moving = false;
    private WasteItem heldItem;
    private int level;
    private double animTick;

    private double lastX = -1;
    private double lastY = -1;
    private int stuckTicks = 0;
    private double escapeAngle = 0;
    private int escapeTicks = 0;

    public CleanerBot(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.homeX = startX;
        this.homeY = startY;
        this.heldItem = null;
        this.level = 0;
        this.animTick = 0;
    }

    @Override
    public void move(double speed) {
        this.animTick += 0.25;
    }

    public WasteItem updateAI(List<WasteItem> floorItems, List<WasteItem> beltItems,
                              List<WorkStation> stations, Map<BinType, StorageBay> bays,
                              boolean horizontalBelt, int forkliftX, int forkliftY,
                              CollisionSensor sensor) {
        if (level <= 0) return null;
        double speed = 2.2 + (level * 0.95);
        this.animTick += 0.28;
        this.moving = false;

        if (sensor != null && sensor.isBlocked(this.x, this.y)) {
            nudgeOutOfObstacle(sensor);
        }

        // STATE 1: Carrying an item
        if (heldItem != null) {
            StationType neededStation = heldItem.getRequiredStation();

            if (neededStation != null) {
                WorkStation targetStation = findEmptyStation(stations, neededStation);
                if (targetStation != null) {
                    double sx = targetStation.getX() + 42;
                    double sy = targetStation.getY() + 36;
                    double[] stand = getBestApproachPoint(sx, sy, 56, sensor);
                    boolean arrived = navigateTowards(stand[0], stand[1], speed, sensor);
                    if (arrived || Math.hypot(sx - x, sy - y) <= 92) {
                        targetStation.placeItem(heldItem);
                        heldItem = null;
                    } else if (heldItem != null) {
                        syncHeldPosition();
                    }
                    return null;
                }
            }

            BinType targetBin = heldItem.getTargetBin();
            StorageBay bay = bays.get(targetBin);
            if (bay != null && !bay.isBeingHauled()) {
                if (bay.isFull()) {
                    if (Math.hypot((forkliftX + 46) - x, (forkliftY + 36) - y) > 115) {
                        double[] fkStand = getBestApproachPoint(forkliftX + 46, forkliftY + 36, 78, sensor);
                        navigateTowards(fkStand[0], fkStand[1], speed, sensor);
                    }
                    syncHeldPosition();
                    return null;
                }

                double bx = bay.getX() + 44;
                double by = bay.getY() + 36;
                double[] binStand = getBestApproachPoint(bx, by, 58, sensor);
                boolean arrived = navigateTowards(binStand[0], binStand[1], speed, sensor);
                if (arrived || Math.hypot(bx - x, by - y) <= 94) {
                    WasteItem delivered = heldItem;
                    heldItem = null;
                    return delivered;
                }
            }
            syncHeldPosition();
            return null;
        }

        // STATE 2: Hands empty -> Priority 1: Pick up the CLOSEST finished item from WorkStations
        WorkStation closestFinishedWs = null;
        double bestWsDist = Double.MAX_VALUE;
        for (WorkStation ws : stations) {
            if (ws.isFinished() && !ws.isBroken()) {
                double d = Math.hypot((ws.getX() + 42) - x, (ws.getY() + 36) - y);
                if (d < bestWsDist) {
                    bestWsDist = d;
                    closestFinishedWs = ws;
                }
            }
        }
        if (closestFinishedWs != null) {
            double sx = closestFinishedWs.getX() + 42;
            double sy = closestFinishedWs.getY() + 36;
            double[] stand = getBestApproachPoint(sx, sy, 56, sensor);
            boolean arrived = (bestWsDist <= 92) || navigateTowards(stand[0], stand[1], speed, sensor);
            if (arrived || Math.hypot(sx - x, sy - y) <= 96) {
                this.heldItem = closestFinishedWs.removeItem();
                if (this.heldItem != null) {
                    this.heldItem.clearSpawnDrop();
                    syncHeldPosition();
                }
            }
            return null;
        }

        // Priority 2: Find the single CLOSEST trash item across both the Floor and the Conveyor Belt
        WasteItem closestTrash = null;
        boolean targetIsOnFloor = false;
        double bestTrashDist = Double.MAX_VALUE;

        for (WasteItem item : floorItems) {
            if (!item.isAirborne()) {
                double d = Math.hypot((item.getX() + 23) - x, (item.getY() + 23) - y);
                if (d < bestTrashDist) {
                    bestTrashDist = d;
                    closestTrash = item;
                    targetIsOnFloor = true;
                }
            }
        }

        for (WasteItem item : beltItems) {
            if (item.getSpawnDropTimer() <= 0) {
                double d = Math.hypot((item.getX() + 23) - x, (item.getY() + 23) - y);
                if (d < bestTrashDist) {
                    bestTrashDist = d;
                    closestTrash = item;
                    targetIsOnFloor = false;
                }
            }
        }

        if (closestTrash != null) {
            double ix = closestTrash.getX() + 23;
            double iy = closestTrash.getY() + 23;

            if (bestTrashDist <= 86) {
                if (targetIsOnFloor) {
                    floorItems.remove(closestTrash);
                } else {
                    beltItems.remove(closestTrash);
                }
                this.heldItem = closestTrash;
                this.heldItem.clearSpawnDrop();
                syncHeldPosition();
                return null;
            }

            double standRadius = targetIsOnFloor ? 28 : 52;
            double[] stand = getBestApproachPoint(ix, iy, standRadius, sensor);
            if (navigateTowards(stand[0], stand[1], speed, sensor) || Math.hypot(ix - x, iy - y) <= 90) {
                if (targetIsOnFloor) {
                    floorItems.remove(closestTrash);
                } else {
                    beltItems.remove(closestTrash);
                }
                this.heldItem = closestTrash;
                this.heldItem.clearSpawnDrop();
                syncHeldPosition();
            }
            return null;
        }

        // Priority 3: If any bin is full while idle, go trigger the Forklift!
        for (StorageBay bay : bays.values()) {
            if (bay.isFull() && !bay.isBeingHauled()) {
                double[] fkStand = getBestApproachPoint(forkliftX + 46, forkliftY + 36, 60, sensor);
                navigateTowards(fkStand[0], fkStand[1], speed, sensor);
                return null;
            }
        }

        navigateTowards(homeX, homeY, speed * 0.6, sensor);
        return null;
    }

    private double[] getBestApproachPoint(double centerX, double centerY, double standRadius, CollisionSensor sensor) {
        if (sensor == null) return new double[]{ centerX, centerY };

        int[][] distMap = computeBfsDistanceMap(sensor);
        double bestX = centerX;
        double bestY = centerY;
        double bestScore = Double.MAX_VALUE;

        double[] radii = { standRadius, standRadius + 14, standRadius + 26, 0 };
        double[] angles = {
                0, Math.PI, Math.PI / 2, -Math.PI / 2,
                Math.PI / 4, -Math.PI / 4, 3 * Math.PI / 4, -3 * Math.PI / 4
        };

        for (double r : radii) {
            for (double ang : angles) {
                double candX = Math.max(22, Math.min(878, centerX + Math.cos(ang) * r));
                double candY = Math.max(52, Math.min(495, centerY + Math.sin(ang) * r));
                if (!sensor.isBlocked(candX, candY)) {
                    int col = Math.max(0, Math.min(GRID_COLS - 1, (int) (candX / CELL_SIZE)));
                    int row = Math.max(2, Math.min(GRID_ROWS - 1, (int) (candY / CELL_SIZE)));
                    int bfsSteps = distMap[row][col];
                    double score = (bfsSteps < Integer.MAX_VALUE)
                            ? (bfsSteps * CELL_SIZE) + (Math.hypot(candX - x, candY - y) * 0.25)
                            : 100000 + Math.hypot(candX - x, candY - y);
                    if (score < bestScore) {
                        bestScore = score;
                        bestX = candX;
                        bestY = candY;
                    }
                }
            }
        }
        return new double[]{ bestX, bestY };
    }

    private int[][] computeBfsDistanceMap(CollisionSensor sensor) {
        int[][] dist = new int[GRID_ROWS][GRID_COLS];
        for (int r = 0; r < GRID_ROWS; r++) {
            for (int c = 0; c < GRID_COLS; c++) dist[r][c] = Integer.MAX_VALUE;
        }
        int startCol = Math.max(0, Math.min(GRID_COLS - 1, (int) (this.x / CELL_SIZE)));
        int startRow = Math.max(2, Math.min(GRID_ROWS - 1, (int) (this.y / CELL_SIZE)));

        Queue<int[]> q = new ArrayDeque<>();
        q.add(new int[]{ startRow, startCol });
        dist[startRow][startCol] = 0;

        int[] dR = { -1, 1, 0, 0 };
        int[] dC = { 0, 0, -1, 1 };

        while (!q.isEmpty()) {
            int[] curr = q.poll();
            int r = curr[0], c = curr[1];
            for (int i = 0; i < 4; i++) {
                int nr = r + dR[i], nc = c + dC[i];
                if (nr < 2 || nr >= GRID_ROWS || nc < 0 || nc >= GRID_COLS) continue;
                if (dist[nr][nc] != Integer.MAX_VALUE) continue;
                double wx = nc * CELL_SIZE + 10.0;
                double wy = nr * CELL_SIZE + 10.0;
                if (sensor != null && sensor.isBlocked(wx, wy)) continue;
                dist[nr][nc] = dist[r][c] + 1;
                q.add(new int[]{ nr, nc });
            }
        }
        return dist;
    }

    private boolean navigateTowards(double tx, double ty, double speed, CollisionSensor sensor) {
        double directDist = Math.hypot(tx - this.x, ty - this.y);
        if (directDist <= speed + 8) {
            stuckTicks = 0;
            return true;
        }

        if (Math.hypot(this.x - lastX, this.y - lastY) < 0.5) {
            stuckTicks++;
            if (stuckTicks >= 5 && escapeTicks <= 0 && sensor != null) {
                escapeAngle = findMostOpenDirection(sensor);
                escapeTicks = 14;
                stuckTicks = 0;
            }
        } else {
            stuckTicks = 0;
        }
        lastX = this.x;
        lastY = this.y;

        if (escapeTicks > 0) {
            escapeTicks--;
            double ex = Math.max(22, Math.min(878, this.x + Math.cos(escapeAngle) * speed));
            double ey = Math.max(52, Math.min(495, this.y + Math.sin(escapeAngle) * speed));
            if (sensor == null || !sensor.isBlocked(ex, ey)) {
                this.facingDx = Math.cos(escapeAngle);
                this.facingDy = Math.sin(escapeAngle);
                this.x = ex;
                this.y = ey;
                this.moving = true;
                return false;
            }
            escapeTicks = 0;
        }

        double waypointX = tx;
        double waypointY = ty;

        if (sensor != null && isLineBlocked(this.x, this.y, tx, ty, sensor)) {
            double[] bfsNext = computeNextBfsWaypoint(tx, ty, sensor);
            if (bfsNext != null) {
                waypointX = bfsNext[0];
                waypointY = bfsNext[1];
            }
        }

        double baseAngle = Math.atan2(waypointY - this.y, waypointX - this.x);
        double[] slideAngles = {
                0, 0.35, -0.35, 0.70, -0.70, 1.05, -1.05,
                1.40, -1.40, 1.75, -1.75, 2.15, -2.15
        };

        for (double offset : slideAngles) {
            double ang = baseAngle + offset;
            double vx = Math.cos(ang);
            double vy = Math.sin(ang);
            double nextX = Math.max(22, Math.min(878, this.x + vx * speed));
            double nextY = Math.max(52, Math.min(495, this.y + vy * speed));
            if (sensor == null || !sensor.isBlocked(nextX, nextY)) {
                this.facingDx = this.facingDx * 0.65 + vx * 0.35;
                this.facingDy = this.facingDy * 0.65 + vy * 0.35;
                this.x = nextX;
                this.y = nextY;
                this.moving = true;
                return false;
            }
        }

        return false;
    }

    private double findMostOpenDirection(CollisionSensor sensor) {
        double bestAngle = 0;
        double bestScore = -1;
        for (int a = 0; a < 16; a++) {
            double ang = a * (Math.PI / 8.0);
            double score = 0;
            for (int dist = 12; dist <= 72; dist += 12) {
                double tx = Math.max(22, Math.min(878, this.x + Math.cos(ang) * dist));
                double ty = Math.max(52, Math.min(495, this.y + Math.sin(ang) * dist));
                if (!sensor.isBlocked(tx, ty)) {
                    score += dist;
                } else {
                    break;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestAngle = ang;
            }
        }
        return bestAngle;
    }

    private boolean isLineBlocked(double x1, double y1, double x2, double y2, CollisionSensor sensor) {
        double dist = Math.hypot(x2 - x1, y2 - y1);
        int steps = Math.max(2, (int) (dist / 12.0));
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double sx = x1 + (x2 - x1) * t;
            double sy = y1 + (y2 - y1) * t;
            if (sensor.isBlocked(sx, sy)) return true;
        }
        return false;
    }

    private double[] computeNextBfsWaypoint(double targetX, double targetY, CollisionSensor sensor) {
        int startCol = Math.max(0, Math.min(GRID_COLS - 1, (int) (this.x / CELL_SIZE)));
        int startRow = Math.max(2, Math.min(GRID_ROWS - 1, (int) (this.y / CELL_SIZE)));
        int goalCol  = Math.max(0, Math.min(GRID_COLS - 1, (int) (targetX / CELL_SIZE)));
        int goalRow  = Math.max(2, Math.min(GRID_ROWS - 1, (int) (targetY / CELL_SIZE)));

        if (startCol == goalCol && startRow == goalRow) return null;

        boolean[][] visited = new boolean[GRID_ROWS][GRID_COLS];
        int[][] parentCol = new int[GRID_ROWS][GRID_COLS];
        int[][] parentRow = new int[GRID_ROWS][GRID_COLS];

        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{ startRow, startCol });
        visited[startRow][startCol] = true;

        int[] dRow = { -1, 1, 0, 0 };
        int[] dCol = { 0, 0, -1, 1 };

        int bestRow = startRow;
        int bestCol = startCol;
        double bestGoalDist = Double.MAX_VALUE;
        boolean reachedGoal = false;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0];
            int c = curr[1];

            if (r == goalRow && c == goalCol) {
                bestRow = r;
                bestCol = c;
                reachedGoal = true;
                break;
            }

            double dToGoal = Math.hypot(c - goalCol, r - goalRow);
            if (!(r == startRow && c == startCol) && dToGoal < bestGoalDist) {
                bestGoalDist = dToGoal;
                bestRow = r;
                bestCol = c;
            }

            for (int dir = 0; dir < 4; dir++) {
                int nr = r + dRow[dir];
                int nc = c + dCol[dir];
                if (nr < 2 || nr >= GRID_ROWS || nc < 0 || nc >= GRID_COLS) continue;
                if (visited[nr][nc]) continue;

                double worldX = nc * CELL_SIZE + (CELL_SIZE / 2.0);
                double worldY = nr * CELL_SIZE + (CELL_SIZE / 2.0);
                if (sensor.isBlocked(worldX, worldY)) continue;

                visited[nr][nc] = true;
                parentRow[nr][nc] = r;
                parentCol[nr][nc] = c;
                queue.add(new int[]{ nr, nc });
            }
        }

        if (!reachedGoal && bestRow == startRow && bestCol == startCol) return null;

        int traceR = bestRow;
        int traceC = bestCol;
        int step2R = traceR, step2C = traceC;

        while (!(traceR == startRow && traceC == startCol)) {
            int pr = parentRow[traceR][traceC];
            int pc = parentCol[traceR][traceC];
            if (pr == startRow && pc == startCol) {
                break;
            }
            step2R = traceR;
            step2C = traceC;
            traceR = pr;
            traceC = pc;
        }

        return new double[]{
                step2C * CELL_SIZE + (CELL_SIZE / 2.0),
                step2R * CELL_SIZE + (CELL_SIZE / 2.0)
        };
    }

    private WorkStation findEmptyStation(List<WorkStation> stations, StationType type) {
        for (WorkStation ws : stations) {
            if (ws.getType() == type && ws.isEmpty() && !ws.isBroken()) return ws;
        }
        return null;
    }

    private void syncHeldPosition() {
        if (heldItem != null) {
            heldItem.setPosition(this.x + (facingDx * 18) - 22, this.y + (facingDy * 14) - 24);
        }
    }

    private void nudgeOutOfObstacle(CollisionSensor sensor) {
        for (int r = 12; r <= 140; r += 12) {
            for (int a = 0; a < 8; a++) {
                double ang = a * (Math.PI / 4.0);
                double nx = Math.max(25, Math.min(875, this.x + Math.cos(ang) * r));
                double ny = Math.max(55, Math.min(490, this.y + Math.sin(ang) * r));
                if (!sensor.isBlocked(nx, ny)) {
                    this.x = nx;
                    this.y = ny;
                    return;
                }
            }
        }
    }

    public void reset() {
        this.x = homeX;
        this.y = homeY;
        this.heldItem = null;
        this.moving = false;
        this.stuckTicks = 0;
        this.escapeTicks = 0;
    }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(0, Math.min(3, level)); }
    public void upgrade() { if (level < 3) level++; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getFacingDx() { return facingDx; }
    public double getFacingDy() { return facingDy; }
    public boolean isMoving() { return moving; }
    public double getAnimTick() { return animTick; }
    public WasteItem getHeldItem() { return heldItem; }
}