package ui.screen;

import game.GameController;
import model.item.BinType;
import model.item.WasteItem;
import model.level.FacilityLevel;
import model.player.OperatorCosmetics;
import model.player.Player;
import model.station.StorageBay;
import model.station.WorkStation;
import ui.fx.FloatingText;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;

public class FactoryPanel extends JPanel {
    private final GameController controller;
    private boolean up, down, left, right;
    private int beltAnimOffset = 0;

    public FactoryPanel(GameController controller) {
        this.controller = controller;
        setPreferredSize(new Dimension(900, 520));
        setBackground(new Color(30, 39, 46));
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();

                // Locker Room Customization Controls
                if (controller.isInLockerRoom()) {
                    OperatorCosmetics cos = controller.getPlayer().getCosmetics();
                    switch (code) {
                        case KeyEvent.VK_1 -> cos.cycleHardhat();
                        case KeyEvent.VK_2 -> cos.cycleSuit();
                        case KeyEvent.VK_3 -> cos.cycleSkin();
                        case KeyEvent.VK_4 -> cos.cycleVisor();
                        case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> controller.setInLockerRoom(false);
                    }
                    return;
                }

                // Live Factory Controls
                switch (code) {
                    case KeyEvent.VK_W, KeyEvent.VK_UP -> up = true;
                    case KeyEvent.VK_S, KeyEvent.VK_DOWN -> down = true;
                    case KeyEvent.VK_A, KeyEvent.VK_LEFT -> left = true;
                    case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> right = true;
                    case KeyEvent.VK_SHIFT -> controller.getPlayer().triggerDash();
                    case KeyEvent.VK_SPACE, KeyEvent.VK_E -> controller.interact();
                    case KeyEvent.VK_F -> controller.actionKeyF();
                    case KeyEvent.VK_C -> controller.setInLockerRoom(true);
                    case KeyEvent.VK_R -> controller.resetGame();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W, KeyEvent.VK_UP -> up = false;
                    case KeyEvent.VK_S, KeyEvent.VK_DOWN -> down = false;
                    case KeyEvent.VK_A, KeyEvent.VK_LEFT -> left = false;
                    case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> right = false;
                }
            }
        });

        Timer timer = new Timer(16, e -> {
            double dx = (right ? 1 : 0) - (left ? 1 : 0);
            double dy = (down ? 1 : 0) - (up ? 1 : 0);
            controller.updateTick(dx, dy);
            beltAnimOffset = (beltAnimOffset + 2) % 24;
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (controller.isInLockerRoom()) {
            drawLockerRoom(g2d);
            return;
        }

        // Apply screen shake if active
        AffineTransform oldTransform = g2d.getTransform();
        if (controller.getScreenShake() > 0) {
            int sx = (int) ((Math.random() - 0.5) * 8);
            int sy = (int) ((Math.random() - 0.5) * 8);
            g2d.translate(sx, sy);
        }

        drawTiledFloor(g2d);
        drawTopHud(g2d);
        drawConveyorAndIncinerator(g2d);
        drawWorkStationsAndForklift(g2d);
        drawBays(g2d);

        // Draw Player Avatar & Floating Popups
        controller.getPlayer().draw(g2d);
        for (FloatingText popup : controller.getPopups()) {
            popup.draw(g2d);
        }

        // Draw Thrown & Floor Items
        for (WasteItem item : controller.getThrownItems()) {
            item.draw(g2d);
        }

        // Draw Throw Aim Indicator when holding an item
        Player p = controller.getPlayer();
        if (p.getHeldItem() != null) {
            g2d.setColor(new Color(255, 255, 255, 110));
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6, 6}, 0));
            int aimX = (int) (p.getX() + p.getThrowDx() * 95);
            int aimY = (int) (p.getY() + p.getThrowDy() * 95);
            g2d.drawLine((int) p.getX(), (int) p.getY(), aimX, aimY);
            g2d.fillOval(aimX - 4, aimY - 4, 8, 8);
            g2d.setStroke(new BasicStroke(1));
        }

        g2d.setTransform(oldTransform);
        drawFooterControls(g2d);

        if (controller.isGameOver() || controller.isGameWon()) {
            drawDebriefOverlay(g2d);
        }
    }

    private void drawLockerRoom(Graphics2D g2d) {
        g2d.setColor(new Color(24, 32, 42));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Header Banner
        g2d.setColor(new Color(15, 188, 249));
        g2d.fillRect(0, 0, getWidth(), 65);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2d.drawString("SCRAPYARD SHIFT — OPERATOR LOCKER ROOM", 180, 42);

        // Left Card: Scaled Avatar Preview Pedestal
        g2d.setColor(new Color(45, 52, 54));
        g2d.fillRoundRect(110, 110, 260, 310, 16, 16);
        g2d.setColor(Color.CYAN);
        g2d.drawRoundRect(110, 110, 260, 310, 16, 16);

        Player p = controller.getPlayer();
        double oldX = p.getX(), oldY = p.getY();
        AffineTransform saved = g2d.getTransform();
        p.resetPosition(0, 0);
        g2d.translate(240, 265);
        g2d.scale(2.8, 2.8);
        p.draw(g2d);
        g2d.setTransform(saved);
        p.resetPosition(oldX, oldY);

        // Right Card: Customization Options
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2d.drawString("CUSTOMIZE MRF OPERATOR", 430, 145);

        g2d.setFont(new Font("Monospaced", Font.BOLD, 15));
        g2d.setColor(new Color(241, 196, 15));
        g2d.drawString("[1] Cycle Hardhat Color", 430, 195);
        g2d.drawString("[2] Cycle Hazmat Suit", 430, 235);
        g2d.drawString("[3] Cycle Skin Tone", 430, 275);
        g2d.drawString("[4] Visor: " + p.getCosmetics().getVisorStyle(), 430, 315);

        g2d.setColor(new Color(189, 195, 199));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g2d.drawString("• Move with [WASD] + [SHIFT] to Dash across the factory floor.", 430, 365);
        g2d.drawString("• Press [SPACE]/[E] to Grab items off the belt, use Stations, or Drop in Bays.", 430, 388);
        g2d.drawString("• Press [F] to Boost a running Station or Throw an item into a Bay!", 430, 411);

        g2d.setColor(new Color(46, 204, 113));
        g2d.fillRoundRect(290, 445, 320, 45, 12, 12);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2d.drawString("PRESS [ENTER] TO START SHIFT", 312, 474);
    }

    private void drawTiledFloor(Graphics2D g2d) {
        int tileSize = 45;
        for (int x = 0; x < getWidth(); x += tileSize) {
            for (int y = 55; y < getHeight() - 35; y += tileSize) {
                boolean alt = ((x / tileSize) + (y / tileSize)) % 2 == 0;
                g2d.setColor(alt ? new Color(38, 50, 56) : new Color(45, 58, 66));
                g2d.fillRect(x, y, tileSize, tileSize);
            }
        }
    }

    private void drawTopHud(Graphics2D g2d) {
        g2d.setColor(new Color(15, 188, 249));
        g2d.fillRect(0, 0, getWidth(), 55);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));

        Player p = controller.getPlayer();
        FacilityLevel lvl = controller.getCurrentLevel();
        g2d.drawString(lvl.getTitle() + " (Goal: " + lvl.getTargetScore() + ")", 20, 23);
        g2d.drawString("Score: " + p.getScore() + "  |  Streak: x" + p.getStreakMeter().getValue(), 415, 23);
        g2d.drawString("Compliance: " + p.getComplianceMeter().getValue() + "%", 715, 23);

        g2d.setColor(new Color(245, 246, 250));
        g2d.fillRoundRect(20, 31, 860, 20, 6, 6);
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.drawString(controller.getLastFeedback(), 30, 45);
    }

    private void drawConveyorAndIncinerator(Graphics2D g2d) {
        int bx = GameController.BELT_START_X;
        int by = GameController.LANE_Y - 8;
        int bw = GameController.BELT_END_X - bx;

        // Conveyor Housing
        g2d.setColor(new Color(87, 101, 116));
        g2d.fillRoundRect(bx - 10, by - 6, bw + 20, 68, 10, 10);
        g2d.setColor(new Color(47, 54, 64));
        g2d.fillRect(bx, by, bw, 56);

        // Animated Rollers
        g2d.setColor(new Color(113, 128, 147));
        for (int rx = bx + beltAnimOffset; rx < GameController.BELT_END_X; rx += 24) {
            g2d.drawLine(rx, by + 2, rx, by + 54);
        }

        // Danger Incinerator Pit at the right end of the belt
        g2d.setColor(new Color(192, 57, 43));
        g2d.fillRoundRect(GameController.BELT_END_X, by - 6, 85, 68, 10, 10);
        g2d.setColor(new Color(241, 196, 15));
        g2d.drawRoundRect(GameController.BELT_END_X, by - 6, 85, 68, 10, 10);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2d.drawString("INCINERATOR", GameController.BELT_END_X + 7, by + 30);

        for (WasteItem item : controller.getBeltItems()) {
            item.draw(g2d);
        }
    }

    private void drawWorkStationsAndForklift(Graphics2D g2d) {
        for (WorkStation station : controller.getStations()) {
            station.draw(g2d);
        }

        // Draw Forklift Dispatch Lever Station
        int fx = GameController.FORKLIFT_X;
        int fy = GameController.FORKLIFT_Y;
        g2d.setColor(new Color(243, 156, 18));
        g2d.fillRoundRect(fx, fy, 105, 70, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(fx, fy, 105, 70, 10, 10);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2d.drawString("FORKLIFT BAY", fx + 12, fy + 22);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2d.drawString("[SPACE] Empty", fx + 14, fy + 45);
    }

    private void drawBays(Graphics2D g2d) {
        BinType highlighted = controller.getClosestBayType(controller.getPlayer().getY());
        int bayY = 80;
        for (StorageBay bay : controller.getBays()) {
            bay.draw(g2d);
            if (controller.getPlayer().getX() >= 520 && bay.getType() == highlighted) {
                g2d.setColor(Color.GREEN);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRect(728, bayY - 2, 124, 64);
                g2d.setStroke(new BasicStroke(1));
            }
            bayY += 95;
        }
    }

    private void drawFooterControls(Graphics2D g2d) {
        g2d.setColor(new Color(20, 25, 30));
        g2d.fillRect(0, 478, getWidth(), 42);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 11));
        g2d.drawString("[WASD] Move  [SHIFT] Dash  [SPACE/E] Grab / Place / Drop  [F] Boost Station / Throw  [C] Locker Room  [R] Restart", 18, 502);
    }

    private void drawDebriefOverlay(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 210));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
        String header = controller.isGameWon() ? "ALL SHIFTS COMPLETED!" : "SHIFT INCIDENT DEBRIEF";
        g2d.drawString(header, 280, 100);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
        int y = 150;
        g2d.drawString("Mistakes logged during shift:", 150, y);
        for (int i = 0; i < Math.min(8, controller.getMistakeLog().size()); i++) {
            y += 28;
            g2d.setColor(new Color(255, 121, 121));
            g2d.drawString("• " + controller.getMistakeLog().get(i), 160, y);
        }
        g2d.setColor(Color.YELLOW);
        g2d.drawString("Press [R] to Start New Shift", 350, 440);
    }
}