package me.jadenp.notranks;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import static org.bukkit.Bukkit.getServer;

public class VaultClass {
    private Economy economy = null;
    private boolean working;
    public VaultClass(){
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            working = false;
            Bukkit.getLogger().warning("Could not get registered service provider from Vault");
            return;
        }
        economy = rsp.getProvider();
    }

    /**
     * Try to get a registered service provider from vault
     * @return true if the attempt was unsuccessful
     */
    private boolean tryRegister(){
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        //Bukkit.getLogger().warning("Could not get registered service provider from Vault");
        working = rsp != null;
        return !working;
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!working && tryRegister())
            return false;
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean withdraw(Player player, double amount) {
        if (!working && tryRegister())
            return false;
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean checkBalance(OfflinePlayer player, double amount) {
        if (!working && tryRegister())
            return false;
        return economy.getBalance(player) >= amount;
    }

    public double getBalance(OfflinePlayer player) {
        if (!working && tryRegister())
            return 0;
        return economy.getBalance(player);
    }
}
