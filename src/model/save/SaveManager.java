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
    private static final String SAVE_FILE = "scrapyard_save.properties";

    public static void save(GameController controller) {
        Properties props = new Properties();
        Player p = controller.getPlayer();
        OperatorCosmetics cos = p.getCosmetics();
        UpgradeShop shop = controller.getShop();
        CleanerBot bot = controller.getCleanerBot();

        props.setProperty("credits", String.valueOf(p.getCredits()));
        props.setProperty("botLevel", String.valueOf(bot.getLevel()));
        props.setProperty("bootsLevel", String.valueOf(shop.getBootsLevel()));
        props.setProperty("stationLevel", String.valueOf(shop.getStationLevel()));
        props.setProperty("hopperLevel", String.valueOf(shop.getHopperLevel()));

        props.setProperty("hardhatIndex", String.valueOf(cos.getHardhatIndex()));
        props.setProperty("suitIndex", String.valueOf(cos.getSuitIndex()));
        props.setProperty("skinIndex", String.valueOf(cos.getSkinIndex()));
        props.setProperty("visorIndex", String.valueOf(cos.getVisorIndex()));

        try (FileOutputStream out = new FileOutputStream(SAVE_FILE)) {
            props.store(out, "Scrapyard Shift Player Save Data");
        } catch (Exception ignored) {}
    }

    public static void load(GameController controller) {
        File file = new File(SAVE_FILE);
        if (!file.exists()) return;

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);

            Player p = controller.getPlayer();
            OperatorCosmetics cos = p.getCosmetics();
            UpgradeShop shop = controller.getShop();
            CleanerBot bot = controller.getCleanerBot();

            p.setCredits(Integer.parseInt(props.getProperty("credits", "0")));
            bot.setLevel(Integer.parseInt(props.getProperty("botLevel", "0")));

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
        } catch (Exception ignored) {}
    }
}