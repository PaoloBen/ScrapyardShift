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
    private final Map<model.item.BinType, Texture[]> baySprites = new java.util.EnumMap<>(model.item.BinType.class);

    public Sound sfxGrab;
    public Sound sfxThrow;
    public Sound sfxSuccess;
    public Sound sfxError;
    public Sound sfxClick;
    public Sound sfxDoorSlam;
    public Sound sfxVictoryFanfare;
    public Sound sfxGameOverTrack;

    public final Sound[] bgmStages = new Sound[20];
    public Sound bgmMenu;
    public Sound bgmCustomize;
    public Sound bgmLoop;

    @FunctionalInterface
    private interface SoundSupplier {
        Sound get() throws Exception;
    }

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
        incineratorTexture = PixelArtGenerator.loadOrCreate("stations/incinerator_v2.png", PixelArtGenerator::buildIncineratorSprite);
        forkliftTexture = PixelArtGenerator.loadOrCreate("stations/forklift_vehicle_v2.png", PixelArtGenerator::buildForkliftSprite);

        for (model.station.StationType st : model.station.StationType.values()) {
            stationSprites.put(st, PixelArtGenerator.loadOrCreate(
                    "stations/machine_" + st.name().toLowerCase() + ".png",
                    () -> PixelArtGenerator.buildStationSprite(st)
            ));
        }
        for (model.item.BinType bt : model.item.BinType.values()) {
            Texture[] stages = new Texture[4];
            for (int s = 0; s < 4; s++) {
                final int stageIdx = s;
                stages[s] = PixelArtGenerator.loadOrCreate(
                        "stations/bin_" + bt.name().toLowerCase() + "_" + s + ".png",
                        () -> PixelArtGenerator.buildBaySprite(bt, stageIdx)
                );
            }
            baySprites.put(bt, stages);
        }

        FileHandle customPlayerFile = Gdx.files.local("src/assets/player/player_sheet.png");
        if (customPlayerFile.exists()) {
            customPlayerSheet = new Texture(customPlayerFile);
            customPlayerSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }

        buildCustomItemSprites();
        buildAudioWithOggOverride();
    }

    // Checks src/assets/audio/<baseName>.ogg (or .wav/.mp3) first so you can drop in custom audio files anytime!
    private Sound loadCustomOrGenerate(String baseName, SoundSupplier fallback) {
        String[] folders = { "src/assets/audio/", "assets/audio/", "src/assets/", "assets/" };
        String[] exts = { ".ogg", ".wav", ".mp3" };
        for (String dir : folders) {
            for (String ext : exts) {
                FileHandle fh = Gdx.files.local(dir + baseName + ext);
                if (fh.exists()) {
                    try {
                        return Gdx.audio.newSound(fh);
                    } catch (Exception ignored) {}
                }
            }
        }
        try {
            return fallback.get();
        } catch (Exception e) {
            return null;
        }
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

    public Texture getBaySprite(model.item.BinType type, int fillLevel, int capacity) {
        Texture[] stages = baySprites.get(type);
        if (stages == null) return whitePixel;
        if (fillLevel <= 0) return stages[0];
        if (fillLevel >= capacity) return stages[3];
        float ratio = (float) fillLevel / Math.max(1, capacity);
        return (ratio < 0.5f) ? stages[1] : stages[2];
    }

    public Texture getItemSprite(String tag) {
        return itemSprites.getOrDefault(tag, whitePixel);
    }

    private void buildAudioWithOggOverride() {
        FileHandle audioDir = Gdx.files.local("src/assets/audio");
        if (!audioDir.exists()) audioDir.mkdirs();

        sfxGrab           = loadCustomOrGenerate("sfx_grab",      () -> generateToneSound(520, 680, 0.08f));
        sfxThrow          = loadCustomOrGenerate("sfx_throw",     () -> generateToneSound(700, 280, 0.14f));
        sfxSuccess        = loadCustomOrGenerate("sfx_success",   () -> generateToneSound(440, 880, 0.18f));
        sfxError          = loadCustomOrGenerate("sfx_error",     () -> generateToneSound(220, 110, 0.28f));
        sfxClick          = loadCustomOrGenerate("sfx_click",     () -> generateToneSound(800, 900, 0.04f));
        sfxDoorSlam       = loadCustomOrGenerate("sfx_door_slam", this::generateDoorSlamSound);
        sfxVictoryFanfare = loadCustomOrGenerate("sfx_victory",   () -> generateJingle(new int[]{523, 659, 784, 1046, 784, 1046}, 0.14f));
        sfxGameOverTrack  = loadCustomOrGenerate("sfx_gameover",  () -> generateJingle(new int[]{392, 370, 349, 330, 293, 261}, 0.20f));

        // Dedicated Upbeat Arcade Menu & World Map BGM (bgm_menu.ogg)
        bgmMenu = loadCustomOrGenerate("bgm_menu", () -> generateStageTrack(
                new int[]{523, 659, 784, 659, 880, 784, 659, 523, 587, 698, 880, 698, 784, 659, 587, 0},
                new int[]{131, 165, 196, 165, 147, 175, 196, 131}, 6, false));

        // Chill Customize Screen BGM (bgm_customize.ogg)
        bgmCustomize = loadCustomOrGenerate("bgm_customize", () -> generateStageTrack(
                new int[]{392, 0, 494, 0, 587, 0, 494, 0, 349, 0, 440, 0, 523, 0, 440, 0},
                new int[]{98, 98, 131, 131, 87, 87, 131, 131}, 4, false));

        int[][] baseMelodies = {
                {523, 0, 659, 587, 523, 659, 784, 0, 698, 0, 659, 587, 523, 587, 659, 0},
                {440, 523, 659, 0, 587, 523, 440, 0, 392, 440, 523, 587, 659, 0, 587, 0},
                {587, 698, 880, 0, 784, 698, 659, 0, 587, 659, 698, 784, 880, 0, 784, 659},
                {659, 0, 784, 880, 987, 880, 784, 659, 587, 0, 659, 784, 659, 587, 494, 0},
                {698, 659, 587, 523, 698, 0, 880, 0, 784, 698, 659, 587, 523, 659, 587, 0}
        };
        int[][] baseBass = {
                {131, 131, 196, 196, 175, 175, 131, 196},
                {110, 110, 165, 165, 147, 147, 110, 165},
                {147, 147, 220, 220, 175, 175, 196, 196},
                {165, 165, 247, 247, 196, 196, 147, 196},
                {147, 220, 147, 220, 175, 261, 196, 147}
        };

        // 20 Unique Stage Tracks (bgm_stage_1.ogg .. bgm_stage_20.ogg)
        for (int i = 0; i < 20; i++) {
            final int stageIdx = i;
            bgmStages[i] = loadCustomOrGenerate("bgm_stage_" + (i + 1), () -> {
                int[] mel = baseMelodies[stageIdx % 5].clone();
                int[] bas = baseBass[stageIdx % 5].clone();
                double pitchShift = Math.pow(1.035, stageIdx / 2);
                for (int k = 0; k < mel.length; k++) mel[k] = (int) (mel[k] * pitchShift);
                for (int k = 0; k < bas.length; k++) bas[k] = (int) (bas[k] * pitchShift);
                int tempo = 7 + (stageIdx % 4);
                return generateStageTrack(mel, bas, tempo, true);
            });
        }

        bgmLoop = bgmMenu;
    }

    private Sound generateDoorSlamSound() throws Exception {
        int sampleRate = 22050;
        int numSamples = (int) (sampleRate * 0.35f);
        byte[] pcm = new byte[numSamples * 2];
        for (int i = 0; i < numSamples; i++) {
            double p = (double) i / numSamples;
            double env = Math.pow(1.0 - p, 1.8);
            double thud = Math.sin(2.0 * Math.PI * (95.0 - 55.0 * p) * i / sampleRate) * 18000;
            double clang = ((Math.random() - 0.5) * 9000) * Math.exp(-p * 6.0);
            short val = (short) Math.max(-32000, Math.min(32000, (thud + clang) * env));
            pcm[i * 2] = (byte) (val & 0xff);
            pcm[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        return loadSoundFromPcm(pcm, sampleRate);
    }

    private Sound generateJingle(int[] notes, float noteDurationSec) throws Exception {
        int sampleRate = 22050;
        int samplesPerNote = (int) (sampleRate * noteDurationSec);
        byte[] pcm = new byte[notes.length * samplesPerNote * 2];
        for (int n = 0; n < notes.length; n++) {
            int freq = notes[n];
            for (int i = 0; i < samplesPerNote; i++) {
                int idx = n * samplesPerNote + i;
                double env = Math.max(0.05, 1.0 - ((double) i / samplesPerNote));
                short val = (short) (Math.tanh(Math.sin(2.0 * Math.PI * freq * i / sampleRate) * 2.0) * 6500 * env);
                pcm[idx * 2] = (byte) (val & 0xff);
                pcm[idx * 2 + 1] = (byte) ((val >> 8) & 0xff);
            }
        }
        return loadSoundFromPcm(pcm, sampleRate);
    }

    private Sound generateStageTrack(int[] melody, int[] bass, int stepsPerSec, boolean withHiHat) throws Exception {
        int sampleRate = 22050;
        int stepSamples = sampleRate / stepsPerSec;
        int totalSamples = melody.length * stepSamples;
        byte[] pcm = new byte[totalSamples * 2];

        for (int step = 0; step < melody.length; step++) {
            int melFreq = melody[step];
            int bassFreq = bass[(step / 2) % bass.length];
            for (int i = 0; i < stepSamples; i++) {
                int idx = step * stepSamples + i;
                double t = (double) i / sampleRate;
                double env = Math.max(0.0, 1.0 - ((double) i / stepSamples));

                double leadWave = (melFreq > 0) ? Math.tanh(Math.sin(2.0 * Math.PI * melFreq * t) * 1.8) * 2000 * env : 0;
                double bassWave = Math.sin(2.0 * Math.PI * bassFreq * t) * 2200 * Math.pow(env, 0.5);
                double hatWave = (withHiHat && step % 2 == 1 && i < stepSamples / 4) ? ((Math.random() - 0.5) * 750 * env) : 0;

                short val = (short) Math.max(-32000, Math.min(32000, leadWave + bassWave + hatWave));
                pcm[idx * 2] = (byte) (val & 0xff);
                pcm[idx * 2 + 1] = (byte) ((val >> 8) & 0xff);
            }
        }
        return loadSoundFromPcm(pcm, sampleRate);
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
        incineratorTexture.dispose();
        forkliftTexture.dispose();
        if (customPlayerSheet != null) customPlayerSheet.dispose();
        itemSprites.values().forEach(Texture::dispose);
        stationSprites.values().forEach(Texture::dispose);
        for (Texture[] arr : baySprites.values()) {
            for (Texture t : arr) t.dispose();
        }
    }
}