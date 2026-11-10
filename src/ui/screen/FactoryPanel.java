package ui.screen;

import game.GameController;
import model.item.BinType;
import model.item.WasteItem;
import model.level.FacilityLevel;
import model.player.Player;
import model.station.StationType;
import model.station.StorageBay;
import model.station.WorkStation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class FactoryPanel extends JPanel {
    private final GameController controller;

    public FactoryPanel(GameController controller) {
        this.controller = controller;
        setPreferredSize(new Dimension(900, 520));
        setBackground(new Color(30, 39, 46));
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_Q -> controller.triggerStation(StationType.WASHER);
                    case KeyEvent.VK_W -> controller.triggerStation(StationType.DISMANTLER);
                    case KeyEvent.VK_E -> controller.triggerStation(StationType.NEUTRALIZER);
                    case KeyEvent.VK_1 -> controller.setActiveDiverter(BinType.BIODEGRADABLE);
                    case KeyEvent.VK_2 -> controller.setActiveDiverter(BinType.RECYCLABLE);
                    case KeyEvent.VK_3 -> controller.setActiveDiverter(BinType.RESIDUAL);
                    case KeyEvent.VK_4 -> controller.setActiveDiverter(BinType.HAZARDOUS);
                    case KeyEvent.VK_SPACE -> controller.dispatchForklift();
                    case KeyEvent.VK_R -> controller.resetGame();
                }
            }
        });

        // 60 FPS Game Loop (~16ms per tick)
        Timer timer = new Timer(16, e -> {
            controller.updateTick();
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawTopHud(g2d);
        drawConveyorAndStations(g2d);
        drawBaysAndDiverter(g2d);
        drawFooterControls(g2d);

        if (controller.isGameOver() || controller.isGameWon()) {
            drawDebriefOverlay(g2d);
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

        // Feedback Banner
        g2d.setColor(new Color(245, 246, 250));
        g2d.fillRoundRect(20, 31, 860, 20, 6, 6);
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.drawString(controller.getLastFeedback(), 30, 45);
    }

    private void drawConveyorAndStations(Graphics2D g2d) {
        // Draw Conveyor Belt Track
        g2d.setColor(new Color(72, 84, 96));
        g2d.fillRect(GameController.BELT_START_X, GameController.LANE_Y - 5,
                GameController.BELT_END_X - GameController.BELT_START_X, 50);
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawRect(GameController.BELT_START_X, GameController.LANE_Y - 5,
                GameController.BELT_END_X - GameController.BELT_START_X, 50);

        // Draw WorkStations
        for (WorkStation station : controller.getStations()) {
            station.draw(g2d);
        }

        // Draw Moving WasteItems
        for (WasteItem item : controller.getBeltItems()) {
            item.draw(g2d);
        }
    }

    private void drawBaysAndDiverter(Graphics2D g2d) {
        int bayY = 80;
        for (StorageBay bay : controller.getBays()) {
            bay.draw(g2d);
            if (bay.getType() == controller.getActiveDiverter()) {
                g2d.setColor(Color.GREEN);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine(GameController.BELT_END_X, GameController.LANE_Y + 20, 720, bayY + 30);
                g2d.drawRect(718, bayY - 2, 124, 64);
                g2d.setStroke(new BasicStroke(1));
            }
            bayY += 95;
        }
    }

    private void drawFooterControls(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 11));
        g2d.drawString("STATIONS: [Q] Wash [W] Dismantle [E] Seal  |  DIVERTER: [1] BIO [2] REC [3] RES [4] HAZ  |  [SPACE] Empty Bays", 20, 495);
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