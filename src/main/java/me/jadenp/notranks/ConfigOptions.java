package me.jadenp.notranks;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static me.jadenp.notranks.LanguageOptions.color;
import static me.jadenp.notranks.LanguageOptions.guiName;

public class ConfigOptions {
    public static ArrayList<Rank> ranks = new ArrayList<>();
    public static boolean HDBEnabled;
    public static String currency;
    public static int maxPages;
    public static boolean usingPlaceholderCurrency;
    public static List<String> removeCommands;
    public static String currencyPrefix;
    public static String currencySuffix;
    public static int decimals;
    public static boolean addPrefix;
    public static boolean overwritePrefix;
    public static String prefixFormat;
    public static String noRank;
    public static boolean autoSize;
    public static ItemStack fillItem;
    public static boolean replacePageItems;
    public static int guiSize;
    public static ItemStack exit = new ItemStack(Material.BARRIER);
    public static ItemStack next = new ItemStack(Material.SPECTRAL_ARROW);
    public static ItemStack back = new ItemStack(Material.TIPPED_ARROW);
    public static List<GUItem> customGUI = new ArrayList<>();
    public static GUItem[] guiLayout;
    public static int ranksPerPage;
    public static boolean usingHeads;
    public static int numberFormatting;
    public static String nfThousands;
    public static int nfDecimals;
    public static LinkedHashMap<Long, String> nfDivisions = new LinkedHashMap<>();
    public static String completionBefore;
    public static String completionPrefix;
    public static String completionSuffix;
    public static String completionAfter;
    public static String denyClickItem;
    public static boolean debug = false;

    public static void loadConfig(){
        // close everyone out of gui
        for (Player player : Bukkit.getOnlinePlayers()){
            if (player.getOpenInventory().getTitle().equals(color(guiName))){
                player.closeInventory();
            }
        }
        NotRanks.getInstance().guiPage.clear();

        ItemMeta meta = exit.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Exit");
        exit.setItemMeta(meta);

        meta = next.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.DARK_GRAY + "Next Page");
        next.setItemMeta(meta);

        meta = back.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.DARK_GRAY + "Last Page");
        back.setItemMeta(meta);

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
        if (!plugin.getConfig().isSet("gui.auto-size"))
            plugin.getConfig().set("gui.auto-size", true);
        if (!plugin.getConfig().isSet("gui.fill-item"))
            plugin.getConfig().set("gui.fill-item", "GRAY_STAINED_GLASS_PANE");
        if (!plugin.getConfig().isSet("gui.replace-page-items"))
            plugin.getConfig().set("gui.replace-page-items", true);
        if (!plugin.getConfig().isSet("gui.size"))
            plugin.getConfig().set("gui.size", 27);
        if (!plugin.getConfig().isSet("gui.deny-click-tem"))
            plugin.getConfig().set("gui.deny-click-item", "VOID_BARRIER");

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

        // loading rank info from the config
        ranks.clear();
        for (int i = 1; plugin.getConfig().getString(i + ".name") != null; i++) {
            List<String> lore = plugin.getConfig().isSet(i + ".lore") ? plugin.getConfig().getStringList(i + ".lore") : new ArrayList<>();
            List<String> requirements = plugin.getConfig().isSet(i + ".requirements") ? plugin.getConfig().getStringList(i + ".requirements") : new ArrayList<>();
            int cost = plugin.getConfig().isSet(i + ".cost") ? plugin.getConfig().getInt(i + ".cost") : 0;
            List<String> commands = plugin.getConfig().isSet(i + ".commands") ? plugin.getConfig().getStringList(i + ".commands") : new ArrayList<>();
            String head;
            if (plugin.getConfig().isSet(i + ".hdb")) {
                head = plugin.getConfig().getString(i + ".hdb");
                plugin.getConfig().set(i + ".head", head);
                plugin.getConfig().set(i + ".hdb", null);
            } else {
                head = plugin.getConfig().isSet(i + ".head") ? plugin.getConfig().getString(i + ".head") : "1";
            }

            String item = plugin.getConfig().isSet(i +".item") ? plugin.getConfig().getString(i +".item") : "EMERALD_BLOCK";
            boolean completionLoreEnabled = plugin.getConfig().isSet(i + ".completion-lore.enabled") && plugin.getConfig().getBoolean(i + ".completion-lore.enabled");
            List<String> completionLore = plugin.getConfig().isSet(i + ".completion-lore.lore") ? plugin.getConfig().getStringList(i + ".completion-lore.lore") : new ArrayList<>();
            boolean hideNBT = plugin.getConfig().isSet(i + ".hide-nbt") && plugin.getConfig().getBoolean(i + ".hide-nbt");
            ranks.add(new Rank(plugin.getConfig().getString(i + ".name"), lore, requirements, cost, commands, head, plugin.getConfig().getString("heads.completed"), item, completionLoreEnabled, completionLore, hideNBT));
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
        autoSize = plugin.getConfig().getBoolean("gui.auto-size");
        replacePageItems = plugin.getConfig().getBoolean("gui.replace-page-items");
        guiSize = plugin.getConfig().getInt("gui.size");
        usingHeads = plugin.getConfig().getBoolean("heads.enabled");
        numberFormatting = plugin.getConfig().getInt("number-formatting.type");
        nfThousands = plugin.getConfig().getString("number-formatting.thousands");
        nfDecimals = plugin.getConfig().getInt("number-formatting.divisions.decimals");
        completionBefore = plugin.getConfig().getString("requirement-completion.before");
        completionAfter = plugin.getConfig().getString("requirement-completion.after");
        completionPrefix = plugin.getConfig().getString("requirement-completion.prefix");
        completionSuffix = plugin.getConfig().getString("requirement-completion.suffix");
        denyClickItem = Objects.requireNonNull(plugin.getConfig().getString("gui.deny-click-item")).toUpperCase();
        if (!denyClickItem.equals("DISABLE") && !denyClickItem.equals("RANK"))
            try {
                Material.valueOf(denyClickItem);
            } catch (IllegalArgumentException e){
                Bukkit.getLogger().warning("Could not get a material from \"" + denyClickItem + "\" for deny click item.");
                denyClickItem = "DISABLE";
            }


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

        Material fillMaterial;
        String fill = plugin.getConfig().getString("gui.fill-item");
        try {
            assert fill != null;
            fillMaterial = Material.valueOf(fill.toUpperCase());
        } catch (IllegalArgumentException e){
            Bukkit.getLogger().warning("Fill item material: " + fill + " is not a valid material!");
            fillMaterial = Material.GRAY_STAINED_GLASS_PANE;
        }
        fillItem = new ItemStack(fillMaterial);
        meta = fillItem.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.BLACK + "");
        fillItem.setItemMeta(meta);

        customGUI.clear();

        int rankNum = 1;
        // get gui settings
        if (autoSize){
            // auto-size settings
            guiSize = ((ranks.size() / 7 + 1) * 9) + 18;
            if (guiSize > 54)
                guiSize = 54;
            int[] everything = new int[guiSize];
            for (int i = 0; i < everything.length; i++) {
                everything[i] = i;
            }
            customGUI.add(new GUItem(everything, new ArrayList<>(), fillItem));
            customGUI.add(new GUItem(new int[]{22}, new ArrayList<>(), exit));
            customGUI.add(new GUItem(new int[]{18}, new ArrayList<>(), back));
            customGUI.add(new GUItem(new int[]{26}, new ArrayList<>(), next));
            for (int i = 10; i < guiSize - 10; i++) {
                if (i % 9 != 0 && (i + 1) % 9 != 0){
                    customGUI.add(new GUItem(new int[]{i}, Collections.singletonList("rank " + rankNum), null));
                    rankNum++;
                }
            }
        } else {
            // custom settings
            for (int i = 1; plugin.getConfig().isSet("gui." + i + ".slot"); i++) {
                // get slots used
                int[] slots;
                String slot = plugin.getConfig().getString("gui." + i + ".slot");
                assert slot != null;
                try {
                    if (slot.contains("-")) {
                        int before = Integer.parseInt(slot.substring(0, slot.indexOf("-")));
                        int after = Integer.parseInt(slot.substring(slot.indexOf("-") + 1));
                        if (after <= before)
                            throw new RuntimeException();
                        slots = new int[after - before + 1];
                        for (int j = before; j < after + 1; j++) {
                            slots[j - before] = j;
                        }
                    } else {
                        slots = new int[]{Integer.parseInt(slot)};
                    }
                } catch (RuntimeException e) {
                    Bukkit.getLogger().warning("Invalid GUI slot (" + slot + ") for item: " + i);
                    slots = new int[0];
                }
                // get item
                ItemStack itemStack;
                List<String> actions = new ArrayList<>();
                if (!plugin.getConfig().isSet("gui." + i + ".item.material")) {
                    String item = plugin.getConfig().getString("gui." + i + ".item");
                    if (item == null)
                        continue;
                    // item is one of the preset items
                    switch (item.toLowerCase()) {
                        case "fill":
                            itemStack = fillItem;
                            break;
                        case "next":
                            itemStack = next;
                            break;
                        case "back":
                            itemStack = back;
                            break;
                        case "exit":
                            itemStack = exit;
                            break;
                        default:
                            if (item.startsWith("rank")) {
                                itemStack = null;
                                actions.add(item);
                            } else {
                                Bukkit.getLogger().warning("Unknown preset item (" + item + ") for item: " + i);
                                itemStack = new ItemStack(Material.STRUCTURE_VOID);
                                meta = itemStack.getItemMeta();
                                assert meta != null;
                                meta.setDisplayName(ChatColor.DARK_AQUA + "Config Error");
                                meta.setLore(new ArrayList<>(Arrays.asList(ChatColor.DARK_AQUA + "" + org.bukkit.ChatColor.ITALIC + "Unknown preset item: " + item, ChatColor.DARK_AQUA + "" + org.bukkit.ChatColor.ITALIC + "for item: " + i)));
                                itemStack.setItemMeta(meta);
                            }
                            break;
                    }
                } else {
                    // create new item
                    Material material;
                    String materialName = plugin.getConfig().getString("gui." + i + ".item.material");
                    try {
                        material = Material.valueOf(materialName);
                    } catch (IllegalArgumentException e) {
                        Bukkit.getLogger().warning("Unknown material (" + materialName + ") for item: " + i);
                        material = Material.STRUCTURE_VOID;
                    }
                    int amount = plugin.getConfig().isSet("gui." + i + ".item.amount") ? plugin.getConfig().getInt("gui." + i + ".item.amount") : 1;
                    itemStack = new ItemStack(material, amount);
                    boolean enchanted = plugin.getConfig().isSet("gui." + i + ".item.enchanted") && (plugin.getConfig().getBoolean("gui." + i + ".item.enchanted"));
                    if (enchanted)
                        itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
                    meta = itemStack.getItemMeta();
                    assert meta != null;
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    boolean hideNBT = plugin.getConfig().isSet("gui." + i + ".item.hide-nbt") && (plugin.getConfig().getBoolean("gui." + i + ".item.hide-nbt"));
                    if (hideNBT){
                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
                        meta.addItemFlags(ItemFlag.HIDE_DYE);
                        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
                        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
                    }
                    if (plugin.getConfig().isSet("gui." + i + ".item.name"))
                        meta.setDisplayName(color(plugin.getConfig().getString("gui." + i + ".item.name")));
                    if (plugin.getConfig().isSet("gui." + i + ".item.lore")) {
                        List<String> lore = plugin.getConfig().getStringList("gui." + i + ".item.lore");
                        lore.replaceAll(LanguageOptions::color);
                        meta.setLore(lore);
                    }
                    itemStack.setItemMeta(meta);
                }
                if (plugin.getConfig().isSet("gui." + i + ".actions"))
                    actions.addAll(plugin.getConfig().getStringList("gui." + i + ".actions"));
                customGUI.add(new GUItem(slots, actions, itemStack));
            }
        }
        ranksPerPage = 0;
        guiLayout = new GUItem[guiSize];
        for (GUItem guItem : customGUI){
            // add everything to the layout
            for (int i = 0; i < guItem.getSlot().length; i++){
                if (guiSize > guItem.getSlot()[i]){
                    if (guItem.getItem() == null) {
                        // rank items need to be formatted in order
                        ranksPerPage++;
                        if (guItem.getActions().get(0).length() == 4){
                            List<String> actions = new ArrayList<>(guItem.getActions());
                            actions.set(0, "rank " + ranksPerPage);
                            GUItem newItem = new GUItem(new int[]{guItem.getSlot()[i]}, actions, guItem.getItem());
                            guiLayout[guItem.getSlot()[i]] = newItem;
                            continue;
                        }
                    }
                    guiLayout[guItem.getSlot()[i]] = guItem;
                }
            }
        }
        if (ranksPerPage == 0){
            Bukkit.getLogger().warning("Did not find any ranks!");
            ranksPerPage = 1;
        } else {
            Bukkit.getLogger().info("Registered " + ranksPerPage + " ranks per page.");
        }
        maxPages = ranks.size() / ranksPerPage;
        if (ranks.size() % ranksPerPage > 0)
            maxPages++;

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

}
