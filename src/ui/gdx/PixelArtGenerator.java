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
            Map.entry('K', Color.valueOf("181C20")),
            Map.entry('W', Color.valueOf("F5F6FA")),
            Map.entry('G', Color.valueOf("7F8FA6")),
            Map.entry('D', Color.valueOf("353B48")),
            Map.entry('Y', Color.valueOf("FBC531")),
            Map.entry('O', Color.valueOf("E1B12C")),
            Map.entry('R', Color.valueOf("E84118")),
            Map.entry('B', Color.valueOf("00A8FF")),
            Map.entry('C', Color.valueOf("00FFCC")),
            Map.entry('E', Color.valueOf("4CD137")),
            Map.entry('P', Color.valueOf("9C88FF")),
            Map.entry('M', Color.valueOf("8C7AE6")),
            Map.entry('N', Color.valueOf("71583E")),
            Map.entry('T', Color.valueOf("C29B61"))
    );

    public static Texture loadOrCreate(String relativePath, PixmapBuilder builder) {
        FileHandle file = Gdx.files.local("src/assets/" + relativePath);
        if (!file.exists()) {
            file = Gdx.files.local("assets/" + relativePath);
        }
        boolean needsCreate = !file.exists();
        if (!needsCreate && (relativePath.startsWith("items/") || relativePath.contains("forklift"))) {
            Texture check = new Texture(file);
            if ((check.getWidth() == 96 && check.getHeight() == 40) || check.getWidth() == 92) {
                needsCreate = true;
            }
            check.dispose();
        }
        if (needsCreate) {
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
        Pixmap pm = new Pixmap(48, 48, Pixmap.Format.RGBA8888);
        drawGrid(pm, grid16, 0, 0, 3);
        return pm;
    }

    public static Pixmap buildStationSprite(StationType type) {
        Pixmap pm = new Pixmap(84, 72, Pixmap.Format.RGBA8888);
        pm.setColor(Color.valueOf("181C20"));
        pm.fillRectangle(0, 0, 84, 72);

        switch (type) {
            case WASHER -> {
                pm.setColor(Color.valueOf("DCE1E7"));
                pm.fillRectangle(2, 2, 80, 68);
                pm.setColor(Color.valueOf("0097E6"));
                pm.fillRectangle(2, 2, 80, 16);
                pm.setColor(Color.valueOf("F5F6FA"));
                pm.fillRectangle(6, 5, 22, 10);
                pm.setColor(Color.valueOf("FBC531"));
                pm.fillCircle(62, 10, 4);
                pm.setColor(Color.valueOf("4CD137"));
                pm.fillCircle(73, 10, 4);
                pm.setColor(Color.valueOf("2F3640"));
                pm.fillCircle(42, 43, 22);
                pm.setColor(Color.valueOf("7F8FA6"));
                pm.fillCircle(42, 43, 19);
                pm.setColor(Color.valueOf("00A8FF"));
                pm.fillCircle(42, 43, 15);
                pm.setColor(Color.valueOf("7ED6DF"));
                pm.fillCircle(37, 47, 6);
                pm.fillCircle(48, 45, 5);
                pm.setColor(Color.WHITE);
                pm.fillCircle(36, 36, 3);
                pm.fillCircle(47, 39, 2);
            }
            case DISMANTLER -> {
                pm.setColor(Color.valueOf("353B48"));
                pm.fillRectangle(2, 2, 80, 68);
                pm.setColor(Color.valueOf("E17055"));
                pm.fillRectangle(2, 2, 80, 12);
                pm.setColor(Color.valueOf("1E272E"));
                pm.fillRectangle(8, 18, 68, 44);
                pm.setColor(Color.valueOf("FBC531"));
                pm.fillRectangle(8, 28, 10, 24);
                pm.fillRectangle(66, 28, 10, 24);
                pm.setColor(Color.valueOf("BDC3C7"));
                pm.fillCircle(33, 40, 13);
                pm.fillCircle(51, 40, 13);
                pm.setColor(Color.valueOf("E84118"));
                pm.fillCircle(33, 40, 4);
                pm.fillCircle(51, 40, 4);
            }
            case NEUTRALIZER -> {
                pm.setColor(Color.valueOf("2F3640"));
                pm.fillRectangle(2, 2, 80, 68);
                pm.setColor(Color.valueOf("00B894"));
                pm.fillRectangle(6, 2, 72, 12);
                pm.fillRectangle(6, 58, 72, 12);
                pm.setColor(Color.valueOf("102A24"));
                pm.fillRectangle(16, 14, 52, 44);
                pm.setColor(Color.valueOf("00FFCC"));
                pm.drawRectangle(16, 14, 52, 44);
                pm.fillCircle(34, 38, 6);
                pm.fillCircle(48, 30, 8);
                pm.setColor(Color.valueOf("FBC531"));
                for (int y = 20; y <= 48; y += 9) {
                    pm.fillRectangle(8, y, 8, 5);
                    pm.fillRectangle(68, y, 8, 5);
                }
            }
        }
        return pm;
    }

    public static Pixmap buildBaySprite(BinType type, int fillStage) {
        Pixmap pm = new Pixmap(88, 72, Pixmap.Format.RGBA8888);
        Color bodyCol = switch (type) {
            case BIODEGRADABLE -> Color.valueOf("2ECC71");
            case RECYCLABLE    -> Color.valueOf("0984E3");
            case RESIDUAL      -> Color.valueOf("636E72");
            case HAZARDOUS     -> Color.valueOf("D63031");
        };
        Color rimCol = switch (type) {
            case BIODEGRADABLE -> Color.valueOf("27AE60");
            case RECYCLABLE    -> Color.valueOf("74B9FF");
            case RESIDUAL      -> Color.valueOf("B2BEC3");
            case HAZARDOUS     -> Color.valueOf("FBC531");
        };

        pm.setColor(Color.valueOf("181C20"));
        pm.fillRectangle(4, 8, 80, 62);
        pm.setColor(bodyCol);
        pm.fillRectangle(6, 10, 76, 58);

        pm.setColor(Color.valueOf("1E272E"));
        pm.fillRectangle(8, 10, 72, 22);

        if (fillStage >= 1) {
            pm.setColor(Color.valueOf("C29B61"));
            pm.fillCircle(26, 26, 8);
            pm.setColor(Color.valueOf("00A8FF"));
            pm.fillRectangle(42, 20, 14, 10);
        }
        if (fillStage >= 2) {
            pm.setColor(Color.valueOf("FBC531"));
            pm.fillCircle(36, 20, 9);
            pm.setColor(Color.valueOf("9C88FF"));
            pm.fillCircle(58, 22, 8);
        }
        if (fillStage >= 3) {
            pm.setColor(Color.valueOf("E84118"));
            pm.fillCircle(28, 13, 9);
            pm.setColor(Color.valueOf("4CD137"));
            pm.fillCircle(46, 11, 10);
            pm.setColor(Color.valueOf("F5F6FA"));
            pm.fillCircle(62, 14, 8);
        }

        pm.setColor(fillStage >= 3 ? Color.RED : rimCol);
        pm.fillRectangle(2, 28, 84, 6);
        pm.setColor(new Color(0f, 0f, 0f, 0.22f));
        pm.fillRectangle(18, 36, 6, 28);
        pm.fillRectangle(64, 36, 6, 28);

        pm.setColor(Color.WHITE);
        switch (type) {
            case BIODEGRADABLE -> {
                pm.fillCircle(44, 50, 9);
                pm.setColor(bodyCol);
                pm.drawLine(44, 42, 44, 58);
            }
            case RECYCLABLE -> {
                pm.drawCircle(44, 50, 10);
                pm.drawCircle(44, 50, 9);
                pm.fillCircle(44, 40, 3);
                pm.fillCircle(35, 55, 3);
                pm.fillCircle(53, 55, 3);
            }
            case RESIDUAL -> {
                pm.fillRectangle(36, 44, 16, 14);
                pm.fillCircle(44, 42, 5);
            }
            case HAZARDOUS -> {
                pm.setColor(Color.valueOf("FBC531"));
                pm.fillCircle(44, 50, 11);
                pm.setColor(Color.BLACK);
                pm.fillCircle(40, 48, 3);
                pm.fillCircle(48, 48, 3);
                pm.fillCircle(44, 55, 3);
            }
        }
        return pm;
    }

    public static Pixmap buildIncineratorSprite() {
        Pixmap pm = new Pixmap(76, 68, Pixmap.Format.RGBA8888);
        pm.setColor(Color.valueOf("181C20"));
        pm.fillRectangle(0, 0, 76, 68);
        pm.setColor(Color.valueOf("8B1E0F"));
        pm.fillRectangle(2, 2, 72, 64);

        pm.setColor(Color.valueOf("FBC531"));
        for (int x = 4; x < 70; x += 12) {
            pm.fillRectangle(x, 4, 6, 5);
            pm.fillRectangle(x, 59, 6, 5);
        }

        pm.setColor(Color.valueOf("14171A"));
        pm.fillRectangle(10, 14, 56, 40);
        pm.fillCircle(38, 24, 20);
        return pm;
    }

    public static Pixmap buildAppIcon() {
        Pixmap pm = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pm.setColor(Color.valueOf("181C20"));
        pm.fillCircle(16, 16, 15);
        pm.setColor(Color.valueOf("0984E3"));
        pm.fillCircle(16, 16, 13);
        pm.setColor(Color.valueOf("FBC531"));
        pm.fillRectangle(7, 10, 18, 9);
        pm.fillRectangle(5, 17, 22, 4);
        pm.setColor(Color.valueOf("00FFCC"));
        pm.fillRectangle(9, 22, 14, 4);
        return pm;
    }

    public static Pixmap buildForkliftSprite() {
        // 138x102 Proportional Side-Profile Pixel-Art Forklift (46x34 grid scaled 3x)
        Pixmap pm = new Pixmap(138, 102, Pixmap.Format.RGBA8888);
        String[] fkGrid = {
                "..............................................",
                ".............................KKKKKK...........",
                ".............................KGGGGK...........",
                ".............KKKKKKKKKKKKK...KGGGGK...........",
                "............KGGGGGGGGGGGGGK..KGGGGK...........",
                "............KGKKKKKKKKKKKGK..KGGDGK...........",
                "...........KGGK.........KGGK.KGGDGK...........",
                "...........KGGK.........KGGK.KGGDGK...........",
                "..........KGGK...........KGGKKGGDGK...........",
                "..........KGGK...........KGGKKGGDGK...........",
                ".........KGGK............KGGKKGGDGK...........",
                ".........KGGK.........KKKKGGKKGGDGK...........",
                "........KGGK.........KGGGGGGKKGGDGK...........",
                "........KGGK..........KGGKGGKKGGDGK...........",
                ".......KGGK............KK.KGGKGGDGK...........",
                "....KKKKGGKKKK............KGGKGGDGK...........",
                "...KYYYYYYYYYYK...........KGGKGGDGKKKK........",
                "..KYYYYYYYYYYYYK.........KOOOKGGDGKGGGK.......",
                "..KOOOOOOOOOOOOKDDDDDDDDDKYYYYGGDGKGGGK.......",
                "..KNNNNNNNNNNNNKDDDDDDDDDKOOOOYGDGKGGGK.......",
                "..KOOOOOOOOOOOOKKKKKKKKKKKOOOOOGDGKGGGK.......",
                "..KOOOOOOOOOOOOYYYYYYYYYYOOOOOOGDGKGGGK.......",
                "..KOOOOOOOOOOOOOOOOOOOOOKKKKKKKGDGKGGGK.......",
                "..KOOOONNNNNNNNOOOOOOOOKKKKKKKKGDGKGGGK.......",
                "..KOOONKKKKKKKKNOOOOOOKKKYYYYKKKDGKGGGK.......",
                "..KOOOKKKKKKKKKKOOOOOOKKYYNNYYKKDGKGGGK.......",
                "..KNNNKKKYYYYKKKOOOOOOKKYNKKNYKKDGKGGGK.......",
                "...KKKKKYYNNYYKKOOOOOOKKYNKKNYKKDGKGGGKKKKKKKK",
                "......KKYNKKNYKKKKKKKKKKYYNNYYKKDGKGGGGGGGGGGK",
                "......KKYYNNYYK........KKYYYYKKKDGKGGGGGGGGGGK",
                ".......KKKKKKKK.........KKKKKKKKKKKKKKK.......",
                "........KKKKKK...........KKKKKK...............",
                "..............................................",
                ".............................................."
        };
        drawGrid(pm, fkGrid, 0, 0, 3);
        return pm;
    }

    private static void drawGrid(Pixmap pm, String[] rows, int startX, int startY, int scale) {
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            for (int c = 0; c < row.length(); c++) {
                Color col = PALETTE.get(row.charAt(c));
                if (col != null && col.a > 0) {
                    pm.setColor(col);
                    pm.fillRectangle(startX + c * scale, startY + r * scale, scale, scale);
                }
            }
        }
    }

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