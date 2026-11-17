package ui.gdx;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class GdxLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Scrapyard Shift — MRF Operations (LibGDX Engine)");
        config.setWindowedMode(ScrapyardGdxGame.V_WIDTH, ScrapyardGdxGame.V_HEIGHT);
        config.setForegroundFPS(60);
        config.useVsync(true);
        new Lwjgl3Application(new ScrapyardGdxGame(), config);
    }
}