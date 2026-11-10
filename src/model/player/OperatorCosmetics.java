package model.player;

import java.awt.Color;

public class OperatorCosmetics {
    public static final Color[] HARDHAT_COLORS = {
            new Color(241, 196, 15), // Classic Safety Yellow
            new Color(231, 76, 60),  // Fire Chief Red
            new Color(52, 152, 219), // Engineer Blue
            new Color(46, 204, 113), // Eco Green
            new Color(155, 89, 182), // Cyber Purple
            new Color(236, 240, 241)  // Inspector White
    };

    public static final Color[] SUIT_COLORS = {
            new Color(230, 126, 34), // Hi-Vis Orange
            new Color(52, 73, 94),   // Navy Mechanics Jumpsuit
            new Color(39, 174, 96),  // Biohazard Green
            new Color(192, 57, 43),  // Heavy Crimson
            new Color(41, 128, 185)  // Cobalt Tech
    };

    public static final Color[] SKIN_TONES = {
            new Color(255, 220, 185),
            new Color(224, 172, 105),
            new Color(198, 134, 66),
            new Color(141, 85, 36)
    };

    public static final String[] VISOR_STYLES = {
            "Standard Goggles",
            "Cyber Visor",
            "Dark Shades",
            "None"
    };

    private int hardhatIndex = 0;
    private int suitIndex = 0;
    private int skinIndex = 1;
    private int visorIndex = 0;

    public void cycleHardhat() { hardhatIndex = (hardhatIndex + 1) % HARDHAT_COLORS.length; }
    public void cycleSuit() { suitIndex = (suitIndex + 1) % SUIT_COLORS.length; }
    public void cycleSkin() { skinIndex = (skinIndex + 1) % SKIN_TONES.length; }
    public void cycleVisor() { visorIndex = (visorIndex + 1) % VISOR_STYLES.length; }

    public Color getHardhatColor() { return HARDHAT_COLORS[hardhatIndex]; }
    public Color getSuitColor() { return SUIT_COLORS[suitIndex]; }
    public Color getSkinColor() { return SKIN_TONES[skinIndex]; }
    public String getVisorStyle() { return VISOR_STYLES[visorIndex]; }
    public int getVisorIndex() { return visorIndex; }
}