package model.item;

import model.station.StationType;
import model.contract.Movable;
import model.contract.Processable;
import model.contract.Renderable;

import java.awt.Color;
import java.awt.Graphics2D;

public abstract class WasteItem implements Movable, Processable, Renderable {
    private final String name;
    private double x;
    private double y;
    private int lane;
    private boolean preprocessed;
    private double vx;
    private double vy;
    private int airTimer;
    private int spawnDropTimer = 16; // Drops from the overhead intake pipe onto the belt!

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

    // Polymorphic hook: returns which StationType this item needs before sorting (or null if ready)
    public StationType getRequiredStation() {
        return null;
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

    public void moveVector(double vx, double vy) {
        if (spawnDropTimer > 0) {
            spawnDropTimer--;
            return; // Wait until item finishes dropping out of the overhead pipe before rolling!
        }
        this.x += vx;
        this.y += vy;
    }

    public int getSpawnDropTimer() { return spawnDropTimer; }
    public void clearSpawnDrop() { this.spawnDropTimer = 0; }

    @Override
    public void draw(Graphics2D g2d) {
        int boxWidth = 96;
        int boxHeight = 40;
        int arcLift = isAirborne() ? (int) (Math.sin((airTimer / 26.0) * Math.PI) * 28) : 0;

        if (isAirborne()) {
            g2d.setColor(new Color(0, 0, 0, 85));
            g2d.fillRoundRect((int) x + 4, (int) y + 8, boxWidth, boxHeight, 8, 8);
        }

        g2d.setColor(getRenderColor());
        g2d.fillRoundRect((int) x, (int) y - arcLift, boxWidth, boxHeight, 8, 8);
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect((int) x, (int) y - arcLift, boxWidth, boxHeight, 8, 8);
        g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11));
        g2d.drawString(getDisplayTag(), (int) x + 6, (int) y + 24 - arcLift);
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

    public void setPosition(double newX, double newY) {
        this.x = newX;
        this.y = newY;
    }

    public void launch(double vx, double vy) {
        this.vx = vx;
        this.vy = vy;
        this.airTimer = 26; // Flies for 26 ticks (~0.45 sec)
    }

    public boolean updateProjectile() {
        if (airTimer > 0) {
            this.x += vx;
            this.y += vy;
            this.vx *= 0.94;
            this.vy *= 0.94;
            this.airTimer--;
            return airTimer == 0; // Returns true on the exact tick it lands
        }
        return false;
    }

    public boolean isAirborne() {
        return airTimer > 0;
    }

    private boolean personallyProcessed = false;

    public boolean isPreprocessed() { return preprocessed; }
    protected void setPreprocessed(boolean preprocessed) {
        this.preprocessed = preprocessed;
        if (preprocessed) {
            this.personallyProcessed = true;
        }
    }

    public boolean isPersonallyProcessed() { return personallyProcessed; }
    public void markPersonallyProcessed() { this.personallyProcessed = true; }

    @Override
    public String toString() {
        return String.format("%s [Lane %d, X=%.1f, Target=%s]",
                getDisplayTag(), lane, x, getTargetBin().name());
    }
}