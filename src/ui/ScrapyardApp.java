package ui;

import game.GameController;
import javax.swing.*;

public class ScrapyardApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Scrapyard Shift - MRF Operations (CIS 2103)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            GameController controller = new GameController("Operator Paul");
            FactoryPanel panel = new FactoryPanel(controller);

            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}