package me.jadenp.notranks;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import me.jadenp.notranks.gui.CustomItem;
import me.jadenp.notranks.gui.GUI;
import me.jadenp.notranks.gui.GUIOptions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;


public class ConfigOptions {
    public static final Map<String, List<Rank>> ranks = new HashMap<>();
    // <Player UUID, <Rank Path, Completed Rank Index (starting from 0)>>
    public static final Map<UUID, Map<String, List<Integer>>> rankData = new HashMap<>();
    // p:PathName - prefix changes as player ranks up through the path
    // r:(rankNum) - shouldn't be used on its own, but will be default rank path
    // r:1p:default - rank 1 of default rank path - will not change when player ranks up
    // nothing - prefix changes with last rankup
    public static final Map<UUID, String> prefixSelections = new HashMap<>();
    public static final Map<String, Boolean> autoRankup = new HashMap<>();
    public static final Map<UUID, String> lastRankPathUsed = new HashMap<>();
    public static final Map<String, List<String>> argumentAliases = new HashMap<>();
    public static boolean HDBEnabled;
    public static int decimals;
    public static boolean addPrefix;
    public static boolean overwritePrefix;
    public static String prefixFormat;
    public static String noRank;
    public static boolean usingHeads;
    public static String completionBefore;
    public static String completionPrefix;
    public static String completionSuffix;
    public static String completionAfter;
    public static boolean debug = false;
    public static File guiFile;
    public static File ranksFile;
    public static boolean confirmation;
    public static boolean papiEnabled;
    private static boolean firstStart = true;

    public static void loadConfig() throws IOException {
        // close everyone out of gui
        for (Player player : Bukkit.getOnlinePlayers()){
            if (GUI.playerInfo.containsKey(player.getUniqueId())){
                player.closeInventory();
                if (debug)
                    Bukkit.getLogger().info("[NotRanks] Closed GUI for " + player.getName() + ".");
            }
        }
        GUI.playerInfo.clear();


        NotRanks plugin = NotRanks.getInstance();

        guiFile = new File(plugin.getDataFolder() + File.separator + "gui.yml");
        ranksFile = new File(plugin.getDataFolder() + File.separator + "ranks.yml");

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


        if (!guiFile.exists())
            plugin.saveResource("gui.yml", false);
        if (!ranksFile.exists())
            plugin.saveResource("ranks.yml", false);

        // migrate ranks
        YamlConfiguration ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
        for (int i = 1; plugin.getConfig().isConfigurationSection(i + ""); i++){
            if (debug)
                Bukkit.getLogger().info("[NotRanks] Migrating rank " + i + " to ranks.yml");
            ranksConfig.set("default." + i + ".name", plugin.getConfig().getString(i + ".name"));
            ranksConfig.set("default." + i + ".head", plugin.getConfig().getString(i + ".head"));
            ranksConfig.set("default." + i + ".item", plugin.getConfig().getString(i + ".item"));
            ranksConfig.set("default." + i + ".lore", plugin.getConfig().getStringList(i + ".lore"));
            ranksConfig.set("default." + i + ".completion-lore.enabled", plugin.getConfig().getBoolean(i + ".completion-lore.enabled"));
            ranksConfig.set("default." + i + ".completion-lore.lore", plugin.getConfig().getStringList(i + ".completion-lore.lore"));
            ranksConfig.set("default." + i + ".hide-nbt", plugin.getConfig().getBoolean(i + ".hide-nbt"));
            ranksConfig.set("default." + i + ".requirements", plugin.getConfig().getStringList(i + ".requirements"));
            ranksConfig.set("default." + i + ".cost", plugin.getConfig().getInt(i + ".cost"));
            ranksConfig.set("default." + i + ".commands", plugin.getConfig().getStringList(i + ".commands"));
            plugin.getConfig().set(i + "", null);
        }
        ranksConfig.save(ranksFile);

        // loading rank info from the config
        ranks.clear();
        autoRankup.clear();
        String completedHead = plugin.getConfig().getString("head.completed");
        for (String path : ranksConfig.getKeys(false)){
            List<Rank> rankPath = new ArrayList<>();
            for (String rankName : Objects.requireNonNull(ranksConfig.getConfigurationSection(path)).getKeys(false)) {
                if (ranksConfig.isConfigurationSection(path + "." + rankName)) {
                    // rank
                    rankPath.add(new Rank(ranksConfig.getConfigurationSection(path + "." + rankName), completedHead));
                    if (debug)
                        Bukkit.getLogger().info(() -> "[NotRanks] Registered rank: " + path + ":" + rankName);
                }
            }
            autoRankup.put(path, ranksConfig.getBoolean(path + ".auto-rankup"));
            ranks.put(path, rankPath);
        }

        // migrate gui
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        if (plugin.getConfig().isConfigurationSection("gui")){
            if (debug)
                Bukkit.getLogger().info("[NotRanks] Migrating gui to gui.yml");
            guiConfig.set("default.auto-size", plugin.getConfig().getBoolean("gui.auto-size"));
            guiConfig.set("default.remove-page-items", plugin.getConfig().getBoolean("gui.replace-page-items"));
            guiConfig.set("default.size", plugin.getConfig().getInt("gui.size"));
            if (plugin.getConfig().isSet("gui.deny-click-item"))
                guiConfig.set("default.deny-click-item", plugin.getConfig().getString("gui.deny-click-item"));
            else
                guiConfig.set("default.deny-click-item", "STRUCTURE_VOID");
            guiConfig.set("custom-items.fill.material", plugin.getConfig().getString("gui.fill-item"));
            File language = new File(NotRanks.getInstance().getDataFolder() + File.separator + "language.yml");
            YamlConfiguration languageConfig = YamlConfiguration.loadConfiguration(language);
            if (languageConfig.isSet("gui-name")){
                guiConfig.set("default.gui-name", languageConfig.get("gui-name"));
                languageConfig.set("gui-name", null);
                languageConfig.save(language);
            }
            List<String> rankSlots = new ArrayList<>();
            for (int i = 1; plugin.getConfig().isSet("gui." + i + ".slot"); i++) {
                if (plugin.getConfig().isSet("gui." + i + ".item.material")){
                    // create new custom item
                    int itemNum = 1;
                    while (guiConfig.isSet("custom-items.item" + itemNum))
                        itemNum++;
                    String generatedName = "item" + itemNum;
                    guiConfig.set("custom-items." + generatedName + ".material", plugin.getConfig().getString("gui." + i + ".item.material"));
                    guiConfig.set("custom-items." + generatedName + ".amount", plugin.getConfig().getInt("gui." + i + ".item.amount"));
                    guiConfig.set("custom-items." + generatedName + ".enchanted", plugin.getConfig().getBoolean("gui." + i + ".item.enchanted"));
                    guiConfig.set("custom-items." + generatedName + ".hide-nbt", plugin.getConfig().getBoolean("gui." + i + ".item.hide-nbt"));
                    guiConfig.set("custom-items." + generatedName + ".name", plugin.getConfig().getString("gui." + i + ".item.name"));
                    guiConfig.set("custom-items." + generatedName + ".lore", plugin.getConfig().getStringList("gui." + i + ".item.lore"));
                    // changing actions to fit with NotBounties commands
                    List<String> actions = plugin.getConfig().getStringList("gui." + i + ".actions");
                    for (int j = 0; j < actions.size(); j++) {
                        if (actions.get(j).startsWith("[command]")){
                            actions.set(j, actions.get(j).substring(10));
                        } else if (actions.get(j).startsWith("[gui]")){
                            actions.set(j, "[gui] default " + actions.get(j).substring(6));
                        }
                    }
                    guiConfig.set("custom-items." + generatedName + ".commands", actions);
                    guiConfig.set("default.layout." + i + ".slot", plugin.getConfig().getString("gui." + i + ".slot"));
                    guiConfig.set("default.layout." + i + ".item", generatedName);
                } else if (Objects.requireNonNull(plugin.getConfig().getString("gui." + i + ".item")).equalsIgnoreCase("rank")){
                    // move item to rank-slots
                    rankSlots.add(plugin.getConfig().getString("gui." + i + ".slot"));
                } else {
                    guiConfig.set("default.layout." + i + ".slot", plugin.getConfig().getString("gui." + i + ".slot"));
                    guiConfig.set("default.layout." + i + ".item", plugin.getConfig().getString("gui." + i + ".item"));
                }
            }
            guiConfig.set("default.rank-slots", rankSlots);

            plugin.getConfig().set("gui", null);
        }

        if (!guiConfig.isConfigurationSection("choose-prefix")){
            // add choose-prefix gui
            guiConfig.set("choose-prefix.gui-name", "&d&lChoose Prefix");
            guiConfig.set("choose-prefix.require-permission", false);
            guiConfig.set("choose-prefix.add-page", false);
            guiConfig.set("choose-prefix.auto-size", false);
            guiConfig.set("choose-prefix.remove-page-items", true);
            guiConfig.set("choose-prefix.deny-click-item", "DISABLE");
            guiConfig.set("choose-prefix.completed-deny-click-item", "DISABLE");
            guiConfig.set("choose-prefix.size", 27);
            guiConfig.set("choose-prefix.orderly-progression", false);
            guiConfig.set("choose-prefix.rank-slots", Collections.singletonList("0-17"));
            guiConfig.set("choose-prefix.layout.1.slot", "18-26");
            guiConfig.set("choose-prefix.layout.1.item", "fill");
            guiConfig.set("choose-prefix.layout.2.slot", "22");
            guiConfig.set("choose-prefix.layout.2.item", "exit");
            guiConfig.set("choose-prefix.layout.3.slot", "18");
            guiConfig.set("choose-prefix.layout.3.item", "back");
            guiConfig.set("choose-prefix.layout.4.slot", "26");
            guiConfig.set("choose-prefix.layout.4.item", "next");
            guiConfig.set("choose-prefix.layout.5.slot", "19");
            guiConfig.set("choose-prefix.layout.5.item", "default-prefix");
            guiConfig.set("choose-prefix.layout.6.slot", "25");
            guiConfig.set("choose-prefix.layout.6.item", "reset-prefix");

            if (!guiConfig.isConfigurationSection("custom-item.reset-prefix")){
                guiConfig.set("custom-items.default-prefix.material", "DIRT");
                guiConfig.set("custom-items.default-prefix.amount", 1);
                guiConfig.set("custom-items.default-prefix.name", "&#6b4616Default Path");
                guiConfig.set("custom-items.default-prefix.lore", Arrays.asList("", "&6&oClick to follow the", "&6&odefault rank path", ""));
                guiConfig.set("custom-items.default-prefix.commands", Arrays.asList("[p] rank prefix default", "[close]"));
                guiConfig.set("custom-items.reset-prefix.material", "WATER_BUCKET");
                guiConfig.set("custom-items.reset-prefix.amount", 1);
                guiConfig.set("custom-items.reset-prefix.name", "&fReset Prefix");
                guiConfig.set("custom-items.reset-prefix.lore", Arrays.asList("", "&7Your prefix will", "&7match your last rank", ""));
                guiConfig.set("custom-items.reset-prefix.commands", Arrays.asList("[p] rank prefix reset", "[close]"));
            }
        }

        // read custom items
        if (guiConfig.isConfigurationSection("custom-items")){
            Map<String, CustomItem> customItems = new HashMap<>();
            for (String key : Objects.requireNonNull(guiConfig.getConfigurationSection("custom-items")).getKeys(false)){
                Material material = Material.STONE;
                String mat = guiConfig.getString("custom-items." + key + ".material");
                if (mat != null)
                    try {
                        material = Material.valueOf(mat.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        Bukkit.getLogger().warning("Unknown material \"" + mat + "\" in " + guiConfig.getName());
                    }
                int amount = guiConfig.isInt("custom-items." + key + ".amount") ? guiConfig.getInt("custom-items." + key + ".amount") : 1;

                ItemStack itemStack = new ItemStack(material, amount);
                ItemMeta itemMeta = itemStack.getItemMeta();
                assert itemMeta != null;
                if (guiConfig.isSet("custom-items." + key + ".name")) {
                    itemMeta.setDisplayName(guiConfig.getString("custom-items." + key + ".name"));
                }
                if (guiConfig.isSet("custom-items." + key + ".custom-model-data")) {
                    itemMeta.setCustomModelData(guiConfig.getInt("custom-items." + key + ".custom-model-data"));
                }
                if (guiConfig.isSet("custom-items." + key + ".lore")) {
                    itemMeta.setLore(guiConfig.getStringList("custom-items." + key + ".lore"));
                }
                if (guiConfig.isSet("custom-items." + key + ".enchanted")) {
                    if (guiConfig.getBoolean("custom-items." + key + ".enchanted")) {
                        itemStack.addUnsafeEnchantment(Enchantment.CHANNELING, 1);
                        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                }
                if (guiConfig.getBoolean("custom-items." + key + ".hide-nbt")) {
                    itemMeta.getItemFlags().clear();
                    Multimap<Attribute, AttributeModifier> attributes = HashMultimap.create();
                    itemMeta.setAttributeModifiers(attributes);
                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    itemMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
                    itemMeta.addItemFlags(ItemFlag.HIDE_DYE);
                    itemMeta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
                    itemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                    itemMeta.addItemFlags(ItemFlag.values()[5]);
                }
                if (guiConfig.getBoolean("custom-items." + key + ".hide-tooltip") && NotRanks.isAboveVersion(20, 4)) {
                    itemMeta.setHideTooltip(true);
                }
                itemStack.setItemMeta(itemMeta);

                List<String> itemCommands = guiConfig.isSet("custom-items." + key + ".commands") ? guiConfig.getStringList("custom-items." + key + ".commands") : new ArrayList<>();
                CustomItem customItem = new CustomItem(itemStack, itemCommands);
                customItems.put(key, customItem);
                if (debug)
                    Bukkit.getLogger().info("[NotRanks] Read custom item: " + key + ".");
            }
            GUI.setCustomItems(customItems);
        }

        // read customGUIs
        GUI.clearGUIs();
        for (String key : guiConfig.getKeys(false)){
            if (key.equals("custom-items"))
                continue;
            // add newer features here
            if (!guiConfig.isSet(key + ".completed-deny-click-item"))
                guiConfig.set(key + ".completed-deny-click-item", guiConfig.getString(key + ".deny-click-item"));
            if (!guiConfig.isSet(key + ".require-permission"))
                guiConfig.set(key + ".require-permission", false);
            if (!ranks.containsKey(key) && !key.equals("confirmation") && !key.equalsIgnoreCase("choose-prefix")){
                Bukkit.getLogger().warning("[NotRanks] Found a GUI for " + key + ", but did not find a rank path to match it.");
            }
            GUIOptions guiOptions = new GUIOptions(Objects.requireNonNull(guiConfig.getConfigurationSection(key)));
            GUI.addGUI(guiOptions, key);
            if (debug)
                Bukkit.getLogger().info("[NotRanks] Registered GUI " + key + ".");
        }
        guiConfig.save(guiFile);

        // fill in any missing default settings
        for (String key : Objects.requireNonNull(plugin.getConfig().getDefaults()).getKeys(true)) {
            // Bukkit.getLogger().info("[key] " + key);
            if (!plugin.getConfig().isSet(key)) {
                //Bukkit.getLogger().info("Not set -> " + config.getDefaults().get(key));
                plugin.getConfig().set(key, plugin.getConfig().getDefaults().get(key));
            }
        }

        // read config
        decimals = plugin.getConfig().getInt("currency.decimals");
        addPrefix = plugin.getConfig().getBoolean("prefix.enabled");
        overwritePrefix = plugin.getConfig().getBoolean("prefix.overwrite-previous");
        prefixFormat = plugin.getConfig().getString("prefix.format");
        noRank = plugin.getConfig().getString("prefix.no-rank");
        completionBefore = plugin.getConfig().getString("requirement-completion.before");
        completionAfter = plugin.getConfig().getString("requirement-completion.after");
        completionPrefix = plugin.getConfig().getString("requirement-completion.prefix");
        completionSuffix = plugin.getConfig().getString("requirement-completion.suffix");
        confirmation = plugin.getConfig().getBoolean("confirmation");
        usingHeads = plugin.getConfig().getBoolean("head.enabled");

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


    public static @Nullable Rank getRank(OfflinePlayer p, String rankType) {
        int rankNum = getRankNum(p, rankType);
        if (rankNum != -1)
            return getRank(rankNum, rankType);
        return null;
    }

    public static @Nullable Rank getRank(int index, String rankType){
        if (ranks.isEmpty()) {
            Bukkit.getLogger().warning("[NotRanks] No ranks found! Is ranks.yml formatted correctly?");
            return null;
        }
        List<Rank> ranksList = ranks.get(rankType);
        if (ranksList == null) {
            if (debug)
                Bukkit.getLogger().info("[NotRanks] " + rankType + " does not exist!");
            return null;
        }
        if (index >= ranksList.size()){
            if (debug)
                Bukkit.getLogger().info("[NotRanks] Rank " + index + " of " + rankType + " does not exist! There are not that many ranks.");
            return null;
        }
        return ranksList.get(index);
    }

    /**
     * Get a rank from rank format
     * @Example
     * <p>p:PathName - last rank player had in path</p>
     * <p>r:(rankNum) - rank in default path</p>
     * <p>r:1p:default - rank 1 of default rank path</p>
     * <p>nothing - last rank</p>
     * @param rankFormat String in rank format
     * @param player Player that the request is for
     * @return Requested Rank or null if the rank format was incorrect or no rank existed
     */
    public static @Nullable Rank getRank(String rankFormat, OfflinePlayer player){
        if (rankFormat.isEmpty())
            return getRank(player, getLastRankPath(player));
        if (rankFormat.startsWith("p:")) {
            String path = rankFormat.substring(2);
            return getRank(player, path);
        }
        return getRank(rankFormat);
    }
    /**
     * Get a rank from rank format
     * @Example
     * <p>r:(rankNum) - rank in default path</p>
     * <p>r:1p:default - rank 1 of default rank path</p>
     * @param rankFormat String in rank format
     * @return Requested Rank or null if the rank format was incorrect or no rank existed
     */
    public static @Nullable Rank getRank(String rankFormat){
        boolean hasPath = rankFormat.contains("p");
        try {
            String path = hasPath ? rankFormat.substring(rankFormat.indexOf("p") + 2) : "default";
            String rank = hasPath ? rankFormat.substring(2, rankFormat.indexOf("p")) : rankFormat.substring(2);
            return getRank(Integer.parseInt(rank), path);
        } catch (IndexOutOfBoundsException | NumberFormatException e){
            // incorrect format
            return null;
        }
    }


    public static String getRankFormat(int rankNum, String path) {
        return "r:" + rankNum + "p:" + path;
    }

    /**
     * Get the prefix rank the player is using
     * @param player Player to get prefix rank of
     * @return Rank that the player wants to be their prefix or null for no rank
     */
    public static @Nullable Rank getPrefixRank(OfflinePlayer player){
        if (prefixSelections.containsKey(player.getUniqueId()))
            return getRank(prefixSelections.get(player.getUniqueId()), player);
        return getRank(player, getLastRankPath(player)); // get last rank
    }

    public static String getLastRankPath(OfflinePlayer player) {
        if (lastRankPathUsed.containsKey(player.getUniqueId()))
            return lastRankPathUsed.get(player.getUniqueId());
        return "default";
    }

    public static void addRank(OfflinePlayer p, String rankType, int index){
        Map<String, List<Integer>> playerRankInfo = rankData.containsKey(p.getUniqueId()) ? rankData.get(p.getUniqueId()) : new HashMap<>();
        List<Integer> completedRanks = playerRankInfo.containsKey(rankType) ? playerRankInfo.get(rankType) : new ArrayList<>();
        completedRanks.add(index);
        playerRankInfo.put(rankType, completedRanks);
        rankData.put(p.getUniqueId(), playerRankInfo);
    }

    public static List<Integer> getRankCompletion(OfflinePlayer p, String rankType){
        Map<String, List<Integer>> playerRankInfo = rankData.containsKey(p.getUniqueId()) ? rankData.get(p.getUniqueId()) : new HashMap<>();
        return playerRankInfo.containsKey(rankType) ? playerRankInfo.get(rankType) : new ArrayList<>();
    }

    public static void setRankCompletion(OfflinePlayer p, String rankType, List<Integer> completion){
        Map<String, List<Integer>> playerRankInfo = rankData.containsKey(p.getUniqueId()) ? rankData.get(p.getUniqueId()) : new HashMap<>();
        playerRankInfo.put(rankType, completion);
        rankData.put(p.getUniqueId(), playerRankInfo);
    }

    public static Rank.CompletionStatus isRankUnlocked(OfflinePlayer p, String rankType, int index){
        if (rankData.containsKey(p.getUniqueId()) && rankData.get(p.getUniqueId()).containsKey(rankType)) {
            List<Integer> completedRanks = rankData.get(p.getUniqueId()).get(rankType);
            if (completedRanks.contains(index))
                return Rank.CompletionStatus.COMPLETE;
            if (completedRanks.contains(index-1))
                return Rank.CompletionStatus.INCOMPLETE;
        }
        GUIOptions gui = GUI.getGUI(rankType);
        if(gui != null && (!gui.isOrderlyProgression() || index == 0))
            return Rank.CompletionStatus.INCOMPLETE;
        return Rank.CompletionStatus.NO_ACCESS;
    }

    public static int getRankNum(OfflinePlayer p, String rankType){
        if (rankData.containsKey(p.getUniqueId()) && rankData.get(p.getUniqueId()).containsKey(rankType)) {
            List<Integer> completedRanks = rankData.get(p.getUniqueId()).get(rankType);
            if (completedRanks.isEmpty()) {
                return -1;
            }
            return completedRanks.get(completedRanks.size()-1);
        }
        return -1;
    }

    /**
     * Find rank number from rank object
     * @param rank Rank to find number of
     * @return Rank number or -1 if no rank was found
     */
    public static int getRankNum(Rank rank){
        for (Map.Entry<String, List<Rank>> entry : ranks.entrySet()) {
            for (Rank compareRank : entry.getValue()) {
                if (compareRank.equals(rank)){
                    return entry.getValue().indexOf(compareRank);
                }
            }
        }
        return -1;
    }

    public static RankInfo getRankInfo(String rankName){
        for (Map.Entry<String, List<Rank>> entry : ranks.entrySet()){
            for (Rank rank : entry.getValue()){
                if (rank.getName().equals(rankName)){
                    return new RankInfo(entry.getKey(), entry.getValue().indexOf(rank), rank);
                }
            }
        }
        return null;
    }

    public static List<Rank> getAllCompletedRanks(OfflinePlayer player){
        List<Rank> completed = new ArrayList<>();
        for (Map.Entry<String, List<Rank>> entry : ranks.entrySet()){
            for (int i = 0; i < entry.getValue().size(); i++) {
                if (isRankUnlocked(player, entry.getKey(), i) == Rank.CompletionStatus.COMPLETE){
                    completed.add(entry.getValue().get(i));
                }
            }
        }
        return completed;
    }

    /**
     * Get rank path from rank
     * @param rank to find path of
     * @return Rank path or an empty string if no rank matches
     */
    public static String getRankPath(Rank rank){
        for (Map.Entry<String, List<Rank>> entry : ranks.entrySet()) {
            for (Rank compareRank : entry.getValue()) {
                if (compareRank.equals(rank)){
                    return entry.getKey();
                }
            }
        }
        return "";
    }

}
