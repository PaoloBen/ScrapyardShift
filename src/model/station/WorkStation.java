package model.station;

import model.contract.Renderable;
import model.item.WasteItem;

import java.awt.Color;
import java.awt.Graphics2D;

public class WorkStation implements Renderable {
    private final StationType type;
    private final double x;
    private final int lane;

    public WorkStation(StationType type, double x, int lane) {
        this.type = type;
        this.x = x;
        this.lane = lane;
    }

    public void process(WasteItem item) {
        // Check if item is within the station's active zone on the same lane
        if (item.getLane() == this.lane && Math.abs(item.getX() - this.x) <= 35) {
            item.applyStation(this.type);
        }
    }

    public StationType getType() { return type; }
    public double getX() { return x; }
    public int getLane() { return lane; }

    @Override
    public void draw(Graphics2D g2d) {
        int drawY = 120 + (lane * 100);
        g2d.setColor(new Color(52, 73, 94));
        g2d.fillRect((int) x, drawY - 10, 70, 60);
        g2d.setColor(Color.CYAN);
        g2d.drawRect((int) x, drawY - 10, 70, 60);
        g2d.drawString(type.name(), (int) x + 5, drawY - 15);
    }
}