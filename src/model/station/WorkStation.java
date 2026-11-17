package model.station;

import model.contract.Renderable;
import model.item.WasteItem;

import java.awt.*;

public class WorkStation implements Renderable {
    private final StationType type;
    private double x;
    private double y;
    private WasteItem storedItem;
    private double progress;
    private int health = 100;
    private boolean wrongItemProcessing = false;
    private boolean justBrokeFromWrongItem = false;
    private static final double MAX_PROGRESS = 90.0;

    public WorkStation(StationType type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.storedItem = null;
        this.progress = 0;
        this.health = 100;
    }

    public void setPosition(double newX, double newY) {
        this.x = newX;
        this.y = newY;
        if (this.storedItem != null) {
            this.storedItem.setPosition(this.x + 19, this.y + 16);
        }
    }

    public boolean updateTick() {
        if (health <= 0) return false;
        if (storedItem != null && progress < MAX_PROGRESS) {
            double healthFactor = Math.max(0.25, health / 100.0);
            progress += healthFactor;
            if (progress >= MAX_PROGRESS) {
                progress = MAX_PROGRESS;
                if (wrongItemProcessing) {
                    damageMachine(50);
                    justBrokeFromWrongItem = true;
                } else {
                    storedItem.applyStation(this.type);
                }
                return true;
            }
        }
        return false;
    }

    public void boostProcess() {
        if (health <= 0 || wrongItemProcessing) return;
        if (storedItem != null && progress < MAX_PROGRESS) {
            double healthFactor = Math.max(0.25, health / 100.0);
            progress = Math.min(MAX_PROGRESS, progress + (18.0 * healthFactor));
            if (progress >= MAX_PROGRESS) {
                storedItem.applyStation(this.type);
            }
        }
    }

    public void damageMachine(int amount) {
        this.health = Math.max(0, this.health - amount);
    }

    public void repairFull() {
        this.health = 100;
        this.wrongItemProcessing = false;
        this.justBrokeFromWrongItem = false;
    }

    public int getHealth() { return health; }
    public boolean isBroken() { return health <= 0; }
    public boolean isWrongItemProcessing() { return wrongItemProcessing; }

    public boolean consumeWrongItemFinished() {
        if (justBrokeFromWrongItem) {
            justBrokeFromWrongItem = false;
            return true;
        }
        return false;
    }

    public boolean isNear(double px, double py) {
        return Math.hypot((x + 42) - px, (y + 36) - py) <= 84;
    }

    public boolean isEmpty() {
        return storedItem == null;
    }

    public boolean isFinished() {
        return storedItem != null && progress >= MAX_PROGRESS;
    }

    public boolean placeItem(WasteItem item) {
        if (storedItem != null || isBroken()) return false;
        this.storedItem = item;
        this.storedItem.setPosition(this.x + 19, this.y + 16);
        this.progress = 0;
        this.wrongItemProcessing = false;
        this.justBrokeFromWrongItem = false;
        return true;
    }

    public boolean placeWrongItem(WasteItem item) {
        if (storedItem != null || isBroken()) return false;
        this.storedItem = item;
        this.storedItem.setPosition(this.x + 19, this.y + 16);
        this.progress = 0;
        this.wrongItemProcessing = true;
        this.justBrokeFromWrongItem = false;
        return true;
    }

    public WasteItem removeItem() {
        WasteItem item = this.storedItem;
        this.storedItem = null;
        this.progress = 0;
        this.wrongItemProcessing = false;
        this.justBrokeFromWrongItem = false;
        return item;
    }

    public void clear() {
        this.storedItem = null;
        this.progress = 0;
        this.wrongItemProcessing = false;
        this.justBrokeFromWrongItem = false;
    }

    @Override
    public void draw(Graphics2D g2d) {}

    public StationType getType() { return type; }
    public double getX() { return x; }
    public double getY() { return y; }
    public WasteItem getStoredItem() { return storedItem; }
    public float getProgressRatio() { return (float) (progress / MAX_PROGRESS); }
}