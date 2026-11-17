package ui.gdx;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.GdxNativesLoader;

import java.io.File;

public class GdxLauncher {
    public static void main(String[] args) {
        // Load gdx64.dll BEFORE creating any Pixmap for the window icon!
        GdxNativesLoader.load();

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Scrapyard Shift");
        config.setWindowedMode(ScrapyardGdxGame.V_WIDTH, ScrapyardGdxGame.V_HEIGHT);
        config.setForegroundFPS(60);
        config.useVsync(true);

        try {
            File iconFile = new File("src/assets/app_icon.png");
            if (!iconFile.exists()) {
                iconFile.getParentFile().mkdirs();
                Pixmap pm = PixelArtGenerator.buildAppIcon();
                PixmapIO.writePNG(new FileHandle(iconFile), pm);
                pm.dispose();
            }
            config.setWindowIcon(com.badlogic.gdx.Files.FileType.Absolute, iconFile.getAbsolutePath());
        } catch (Exception ignored) {}

        new Lwjgl3Application(new ScrapyardGdxGame(), config);
    }
}