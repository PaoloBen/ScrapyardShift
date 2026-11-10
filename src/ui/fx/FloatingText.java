package ui.fx;

import java.awt.*;

public class FloatingText {
    private final String text;
    private double x;
    private double y;
    private final Color color;
    private int life;
    private static final int MAX_LIFE = 45;

    public FloatingText(String text, double x, double y, Color color) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.life = MAX_LIFE;
    }

    public boolean update() {
        this.y -= 0.8;
        this.life--;
        return life <= 0;
    }

    public void draw(Graphics2D g2d) {
        int alpha = (int) (255 * ((double) life / MAX_LIFE));
        alpha = Math.max(0, Math.min(255, alpha));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 13));

        g2d.setColor(new Color(0, 0, 0, alpha));
        g2d.drawString(text, (int) x + 1, (int) y + 1);

        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g2d.drawString(text, (int) x, (int) y);
    }
}