package me.jadenp.notranks.gui;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import me.jadenp.notranks.*;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;

import static me.jadenp.notranks.ConfigOptions.*;
import static me.jadenp.notranks.LanguageOptions.*;

public class GUI implements Listener {

    public static final Map<UUID, PlayerInfo> playerInfo = new HashMap<>();
    public static final Map<UUID, Long> notifyThroughGUIDelay = new HashMap<>();
    private static final Map<String, GUIOptions> customGuis = new HashMap<>();
    public static Map<String, CustomItem> customItems = new HashMap<>();
    private static CustomItem exit;
    private static CustomItem next;
    private static CustomItem back;
    private static CustomItem fill;

    public static void addGUI(GUIOptions gui, String name){
        customGuis.put(name, gui);
    }

    public static void clearGUIs(){
        customGuis.clear();
    }

    public static GUIOptions getGUI(String type){
        return customGuis.get(type);
    }

    private static void fillOldOptions(YamlConfiguration guiConfig, File guiFile) throws IOException {
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
        guiConfig.save(guiFile);
    }

    private static void loadGUI(ConfigurationSection guiConfig) {
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
                NotRanks.debugMessage("Read custom item: " + key + ".", false);
            }
            GUI.setCustomItems(customItems);
        }

        // read customGUIs
        GUI.clearGUIs();
        for (String key : guiConfig.getKeys(false)){
            if (key.equals("custom-items"))
                continue;
            GUIOptions guiOptions = new GUIOptions(Objects.requireNonNull(guiConfig.getConfigurationSection(key)));
            GUI.addGUI(guiOptions, key);
            NotRanks.debugMessage("[NotRanks] Registered GUI " + key + ".", false);
        }
    }

    public static void readConfig(Plugin plugin) throws IOException {
        playerInfo.clear();
        File guiFile = new File(plugin.getDataFolder() + File.separator + "gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        if (guiConfig.getKeys(true).size() <= 2) {
            Bukkit.getLogger().severe("[NotBounties] Loaded an empty configuration for the gui.yml file. Fix the YAML formatting errors, or the GUI may not work!\nFor more information on YAML formatting, see here: https://spacelift.io/blog/yaml");
            if (plugin.getResource("gui.yml") != null) {
                guiConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(plugin.getResource("gui.yml"))));
                loadGUI(guiConfig);
            }
        } else {
            fillOldOptions(guiConfig, guiFile);
            loadGUI(guiConfig);
        }
    }

    public static void setCustomItems(Map<String, CustomItem> customItems){

        // create some preset custom items for auto-size
        ItemStack item;
        ItemMeta meta;

        if (!customItems.containsKey("exit")) {
            item = new ItemStack(Material.BARRIER);
            meta = item.getItemMeta();
            assert meta != null;
            meta.setDisplayName(net.md_5.bungee.api.ChatColor.RED + "" + net.md_5.bungee.api.ChatColor.BOLD + "Exit");
            item.setItemMeta(meta);
            customItems.put("exit", new CustomItem(item, Collections.singletonList("[close]")));
        }

        if (!customItems.containsKey("next")) {
            item = new ItemStack(Material.SPECTRAL_ARROW);
            meta = item.getItemMeta();
            assert meta != null;
            meta.setDisplayName(net.md_5.bungee.api.ChatColor.DARK_GRAY + "Next Page");
            item.setItemMeta(meta);
            customItems.put("next", new CustomItem(item, Collections.singletonList("[next]")));
        }

        if (!customItems.containsKey("back")) {
            item = new ItemStack(Material.TIPPED_ARROW);
            meta = item.getItemMeta();
            assert meta != null;
            meta.setDisplayName(ChatColor.DARK_GRAY + "Last Page");
            item.setItemMeta(meta);
            customItems.put("back", new CustomItem(item, Collections.singletonList("[back]")));
        }

        if (!customItems.containsKey("fill")) {
            item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            meta = item.getItemMeta();
            assert meta != null;
            meta.setDisplayName(ChatColor.DARK_GRAY + "");
            item.setItemMeta(meta);
            customItems.put("fill", new CustomItem(item, new ArrayList<>()));
        }

        GUI.customItems = customItems;
    }


    public static void openGUI(Player player, String name, int page, String... displayRanks) {
        if (!customGuis.containsKey(name))
            return;
        GUIOptions gui = customGuis.get(name);
        if (page < 1)
            page = 1;

        player.openInventory(gui.createInventory(player, page, displayRanks));
        playerInfo.put(player.getUniqueId(), new PlayerInfo(page, name, displayRanks));
    }

    /**
     * Get a custom item defined in the config or a preset item
     * <p> Preset items include: exit, next, back, and fill</p>
     * @param key Name of the custom item path in the config
     * @return Custom item that is mapped to the key. If there is no custom item found, an empty item will be returned
     */
    public static CustomItem getCustomItem(String key){
        if (customItems.containsKey(key.toLowerCase())){
            return customItems.get(key.toLowerCase());
        }
        switch (key.toLowerCase()){
            case "exit":
                return exit;
            case "next":
                return next;
            case "back":
                return back;
            case "fill":
                return fill;
        }
        return new CustomItem(null, new ArrayList<>());
    }


    // is this called when server forces the inventory to be closed?
    @EventHandler
    public void onGUIClose(InventoryCloseEvent event){
        playerInfo.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Get a gui from the title
     * @param title Title of the GUI
     * @return the GUIOptions if the title matches a NotBounties GUI, or null if the title does not match any GUI
     */
    public static @Nullable GUIOptions getGUIByTitle(String title){
        GUIOptions gui = null;
        for (Map.Entry<String, GUIOptions> entry : customGuis.entrySet()){
            if (title.startsWith(entry.getValue().getName())){
                gui = entry.getValue();
                break;
            }
        }
        return gui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!playerInfo.containsKey(event.getWhoClicked().getUniqueId())) // check if they are in a GUI
            return;
        PlayerInfo info = playerInfo.get(event.getWhoClicked().getUniqueId());
        GUIOptions gui = getGUI(info.getGuiType());

        if (gui == null) // JIC a player has a page number, but they aren't in a gui
            return;
        String guiType = gui.getType();
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) // make sure it is in the top inventory
            return;
        if (event.getCurrentItem() == null)
            return;
        // check if it is a rank slot
        if (gui.getRankSlots().contains(event.getSlot()) && !gui.getType().equalsIgnoreCase("confirmation")){
            int rankNum = gui.getRankSlots().indexOf(event.getSlot()) + (info.getPage() - 1) * gui.getRankSlots().size();
            if (gui.getType().equalsIgnoreCase("choose-prefix")){
                List<Rank> completedRanks = RankManager.getAllCompletedPrefixRanks((OfflinePlayer) event.getWhoClicked());
                if (completedRanks.size() <= rankNum) {
                    event.getWhoClicked().sendMessage(parse(prefix + unknownRank, (Player) event.getWhoClicked()));
                    NotRanks.debugMessage("Completed ranks is smaller than rank number! " + completedRanks + "<=" + rankNum, false);
                    return;
                }
                Rank rank = completedRanks.get(rankNum);
                String path = RankManager.getRankPath(rank);
                int rankIndex = RankManager.getRankNum(rank);
                if (path.isEmpty() || rankIndex == -1) {
                    event.getWhoClicked().sendMessage(parse(prefix + unknownRank, (Player) event.getWhoClicked()));
                    NotRanks.debugMessage("Could not find rank path or number from rank " + path + ":" + rankIndex, false);
                    return;
                }
                RankManager.setPrefix(event.getWhoClicked().getUniqueId(), path, rankIndex);
                event.getWhoClicked().sendMessage(parse(prefix + prefixRank.replace("{rank}", rank.getName()), (OfflinePlayer) event.getWhoClicked()));
                event.getView().close();
                return;
            }

            Rank rank = RankManager.getRank(rankNum, guiType);
            if (RankManager.isRankUnlocked((OfflinePlayer) event.getWhoClicked(), guiType, rankNum) == Rank.CompletionStatus.COMPLETE) {
                // rank already unlocked
                gui.notifyThroughGUI(event, LanguageOptions.parse(LanguageOptions.alreadyCompleted, (Player) event.getWhoClicked()), true);
                return;
            }
            if (gui.isOrderlyProgression()){
                // check if it is the next rank
                if (RankManager.getRankNum((Player) event.getWhoClicked(), guiType) != rankNum - 1){
                    // not the next rank
                    gui.notifyThroughGUI(event, LanguageOptions.parse(LanguageOptions.notOnRank, (Player) event.getWhoClicked()), false);
                    return;
                }
            }
            // check for completion
            assert rank != null;
            if (rank.checkUncompleted((Player) event.getWhoClicked(), guiType)) {
                // incomplete
                gui.notifyThroughGUI(event, LanguageOptions.parse(LanguageOptions.rankUpDeny, (Player) event.getWhoClicked()), false);
                return;
            }
            if (confirmation){
                openGUI((Player) event.getWhoClicked(), "confirmation", 1, RankManager.getRankFormat(rankNum, guiType));
            } else {
                NotRanks.getInstance().rankup((Player) event.getWhoClicked(), gui.getType(), rankNum);
                event.getView().close();
            }
        } else {
            CustomItem customItem = gui.getCustomItems()[event.getSlot()];
            if (customItem == null)
                return;
            ActionCommands.execute((Player) event.getWhoClicked(), customItem.getCommands());
        }

    }

}
