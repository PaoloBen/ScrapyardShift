package model.shop;

import model.bot.CleanerBot;
import model.player.Player;
import model.station.StorageBay;

import java.util.Collection;

public class UpgradeShop {
    // Active levels currently in effect for this shift
    private int bootsLevel = 0;
    private int stationLevel = 0;
    private int hopperLevel = 0;

    // Total purchased levels (applied when the current shift ends!)
    private int purchasedBotLevel = 0;
    private int purchasedBootsLevel = 0;
    private int purchasedStationLevel = 0;
    private int purchasedHopperLevel = 0;

    public int getBotCost() { return (purchasedBotLevel + 1) * 75; }
    public int getBootsCost() { return (purchasedBootsLevel + 1) * 60; }
    public int getStationCost() { return (purchasedStationLevel + 1) * 65; }
    public int getHopperCost() { return (purchasedHopperLevel + 1) * 80; }

    public boolean buyBotUpgrade(Player player, CleanerBot bot) {
        int cost = getBotCost();
        if (purchasedBotLevel < 3 && player.spendCredits(cost)) {
            purchasedBotLevel++;
            return true;
        }
        return false;
    }

    public boolean buyBootsUpgrade(Player player) {
        int cost = getBootsCost();
        if (purchasedBootsLevel < 3 && player.spendCredits(cost)) {
            purchasedBootsLevel++;
            return true;
        }
        return false;
    }

    public boolean buyStationUpgrade(Player player) {
        int cost = getStationCost();
        if (purchasedStationLevel < 3 && player.spendCredits(cost)) {
            purchasedStationLevel++;
            return true;
        }
        return false;
    }

    public boolean buyHopperUpgrade(Player player, Collection<StorageBay> bays) {
        int cost = getHopperCost();
        if (purchasedHopperLevel < 3 && player.spendCredits(cost)) {
            purchasedHopperLevel++;
            return true;
        }
        return false;
    }

    // Called when a shift finishes (or when loading a save) to activate all queued upgrades!
    public void applyPendingUpgrades(Player player, CleanerBot bot, Collection<StorageBay> bays) {
        bot.setLevel(purchasedBotLevel);
        this.bootsLevel = purchasedBootsLevel;
        this.stationLevel = purchasedStationLevel;
        this.hopperLevel = purchasedHopperLevel;

        player.setSpeedMultiplier(1.0 + (this.bootsLevel * 0.20));
        for (StorageBay bay : bays) {
            bay.resetCapacity();
            bay.increaseCapacity(this.hopperLevel * 2);
        }
    }

    public void applySavedLevels(int boots, int station, int hopper, Player player, Collection<StorageBay> bays) {
        this.bootsLevel = Math.max(0, Math.min(3, boots));
        this.stationLevel = Math.max(0, Math.min(3, station));
        this.hopperLevel = Math.max(0, Math.min(3, hopper));
        this.purchasedBootsLevel = this.bootsLevel;
        this.purchasedStationLevel = this.stationLevel;
        this.purchasedHopperLevel = this.hopperLevel;

        player.setSpeedMultiplier(1.0 + (this.bootsLevel * 0.20));
        for (StorageBay bay : bays) {
            bay.resetCapacity();
            bay.increaseCapacity(this.hopperLevel * 2);
        }
    }

    public void syncBotSavedLevel(int botLvl) {
        this.purchasedBotLevel = Math.max(0, Math.min(3, botLvl));
    }

    public void resetAll(Player player, Collection<StorageBay> bays) {
        this.bootsLevel = 0;
        this.stationLevel = 0;
        this.hopperLevel = 0;
        this.purchasedBotLevel = 0;
        this.purchasedBootsLevel = 0;
        this.purchasedStationLevel = 0;
        this.purchasedHopperLevel = 0;
        player.setSpeedMultiplier(1.0);
        for (StorageBay bay : bays) {
            bay.resetCapacity();
        }
    }

    public int getBootsLevel() { return bootsLevel; }
    public int getStationLevel() { return stationLevel; }
    public int getHopperLevel() { return hopperLevel; }
    public int getPurchasedBotLevel() { return purchasedBotLevel; }
    public int getPurchasedBootsLevel() { return purchasedBootsLevel; }
    public int getPurchasedStationLevel() { return purchasedStationLevel; }
    public int getPurchasedHopperLevel() { return purchasedHopperLevel; }
    public int getStationSpeedBonus() { return stationLevel; }
}