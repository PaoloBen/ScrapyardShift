package ui.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.GameController;
import model.bot.CleanerBot;
import model.item.BinType;
import model.item.WasteItem;
import model.level.FacilityLevel;
import model.player.Direction;
import model.player.OperatorCosmetics;
import model.player.Player;
import model.save.SaveManager;
import model.shop.UpgradeShop;
import model.station.StationType;
import model.station.StorageBay;
import model.station.WorkStation;
import ui.fx.FloatingText;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ScrapyardGdxGame extends ApplicationAdapter {
    public static final int V_WIDTH = 900;
    public static final int V_HEIGHT = 520;

    private enum ScreenState {
        MAIN_MENU, WORLD_MAP, PAUSED, RESTART_CONFIRM, SHIFT_COMPLETE,
        SAVE_SLOTS, CUSTOMIZE, PLAYING, SHOP, OPTIONS, MINIGAME
    }
    private ScreenState screenState = ScreenState.MAIN_MENU;
    private ScreenState previousState = ScreenState.MAIN_MENU;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private GlyphLayout glyphLayout;
    private GdxAssets assets;
    private GameController controller;

    private Sound activeBgmSound = null;
    private long activeBgmId = -1;
    private float activeBgmPitch = 1.0f;
    private float bgmVolume = 0.35f;
    private float sfxVolume = 0.65f;
    private boolean mouseAimEnabled = true;
    private boolean borderlessFullscreen = false;
    private boolean gameOverSoundPlayed = false;

    private boolean mobileHudEnabled = false;
    private int beltAnimOffset = 0;
    private float menuAnimTick = 0f;
    private float mouseX = -1, mouseY = -1;
    private int toastTimer = 180;
    private String lastSeenFeedback = "";

    private boolean chargingThrow = false;
    private float throwCharge = 0f;

    // NSMB Bird's-Eye Scrolling World Map State (2,800px wide scenic map with camera pan!)
    private static final float MAP_WORLD_WIDTH = 2850f;
    private final float[] mapNodeX = new float[20];
    private final float[] mapNodeY = new float[20];
    private int selectedMapNode = 0;
    private float mapAvatarX = 0, mapAvatarY = 0;
    private float mapCameraX = 0f;
    private boolean mapDeployPromptOpen = false;
    private boolean mapIntroTourActive = false;
    private float mapIntroTourProgress = 0f; // 0..1 forward camera pan, 1..2 return camera pan
    private boolean mapIntroPlayedThisSession = false;
    private int mapUnlockAnimFrom = -1, mapUnlockAnimTo = -1;
    private float mapUnlockAnimProgress = 0f;

    private boolean victoryCelebrationActive = false;
    private float victoryTimer = 0f;
    private final List<ConfettiPiece> confetti = new ArrayList<>();

    private static class ConfettiPiece {
        float x, y, vx, vy, swayPhase;
        Color color;
        ConfettiPiece(float x, float y, Color color) {
            this.x = x;
            this.y = y;
            this.vx = (float) ((Math.random() - 0.5) * 3.5);
            this.vy = (float) (1.2 + Math.random() * 2.4);
            this.swayPhase = (float) (Math.random() * 6.28);
            this.color = color;
        }
    }

    private boolean doorTransitionActive = false;
    private float doorProgress = 0f;
    private boolean doorClosing = true;
    private int doorHoldTimer = 0;
    private float countdownTimer = 0f;
    private Runnable onDoorClosedCallback = null;

    private enum ConfirmAction { NONE, LOAD_SLOT, SAVE_OVERWRITE, DELETE_SLOT }
    private ConfirmAction pendingConfirmAction = ConfirmAction.NONE;
    private int pendingConfirmSlot = -1;

    private WorkStation activeMinigameStation = null;
    private final List<Rectangle> washerValves = new ArrayList<>();
    private float sawMarkerPos = 0.5f;
    private float sawMarkerDir = 1.0f;
    private int sawHitsRemaining = 3;
    private final float[] plasmaHeat = { 0.85f, 0.90f, 0.80f };

    private final Rectangle btnMenuStart     = new Rectangle(62, 172, 256, 38);
    private final Rectangle btnMenuSaves     = new Rectangle(62, 216, 256, 38);
    private final Rectangle btnMenuCustomize = new Rectangle(62, 260, 256, 38);
    private final Rectangle btnMenuShop      = new Rectangle(62, 304, 256, 38);
    private final Rectangle btnMenuOptions   = new Rectangle(62, 348, 256, 38);
    private final Rectangle btnMenuQuit      = new Rectangle(62, 392, 256, 38);

    // Top-Right In-Game Pill Buttons (Currency separated to Top-Left!)
    private final Rectangle btnHudShop      = new Rectangle(510, 10, 74, 30);
    private final Rectangle btnHudSaves     = new Rectangle(590, 10, 70, 30);
    private final Rectangle btnHudCustomize = new Rectangle(666, 10, 88, 30);
    private final Rectangle btnHudOptions   = new Rectangle(760, 10, 68, 30);
    private final Rectangle btnHudPause     = new Rectangle(834, 10, 56, 30);

    // World Map HUD Buttons
    private final Rectangle btnMapDeploy = new Rectangle(520, 444, 230, 48);
    private final Rectangle btnMapShop   = new Rectangle(760, 444, 115, 48);
    private final Rectangle btnMapBack   = new Rectangle(24, 16, 120, 36);

    private final Rectangle btnPauseResume  = new Rectangle(310, 155, 280, 42);
    private final Rectangle btnPauseOptions = new Rectangle(310, 207, 280, 42);
    private final Rectangle btnPauseSaves   = new Rectangle(310, 259, 280, 42);
    private final Rectangle btnPauseRestart = new Rectangle(310, 311, 280, 42);
    private final Rectangle btnPauseMenu    = new Rectangle(310, 363, 280, 42);

    private final Rectangle btnPostNext      = new Rectangle(200, 255, 240, 46);
    private final Rectangle btnPostShop      = new Rectangle(460, 255, 240, 46);
    private final Rectangle btnPostCustomize = new Rectangle(200, 312, 240, 44);
    private final Rectangle btnPostSaves     = new Rectangle(460, 312, 240, 44);
    private final Rectangle btnPostOptions   = new Rectangle(200, 368, 240, 44);
    private final Rectangle btnPostMenu      = new Rectangle(460, 368, 240, 44);

    // Options Modal Controls (Includes Borderless Fullscreen!)
    private final Rectangle sliderBgmBar  = new Rectangle(280, 148, 340, 22);
    private final Rectangle sliderSfxBar  = new Rectangle(280, 208, 340, 22);
    private final Rectangle btnToggleAim  = new Rectangle(260, 256, 380, 36);
    private final Rectangle btnToggleFull = new Rectangle(260, 302, 380, 36);

    private final Rectangle btnDebriefRetry = new Rectangle(190, 415, 165, 44);
    private final Rectangle btnDebriefShop  = new Rectangle(368, 415, 165, 44);
    private final Rectangle btnDebriefMenu  = new Rectangle(546, 415, 165, 44);

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(V_WIDTH, V_HEIGHT, camera);
        camera.position.set(V_WIDTH / 2f, V_HEIGHT / 2f, 0);
        camera.update();

        batch = new SpriteBatch();
        glyphLayout = new GlyphLayout();
        assets = new GdxAssets();
        controller = new GameController("Operator Paul");
        controller.setInLockerRoom(false);
        controller.applyStageLayout();
        initWorldMapNodes();

        syncBackgroundMusic();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                Vector3 world = viewport.unproject(new Vector3(screenX, screenY, 0));
                mouseX = world.x;
                mouseY = V_HEIGHT - world.y;
                if (screenState == ScreenState.PLAYING && mouseAimEnabled && !isGameplayPausedByTransition()) {
                    controller.getPlayer().aimAt(mouseX, mouseY);
                }
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                Vector3 world = viewport.unproject(new Vector3(screenX, screenY, 0));
                mouseX = world.x;
                mouseY = V_HEIGHT - world.y;
                if (screenState == ScreenState.OPTIONS) {
                    updateVolumeSliders(mouseX, mouseY);
                } else if (screenState == ScreenState.PLAYING && mouseAimEnabled && !isGameplayPausedByTransition()) {
                    controller.getPlayer().aimAt(mouseX, mouseY);
                }
                return true;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Vector3 world = viewport.unproject(new Vector3(screenX, screenY, 0));
                mouseX = world.x;
                mouseY = V_HEIGHT - world.y;
                handleMouseDown(mouseX, mouseY);
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (chargingThrow && screenState == ScreenState.PLAYING) {
                    releaseThrowOrPlace();
                }
                return true;
            }

            @Override
            public boolean keyDown(int keycode) {
                handleKeyPress(keycode);
                return true;
            }

            @Override
            public boolean keyUp(int keycode) {
                if ((keycode == Input.Keys.SPACE || keycode == Input.Keys.E) && chargingThrow && screenState == ScreenState.PLAYING) {
                    releaseThrowOrPlace();
                }
                return true;
            }
        });
    }

    // 2,850px Scrolling Highway across Cebu & Mactan (Camera pans horizontally across all 20 facilities!)
    private void initWorldMapNodes() {
        for (int i = 0; i < 20; i++) {
            mapNodeX[i] = 180f + i * 132f;
            mapNodeY[i] = 235f + (float) Math.sin(i * 0.85f) * 88f;
        }
        selectedMapNode = controller.getCurrentLevelIndex();
        mapAvatarX = mapNodeX[selectedMapNode];
        mapAvatarY = mapNodeY[selectedMapNode];
        mapCameraX = Math.max(0f, Math.min(MAP_WORLD_WIDTH - V_WIDTH, mapAvatarX - V_WIDTH / 2f));
    }

    private void openWorldMap(boolean forceIntroTour) {
        clearVictoryOverlayState();
        mapDeployPromptOpen = false;
        int newlyUnlocked = controller.consumeJustUnlockedLevel();
        if (newlyUnlocked > 0) {
            mapUnlockAnimFrom = newlyUnlocked - 1;
            mapUnlockAnimTo = newlyUnlocked;
            mapUnlockAnimProgress = 0f;
            selectedMapNode = mapUnlockAnimFrom;
            mapAvatarX = mapNodeX[selectedMapNode];
            mapAvatarY = mapNodeY[selectedMapNode];
            mapCameraX = Math.max(0f, Math.min(MAP_WORLD_WIDTH - V_WIDTH, mapAvatarX - V_WIDTH / 2f));
            mapIntroTourActive = false;
        } else if (forceIntroTour || !mapIntroPlayedThisSession) {
            mapIntroPlayedThisSession = true;
            mapIntroTourActive = true;
            mapIntroTourProgress = 0f;
            selectedMapNode = Math.min(controller.getMaxUnlockedLevel(), controller.getCurrentLevelIndex());
            mapAvatarX = mapNodeX[selectedMapNode];
            mapAvatarY = mapNodeY[selectedMapNode];
            mapCameraX = 0f;
        } else {
            selectedMapNode = Math.min(controller.getMaxUnlockedLevel(), controller.getCurrentLevelIndex());
            mapAvatarX = mapNodeX[selectedMapNode];
            mapAvatarY = mapNodeY[selectedMapNode];
            mapCameraX = Math.max(0f, Math.min(MAP_WORLD_WIDTH - V_WIDTH, mapAvatarX - V_WIDTH / 2f));
        }
        screenState = ScreenState.WORLD_MAP;
        syncBackgroundMusic();
    }

    private void clearVictoryOverlayState() {
        victoryCelebrationActive = false;
        victoryTimer = 0f;
        confetti.clear();
        controller.clearShiftVictoryPending();
    }

    private void toggleBorderlessFullscreen() {
        borderlessFullscreen = !borderlessFullscreen;
        if (borderlessFullscreen) {
            Graphics.DisplayMode dm = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setUndecorated(true);
            Gdx.graphics.setWindowedMode(dm.width, dm.height);
        } else {
            Gdx.graphics.setUndecorated(false);
            Gdx.graphics.setWindowedMode(V_WIDTH, V_HEIGHT);
        }
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }

    private void syncBackgroundMusic() {
        Sound desired;
        float desiredPitch = 1.0f;

        if (screenState == ScreenState.MAIN_MENU || screenState == ScreenState.WORLD_MAP
                || (screenState == ScreenState.SAVE_SLOTS && previousState == ScreenState.MAIN_MENU)
                || (screenState == ScreenState.OPTIONS && previousState == ScreenState.MAIN_MENU)) {
            desired = assets.bgmMenu;
        } else if (screenState == ScreenState.CUSTOMIZE) {
            desired = assets.bgmCustomize;
        } else {
            int idx = Math.max(0, Math.min(19, controller.getCurrentLevelIndex()));
            desired = assets.bgmStages[idx];
            if (screenState == ScreenState.PLAYING) {
                desiredPitch = switch (controller.getTimerSpeedStage()) {
                    case 1 -> 1.00f;
                    case 2 -> 1.08f;
                    case 3 -> 1.16f;
                    default -> 1.25f;
                };
            }
        }

        if (desired != activeBgmSound) {
            if (activeBgmSound != null && activeBgmId != -1) {
                activeBgmSound.stop(activeBgmId);
            }
            activeBgmSound = desired;
            activeBgmPitch = desiredPitch;
            if (activeBgmSound != null) {
                activeBgmId = activeBgmSound.loop(bgmVolume);
                activeBgmSound.setPitch(activeBgmId, activeBgmPitch);
            }
        } else if (activeBgmSound != null && activeBgmId != -1) {
            activeBgmSound.setVolume(activeBgmId, bgmVolume);
            if (Math.abs(activeBgmPitch - desiredPitch) > 0.01f) {
                activeBgmPitch = desiredPitch;
                activeBgmSound.setPitch(activeBgmId, activeBgmPitch);
            }
        }
    }

    private void beginOrTriggerAction() {
        Player p = controller.getPlayer();
        if (p.getHeldItem() != null) {
            chargingThrow = true;
            throwCharge = 0.04f;
            return;
        }
        triggerActionOrMinigame();
    }

    private void releaseThrowOrPlace() {
        chargingThrow = false;
        if (controller.getPlayer().getHeldItem() != null) {
            if (throwCharge < 0.22f) {
                controller.interact();
                playSfx(assets.sfxGrab, 1.0f);
            } else {
                controller.throwHeldItem(throwCharge);
                playSfx(assets.sfxThrow, 0.85f + throwCharge * 0.4f);
            }
        }
        throwCharge = 0f;
    }

    private void playSfx(Sound sound, float pitchMultiplier) {
        if (sound != null && sfxVolume > 0.01f) {
            long id = sound.play(sfxVolume);
            sound.setPitch(id, pitchMultiplier);
        }
    }

    private void startVictoryCelebration() {
        chargingThrow = false;
        victoryCelebrationActive = true;
        victoryTimer = 3.2f;
        confetti.clear();
        Color[] palette = {
                Color.GOLD, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.ORANGE, Color.SCARLET, Color.WHITE
        };
        for (int i = 0; i < 130; i++) {
            float cx = (float) (Math.random() * V_WIDTH);
            float cy = (float) (-220 + Math.random() * 240);
            confetti.add(new ConfettiPiece(cx, cy, palette[i % palette.length]));
        }
        playSfx(assets.sfxVictoryFanfare, 1.0f);
    }

    private void startBlastDoorTransition(Runnable midPointAction) {
        chargingThrow = false;
        doorTransitionActive = true;
        doorClosing = true;
        doorProgress = 0f;
        doorHoldTimer = 24;
        onDoorClosedCallback = midPointAction;
        playSfx(assets.sfxDoorSlam, 1.0f);
    }

    private boolean isGameplayPausedByTransition() {
        return victoryCelebrationActive || doorTransitionActive || countdownTimer > 0f;
    }

    private void updateVolumeSliders(float mx, float my) {
        if (sliderBgmBar.contains(mx, my)) {
            bgmVolume = Math.max(0f, Math.min(1f, (mx - sliderBgmBar.x) / sliderBgmBar.width));
            syncBackgroundMusic();
        } else if (sliderSfxBar.contains(mx, my)) {
            sfxVolume = Math.max(0f, Math.min(1f, (mx - sliderSfxBar.x) / sliderSfxBar.width));
            playSfx(assets.sfxClick, 1.0f);
        }
    }

    private void launchMachineMinigame(WorkStation ws) {
        activeMinigameStation = ws;
        switch (ws.getType()) {
            case WASHER -> {
                washerValves.clear();
                for (int i = 0; i < 4; i++) {
                    float tx = 260 + (i % 2) * 220 + (float) (Math.random() * 60);
                    float ty = 195 + (i / 2) * 105 + (float) (Math.random() * 25);
                    washerValves.add(new Rectangle(tx, ty, 64, 54));
                }
            }
            case DISMANTLER -> {
                sawMarkerPos = 0.1f;
                sawMarkerDir = 1.0f;
                sawHitsRemaining = 3;
            }
            case NEUTRALIZER -> {
                plasmaHeat[0] = 0.85f;
                plasmaHeat[1] = 0.92f;
                plasmaHeat[2] = 0.78f;
            }
        }
        screenState = ScreenState.MINIGAME;
    }

    private void completeMachineMinigame() {
        if (activeMinigameStation != null) {
            if (activeMinigameStation.isBroken()) {
                activeMinigameStation.repairFull();
                controller.getPopups().add(new FloatingText(activeMinigameStation.getType().name() + " REPAIRED!",
                        activeMinigameStation.getX(), activeMinigameStation.getY() - 12, java.awt.Color.GREEN));
            } else {
                for (int i = 0; i < 6; i++) activeMinigameStation.boostProcess();
                controller.getPlayer().addPendingCredits(20);
            }
            playSfx(assets.sfxSuccess, 1.15f);
            activeMinigameStation = null;
        }
        screenState = ScreenState.PLAYING;
    }

    private void handleMouseDown(float mx, float my) {
        playSfx(assets.sfxClick, 1.0f);

        if (screenState == ScreenState.MAIN_MENU) {
            if (btnMenuStart.contains(mx, my)) {
                openWorldMap(!controller.hasActiveRun());
            } else if (btnMenuSaves.contains(mx, my)) {
                previousState = ScreenState.MAIN_MENU;
                screenState = ScreenState.SAVE_SLOTS;
            } else if (btnMenuCustomize.contains(mx, my)) {
                previousState = ScreenState.MAIN_MENU;
                screenState = ScreenState.CUSTOMIZE;
                syncBackgroundMusic();
            } else if (btnMenuShop.contains(mx, my)) {
                previousState = ScreenState.MAIN_MENU;
                screenState = ScreenState.SHOP;
            } else if (btnMenuOptions.contains(mx, my)) {
                previousState = ScreenState.MAIN_MENU;
                screenState = ScreenState.OPTIONS;
            } else if (btnMenuQuit.contains(mx, my)) {
                Gdx.app.exit();
            }
            return;
        }

        if (screenState == ScreenState.WORLD_MAP) {
            if (mapIntroTourActive) {
                mapIntroTourActive = false;
                selectedMapNode = Math.min(controller.getMaxUnlockedLevel(), controller.getCurrentLevelIndex());
                mapAvatarX = mapNodeX[selectedMapNode];
                mapAvatarY = mapNodeY[selectedMapNode];
                mapCameraX = Math.max(0f, Math.min(MAP_WORLD_WIDTH - V_WIDTH, mapAvatarX - V_WIDTH / 2f));
                return;
            }
            if (mapUnlockAnimTo != -1) return;

            // Handle "Start working in this facility?" Confirmation Modal!
            if (mapDeployPromptOpen) {
                Rectangle btnYes = new Rectangle(250, 290, 190, 44);
                Rectangle btnNo  = new Rectangle(460, 290, 190, 44);
                if (btnYes.contains(mx, my)) {
                    mapDeployPromptOpen = false;
                    final int chosen = selectedMapNode;
                    startBlastDoorTransition(() -> {
                        controller.selectLevel(chosen);
                        screenState = ScreenState.PLAYING;
                        syncBackgroundMusic();
                    });
                } else if (btnNo.contains(mx, my)) {
                    mapDeployPromptOpen = false;
                }
                return;
            }

            if (btnMapBack.contains(mx, my)) {
                screenState = ScreenState.MAIN_MENU;
                syncBackgroundMusic();
                return;
            }
            if (btnMapShop.contains(mx, my)) {
                previousState = ScreenState.WORLD_MAP;
                screenState = ScreenState.SHOP;
                return;
            }
            if (btnMapDeploy.contains(mx, my) && selectedMapNode <= controller.getMaxUnlockedLevel()) {
                mapDeployPromptOpen = true;
                return;
            }

            // Click on any unlocked node on the scrolling map to walk the Avatar there (or open prompt if already there!)
            float worldClickX = mx + mapCameraX;
            for (int i = 0; i < 20; i++) {
                if (Math.hypot(worldClickX - mapNodeX[i], my - mapNodeY[i]) <= 34) {
                    if (i <= controller.getMaxUnlockedLevel()) {
                        if (selectedMapNode == i) {
                            mapDeployPromptOpen = true;
                        } else {
                            selectedMapNode = i;
                        }
                        playSfx(assets.sfxGrab, 1.1f);
                    } else {
                        playSfx(assets.sfxError, 1.0f);
                    }
                    return;
                }
            }
            return;
        }

        if (screenState == ScreenState.RESTART_CONFIRM) {
            Rectangle btnYes = new Rectangle(255, 285, 185, 44);
            Rectangle btnNo  = new Rectangle(460, 285, 185, 44);
            if (btnYes.contains(mx, my)) {
                clearVictoryOverlayState();
                screenState = ScreenState.PLAYING;
                startBlastDoorTransition(() -> {
                    controller.resetGame();
                    gameOverSoundPlayed = false;
                    syncBackgroundMusic();
                });
            } else if (btnNo.contains(mx, my)) {
                screenState = previousState;
            }
            return;
        }

        if (screenState == ScreenState.SHIFT_COMPLETE) {
            if (btnPostNext.contains(mx, my)) {
                controller.completeShiftPromotion();
                openWorldMap(false);
            } else if (btnPostShop.contains(mx, my)) {
                previousState = ScreenState.SHIFT_COMPLETE;
                screenState = ScreenState.SHOP;
            } else if (btnPostCustomize.contains(mx, my)) {
                previousState = ScreenState.SHIFT_COMPLETE;
                screenState = ScreenState.CUSTOMIZE;
                syncBackgroundMusic();
            } else if (btnPostSaves.contains(mx, my)) {
                previousState = ScreenState.SHIFT_COMPLETE;
                screenState = ScreenState.SAVE_SLOTS;
            } else if (btnPostOptions.contains(mx, my)) {
                previousState = ScreenState.SHIFT_COMPLETE;
                screenState = ScreenState.OPTIONS;
            } else if (btnPostMenu.contains(mx, my)) {
                clearVictoryOverlayState();
                controller.completeShiftPromotion();
                screenState = ScreenState.MAIN_MENU;
                syncBackgroundMusic();
            }
            return;
        }

        if (screenState == ScreenState.PAUSED) {
            if (btnPauseResume.contains(mx, my)) {
                screenState = ScreenState.PLAYING;
            } else if (btnPauseOptions.contains(mx, my)) {
                previousState = ScreenState.PAUSED;
                screenState = ScreenState.OPTIONS;
            } else if (btnPauseSaves.contains(mx, my)) {
                previousState = ScreenState.PAUSED;
                screenState = ScreenState.SAVE_SLOTS;
            } else if (btnPauseRestart.contains(mx, my)) {
                previousState = ScreenState.PAUSED;
                screenState = ScreenState.RESTART_CONFIRM;
            } else if (btnPauseMenu.contains(mx, my)) {
                clearVictoryOverlayState();
                screenState = ScreenState.MAIN_MENU;
                syncBackgroundMusic();
            }
            return;
        }

        if (screenState == ScreenState.SAVE_SLOTS) {
            if (pendingConfirmAction != ConfirmAction.NONE) {
                Rectangle btnConfirmYes = new Rectangle(265, 285, 175, 42);
                Rectangle btnConfirmNo  = new Rectangle(460, 285, 175, 42);
                if (btnConfirmYes.contains(mx, my)) {
                    int targetSlot = pendingConfirmSlot;
                    ConfirmAction act = pendingConfirmAction;
                    pendingConfirmAction = ConfirmAction.NONE;
                    pendingConfirmSlot = -1;

                    if (act == ConfirmAction.LOAD_SLOT) {
                        SaveManager.loadSlot(targetSlot, controller);
                        playSfx(assets.sfxSuccess, 1.0f);
                        openWorldMap(false);
                    } else if (act == ConfirmAction.SAVE_OVERWRITE) {
                        SaveManager.saveSlot(targetSlot, controller);
                        playSfx(assets.sfxSuccess, 1.15f);
                    } else if (act == ConfirmAction.DELETE_SLOT) {
                        SaveManager.deleteSlot(targetSlot);
                        playSfx(assets.sfxError, 0.95f);
                    }
                } else if (btnConfirmNo.contains(mx, my)) {
                    pendingConfirmAction = ConfirmAction.NONE;
                    pendingConfirmSlot = -1;
                }
                return;
            }

            for (int slot = 1; slot <= 5; slot++) {
                float rowY = 118 + (slot - 1) * 58;
                final int targetSlot = slot;

                if (!SaveManager.slotExists(slot)) {
                    Rectangle btnCreateNew = new Rectangle(545, rowY + 6, 245, 38);
                    if (btnCreateNew.contains(mx, my)) {
                        SaveManager.createNewSlot(targetSlot, controller);
                        playSfx(assets.sfxSuccess, 1.1f);
                        mapIntroPlayedThisSession = false;
                        openWorldMap(true);
                        return;
                    }
                } else {
                    Rectangle btnLoadSlot = new Rectangle(515, rowY + 6, 86, 38);
                    Rectangle btnSaveSlot = new Rectangle(608, rowY + 6, 86, 38);
                    Rectangle btnDelSlot  = new Rectangle(701, rowY + 6, 89, 38);

                    if (btnLoadSlot.contains(mx, my)) {
                        pendingConfirmSlot = targetSlot;
                        pendingConfirmAction = ConfirmAction.LOAD_SLOT;
                        return;
                    } else if (btnSaveSlot.contains(mx, my)) {
                        pendingConfirmSlot = targetSlot;
                        pendingConfirmAction = ConfirmAction.SAVE_OVERWRITE;
                        return;
                    } else if (btnDelSlot.contains(mx, my)) {
                        pendingConfirmSlot = targetSlot;
                        pendingConfirmAction = ConfirmAction.DELETE_SLOT;
                        return;
                    }
                }
            }
            if (new Rectangle(310, 425, 280, 44).contains(mx, my)) {
                screenState = previousState;
            }
            return;
        }

        if (screenState == ScreenState.OPTIONS) {
            updateVolumeSliders(mx, my);
            if (btnToggleAim.contains(mx, my)) {
                mouseAimEnabled = !mouseAimEnabled;
                controller.getPlayer().setKeyboardAimMode(!mouseAimEnabled);
                playSfx(assets.sfxSuccess, 1.0f);
            } else if (btnToggleFull.contains(mx, my)) {
                toggleBorderlessFullscreen();
                playSfx(assets.sfxSuccess, 1.1f);
            } else if (new Rectangle(280, 355, 160, 40).contains(mx, my)) {
                bgmVolume = 0f;
                sfxVolume = 0f;
                syncBackgroundMusic();
            } else if (new Rectangle(460, 355, 160, 40).contains(mx, my)) {
                bgmVolume = 0.45f;
                sfxVolume = 0.75f;
                syncBackgroundMusic();
                playSfx(assets.sfxSuccess, 1.0f);
            } else if (new Rectangle(310, 420, 280, 44).contains(mx, my)) {
                screenState = previousState;
            }
            return;
        }

        if (screenState == ScreenState.CUSTOMIZE) {
            OperatorCosmetics cos = controller.getPlayer().getCosmetics();
            if (new Rectangle(420, 165, 340, 40).contains(mx, my)) cos.cycleHardhat();
            else if (new Rectangle(420, 215, 340, 40).contains(mx, my)) cos.cycleSuit();
            else if (new Rectangle(420, 265, 340, 40).contains(mx, my)) cos.cycleSkin();
            else if (new Rectangle(420, 315, 340, 40).contains(mx, my)) cos.cycleVisor();
            else if (new Rectangle(290, 425, 320, 46).contains(mx, my)) {
                screenState = previousState;
                syncBackgroundMusic();
            }
            SaveManager.save(controller);
            return;
        }

        if (screenState == ScreenState.SHOP) {
            Player p = controller.getPlayer();
            UpgradeShop shop = controller.getShop();
            CleanerBot bot = controller.getCleanerBot();

            // Row 1: Boots, Row 2: Station Overclock, Row 3: Bay Hoppers, Row 4 (Bottom): Assistant CleanerBot ($50k)!
            if (new Rectangle(85, 128, 730, 62).contains(mx, my)) {
                if (shop.buyBootsUpgrade(p)) playSfx(assets.sfxSuccess, 1.0f);
                else playSfx(assets.sfxError, 1.0f);
            } else if (new Rectangle(85, 198, 730, 62).contains(mx, my)) {
                if (shop.buyStationUpgrade(p)) playSfx(assets.sfxSuccess, 1.0f);
                else playSfx(assets.sfxError, 1.0f);
            } else if (new Rectangle(85, 268, 730, 62).contains(mx, my)) {
                if (shop.buyHopperUpgrade(p, controller.getBays())) playSfx(assets.sfxSuccess, 1.0f);
                else playSfx(assets.sfxError, 1.0f);
            } else if (new Rectangle(85, 338, 730, 66).contains(mx, my)) {
                if (shop.buyBotUpgrade(p, bot)) playSfx(assets.sfxSuccess, 1.15f);
                else playSfx(assets.sfxError, 1.0f);
            } else if (new Rectangle(310, 422, 280, 44).contains(mx, my)) {
                screenState = previousState;
            }
            SaveManager.save(controller);
            return;
        }

        if (screenState == ScreenState.MINIGAME && activeMinigameStation != null) {
            switch (activeMinigameStation.getType()) {
                case WASHER -> {
                    Iterator<Rectangle> it = washerValves.iterator();
                    while (it.hasNext()) {
                        if (it.next().contains(mx, my)) {
                            it.remove();
                            playSfx(assets.sfxGrab, 1.25f);
                            if (washerValves.isEmpty()) completeMachineMinigame();
                            return;
                        }
                    }
                }
                case DISMANTLER -> {
                    if (new Rectangle(320, 315, 260, 48).contains(mx, my)) {
                        if (sawMarkerPos >= 0.36f && sawMarkerPos <= 0.64f) {
                            sawHitsRemaining--;
                            playSfx(assets.sfxGrab, 1.3f);
                            if (sawHitsRemaining <= 0) completeMachineMinigame();
                        } else {
                            playSfx(assets.sfxError, 1.0f);
                        }
                    }
                }
                case NEUTRALIZER -> {
                    for (int i = 0; i < 3; i++) {
                        Rectangle coolBtn = new Rectangle(275 + i * 125, 325, 100, 42);
                        if (coolBtn.contains(mx, my)) {
                            plasmaHeat[i] = Math.max(0f, plasmaHeat[i] - 0.35f);
                            playSfx(assets.sfxGrab, 1.15f);
                            if (plasmaHeat[0] <= 0.25f && plasmaHeat[1] <= 0.25f && plasmaHeat[2] <= 0.25f) {
                                completeMachineMinigame();
                            }
                            return;
                        }
                    }
                }
            }
            return;
        }

        if (controller.isGameOver() || controller.isGameWon()) {
            if (btnDebriefRetry.contains(mx, my)) {
                previousState = ScreenState.PLAYING;
                screenState = ScreenState.RESTART_CONFIRM;
            } else if (btnDebriefShop.contains(mx, my)) {
                previousState = ScreenState.PLAYING;
                screenState = ScreenState.SHOP;
            } else if (btnDebriefMenu.contains(mx, my)) {
                clearVictoryOverlayState();
                screenState = ScreenState.MAIN_MENU;
                syncBackgroundMusic();
            }
            return;
        }

        if (btnHudShop.contains(mx, my)) { previousState = ScreenState.PLAYING; screenState = ScreenState.SHOP; return; }
        if (btnHudSaves.contains(mx, my)) { previousState = ScreenState.PLAYING; screenState = ScreenState.SAVE_SLOTS; return; }
        if (btnHudCustomize.contains(mx, my)) {
            previousState = ScreenState.PLAYING;
            screenState = ScreenState.CUSTOMIZE;
            syncBackgroundMusic();
            return;
        }
        if (btnHudOptions.contains(mx, my)) { previousState = ScreenState.PLAYING; screenState = ScreenState.OPTIONS; return; }
        if (btnHudPause.contains(mx, my)) { screenState = ScreenState.PAUSED; return; }

        if (isGameplayPausedByTransition()) return;

        if (mobileHudEnabled) {
            if (new Rectangle(760, 360, 110, 52).contains(mx, my)) {
                beginOrTriggerAction();
                return;
            }
            if (new Rectangle(640, 360, 105, 52).contains(mx, my)) {
                controller.getPlayer().triggerDash();
                return;
            }
        }

        if (mouseAimEnabled) {
            controller.getPlayer().aimAt(mx, my);
        }
        beginOrTriggerAction();
    }

    private void handleKeyPress(int keycode) {
        // Global [F11] Hotkey for Borderless Fullscreen from anywhere in the game!
        if (keycode == Input.Keys.F11) {
            toggleBorderlessFullscreen();
            playSfx(assets.sfxClick, 1.1f);
            return;
        }

        if (keycode == Input.Keys.ESCAPE) {
            chargingThrow = false;
            if (victoryCelebrationActive) {
                clearVictoryOverlayState();
                screenState = ScreenState.SHIFT_COMPLETE;
                return;
            }
            if (screenState == ScreenState.WORLD_MAP && mapDeployPromptOpen) {
                mapDeployPromptOpen = false;
                return;
            }
            if (screenState == ScreenState.PLAYING) {
                screenState = ScreenState.PAUSED;
            } else if (screenState == ScreenState.PAUSED) {
                screenState = ScreenState.PLAYING;
            } else if (screenState == ScreenState.WORLD_MAP) {
                screenState = ScreenState.MAIN_MENU;
                syncBackgroundMusic();
            } else if (screenState == ScreenState.OPTIONS || screenState == ScreenState.SAVE_SLOTS
                    || screenState == ScreenState.SHOP || screenState == ScreenState.CUSTOMIZE
                    || screenState == ScreenState.RESTART_CONFIRM) {
                screenState = previousState;
                syncBackgroundMusic();
            }
            return;
        }

        if (keycode == Input.Keys.O) {
            chargingThrow = false;
            if (screenState == ScreenState.OPTIONS) {
                screenState = previousState;
            } else {
                previousState = screenState;
                screenState = ScreenState.OPTIONS;
            }
            return;
        }

        if (screenState == ScreenState.WORLD_MAP) {
            if (mapIntroTourActive) {
                mapIntroTourActive = false;
                selectedMapNode = Math.min(controller.getMaxUnlockedLevel(), controller.getCurrentLevelIndex());
                mapAvatarX = mapNodeX[selectedMapNode];
                mapAvatarY = mapNodeY[selectedMapNode];
                mapCameraX = Math.max(0f, Math.min(MAP_WORLD_WIDTH - V_WIDTH, mapAvatarX - V_WIDTH / 2f));
                return;
            }
            if (mapUnlockAnimTo != -1) return;

            if (mapDeployPromptOpen) {
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                    mapDeployPromptOpen = false;
                    final int chosen = selectedMapNode;
                    startBlastDoorTransition(() -> {
                        controller.selectLevel(chosen);
                        screenState = ScreenState.PLAYING;
                        syncBackgroundMusic();
                    });
                }
                return;
            }

            if (keycode == Input.Keys.D || keycode == Input.Keys.RIGHT || keycode == Input.Keys.W || keycode == Input.Keys.UP) {
                if (selectedMapNode < controller.getMaxUnlockedLevel()) {
                    selectedMapNode++;
                    playSfx(assets.sfxGrab, 1.1f);
                }
            } else if (keycode == Input.Keys.A || keycode == Input.Keys.LEFT || keycode == Input.Keys.S || keycode == Input.Keys.DOWN) {
                if (selectedMapNode > 0) {
                    selectedMapNode--;
                    playSfx(assets.sfxGrab, 0.95f);
                }
            } else if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                mapDeployPromptOpen = true;
                playSfx(assets.sfxClick, 1.1f);
            }
            return;
        }

        if (keycode == Input.Keys.O) {
            chargingThrow = false;
            if (screenState == ScreenState.OPTIONS) {
                screenState = previousState;
            } else {
                previousState = screenState;
                screenState = ScreenState.OPTIONS;
            }
            return;
        }

        if (screenState == ScreenState.WORLD_MAP) {
            if (mapIntroTourActive) {
                mapIntroTourActive = false;
                selectedMapNode = Math.min(controller.getMaxUnlockedLevel(), controller.getCurrentLevelIndex());
                mapAvatarX = mapNodeX[selectedMapNode];
                mapAvatarY = mapNodeY[selectedMapNode];
                return;
            }
            if (mapUnlockAnimTo != -1) return;

            if (keycode == Input.Keys.D || keycode == Input.Keys.RIGHT || keycode == Input.Keys.W || keycode == Input.Keys.UP) {
                if (selectedMapNode < controller.getMaxUnlockedLevel()) {
                    selectedMapNode++;
                    playSfx(assets.sfxGrab, 1.1f);
                }
            } else if (keycode == Input.Keys.A || keycode == Input.Keys.LEFT || keycode == Input.Keys.S || keycode == Input.Keys.DOWN) {
                if (selectedMapNode > 0) {
                    selectedMapNode--;
                    playSfx(assets.sfxGrab, 0.95f);
                }
            } else if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                final int chosen = selectedMapNode;
                startBlastDoorTransition(() -> {
                    controller.selectLevel(chosen);
                    screenState = ScreenState.PLAYING;
                    syncBackgroundMusic();
                });
            }
            return;
        }

        if (screenState == ScreenState.CUSTOMIZE) {
            OperatorCosmetics cos = controller.getPlayer().getCosmetics();
            if (keycode == Input.Keys.NUM_1) cos.cycleHardhat();
            else if (keycode == Input.Keys.NUM_2) cos.cycleSuit();
            else if (keycode == Input.Keys.NUM_3) cos.cycleSkin();
            else if (keycode == Input.Keys.NUM_4) cos.cycleVisor();
            else if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                screenState = previousState;
                syncBackgroundMusic();
            }
            SaveManager.save(controller);
            return;
        }

        if (screenState == ScreenState.SHOP || screenState == ScreenState.OPTIONS || screenState == ScreenState.SAVE_SLOTS) {
            if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE || keycode == Input.Keys.B) {
                screenState = previousState;
                syncBackgroundMusic();
            }
            return;
        }

        if (screenState == ScreenState.PLAYING) {
            if (keycode == Input.Keys.R) {
                chargingThrow = false;
                previousState = ScreenState.PLAYING;
                screenState = ScreenState.RESTART_CONFIRM;
                return;
            }

            if (controller.isGameOver() || controller.isGameWon()) return;

            if (keycode == Input.Keys.B) { previousState = ScreenState.PLAYING; screenState = ScreenState.SHOP; return; }
            if (keycode == Input.Keys.C) {
                previousState = ScreenState.PLAYING;
                screenState = ScreenState.CUSTOMIZE;
                syncBackgroundMusic();
                return;
            }
            if (keycode == Input.Keys.L) { previousState = ScreenState.PLAYING; screenState = ScreenState.SAVE_SLOTS; return; }
            if (keycode == Input.Keys.M) { mobileHudEnabled = !mobileHudEnabled; return; }

            if (isGameplayPausedByTransition()) return;

            if (keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT) {
                controller.getPlayer().triggerDash();
            } else if (keycode == Input.Keys.SPACE || keycode == Input.Keys.E) {
                beginOrTriggerAction();
            }
        }
    }

    private void triggerActionOrMinigame() {
        Player p = controller.getPlayer();
        if (p.getHeldItem() == null) {
            for (WorkStation ws : controller.getStations()) {
                if (ws.isNear(p.getX(), p.getY())) {
                    if (ws.isBroken() || (!ws.isEmpty() && !ws.isFinished() && !ws.isWrongItemProcessing())) {
                        launchMachineMinigame(ws);
                        return;
                    }
                }
            }
        }

        boolean wasHolding = p.getHeldItem() != null;
        controller.interact();
        if (wasHolding && p.getHeldItem() == null) {
            playSfx(assets.sfxThrow, 1.0f);
        } else if (!wasHolding && p.getHeldItem() != null) {
            playSfx(assets.sfxGrab, 1.0f);
        }
    }

    @Override
    public void render() {
        updateGameLoop();

        Gdx.gl.glClearColor(0.08f, 0.11f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.position.set(V_WIDTH / 2f, V_HEIGHT / 2f, 0);
        if (screenState == ScreenState.PLAYING && controller.getScreenShake() > 0) {
            camera.position.x += (float) ((Math.random() - 0.5) * 8);
            camera.position.y += (float) ((Math.random() - 0.5) * 8);
        }
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        switch (screenState) {
            case MAIN_MENU -> renderOvercookedMainMenu();
            case WORLD_MAP -> renderWorldMapScreen();
            case PAUSED -> {
                renderFactoryStage();
                renderPauseModal();
            }
            case RESTART_CONFIRM -> {
                renderFactoryStage();
                renderRestartConfirmModal();
            }
            case SHIFT_COMPLETE -> {
                renderFactoryStage();
                renderShiftCompleteModal();
            }
            case SAVE_SLOTS -> {
                if (previousState == ScreenState.MAIN_MENU) renderOvercookedMainMenu();
                else if (previousState == ScreenState.WORLD_MAP) renderWorldMapScreen();
                else renderFactoryStage();
                renderSaveSlotsModal();
            }
            case CUSTOMIZE -> renderCustomizeScreen();
            case SHOP -> renderShop();
            case OPTIONS -> {
                if (previousState == ScreenState.MAIN_MENU) renderOvercookedMainMenu();
                else if (previousState == ScreenState.WORLD_MAP) renderWorldMapScreen();
                else renderFactoryStage();
                renderOptionsModal();
            }
            case PLAYING -> renderFactoryStage();
            case MINIGAME -> {
                renderFactoryStage();
                renderMinigameOverlay();
            }
        }

        if (victoryCelebrationActive && screenState == ScreenState.PLAYING) {
            renderVictoryCelebrationOverlay();
        }

        if (doorTransitionActive || (countdownTimer > 0f && screenState == ScreenState.PLAYING)) {
            renderBlastDoorsAndCountdown();
        }
        batch.end();
    }

    private void updateGameLoop() {
        menuAnimTick += 0.16f;

        // 1. Always tick Hydraulic Blast Door Transition FIRST so transitions from the World Map never freeze!
        if (doorTransitionActive) {
            if (doorClosing) {
                doorProgress = Math.min(1f, doorProgress + 0.055f);
                if (doorProgress >= 1f) {
                    if (onDoorClosedCallback != null) {
                        onDoorClosedCallback.run();
                        onDoorClosedCallback = null;
                    }
                    doorHoldTimer--;
                    if (doorHoldTimer <= 0) {
                        doorClosing = false;
                        playSfx(assets.sfxGrab, 0.75f);
                    }
                }
            } else {
                doorProgress = Math.max(0f, doorProgress - 0.055f);
                if (doorProgress <= 0f) {
                    doorTransitionActive = false;
                    if (!controller.isGameWon() && screenState == ScreenState.PLAYING) {
                        countdownTimer = 3.4f;
                        playSfx(assets.sfxClick, 1.2f);
                    }
                }
            }
            return;
        }

        // 2. Update Scrolling World Map Camera Tour & Avatar Walking
        if (screenState == ScreenState.WORLD_MAP) {
            float maxCam = MAP_WORLD_WIDTH - V_WIDTH;
            if (mapIntroTourActive) {
                // Smoothly pan the camera across the entire 20-facility map and back while the Avatar waits at Facility 1!
                mapIntroTourProgress += 0.0032f;
                if (mapIntroTourProgress < 1.0f) {
                    float smooth = (float) (0.5 - 0.5 * Math.cos(mapIntroTourProgress * Math.PI));
                    mapCameraX = smooth * maxCam;
                } else if (mapIntroTourProgress < 2.0f) {
                    float back = mapIntroTourProgress - 1.0f;
                    float smooth = (float) (0.5 - 0.5 * Math.cos(back * Math.PI));
                    float targetCam = Math.max(0f, Math.min(maxCam, mapAvatarX - V_WIDTH / 2f));
                    mapCameraX = maxCam - smooth * (maxCam - targetCam);
                } else {
                    mapIntroTourActive = false;
                }
            } else if (mapUnlockAnimTo != -1) {
                mapUnlockAnimProgress += 0.018f;
                float t = Math.min(1f, mapUnlockAnimProgress);
                mapAvatarX = mapNodeX[mapUnlockAnimFrom] + (mapNodeX[mapUnlockAnimTo] - mapNodeX[mapUnlockAnimFrom]) * t;
                mapAvatarY = mapNodeY[mapUnlockAnimFrom] + (mapNodeY[mapUnlockAnimTo] - mapNodeY[mapUnlockAnimFrom]) * t;
                float desiredCam = Math.max(0f, Math.min(maxCam, mapAvatarX - V_WIDTH / 2f));
                mapCameraX += (desiredCam - mapCameraX) * 0.15f;
                if (mapUnlockAnimProgress >= 1f) {
                    selectedMapNode = mapUnlockAnimTo;
                    mapUnlockAnimFrom = -1;
                    mapUnlockAnimTo = -1;
                    playSfx(assets.sfxSuccess, 1.2f);
                }
            } else {
                // Avatar walks smoothly along the road to the selected facility while the camera follows!
                mapAvatarX += (mapNodeX[selectedMapNode] - mapAvatarX) * 0.14f;
                mapAvatarY += (mapNodeY[selectedMapNode] - mapAvatarY) * 0.14f;
                float desiredCam = Math.max(0f, Math.min(maxCam, mapAvatarX - V_WIDTH / 2f));
                mapCameraX += (desiredCam - mapCameraX) * 0.10f;
            }
            return;
        }

        if (screenState == ScreenState.MINIGAME && activeMinigameStation != null) {
            if (activeMinigameStation.getType() == StationType.DISMANTLER) {
                sawMarkerPos += sawMarkerDir * 0.022f;
                if (sawMarkerPos >= 0.95f) { sawMarkerPos = 0.95f; sawMarkerDir = -1f; }
                if (sawMarkerPos <= 0.05f) { sawMarkerPos = 0.05f; sawMarkerDir = 1f; }
            } else if (activeMinigameStation.getType() == StationType.NEUTRALIZER) {
                for (int i = 0; i < 3; i++) {
                    plasmaHeat[i] = Math.min(1.0f, plasmaHeat[i] + 0.0025f);
                }
            }
            return;
        }

        if (screenState != ScreenState.PLAYING) return;

        int speedStage = controller.getTimerSpeedStage();
        beltAnimOffset = (beltAnimOffset + 1 + speedStage) % 24;
        syncBackgroundMusic();

        if (victoryCelebrationActive) {
            for (ConfettiPiece cp : confetti) {
                cp.y += cp.vy;
                cp.x += cp.vx + (float) Math.sin(menuAnimTick * 0.5f + cp.swayPhase) * 1.4f;
                if (cp.y > V_HEIGHT + 10) {
                    cp.y = -15;
                    cp.x = (float) (Math.random() * V_WIDTH);
                }
            }
            victoryTimer -= Gdx.graphics.getDeltaTime();
            if (victoryTimer <= 0f) {
                clearVictoryOverlayState();
                screenState = ScreenState.SHIFT_COMPLETE;
            }
            return;
        }

        if (countdownTimer > 0f) {
            float prevSec = (float) Math.ceil(countdownTimer);
            countdownTimer = Math.max(0f, countdownTimer - Gdx.graphics.getDeltaTime());
            float newSec = (float) Math.ceil(countdownTimer);
            if (newSec != prevSec && newSec > 0) {
                playSfx(assets.sfxClick, newSec == 1 ? 1.5f : 1.1f);
            }
            return;
        }

        if (chargingThrow) {
            if (controller.getPlayer().getHeldItem() == null) {
                chargingThrow = false;
                throwCharge = 0f;
            } else {
                throwCharge = Math.min(1.0f, throwCharge + 0.025f);
            }
        }

        double dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) dy -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) dy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dx -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx += 1;

        if (mobileHudEnabled && Gdx.input.isTouched()) {
            Vector3 world = viewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            float tx = world.x;
            float ty = V_HEIGHT - world.y;
            if (Math.hypot(tx - 95, ty - 385) <= 65) {
                dx = tx - 95;
                dy = ty - 385;
            }
        }

        controller.updateTick(dx, dy);

        if (mouseAimEnabled && !mobileHudEnabled && mouseX >= 0 && mouseY >= 0) {
            controller.getPlayer().aimAt(mouseX, mouseY);
        }

        if (controller.isShiftVictoryPending() && !victoryCelebrationActive) {
            startVictoryCelebration();
        }

        if (controller.isGameOver() && !gameOverSoundPlayed) {
            gameOverSoundPlayed = true;
            playSfx(assets.sfxGameOverTrack, 1.0f);
        } else if (!controller.isGameOver()) {
            gameOverSoundPlayed = false;
        }

        if (!controller.getLastFeedback().equals(lastSeenFeedback)) {
            lastSeenFeedback = controller.getLastFeedback();
            toastTimer = 190;
        } else if (toastTimer > 0) {
            toastTimer--;
        }
    }

    // Dynamic Scrolling NSMB World Map (2,850px wide with Biomes, Scenic Landmarks, & Confirmation Modal!)
    private void renderWorldMapScreen() {
        // 1. Deep Cebu Strait Ocean & Animated Waves
        drawBox(0, 0, V_WIDTH, V_HEIGHT, new Color(0.07f, 0.24f, 0.40f, 1f), Color.CLEAR);
        for (int wx = -((int) (mapCameraX * 0.5f) % 90); wx < V_WIDTH; wx += 90) {
            for (int wy = 75; wy < 415; wy += 65) {
                drawBox(wx + (wy % 2) * 30, wy, 28, 4, new Color(0.14f, 0.38f, 0.58f, 0.65f), Color.CLEAR);
            }
        }

        // 2. Scrolling Island Biomes (Coastal Green -> Mandaue Industrial Sand -> Harbor Pier -> Mountain -> Mactan Core)
        Color[] biomeCols = {
                new Color(0.18f, 0.48f, 0.30f, 1f),
                new Color(0.46f, 0.40f, 0.26f, 1f),
                new Color(0.16f, 0.38f, 0.44f, 1f),
                new Color(0.24f, 0.42f, 0.24f, 1f),
                new Color(0.38f, 0.22f, 0.26f, 1f)
        };
        for (int b = 0; b < 5; b++) {
            float bx = 80f + b * 540f - mapCameraX;
            if (bx + 530f < 0 || bx > V_WIDTH) continue;
            // Sandy shoreline border + biome landmass
            drawArcadePanel(bx - 8, 76, 526, 336, new Color(0.86f, 0.76f, 0.48f, 1f), new Color(0.12f, 0.18f, 0.24f, 1f));
            drawArcadePanel(bx, 84, 510, 320, biomeCols[b], new Color(0.72f, 0.64f, 0.38f, 1f));
        }

        // 3. Scenic Trees, Mountains & Factory Smokestacks across the scrolling landscape
        for (int d = 0; d < 40; d++) {
            float dx = (d * 72f + 110f) - mapCameraX;
            if (dx < -40 || dx > V_WIDTH + 40) continue;
            float dy = (d % 2 == 0) ? 102f : 362f;
            drawBox(dx, dy, 16, 18, new Color(0.12f, 0.32f, 0.18f, 1f), Color.BLACK);
            drawBox(dx - 4, dy - 10, 24, 14, new Color(0.22f, 0.62f, 0.32f, 1f), Color.BLACK);
        }

        // 4. Winding Brick/Cobblestone Highway Connecting Facilities 1 -> 20
        int maxUnl = controller.getMaxUnlockedLevel();
        for (int i = 0; i < 19; i++) {
            float x1 = mapNodeX[i] - mapCameraX, y1 = mapNodeY[i];
            float x2 = mapNodeX[i + 1] - mapCameraX, y2 = mapNodeY[i + 1];
            if (Math.max(x1, x2) < -50 || Math.min(x1, x2) > V_WIDTH + 50) continue;

            boolean unlockedRoad = (i < maxUnl) && !(mapUnlockAnimTo == i + 1);
            for (int s = 0; s <= 14; s++) {
                float t = s / 14f;
                float rx = x1 + (x2 - x1) * t;
                float ry = y1 + (y2 - y1) * t;
                Color roadCol;
                if (mapUnlockAnimTo == i + 1) {
                    roadCol = (t <= mapUnlockAnimProgress) ? Color.GOLD : new Color(0.25f, 0.30f, 0.35f, 0.85f);
                } else {
                    roadCol = unlockedRoad ? new Color(0.96f, 0.80f, 0.24f, 1f) : new Color(0.25f, 0.30f, 0.35f, 0.85f);
                }
                drawBox(rx - 6, ry - 6, 12, 12, roadCol, Color.BLACK);
            }
        }

        // 5. Draw Facility Buildings & Nodes (1 to 20)
        List<FacilityLevel> lvls = controller.getLevels();
        for (int i = 0; i < 20; i++) {
            float nx = mapNodeX[i] - mapCameraX;
            float ny = mapNodeY[i];
            if (nx < -60 || nx > V_WIDTH + 60) continue;

            boolean unlocked = (i <= maxUnl) && !(mapUnlockAnimTo == i && mapUnlockAnimProgress < 1f);
            boolean selected = (i == selectedMapNode) && !mapIntroTourActive;

            // Mini MRF Facility Building Roof & Chimney above each node
            Color roofCol = unlocked ? new Color(0.88f, 0.42f, 0.16f, 1f) : new Color(0.28f, 0.30f, 0.34f, 1f);
            drawBox(nx + 8, ny - 38, 7, 16, Color.DARK_GRAY, Color.BLACK);
            drawBox(nx - 18, ny - 26, 36, 10, roofCol, Color.BLACK);

            Color nodeCol = !unlocked
                    ? new Color(0.32f, 0.35f, 0.40f, 1f)
                    : (i < maxUnl ? new Color(0.18f, 0.82f, 0.45f, 1f) : new Color(0.20f, 0.68f, 0.98f, 1f));
            Color ringCol = selected ? Color.YELLOW : (unlocked ? Color.WHITE : Color.DARK_GRAY);

            float size = selected ? 44f : 36f;
            drawArcadePanel(nx - size / 2f, ny - size / 2f, size, size, nodeCol, ringCol);
            drawCenteredText(assets.fontSmall, String.valueOf(i + 1), nx, ny, Color.WHITE);

            // Floating Facility Name Tag under nearby nodes
            if (selected || Math.abs(nx - V_WIDTH / 2f) < 220f) {
                String shortName = lvls.get(i).getTitle();
                glyphLayout.setText(assets.fontSmall, shortName);
                float tagW = glyphLayout.width + 12f;
                drawBox(nx - tagW / 2f, ny + 26, tagW, 16, new Color(0.06f, 0.10f, 0.14f, 0.88f), selected ? Color.YELLOW : Color.GRAY);
                drawCenteredText(assets.fontSmall, shortName, nx, ny + 34, selected ? Color.YELLOW : Color.WHITE);
            }
        }

        // 6. Draw Player Avatar Standing on the Current Unlocked Node
        float bounce = (float) Math.abs(Math.sin(menuAnimTick * 0.8f)) * 5f;
        drawPlayerAvatar(mapAvatarX - mapCameraX, mapAvatarY - 22 - bounce, 0.95f, menuAnimTick, Direction.DOWN, 0f);

        // 7. Top Bar HUD
        drawArcadeButton(btnMapBack, "< MENU [ESC]", new Color(0.28f, 0.36f, 0.46f, 1f));
        drawArcadePanel(155, 12, 530, 42, new Color(0.10f, 0.16f, 0.24f, 0.95f), Color.GOLD);
        String mapHdr = mapIntroTourActive
                ? "PANNING MRF FACILITY NETWORK... (Click or press any key to skip)"
                : "MRF FACILITY WORLD MAP  --  Unlocked: " + (controller.getMaxUnlockedLevel() + 1) + "/20";
        drawCenteredText(assets.fontSmall, mapHdr, 420, 33, Color.GOLD);
        drawCurrencyBadge(700, 14, controller.getPlayer().getCredits());

        // 8. Bottom Bar HUD (Left-aligned text so it NEVER overlaps the buttons!)
        FacilityLevel selLvl = lvls.get(selectedMapNode);
        drawArcadePanel(20, 432, 860, 74, new Color(0.09f, 0.14f, 0.22f, 0.96f), Color.CYAN);

        assets.fontMedium.setColor(Color.YELLOW);
        assets.fontMedium.draw(batch, "FACILITY " + (selectedMapNode + 1) + ": " + selLvl.getTitle().toUpperCase(), 38, V_HEIGHT - 448);

        assets.fontSmall.setColor(Color.WHITE);
        String quotaInfo = "Quota: " + selLvl.getTrashQuota() + " Items  |  Time: 5:00  |  [A/D] or Click Node to Move";
        assets.fontSmall.draw(batch, quotaInfo, 38, V_HEIGHT - 478);

        drawArcadeButton(btnMapDeploy, "WORK IN FACILITY [ENTER]", new Color(0.18f, 0.80f, 0.44f, 1f));
        drawArcadeButton(btnMapShop,   "SHOP",                     new Color(0.92f, 0.56f, 0.12f, 1f));

        // 9. Facility Deployment Confirmation Modal ("Start working in this facility?")
        if (mapDeployPromptOpen) {
            drawBox(0, 0, V_WIDTH, V_HEIGHT, new Color(0f, 0f, 0f, 0.72f), Color.CLEAR);
            drawArcadePanel(210, 150, 480, 205, new Color(0.09f, 0.14f, 0.20f, 0.98f), Color.GOLD);
            drawCenteredText(assets.fontLarge, "DEPLOY TO FACILITY " + (selectedMapNode + 1) + "?", 450, 192, Color.GOLD);
            drawCenteredText(assets.fontMedium, selLvl.getTitle(), 450, 228, Color.CYAN);
            drawCenteredText(assets.fontSmall, "Forklift Quota: " + selLvl.getTrashQuota() + " Items   |   Time Limit: 5:00", 450, 260, Color.WHITE);

            drawArcadeButton(new Rectangle(250, 290, 190, 44), "YES, START [ENTER]", new Color(0.18f, 0.80f, 0.42f, 1f));
            drawArcadeButton(new Rectangle(460, 290, 190, 44), "STAY ON MAP [ESC]",  new Color(0.75f, 0.22f, 0.18f, 1f));
        }
    }

    private void renderVictoryCelebrationOverlay() {
        for (ConfettiPiece cp : confetti) {
            drawBox(cp.x, cp.y, 8, 5, cp.color, Color.BLACK);
        }

        String subMsg = controller.getCurrentLevel().getTitle() + " Cleared! Earned +$" + controller.getLastEarnedShiftCredits() + " Credits!";
        glyphLayout.setText(assets.fontMedium, subMsg);
        float bannerW = Math.max(540f, glyphLayout.width + 48f);
        drawArcadePanel(450f - bannerW / 2f, 110, bannerW, 95, new Color(0.10f, 0.16f, 0.22f, 0.96f), Color.GOLD);
        drawCenteredText(assets.fontLarge, controller.getCurrentLevel().getTitle().toUpperCase() + " COMPLETE!", 450, 145, Color.GOLD);
        drawCenteredText(assets.fontMedium, subMsg, 450, 182, Color.CYAN);
    }

    private void renderShiftCompleteModal() {
        drawBox(0, 0, V_WIDTH, V_HEIGHT, new Color(0f, 0f, 0f, 0.78f), Color.CLEAR);
        drawArcadePanel(155, 55, 590, 395, new Color(0.09f, 0.14f, 0.19f, 0.98f), Color.GOLD);

        FacilityLevel lvl = controller.getCurrentLevel();
        drawCenteredText(assets.fontLarge, lvl.getTitle().toUpperCase() + " COMPLETE!", 450, 100, Color.GOLD);

        String scoreLine = "Facility Score: " + controller.getPlayer().getScore()
                + "   |   Trash Transported: " + controller.getTransportedTrash() + "/" + lvl.getTrashQuota();
        drawCenteredText(assets.fontMedium, scoreLine, 450, 148, Color.WHITE);

        String creditLine = "Facility Payout: +$" + controller.getLastEarnedShiftCredits()
                + "   |   Total Balance: $" + controller.getPlayer().getCredits();
        drawCenteredText(assets.fontMedium, creditLine, 450, 185, Color.GREEN);
        drawCenteredText(assets.fontSmall, "Upgrades purchased in the Shop are now active! Choose your next action:", 450, 222, Color.CYAN);

        boolean hasNext = controller.getCurrentLevelIndex() < 19;
        drawArcadeButton(btnPostNext, hasNext ? "WORLD MAP (UNLOCK NEXT) >>" : "FINISH CAMPAIGN!", new Color(0.18f, 0.80f, 0.42f, 1f));
        drawArcadeButton(btnPostShop, "UPGRADE SHOP", new Color(0.88f, 0.55f, 0.10f, 1f));
        drawArcadeButton(btnPostCustomize, "CUSTOMIZE", new Color(0.20f, 0.55f, 0.82f, 1f));
        drawArcadeButton(btnPostSaves, "SAVES", new Color(0.18f, 0.68f, 0.52f, 1f));
        drawArcadeButton(btnPostOptions, "OPTIONS", new Color(0.55f, 0.27f, 0.68f, 1f));
        drawArcadeButton(btnPostMenu, "EXIT TO MAIN MENU", new Color(0.75f, 0.22f, 0.18f, 1f));
    }

    private void renderRestartConfirmModal() {
        drawBox(0, 0, V_WIDTH, V_HEIGHT, new Color(0f, 0f, 0f, 0.76f), Color.CLEAR);
        drawArcadePanel(200, 155, 500, 195, new Color(0.10f, 0.14f, 0.19f, 0.98f), Color.ORANGE);
        String facName = controller.getCurrentLevel().getTitle();
        drawCenteredText(assets.fontLarge, "RESTART CURRENT FACILITY?", 450, 198, Color.ORANGE);
        drawCenteredText(assets.fontSmall, "Restarting will reset your current shift in " + facName + ".", 450, 242, Color.WHITE);

        drawArcadeButton(new Rectangle(255, 285, 185, 44), "YES, RESTART", new Color(0.85f, 0.25f, 0.18f, 1f));
        drawArcadeButton(new Rectangle(460, 285, 185, 44), "CANCEL [ESC]", new Color(0.30f, 0.36f, 0.42f, 1f));
    }

    private void renderBlastDoorsAndCountdown() {
        if (doorTransitionActive && doorProgress > 0f) {
            float halfH = (V_HEIGHT / 2f) * doorProgress;
            drawArcadePanel(0, 0, V_WIDTH, halfH, new Color(0.22f, 0.26f, 0.30f, 1f), new Color(0.12f, 0.14f, 0.16f, 1f));
            drawArcadePanel(0, V_HEIGHT - halfH, V_WIDTH, halfH, new Color(0.22f, 0.26f, 0.30f, 1f), new Color(0.12f, 0.14f, 0.16f, 1f));

            for (int x = 0; x < V_WIDTH; x += 40) {
                drawBox(x, halfH - 18, 22, 18, new Color(0.95f, 0.75f, 0.1f, 1f), Color.BLACK);
                drawBox(x + 20, V_HEIGHT - halfH, 22, 18, new Color(0.95f, 0.75f, 0.1f, 1f), Color.BLACK);
            }

            if (doorProgress >= 0.85f) {
                FacilityLevel lvl = controller.getCurrentLevel();
                drawArcadePanel(210, 195, 480, 125, new Color(0.1f, 0.14f, 0.18f, 0.98f), getStageAccentColor());
                drawCenteredText(assets.fontLarge, lvl.getTitle().toUpperCase(), 450, 238, Color.GOLD);
                drawCenteredText(assets.fontMedium, "Quota: " + lvl.getTrashQuota() + " Items  |  Time Limit: 5:00", 450, 282, Color.CYAN);
            }
        } else if (countdownTimer > 0f) {
            drawBox(0, 0, V_WIDTH, V_HEIGHT, new Color(0f, 0f, 0f, 0.38f), Color.CLEAR);
            int sec = (int) Math.ceil(countdownTimer);
            String text = (sec <= 1 && countdownTimer < 0.7f) ? "GO!" : String.valueOf(sec - 1 > 0 ? sec - 1 : 1);
            Color badgeCol = text.equals("GO!") ? new Color(0.18f, 0.85f, 0.44f, 1f) : new Color(0.98f, 0.75f, 0.12f, 1f);

            drawArcadePanel(290, 195, 320, 115, new Color(0.12f, 0.16f, 0.22f, 0.95f), badgeCol);
            drawCenteredText(assets.fontLarge, text, 450, 240, badgeCol);
            drawCenteredText(assets.fontSmall, controller.getCurrentLevel().getTitle(), 450, 282, Color.WHITE);
        }
    }

    private void renderOvercookedMainMenu() {
        drawTiledFloor();

        drawArcadePanel(355, 38, 510, 444, new Color(0.11f, 0.16f, 0.22f, 0.95f), getStageAccentColor());
        drawArcadePanel(355, 38, 510, 38, new Color(0.92f, 0.48f, 0.14f, 1f), Color.WHITE);
        drawCenteredText(assets.fontSmall, "LIVE FACILITY PREVIEW (SAVE " + SaveManager.getActiveSlot() + ")  --  " + controller.getCurrentLevel().getTitle().toUpperCase(), 610, 57, Color.WHITE);

        // Separate Total Currency Balance Badge in the Top-Right of Preview Window
        drawCurrencyBadge(685, 86, controller.getPlayer().getCredits());

        // Only show CleanerBot in the menu if unlocked (level > 0); otherwise center the Avatar!
        float patrolOffset = (float) (Math.sin(menuAnimTick * 0.25f) * 24f);
        if (controller.getCleanerBot().getLevel() > 0) {
            drawPlayerAvatar(525 + patrolOffset, 285, 2.1f, menuAnimTick, Direction.DOWN, 0f);
            drawCleanerBot(695 - patrolOffset, 292, 1.85f, menuAnimTick * 1.2f, 0f);
        } else {
            drawPlayerAvatar(610 + patrolOffset, 285, 2.25f, menuAnimTick, Direction.DOWN, 0f);
        }

        drawArcadePanel(38, 38, 296, 444, new Color(0.96f, 0.95f, 0.92f, 1f), new Color(0.25f, 0.30f, 0.38f, 1f));
        drawArcadePanel(52, 48, 268, 115, new Color(0.14f, 0.24f, 0.36f, 1f), new Color(0.98f, 0.80f, 0.18f, 1f));
        drawCenteredText(assets.fontLarge, "SCRAPYARD", 186, 76, new Color(0.98f, 0.80f, 0.18f, 1f));
        drawCenteredText(assets.fontLarge, "SHIFT", 186, 112, Color.WHITE);
        drawCenteredText(assets.fontSmall, "Manage 20 MRF Recycling Facilities!", 186, 144, Color.CYAN);

        String startOrContinue = controller.hasActiveRun() ? "CONTINUE" : "START";
        drawOvercookedMenuOption(btnMenuStart,     startOrContinue);
        drawOvercookedMenuOption(btnMenuSaves,     "SAVES");
        drawOvercookedMenuOption(btnMenuCustomize, "CUSTOMIZE");
        drawOvercookedMenuOption(btnMenuShop,      "UPGRADE SHOP");
        drawOvercookedMenuOption(btnMenuOptions,   "OPTIONS");
        drawOvercookedMenuOption(btnMenuQuit,      "EXIT TO DESKTOP");
    }

    private void drawOvercookedMenuOption(Rectangle rect, String label) {
        boolean hovered = rect.contains(mouseX, mouseY);
        Color bg = hovered ? new Color(0.98f, 0.56f, 0.12f, 1f) : new Color(0.88f, 0.86f, 0.82f, 1f);
        Color border = hovered ? Color.YELLOW : new Color(0.62f, 0.60f, 0.56f, 1f);
        Color textCol = hovered ? Color.WHITE : new Color(0.16f, 0.18f, 0.22f, 1f);

        float drawX = hovered ? rect.x + 4 : rect.x;
        drawArcadePanel(drawX, rect.y, rect.width, rect.height, bg, border);
        drawCenteredText(assets.fontMedium, (hovered ? "> " : "") + label, drawX + rect.width / 2f, rect.y + rect.height / 2f, textCol);
    }

    private void renderPauseModal() {
        drawBox(0, 0, V_WIDTH, V_HEIGHT, new Color(0f, 0f, 0f, 0.72f), Color.CLEAR);
        drawArcadePanel(250, 85, 400, 345, new Color(0.10f, 0.15f, 0.22f, 0.98f), Color.CYAN);
        drawCenteredText(assets.fontLarge, "FACILITY PAUSED", 450, 122, Color.YELLOW);

        drawArcadeButton(btnPauseResume,  "RESUME [ESC]",      new Color(0.18f, 0.78f, 0.42f, 1f));
        drawArcadeButton(btnPauseOptions, "OPTIONS",           new Color(0.55f, 0.27f, 0.68f, 1f));
        drawArcadeButton(btnPauseSaves,   "SAVES",             new Color(0.20f, 0.55f, 0.86f, 1f));
        drawArcadeButton(btnPauseRestart, "RESTART [R]",       new Color(0.88f, 0.52f, 0.12f, 1f));
        drawArcadeButton(btnPauseMenu,    "EXIT TO MAIN MENU", new Color(0.78f, 0.22f, 0.18f, 1f));
    }

    private void renderSaveSlotsModal() {
        drawBox(0, 0, V_WIDTH, V_HEIGHT, new Color(0f, 0f, 0f, 0.76f), Color.CLEAR);
        drawArcadePanel(85, 45, 730, 435, new Color(0.10f, 0.15f, 0.22f, 0.98f), Color.GOLD);
        drawCenteredText(assets.fontLarge, "SELECT SAVE SLOT (1 - 5)", 450, 82, Color.GOLD);

        for (int slot = 1; slot <= 5; slot++) {
            float rowY = 118 + (slot - 1) * 58;
            boolean exists = SaveManager.slotExists(slot);
            boolean isCurrent = (slot == SaveManager.getActiveSlot()) && exists;
            drawArcadePanel(105, rowY, 695, 50, isCurrent ? new Color(0.16f, 0.28f, 0.40f, 1f) : new Color(0.14f, 0.18f, 0.24f, 1f), isCurrent ? Color.CYAN : Color.GRAY);

            assets.fontSmall.setColor(isCurrent ? Color.YELLOW : (exists ? Color.WHITE : Color.LIGHT_GRAY));
            assets.fontSmall.draw(batch, SaveManager.getSlotSummary(slot), 122, V_HEIGHT - (rowY + 20));

            if (!exists) {
                drawArcadeButton(new Rectangle(545, rowY + 6, 245, 38), "+ CREATE NEW SAVE", new Color(0.18f, 0.75f, 0.42f, 1f));
            } else {
                drawArcadeButton(new Rectangle(515, rowY + 6, 86, 38), "LOAD",   new Color(0.18f, 0.75f, 0.42f, 1f));
                drawArcadeButton(new Rectangle(608, rowY + 6, 86, 38), "SAVE",   new Color(0.20f, 0.55f, 0.86f, 1f));
                drawArcadeButton(new Rectangle(701, rowY + 6, 89, 38), "DELETE", new Color(0.82f, 0.22f, 0.18f, 1f));
            }
        }

        drawArcadeButton(new Rectangle(310, 425, 280, 42), "BACK [ESC]", new Color(0.35f, 0.40f, 0.45f, 1f));

        if (pendingConfirmAction != ConfirmAction.NONE) {
            drawBox(0, 0, V_WIDTH, V_HEIGHT, new Color(0f, 0f, 0f, 0.72f), Color.CLEAR);
            Color borderCol = switch (pendingConfirmAction) {
                case LOAD_SLOT -> Color.GREEN;
                case SAVE_OVERWRITE -> Color.CYAN;
                default -> Color.SCARLET;
            };
            drawArcadePanel(220, 165, 460, 185, new Color(0.09f, 0.13f, 0.18f, 0.98f), borderCol);

            String title = switch (pendingConfirmAction) {
                case LOAD_SLOT -> "LOAD SAVE SLOT " + pendingConfirmSlot + "?";
                case SAVE_OVERWRITE -> "OVERWRITE SAVE SLOT " + pendingConfirmSlot + "?";
                default -> "DELETE SAVE SLOT " + pendingConfirmSlot + "?";
            };
            String desc = switch (pendingConfirmAction) {
                case LOAD_SLOT -> "Unsaved progress in your current session will be lost.";
                case SAVE_OVERWRITE -> "Existing data in Slot " + pendingConfirmSlot + " will be overwritten.";
                default -> "Slot " + pendingConfirmSlot + " will be permanently deleted.";
            };
            String yesLabel = switch (pendingConfirmAction) {
                case LOAD_SLOT -> "YES, LOAD";
                case SAVE_OVERWRITE -> "YES, OVERWRITE";
                default -> "YES, DELETE";
            };

            drawCenteredText(assets.fontLarge, title, 450, 205, borderCol);
            drawCenteredText(assets.fontSmall, desc, 450, 245, Color.WHITE);

            drawArcadeButton(new Rectangle(265, 285, 175, 42), yesLabel, new Color(0.18f, 0.75f, 0.42f, 1f));
            drawArcadeButton(new Rectangle(460, 285, 175, 42), "CANCEL", new Color(0.75f, 0.22f, 0.18f, 1f));
        }
    }

    private void renderOptionsModal() {
        drawBox(0, 0, V_WIDTH, V_HEIGHT, new Color(0f, 0f, 0f, 0.72f), Color.CLEAR);
        drawArcadePanel(195, 52, 510, 426, new Color(0.11f, 0.16f, 0.22f, 0.98f), Color.CYAN);

        drawCenteredText(assets.fontLarge, "OPTIONS", 450, 92, Color.YELLOW);

        drawCenteredText(assets.fontMedium, "BGM Music Volume: " + (int) (bgmVolume * 100) + "%", 450, 132, Color.WHITE);
        drawArcadePanel(sliderBgmBar.x, sliderBgmBar.y, sliderBgmBar.width, sliderBgmBar.height, Color.BLACK, Color.WHITE);
        batch.setColor(new Color(0.18f, 0.85f, 0.48f, 1f));
        batch.draw(assets.whitePixel, sliderBgmBar.x + 3, V_HEIGHT - sliderBgmBar.y - sliderBgmBar.height + 3, (sliderBgmBar.width - 6) * bgmVolume, sliderBgmBar.height - 6);

        drawCenteredText(assets.fontMedium, "Sound Effects (SFX): " + (int) (sfxVolume * 100) + "%", 450, 192, Color.WHITE);
        drawArcadePanel(sliderSfxBar.x, sliderSfxBar.y, sliderSfxBar.width, sliderSfxBar.height, Color.BLACK, Color.WHITE);
        batch.setColor(new Color(0.20f, 0.68f, 0.98f, 1f));
        batch.draw(assets.whitePixel, sliderSfxBar.x + 3, V_HEIGHT - sliderSfxBar.y - sliderSfxBar.height + 3, (sliderSfxBar.width - 6) * sfxVolume, sliderSfxBar.height - 6);

        String aimLabel = mouseAimEnabled ? "AIM MODE: MOUSE CURSOR AIMING (ON)" : "AIM MODE: KEYBOARD WASD AIMING (OFF)";
        drawArcadeButton(btnToggleAim, aimLabel, mouseAimEnabled ? new Color(0.16f, 0.62f, 0.52f, 1f) : new Color(0.68f, 0.42f, 0.14f, 1f));

        String fullLabel = borderlessFullscreen ? "DISPLAY: BORDERLESS FULLSCREEN (ON)" : "DISPLAY: WINDOWED 900x520 (OFF)";
        drawArcadeButton(btnToggleFull, fullLabel, borderlessFullscreen ? new Color(0.22f, 0.58f, 0.88f, 1f) : new Color(0.36f, 0.42f, 0.50f, 1f));

        drawArcadeButton(new Rectangle(280, 355, 160, 40), "MUTE AUDIO", new Color(0.75f, 0.22f, 0.17f, 1f));
        drawArcadeButton(new Rectangle(460, 355, 160, 40), "RESET AUDIO", new Color(0.20f, 0.55f, 0.82f, 1f));
        drawArcadeButton(new Rectangle(310, 420, 280, 44), "SAVE & RETURN [ESC]", new Color(0.18f, 0.78f, 0.42f, 1f));
    }

    private void renderCustomizeScreen() {
        drawTiledFloor();
        drawArcadePanel(80, 42, 740, 440, new Color(0.1f, 0.15f, 0.22f, 0.96f), Color.CYAN);
        drawCenteredText(assets.fontLarge, "CUSTOMIZE OPERATOR", 450, 82, Color.YELLOW);

        drawArcadePanel(125, 125, 240, 285, new Color(0.16f, 0.22f, 0.28f, 1f), Color.WHITE);
        drawPlayerAvatar(245, 275, 2.25f, menuAnimTick, Direction.DOWN, 0f);

        Player p = controller.getPlayer();
        drawArcadeButton(new Rectangle(420, 165, 340, 40), "[1] Cycle Hardhat Color", toGdxColor(p.getCosmetics().getHardhatColor()));
        drawArcadeButton(new Rectangle(420, 215, 340, 40), "[2] Cycle Hazmat Suit", toGdxColor(p.getCosmetics().getSuitColor()));
        drawArcadeButton(new Rectangle(420, 265, 340, 40), "[3] Cycle Skin Tone", new Color(0.4f, 0.45f, 0.5f, 1f));
        drawArcadeButton(new Rectangle(420, 315, 340, 40), "[4] Visor: " + p.getCosmetics().getVisorStyle(), new Color(0.2f, 0.6f, 0.86f, 1f));

        drawArcadeButton(new Rectangle(290, 425, 320, 46), "CONFIRM & RETURN", new Color(0.18f, 0.8f, 0.44f, 1f));
    }

    // Icon-Based Upgrade Shop with Separate Balance Indicator & CleanerBot Model at the Bottom!
    private void renderShop() {
        drawTiledFloor();
        drawArcadePanel(55, 28, 790, 462, new Color(0.09f, 0.14f, 0.20f, 0.97f), Color.GOLD);

        Player p = controller.getPlayer();
        UpgradeShop shop = controller.getShop();
        CleanerBot bot = controller.getCleanerBot();

        drawCurrencyBadge(75, 44, p.getCredits());
        drawCenteredText(assets.fontLarge, "MRF UPGRADE SHOP", 470, 62, Color.GOLD);
        drawCenteredText(assets.fontSmall, "Upgrades go up to Lv.10 and activate after your current facility shift ends!", 470, 96, Color.CYAN);

        // Row 1: Pneumatic Speed Boots (with Boot Icon)
        String bootsCost = shop.getPurchasedBootsLevel() >= 10 ? "MAXED" : "$" + shop.getBootsCost();
        drawShopUpgradeRow(new Rectangle(85, 128, 730, 62),
                "Pneumatic Speed Boots  (Lv." + shop.getBootsLevel() + " -> Next: Lv." + shop.getPurchasedBootsLevel() + "/10)",
                "+12% Walk & Dash Speed per level", bootsCost, new Color(0.16f, 0.65f, 0.45f, 1f), 1);

        // Row 2: Turbo Station Overclock (with Machine Icon)
        String stCost = shop.getPurchasedStationLevel() >= 10 ? "MAXED" : "$" + shop.getStationCost();
        drawShopUpgradeRow(new Rectangle(85, 198, 730, 62),
                "Turbo Station Overclock  (Lv." + shop.getStationLevel() + " -> Next: Lv." + shop.getPurchasedStationLevel() + "/10)",
                "+8% Machine Processing Speed per level (Max +80% at Lv.10)", stCost, new Color(0.88f, 0.48f, 0.14f, 1f), 2);

        // Row 3: Expanded Bay Hoppers (with Bin Icon)
        String hopCost = shop.getPurchasedHopperLevel() >= 10 ? "MAXED" : "$" + shop.getHopperCost();
        drawShopUpgradeRow(new Rectangle(85, 268, 730, 62),
                "Expanded Bin Hoppers  (Lv." + shop.getHopperLevel() + " -> Next: Lv." + shop.getPurchasedHopperLevel() + "/10)",
                "+2 Max Capacity for all 4 sorting bins per level", hopCost, new Color(0.52f, 0.28f, 0.72f, 1f), 3);

        // Row 4 (Bottom): Assistant CleanerBot ($50,000 Unlock, uses live Robot Model as Icon!)
        String botCost = shop.getPurchasedBotLevel() >= 10 ? "MAXED" : "$" + shop.getBotCost();
        String botTitle = shop.getPurchasedBotLevel() == 0
                ? "UNLOCK ASSISTANT CLEANERBOT  (Lv.0 -> Lv.1/10)"
                : "Upgrade Assistant CleanerBot  (Lv." + bot.getLevel() + " -> Next: Lv." + shop.getPurchasedBotLevel() + "/10)";
        drawShopUpgradeRow(new Rectangle(85, 338, 730, 66),
                botTitle, "Lv.1-3: Sort Ready Trash | Lv.4+: Use Machines | Lv.7+: Wake Forklift",
                botCost, new Color(0.15f, 0.52f, 0.78f, 1f), 4);

        drawArcadeButton(new Rectangle(310, 422, 280, 44), "BACK [SPACE / CLICK]", new Color(0.32f, 0.38f, 0.46f, 1f));
    }

    private void drawShopUpgradeRow(Rectangle row, String title, String subtitle, String priceTag, Color rowColor, int iconType) {
        boolean hovered = row.contains(mouseX, mouseY);
        Color fill = hovered ? new Color(Math.min(1f, rowColor.r + 0.10f), Math.min(1f, rowColor.g + 0.10f), Math.min(1f, rowColor.b + 0.10f), 1f) : rowColor;
        drawArcadePanel(row.x, row.y, row.width, row.height, fill, hovered ? Color.YELLOW : Color.WHITE);

        // Left Icon Frame (54x50)
        float ix = row.x + 8;
        float iy = row.y + (row.height - 50) / 2f;
        drawArcadePanel(ix, iy, 54, 50, new Color(0.10f, 0.14f, 0.18f, 1f), Color.GOLD);

        batch.setColor(Color.WHITE);
        switch (iconType) {
            case 1 -> {
                // Winged Pneumatic Boot Icon
                drawBox(ix + 14, iy + 14, 16, 22, new Color(0.2f, 0.85f, 0.55f, 1f), Color.BLACK);
                drawBox(ix + 14, iy + 30, 28, 10, new Color(0.2f, 0.85f, 0.55f, 1f), Color.BLACK);
                drawBox(ix + 8, iy + 18, 10, 6, Color.YELLOW, Color.BLACK);
            }
            case 2 -> batch.draw(assets.getStationSprite(StationType.WASHER), ix + 5, V_HEIGHT - (iy + 44), 44, 38);
            case 3 -> batch.draw(assets.getBaySprite(BinType.RECYCLABLE, 2, 4), ix + 5, V_HEIGHT - (iy + 44), 44, 38);
            case 4 -> drawCleanerBot(ix + 27, iy + 32, 0.82f, menuAnimTick * 1.2f, 0f);
        }

        assets.fontMedium.setColor(Color.WHITE);
        assets.fontMedium.draw(batch, title, row.x + 76, V_HEIGHT - (row.y + 14));
        assets.fontSmall.setColor(new Color(0.9f, 0.95f, 1f, 0.9f));
        assets.fontSmall.draw(batch, subtitle, row.x + 76, V_HEIGHT - (row.y + 40));

        // Right Price Button Pill
        drawArcadePanel(row.x + row.width - 145, row.y + 11, 132, row.height - 22, new Color(0.12f, 0.16f, 0.22f, 1f), Color.GOLD);
        drawCenteredText(assets.fontMedium, priceTag, row.x + row.width - 79, row.y + row.height / 2f, Color.GOLD);
    }

    private void drawCurrencyBadge(float x, float y, int amount) {
        String txt = "BALANCE: $" + amount;
        glyphLayout.setText(assets.fontSmall, txt);
        float w = Math.max(138f, glyphLayout.width + 26f);
        drawArcadePanel(x, y, w, 30, new Color(0.12f, 0.18f, 0.24f, 0.96f), Color.GOLD);
        drawBox(x + 6, y + 6, 14, 18, Color.GOLD, Color.BLACK);
        drawCenteredText(assets.fontSmall, txt, x + 10 + w / 2f, y + 15, Color.GOLD);
    }

    private void renderFactoryStage() {
        drawTiledFloor();
        drawConveyorAndIncinerator();
        drawWorkStationsAndForklift();
        drawBays();

        for (WasteItem item : controller.getThrownItems()) {
            drawWasteItem(item);
        }

        float celebJump = victoryCelebrationActive ? (float) Math.abs(Math.sin(menuAnimTick * 0.9f)) * 22f : 0f;

        CleanerBot bot = controller.getCleanerBot();
        if (bot.getLevel() > 0) {
            drawCleanerBot((float) bot.getX(), (float) bot.getY(), 1.0f, (float) bot.getAnimTick(), celebJump * 0.85f);
            if (bot.getHeldItem() != null) {
                drawWasteItem(bot.getHeldItem());
            }
        }

        Player p = controller.getPlayer();
        if (p.getHeldItem() != null) {
            int maxDist = chargingThrow ? (int) (75 + throwCharge * 95) : 85;
            Color dotCol = chargingThrow
                    ? new Color(1f, 1f - throwCharge * 0.6f, 0.2f, 0.85f)
                    : new Color(1f, 1f, 1f, 0.55f);
            batch.setColor(dotCol);
            for (int step = 15; step <= maxDist; step += 15) {
                float ax = (float) (p.getX() + p.getThrowDx() * step);
                float ay = (float) (p.getY() + p.getThrowDy() * step);
                batch.draw(assets.whitePixel, ax - 2, V_HEIGHT - ay - 2, 5, 5);
            }
        }

        Direction avatarDir = victoryCelebrationActive ? Direction.DOWN : p.getFacing();
        float animCycle = victoryCelebrationActive ? menuAnimTick * 1.4f : (float) p.getWalkCycle();
        drawPlayerAvatar((float) p.getX(), (float) p.getY(), 1.0f, animCycle, avatarDir, celebJump);
        if (p.getHeldItem() != null) {
            drawWasteItem(p.getHeldItem());
        }

        if (chargingThrow && p.getHeldItem() != null) {
            float barX = (float) p.getX() - 28;
            float barY = (float) p.getY() - 48;
            drawBox(barX, barY, 56, 8, Color.BLACK, Color.WHITE);
            Color chargeCol = new Color(1f, Math.max(0f, 1f - throwCharge * 0.85f), 0.1f, 1f);
            batch.setColor(chargeCol);
            batch.draw(assets.whitePixel, barX + 1, V_HEIGHT - barY - 7, 54 * throwCharge, 6);
            batch.setColor(Color.WHITE);
        }

        drawIncineratorRoofVents();
        drawProximityIndicators();

        for (FloatingText ft : controller.getPopups()) {
            Color c = toGdxColor(ft.getColor());
            assets.fontMedium.setColor(c.r, c.g, c.b, ft.getAlpha());
            assets.fontMedium.draw(batch, ft.getText(), (float) ft.getX(), V_HEIGHT - (float) ft.getY());
        }

        drawOvercookedMinimalistHud();

        if (mobileHudEnabled) {
            drawMobileTouchControls();
        }

        if (controller.isGameOver() || controller.isGameWon()) {
            renderDebriefOverlay();
        }
    }

    private void drawOvercookedMinimalistHud() {
        Player p = controller.getPlayer();
        FacilityLevel lvl = controller.getCurrentLevel();

        // 1. Dedicated Top-Left Total Balance Indicator + Order Tickets Below It
        drawCurrencyBadge(12, 8, p.getCredits());

        List<WasteItem> belt = controller.getBeltItems();
        int ticketX = 12;
        for (int i = 0; i < Math.min(3, belt.size()); i++) {
            WasteItem item = belt.get(i);
            StationType req = item.getRequiredStation();
            Color headerCol = (req != null) ? new Color(0.95f, 0.61f, 0.07f, 1f) : new Color(0.18f, 0.8f, 0.44f, 1f);

            drawArcadePanel(ticketX, 44, 48, 48, new Color(0.96f, 0.96f, 0.96f, 0.94f), new Color(0.18f, 0.22f, 0.26f, 1f));
            drawBox(ticketX + 2, 46, 44, 7, headerCol, Color.BLACK);

            batch.setColor(Color.WHITE);
            batch.draw(assets.getItemSprite(item.getDisplayTag()), ticketX + 8, V_HEIGHT - 88, 32, 32);
            ticketX += 54;
        }

        // 2. Top-Center Facility Name, Quota & 5:00 Timer Stage Pill
        int speedSt = controller.getTimerSpeedStage();
        String shiftLabel = lvl.getTitle() + "  |  Haul: " + controller.getTransportedTrash() + "/" + lvl.getTrashQuota()
                + "  |  " + controller.getFormattedTimeRemaining() + " (Spd x" + speedSt + ")";
        glyphLayout.setText(assets.fontSmall, shiftLabel);
        float pillW = Math.max(290f, glyphLayout.width + 20f);
        float pillX = Math.min(502f - pillW, Math.max(176f, 340f - pillW / 2f));
        Color timerBorder = speedSt >= 4 ? Color.RED : (speedSt == 3 ? Color.ORANGE : getStageAccentColor());
        drawArcadePanel(pillX, 10, pillW, 30, new Color(0.1f, 0.14f, 0.20f, 0.94f), timerBorder);
        drawCenteredText(assets.fontSmall, shiftLabel, pillX + pillW / 2f, 25, Color.WHITE);

        if (toastTimer > 0) {
            float alpha = Math.min(1f, toastTimer / 35f);
            glyphLayout.setText(assets.fontSmall, controller.getLastFeedback());
            float toastW = Math.max(380f, Math.min(680f, glyphLayout.width + 28f));
            drawArcadePanel(450f - toastW / 2f, 44, toastW, 20, new Color(0.08f, 0.11f, 0.14f, 0.85f * alpha), new Color(1f, 1f, 1f, 0.4f * alpha));
            drawCenteredText(assets.fontSmall, controller.getLastFeedback(), 450, 54, new Color(1f, 0.95f, 0.6f, alpha));
        }

        // 3. Top-Right Action Buttons (SHOP separated from Currency!)
        drawArcadeButton(btnHudShop,      "SHOP",      new Color(0.88f, 0.55f, 0.10f, 0.95f));
        drawArcadeButton(btnHudSaves,     "SAVES",     new Color(0.18f, 0.72f, 0.45f, 0.95f));
        drawArcadeButton(btnHudCustomize, "CUSTOMIZE", new Color(0.20f, 0.55f, 0.82f, 0.95f));
        drawArcadeButton(btnHudOptions,   "OPTIONS",   new Color(0.55f, 0.27f, 0.68f, 0.95f));
        drawArcadeButton(btnHudPause,     "PAUSE",     new Color(0.30f, 0.36f, 0.42f, 0.95f));

        // 4. Bottom-Left Score & Streak
        drawArcadePanel(14, 448, 74, 58, new Color(0.95f, 0.76f, 0.12f, 1f), new Color(0.45f, 0.30f, 0.02f, 1f));
        drawCenteredText(assets.fontLarge, String.valueOf(p.getScore()), 51, 477, Color.BLACK);

        if (p.getStreakMeter().getValue() > 0) {
            drawArcadePanel(94, 468, 98, 28, new Color(0.18f, 0.82f, 0.44f, 0.95f), Color.WHITE);
            drawCenteredText(assets.fontSmall, "STREAK x" + p.getStreakMeter().getValue(), 143, 482, Color.BLACK);
        }

        // 5. Bottom-Right Compliance HP
        int comp = p.getComplianceMeter().getValue();
        Color gaugeCol = comp > 50 ? new Color(0.18f, 0.82f, 0.44f, 1f) : (comp > 25 ? Color.ORANGE : Color.RED);
        drawArcadePanel(762, 452, 124, 54, new Color(0.12f, 0.16f, 0.20f, 0.94f), Color.LIGHT_GRAY);
        drawBox(772, 483, 104, 13, Color.BLACK, Color.WHITE);
        batch.setColor(gaugeCol);
        batch.draw(assets.whitePixel, 774, V_HEIGHT - 494, 1.0f * comp, 9);
        drawCenteredText(assets.fontMedium, "HP: " + comp + "%", 824, 467, Color.WHITE);
    }

    private void renderMinigameOverlay() {
        if (activeMinigameStation == null) return;
        drawBox(0, 0, V_WIDTH, V_HEIGHT, new Color(0f, 0f, 0f, 0.65f), Color.CLEAR);

        boolean repairing = activeMinigameStation.isBroken();
        StationType type = activeMinigameStation.getType();
        Color borderCol = repairing ? Color.RED : Color.YELLOW;
        drawArcadePanel(200, 105, 500, 305, new Color(0.06f, 0.10f, 0.15f, 0.98f), borderCol);

        switch (type) {
            case WASHER -> {
                String hdr = repairing ? "HYDRO-WASHER REPAIR: SEAL LEAKING VALVES!" : "HYDRO-WASHER OVERDRIVE: SEAL PRESSURE VALVES!";
                drawCenteredText(assets.fontMedium, hdr, 450, 142, Color.CYAN);
                drawCenteredText(assets.fontSmall, "Click all remaining water pressure valves to seal the drum!", 450, 170, Color.WHITE);
                for (Rectangle v : washerValves) {
                    drawArcadeButton(v, "SEAL VALVE", new Color(0.12f, 0.62f, 0.92f, 1f));
                }
            }
            case DISMANTLER -> {
                String hdr = repairing ? "DISMANTLER REPAIR: CALIBRATE LASER SAW!" : "DISMANTLER OVERDRIVE: PRECISION LASER CUT!";
                drawCenteredText(assets.fontMedium, hdr, 450, 142, Color.ORANGE);
                drawCenteredText(assets.fontSmall, "Click [CALIBRATE] when the moving laser aligns with the GREEN center zone! (" + sawHitsRemaining + " left)", 450, 172, Color.WHITE);

                drawArcadePanel(250, 225, 400, 36, Color.BLACK, Color.WHITE);
                drawBox(394, 228, 112, 30, new Color(0.18f, 0.82f, 0.42f, 0.65f), Color.GREEN);
                float markerX = 254 + sawMarkerPos * 384;
                drawBox(markerX, 220, 8, 46, Color.RED, Color.YELLOW);

                drawArcadeButton(new Rectangle(320, 315, 260, 48), "CALIBRATE LASER!", new Color(0.90f, 0.45f, 0.12f, 1f));
            }
            case NEUTRALIZER -> {
                String hdr = repairing ? "NEUTRALIZER REPAIR: VENT OVERHEATED COILS!" : "NEUTRALIZER OVERDRIVE: COOL PLASMA CORES!";
                drawCenteredText(assets.fontMedium, hdr, 450, 142, Color.GREEN);
                drawCenteredText(assets.fontSmall, "Click [COOL ROD] below each plasma core until all 3 rods drop into the green (<25%)!", 450, 172, Color.WHITE);

                for (int i = 0; i < 3; i++) {
                    float rx = 275 + i * 125;
                    drawArcadePanel(rx + 20, 195, 60, 115, Color.BLACK, Color.WHITE);
                    float hRatio = plasmaHeat[i];
                    Color rodCol = hRatio <= 0.25f ? Color.GREEN : (hRatio <= 0.6f ? Color.ORANGE : Color.RED);
                    float barH = 107 * hRatio;
                    drawBox(rx + 24, 306 - barH, 52, barH, rodCol, Color.CLEAR);
                    drawArcadeButton(new Rectangle(rx, 325, 100, 42), "COOL ROD " + (i + 1), new Color(0.15f, 0.72f, 0.58f, 1f));
                }
            }
        }
    }

    private Color getStageTint(boolean altTile) {
        int lvl = (controller.getCurrentLevel().getLevelNumber() - 1) % 5 + 1;
        return switch (lvl) {
            case 1 -> altTile ? new Color(0.82f, 1.0f, 0.88f, 1f) : new Color(0.75f, 0.94f, 0.82f, 1f);
            case 2 -> altTile ? new Color(1.0f, 0.88f, 0.72f, 1f) : new Color(0.94f, 0.82f, 0.66f, 1f);
            case 3 -> altTile ? new Color(0.68f, 0.85f, 1.0f, 1f) : new Color(0.60f, 0.78f, 0.96f, 1f);
            case 4 -> altTile ? new Color(0.68f, 0.98f, 0.96f, 1f) : new Color(0.60f, 0.90f, 0.88f, 1f);
            default -> altTile ? new Color(1.0f, 0.72f, 0.78f, 1f) : new Color(0.92f, 0.64f, 0.70f, 1f);
        };
    }

    private Color getStageAccentColor() {
        return switch ((controller.getCurrentLevel().getLevelNumber() - 1) % 5 + 1) {
            case 1 -> new Color(0.18f, 0.85f, 0.44f, 1f);
            case 2 -> new Color(0.95f, 0.61f, 0.07f, 1f);
            case 3 -> new Color(0.0f, 0.82f, 0.95f, 1f);
            case 4 -> new Color(0.28f, 0.86f, 0.98f, 1f);
            default -> new Color(0.95f, 0.30f, 0.30f, 1f);
        };
    }

    private void drawTiledFloor() {
        for (int x = 0; x < V_WIDTH; x += 45) {
            for (int y = 0; y < V_HEIGHT; y += 45) {
                boolean alt = ((x / 45) + (y / 45)) % 2 == 0;
                batch.setColor(getStageTint(alt));
                batch.draw(alt ? assets.floorTileA : assets.floorTileB, x, y);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void drawConveyorAndIncinerator() {
        int sx = controller.getBeltStartX();
        int sy = controller.getBeltStartY();
        int ex = controller.getBeltEndX();
        int ey = controller.getBeltEndY();

        boolean horizontal = Math.abs(ex - sx) >= Math.abs(ey - sy);
        if (horizontal) {
            int minX = Math.min(sx, ex);
            int maxX = Math.max(sx, ex);
            int bw = (maxX - minX) + 48;
            int by = sy - 8;

            drawArcadePanel(minX - 6, by - 4, bw + 12, 62, new Color(0.28f, 0.33f, 0.38f, 1f), getStageAccentColor());
            drawBox(minX, by, bw, 54, new Color(0.18f, 0.21f, 0.25f, 1f), Color.BLACK);

            int dir = (ex >= sx) ? 1 : -1;
            int shift = (dir > 0) ? beltAnimOffset : (24 - beltAnimOffset);
            batch.setColor(new Color(0.45f, 0.5f, 0.58f, 1f));
            for (int rx = minX + shift; rx < minX + bw; rx += 24) {
                batch.draw(assets.whitePixel, rx, V_HEIGHT - (by + 52), 3, 50);
            }
        } else {
            int minY = Math.min(sy, ey);
            int maxY = Math.max(sy, ey);
            int bh = (maxY - minY) + 48;
            int bx = sx - 8;

            drawArcadePanel(bx - 4, minY - 6, 62, bh + 12, new Color(0.28f, 0.33f, 0.38f, 1f), getStageAccentColor());
            drawBox(bx, minY, 54, bh, new Color(0.18f, 0.21f, 0.25f, 1f), Color.BLACK);

            int dir = (ey >= sy) ? 1 : -1;
            int shift = (dir > 0) ? beltAnimOffset : (24 - beltAnimOffset);
            batch.setColor(new Color(0.45f, 0.5f, 0.58f, 1f));
            for (int ry = minY + shift; ry < minY + bh; ry += 24) {
                batch.draw(assets.whitePixel, bx + 2, V_HEIGHT - ry - 3, 50, 3);
            }
        }

        float incX = ex - 14;
        float incY = ey - 10;
        batch.setColor(Color.WHITE);
        batch.draw(assets.incineratorTexture, incX, V_HEIGHT - incY - 68);

        float flicker1 = (float) (Math.sin(menuAnimTick * 1.8f) * 4f);
        float flicker2 = (float) (Math.cos(menuAnimTick * 2.5f) * 3f);
        drawBox(incX + 20, incY + 24 - flicker1, 36, 26 + flicker1, new Color(0.92f, 0.25f, 0.12f, 0.95f), Color.CLEAR);
        drawBox(incX + 26, incY + 28 - flicker2, 24, 20 + flicker2, new Color(0.98f, 0.62f, 0.10f, 0.95f), Color.CLEAR);
        drawBox(incX + 31, incY + 34, 14, 13, new Color(1f, 0.95f, 0.4f, 1f), Color.CLEAR);

        for (int gx = 20; gx <= 54; gx += 11) {
            drawBox(incX + gx, incY + 18, 3, 34, new Color(0.12f, 0.14f, 0.16f, 0.85f), Color.CLEAR);
        }

        for (WasteItem item : controller.getBeltItems()) {
            drawWasteItem(item);
        }
    }

    private void drawIncineratorRoofVents() {
        int sx = controller.getBeltStartX();
        int sy = controller.getBeltStartY();
        int ex = controller.getBeltEndX();
        int ey = controller.getBeltEndY();
        float incX = ex - 14;
        float incY = ey - 10;
        if (incY <= 0) return;

        boolean verticalTopToBottom = Math.abs(ey - sy) > Math.abs(ex - sx) && ey > sy;
        if (verticalTopToBottom) {
            float ductX = incX - 28;
            drawBox(ductX, incY + 14, 30, 18, new Color(0.25f, 0.29f, 0.33f, 1f), Color.BLACK);
            drawBox(ductX, 0, 20, incY + 28, new Color(0.25f, 0.29f, 0.33f, 1f), Color.BLACK);
            for (int vy = 12; vy < incY + 18; vy += 22) {
                drawBox(ductX - 2, vy, 24, 5, new Color(0.42f, 0.48f, 0.54f, 1f), Color.BLACK);
            }
        } else {
            drawBox(incX + 12, 0, 18, incY + 4, new Color(0.25f, 0.29f, 0.33f, 1f), Color.BLACK);
            drawBox(incX + 46, 0, 18, incY + 4, new Color(0.25f, 0.29f, 0.33f, 1f), Color.BLACK);
            for (int vy = 12; vy < incY; vy += 22) {
                drawBox(incX + 10, vy, 22, 5, new Color(0.42f, 0.48f, 0.54f, 1f), Color.BLACK);
                drawBox(incX + 44, vy, 22, 5, new Color(0.42f, 0.48f, 0.54f, 1f), Color.BLACK);
            }
        }

        float pipeCenterX = sx + 23;
        float nozzleBottomY = Math.max(18, sy - 22);
        drawBox(pipeCenterX - 20, sy + 6, 40, 12, new Color(0f, 0f, 0f, 0.38f), Color.CLEAR);

        if (nozzleBottomY > 14) {
            drawBox(pipeCenterX - 16, 0, 32, nozzleBottomY - 12, new Color(0.22f, 0.31f, 0.38f, 1f), Color.BLACK);
            drawBox(pipeCenterX - 10, 0, 8, nozzleBottomY - 12, new Color(0.32f, 0.43f, 0.52f, 1f), Color.CLEAR);
            for (int py = 10; py < nozzleBottomY - 16; py += 20) {
                drawBox(pipeCenterX - 19, py, 38, 5, new Color(0.45f, 0.54f, 0.62f, 1f), Color.BLACK);
            }
        }

        drawArcadePanel(pipeCenterX - 24, nozzleBottomY - 16, 48, 16, new Color(0.18f, 0.24f, 0.30f, 1f), new Color(0.95f, 0.76f, 0.12f, 1f));
        drawBox(pipeCenterX - 18, nozzleBottomY - 5, 36, 5, new Color(0.06f, 0.08f, 0.10f, 1f), Color.BLACK);
    }

    private void drawWorkStationsAndForklift() {
        double px = controller.getPlayer().getX();
        double py = controller.getPlayer().getY();
        WorkStation nearestWs = controller.getNearbyStation(px, py, 92);

        for (WorkStation ws : controller.getStations()) {
            float sx = (float) ws.getX();
            float sy = (float) ws.getY();
            boolean running = ws.getStoredItem() != null && !ws.isFinished() && !ws.isBroken();
            float shake = running ? (float) (Math.sin(menuAnimTick * 3.0f) * 1.8f) : 0f;

            if (ws == nearestWs) {
                drawBox(sx - 3, sy - 3, 90, 78, ws.isBroken() ? Color.RED : Color.CYAN, Color.WHITE);
            }

            float hpRatio = ws.getHealth() / 100f;
            batch.setColor(1f, 0.45f + 0.55f * hpRatio, 0.45f + 0.55f * hpRatio, 1f);
            batch.draw(assets.getStationSprite(ws.getType()), sx + shake, V_HEIGHT - sy - 72);
            batch.setColor(Color.WHITE);

            if (ws.getHealth() < 100) {
                drawBox(sx + 8, sy + 64, 68, 5, Color.BLACK, Color.WHITE);
                Color hpCol = ws.isBroken() ? Color.RED : (ws.getHealth() > 50 ? Color.ORANGE : Color.SCARLET);
                batch.setColor(hpCol);
                batch.draw(assets.whitePixel, sx + 9, V_HEIGHT - (sy + 68), 66 * hpRatio, 3);
                batch.setColor(Color.WHITE);
            }

            if (ws.isBroken()) {
                float sparkX = (float) ((Math.random() - 0.5) * 26f);
                drawBox(sx + 34 + sparkX, sy + 28, 12, 12, Color.RED, Color.YELLOW);
                drawCenteredText(assets.fontSmall, "BROKEN!", sx + 42, sy + 42, Color.RED);
            } else if (running) {
                switch (ws.getType()) {
                    case WASHER -> {
                        float waveY = (float) (Math.sin(menuAnimTick * 1.6f) * 4f);
                        drawBox(sx + 30, sy + 38 + waveY, 24, 10, new Color(0.2f, 0.9f, 1f, 0.7f), Color.WHITE);
                    }
                    case DISMANTLER -> {
                        float sparkX = (float) ((Math.random() - 0.5) * 18f);
                        drawBox(sx + 38 + sparkX, sy + 34, 6, 6, Color.YELLOW, Color.ORANGE);
                    }
                    case NEUTRALIZER -> {
                        float pulse = 0.35f + (float) Math.abs(Math.sin(menuAnimTick * 1.4f)) * 0.45f;
                        drawBox(sx + 20, sy + 18, 44, 36, new Color(0.1f, 1f, 0.6f, pulse), Color.CLEAR);
                    }
                }
            }

            if (ws.getStoredItem() != null) {
                ws.getStoredItem().setPosition(sx + 19 + shake, sy + 16);
                drawWasteItem(ws.getStoredItem());
                drawBox(sx + 7, sy - 12, 70, 7, Color.BLACK, Color.WHITE);
                Color barCol = ws.isWrongItemProcessing() ? Color.RED : (ws.isFinished() ? Color.GREEN : Color.YELLOW);
                batch.setColor(barCol);
                batch.draw(assets.whitePixel, sx + 8, V_HEIGHT - (sy - 6), 68 * ws.getProgressRatio(), 5);
                batch.setColor(Color.WHITE);
            }
        }

        // Draw Large Side-Profile Forklift (138x102) + Sleeping/Active Human Operator Seated Inside Cab!
        float fx = controller.getForkliftX() - 18;
        float fy = controller.getForkliftY() - 24;
        boolean busy = controller.isForkliftBusy();
        if (!busy && controller.isNearForklift(px, py, 96)) {
            drawBox(fx + 4, fy + 6, 130, 92, new Color(1f, 0.85f, 0.1f, 0.35f), Color.GOLD);
        }

        float idleBob = (float) (Math.sin(menuAnimTick * (busy ? 2.4f : 0.45f)) * 1.5f);
        boolean flipX = controller.isForkliftFacingLeft();

        float npcX = flipX ? (fx + 84) : (fx + 54);
        float npcY = fy + 48 + idleBob + (busy ? 0f : 3f); // Head nods down slightly while sleeping!
        float steerBob = busy ? (float) (Math.cos(menuAnimTick * 2.6f) * 2.0f) : 0f;
        Color skin = new Color(0.92f, 0.72f, 0.54f, 1f);
        Color hatCol = new Color(0.98f, 0.78f, 0.12f, 1f);

        drawBox(npcX - 16, npcY - 6, 32, 20, new Color(0.18f, 0.52f, 0.82f, 1f), Color.BLACK);
        drawBox(npcX - 11, npcY - 6, 5, 20, Color.YELLOW, Color.BLACK);
        drawBox(npcX + 6, npcY - 6, 5, 20, Color.YELLOW, Color.BLACK);

        drawBox(npcX - 14, npcY - 26, 28, 22, skin, Color.BLACK);

        float eyeShift = flipX ? -4.0f : 4.0f;
        if (busy) {
            // Wide-awake eyes steering the forklift
            drawBox(npcX - 8 + eyeShift, npcY - 18, 5, 5, Color.WHITE, Color.BLACK);
            drawBox(npcX + 3 + eyeShift, npcY - 18, 5, 5, Color.WHITE, Color.BLACK);
            drawBox(npcX - 5.5f + eyeShift, npcY - 17, 2.5f, 3, Color.BLACK, Color.BLACK);
            drawBox(npcX + 3.0f + eyeShift, npcY - 17, 2.5f, 3, Color.BLACK, Color.BLACK);
        } else {
            // Closed sleeping eyelids (- -) while parked!
            drawBox(npcX - 8 + eyeShift, npcY - 15, 6, 2, Color.BLACK, Color.BLACK);
            drawBox(npcX + 2 + eyeShift, npcY - 15, 6, 2, Color.BLACK, Color.BLACK);
        }

        drawBox(npcX - 15, npcY - 35, 30, 10, hatCol, Color.BLACK);
        drawBox(npcX - 18, npcY - 26, 36, 4, hatCol, Color.BLACK);

        float handReachX = flipX ? -18 : 10;
        drawBox(npcX + handReachX, npcY - 4 + steerBob, 9, 9, skin, Color.BLACK);

        Texture fkTex = assets.forkliftTexture;
        batch.setColor(Color.WHITE);
        batch.draw(fkTex, fx, V_HEIGHT - (fy + idleBob) - 102,
                138, 102,
                0, 0, fkTex.getWidth(), fkTex.getHeight(), flipX, false);

        // Floating "Zzz" Sleep Bubbles Above the Forklift Driver When Idle!
        if (!busy) {
            for (int z = 0; z < 3; z++) {
                float phase = (menuAnimTick * 0.12f + z * 0.33f) % 1.0f;
                float zx = npcX + 10 + phase * 18f + (float) Math.sin(phase * 6.28f) * 4f;
                float zy = (npcY - 40) - phase * 28f;
                assets.fontSmall.setColor(0.75f, 0.92f, 1.0f, 1.0f - phase * 0.85f);
                assets.fontSmall.draw(batch, z == 0 ? "z" : "Z", zx, V_HEIGHT - zy);
            }
            batch.setColor(Color.WHITE);
        }

        StorageBay hauledBay = controller.getActiveHaulBay();
        if (hauledBay != null && hauledBay.isBeingHauled()) {
            int fillVis = controller.isCarryingFullBin() ? hauledBay.getCapacity() : 0;
            Texture binTex = assets.getBaySprite(hauledBay.getType(), fillVis, hauledBay.getCapacity());
            float binX = flipX ? (fx - 54) : (fx + 104);
            float binY = fy + 20 + idleBob;
            batch.setColor(Color.WHITE);
            batch.draw(binTex, binX, V_HEIGHT - binY - 72, 88, 72);
        }
    }

    private void drawBays() {
        StorageBay nearest = controller.getNearbyBay(controller.getPlayer().getX(), controller.getPlayer().getY(), 96);
        for (StorageBay bay : controller.getBays()) {
            if (bay.isBeingHauled()) continue;

            if (bay == nearest) {
                drawBox(bay.getX() - 3, bay.getY() - 3, 94, 78, Color.GREEN, Color.WHITE);
            }
            batch.setColor(Color.WHITE);
            batch.draw(
                    assets.getBaySprite(bay.getType(), bay.getFillLevel(), bay.getCapacity()),
                    bay.getX(),
                    V_HEIGHT - bay.getY() - 72
            );

            int cap = Math.max(1, bay.getCapacity());
            float pipW = Math.max(4f, (72f / cap) - 2f);
            for (int i = 0; i < cap; i++) {
                boolean filled = i < bay.getFillLevel();
                Color pipCol = bay.isFull() ? Color.RED : (filled ? Color.YELLOW : new Color(0f, 0f, 0f, 0.45f));
                drawBox(bay.getX() + 8 + i * (pipW + 2f), bay.getY() + 63, pipW, 5, pipCol, Color.BLACK);
            }
        }
    }

    private void drawProximityIndicators() {
        double px = controller.getPlayer().getX();
        double py = controller.getPlayer().getY();

        StorageBay nearBay = controller.getNearbyBay(px, py, 96);
        if (nearBay != null) {
            String tag = nearBay.getType().name() + " BIN (" + nearBay.getFillLevel() + "/" + nearBay.getCapacity() + ") [Tap: Drop Off]";
            drawFloatingIndicatorBadge(nearBay.getX() + 44f, nearBay.getY() - 16f, tag, Color.GREEN);
            return;
        }

        WorkStation nearWs = controller.getNearbyStation(px, py, 92);
        if (nearWs != null) {
            String action = nearWs.isBroken()
                    ? "[SPACE/Click: Repair Minigame!]"
                    : (nearWs.isEmpty() ? "[Tap/Throw: Load]" : (nearWs.isFinished() ? "[Tap: Grab]" : "[Tap: Overdrive Minigame]"));
            String tag = nearWs.getType().getLabel() + " (HP:" + nearWs.getHealth() + "%) " + action;
            drawFloatingIndicatorBadge((float) nearWs.getX() + 42f, (float) nearWs.getY() - 22f, tag,
                    nearWs.isBroken() ? Color.RED : Color.CYAN);
            return;
        }

        if (!controller.isForkliftBusy() && controller.isNearForklift(px, py, 92)) {
            boolean ready = controller.hasAnyFullBin();
            String msg = ready
                    ? "SLEEPING FORKLIFT DRIVER [SPACE: Wake Up & Haul Bins!]"
                    : "SLEEPING FORKLIFT DRIVER (Zzz... Fill a Bin completely first)";
            drawFloatingIndicatorBadge(controller.getForkliftX() + 46f, controller.getForkliftY() - 28f,
                    msg, ready ? Color.GREEN : Color.ORANGE);
        }
    }

    private void drawFloatingIndicatorBadge(float centerX, float topDownY, String text, Color borderCol) {
        glyphLayout.setText(assets.fontSmall, text);
        float w = glyphLayout.width + 18f;
        float h = 22f;
        float clampedX = Math.max(10f, Math.min(V_WIDTH - w - 10f, centerX - w / 2f));
        float clampedY = Math.max(48f, topDownY);
        drawArcadePanel(clampedX, clampedY, w, h, new Color(0.08f, 0.12f, 0.16f, 0.94f), borderCol);
        drawCenteredText(assets.fontSmall, text, clampedX + w / 2f, clampedY + h / 2f, Color.WHITE);
    }

    private void drawWasteItem(WasteItem item) {
        float ix = (float) item.getX();
        float iy = (float) item.getY();
        int dropTicks = item.getSpawnDropTimer();

        if (item.isAirborne()) {
            drawBox(ix + 6, iy + 36, 36, 10, new Color(0f, 0f, 0f, 0.35f), Color.CLEAR);
        } else if (dropTicks > 0) {
            drawBox(ix + 8, iy + 34, 30, 8, new Color(0f, 0f, 0f, 0.32f), Color.CLEAR);
            iy -= (dropTicks * 2.2f);
        }

        batch.setColor(Color.WHITE);
        batch.draw(assets.getItemSprite(item.getDisplayTag()), ix, V_HEIGHT - iy - 46, 46, 46);
    }

    private void drawCleanerBot(float bx, float by, float scale, float animTick, float jumpHeight) {
        CleanerBot bot = controller.getCleanerBot();
        float bob = (float) (Math.sin(animTick) * 2.5f * scale) - jumpHeight;
        float treadShift = (float) (Math.cos(animTick * 1.5f) * 3.0f * scale);

        drawBox(bx - 18 * scale, by + 13 * scale, 36 * scale, 8 * scale, new Color(0f, 0f, 0f, 0.45f), Color.CLEAR);
        drawBox(bx - 16 * scale, by + 6 * scale + treadShift, 9 * scale, 10 * scale, Color.DARK_GRAY, Color.BLACK);
        drawBox(bx + 7 * scale, by + 6 * scale - treadShift, 9 * scale, 10 * scale, Color.DARK_GRAY, Color.BLACK);

        Color chassisCol = bot.getLevel() >= 7 ? new Color(0.95f, 0.75f, 0.18f, 1f)
                : (bot.getLevel() >= 3 ? new Color(0.18f, 0.85f, 0.65f, 1f) : new Color(0.2f, 0.65f, 0.95f, 1f));
        drawBox(bx - 16 * scale, by - 10 * scale + bob, 32 * scale, 20 * scale, chassisCol, Color.BLACK);
        drawBox(bx - 6 * scale, by - 4 * scale + bob, 12 * scale, 8 * scale, Color.YELLOW, Color.BLACK);

        float clawX = (float) (bot.getFacingDx() * 10 * scale);
        float clawY = (float) (bot.getFacingDy() * 6 * scale);
        drawBox(bx - 21 * scale + clawX, by - 4 * scale + clawY + bob, 7 * scale, 8 * scale, Color.LIGHT_GRAY, Color.BLACK);
        drawBox(bx + 14 * scale + clawX, by - 4 * scale + clawY + bob, 7 * scale, 8 * scale, Color.LIGHT_GRAY, Color.BLACK);

        drawBox(bx - 13 * scale, by - 24 * scale + bob, 26 * scale, 15 * scale, chassisCol, Color.BLACK);
        drawBox(bx - 10 * scale, by - 20 * scale + bob, 20 * scale, 8 * scale, new Color(0.08f, 0.12f, 0.16f, 1f), Color.BLACK);
        Color ledCol = (bot.getLevel() >= 5) ? Color.CYAN : Color.GREEN;
        drawBox(bx - 6 * scale, by - 18 * scale + bob, 4 * scale, 4 * scale, ledCol, ledCol);
        drawBox(bx + 2 * scale, by - 18 * scale + bob, 4 * scale, 4 * scale, ledCol, ledCol);

        drawBox(bx - 1 * scale, by - 32 * scale + bob, 3 * scale, 8 * scale, Color.LIGHT_GRAY, Color.BLACK);
        Color beacon = ((int) (animTick * 2) % 2 == 0) ? Color.RED : Color.YELLOW;
        drawBox(bx - 3 * scale, by - 36 * scale + bob, 7 * scale, 5 * scale, beacon, Color.BLACK);
    }

    private void drawPlayerAvatar(float px, float py, float scale, float cycleTick, Direction renderFacing, float jumpHeight) {
        Player p = controller.getPlayer();
        OperatorCosmetics cos = p.getCosmetics();

        boolean activelyWalking = (cycleTick > 0.01f);
        float idleBreath = (float) (Math.sin(p.getIdleCycle() + menuAnimTick * 0.4f) * 1.8f * scale);
        float bob = activelyWalking
                ? (float) (Math.abs(Math.sin(cycleTick)) * 3.0f * scale) - jumpHeight
                : idleBreath - jumpHeight;

        float legSwing = activelyWalking ? (float) (Math.sin(cycleTick) * 4.0f * scale) : 0f;

        drawBox(px - 18 * scale, py + 15 * scale, 36 * scale, 8 * scale, new Color(0f, 0f, 0f, 0.45f), Color.CLEAR);

        if (p.isDashing() && renderFacing == p.getFacing()) {
            drawBox(px - 24 * scale, py - 30 * scale + bob, 48 * scale, 52 * scale, new Color(0f, 1f, 1f, 0.3f), Color.CYAN);
        }

        drawBox(px - 13 * scale, py + 12 * scale + legSwing - jumpHeight * 0.5f, 10 * scale, 7 * scale, Color.DARK_GRAY, Color.BLACK);
        drawBox(px + 3 * scale, py + 12 * scale - legSwing - jumpHeight * 0.5f, 10 * scale, 7 * scale, Color.DARK_GRAY, Color.BLACK);

        float w = 36 * scale;
        float h = 24 * scale;
        drawBox(px - w / 2, py - 8 * scale + bob, w, h, toGdxColor(cos.getSuitColor()), Color.BLACK);
        Color stripeColor = new Color(0.95f, 0.77f, 0.06f, 1f);
        drawBox(px - 12 * scale, py - 8 * scale + bob, 5 * scale, h, stripeColor, Color.BLACK);
        drawBox(px + 7 * scale, py - 8 * scale + bob, 5 * scale, h, stripeColor, Color.BLACK);

        float handX = renderFacing.getDx() * 12 * scale;
        float handY = (jumpHeight > 1f) ? -14 * scale : (renderFacing.getDy() * 8 * scale);
        Color skin = toGdxColor(cos.getSkinColor());
        drawBox(px - 18 * scale + handX, py - 2 * scale + handY + bob - legSwing * 0.5f, 9 * scale, 9 * scale, skin, Color.BLACK);
        drawBox(px + 9 * scale + handX, py - 2 * scale + handY + bob + legSwing * 0.5f, 9 * scale, 9 * scale, skin, Color.BLACK);

        drawBox(px - 14 * scale, py - 24 * scale + bob, 28 * scale, 22 * scale, skin, Color.BLACK);

        Color hat = toGdxColor(cos.getHardhatColor());
        drawBox(px - 15 * scale, py - 33 * scale + bob, 30 * scale, 10 * scale, hat, Color.BLACK);
        drawBox(px - 18 * scale, py - 24 * scale + bob, 36 * scale, 4 * scale, hat, Color.BLACK);

        if (renderFacing != Direction.UP) {
            float eyeOffsetX = renderFacing.getDx() * 4.0f * scale;
            float eyeOffsetY = renderFacing.getDy() * 1.5f * scale;
            int vIdx = cos.getVisorIndex();
            if (vIdx == 3) {
                drawBox(px - 8 * scale + eyeOffsetX, py - 16 * scale + eyeOffsetY + bob, 5 * scale, 5 * scale, Color.WHITE, Color.BLACK);
                drawBox(px + 3 * scale + eyeOffsetX, py - 16 * scale + eyeOffsetY + bob, 5 * scale, 5 * scale, Color.WHITE, Color.BLACK);
                drawBox(px - 5.5f * scale + eyeOffsetX, py - 15 * scale + eyeOffsetY + bob, 2.5f * scale, 3 * scale, Color.BLACK, Color.BLACK);
                drawBox(px + 3.0f * scale + eyeOffsetX, py - 15 * scale + eyeOffsetY + bob, 2.5f * scale, 3 * scale, Color.BLACK, Color.BLACK);
            } else {
                Color visorColor = switch (vIdx) {
                    case 0 -> Color.SKY;
                    case 1 -> Color.CYAN;
                    default -> Color.BLACK;
                };
                drawBox(px - 10 * scale + eyeOffsetX, py - 16 * scale + eyeOffsetY + bob, 20 * scale, 6 * scale, visorColor, Color.WHITE);
            }
        }
    }

    private void drawMobileTouchControls() {
        drawArcadePanel(45, 335, 100, 100, new Color(1f, 1f, 1f, 0.18f), Color.CYAN);
        drawCenteredText(assets.fontSmall, "TOUCH PAD", 95, 385, Color.CYAN);

        drawArcadeButton(new Rectangle(640, 360, 105, 52), "DASH", new Color(0.2f, 0.6f, 0.86f, 0.85f));
        drawArcadeButton(new Rectangle(760, 360, 110, 52), "ACT / THROW", new Color(0.18f, 0.8f, 0.44f, 0.85f));
    }

    private void renderDebriefOverlay() {
        drawBox(0, 0, V_WIDTH, V_HEIGHT, new Color(0f, 0f, 0f, 0.82f), Color.CLEAR);

        boolean won = controller.isGameWon();
        Color borderCol = won ? Color.GOLD : Color.SCARLET;
        drawArcadePanel(120, 42, 660, 436, new Color(0.09f, 0.13f, 0.18f, 0.98f), borderCol);

        String title = won ? "ALL 20 FACILITIES CLEARED!" : "FACILITY SHUTDOWN — GAME OVER";
        drawCenteredText(assets.fontLarge, title, 450, 84, borderCol);

        FacilityLevel lvl = controller.getCurrentLevel();
        String stats = String.format("%s   |   Transported: %d/%d   |   Score: %d   |   Balance: $%d",
                lvl.getTitle(), controller.getTransportedTrash(), lvl.getTrashQuota(),
                controller.getPlayer().getScore(), controller.getPlayer().getCredits());
        drawCenteredText(assets.fontSmall, stats, 450, 122, Color.CYAN);

        drawArcadePanel(150, 146, 600, 248, new Color(0.06f, 0.09f, 0.12f, 1f), Color.DARK_GRAY);
        drawCenteredText(assets.fontMedium, "INCIDENT & VIOLATION LOG", 450, 170, Color.YELLOW);

        List<String> mistakes = controller.getMistakeLog();
        if (mistakes.isEmpty()) {
            drawCenteredText(assets.fontSmall, "Zero compliance violations logged during this shift!", 450, 260, Color.GREEN);
        } else {
            int y = 204;
            for (int i = 0; i < Math.min(6, mistakes.size()); i++) {
                String line = "- " + mistakes.get(i);
                if (line.length() > 74) {
                    line = line.substring(0, 71) + "...";
                }
                assets.fontSmall.setColor(Color.SALMON);
                assets.fontSmall.draw(batch, line, 170, V_HEIGHT - y);
                y += 30;
            }
        }

        drawArcadeButton(btnDebriefRetry, "RESTART [R]",  new Color(0.18f, 0.78f, 0.42f, 1f));
        drawArcadeButton(btnDebriefShop,  "UPGRADE SHOP", new Color(0.88f, 0.55f, 0.10f, 1f));
        drawArcadeButton(btnDebriefMenu,  "MAIN MENU",    new Color(0.25f, 0.50f, 0.80f, 1f));
    }

    private void drawArcadeButton(Rectangle rect, String label, Color bg) {
        boolean hovered = rect.contains(mouseX, mouseY);
        Color fill = hovered ? new Color(Math.min(1f, bg.r + 0.14f), Math.min(1f, bg.g + 0.14f), Math.min(1f, bg.b + 0.14f), bg.a) : bg;
        Color border = hovered ? Color.YELLOW : Color.WHITE;
        drawArcadePanel(rect.x, rect.y, rect.width, rect.height, fill, border);
        drawCenteredText(assets.fontSmall, label, rect.x + rect.width / 2f, rect.y + rect.height / 2f, Color.WHITE);
    }

    // Smooth Rounded-Corner Vibrant UI Panel (Beveled 2-step rounded corners + Specular Gloss + Soft Drop Shadow!)
    private void drawArcadePanel(float topDownX, float topDownY, float w, float h, Color fill, Color border) {
        float gdxY = V_HEIGHT - topDownY - h;
        float r = Math.min(6f, Math.min(w, h) * 0.2f);

        // Soft Rounded Drop Shadow
        batch.setColor(new Color(0f, 0f, 0f, 0.38f));
        drawRoundedRectRaw(topDownX + 3, gdxY - 4, w, h, r);

        // Vibrant Rounded Border
        batch.setColor(border);
        drawRoundedRectRaw(topDownX - 2, gdxY - 2, w + 4, h + 4, r + 1);

        // Smooth Rounded Inner Body
        batch.setColor(fill);
        drawRoundedRectRaw(topDownX, gdxY, w, h, r);

        // Top Vibrant Gloss Highlight
        batch.setColor(new Color(1f, 1f, 1f, 0.22f));
        batch.draw(assets.whitePixel, topDownX + r, gdxY + h - 5, Math.max(0, w - r * 2), 4);

        // Bottom 3D Shade Lip
        batch.setColor(new Color(0f, 0f, 0f, 0.22f));
        batch.draw(assets.whitePixel, topDownX + r, gdxY + 1, Math.max(0, w - r * 2), 4);
        batch.setColor(Color.WHITE);
    }

    private void drawRoundedRectRaw(float x, float gdxY, float w, float h, float r) {
        if (w <= r * 2 || h <= r * 2) {
            batch.draw(assets.whitePixel, x, gdxY, w, h);
            return;
        }
        batch.draw(assets.whitePixel, x + r, gdxY, w - r * 2, h);
        batch.draw(assets.whitePixel, x + r * 0.4f, gdxY + r * 0.4f, w - r * 0.8f, h - r * 0.8f);
        batch.draw(assets.whitePixel, x, gdxY + r, w, h - r * 2);
    }

    private void drawCenteredText(BitmapFont font, String text, float centerX, float centerTopDownY, Color color) {
        font.setColor(color);
        glyphLayout.setText(font, text);
        float drawX = centerX - (glyphLayout.width / 2f);
        float drawY = V_HEIGHT - centerTopDownY + (glyphLayout.height / 2f);
        font.draw(batch, glyphLayout, drawX, drawY);
    }

    private void drawBox(float topDownX, float topDownY, float w, float h, Color fill, Color border) {
        float gdxY = V_HEIGHT - topDownY - h;
        if (border.a > 0f) {
            batch.setColor(border);
            batch.draw(assets.whitePixel, topDownX - 1, gdxY - 1, w + 2, h + 2);
        }
        batch.setColor(fill);
        batch.draw(assets.whitePixel, topDownX, gdxY, w, h);
        batch.setColor(Color.WHITE);
    }

    private Color toGdxColor(java.awt.Color c) {
        return new Color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 1f);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (activeBgmSound != null) {
            activeBgmSound.stop();
        }
        batch.dispose();
        assets.dispose();
    }
}