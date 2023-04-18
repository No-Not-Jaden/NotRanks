package me.jadenp.notranks;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConfigOptions {
    public static ArrayList<Rank> ranks = new ArrayList<>();
    public static boolean HDBEnabled;
    public static String currency;
    public static boolean usingPlaceholderCurrency;
    public static List<String> removeCommands;
    public static String currencyPrefix;
    public static String currencySuffix;
    public static int decimals;
    public static boolean addPrefix;
    public static boolean overwritePrefix;
    public static String prefixFormat;
    public static String noRank;

    public static void loadConfig(){
        NotRanks plugin = NotRanks.getInstance();

        HDBEnabled = plugin.getServer().getPluginManager().getPlugin("HeadDatabase") != null;

        plugin.reloadConfig();
        if (!plugin.getConfig().isSet("currency.unit"))
            plugin.getConfig().set("currency.unit", "DIAMOND");
        if (!plugin.getConfig().isSet("currency.remove-currency-commands"))
            plugin.getConfig().set("currency.remove-currency-commands", new ArrayList<>());
        if (!plugin.getConfig().isSet("currency.prefix"))
            plugin.getConfig().set("currency.prefix", "");
        if (!plugin.getConfig().isSet("currency.suffix"))
            plugin.getConfig().set("currency.suffix", "");
        if (!plugin.getConfig().isSet("currency.decimals"))
            plugin.getConfig().set("currency.decimals", 0);
        if (!plugin.getConfig().isSet("prefix.enabled"))
            plugin.getConfig().set("prefix.enabled", false);
        if (!plugin.getConfig().isSet("prefix.overwrite-previous"))
            plugin.getConfig().set("prefix.overwrite-previous", false);
        if (!plugin.getConfig().isSet("prefix.format"))
            plugin.getConfig().set("prefix.format", "&7[{prefix}&7] &r");
        if (!plugin.getConfig().isSet("prefix.no-rank"))
            plugin.getConfig().set("prefix.no-rank", "&fUnranked");


        // loading rank info from the config
        ranks.clear();
        for (int i = 1; plugin.getConfig().getString(i + ".name") != null; i++) {
            //Bukkit.getLogger().info(i + "");
            ranks.add(new Rank(plugin.getConfig().getString(i + ".name"), plugin.getConfig().getStringList(i + ".lore"),  plugin.getConfig().getStringList(i + ".requirements"), plugin.getConfig().getInt(i + ".cost"), (List<String>) plugin.getConfig().getStringList(i + ".commands"), plugin.getConfig().getInt(i + ".hdb"),  plugin.getConfig().getInt("completed-hdb"), plugin.getConfig().getString(i +".item")));
        }

        currency = plugin.getConfig().getString("currency.unit");
        usingPlaceholderCurrency = Objects.requireNonNull(plugin.getConfig().getString("currency.unit")).contains("%");
        removeCommands = plugin.getConfig().getStringList("currency.remove-currency-commands");
        currencyPrefix = plugin.getConfig().getString("currency.prefix");
        currencySuffix = plugin.getConfig().getString("currency.suffix");
        decimals = plugin.getConfig().getInt("currency.decimals");
        addPrefix = plugin.getConfig().getBoolean("prefix.enabled");
        overwritePrefix = plugin.getConfig().getBoolean("prefix.overwrite-previous");
        prefixFormat = plugin.getConfig().getString("prefix.format");
        noRank = plugin.getConfig().getString("prefix.no-rank");

        if (!usingPlaceholderCurrency){
            try {
                Material.valueOf(currency);
            } catch (IllegalArgumentException ignored){
                Bukkit.getLogger().warning("[NotRanks] Material for currency is not valid! defaulting to DIAMOND.");
                currency = "DIAMOND";
            }
        }

        plugin.saveConfig();
    }

}
