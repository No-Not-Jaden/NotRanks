package me.jadenp.notranks;

import me.jadenp.notranks.gui.CustomItem;
import me.jadenp.notranks.gui.GUI;
import me.jadenp.notranks.gui.GUIOptions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigOptions {
    public static Map<String, List<Rank>> ranks = new HashMap<>();
    // <Player UUID, <Rank Type, Completed Rank Index (starting from 0)>>
    public static Map<UUID, Map<String, List<Integer>>> rankData = new HashMap<>();
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
    public static boolean usingHeads;
    public static int numberFormatting;
    public static String nfThousands;
    public static int nfDecimals;
    public static LinkedHashMap<Long, String> nfDivisions = new LinkedHashMap<>();
    public static String completionBefore;
    public static String completionPrefix;
    public static String completionSuffix;
    public static String completionAfter;
    public static boolean debug = false;
    public static File guiFile;
    public static File ranksFile;

    public static void loadConfig() throws IOException {
        // close everyone out of gui
        for (Player player : Bukkit.getOnlinePlayers()){
            if (GUI.playerPages.containsKey(player.getUniqueId())){
                player.closeInventory();
            }
        }
        GUI.playerPages.clear();


        NotRanks plugin = NotRanks.getInstance();

        guiFile = new File(plugin.getDataFolder() + File.separator + "gui.yml");
        ranksFile = new File(plugin.getDataFolder() + File.separator + "ranks.yml");

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
        if (plugin.getConfig().isSet("requirement-strikethrough")){
            if (plugin.getConfig().getBoolean("requirement-strikethrough")){
                plugin.getConfig().set("requirement-completion.before", "&a&m");
            } else {
                plugin.getConfig().set("requirement-completion.before", "&a");
            }
            plugin.getConfig().set("requirement-strikethrough", null);
        }
        if (!plugin.getConfig().isSet("requirement-completion.before"))
            plugin.getConfig().set("requirement-completion.before", "&a&m");
        if (!plugin.getConfig().isSet("requirement-completion.prefix"))
            plugin.getConfig().set("requirement-completion.prefix", "");
        if (!plugin.getConfig().isSet("requirement-completion.suffix"))
            plugin.getConfig().set("requirement-completion.suffix", "");
        if (!plugin.getConfig().isSet("requirement-completion.after"))
            plugin.getConfig().set("requirement-completion.after", "");
        if (plugin.getConfig().isSet("hdb.enabled")){
            plugin.getConfig().set("head.enabled", plugin.getConfig().getBoolean("hdb.enabled"));
            plugin.getConfig().set("head.completed", plugin.getConfig().getString("hdb.completed"));
            plugin.getConfig().set("hdb", null);
        }
        if (!plugin.getConfig().isSet("head.enabled"))
            plugin.getConfig().set("head.enabled", true);
        if (!plugin.getConfig().isSet("head.completed"))
            plugin.getConfig().set("head.completed", 6269);
        if (!plugin.getConfig().isSet("number-formatting.type"))
            plugin.getConfig().set("number-formatting.type", 1);
        if (!plugin.getConfig().isSet("number-formatting.thousands"))
            plugin.getConfig().set("number-formatting.thousands", ",");
        if (!plugin.getConfig().isSet("number-formatting.divisions.decimals")) {
            plugin.getConfig().set("number-formatting.divisions.decimals", 2);
            plugin.getConfig().set("number-formatting.divisions.1000", "K");
        }

        if (!guiFile.exists())
            plugin.saveResource("gui.yml", false);
        if (!ranksFile.exists())
            plugin.saveResource("ranks.yml", false);

        // migrate ranks
        YamlConfiguration ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
        for (int i = 1; plugin.getConfig().isConfigurationSection(i + ""); i++){
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
        String completedHead = plugin.getConfig().getString("head.completed");
        for (String key : ranksConfig.getKeys(false)){
            List<Rank> rankPath = new ArrayList<>();
            for (int i = 1; ranksConfig.isSet(key + "." + i); i++) {
                String name = ranksConfig.isSet(key + "." + i + ".name") ? ranksConfig.getString(key + "." + i + ".name") : "&6&lRank " + i;
                List<String> lore = ranksConfig.isSet(key + "." + i + ".lore") ? ranksConfig.getStringList(key + "." + i + ".lore") : new ArrayList<>();
                List<String> requirements = ranksConfig.isSet(key + "." + i + ".requirements") ? ranksConfig.getStringList(key + "." + i + ".requirements") : new ArrayList<>();
                int cost = ranksConfig.isSet(key + "." + i + ".cost") ? ranksConfig.getInt(key + "." + i + ".cost") : 0;
                List<String> commands = ranksConfig.isSet(key + "." + i + ".commands") ? ranksConfig.getStringList(key + "." + i + ".commands") : new ArrayList<>();
                String head = ranksConfig.isSet(key + "." + i + ".head") ? ranksConfig.getString(key + "." + i + ".head") : "1";
                String item = ranksConfig.isSet(key + "." + i + ".item") ? ranksConfig.getString(key + "." + i + ".item") : "EMERALD_BLOCK";
                boolean completionLoreEnabled = ranksConfig.isSet(key + "." + i + ".completion-lore.enabled") && ranksConfig.getBoolean(key + "." + i + ".completion-lore.enabled");
                List<String> completionLore = ranksConfig.isSet(key + "." + i + ".completion-lore.lore") ? ranksConfig.getStringList(key + "." + i + ".completion-lore.lore") : new ArrayList<>();
                boolean hideNBT = ranksConfig.isSet(key + "." + i + ".hide-nbt") && ranksConfig.getBoolean(key + "." + i + ".hide-nbt");

                rankPath.add(new Rank(name, lore, requirements, cost, commands, head, completedHead, item, completionLoreEnabled, completionLore, hideNBT));
            }
            ranks.put(key, rankPath);
        }

        // migrate old config to separated files
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        if (plugin.getConfig().isConfigurationSection("gui")){
            guiConfig.set("default.auto-size", plugin.getConfig().getBoolean("gui.auto-size"));
            guiConfig.set("default.remove-page-items", plugin.getConfig().getBoolean("gui.replace-page-items"));
            guiConfig.set("default.size", plugin.getConfig().getInt("gui.size"));
            guiConfig.set("default.deny-click-item", plugin.getConfig().getString("gui.deny-click-item"));
            guiConfig.set("custom-items.fill.material", plugin.getConfig().getString("gui.fill-item"));
            File language = new File(NotRanks.getInstance().getDataFolder() + File.separator + "language.yml");
            YamlConfiguration languageConfig = YamlConfiguration.loadConfiguration(language);
            if (languageConfig.isSet("gui-name")){
                guiConfig.set("default.gui-name", languageConfig.get("gui-name"));
                languageConfig.set("gui-name", null);
                languageConfig.save(language);
            }
            List<String> rankSlots = new ArrayList<>();
            for (int i = 1; plugin.getConfig().isSet("gui." + i + "slot"); i++) {
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
            guiConfig.save(guiFile);
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
                        itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
                        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                }
                if (guiConfig.getBoolean("custom-items." + key + ".hide-nbt")) {
                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    itemMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
                }
                itemStack.setItemMeta(itemMeta);

                List<String> itemCommands = guiConfig.isSet("custom-items." + key + ".commands") ? guiConfig.getStringList("custom-items." + key + ".commands") : new ArrayList<>();
                CustomItem customItem = new CustomItem(itemStack, itemCommands);
                customItems.put(key, customItem);
            }
            GUI.setCustomItems(customItems);
        }

        // read customGUIs
        GUI.clearGUIs();
        for (String key : guiConfig.getKeys(false)){
            GUIOptions guiOptions = new GUIOptions(Objects.requireNonNull(guiConfig.getConfigurationSection(key)));
            GUI.addGUI(guiOptions, key);
        }

        // read config
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
        usingHeads = plugin.getConfig().getBoolean("head.enabled");
        numberFormatting = plugin.getConfig().getInt("number-formatting.type");
        nfThousands = plugin.getConfig().getString("number-formatting.thousands");
        nfDecimals = plugin.getConfig().getInt("number-formatting.divisions.decimals");
        completionBefore = plugin.getConfig().getString("requirement-completion.before");
        completionAfter = plugin.getConfig().getString("requirement-completion.after");
        completionPrefix = plugin.getConfig().getString("requirement-completion.prefix");
        completionSuffix = plugin.getConfig().getString("requirement-completion.suffix");


        nfDivisions.clear();
        Map<Long, String> preDivisions = new HashMap<>();
        for (String s : Objects.requireNonNull(plugin.getConfig().getConfigurationSection("number-formatting.divisions")).getKeys(false)){
            if (s.equals("decimals"))
                continue;
            try {
                preDivisions.put(Long.parseLong(s), plugin.getConfig().getString("number-formatting.divisions." + s));
            } catch (NumberFormatException e){
                Bukkit.getLogger().warning("Division is not a number: " + s);
            }
        }
        nfDivisions = sortByValue(preDivisions);


        if (!usingPlaceholderCurrency) {
            try {
                Material.valueOf(currency);
            } catch (IllegalArgumentException ignored) {
                Bukkit.getLogger().warning("[NotRanks] Material for currency is not valid! defaulting to DIAMOND.");
                currency = "DIAMOND";
            }
        }

        plugin.saveConfig();
    }
    public static LinkedHashMap<Long, String> sortByValue(Map<Long, String> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<Long, String>> list =
                new LinkedList<>(hm.entrySet());

        // Sort the list
        list.sort((o1, o2) -> (o2.getKey()).compareTo(o1.getKey()));

        // put data from sorted list to hashmap
        LinkedHashMap<Long, String> temp = new LinkedHashMap<>();
        for (Map.Entry<Long, String> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static @Nullable Rank getRank(OfflinePlayer p, String rankType) {
        int rankNum = getRankNum(p, rankType);
        if (rankNum != 0)
            return getRank(rankNum, rankType);
        return null;
    }

    public static Rank getRank(int index, String rankType){
        return ranks.get(rankType).get(index);
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

    public static boolean isRankUnlocked(OfflinePlayer p, String rankType, int index){
        if (rankData.containsKey(p.getUniqueId()) && rankData.get(p.getUniqueId()).containsKey(rankType)) {
            List<Integer> completedRanks = rankData.get(p.getUniqueId()).get(rankType);
            return completedRanks.contains(index);
        }
        return false;
    }

    public static int getRankNum(OfflinePlayer p, String rankType){
        if (rankData.containsKey(p.getUniqueId()) && rankData.get(p.getUniqueId()).containsKey(rankType)) {
            List<Integer> completedRanks = rankData.get(p.getUniqueId()).get(rankType);
            if (completedRanks.isEmpty())
                return -1;
            return completedRanks.get(completedRanks.size()-1);
        }
        return -1;
    }
}
