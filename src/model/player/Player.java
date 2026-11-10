package model.player;

import model.contract.Renderable;
import model.item.WasteItem;
import model.meter.ComplianceMeter;
import model.meter.StreakMeter;

import java.awt.*;

public class Player implements Renderable {
    private String name;
    private int score;
    private final ComplianceMeter complianceMeter;
    private final StreakMeter streakMeter;
    private final OperatorCosmetics cosmetics;

    // 2D Top-Down Avatar State
    private double x;
    private double y;
    private Direction facing;
    private WasteItem heldItem;
    private int dashTimer;
    private int dashCooldown;
    private double walkCycle;
    private double throwDx = 0;
    private double throwDy = 1;

    public Player(String name) {
        this.name = name;
        this.score = 0;
        this.complianceMeter = new ComplianceMeter();
        this.streakMeter = new StreakMeter();
        this.cosmetics = new OperatorCosmetics();
        resetPosition(420, 280);
    }

    public void resetPosition(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.facing = Direction.DOWN; // Face camera in Locker Room!
        this.throwDx = 0;
        this.throwDy = 1;
        this.heldItem = null;
        this.dashTimer = 0;
        this.dashCooldown = 0;
        this.walkCycle = 0;
    }

    public void move(double dx, double dy, int minX, int maxX, int minY, int maxY) {
        if (dashCooldown > 0) dashCooldown--;
        if (dashTimer > 0) dashTimer--;

        if (dx == 0 && dy == 0) {
            walkCycle = 0;
            return;
        }

        if (Math.abs(dx) >= Math.abs(dy)) {
            facing = dx > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            facing = dy > 0 ? Direction.DOWN : Direction.UP;
        }

        double length = Math.hypot(dx, dy);
        this.throwDx = dx / length;
        this.throwDy = dy / length;

        double speed = (dashTimer > 0) ? 8.5 : 4.2;
        double nextX = Math.max(minX, Math.min(maxX, this.x + this.throwDx * speed));
        double nextY = Math.max(minY, Math.min(maxY, this.y + this.throwDy * speed));

        // Solid collision around the top Conveyor Belt & Incinerator (x < 655, y < 195),
        // leaving the right-side corridor (x >= 655) open up to y = 95 for BIODEGRADABLE!
        if (nextX < 655 && nextY < 195) {
            if (this.x >= 655) {
                nextX = 655;
            } else {
                nextY = 195;
            }
        }

        this.x = nextX;
        this.y = nextY;
        this.walkCycle += 0.35;

        syncHeldItemPosition();
    }

    public void triggerDash() {
        if (dashCooldown == 0) {
            dashTimer = 10;
            dashCooldown = 45;
        }
    }

    public void syncHeldItemPosition() {
        if (heldItem != null) {
            double offsetX = facing.getDx() * 34 - 48;
            double offsetY = (facing == Direction.UP) ? -52 : (facing.getDy() * 24 - 10);
            heldItem.setPosition(this.x + offsetX, this.y + offsetY);
        }
    }

    @Override
    public void draw(Graphics2D g2d) {
        int px = (int) x;
        int py = (int) y;
        int bob = (int) (Math.sin(walkCycle) * 3);

        // If facing UP, draw held item behind the avatar so it doesn't block the body
        if (heldItem != null && facing == Direction.UP) {
            heldItem.draw(g2d);
        }

        // Drop shadow
        g2d.setColor(new Color(0, 0, 0, 90));
        g2d.fillOval(px - 20, py + 12, 40, 14);

        // Dash aura
        if (dashTimer > 0) {
            g2d.setColor(new Color(0, 255, 255, 100));
            g2d.fillOval(px - 26, py - 26, 52, 52);
        }

        // Hazmat / Jumpsuit Shoulders & Torso
        g2d.setColor(cosmetics.getSuitColor());
        g2d.fillRoundRect(px - 18, py - 10 + bob, 36, 26, 12, 12);
        g2d.setColor(new Color(241, 196, 15));
        g2d.fillRect(px - 12, py - 10 + bob, 5, 26);
        g2d.fillRect(px + 7, py - 10 + bob, 5, 26);
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(px - 18, py - 10 + bob, 36, 26, 12, 12);

        // Hands
        g2d.setColor(cosmetics.getSkinColor());
        int handOffsetX = facing.getDx() * 14;
        int handOffsetY = facing.getDy() * 12;
        g2d.fillOval(px - 16 + handOffsetX, py - 4 + handOffsetY + bob, 10, 10);
        g2d.fillOval(px + 6 + handOffsetX, py - 4 + handOffsetY + bob, 10, 10);

        // Head
        g2d.setColor(cosmetics.getSkinColor());
        g2d.fillOval(px - 14, py - 24 + bob, 28, 26);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(px - 14, py - 24 + bob, 28, 26);

        // Hardhat Dome & Brim (drawn BEFORE visor and raised by 3px so it never blocks the visor)
        g2d.setColor(cosmetics.getHardhatColor());
        g2d.fillArc(px - 16, py - 31 + bob, 32, 22, 0, 180);
        g2d.fillRoundRect(px - 18, py - 22 + bob, 36, 5, 4, 4);
        g2d.setColor(Color.BLACK);
        g2d.drawArc(px - 16, py - 31 + bob, 32, 22, 0, 180);
        g2d.drawRoundRect(px - 18, py - 22 + bob, 36, 5, 4, 4);

        // Visor / Goggles (drawn on top of face when not facing away)
        if (facing != Direction.UP) {
            int eyeX = px + (facing.getDx() * 5);
            int eyeY = py - 12 + (facing.getDy() * 2) + bob;
            switch (cosmetics.getVisorIndex()) {
                case 0 -> {
                    g2d.setColor(new Color(52, 152, 219));
                    g2d.fillRoundRect(eyeX - 10, eyeY - 3, 20, 7, 4, 4);
                    g2d.setColor(Color.WHITE);
                    g2d.drawRoundRect(eyeX - 10, eyeY - 3, 20, 7, 4, 4);
                }
                case 1 -> {
                    g2d.setColor(new Color(0, 255, 200));
                    g2d.fillRect(eyeX - 11, eyeY - 3, 22, 5);
                }
                case 2 -> {
                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(eyeX - 10, eyeY - 3, 20, 6);
                }
                default -> {
                    g2d.setColor(Color.BLACK);
                    g2d.fillOval(eyeX - 6, eyeY - 2, 4, 4);
                    g2d.fillOval(eyeX + 2, eyeY - 2, 4, 4);
                }
            }
        }

        // Draw Held WasteItem in front if not facing UP
        if (heldItem != null && facing != Direction.UP) {
            heldItem.draw(g2d);
        }
    }

    public double getThrowDx() { return throwDx; }
    public double getThrowDy() { return throwDy; }

    // Score & Meter logic
    public void addScore(int basePoints) {
        this.score += basePoints + streakMeter.getBonus();
        this.streakMeter.change(1);
    }

    public void addScore(int basePoints, int preprocessBonus) {
        this.score += basePoints + preprocessBonus + streakMeter.getBonus();
        this.streakMeter.change(1);
    }

    public void applyPenalty(int penaltyAmount) {
        this.complianceMeter.change(-penaltyAmount);
        this.streakMeter.reset(0);
    }

    public void resetScore() { this.score = 0; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getScore() { return score; }
    public ComplianceMeter getComplianceMeter() { return complianceMeter; }
    public StreakMeter getStreakMeter() { return streakMeter; }
    public OperatorCosmetics getCosmetics() { return cosmetics; }

    public double getX() { return x; }
    public double getY() { return y; }
    public Direction getFacing() { return facing; }
    public boolean isDashing() { return dashTimer > 0; }
    public WasteItem getHeldItem() { return heldItem; }
    public void setHeldItem(WasteItem item) {
        this.heldItem = item;
        syncHeldItemPosition();
    }
}