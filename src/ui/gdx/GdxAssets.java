package ui.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GdxAssets {
    public final BitmapFont fontSmall;
    public final BitmapFont fontMedium;
    public final BitmapFont fontLarge;

    public final Texture whitePixel;
    public final Texture floorTileA;
    public final Texture floorTileB;
    public final Texture botTexture;
    public final Texture incineratorTexture;
    public final Texture forkliftTexture;
    public Texture customPlayerSheet;

    private final Map<String, Texture> itemSprites = new HashMap<>();
    private final Map<model.station.StationType, Texture> stationSprites = new java.util.EnumMap<>(model.station.StationType.class);
    private final Map<model.item.BinType, Texture> baySprites = new java.util.EnumMap<>(model.item.BinType.class);

    public Sound sfxGrab;
    public Sound sfxThrow;
    public Sound sfxSuccess;
    public Sound sfxError;
    public Sound sfxClick;
    public Sound bgmLoop;

    public GdxAssets() {
        fontSmall = new BitmapFont();
        fontSmall.getData().setScale(1.0f);

        fontMedium = new BitmapFont();
        fontMedium.getData().setScale(1.35f);

        fontLarge = new BitmapFont();
        fontLarge.getData().setScale(2.0f);

        whitePixel = createSolidTexture(1, 1, Color.WHITE);
        floorTileA = PixelArtGenerator.loadOrCreate("stations/floor_tile_a.png",
                () -> createTilePixmap(new Color(0.14f, 0.19f, 0.22f, 1f), new Color(0.12f, 0.16f, 0.19f, 1f)));
        floorTileB = PixelArtGenerator.loadOrCreate("stations/floor_tile_b.png",
                () -> createTilePixmap(new Color(0.17f, 0.22f, 0.26f, 1f), new Color(0.15f, 0.19f, 0.22f, 1f)));
        botTexture = PixelArtGenerator.loadOrCreate("player/cleaner_bot.png", this::createBotPixmap);
        incineratorTexture = PixelArtGenerator.loadOrCreate("stations/incinerator.png", PixelArtGenerator::buildIncineratorSprite);
        forkliftTexture = PixelArtGenerator.loadOrCreate("stations/forklift_bay.png", PixelArtGenerator::buildForkliftSprite);

        for (model.station.StationType st : model.station.StationType.values()) {
            stationSprites.put(st, PixelArtGenerator.loadOrCreate(
                    "stations/station_" + st.name().toLowerCase() + ".png",
                    () -> PixelArtGenerator.buildStationSprite(st)
            ));
        }
        for (model.item.BinType bt : model.item.BinType.values()) {
            baySprites.put(bt, PixelArtGenerator.loadOrCreate(
                    "stations/bay_" + bt.name().toLowerCase() + ".png",
                    () -> PixelArtGenerator.buildBaySprite(bt)
            ));
        }

        FileHandle customPlayerFile = Gdx.files.local("src/assets/player/player_sheet.png");
        if (customPlayerFile.exists()) {
            customPlayerSheet = new Texture(customPlayerFile);
            customPlayerSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }

        buildCustomItemSprites();
        buildProceduralAudio();
    }

    private Texture createSolidTexture(int w, int h, Color c) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(c);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        return tex;
    }

    private Pixmap createTilePixmap(Color base, Color border) {
        Pixmap pm = new Pixmap(45, 45, Pixmap.Format.RGBA8888);
        pm.setColor(base);
        pm.fill();
        pm.setColor(border);
        pm.drawRectangle(0, 0, 45, 45);
        pm.fillCircle(5, 5, 1);
        pm.fillCircle(40, 5, 1);
        pm.fillCircle(5, 40, 1);
        pm.fillCircle(40, 40, 1);
        return pm;
    }

    private Pixmap createBotPixmap() {
        Pixmap pm = new Pixmap(36, 36, Pixmap.Format.RGBA8888);
        pm.setColor(new Color(0.18f, 0.8f, 0.75f, 1f));
        pm.fillCircle(18, 18, 16);
        pm.setColor(Color.BLACK);
        pm.drawCircle(18, 18, 16);
        pm.setColor(new Color(0.1f, 0.15f, 0.2f, 1f));
        pm.fillRectangle(8, 10, 20, 10);
        pm.setColor(Color.GREEN);
        pm.fillRectangle(11, 13, 5, 4);
        pm.fillRectangle(20, 13, 5, 4);
        return pm;
    }

    private void buildCustomItemSprites() {
        registerItem("Banana Peel", "items/banana_peel.png", Color.valueOf("D4AC0D"), Color.YELLOW, PixelArtGenerator.ICON_BANANA);
        registerItem("Spoiled Mango", "items/spoiled_mango.png", Color.valueOf("CA6F1E"), Color.ORANGE, PixelArtGenerator.ICON_MANGO);
        registerItem("Soda Can", "items/soda_can.png", Color.valueOf("2471A3"), Color.CYAN, PixelArtGenerator.ICON_CAN);
        registerItem("Chip Wrapper", "items/chip_wrapper.png", Color.valueOf("7D3C98"), Color.MAGENTA, PixelArtGenerator.ICON_WRAPPER);
        registerItem("Bottle (Dirty)", "items/bottle_dirty.png", Color.valueOf("616A6B"), Color.GRAY, PixelArtGenerator.ICON_BOTTLE_DIRTY);
        registerItem("Bottle (Clean)", "items/bottle_clean.png", Color.valueOf("2980B9"), Color.CYAN, PixelArtGenerator.ICON_BOTTLE_CLEAN);
        registerItem("Box (Greasy)", "items/box_greasy.png", Color.valueOf("6E5B4B"), Color.GRAY, PixelArtGenerator.ICON_BOX_GREASY);
        registerItem("Box (Clean)", "items/box_clean.png", Color.valueOf("BA681E"), Color.ORANGE, PixelArtGenerator.ICON_BOX_CLEAN);
        registerItem("Phone (Intact)", "items/phone_intact.png", Color.valueOf("D35400"), Color.RED, PixelArtGenerator.ICON_PHONE_INTACT);
        registerItem("Phone (Frame)", "items/phone_frame.png", Color.valueOf("229954"), Color.GREEN, PixelArtGenerator.ICON_PHONE_FRAME);
        registerItem("Battery (Live!)", "items/battery_live.png", Color.valueOf("C0392B"), Color.YELLOW, PixelArtGenerator.ICON_BATTERY);
        registerItem("Battery (Sealed)", "items/battery_sealed.png", Color.valueOf("1E8449"), Color.GREEN, PixelArtGenerator.ICON_BATTERY);
        registerItem("Used Syringe", "items/used_syringe.png", Color.valueOf("922B21"), Color.RED, PixelArtGenerator.ICON_SYRINGE);
        registerItem("Chem (Leaking!)", "items/chem_leaking.png", Color.valueOf("B03A2E"), Color.GREEN, PixelArtGenerator.ICON_CHEM);
        registerItem("Chem (Sealed)", "items/chem_sealed.png", Color.valueOf("196F3D"), Color.CYAN, PixelArtGenerator.ICON_CHEM);
    }

    private void registerItem(String tag, String path, Color bg, Color border, String[] grid) {
        Texture tex = PixelArtGenerator.loadOrCreate(path, () -> PixelArtGenerator.buildItemCard(bg, border, grid));
        itemSprites.put(tag, tex);
    }

    public Texture getStationSprite(model.station.StationType type) {
        return stationSprites.getOrDefault(type, whitePixel);
    }

    public Texture getBaySprite(model.item.BinType type) {
        return baySprites.getOrDefault(type, whitePixel);
    }

    private Texture makeIcon(Color primary, Color detail, String shape) {
        Pixmap pm = new Pixmap(96, 40, Pixmap.Format.RGBA8888);
        // Card body
        pm.setColor(primary);
        pm.fillRectangle(2, 2, 92, 36);
        pm.setColor(Color.BLACK);
        pm.drawRectangle(0, 0, 96, 40);
        pm.drawRectangle(1, 1, 94, 38);

        // Left pixel-art emblem box
        pm.setColor(new Color(0, 0, 0, 0.28f));
        pm.fillRectangle(5, 5, 26, 30);
        pm.setColor(detail);
        switch (shape) {
            case "BOTTLE" -> {
                pm.fillRectangle(14, 7, 8, 5);
                pm.fillRectangle(11, 12, 14, 20);
            }
            case "CAN" -> {
                pm.fillRectangle(11, 9, 14, 22);
                pm.setColor(Color.WHITE);
                pm.drawLine(11, 13, 25, 13);
            }
            case "PHONE" -> {
                pm.fillRectangle(11, 7, 14, 26);
                pm.setColor(Color.BLACK);
                pm.fillRectangle(13, 10, 10, 18);
            }
            case "BATT", "CHEM" -> {
                pm.fillRectangle(10, 10, 16, 22);
                pm.setColor(Color.BLACK);
                pm.fillCircle(18, 21, 5);
            }
            default -> pm.fillCircle(18, 20, 9);
        }

        Texture tex = new Texture(pm);
        pm.dispose();
        return tex;
    }

    public Texture getItemSprite(String tag) {
        return itemSprites.getOrDefault(tag, whitePixel);
    }

    private void buildProceduralAudio() {
        try {
            sfxGrab = generateToneSound(520, 680, 0.08f);
            sfxThrow = generateToneSound(700, 280, 0.14f);
            sfxSuccess = generateToneSound(440, 880, 0.18f);
            sfxError = generateToneSound(220, 110, 0.28f);
            sfxClick = generateToneSound(800, 900, 0.04f);
            bgmLoop = generateBgmTrack();
        } catch (Exception ignored) {}
    }

    private Sound generateToneSound(double startFreq, double endFreq, float durationSec) throws Exception {
        int sampleRate = 22050;
        int numSamples = (int) (sampleRate * durationSec);
        byte[] pcm = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            double progress = (double) i / numSamples;
            double freq = startFreq + (endFreq - startFreq) * progress;
            double env = 1.0 - progress;
            short val = (short) (Math.sin(2.0 * Math.PI * freq * i / sampleRate) * 14000 * env);
            pcm[i * 2] = (byte) (val & 0xff);
            pcm[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        return loadSoundFromPcm(pcm, sampleRate);
    }

    private Sound generateBgmTrack() throws Exception {
        int sampleRate = 22050;
        int[] notes = {261, 329, 392, 523, 392, 329, 293, 349, 440, 587, 440, 349};
        int samplesPerNote = sampleRate / 5;
        int totalSamples = notes.length * samplesPerNote;
        byte[] pcm = new byte[totalSamples * 2];

        for (int n = 0; n < notes.length; n++) {
            int freq = notes[n];
            for (int i = 0; i < samplesPerNote; i++) {
                int idx = n * samplesPerNote + i;
                double env = Math.max(0.1, 1.0 - ((double) i / samplesPerNote));
                short val = (short) (Math.sin(2.0 * Math.PI * freq * i / sampleRate) * 3500 * env);
                pcm[idx * 2] = (byte) (val & 0xff);
                pcm[idx * 2 + 1] = (byte) ((val >> 8) & 0xff);
            }
        }
        return loadSoundFromPcm(pcm, sampleRate);
    }

    private Sound loadSoundFromPcm(byte[] pcm, int sampleRate) throws Exception {
        File temp = File.createTempFile("scrapyard_sfx_", ".wav");
        temp.deleteOnExit();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        int dataSize = pcm.length;
        dos.writeBytes("RIFF");
        dos.writeInt(Integer.reverseBytes(36 + dataSize));
        dos.writeBytes("WAVEfmt ");
        dos.writeInt(Integer.reverseBytes(16));
        dos.writeShort(Short.reverseBytes((short) 1));
        dos.writeShort(Short.reverseBytes((short) 1));
        dos.writeInt(Integer.reverseBytes(sampleRate));
        dos.writeInt(Integer.reverseBytes(sampleRate * 2));
        dos.writeShort(Short.reverseBytes((short) 2));
        dos.writeShort(Short.reverseBytes((short) 16));
        dos.writeBytes("data");
        dos.writeInt(Integer.reverseBytes(dataSize));
        dos.write(pcm);

        FileHandle fh = new FileHandle(temp);
        fh.writeBytes(baos.toByteArray(), false);
        return Gdx.audio.newSound(fh);
    }

    public void dispose() {
        fontSmall.dispose();
        fontMedium.dispose();
        fontLarge.dispose();
        whitePixel.dispose();
        floorTileA.dispose();
        floorTileB.dispose();
        botTexture.dispose();
        itemSprites.values().forEach(Texture::dispose);
    }
}