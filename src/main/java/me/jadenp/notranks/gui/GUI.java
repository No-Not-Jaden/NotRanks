package me.jadenp.notranks.gui;

import me.jadenp.notranks.ConfigOptions;
import me.jadenp.notranks.LanguageOptions;
import me.jadenp.notranks.NotRanks;
import me.jadenp.notranks.Rank;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GUI implements Listener {

    public static final Map<UUID, Integer> playerPages = new HashMap<>();
    public static Map<UUID, Long> notifyThroughGUIDelay = new HashMap<>();
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


    public static void openGUI(Player player, String name, int page) {
        if (!customGuis.containsKey(name))
            return;
        GUIOptions gui = customGuis.get(name);
        if (page < 1)
            page = 1;
        player.openInventory(gui.createInventory(player, page));
        playerPages.put(player.getUniqueId(), page);
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
        playerPages.remove(event.getPlayer().getUniqueId());
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
        if (!playerPages.containsKey(event.getWhoClicked().getUniqueId())) // check if they are in a GUI
            return;
        // find the gui - yeah, a linear search
        GUIOptions gui = getGUIByTitle(event.getView().getTitle());

        if (gui == null) // JIC a player has a page number, but they aren't in a gui
            return;
        String guiType = gui.getType();
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) // make sure it is in the top inventory
            return;
        if (event.getCurrentItem() == null)
            return;
        // check if it is a rank slot
        if (gui.getRankSlots().contains(event.getSlot())){
            int rankNum = gui.getRankSlots().indexOf(event.getSlot()) + (playerPages.get(event.getWhoClicked().getUniqueId()) - 1) * gui.getRankSlots().size();
            Rank rank = ConfigOptions.getRank(rankNum, gui.getType());
            if (ConfigOptions.isRankUnlocked((OfflinePlayer) event.getWhoClicked(), guiType, rankNum)) {
                // rank already unlocked
                gui.notifyThroughGUI(event, LanguageOptions.parse(LanguageOptions.notOnRank, (Player) event.getWhoClicked()));
                return;
            }
            if (gui.isOrderlyProgression()){
                // check if it is the next rank
                if (ConfigOptions.getRankNum((Player) event.getWhoClicked(), guiType) != rankNum - 1){
                    // not the next rank
                    gui.notifyThroughGUI(event, LanguageOptions.parse(LanguageOptions.notOnRank, (Player) event.getWhoClicked()));
                    return;
                }
            }
            // check for completion
            if (!rank.checkRequirements((Player) event.getWhoClicked(), guiType)) {
                // incomplete
                gui.notifyThroughGUI(event, LanguageOptions.parse(LanguageOptions.rankUpDeny, (Player) event.getWhoClicked()));
                return;
            }
            NotRanks.getInstance().rankup((Player) event.getWhoClicked(), gui.getType(), rankNum);
            event.getView().close();
        } else {
            CustomItem customItem = gui.getCustomItems()[event.getSlot()];
            if (customItem == null)
                return;
            for (String command : customItem.getCommands()){
                command = command.replaceAll("\\{player}", event.getWhoClicked().getName());
                while (command.contains("{slot") && command.substring(command.indexOf("{slot")).contains("}")){
                    String replacement = "";
                    try {
                        int slot = Integer.parseInt(command.substring(command.indexOf("{slot") + 5, command.substring(command.indexOf("{slot")).indexOf("}") + command.substring(0, command.indexOf("{slot")).length()));
                        ItemStack item = event.getInventory().getContents()[slot];
                        if (item != null) {
                            if (item.getType() == Material.PLAYER_HEAD) {
                                SkullMeta meta = (SkullMeta) item.getItemMeta();
                                assert meta != null;
                                OfflinePlayer player = meta.getOwningPlayer();
                                if (player != null && player.getName() != null) {
                                    replacement = meta.getOwningPlayer().getName();
                                } else {
                                    Bukkit.getLogger().warning("Invalid player for slot " + slot);
                                }
                            }
                            if (replacement == null)
                                replacement = "";
                            if (replacement.equals("")) {
                                ItemMeta meta = item.getItemMeta();
                                assert meta != null;
                                replacement = meta.getDisplayName();
                            }
                        }
                    } catch (NumberFormatException e){
                        Bukkit.getLogger().warning("Error getting slot in command: \n" + command);
                    }
                    command = command.substring(0, command.indexOf("{slot")) + replacement + command.substring(command.substring(command.indexOf("{slot")).indexOf("}") + command.substring(0, command.indexOf("{slot")).length()+ 1);
                }
                if (command.startsWith("@")){
                    String permission = command.substring(1, command.indexOf(" "));
                    if (!event.getWhoClicked().hasPermission(permission))
                        continue;
                    command = command.substring(command.indexOf(" ") + 1);
                } else if (command.startsWith("!@")){
                    String permission = command.substring(2, command.indexOf(" "));
                    if (event.getWhoClicked().hasPermission(permission))
                        continue;
                    command = command.substring(command.indexOf(" ") + 1);
                }
                if (command.startsWith("[close]")) {
                    event.getView().close();
                } else if (command.startsWith("[p]")) {
                    Bukkit.dispatchCommand(event.getWhoClicked(), command.substring(4));
                } else if (command.startsWith("[next]")) {
                    int amount = 1;
                    try {
                        amount = Integer.parseInt(command.substring(7));
                    } catch (IndexOutOfBoundsException | NumberFormatException ignored){}
                    openGUI((Player) event.getWhoClicked(), gui.getType(), playerPages.get(event.getWhoClicked().getUniqueId()) + amount);
                } else if (command.startsWith("[back]")) {
                    int amount = 1;
                    try {
                        amount = Integer.parseInt(command.substring(7));
                    } catch (IndexOutOfBoundsException | NumberFormatException ignored){}
                    openGUI((Player) event.getWhoClicked(), gui.getType(), playerPages.get(event.getWhoClicked().getUniqueId()) - amount);
                } else if (command.startsWith("[gui]")){
                    int amount = 1;
                    String guiName = command.substring(6);
                    if (command.substring(6).contains(" ")) {
                        try {
                            amount = Integer.parseInt(command.substring(6 + command.substring(6).indexOf(" ")));
                            guiName = guiName.substring(0, guiName.indexOf(" "));
                        } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
                        }
                    }
                    openGUI((Player) event.getWhoClicked(), guiName, amount);
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
        }

    }

}
