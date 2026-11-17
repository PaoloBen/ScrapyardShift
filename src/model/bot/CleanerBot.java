package model.bot;

import model.contract.Movable;
import model.item.BinType;
import model.item.WasteItem;
import model.station.StorageBay;

import java.util.List;
import java.util.Map;

public class CleanerBot implements Movable {
    private double x;
    private double y;
    private final double homeX;
    private final double homeY;
    private WasteItem heldItem;
    private int level; // 0 = Not purchased, 1 = Mk.I, 2 = Mk.II, 3 = Mk.III
    private double animTick;

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
        this.animTick += 0.2;
    }

    public String updateAI(List<WasteItem> floorItems, List<WasteItem> beltItems, Map<BinType, StorageBay> bays) {
        if (level <= 0) return null;
        double speed = 1.6 + (level * 0.85);
        this.animTick += 0.25;

        // STATE 1: Carrying an item -> Deliver to its target StorageBay
        if (heldItem != null) {
            BinType targetBin = heldItem.getTargetBin();
            double targetX = 710;
            double targetY = getBayY(targetBin);

            if (moveTowards(targetX, targetY, speed)) {
                StorageBay bay = bays.get(targetBin);
                if (!bay.isFull()) {
                    try {
                        bay.accept(heldItem);
                        String sortedName = heldItem.getDisplayTag();
                        heldItem = null;
                        return sortedName; // Signals GameController to award bot points!
                    } catch (Exception ignored) {}
                }
            }
            if (heldItem != null) {
                heldItem.setPosition(this.x - 24, this.y - 28);
            }
            return null;
        }

        // STATE 2: Hands empty -> Prioritize items lying on the floor, then items on the belt
        WasteItem target = null;
        boolean fromFloor = false;

        for (WasteItem item : floorItems) {
            if (!item.isAirborne()) {
                target = item;
                fromFloor = true;
                break;
            }
        }

        if (target == null && !beltItems.isEmpty()) {
            // Target the item closest to the dangerous incinerator!
            target = beltItems.get(0);
            for (WasteItem item : beltItems) {
                if (item.getX() > target.getX()) target = item;
            }
        }

        if (target != null) {
            if (moveTowards(target.getX() + 20, target.getY() + 20, speed)) {
                if (fromFloor) {
                    floorItems.remove(target);
                } else {
                    beltItems.remove(target);
                }
                this.heldItem = target;
            }
        } else {
            // Return to charging pad when idle
            moveTowards(homeX, homeY, speed * 0.6);
        }
        return null;
    }

    private boolean moveTowards(double tx, double ty, double speed) {
        double dx = tx - this.x;
        double dy = ty - this.y;
        double dist = Math.hypot(dx, dy);
        if (dist <= speed + 4) {
            this.x = tx;
            this.y = ty;
            return true;
        }
        this.x += (dx / dist) * speed;
        this.y += (dy / dist) * speed;
        return false;
    }

    private double getBayY(BinType type) {
        return switch (type) {
            case BIODEGRADABLE -> 110;
            case RECYCLABLE -> 205;
            case RESIDUAL -> 300;
            case HAZARDOUS -> 395;
        };
    }

    public void reset() {
        this.x = homeX;
        this.y = homeY;
        this.heldItem = null;
    }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(0, Math.min(3, level)); }
    public void upgrade() { if (level < 3) level++; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getAnimTick() { return animTick; }
    public WasteItem getHeldItem() { return heldItem; }
}