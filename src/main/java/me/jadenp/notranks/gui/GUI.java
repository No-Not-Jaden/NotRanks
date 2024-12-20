package me.jadenp.notranks.gui;

import me.jadenp.notranks.*;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;

import static me.jadenp.notranks.ConfigOptions.*;
import static me.jadenp.notranks.LanguageOptions.*;

public class GUI implements Listener {

    public static final Map<UUID, PlayerInfo> playerInfo = new HashMap<>();
    public static final Map<UUID, Long> notifyThroughGUIDelay = new HashMap<>();
    private static final Map<String, GUIOptions> customGuis = new HashMap<>();
    public static Map<String, CustomItem> customItems = new HashMap<>();
    public static CustomItem exit;
    public static CustomItem next;
    public static CustomItem back;
    public static CustomItem fill;

    public static void addGUI(GUIOptions gui, String name){
        customGuis.put(name, gui);
    }

    public static void clearGUIs(){
        customGuis.clear();
    }

    public static GUIOptions getGUI(String type){
        return customGuis.get(type);
    }

    public static void setCustomItems(Map<String, CustomItem> customItems){
        GUI.customItems = customItems;

        // create some preset custom items for auto-size
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(net.md_5.bungee.api.ChatColor.RED + "" + net.md_5.bungee.api.ChatColor.BOLD + "Exit");
        item.setItemMeta(meta);
        exit = new CustomItem(item, Collections.singletonList("[close]"));

        item = new ItemStack(Material.SPECTRAL_ARROW);
        meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(net.md_5.bungee.api.ChatColor.DARK_GRAY + "Next Page");
        item.setItemMeta(meta);
        next = new CustomItem(item, Collections.singletonList("[next]"));

        item = new ItemStack(Material.TIPPED_ARROW);
        meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.DARK_GRAY + "Last Page");
        item.setItemMeta(meta);
        back = new CustomItem(item, Collections.singletonList("[back]"));

        item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.DARK_GRAY + "");
        item.setItemMeta(meta);
        fill = new CustomItem(item, new ArrayList<>());
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
                List<Rank> completedRanks = getAllCompletedRanks((OfflinePlayer) event.getWhoClicked());
                if (completedRanks.size() <= rankNum) {
                    event.getWhoClicked().sendMessage(parse(prefix + unknownRank, (Player) event.getWhoClicked()));
                    if (debug)
                        Bukkit.getLogger().info("[NotRanks] Completed ranks is smaller than rank number! " + completedRanks + "<=" + rankNum);
                    return;
                }
                Rank rank = completedRanks.get(rankNum);
                String path = getRankPath(rank);
                int rankIndex = getRankNum(rank);
                if (path.isEmpty() || rankIndex == -1) {
                    event.getWhoClicked().sendMessage(parse(prefix + unknownRank, (Player) event.getWhoClicked()));
                    if (debug)
                        Bukkit.getLogger().info("[NotRanks] Could not find rank path or number from rank " + path + ":" + rankIndex);
                    return;
                }
                prefixSelections.put(event.getWhoClicked().getUniqueId(), "r:" + rankIndex + "p:" + path);
                event.getWhoClicked().sendMessage(parse(prefix + prefixRank.replaceAll("\\{rank}", Matcher.quoteReplacement(rank.getName())), (OfflinePlayer) event.getWhoClicked()));
                event.getView().close();
                return;
            }

            Rank rank = ConfigOptions.getRank(rankNum, guiType);
            if (ConfigOptions.isRankUnlocked((OfflinePlayer) event.getWhoClicked(), guiType, rankNum) == Rank.CompletionStatus.COMPLETE) {
                // rank already unlocked
                gui.notifyThroughGUI(event, LanguageOptions.parse(LanguageOptions.alreadyCompleted, (Player) event.getWhoClicked()), true);
                return;
            }
            if (gui.isOrderlyProgression()){
                // check if it is the next rank
                if (ConfigOptions.getRankNum((Player) event.getWhoClicked(), guiType) != rankNum - 1){
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
                openGUI((Player) event.getWhoClicked(), "confirmation", 1, getRankFormat(rankNum, guiType));
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
