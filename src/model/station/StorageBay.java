package model.station;

import model.contract.Renderable;
import model.exception.BinFullException;
import model.item.BinType;
import model.item.WasteItem;

import java.awt.Color;
import java.awt.Graphics2D;

public class StorageBay implements Renderable {
    private final BinType type;
    private final int baseCapacity;
    private int capacity;
    private int fillLevel;
    private int x, y;
    private boolean beingHauled = false;

    public boolean isBeingHauled() { return beingHauled; }
    public void setBeingHauled(boolean hauled) { this.beingHauled = hauled; }

    public void setPosition(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }

    public StorageBay(BinType type, int capacity, int x, int y) {
        this.type = type;
        this.baseCapacity = capacity;
        this.capacity = capacity;
        this.fillLevel = 0;
        this.x = x;
        this.y = y;
    }

    public void accept(WasteItem item) throws BinFullException {
        if (isFull()) {
            throw new BinFullException(this.type);
        }
        this.fillLevel++;
    }

    public void empty() {
        this.fillLevel = 0;
    }

    public boolean isFull() {
        return fillLevel >= capacity;
    }

    public BinType getType() { return type; }
    public int getFillLevel() { return fillLevel; }
    public int getCapacity() { return capacity; }
    public void increaseCapacity(int extra) {
        this.capacity += extra;
    }

    public void resetCapacity() {
        this.capacity = this.baseCapacity;
        this.fillLevel = 0;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(x, y, 120, 60);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(x, y, 120, 60);
        g2d.drawString(type.name(), x + 10, y + 25);
        g2d.drawString("Fill: " + fillLevel + "/" + capacity, x + 10, y + 45);
    }
}