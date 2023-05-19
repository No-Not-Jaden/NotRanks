package me.jadenp.notranks;


import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

import static me.jadenp.notranks.ConfigOptions.*;
import static me.jadenp.notranks.LanguageOptions.*;

/**
 * Wrong rank item -
 */
public final class NotRanks extends JavaPlugin implements CommandExecutor, Listener {

    public File playerdata = new File(this.getDataFolder() + File.separator + "playerdata.yml");

    public File logsFolder = new File(this.getDataFolder() + File.separator + "logs");
    Date now = new Date();
    SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
    SimpleDateFormat formatExact = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    File today = new File(logsFolder + File.separator + format.format(now) + ".txt");
    public ArrayList<String> logs = new ArrayList<>();
    public HashMap<String, Integer> playerRank = new HashMap<>();

    public static NotRanks instance;
    public HashMap<UUID, Integer> guiPage = new HashMap<>();


    public static NotRanks getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        Bukkit.getLogger().info(ChatColor.BLUE + "Running NotRanks version " + getDescription().getVersion() + ".");
        Objects.requireNonNull(getCommand("ranks")).setExecutor(this);
        Objects.requireNonNull(getCommand("rankup")).setExecutor(this);
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        new RankPlaceholder(this).register();
        saveDefaultConfig();
        // create logs stuff
        //noinspection ResultOfMethodCallIgnored
        logsFolder.mkdir();
        try {
            if (!today.createNewFile()){
                try {
                    Scanner scanner = new Scanner(today);
                    while (scanner.hasNextLine()){
                        String data = scanner.nextLine();
                        logs.add(data);
                    }
                    scanner.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // creating file to store player's ranks if the file hadn't already been created
            try {
                if (playerdata.createNewFile()){
                    Bukkit.getLogger().info("Creating a new player data file.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }




        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(playerdata);
        for (int i = 1; configuration.getString(i + ".uuid") != null; i++) {
            if (playerRank.containsKey(configuration.getString(i + ".uuid"))) {
                playerRank.replace(configuration.getString(i + ".uuid"), configuration.getInt(i + ".rank"));
            } else {
                playerRank.put(configuration.getString(i + ".uuid"), configuration.getInt(i + ".rank"));
            }
        }
        try {
            this.loadConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // auto save every 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    saveRanks();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskTimer(this, 6000L, 6000L);

        // check if they completed a rank requirement
        new BukkitRunnable(){
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()){
                    if (ranks.size() > playerRank.get(p.getUniqueId().toString())) {
                        Rank rank = ranks.get(playerRank.get(p.getUniqueId().toString()));
                        rank.checkRankCompletion(p);
                    }
                }
            }
        }.runTaskTimer(this, 500L, 60);
        new BukkitRunnable(){
            @Override
            public void run() {
                log();
            }
        }.runTaskTimerAsynchronously(this,5000,5000);
        logs.add("[" +formatExact.format(now) + "] Plugin Loaded!");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        logs.add("[" +formatExact.format(now) + "] Plugin disabling.");
        try {
            saveRanks();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log();
    }

    public int getRank(Player p) {
        if (playerRank.containsKey(p.getUniqueId().toString()))
            return playerRank.get(p.getUniqueId().toString());
        return 0;
    }

    public int getRank(OfflinePlayer p) {
        if (playerRank.containsKey(p.getUniqueId().toString()))
            return playerRank.get(p.getUniqueId().toString());
        return 0;
    }

    public void rankup(Player p, Rank newRank) {
        RankupEvent event = new RankupEvent(p, newRank, ranks.get(playerRank.get(p.getUniqueId().toString())), playerRank.get(p.getUniqueId().toString()) + 1);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()){
            if (rankUp.length() > 0) {
                String text = rankUp;
                text = text.replaceAll("\\{player}", p.getName());
                text = text.replaceAll("\\{rank}", ranks.get(playerRank.get(p.getUniqueId().toString())).getName());

                Bukkit.broadcastMessage(prefix + parse(text, p));
            }
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
            newRank.rankup(p);
            logs.add("[" +formatExact.format(now) + "] " + p.getName() + " ranked up to " + newRank.getName() + ".");
            playerRank.replace(p.getUniqueId().toString(), playerRank.get(p.getUniqueId().toString()) + 1);
        }
    }


    public void saveRanks() throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        int i = 1;
        for (Map.Entry<String, Integer> stringIntegerEntry : playerRank.entrySet()) {
            configuration.set(i + ".uuid", stringIntegerEntry.getKey());
            configuration.set(i + ".rank", stringIntegerEntry.getValue());
            i++;
        }
        configuration.save(playerdata);
    }

    public void loadConfig() throws IOException {
        log();
        LanguageOptions.loadConfig();
        ConfigOptions.loadConfig();
    }

    public void openGUI(Player p, int page) {
        if (page < 1)
            page = 1;
        if (page > maxPages)
            page = maxPages;
        Inventory inv = Bukkit.createInventory(p, guiSize, color(guiName));
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < guiSize; i++) {
            GUItem guItem = guiLayout[i];
            if (guItem == null)
                continue;
            if (guItem.getItem() == null){
                // rank item
                ItemStack item;
                try {
                    int rankNum = Integer.parseInt(guItem.getActions().get(0).substring(5)) - 1 + ((page - 1) * ranksPerPage);
                    item = ranks.get(rankNum).getItem(p, (playerRank.get(p.getUniqueId().toString()) > rankNum));
                } catch (NumberFormatException | IndexOutOfBoundsException e){
                    item = fillItem;
                }
                contents[i] = item;
            } else if ((guItem.getItem().isSimilar(next) || guItem.getActions().contains("[next]")) && replacePageItems) {
                if (ranks.size() > ranksPerPage * page){
                    // there are more ranks on the next page
                    contents[i] = next;
                } else {
                    contents[i] = fillItem;
                }
            } else if ((guItem.getItem().isSimilar(back) || guItem.getActions().contains("[back]")) && replacePageItems) {
                if (page > 1){
                    // there are more ranks on the previous
                    contents[i] = back;
                } else {
                    contents[i] = fillItem;
                }
            } else {
                contents[i] = guItem.getPapiItem(p);
            }
        }
        inv.setContents(contents);
        guiPage.put(p.getUniqueId(), page);
        p.openInventory(inv);
    }




    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("rankup")) {
            if (sender.hasPermission("notranks.default")) {
                if (args.length == 0) {
                    if (!(playerRank.get(((Player) sender).getUniqueId().toString()) > ranks.size() - 1)) {
                        if (ranks.get(playerRank.get(((Player) sender).getUniqueId().toString())).checkRequirements(((Player) sender))) {
                            rankup(((Player) sender), ranks.get(playerRank.get(((Player) sender).getUniqueId().toString())));
                        } else {
                            sender.sendMessage(prefix + parse(rankUpDeny, (Player) sender));
                            ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        }
                    } else {
                        sender.sendMessage(prefix + parse(maxRank, (Player) sender));
                    }

                } else {
                    sender.sendMessage(prefix + parse(unknownCommand, (Player) sender));
                }
            } else {
                sender.sendMessage(prefix + parse(noAccess, (Player) sender));
            }
        } else if (command.getName().equalsIgnoreCase("ranks")) {
            if (args.length == 0) {
                if (sender.hasPermission("notranks.default")) {
                    sender.sendMessage(prefix + parse(guiOpen, (Player) sender));
                    openGUI((Player) sender, 1);
                } else {
                    sender.sendMessage(prefix + parse(noAccess, (Player) sender));
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("notranks.admin")) {
                    try {
                        this.loadConfig();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    sender.sendMessage(prefix + ChatColor.GREEN + "Reloaded NotRanks version " + this.getDescription().getVersion() + ".");
                } else {
                    sender.sendMessage(prefix + parse(noAccess, (Player) sender));
                }
            } else if (args[0].equalsIgnoreCase("set")) {
                if (sender.hasPermission("notranks.admin")) {
                    if (args.length == 3) {
                        Player player = Bukkit.getPlayer(args[1]);
                        if (player != null) {
                            int rankNum = -1;
                            for (Rank rank : ranks){
                                if (ChatColor.stripColor(rank.getName()).equalsIgnoreCase(args[2])){
                                    rankNum = ranks.indexOf(rank) + 1;
                                    break;
                                }
                            }
                            if (rankNum == -1 && args[2].equalsIgnoreCase("none")){
                                rankNum = 0;
                            }
                            if (rankNum == -1){
                                try {
                                    rankNum = Integer.parseInt(args[2]);
                                } catch (NumberFormatException ignored){
                                    sender.sendMessage(prefix + ChatColor.RED + "Unknown Rank");
                                    return true;
                                }
                                if (!(rankNum > -1 && rankNum <= ranks.size())){
                                    sender.sendMessage(prefix + ChatColor.RED + "Unknown Rank");
                                    return true;
                                }
                            }
                            playerRank.replace(player.getUniqueId().toString(), rankNum);
                            if (rankNum == 0) {
                                sender.sendMessage(prefix + ChatColor.GREEN + "Set " + Objects.requireNonNull(Bukkit.getPlayer(args[1])).getName() + "'s rank to " + ChatColor.GRAY + "none" + ChatColor.GREEN + ".");
                            } else {
                                sender.sendMessage(prefix + ChatColor.GREEN + "Set " + Objects.requireNonNull(Bukkit.getPlayer(args[1])).getName() + "'s rank to " + ranks.get(rankNum - 1).getName() + ChatColor.GREEN + ".");
                            }
                        } else {
                            sender.sendMessage(prefix + ChatColor.RED + "Unknown Player.");
                        }
                    } else {
                        sender.sendMessage(prefix + ChatColor.GOLD + "" + ChatColor.BOLD + "Usage: " + ChatColor.YELLOW + "/ranks set (player) (#/rank)");
                    }
                } else {
                    sender.sendMessage(prefix + parse(noAccess, (Player) sender));
                }
            } else if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(prefix + ChatColor.YELLOW + "/ranks" + ChatColor.GOLD + "  Opens the rank gui");
                sender.sendMessage(prefix + ChatColor.YELLOW + "/rankup" + ChatColor.GOLD + "  Ranks yourself up");
                if (sender.hasPermission("notranks.admin")) {
                    sender.sendMessage(prefix + ChatColor.RED + "/ranks reload" + ChatColor.DARK_RED + "  Reloads the plugin");
                    sender.sendMessage(prefix + ChatColor.RED + "/ranks set (player) (#/rank)" + ChatColor.DARK_RED + "  Sets the player's rank");

                }
                sender.sendMessage(prefix + ChatColor.RED + "/ranks help" + ChatColor.DARK_RED + "  What you just typed in");
            } else if (args[0].equalsIgnoreCase("debug") && sender.hasPermission("notranks.admin")) {
                debug = !debug;
                sender.sendMessage(prefix + ChatColor.YELLOW + "Debug mode is now set to: " + debug);

            } else {
                sender.sendMessage(prefix + parse(unknownCommand, (Player) sender));
            }
        } else if (command.getName().equalsIgnoreCase("rankinfo")) {
            if (!(playerRank.get(((Player) sender).getUniqueId().toString()) > ranks.size() - 1)) {
                if (sender.hasPermission("notranks.default")) {
                    Rank rank = ranks.get(getRank((Player) sender));
                    List<String> chat = rank.getLore((Player) sender, false);
                    String name = ChatColor.translateAlternateColorCodes('&',rank.getName());
                    sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "            " + ChatColor.RESET + " " + name + ChatColor.RESET + " " + ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "            ");

                    for (String str : chat){
                        sender.sendMessage(str);
                    }
                    StringBuilder str = new StringBuilder("                        ");
                    int multiplier = name.contains("&l") ? 2 : 1;
                    for (int i = 0; i < ChatColor.stripColor(name).length() * multiplier; i++){
                        str.append(" ");
                    }
                    sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + str);
                } else {
                    sender.sendMessage(prefix + parse(noAccess, (Player) sender));
                }
            } else {
                sender.sendMessage(prefix + parse(maxRank, (Player) sender));
            }
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("ranks")){
            if (sender.hasPermission("notranks.admin")){
                List<String> tab = new ArrayList<>();
                if (args.length == 1){
                    tab.add("reload");
                    tab.add("set");
                    tab.add("help");
                } else if (args.length == 2){
                    if (args[0].equalsIgnoreCase("set")){
                        for (Player player : Bukkit.getOnlinePlayers()){
                            tab.add(player.getName());
                        }
                    }
                } else if (args.length == 3){
                    if (args[0].equalsIgnoreCase("set")){
                        for (Rank rank : ranks){
                            tab.add(ChatColor.stripColor(rank.getName()));
                        }
                        tab.add("none");
                    }
                }
                String typed = args[args.length-1];
                tab.removeIf(test -> test.toLowerCase(Locale.ROOT).indexOf(typed.toLowerCase(Locale.ROOT)) != 0);
                Collections.sort(tab);
                return tab;
            }
        }

        return super.onTabComplete(sender, command, alias, args);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!playerRank.containsKey(event.getPlayer().getUniqueId().toString()))
            playerRank.put(event.getPlayer().getUniqueId().toString(), 0);
    }

    @EventHandler
    public void onInventoryInteract(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(color(guiName)))
            return;
        event.setCancelled(true);
        if (event.getSlot() > event.getView().getTopInventory().getSize())
            return;
        ItemStack current = event.getCurrentItem();
        if (current == null)
            return;
        if (current.isSimilar(fillItem))
            return;
        if (current.isSimilar(exit)) {
            event.getView().close();
        }
        if (current.isSimilar(next)){
            openGUI((Player) event.getWhoClicked(), guiPage.get(event.getWhoClicked().getUniqueId()) + 1);
        }
        if (current.isSimilar(back)){
            openGUI((Player) event.getWhoClicked(), guiPage.get(event.getWhoClicked().getUniqueId()) - 1);
        }
        GUItem guItem = guiLayout[event.getSlot()];
        for (String action : guItem.getActions()){
            if (action.startsWith("[gui]")){
                int page = 1;
                try {
                    page = Integer.parseInt(action.substring(6));
                } catch (NumberFormatException e){
                    Bukkit.getLogger().warning("Error getting gui page number: " + action);
                }
                openGUI((Player) event.getWhoClicked(), page);
            } else if (action.startsWith("[exit]")){
                event.getView().close();
            } else if (action.startsWith("[next]")){
                openGUI((Player) event.getWhoClicked(), guiPage.get(event.getWhoClicked().getUniqueId()) + 1);
            } else if (action.startsWith("[back]")){
                openGUI((Player) event.getWhoClicked(), guiPage.get(event.getWhoClicked().getUniqueId()) - 1);
            } else if (action.startsWith("[command]")){
                String text = action.substring(10);
                text = text.replaceAll("\\{player}", event.getWhoClicked().getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), text);
            } else if (action.startsWith("rank")){
                int rank = Integer.parseInt(action.substring(5)) + ((guiPage.get(event.getWhoClicked().getUniqueId()) - 1) * ranksPerPage);
                int currentRank = getRank((Player) event.getWhoClicked());
                if (rank <= currentRank || rank > currentRank + 1){
                    // not on this rank
                    event.getWhoClicked().sendMessage(prefix + parse(notOnRank, (Player) event.getWhoClicked()));
                    continue;
                }
                if (ranks.get(playerRank.get(event.getWhoClicked().getUniqueId().toString())).checkRequirements(((Player) event.getWhoClicked()))) {
                    rankup(((Player) event.getWhoClicked()), ranks.get(playerRank.get(event.getWhoClicked().getUniqueId().toString())));
                    event.getView().close();
                } else {
                    if (denyClickItem.equals("DISABLE")) {
                        event.getWhoClicked().sendMessage(prefix + parse(rankUpDeny, (Player) event.getWhoClicked()));
                    } else if (denyClickItem.equals("RANK")){
                        ItemStack[] contents = event.getInventory().getContents();
                        ItemStack item = contents[event.getSlot()];
                        ItemMeta meta = item.getItemMeta();
                        assert meta != null;
                        meta.setDisplayName(parse(rankUpDeny, (Player) event.getWhoClicked()));
                        item.setItemMeta(meta);
                        contents[event.getSlot()] = item;
                        event.getInventory().setContents(contents);
                        new BukkitRunnable(){
                            @Override
                            public void run() {
                                if (guiPage.containsKey(event.getWhoClicked().getUniqueId())){
                                    openGUI((Player) event.getWhoClicked(), guiPage.get(event.getWhoClicked().getUniqueId()));
                                }
                            }
                        }.runTaskLater(this, 20);
                    } else {
                        ItemStack[] contents = event.getInventory().getContents();
                        ItemStack item = new ItemStack(Material.valueOf(denyClickItem));
                        ItemMeta meta = item.getItemMeta();
                        assert meta != null;
                        meta.setDisplayName(parse(rankUpDeny, (Player) event.getWhoClicked()));
                        item.setItemMeta(meta);
                        contents[event.getSlot()] = item;
                        event.getInventory().setContents(contents);new BukkitRunnable(){
                            @Override
                            public void run() {
                                if (guiPage.containsKey(event.getWhoClicked().getUniqueId())){
                                    openGUI((Player) event.getWhoClicked(), guiPage.get(event.getWhoClicked().getUniqueId()));
                                }
                            }
                        }.runTaskLater(this, 20);

                    }
                }
            }
        }
        ((Player) event.getWhoClicked()).updateInventory();
    }

    public String parse (String text, OfflinePlayer player){
        return PlaceholderAPI.setPlaceholders(player, color(text));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event){
        if (!addPrefix)
            return;
        String parsedPrefix = prefixFormat;
        int rank = getRank(event.getPlayer()) - 1;
        String rankName = rank != -1 ? ranks.get(rank).getName() : noRank;
        parsedPrefix = parsedPrefix.replaceAll("\\{prefix}", rankName);
        parsedPrefix = parsedPrefix.replaceAll("\\{name}", "%s");
        if (overwritePrefix)
            parsedPrefix+= "%s";
        parsedPrefix = parse(parsedPrefix, event.getPlayer());
        if (overwritePrefix){
            event.setFormat(parsedPrefix);
        } else {
            event.setFormat(parsedPrefix + event.getFormat());
        }
    }



    public void log(){
        try{
            PrintWriter writer = new PrintWriter(today.getPath(), "UTF-8");
            for (String s : logs){
                writer.println(s);
            }
            writer.close();
        } catch (IOException e) {
            // do something
        }
    }

    public void writeLog(String text){
        logs.add("[" +formatExact.format(now) + "] " + text);
    }
}
