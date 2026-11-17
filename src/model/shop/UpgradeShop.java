package model.shop;

import model.bot.CleanerBot;
import model.player.Player;
import model.station.StorageBay;

import java.util.Collection;

public class UpgradeShop {
    private int bootsLevel = 0;      // Increases Player walk/dash speed
    private int stationLevel = 0;    // Increases WorkStation processing speed
    private int hopperLevel = 0;     // Increases StorageBay max capacity

    public int getBotCost(CleanerBot bot) { return (bot.getLevel() + 1) * 75; }
    public int getBootsCost() { return (bootsLevel + 1) * 60; }
    public int getStationCost() { return (stationLevel + 1) * 65; }
    public int getHopperCost() { return (hopperLevel + 1) * 80; }

    public boolean buyBotUpgrade(Player player, CleanerBot bot) {
        int cost = getBotCost(bot);
        if (bot.getLevel() < 3 && player.spendCredits(cost)) {
            bot.upgrade();
            return true;
        }
        return false;
    }

    public boolean buyBootsUpgrade(Player player) {
        int cost = getBootsCost();
        if (bootsLevel < 3 && player.spendCredits(cost)) {
            bootsLevel++;
            player.setSpeedMultiplier(1.0 + (bootsLevel * 0.20));
            return true;
        }
        return false;
    }

    public boolean buyStationUpgrade(Player player) {
        int cost = getStationCost();
        if (stationLevel < 3 && player.spendCredits(cost)) {
            stationLevel++;
            return true;
        }
        return false;
    }

    public boolean buyHopperUpgrade(Player player, Collection<StorageBay> bays) {
        int cost = getHopperCost();
        if (hopperLevel < 3 && player.spendCredits(cost)) {
            hopperLevel++;
            for (StorageBay bay : bays) {
                bay.increaseCapacity(2);
            }
            return true;
        }
        return false;
    }

    public void applySavedLevels(int boots, int station, int hopper, Player player, Collection<StorageBay> bays) {
        this.bootsLevel = Math.max(0, Math.min(3, boots));
        this.stationLevel = Math.max(0, Math.min(3, station));
        this.hopperLevel = Math.max(0, Math.min(3, hopper));

        player.setSpeedMultiplier(1.0 + (this.bootsLevel * 0.20));
        for (StorageBay bay : bays) {
            bay.resetCapacity();
            bay.increaseCapacity(this.hopperLevel * 2);
        }
    }

    public int getBootsLevel() { return bootsLevel; }
    public int getStationLevel() { return stationLevel; }
    public int getHopperLevel() { return hopperLevel; }
    public int getStationSpeedBonus() { return stationLevel; }
}