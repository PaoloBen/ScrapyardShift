package model;

import java.awt.Color;
import java.awt.Graphics2D;

public abstract class WasteItem implements Movable, Processable, Renderable {
    private final String name;
    private double x;
    private double y;
    private int lane;
    private boolean preprocessed;

    protected WasteItem(String name, double startX, double startY, int lane) {
        this.name = name;
        this.x = startX;
        this.y = startY;
        this.lane = lane;
        this.preprocessed = false;
    }

    // Abstract contracts every subclass MUST implement
    public abstract BinType getTargetBin();
    public abstract String getExplanation();
    public abstract Color getRenderColor();

    // Default penalty (Hazardous items will override this to return 20)
    public int getPenalty() {
        return 10;
    }

    // Default station behavior (Stateful items will override this)
    @Override
    public void applyStation(StationType station) {
        // Default: no effect on basic items
    }

    @Override
    public void move(double speed) {
        this.x += speed;
    }

    @Override
    public void draw(Graphics2D g2d) {
        int boxWidth = 96;
        int boxHeight = 40;
        g2d.setColor(getRenderColor());
        g2d.fillRoundRect((int) x, (int) y, boxWidth, boxHeight, 8, 8);
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect((int) x, (int) y, boxWidth, boxHeight, 8, 8);
        g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11));
        g2d.drawString(getDisplayTag(), (int) x + 6, (int) y + 24);
    }

    // Subclasses can override to show status like "[Dirty]" or "[Clean]"
    public String getDisplayTag() {
        return name;
    }

    // Strictly encapsulated getters and setters
    public String getName() { return name; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getLane() { return lane; }
    public void setLane(int lane, double newY) {
        this.lane = lane;
        this.y = newY;
    }
    public boolean isPreprocessed() { return preprocessed; }
    protected void setPreprocessed(boolean preprocessed) { this.preprocessed = preprocessed; }

    @Override
    public String toString() {
        return String.format("%s [Lane %d, X=%.1f, Target=%s]",
                getDisplayTag(), lane, x, getTargetBin().name());
    }
}