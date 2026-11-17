package model.save;

import game.GameController;
import model.bot.CleanerBot;
import model.player.OperatorCosmetics;
import model.player.Player;
import model.shop.UpgradeShop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class SaveManager {
    private static int activeSlot = 1;

    private static String getSlotFilename(int slot) {
        int s = Math.max(1, Math.min(5, slot));
        return "scrapyard_slot_" + s + ".properties";
    }

    public static int getActiveSlot() { return activeSlot; }
    public static void setActiveSlot(int slot) { activeSlot = Math.max(1, Math.min(5, slot)); }

    public static boolean slotExists(int slot) {
        return new File(getSlotFilename(slot)).exists();
    }

    public static void createNewSlot(int slot, GameController controller) {
        activeSlot = Math.max(1, Math.min(5, slot));
        controller.resetToBrandNewSave();
        saveSlot(activeSlot, controller);
    }

    public static void deleteSlot(int slot) {
        File file = new File(getSlotFilename(slot));
        if (file.exists()) {
            file.delete();
        }
    }

    public static void save(GameController controller) {
        saveSlot(activeSlot, controller);
    }

    public static void saveSlot(int slot, GameController controller) {
        activeSlot = Math.max(1, Math.min(5, slot));
        Properties props = new Properties();
        Player p = controller.getPlayer();
        OperatorCosmetics cos = p.getCosmetics();
        UpgradeShop shop = controller.getShop();
        CleanerBot bot = controller.getCleanerBot();

        props.setProperty("levelIndex", String.valueOf(controller.getCurrentLevelIndex()));
        props.setProperty("maxUnlockedLevel", String.valueOf(controller.getMaxUnlockedLevel()));
        props.setProperty("hasActiveRun", String.valueOf(controller.hasActiveRun()));
        props.setProperty("transportedTrash", String.valueOf(controller.getTransportedTrash()));
        props.setProperty("score", String.valueOf(p.getScore()));
        props.setProperty("credits", String.valueOf(p.getCredits()));
        props.setProperty("botLevel", String.valueOf(Math.max(bot.getLevel(), shop.getPurchasedBotLevel())));
        props.setProperty("bootsLevel", String.valueOf(Math.max(shop.getBootsLevel(), shop.getPurchasedBootsLevel())));
        props.setProperty("stationLevel", String.valueOf(Math.max(shop.getStationLevel(), shop.getPurchasedStationLevel())));
        props.setProperty("hopperLevel", String.valueOf(Math.max(shop.getHopperLevel(), shop.getPurchasedHopperLevel())));

        props.setProperty("hardhatIndex", String.valueOf(cos.getHardhatIndex()));
        props.setProperty("suitIndex", String.valueOf(cos.getSuitIndex()));
        props.setProperty("skinIndex", String.valueOf(cos.getSkinIndex()));
        props.setProperty("visorIndex", String.valueOf(cos.getVisorIndex()));

        try (FileOutputStream out = new FileOutputStream(getSlotFilename(activeSlot))) {
            props.store(out, "Scrapyard Shift Save Slot " + activeSlot);
        } catch (Exception ignored) {}
    }

    public static void load(GameController controller) {
        File legacy = new File("scrapyard_save.properties");
        File slot1 = new File(getSlotFilename(1));
        if (legacy.exists() && !slot1.exists()) {
            legacy.renameTo(slot1);
        }
        if (slotExists(activeSlot)) {
            loadSlot(activeSlot, controller);
        } else {
            controller.resetToBrandNewSave();
        }
    }

    public static boolean loadSlot(int slot, GameController controller) {
        activeSlot = Math.max(1, Math.min(5, slot));
        File file = new File(getSlotFilename(activeSlot));
        if (!file.exists()) return false;

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);

            Player p = controller.getPlayer();
            OperatorCosmetics cos = p.getCosmetics();
            UpgradeShop shop = controller.getShop();
            CleanerBot bot = controller.getCleanerBot();

            int savedLvl = Integer.parseInt(props.getProperty("levelIndex", "0"));
            int savedMaxLvl = Integer.parseInt(props.getProperty("maxUnlockedLevel", String.valueOf(savedLvl)));
            boolean activeRun = Boolean.parseBoolean(props.getProperty("hasActiveRun", "false"));
            int savedScore = Integer.parseInt(props.getProperty("score", "0"));
            int savedHaul = Integer.parseInt(props.getProperty("transportedTrash", "0"));

            controller.loadSavedProgress(savedLvl, savedScore);
            controller.setMaxUnlockedLevel(Math.max(savedLvl, savedMaxLvl));
            controller.setHasActiveRun(activeRun || savedMaxLvl > 0 || savedScore > 0 || savedHaul > 0);
            controller.setTransportedTrash(savedHaul);

            p.setCredits(Integer.parseInt(props.getProperty("credits", "0")));
            int savedBotLv = Integer.parseInt(props.getProperty("botLevel", "0"));
            bot.setLevel(savedBotLv);
            shop.syncBotSavedLevel(savedBotLv);

            int boots = Integer.parseInt(props.getProperty("bootsLevel", "0"));
            int station = Integer.parseInt(props.getProperty("stationLevel", "0"));
            int hopper = Integer.parseInt(props.getProperty("hopperLevel", "0"));
            shop.applySavedLevels(boots, station, hopper, p, controller.getBays());

            cos.setSavedIndices(
                    Integer.parseInt(props.getProperty("hardhatIndex", "0")),
                    Integer.parseInt(props.getProperty("suitIndex", "0")),
                    Integer.parseInt(props.getProperty("skinIndex", "1")),
                    Integer.parseInt(props.getProperty("visorIndex", "0"))
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getSlotSummary(int slot) {
        File file = new File(getSlotFilename(slot));
        if (!file.exists()) {
            return "SAVE " + slot + "  --  [ EMPTY SLOT ]";
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
            int unlocked = Integer.parseInt(props.getProperty("maxUnlockedLevel",
                    props.getProperty("levelIndex", "0"))) + 1;
            int score = Integer.parseInt(props.getProperty("score", "0"));
            int credits = Integer.parseInt(props.getProperty("credits", "0"));
            int botLv = Integer.parseInt(props.getProperty("botLevel", "0"));
            return String.format("SAVE %d  --  Facility %d/20  |  Score: %d  |  $%d  |  Bot Lv.%d",
                    slot, unlocked, score, credits, botLv);
        } catch (Exception e) {
            return "SAVE " + slot + "  --  [ CORRUPTED ]";
        }
    }
}