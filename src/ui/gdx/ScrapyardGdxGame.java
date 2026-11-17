package ui.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
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
import model.shop.UpgradeShop;
import model.station.StorageBay;
import model.station.WorkStation;
import ui.fx.FloatingText;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ScrapyardGdxGame extends ApplicationAdapter {
    public static final int V_WIDTH = 900;
    public static final int V_HEIGHT = 520;

    private enum ScreenState { MAIN_MENU, LOCKER_ROOM, PLAYING, SHOP, MINIGAME }
    private ScreenState screenState = ScreenState.MAIN_MENU;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private GdxAssets assets;
    private GameController controller;

    private boolean mobileHudEnabled = false;
    private boolean bgmMuted = false;
    private int beltAnimOffset = 0;

    // Station Minigame state
    private WorkStation activeMinigameStation = null;
    private final List<Rectangle> minigameTargets = new ArrayList<>();

    // Clickable UI Buttons (in top-down coordinates matching 900x520)
    private final Rectangle btnMenuPlay = new Rectangle(310, 200, 280, 50);
    private final Rectangle btnMenuLocker = new Rectangle(310, 265, 280, 50);
    private final Rectangle btnMenuShop = new Rectangle(310, 330, 280, 50);
    private final Rectangle btnMenuQuit = new Rectangle(310, 395, 280, 50);

    private final Rectangle btnHudShop = new Rectangle(580, 480, 90, 32);
    private final Rectangle btnHudLocker = new Rectangle(680, 480, 95, 32);
    private final Rectangle btnHudMobile = new Rectangle(785, 480, 95, 32);

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(V_WIDTH, V_HEIGHT, camera);
        camera.position.set(V_WIDTH / 2f, V_HEIGHT / 2f, 0);
        camera.update();

        batch = new SpriteBatch();
        assets = new GdxAssets();
        controller = new GameController("Operator Paul");
        controller.setInLockerRoom(false);

        if (assets.bgmLoop != null) {
            assets.bgmLoop.loop(0.35f);
        }

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                Vector3 world = viewport.unproject(new Vector3(screenX, screenY, 0));
                double topDownY = V_HEIGHT - world.y;
                if (screenState == ScreenState.PLAYING) {
                    controller.getPlayer().aimAt(world.x, topDownY);
                }
                return true;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Vector3 world = viewport.unproject(new Vector3(screenX, screenY, 0));
                float mx = world.x;
                float my = V_HEIGHT - world.y; // Convert to top-down Y!
                handleMouseClick(mx, my);
                return true;
            }

            @Override
            public boolean keyDown(int keycode) {
                handleKeyPress(keycode);
                return true;
            }
        });
    }

    private void handleMouseClick(float mx, float my) {
        if (assets.sfxClick != null) assets.sfxClick.play(0.5f);

        if (screenState == ScreenState.MAIN_MENU) {
            if (btnMenuPlay.contains(mx, my)) screenState = ScreenState.PLAYING;
            else if (btnMenuLocker.contains(mx, my)) screenState = ScreenState.LOCKER_ROOM;
            else if (btnMenuShop.contains(mx, my)) screenState = ScreenState.SHOP;
            else if (btnMenuQuit.contains(mx, my)) Gdx.app.exit();
            return;
        }

        if (screenState == ScreenState.LOCKER_ROOM) {
            OperatorCosmetics cos = controller.getPlayer().getCosmetics();
            if (new Rectangle(420, 170, 320, 35).contains(mx, my)) cos.cycleHardhat();
            else if (new Rectangle(420, 215, 320, 35).contains(mx, my)) cos.cycleSuit();
            else if (new Rectangle(420, 260, 320, 35).contains(mx, my)) cos.cycleSkin();
            else if (new Rectangle(420, 305, 320, 35).contains(mx, my)) cos.cycleVisor();
            else if (new Rectangle(290, 440, 320, 48).contains(mx, my)) screenState = ScreenState.PLAYING;
            model.save.SaveManager.save(controller);
            return;
        }

        if (screenState == ScreenState.SHOP) {
            Player p = controller.getPlayer();
            UpgradeShop shop = controller.getShop();
            CleanerBot bot = controller.getCleanerBot();

            if (new Rectangle(120, 150, 660, 55).contains(mx, my)) {
                if (shop.buyBotUpgrade(p, bot) && assets.sfxSuccess != null) assets.sfxSuccess.play();
            } else if (new Rectangle(120, 220, 660, 55).contains(mx, my)) {
                if (shop.buyBootsUpgrade(p) && assets.sfxSuccess != null) assets.sfxSuccess.play();
            } else if (new Rectangle(120, 290, 660, 55).contains(mx, my)) {
                if (shop.buyStationUpgrade(p) && assets.sfxSuccess != null) assets.sfxSuccess.play();
            } else if (new Rectangle(120, 360, 660, 55).contains(mx, my)) {
                if (shop.buyHopperUpgrade(p, controller.getBays()) && assets.sfxSuccess != null) assets.sfxSuccess.play();
            } else if (new Rectangle(310, 435, 280, 45).contains(mx, my)) {
                screenState = ScreenState.PLAYING;
            }
            model.save.SaveManager.save(controller);
            return;
        }

        if (screenState == ScreenState.MINIGAME) {
            Iterator<Rectangle> it = minigameTargets.iterator();
            while (it.hasNext()) {
                if (it.next().contains(mx, my)) {
                    it.remove();
                    if (assets.sfxGrab != null) assets.sfxGrab.play();
                    if (minigameTargets.isEmpty() && activeMinigameStation != null) {
                        for (int i = 0; i < 6; i++) activeMinigameStation.boostProcess();
                        controller.getPlayer().addCredits(20);
                        if (assets.sfxSuccess != null) assets.sfxSuccess.play();
                        screenState = ScreenState.PLAYING;
                        activeMinigameStation = null;
                    }
                    return;
                }
            }
            return;
        }

        // PLAYING STATE: Check bottom HUD buttons first
        if (btnHudShop.contains(mx, my)) { screenState = ScreenState.SHOP; return; }
        if (btnHudLocker.contains(mx, my)) { screenState = ScreenState.LOCKER_ROOM; return; }
        if (btnHudMobile.contains(mx, my)) { mobileHudEnabled = !mobileHudEnabled; return; }

        // Mobile Touch Action Buttons
        if (mobileHudEnabled) {
            if (new Rectangle(760, 390, 110, 55).contains(mx, my)) {
                triggerActionOrMinigame();
                return;
            }
            if (new Rectangle(640, 390, 105, 55).contains(mx, my)) {
                controller.getPlayer().triggerDash();
                return;
            }
        }

        // Aim towards click and trigger unified Interact / Place / Throw!
        controller.getPlayer().aimAt(mx, my);
        triggerActionOrMinigame();
    }

    private void handleKeyPress(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            screenState = (screenState == ScreenState.MAIN_MENU) ? ScreenState.PLAYING : ScreenState.MAIN_MENU;
            return;
        }

        if (screenState == ScreenState.LOCKER_ROOM) {
            OperatorCosmetics cos = controller.getPlayer().getCosmetics();
            if (keycode == Input.Keys.NUM_1) cos.cycleHardhat();
            else if (keycode == Input.Keys.NUM_2) cos.cycleSuit();
            else if (keycode == Input.Keys.NUM_3) cos.cycleSkin();
            else if (keycode == Input.Keys.NUM_4) cos.cycleVisor();
            else if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) screenState = ScreenState.PLAYING;
            return;
        }

        if (screenState == ScreenState.SHOP) {
            if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE || keycode == Input.Keys.B) {
                screenState = ScreenState.PLAYING;
            }
            return;
        }

        if (screenState == ScreenState.PLAYING) {
            if (keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT) {
                controller.getPlayer().triggerDash();
            } else if (keycode == Input.Keys.SPACE || keycode == Input.Keys.E) {
                triggerActionOrMinigame();
            } else if (keycode == Input.Keys.B) {
                screenState = ScreenState.SHOP;
            } else if (keycode == Input.Keys.C) {
                screenState = ScreenState.LOCKER_ROOM;
            } else if (keycode == Input.Keys.M) {
                mobileHudEnabled = !mobileHudEnabled;
            } else if (keycode == Input.Keys.R) {
                controller.resetGame();
            }
        }
    }

    private void triggerActionOrMinigame() {
        Player p = controller.getPlayer();
        // If empty-handed next to a processing station -> Launch interactive Scrub/Dismantle Minigame!
        if (p.getHeldItem() == null) {
            for (WorkStation ws : controller.getStations()) {
                if (ws.isNear(p.getX(), p.getY()) && !ws.isEmpty() && !ws.isFinished()) {
                    activeMinigameStation = ws;
                    minigameTargets.clear();
                    for (int i = 0; i < 3; i++) {
                        float tx = 280 + (float) (Math.random() * 300);
                        float ty = 190 + (float) (Math.random() * 140);
                        minigameTargets.add(new Rectangle(tx, ty, 52, 52));
                    }
                    screenState = ScreenState.MINIGAME;
                    return;
                }
            }
        }

        boolean wasHolding = p.getHeldItem() != null;
        controller.interact();
        if (wasHolding && p.getHeldItem() == null && assets.sfxThrow != null) {
            assets.sfxThrow.play(0.6f);
        } else if (!wasHolding && p.getHeldItem() != null && assets.sfxGrab != null) {
            assets.sfxGrab.play(0.6f);
        }
    }

    @Override
    public void render() {
        updateGameLoop();

        Gdx.gl.glClearColor(0.09f, 0.12f, 0.15f, 1f);
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
            case MAIN_MENU -> renderMainMenu();
            case LOCKER_ROOM -> renderLockerRoom();
            case SHOP -> renderShop();
            case PLAYING -> renderFactoryStage();
            case MINIGAME -> {
                renderFactoryStage();
                renderMinigameOverlay();
            }
        }
        batch.end();
    }

    private void updateGameLoop() {
        if (screenState != ScreenState.PLAYING) return;

        double dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) dy -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) dy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dx -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx += 1;

        // Check Mobile Virtual Joystick touch drag
        if (mobileHudEnabled && Gdx.input.isTouched()) {
            Vector3 world = viewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            float tx = world.x;
            float ty = V_HEIGHT - world.y;
            if (Math.hypot(tx - 95, ty - 405) <= 65) {
                dx = tx - 95;
                dy = ty - 405;
            }
        }

        // Apply UpgradeShop Station Speed Bonus
        for (int i = 0; i < controller.getShop().getStationSpeedBonus(); i++) {
            for (WorkStation ws : controller.getStations()) ws.updateTick();
        }

        controller.updateTick(dx, dy);
        beltAnimOffset = (beltAnimOffset + 2) % 24;
    }

    private void renderMainMenu() {
        drawTiledFloor();
        drawBox(180, 60, 540, 410, new Color(0.1f, 0.14f, 0.18f, 0.95f), Color.CYAN);

        assets.fontLarge.setColor(Color.CYAN);
        assets.fontLarge.draw(batch, "SCRAPYARD SHIFT", 315, V_HEIGHT - 95);
        assets.fontSmall.setColor(Color.LIGHT_GRAY);
        assets.fontSmall.draw(batch, "LibGDX Industrial MRF Simulator -- CIS 2103 Edition", 295, V_HEIGHT - 135);

        drawButton(btnMenuPlay, "START / RESUME SHIFT", new Color(0.18f, 0.8f, 0.44f, 1f));
        drawButton(btnMenuLocker, "OPERATOR LOCKER ROOM", new Color(0.2f, 0.6f, 0.86f, 1f));
        drawButton(btnMenuShop, "MRF UPGRADE SHOP ($" + controller.getPlayer().getCredits() + ")", new Color(0.95f, 0.61f, 0.07f, 1f));
        drawButton(btnMenuQuit, "EXIT TO DESKTOP", new Color(0.75f, 0.22f, 0.17f, 1f));
    }

    private void renderLockerRoom() {
        drawTiledFloor();
        drawBox(80, 45, 740, 445, new Color(0.1f, 0.14f, 0.18f, 0.96f), Color.CYAN);

        assets.fontLarge.setColor(Color.YELLOW);
        assets.fontLarge.draw(batch, "OPERATOR LOCKER ROOM", 265, V_HEIGHT - 70);

        // Avatar Preview Card
        drawBox(125, 130, 240, 280, new Color(0.16f, 0.2f, 0.24f, 1f), Color.WHITE);
        drawPlayerAvatar(245, 270, 2.2f);

        Player p = controller.getPlayer();
        drawButton(new Rectangle(420, 170, 320, 35), "[1] Click to Cycle Hardhat", toGdxColor(p.getCosmetics().getHardhatColor()));
        drawButton(new Rectangle(420, 215, 320, 35), "[2] Click to Cycle Hazmat Suit", toGdxColor(p.getCosmetics().getSuitColor()));
        drawButton(new Rectangle(420, 260, 320, 35), "[3] Click to Cycle Skin Tone", new Color(0.4f, 0.45f, 0.5f, 1f));
        drawButton(new Rectangle(420, 305, 320, 35), "[4] Visor: " + p.getCosmetics().getVisorStyle(), new Color(0.2f, 0.6f, 0.86f, 1f));

        drawButton(new Rectangle(290, 440, 320, 44), "CONFIRM & DEPLOY TO FLOOR", new Color(0.18f, 0.8f, 0.44f, 1f));
    }

    private void renderShop() {
        drawTiledFloor();
        drawBox(75, 40, 750, 450, new Color(0.1f, 0.13f, 0.17f, 0.96f), Color.GOLD);

        Player p = controller.getPlayer();
        UpgradeShop shop = controller.getShop();
        CleanerBot bot = controller.getCleanerBot();

        assets.fontLarge.setColor(Color.GOLD);
        assets.fontLarge.draw(batch, "MRF UPGRADE SHOP   |   CREDITS: $" + p.getCredits(), 165, V_HEIGHT - 70);

        drawButton(new Rectangle(120, 150, 660, 52),
                "Assistant CleanerBot (Lv." + bot.getLevel() + "/3) : Auto-collects & sorts trash   [$" + shop.getBotCost(bot) + "]",
                new Color(0.16f, 0.5f, 0.72f, 1f));

        drawButton(new Rectangle(120, 220, 660, 52),
                "Pneumatic Speed Boots (Lv." + shop.getBootsLevel() + "/3) : +20% Walk & Dash speed   [$" + shop.getBootsCost() + "]",
                new Color(0.18f, 0.65f, 0.45f, 1f));

        drawButton(new Rectangle(120, 290, 660, 52),
                "Turbo Station Overclock (Lv." + shop.getStationLevel() + "/3) : Faster Wash/Dismantle   [$" + shop.getStationCost() + "]",
                new Color(0.85f, 0.45f, 0.12f, 1f));

        drawButton(new Rectangle(120, 360, 660, 52),
                "Expanded Bay Hoppers (Lv." + shop.getHopperLevel() + "/3) : +2 Max Capacity all bays   [$" + shop.getHopperCost() + "]",
                new Color(0.55f, 0.27f, 0.68f, 1f));

        drawButton(new Rectangle(310, 435, 280, 42), "BACK TO SHIFT [SPACE / CLICK]", new Color(0.3f, 0.35f, 0.4f, 1f));
    }

    private void renderFactoryStage() {
        drawTiledFloor();
        drawTopHud();
        drawConveyorAndIncinerator();
        drawWorkStationsAndForklift();
        drawBays();

        // Draw Thrown / Floor Items
        for (WasteItem item : controller.getThrownItems()) {
            drawWasteItem(item);
        }

        // Draw Assistant CleanerBot if unlocked
        CleanerBot bot = controller.getCleanerBot();
        if (bot.getLevel() > 0) {
            batch.setColor(Color.WHITE);
            batch.draw(assets.botTexture, (float) bot.getX() - 18, V_HEIGHT - (float) bot.getY() - 18);
            assets.fontSmall.setColor(Color.CYAN);
            assets.fontSmall.draw(batch, "BOT Mk." + bot.getLevel(), (float) bot.getX() - 22, V_HEIGHT - (float) bot.getY() + 28);
            if (bot.getHeldItem() != null) drawWasteItem(bot.getHeldItem());
        }

        // Draw Aim Line when holding an item
        Player p = controller.getPlayer();
        if (p.getHeldItem() != null) {
            batch.setColor(new Color(1f, 1f, 1f, 0.55f));
            for (int step = 15; step <= 90; step += 15) {
                float ax = (float) (p.getX() + p.getThrowDx() * step);
                float ay = (float) (p.getY() + p.getThrowDy() * step);
                batch.draw(assets.whitePixel, ax - 2, V_HEIGHT - ay - 2, 5, 5);
            }
        }

        // Draw Player Avatar
        drawPlayerAvatar((float) p.getX(), (float) p.getY(), 1.0f);
        if (p.getHeldItem() != null) {
            drawWasteItem(p.getHeldItem());
        }

        // Draw Floating Popups
        for (FloatingText ft : controller.getPopups()) {
            Color c = toGdxColor(ft.getColor());
            assets.fontMedium.setColor(c.r, c.g, c.b, ft.getAlpha());
            assets.fontMedium.draw(batch, ft.getText(), (float) ft.getX(), V_HEIGHT - (float) ft.getY());
        }

        if (mobileHudEnabled) {
            drawMobileTouchControls();
        }

        drawFooterBar();

        if (controller.isGameOver() || controller.isGameWon()) {
            renderDebriefOverlay();
        }
    }

    private void renderMinigameOverlay() {
        drawBox(220, 120, 460, 280, new Color(0.05f, 0.08f, 0.12f, 0.95f), Color.YELLOW);
        assets.fontMedium.setColor(Color.YELLOW);
        assets.fontMedium.draw(batch, "STATION OVERDRIVE MINIGAME!", 310, V_HEIGHT - 140);
        assets.fontSmall.setColor(Color.WHITE);
        assets.fontSmall.draw(batch, "Click all 3 nodes to instantly finish processing (+20 Credits)!", 275, V_HEIGHT - 170);

        for (Rectangle target : minigameTargets) {
            drawBox(target.x, target.y, target.width, target.height, new Color(0.9f, 0.25f, 0.2f, 1f), Color.WHITE);
            assets.fontSmall.setColor(Color.WHITE);
            assets.fontSmall.draw(batch, "CLICK", target.x + 8, V_HEIGHT - target.y - 20);
        }
    }

    private void drawTiledFloor() {
        batch.setColor(Color.WHITE);
        for (int x = 0; x < V_WIDTH; x += 45) {
            for (int y = 0; y < V_HEIGHT; y += 45) {
                boolean alt = ((x / 45) + (y / 45)) % 2 == 0;
                batch.draw(alt ? assets.floorTileA : assets.floorTileB, x, y);
            }
        }
    }

    private void drawTopHud() {
        drawBox(0, 0, V_WIDTH, 55, new Color(0.06f, 0.73f, 0.97f, 1f), Color.BLACK);
        Player p = controller.getPlayer();
        FacilityLevel lvl = controller.getCurrentLevel();

        assets.fontMedium.setColor(Color.BLACK);
        assets.fontMedium.draw(batch, lvl.getTitle() + " (" + lvl.getTargetScore() + ")", 16, V_HEIGHT - 8);
        assets.fontMedium.draw(batch, "Score: " + p.getScore() + " | $" + p.getCredits() + " | Streak: x" + p.getStreakMeter().getValue(), 415, V_HEIGHT - 8);
        assets.fontMedium.draw(batch, "Compliance: " + p.getComplianceMeter().getValue() + "%", 730, V_HEIGHT - 8);

        drawBox(16, 30, 868, 21, new Color(0.96f, 0.96f, 0.98f, 1f), Color.DARK_GRAY);
        assets.fontSmall.setColor(Color.BLACK);
        assets.fontSmall.draw(batch, controller.getLastFeedback(), 26, V_HEIGHT - 35);
    }

    private void drawConveyorAndIncinerator() {
        int bx = GameController.BELT_START_X;
        int by = GameController.LANE_Y - 8;
        int bw = GameController.BELT_END_X - bx;

        drawBox(bx - 8, by - 5, bw + 16, 66, new Color(0.28f, 0.33f, 0.38f, 1f), Color.LIGHT_GRAY);
        drawBox(bx, by, bw, 56, new Color(0.18f, 0.21f, 0.25f, 1f), Color.BLACK);

        batch.setColor(new Color(0.45f, 0.5f, 0.58f, 1f));
        for (int rx = bx + beltAnimOffset; rx < GameController.BELT_END_X; rx += 24) {
            batch.draw(assets.whitePixel, rx, V_HEIGHT - (by + 54), 3, 52);
        }

        batch.setColor(Color.WHITE);
        batch.draw(assets.incineratorTexture, GameController.BELT_END_X, V_HEIGHT - (by - 5) - 66);
        assets.fontSmall.setColor(Color.WHITE);
        assets.fontSmall.draw(batch, "INCINERATOR", GameController.BELT_END_X + 7, V_HEIGHT - (by + 22));

        for (WasteItem item : controller.getBeltItems()) {
            drawWasteItem(item);
        }
    }

    private void drawWorkStationsAndForklift() {
        for (WorkStation ws : controller.getStations()) {
            float sx = (float) ws.getX();
            float sy = (float) ws.getY();
            batch.setColor(Color.WHITE);
            batch.draw(assets.getStationSprite(ws.getType()), sx, V_HEIGHT - sy - 70);
            assets.fontSmall.setColor(Color.WHITE);
            assets.fontSmall.draw(batch, ws.getType().name(), sx + 10, V_HEIGHT - (sy + 4));

            if (ws.getStoredItem() != null) {
                drawWasteItem(ws.getStoredItem());
                drawBox(sx + 10, sy - 14, 90, 8, Color.BLACK, Color.WHITE);
                batch.setColor(ws.isFinished() ? Color.GREEN : Color.YELLOW);
                batch.draw(assets.whitePixel, sx + 11, V_HEIGHT - (sy - 7), 88 * ws.getProgressRatio(), 6);
            } else {
                assets.fontSmall.setColor(Color.LIGHT_GRAY);
                assets.fontSmall.draw(batch, "[Click/Space]", sx + 14, V_HEIGHT - (sy + 44));
            }
        }

        float fx = GameController.FORKLIFT_X;
        float fy = GameController.FORKLIFT_Y;
        batch.setColor(Color.WHITE);
        batch.draw(assets.forkliftTexture, fx, V_HEIGHT - fy - 70);
        assets.fontSmall.setColor(Color.BLACK);
        assets.fontSmall.draw(batch, "FORKLIFT BAY", fx + 12, V_HEIGHT - (fy + 5));
        assets.fontSmall.setColor(Color.WHITE);
        assets.fontSmall.draw(batch, "[Space/Click]", fx + 14, V_HEIGHT - (fy + 38));
    }

    private void drawBays() {
        BinType highlighted = controller.getClosestBayType(controller.getPlayer().getY());
        for (StorageBay bay : controller.getBays()) {
            boolean isTarget = controller.getPlayer().getX() >= 520 && bay.getType() == highlighted;
            if (isTarget) {
                drawBox(bay.getX() - 2, bay.getY() - 2, 149, 66, Color.GREEN, Color.GREEN);
            }
            batch.setColor(Color.WHITE);
            batch.draw(assets.getBaySprite(bay.getType()), bay.getX(), V_HEIGHT - bay.getY() - 62);
            assets.fontSmall.setColor(Color.BLACK);
            assets.fontSmall.draw(batch, bay.getType().name(), bay.getX() + 10, V_HEIGHT - (bay.getY() + 3));
            assets.fontSmall.setColor(bay.isFull() ? Color.RED : Color.CYAN);
            assets.fontSmall.draw(batch, "Fill: " + bay.getFillLevel() + "/" + bay.getCapacity(), bay.getX() + 12, V_HEIGHT - (bay.getY() + 32));
        }
    }

    private void drawWasteItem(WasteItem item) {
        float ix = (float) item.getX();
        float iy = (float) item.getY();

        // Draw floor shadow if the item is currently flying through the air
        if (item.isAirborne()) {
            drawBox(ix + 8, iy + 28, 36, 10, new Color(0f, 0f, 0f, 0.35f), new Color(0f, 0f, 0f, 0f));
        }

        batch.setColor(Color.WHITE);
        batch.draw(assets.getItemSprite(item.getDisplayTag()), ix, V_HEIGHT - iy - 40);
    }

    private void drawPlayerAvatar(float px, float py, float scale) {
        Player p = controller.getPlayer();
        OperatorCosmetics cos = p.getCosmetics();
        float bob = (float) (Math.sin(p.getWalkCycle()) * 3.0f * scale);
        float legSwing = (float) (Math.cos(p.getWalkCycle()) * 4.5f * scale);

        // 1. Drop Shadow
        drawBox(px - 18 * scale, py + 14 * scale, 36 * scale, 8 * scale, new Color(0f, 0f, 0f, 0.45f), new Color(0f, 0f, 0f, 0f));

        // 2. Cyan Dash Aura
        if (p.isDashing()) {
            drawBox(px - 24 * scale, py - 30 * scale + bob, 48 * scale, 52 * scale, new Color(0f, 1f, 1f, 0.3f), Color.CYAN);
        }

        // 3. Animated Walking Boots
        drawBox(px - 13 * scale, py + 10 * scale + legSwing, 10 * scale, 8 * scale, Color.DARK_GRAY, Color.BLACK);
        drawBox(px + 3 * scale, py + 10 * scale - legSwing, 10 * scale, 8 * scale, Color.DARK_GRAY, Color.BLACK);

        // 4. Hazmat Jumpsuit & Reflective Hi-Vis Stripes
        float w = 36 * scale;
        float h = 24 * scale;
        drawBox(px - w / 2, py - 8 * scale + bob, w, h, toGdxColor(cos.getSuitColor()), Color.BLACK);
        Color stripeColor = new Color(0.95f, 0.77f, 0.06f, 1f);
        drawBox(px - 12 * scale, py - 8 * scale + bob, 5 * scale, h, stripeColor, Color.BLACK);
        drawBox(px + 7 * scale, py - 8 * scale + bob, 5 * scale, h, stripeColor, Color.BLACK);

        // 5. Directional Reaching Hands
        float handX = p.getFacing().getDx() * 12 * scale;
        float handY = p.getFacing().getDy() * 8 * scale;
        Color skin = toGdxColor(cos.getSkinColor());
        drawBox(px - 18 * scale + handX, py - 2 * scale + handY + bob - legSwing * 0.5f, 9 * scale, 9 * scale, skin, Color.BLACK);
        drawBox(px + 9 * scale + handX, py - 2 * scale + handY + bob + legSwing * 0.5f, 9 * scale, 9 * scale, skin, Color.BLACK);

        // 6. Head
        drawBox(px - 14 * scale, py - 24 * scale + bob, 28 * scale, 22 * scale, skin, Color.BLACK);

        // 7. Hardhat Dome & Wide Brim
        Color hat = toGdxColor(cos.getHardhatColor());
        drawBox(px - 15 * scale, py - 33 * scale + bob, 30 * scale, 10 * scale, hat, Color.BLACK);
        drawBox(px - 18 * scale, py - 24 * scale + bob, 36 * scale, 4 * scale, hat, Color.BLACK);

        // 8. Directional Visor / Goggles
        if (p.getFacing() != Direction.UP) {
            float eyeOffsetX = p.getFacing().getDx() * 4 * scale;
            Color visorColor = switch (cos.getVisorIndex()) {
                case 0 -> Color.SKY;
                case 1 -> Color.CYAN;
                default -> Color.BLACK;
            };
            drawBox(px - 10 * scale + eyeOffsetX, py - 16 * scale + bob, 20 * scale, 6 * scale, visorColor, Color.WHITE);
        }
    }

    private void drawMobileTouchControls() {
        drawBox(45, 355, 100, 100, new Color(1f, 1f, 1f, 0.18f), Color.CYAN);
        assets.fontSmall.setColor(Color.CYAN);
        assets.fontSmall.draw(batch, "TOUCH PAD", 62, V_HEIGHT - 398);

        drawButton(new Rectangle(640, 390, 105, 55), "DASH", new Color(0.2f, 0.6f, 0.86f, 0.8f));
        drawButton(new Rectangle(760, 390, 110, 55), "ACT / THROW", new Color(0.18f, 0.8f, 0.44f, 0.8f));
    }

    private void drawFooterBar() {
        drawBox(0, 475, V_WIDTH, 45, new Color(0.08f, 0.1f, 0.12f, 1f), Color.DARK_GRAY);
        assets.fontSmall.setColor(Color.WHITE);
        assets.fontSmall.draw(batch, "[WASD] Move  [SHIFT] Dash  [SPACE/Click] Act / Throw  [R] Retry  [ESC] Menu", 14, V_HEIGHT - 492);

        drawButton(btnHudShop, "SHOP [B]", new Color(0.85f, 0.55f, 0.1f, 1f));
        drawButton(btnHudLocker, "LOCKER [C]", new Color(0.2f, 0.55f, 0.8f, 1f));
        drawButton(btnHudMobile, mobileHudEnabled ? "MOBILE: ON" : "MOBILE [M]", new Color(0.3f, 0.4f, 0.5f, 1f));
    }

    private void renderDebriefOverlay() {
        drawBox(0, 0, V_WIDTH, V_HEIGHT, new Color(0f, 0f, 0f, 0.85f), Color.BLACK);
        assets.fontLarge.setColor(Color.WHITE);
        String header = controller.isGameWon() ? "ALL SHIFTS COMPLETED!" : "SHIFT INCIDENT DEBRIEF";
        assets.fontLarge.draw(batch, header, 270, V_HEIGHT - 85);

        int y = 140;
        for (int i = 0; i < Math.min(8, controller.getMistakeLog().size()); i++) {
            y += 28;
            assets.fontSmall.setColor(Color.SALMON);
            assets.fontSmall.draw(batch, "• " + controller.getMistakeLog().get(i), 140, V_HEIGHT - y);
        }
        assets.fontMedium.setColor(Color.YELLOW);
        assets.fontMedium.draw(batch, "Press [R] to Start New Shift", 335, V_HEIGHT - 430);
    }

    private void drawButton(Rectangle rect, String label, Color bg) {
        drawBox(rect.x, rect.y, rect.width, rect.height, bg, Color.WHITE);
        assets.fontSmall.setColor(Color.WHITE);
        assets.fontSmall.draw(batch, label, rect.x + 14, V_HEIGHT - (rect.y + rect.height / 2f - 5));
    }

    private void drawBox(float topDownX, float topDownY, float w, float h, Color fill, Color border) {
        float gdxY = V_HEIGHT - topDownY - h;
        batch.setColor(border);
        batch.draw(assets.whitePixel, topDownX - 1, gdxY - 1, w + 2, h + 2);
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
        batch.dispose();
        assets.dispose();
    }
}