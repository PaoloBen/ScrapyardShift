package model.station;

import model.contract.Renderable;
import model.item.WasteItem;

import java.awt.*;

public class WorkStation implements Renderable {
    private final StationType type;
    private final double x;
    private final double y;
    private WasteItem storedItem;
    private int progress;
    private static final int MAX_PROGRESS = 90; // ~1.5 seconds at 60 FPS

    public WorkStation(StationType type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.storedItem = null;
        this.progress = 0;
    }

    public boolean updateTick() {
        if (storedItem != null && progress < MAX_PROGRESS) {
            progress++;
            if (progress >= MAX_PROGRESS) {
                storedItem.applyStation(this.type);
                return true; // Signals completion on this exact tick
            }
        }
        return false;
    }

    public void boostProcess() {
        if (storedItem != null && progress < MAX_PROGRESS) {
            progress = Math.min(MAX_PROGRESS, progress + 18);
            if (progress >= MAX_PROGRESS) {
                storedItem.applyStation(this.type);
            }
        }
    }

    public boolean isNear(double px, double py) {
        return Math.hypot((x + 55) - px, (y + 35) - py) <= 75;
    }

    public boolean isEmpty() {
        return storedItem == null;
    }

    public boolean isFinished() {
        return storedItem != null && progress >= MAX_PROGRESS;
    }

    public boolean placeItem(WasteItem item) {
        if (storedItem != null) return false;
        this.storedItem = item;
        this.storedItem.setPosition(this.x + 7, this.y + 18);
        this.progress = 0;
        return true;
    }

    public WasteItem removeItem() {
        WasteItem item = this.storedItem;
        this.storedItem = null;
        this.progress = 0;
        return item;
    }

    public void clear() {
        this.storedItem = null;
        this.progress = 0;
    }

    @Override
    public void draw(Graphics2D g2d) {
        int sx = (int) x;
        int sy = (int) y;
        int shake = (storedItem != null && !isFinished()) ? (progress % 4 < 2 ? -1 : 1) : 0;

        // Drop shadow
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillRoundRect(sx + 4, sy + 6, 110, 70, 10, 10);

        // Machine Chassis
        g2d.setColor(new Color(44, 62, 80));
        g2d.fillRoundRect(sx + shake, sy, 110, 70, 10, 10);

        // Color-coded Header Strip
        Color accent = switch (type) {
            case WASHER -> new Color(52, 152, 219);
            case DISMANTLER -> new Color(230, 126, 34);
            case NEUTRALIZER -> new Color(46, 204, 113);
        };
        g2d.setColor(accent);
        g2d.fillRoundRect(sx + shake, sy, 110, 20, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2d.drawString(type.name(), sx + 12 + shake, sy + 14);

        // Border
        g2d.setColor(isFinished() ? Color.GREEN : Color.LIGHT_GRAY);
        g2d.drawRoundRect(sx + shake, sy, 110, 70, 10, 10);

        // Stored Item & Progress Bar
        if (storedItem != null) {
            storedItem.setPosition(sx + 7 + shake, sy + 24);
            storedItem.draw(g2d);

            // Floating Progress Bar above station
            g2d.setColor(Color.BLACK);
            g2d.fillRect(sx + 10, sy - 14, 90, 8);
            g2d.setColor(isFinished() ? new Color(46, 204, 113) : new Color(241, 196, 15));
            int fillW = (int) (90 * ((double) progress / MAX_PROGRESS));
            g2d.fillRect(sx + 10, sy - 14, fillW, 8);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(sx + 10, sy - 14, 90, 8);
        } else {
            g2d.setColor(new Color(127, 140, 141));
            g2d.setFont(new Font("SansSerif", Font.ITALIC, 10));
            g2d.drawString("[SPACE] Drop Item", sx + 10, sy + 46);
        }
    }

    public StationType getType() { return type; }
    public double getX() { return x; }
    public double getY() { return y; }
}