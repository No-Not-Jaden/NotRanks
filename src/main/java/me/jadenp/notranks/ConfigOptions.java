package me.jadenp.notranks;

import me.jadenp.notranks.gui.GUI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.*;


public class ConfigOptions {

    public static final Map<String, List<String>> argumentAliases = new HashMap<>();
    public static boolean HDBEnabled;
    public static int decimals;
    public static boolean prefixEnabled;
    public static boolean prefixModifyChat;
    public static boolean overwritePrefix;
    public static String prefixFormat;
    public static String noRank;
    public static boolean usingHeads;
    public static String completionBefore;
    public static String completionPrefix;
    public static String completionSuffix;
    public static String completionAfter;
    public static boolean confirmation;
    public static boolean papiEnabled;
    private static boolean firstStart = true;
    private static final String[] modifiableSections = new String[]{"number-formatting.divisions"};

    public static void loadConfig(Plugin plugin) throws IOException {
        // close everyone out of gui
        for (Player player : Bukkit.getOnlinePlayers()){
            if (GUI.playerInfo.containsKey(player.getUniqueId())){
                player.closeInventory();
                NotRanks.debugMessage("Closed GUI for " + player.getName() + ".", false);
            }
        }

        NotRanks.log();
        LanguageOptions.loadConfig();
        RankManager.readConfig(plugin);
        GUI.readConfig(plugin);


        HDBEnabled = plugin.getServer().getPluginManager().isPluginEnabled("HeadDataBase");
        papiEnabled = plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");

        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        if (plugin.getConfig().isSet("currency.unit")){
            plugin.getConfig().set("currency.object", plugin.getConfig().getString("currency.unit"));
            plugin.getConfig().set("currency.remove-commands", plugin.getConfig().getStringList("currency.remove-currency-commands"));
            plugin.getConfig().set("currency.unit", null);
            plugin.getConfig().set("currency.remove-currency-commands", null);
        }
        if (plugin.getConfig().isSet("currency.decimals"))
            plugin.getConfig().set("currency.decimals", null);

        if (plugin.getConfig().isSet("requirement-strikethrough")){
            if (plugin.getConfig().getBoolean("requirement-strikethrough")){
                plugin.getConfig().set("requirement-completion.before", "&a&m");
            } else {
                plugin.getConfig().set("requirement-completion.before", "&a");
            }
            plugin.getConfig().set("requirement-strikethrough", null);
        }

        if (plugin.getConfig().isSet("hdb.enabled")){
            plugin.getConfig().set("head.enabled", plugin.getConfig().getBoolean("hdb.enabled"));
            plugin.getConfig().set("head.completed", plugin.getConfig().getString("hdb.completed"));
            plugin.getConfig().set("hdb", null);
        }

        if (plugin.getConfig().isInt("number-formatting.type")) {
            switch (plugin.getConfig().getInt("number-formatting.type")) {
                case 0:
                    plugin.getConfig().set("number-formatting.pattern", "#.##");
                    plugin.getConfig().set("number-formatting.use-divisions", true);
                    break;
                case 1:
                    plugin.getConfig().set("number-formatting.use-divisions", false);
                    break;
                case 2:
                    plugin.getConfig().set("number-formatting.use-divisions", true);
                    break;
            }
            plugin.getConfig().set("number-formatting.type", null);
            plugin.getConfig().set("number-formatting.thousands", null);
            plugin.getConfig().set("number-formatting.divisions.decimals", null);
            plugin.getConfig().set("number-formatting.decimal-symbol", null);
            plugin.getConfig().set("currency.decimals", null);
        }

        if (!plugin.getConfig().isSet("prefix.modify-chat") && plugin.getConfig().isSet("prefix.enabled")) {
            plugin.getConfig().set("prefix.modify-chat", plugin.getConfig().getBoolean("prefix.enabled"));
        }




        // fill in any default options that aren't present
        if (NotRanks.getInstance().getResource("config.yml") != null) {
            plugin.getConfig().setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotRanks.getInstance().getResource("config.yml")))));
            for (String key : Objects.requireNonNull(plugin.getConfig().getDefaults()).getKeys(true)) {
                if (Arrays.stream(modifiableSections).anyMatch(key::startsWith))
                    continue;
                if (!plugin.getConfig().isSet(key))
                    plugin.getConfig().set(key, plugin.getConfig().getDefaults().get(key));
            }
        }

        // read config
        decimals = plugin.getConfig().getInt("currency.decimals");
        prefixEnabled = plugin.getConfig().getBoolean("prefix.enabled");
        overwritePrefix = plugin.getConfig().getBoolean("prefix.overwrite-previous");
        prefixFormat = plugin.getConfig().getString("prefix.format");
        noRank = plugin.getConfig().getString("prefix.no-rank");
        completionBefore = plugin.getConfig().getString("requirement-completion.before");
        completionAfter = plugin.getConfig().getString("requirement-completion.after");
        completionPrefix = plugin.getConfig().getString("requirement-completion.prefix");
        completionSuffix = plugin.getConfig().getString("requirement-completion.suffix");
        confirmation = plugin.getConfig().getBoolean("confirmation");
        usingHeads = plugin.getConfig().getBoolean("head.enabled");
        prefixModifyChat = plugin.getConfig().getBoolean("prefix.modify-chat");

        NumberFormatting.setCurrencyOptions(Objects.requireNonNull(plugin.getConfig().getConfigurationSection("currency")), plugin.getConfig().getConfigurationSection("number-formatting"));

        plugin.saveConfig();

        // if the config is loading for the first time
        if (firstStart) {
            firstStart = false;
            // load some custom aliases to use
            List<String> rankAliases = plugin.getConfig().getStringList("command-aliases.notranks");
            List<String> rankupAliases = plugin.getConfig().getStringList("command-aliases.notrankup");
            List<String> rankInfoAliases = plugin.getConfig().getStringList("command-aliases.notrankinfo");
            setAliases("notranks", rankAliases);
            setAliases("notrankup", rankupAliases);
            setAliases("notrankinfo", rankInfoAliases);
        }
        // load some argument aliases
        argumentAliases.clear();
        for (String key : Objects.requireNonNull(plugin.getConfig().getConfigurationSection("command-aliases.arguments")).getKeys(false)) {
            if (plugin.getConfig().isList("command-aliases.arguments." + key)) {
                argumentAliases.put(key, plugin.getConfig().getStringList("command-aliases.arguments." + key));
            } else {
                argumentAliases.put(key, new ArrayList<>());
            }
        }

    }

    private static void setAliases(String commandName, List<String> aliases) {
        PluginCommand command = NotRanks.getInstance().getCommand(commandName);
        if (command == null) {
            Bukkit.getLogger().warning("[NotRanks] Unknown command name: " + commandName);
            return;
        }
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            for (String alias : aliases) {
                commandMap.register(alias, "notranks", command);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Bukkit.getLogger().warning("[NotRanks] Error adding command aliases");
            Bukkit.getLogger().warning(e.toString());
        }
    }




}
