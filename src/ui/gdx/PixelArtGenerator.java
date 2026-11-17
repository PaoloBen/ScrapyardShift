package ui.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import model.item.BinType;
import model.station.StationType;

import java.util.Map;

public class PixelArtGenerator {

    private static final Map<Character, Color> PALETTE = Map.ofEntries(
            Map.entry('.', new Color(0, 0, 0, 0)),
            Map.entry('K', Color.valueOf("181C20")), // Outlines
            Map.entry('W', Color.valueOf("F5F6FA")), // White highlight
            Map.entry('G', Color.valueOf("7F8FA6")), // Metal Gray
            Map.entry('D', Color.valueOf("353B48")), // Dark Steel
            Map.entry('Y', Color.valueOf("FBC531")), // Banana / Hazard Yellow
            Map.entry('O', Color.valueOf("E1B12C")), // Mango / Gold Orange
            Map.entry('R', Color.valueOf("E84118")), // Red / Biohazard
            Map.entry('B', Color.valueOf("00A8FF")), // Clean Plastic Blue
            Map.entry('C', Color.valueOf("00FFCC")), // Cyber / Acid Green-Cyan
            Map.entry('E', Color.valueOf("4CD137")), // Eco Green
            Map.entry('P', Color.valueOf("9C88FF")), // Wrapper Purple
            Map.entry('M', Color.valueOf("8C7AE6")), // Dark Purple
            Map.entry('N', Color.valueOf("71583E")), // Cardboard / Dirt Brown
            Map.entry('T', Color.valueOf("C29B61"))  // Tan Box
    );

    public static Texture loadOrCreate(String relativePath, PixmapBuilder builder) {
        FileHandle file = Gdx.files.local("src/assets/" + relativePath);
        if (!file.exists()) {
            file = Gdx.files.local("assets/" + relativePath);
        }
        if (!file.exists()) {
            file = Gdx.files.local("src/assets/" + relativePath);
            file.parent().mkdirs();
            Pixmap pm = builder.build();
            PixmapIO.writePNG(file, pm);
            pm.dispose();
        }
        Texture tex = new Texture(file);
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return tex;
    }

    @FunctionalInterface
    public interface PixmapBuilder {
        Pixmap build();
    }

    public static Pixmap buildItemCard(Color cardBg, Color badgeTint, String[] grid16) {
        Pixmap pm = new Pixmap(96, 40, Pixmap.Format.RGBA8888);
        // Outer industrial bevel
        pm.setColor(Color.valueOf("181C20"));
        pm.fillRectangle(0, 0, 96, 40);
        pm.setColor(cardBg);
        pm.fillRectangle(2, 2, 92, 36);

        // Top highlight & bottom shadow bevel
        pm.setColor(new Color(1f, 1f, 1f, 0.25f));
        pm.fillRectangle(2, 2, 92, 3);
        pm.setColor(new Color(0f, 0f, 0f, 0.35f));
        pm.fillRectangle(2, 35, 92, 3);

        // Left Icon Slot
        pm.setColor(Color.valueOf("1E272E"));
        pm.fillRectangle(4, 4, 32, 32);
        pm.setColor(badgeTint);
        pm.drawRectangle(4, 4, 32, 32);

        // Draw 16x16 Pixel Art scaled 2x inside the 32x32 slot
        drawGrid(pm, grid16, 4, 4, 2);
        return pm;
    }

    public static Pixmap buildStationSprite(StationType type) {
        Pixmap pm = new Pixmap(110, 70, Pixmap.Format.RGBA8888);
        Color header = switch (type) {
            case WASHER -> Color.valueOf("0097E6");
            case DISMANTLER -> Color.valueOf("E17055");
            case NEUTRALIZER -> Color.valueOf("00B894");
        };

        // Steel chassis with corner bolts
        pm.setColor(Color.valueOf("181C20"));
        pm.fillRectangle(0, 0, 110, 70);
        pm.setColor(Color.valueOf("2F3640"));
        pm.fillRectangle(2, 2, 106, 66);
        pm.setColor(header);
        pm.fillRectangle(2, 2, 106, 18);

        // Machine details inside body
        pm.setColor(Color.valueOf("1E272E"));
        pm.fillCircle(55, 45, 18);
        pm.setColor(header);
        pm.drawCircle(55, 45, 18);
        pm.drawCircle(55, 45, 12);

        // Corner rivets
        pm.setColor(Color.valueOf("7F8FA6"));
        pm.fillCircle(7, 25, 2);
        pm.fillCircle(103, 25, 2);
        pm.fillCircle(7, 63, 2);
        pm.fillCircle(103, 63, 2);
        return pm;
    }

    public static Pixmap buildBaySprite(BinType type) {
        Pixmap pm = new Pixmap(145, 62, Pixmap.Format.RGBA8888);
        Color accent = switch (type) {
            case BIODEGRADABLE -> Color.valueOf("4CD137");
            case RECYCLABLE -> Color.valueOf("00A8FF");
            case RESIDUAL -> Color.valueOf("7F8FA6");
            case HAZARDOUS -> Color.valueOf("E84118");
        };

        pm.setColor(Color.valueOf("181C20"));
        pm.fillRectangle(0, 0, 145, 62);
        pm.setColor(Color.valueOf("273C75"));
        pm.fillRectangle(2, 2, 141, 58);
        pm.setColor(Color.valueOf("1E272E"));
        pm.fillRectangle(4, 18, 137, 40);

        // Colored top hazard/category bar + side ribs
        pm.setColor(accent);
        pm.fillRectangle(2, 2, 141, 16);
        pm.fillRectangle(2, 2, 6, 58);
        pm.fillRectangle(137, 2, 6, 58);
        return pm;
    }

    public static Pixmap buildIncineratorSprite() {
        Pixmap pm = new Pixmap(86, 66, Pixmap.Format.RGBA8888);
        pm.setColor(Color.valueOf("181C20"));
        pm.fillRectangle(0, 0, 86, 66);
        pm.setColor(Color.valueOf("C23616"));
        pm.fillRectangle(2, 2, 82, 62);

        // Hazard stripes on top & bottom
        pm.setColor(Color.valueOf("FBC531"));
        for (int x = 4; x < 80; x += 12) {
            pm.fillRectangle(x, 4, 6, 6);
            pm.fillRectangle(x, 56, 6, 6);
        }

        // Glowing Fire Core
        pm.setColor(Color.valueOf("1E272E"));
        pm.fillRectangle(10, 14, 66, 38);
        pm.setColor(Color.valueOf("E84118"));
        pm.fillCircle(43, 34, 14);
        pm.setColor(Color.valueOf("FBC531"));
        pm.fillCircle(43, 34, 8);
        return pm;
    }

    public static Pixmap buildForkliftSprite() {
        Pixmap pm = new Pixmap(112, 70, Pixmap.Format.RGBA8888);
        pm.setColor(Color.valueOf("181C20"));
        pm.fillRectangle(0, 0, 112, 70);
        pm.setColor(Color.valueOf("E1B12C"));
        pm.fillRectangle(2, 2, 108, 66);
        pm.setColor(Color.valueOf("2F3640"));
        pm.fillRectangle(6, 22, 100, 42);
        // Lever slot
        pm.setColor(Color.valueOf("E84118"));
        pm.fillCircle(90, 43, 7);
        return pm;
    }

    private static void drawGrid(Pixmap pm, String[] rows, int startX, int startY, int scale) {
        for (int r = 0; r < Math.min(16, rows.length); r++) {
            String row = rows[r];
            for (int c = 0; c < Math.min(16, row.length()); c++) {
                Color col = PALETTE.get(row.charAt(c));
                if (col != null && col.a > 0) {
                    pm.setColor(col);
                    pm.fillRectangle(startX + c * scale, startY + r * scale, scale, scale);
                }
            }
        }
    }

    // 16x16 Pixel Art Patterns
    public static final String[] ICON_BANANA = {
            ".......KK.......", "......KYYK......", ".....KYYYYK.....", ".....KYYYYK.....",
            "....KYYYYK......", "....KYYYYK......", "...KYYYYYYK.....", "..KYYYYYYYYK....",
            ".KYYYKKKKYYYK...", ".KYYK....KYYK...", "KYYK......KYYK..", "KNNK......KNNK..",
            ".KK........KK...", "................", "................", "................"
    };

    public static final String[] ICON_MANGO = {
            ".......KEE......", "......KEEEK.....", ".....KKKKKK.....", "...KKOOOOOOKK...",
            "..KOOOOOOOOOOK..", ".KOOOWWOOOOOOOK.", ".KOOOWOOOOOOOOK.", ".KOOOOOOOOYONOK.",
            ".KOOOOOOONNNNOK.", "..KOOOOOONNNOK..", "...KOOOOOOOOOK..", "....KKOOOOOOK...",
            "......KKKKKK....", "................", "................", "................"
    };

    public static final String[] ICON_CAN = {
            "....KKKKKKKK....", "...KGGGGGGGGK...", "...KDDDDDDDDK...", "...KBBBBBBBBK...",
            "...KBWWBBBBBK...", "...KBWBBBBBBK...", "...KBBBBWWWWK...", "...KBBBBWBBBK...",
            "...KBBBBWWWWK...", "...KBBBBBBBBK...", "...KBBBBBBBBK...", "...KGGGGGGGGK...",
            "....KKKKKKKK....", "................", "................", "................"
    };

    public static final String[] ICON_WRAPPER = {
            "..KKKKKKKKKKKK..", "...KPPPPPPPPK...", "..KPPPPPPPPPPK..", "..KPPYYYYYYPPK..",
            "..KPPYWWWWYPPK..", "..KPPYYYYYYPPK..", "..KPPPPPPPPPPK..", "..KPMMMMMMMMPK..",
            "..KPPPPPPPPPPK..", "...KPPPPPPPPK...", "..KKKKKKKKKKKK..", "................",
            "................", "................", "................", "................"
    };

    public static final String[] ICON_BOTTLE_DIRTY = {
            "......KKKK......", "......KRRK......", "......KKKK......", ".....KGGGGK.....",
            "....KGGGGGGK....", "....KNNNNNNGK...", "....KNNNNNNGK...", "....KGGNNNNGK...",
            "....KGGGGGGGK...", "....KNNGGGGGK...", "....KNNNNNNGK...", "....KGGGGGGGK...",
            ".....KKKKKKK....", "................", "................", "................"
    };

    public static final String[] ICON_BOTTLE_CLEAN = {
            "......KKKK......", "......KBBK......", "......KKKK......", ".....KBBBBK.....",
            "....KBBBBBBK....", "....KBWWBBBBK...", "....KBWBBBBBK...", "....KBBBBBBBK...",
            "....KWWWWWWWK...", "....KBBBBBBBK...", "....KBWWBBBBK...", "....KBBBBBBBK...",
            ".....KKKKKKK....", "................", "................", "................"
    };

    public static final String[] ICON_BOX_GREASY = {
            "..KKKKKKKKKKKK..", ".KTTTTTTTTTTTTK.", ".KTNNNTTTTTTNTK.", ".KTNNNNTTTTNNTK.",
            ".KTTTTTTTTTTTTK.", ".KKKKKKKKKKKKKK.", ".KTTTTTTTTTTTTK.", ".KTTNNNNNNNNTTK.",
            ".KTTNNNNNNNNTTK.", ".KTTTTNNNNTTTTK.", ".KTTTTTTTTTTTTK.", "..KKKKKKKKKKKK..",
            "................", "................", "................", "................"
    };

    public static final String[] ICON_BOX_CLEAN = {
            "..KKKKKKKKKKKK..", ".KTTTTTTTTTTTTK.", ".KTWWTTTTTTTTTK.", ".KTTTTTTTTTTTTK.",
            ".KTTTTTTTTTTTTK.", ".KKKKKKKKKKKKKK.", ".KTTTTTTTTTTTTK.", ".KTTTTWWWWTTTTK.",
            ".KTTTTTTTTTTTTK.", ".KTTTTTTTTTTTTK.", ".KTTTTTTTTTTTTK.", "..KKKKKKKKKKKK..",
            "................", "................", "................", "................"
    };

    public static final String[] ICON_PHONE_INTACT = {
            "....KKKKKKKK....", "...KDDDDDDDDK...", "...KDDRRRRDDK...", "...KDRRRRRRDK...",
            "...KDRRYYRRDK...", "...KDDRRRRDDK...", "...KDDDDDDDDK...", "...KDDWWWWDDK...",
            "...KDDDDDDDDK...", "...KDDDDDDDDK...", "...KDDGGGGDDK...", "....KKKKKKKK....",
            "................", "................", "................", "................"
    };

    public static final String[] ICON_PHONE_FRAME = {
            "....KKKKKKKK....", "...KEEEEEEEEK...", "...KEDDDDDDEK...", "...KED....DEK...",
            "...KED....DEK...", "...KED....DEK...", "...KED....DEK...", "...KED....DEK...",
            "...KEDDDDDDEK...", "...KEEEEEEEEK...", "...KEEGGGGEEK...", "....KKKKKKKK....",
            "................", "................", "................", "................"
    };

    public static final String[] ICON_BATTERY = {
            ".....KKKKKK.....", "....KGGGGGGK....", "...KKKKKKKKKK...", "...KRRRRRRRRK...",
            "...KRRYYYYRRK...", "...KRRYWWYRRK...", "...KRRYYYYRRK...", "...KRRRRRRRRK...",
            "...KDDDDDDDDK...", "...KDDDDDDDDK...", "...KDDDDDDDDK...", "...KKKKKKKKKK...",
            "................", "................", "................", "................"
    };

    public static final String[] ICON_SYRINGE = {
            ".......KKK......", ".......KGK......", ".....KKKKKKK....", ".....KWWWWWK....",
            ".....KWWRWWK....", ".....KWRRRWK....", ".....KWRRRWK....", ".....KWRRRWK....",
            ".....KKKKKKK....", ".......KGK......", ".......KGK......", ".......K.K......",
            "................", "................", "................", "................"
    };

    public static final String[] ICON_CHEM = {
            ".....KKKK.......", ".....KGGK.......", "....KKKKKKK.....", "...KRRRRRRRK....",
            "..KRRRRRRRRRK...", "..KRRCCCCCRRK...", "..KRRCCCCCRRK...", "..KRRRRCCCCRK...",
            "..KRRRRRRCCRK...", "..KRRRRRRRRRK...", "...KKKKKKKKK....", "......CCC.......",
            ".....CCCCC......", "................", "................", "................"
    };
}