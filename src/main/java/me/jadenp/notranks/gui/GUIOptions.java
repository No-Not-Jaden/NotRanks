package me.jadenp.notranks.gui;

import me.jadenp.notranks.NotRanks;
import me.jadenp.notranks.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static me.jadenp.notranks.ConfigOptions.*;
import static me.jadenp.notranks.LanguageOptions.*;
import static me.jadenp.notranks.gui.GUI.*;

public class GUIOptions {
    private final int size;
    private final CustomItem[] customItems;
    private final String name;
    private final boolean removePageItems;
    private final boolean addPage;
    private final String type;
    private final List<CustomItem> pageReplacements = new ArrayList<>();
    private final String denyClickItem;
    private final List<Integer> rankSlots = new ArrayList<>();
    private final boolean orderlyProgression;
    private final String completedDenyClickItem;
    private final boolean requirePermission;

    public GUIOptions(ConfigurationSection settings){
        String denyClickItem1;
        type = settings.getName();
        name = color(settings.getString("gui-name"));
        boolean autoSize = settings.getBoolean("auto-size");
        addPage = settings.getBoolean("add-page");
        removePageItems = settings.getBoolean("remove-page-items");
        orderlyProgression = settings.getBoolean("orderly-progression");
        requirePermission = settings.getBoolean("require-permission");
        if (autoSize){
            int rankSize = ranks.get(type).size();
            int rows = rankSize / 7;
            if (rankSize % 7 > 0)
                rows++;
            if (rows > 4)
                rows = 4;
            size = rows * 9 + 18;
            // auto add rank slots
            if (rankSize % 7 > 0) {
                for (int i = 0; i < rows - 1; i++) {
                    for (int j = i * 9 + 10; j < i * 9 + 17; j++) {
                        rankSlots.add(j);
                    }
                }
                // adds the extra rank slots needed at the end
                for (int j = (rows - 1) * 9 + 10; j < (rows - 1) * 9 + 10 + rankSize % 7; j++) {
                    rankSlots.add(j);
                }
            } else {
                for (int i = 0; i < rows; i++) {
                    for (int j = i * 9 + 10; j < i * 9 + 17; j++) {
                        rankSlots.add(j);
                    }
                }
            }
            // setup auto-size format
            customItems = new CustomItem[size];
            Arrays.fill(customItems, GUI.getCustomItem("fill"));
            customItems[customItems.length-5] = exit;
            customItems[customItems.length-1] = next;
            customItems[customItems.length-9] = back;
            pageReplacements.add(GUI.getCustomItem("fill"));
            pageReplacements.add(GUI.getCustomItem("fill"));
        } else {
            size = settings.getInt("size");
            List<String> slotNames = settings.getStringList("rank-slots");
            for (String slot : slotNames) {
                int[] range = getRange(slot);
                for (int i : range) {
                    rankSlots.add(i);
                }
            }
            customItems = new CustomItem[size];
            for (String key : Objects.requireNonNull(settings.getConfigurationSection("layout")).getKeys(false)) {
                String item = settings.getString("layout." + key + ".item");
                int[] slots = getRange(settings.getString("layout." + key + ".slot"));
                //Bukkit.getLogger().info(item);
                if (GUI.customItems.containsKey(item)) {
                    CustomItem customItem = GUI.customItems.get(item);
                    for (int i : slots){
                        //Bukkit.getLogger().info(i + "");
                        if (customItems[i] != null){
                            if (getPageType(customItem.getCommands()) > 0){
                                pageReplacements.add(customItems[i]);
                            }
                        }
                        customItems[i] = customItem;
                    }
                } else {
                    // unknown item
                    Bukkit.getLogger().warning("Unknown item \"" + item + "\" in gui: " + settings.getName());
                }

            }
        }

        denyClickItem1 = Objects.requireNonNull(settings.getString("deny-click-item")).toUpperCase();
        if (!denyClickItem1.equals("DISABLE") && !denyClickItem1.equals("RANK"))
            try {
                Material.valueOf(denyClickItem1);
            } catch (IllegalArgumentException e){
                Bukkit.getLogger().warning("Could not get a material from \"" + denyClickItem1 + "\" for deny click item.");
                denyClickItem1 = "DISABLE";
            }
        denyClickItem = denyClickItem1;
        if (debug)
            Bukkit.getLogger().info("[NotRanks] Loaded deny click item: " + denyClickItem1 + " for " + type + " gui");
        denyClickItem1 = settings.isSet("completed-deny-click-item") ? Objects.requireNonNull(settings.getString("completed-deny-click-item")).toUpperCase() : denyClickItem;
        if (!denyClickItem1.equals("DISABLE") && !denyClickItem1.equals("RANK"))
            try {
                Material.valueOf(denyClickItem1);
            } catch (IllegalArgumentException e){
                Bukkit.getLogger().warning("Could not get a material from \"" + denyClickItem1 + "\" for completed deny click item.");
                denyClickItem1 = "DISABLE";
            }
        completedDenyClickItem = denyClickItem1;
        if (debug)
            Bukkit.getLogger().info("[NotRanks] Loaded completed deny click item: " + denyClickItem1 + " for " + type + " gui");

        if (rankSlots.isEmpty()){
            Bukkit.getLogger().warning("[NotRanks] No slots for ranks in the " + name + " GUI!");
        }
    }



    public CustomItem[] getCustomItems() {
        return customItems;
    }

    /**
     * Get the formatted custom inventory
     * @param player Player that owns the inventory and will be parsed for any placeholders
     * @param page Page of gui - This will change the items in player slots and page items if enabled
     * @return Custom GUI Inventory
     */
    public Inventory createInventory(Player player, int page, String... formattedRanks){
        if (page < 1) {
            page = 1;
        }
        Rank[] displayRanks = new Rank[formattedRanks.length];
        for (int i = 0; i < formattedRanks.length; i++) {
            displayRanks[i] = getRank(formattedRanks[i], player);
        }
        List<Rank> guiRanks = type.equalsIgnoreCase("choose-prefix") ? getAllCompletedRanks(player) : getRanks();
        int ranksSize = guiRanks.size();
        while ((page-1) * rankSlots.size() >= ranksSize && page != 1){
            page--;
            playerInfo.put(player.getUniqueId(), new PlayerInfo(page, type, formattedRanks));
        }
        String name = addPage ? this.name + " " + page : this.name;
        name = parse(name, player);
        Inventory inventory = Bukkit.createInventory(player, size, name);
        ItemStack[] contents = inventory.getContents();
        // set up regular items
        int replacementIndex = 0;
        for (int i = 0; i < contents.length; i++) {
            if (customItems[i] == null)
                continue;
            // check if item is a page item
            if (removePageItems){
                // next
                if (getPageType(customItems[i].getCommands()) == 1 && page * rankSlots.size() >= ranksSize){
                    contents[i] = pageReplacements.get(replacementIndex).getFormattedItem(player);
                    replacementIndex++;
                    continue;
                }
                // back
                if (getPageType(customItems[i].getCommands()) == 2 && page == 1){
                    contents[i] = pageReplacements.get(replacementIndex).getFormattedItem(player);
                    replacementIndex++;
                    continue;
                }
            }
            contents[i] = customItems[i].getFormattedItem(player);
        }
        // cases for non-rank GUIs
        if (type.equalsIgnoreCase("confirmation")){
            for (Integer rankSlot : rankSlots) {
                assert displayRanks[0] != null;
                contents[rankSlot] = displayRanks[0].getItem(player, Rank.CompletionStatus.INCOMPLETE);
            }
        }
        // set up player slots - i = index of rank in rank list
        Rank prefixRank = getPrefixRank(player);
        for (int i = (page-1) * rankSlots.size(); i < Math.min(page * rankSlots.size(), ranksSize); i++){
            Rank rank = guiRanks.get(i);
            ItemStack item = type.equalsIgnoreCase("choose-prefix") ? rank.getPrefixItem(rank.equals(prefixRank), player) : rank.getItem(player, isRankUnlocked(player, type, i));
            contents[rankSlots.get(i % rankSlots.size())] = item;
        }
        inventory.setContents(contents);
        return inventory;
    }

    public List<Integer> getRankSlots() {
        return rankSlots;
    }

    public List<Rank> getRanks(){
        if (type.equalsIgnoreCase("confirmation"))
            return new ArrayList<>();
        return ranks.get(type);
    }

    public boolean isPermissionRequired(){
        return requirePermission;
    }

    public boolean isOrderlyProgression() {
        return orderlyProgression;
    }

    public String getName() {
        return name;
    }

    /**
     * Get an array of desired numbers from a string from (x)-(y). Both x and y are inclusive.
     * <p>"1" -> [1]</p>
     * <p>"3-6" -> [3, 4, 5, 6]</p>
     * @param str String to parse
     * @return desired range of numbers sorted numerically or an empty list if there is a formatting error
     */
    private static int[] getRange(String str){
        try {
            // check if it is a single number
            int i = Integer.parseInt(str);
            // return if an exception was not thrown
            return new int[]{i};
        } catch (NumberFormatException e){
            // there is a dash we need to get out
        }
        String[] split = str.split("-");
        try {
            int bound1 = Integer.parseInt(split[0]);
            int bound2 = Integer.parseInt(split[1]);
            int[] result = new int[Math.abs(bound1 - bound2) + 1];

            for (int i = Math.min(bound1, bound2); i < Math.max(bound1, bound2) + 1; i++) {
                //Bukkit.getLogger().info(i + "");
                result[i-Math.min(bound1, bound2)] = i;
            }
            return result;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e){
            // formatting error
            Bukkit.getLogger().warning("[NotRanks] Formatting error for rank-slots for: " + str);
            return new int[0];
        }
    }

    /**
     * Parses through commands for "[next]" and "[back]"
     * @param commands Commands of the CustomItem
     * @return 1 for next, 2 for back, 0 for no page item
     */
    public static int getPageType(List<String> commands){
        for (String command : commands){
            if (command.startsWith("[next]"))
                return 1;
            if (command.startsWith("[back]"))
                return 2;
        }
        return 0;
    }

    public String getType() {
        return type;
    }

    public void notifyThroughGUI(InventoryClickEvent event, String message, boolean completed){
        // check the delay time
        if (notifyThroughGUIDelay.containsKey(event.getWhoClicked().getUniqueId()) && notifyThroughGUIDelay.get(event.getWhoClicked().getUniqueId()) > System.currentTimeMillis())
            return;
        notifyThroughGUIDelay.put(event.getWhoClicked().getUniqueId(), System.currentTimeMillis() + 1000);

        String notifyItem = completed ? completedDenyClickItem : denyClickItem;
        if (notifyItem.equals("DISABLE")) {
            event.getWhoClicked().sendMessage(prefix + message);
        } else if (notifyItem.equals("RANK")){
            ItemStack[] contents = event.getInventory().getContents();
            ItemStack item = contents[event.getSlot()];
            ItemMeta meta = item.getItemMeta();
            assert meta != null;
            meta.setDisplayName(message);
            item.setItemMeta(meta);
            contents[event.getSlot()] = item;
            event.getInventory().setContents(contents);
            new BukkitRunnable(){
                @Override
                public void run() {
                    if (playerInfo.containsKey(event.getWhoClicked().getUniqueId())){
                        openGUI((Player) event.getWhoClicked(), type, playerInfo.get(event.getWhoClicked().getUniqueId()).getPage());
                    }
                }
            }.runTaskLater(NotRanks.getInstance(), 20);
        } else {
            ItemStack[] contents = event.getInventory().getContents();
            ItemStack item = new ItemStack(Material.valueOf(notifyItem));
            ItemMeta meta = item.getItemMeta();
            assert meta != null;
            meta.setDisplayName(message);
            item.setItemMeta(meta);
            contents[event.getSlot()] = item;
            event.getInventory().setContents(contents);new BukkitRunnable(){
                @Override
                public void run() {
                    if (playerInfo.containsKey(event.getWhoClicked().getUniqueId())){
                        openGUI((Player) event.getWhoClicked(), type, playerInfo.get(event.getWhoClicked().getUniqueId()).getPage());
                    }
                }
            }.runTaskLater(NotRanks.getInstance(), 20);
        }
    }
}

