package me.jadenp.notranks;


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
import java.util.List;

import static me.jadenp.notranks.ConfigOptions.*;
import static me.jadenp.notranks.LanguageOptions.*;

import static org.bukkit.util.NumberConversions.ceil;

/**
 * check to see if rankup msg works
 */
public final class NotRanks extends JavaPlugin implements CommandExecutor, Listener {

    private String prefix;
    public File playerdata = new File(this.getDataFolder() + File.separator + "playerdata.yml");

    public File logsFolder = new File(this.getDataFolder() + File.separator + "logs");
    Date now = new Date();
    SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
    SimpleDateFormat formatExact = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    File today = new File(logsFolder + File.separator + format.format(now) + ".txt");
    public ArrayList<String> logs = new ArrayList<>();
    public HashMap<String, Integer> playerRank = new HashMap<>();

    public static NotRanks instance;

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
        saveDefaultConfig();
        // create logs stuff
        if (!logsFolder.exists()){
            logsFolder.mkdir();
        }
        if (!today.exists()){
            try {
                today.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
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
        // creating file to store player's ranks if the file hadn't already been created
        if (!playerdata.exists()) {
            try {
                playerdata.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(playerdata);
        for (int i = 1; configuration.getString(i + ".uuid") != null; i++) {
            if (playerRank.containsKey(configuration.getString(i + ".uuid"))) {
                playerRank.replace(configuration.getString(i + ".uuid"), configuration.getInt(i + ".rank"));
            } else {
                playerRank.put(configuration.getString(i + ".uuid"), configuration.getInt(i + ".rank"));
            }
        }
        loadConfig();

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
        if (event.isCancelled()){
            // log here
        } else {
            if (rankUp.length() > 0) {
                String text = color(rankUp, p);
                if (text.contains("{player}"))
                    text = text.replace("{player}", p.getName());
                if (text.contains("{rank}"))
                    text = text.replace("{rank}", ranks.get(playerRank.get(p.getUniqueId().toString())).getName());
                Bukkit.broadcastMessage(prefix + text);
            }
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
            newRank.rankup(p);
            logs.add("[" +formatExact.format(now) + "] " + p.getName() + " ranked up to " + newRank.getName() + ".");
            playerRank.replace(p.getUniqueId().toString(), playerRank.get(p.getUniqueId().toString()) + 1);
        }

    }

    public void setRank(Player p, int rank) {
        playerRank.replace(p.getUniqueId().toString(), rank);
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

    public void loadConfig() {
        log();
        ConfigOptions.loadConfig();
        LanguageOptions.loadConfig();
    }

    public void openGUI(Player p, int page) {
        ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = fill.getItemMeta();
        assert meta != null;
        meta.setDisplayName(" ");
        fill.setItemMeta(meta);
        ItemStack exit = new ItemStack(Material.BARRIER);
        meta = exit.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Exit");
        exit.setItemMeta(meta);
        ItemStack next = new ItemStack(Material.SPECTRAL_ARROW);
        meta = next.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.DARK_GRAY + "Next Page");
        next.setItemMeta(meta);
        ItemStack back = new ItemStack(Material.TIPPED_ARROW);
        meta = back.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.DARK_GRAY + "Last Page");
        back.setItemMeta(meta);
        int size = (ceil((double) ranks.size() / 7) * 9);
        if (size <= 36) {
            Inventory inv = Bukkit.createInventory(p, size + 18, color(guiName, p));
            ItemStack[] contents = inv.getContents();
            for (int i = 0; i < 10; i++) {
                contents[i] = fill;
            }
            int rankNum = 0;
            for (int i = 10; i < contents.length; i++) {
                // some math if contents is at end or start of line
                // check if i-10 ranks is a thing or set to  fill
                if (i % 9 == 0 || (i + 1) % 9 == 0) {
                    contents[i] = fill;
                } else if (rankNum < ranks.size()) {
                    contents[i] = ranks.get(rankNum).getItem(p, (playerRank.get(p.getUniqueId().toString()) > rankNum));
                    rankNum++;
                } else {
                    contents[i] = fill;
                }

            }
            // set last item to exit button
            contents[contents.length - 5] = exit;
            inv.setContents(contents);
            p.openInventory(inv);
        } else {
            Inventory inv = Bukkit.createInventory(p, 54, color(guiName, p));
            ItemStack[] contents = inv.getContents();
            for (int i = 0; i < 10; i++) {
                contents[i] = fill;
            }
            int rankNum = 0;
            rankNum += (page-1) * 36;
            for (int i = 10; i < 45; i++) {
                // some math if contents is at end or start of line
                // check if i-10 ranks is a thing or set to  fill
                if (i % 9 == 0 || (i + 1) % 9 == 0) {
                    contents[i] = fill;
                } else if (rankNum < ranks.size()) {
                    contents[i] = ranks.get(rankNum).getItem(p, (playerRank.get(p.getUniqueId().toString()) > rankNum));
                    rankNum++;
                } else {
                    contents[i] = fill;
                }
            }
            for (int i = 45; i < 54; i++) {
                contents[i] = fill;
            }
            contents[contents.length - 5] = exit;
            contents[contents.length - 1] = next;
            if (page > 1){
                contents[contents.length-9] = back;
            }
        }
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
                            sender.sendMessage(prefix + color(rankUpDeny, (Player) sender));
                            ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        }
                    } else {
                        sender.sendMessage(prefix + color(maxRank, (Player) sender));
                    }

                } else {
                    sender.sendMessage(prefix + color(unknownCommand, (Player) sender));
                }
            } else {
                sender.sendMessage(prefix + color(noAccess, (Player) sender));
            }
        } else if (command.getName().equalsIgnoreCase("ranks")) {
            if (args.length == 0) {
                if (sender.hasPermission("notranks.default")) {
                    sender.sendMessage(prefix + color(guiOpen, (Player) sender));
                    openGUI((Player) sender, 1);
                } else {
                    sender.sendMessage(prefix + color(noAccess, (Player) sender));
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("notranks.admin")) {
                    loadConfig();
                    sender.sendMessage(prefix + ChatColor.GREEN + "Reloaded NotRanks version " + this.getDescription().getVersion() + ".");
                } else {
                    sender.sendMessage(prefix + color(noAccess, (Player) sender));
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
                    sender.sendMessage(prefix + color(noAccess, (Player) sender));
                }
            } else if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(prefix + ChatColor.YELLOW + "/ranks" + ChatColor.GOLD + "  Opens the rank gui");
                sender.sendMessage(prefix + ChatColor.YELLOW + "/rankup" + ChatColor.GOLD + "  Ranks yourself up");
                if (sender.hasPermission("notranks.admin")) {
                    sender.sendMessage(prefix + ChatColor.RED + "/ranks reload" + ChatColor.DARK_RED + "  Reloads the plugin");
                    sender.sendMessage(prefix + ChatColor.RED + "/ranks set (player) (#/rank)" + ChatColor.DARK_RED + "  Sets the player's rank");

                }
                sender.sendMessage(prefix + ChatColor.RED + "/ranks help" + ChatColor.DARK_RED + "  What you just typed in");
            } else {
                sender.sendMessage(prefix + color(unknownCommand, (Player) sender));
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
                    sender.sendMessage(prefix + color(noAccess, (Player) sender));
                }
            } else {
                sender.sendMessage(prefix + color(maxRank, (Player) sender));
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
        if (event.getCurrentItem() != null) {
            ItemStack exit = new ItemStack(Material.BARRIER);
            ItemMeta meta = exit.getItemMeta();
            assert meta != null;
            meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Exit");
            exit.setItemMeta(meta);
            ItemStack next = new ItemStack(Material.SPECTRAL_ARROW);
            meta = next.getItemMeta();
            assert meta != null;
            meta.setDisplayName(ChatColor.DARK_GRAY + "Next Page");
            next.setItemMeta(meta);
            ItemStack back = new ItemStack(Material.TIPPED_ARROW);
            meta = back.getItemMeta();
            assert meta != null;
            meta.setDisplayName(ChatColor.DARK_GRAY + "Last Page");
            back.setItemMeta(meta);
            if (event.getView().getTitle().equals(color(guiName, (Player) event.getWhoClicked()))) {
                event.setCancelled(true);
                if (event.getCurrentItem() != null)
                    if (Objects.equals(event.getCurrentItem(), exit)) {
                        event.getWhoClicked().closeInventory();
                    } else if (Objects.requireNonNull(event.getCurrentItem()).getType() == Material.PLAYER_HEAD) {
                        for (int i = 0; i < ranks.size(); i++) {
                            if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ranks.get(i).getName())) {
                                if (playerRank.get(event.getWhoClicked().getUniqueId().toString()) == i) {
                                    if (ranks.get(i).checkRequirements((Player) event.getWhoClicked())) {
                                        event.getWhoClicked().closeInventory();

                                        rankup((Player) event.getWhoClicked(), ranks.get(i));
                                    } else {
                                        event.getWhoClicked().closeInventory();
                                        event.getWhoClicked().sendMessage(prefix + color(rankUpDeny, (Player) event.getWhoClicked()));
                                        ((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                                    }
                                } else {
                                    event.getWhoClicked().sendMessage(prefix + color(notOnRank, (Player) event.getWhoClicked()));
                                    ((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                                }
                            }
                        }

                    } else if (event.getCurrentItem().equals(next)) {
                        for (int rankNum = 0; rankNum < ranks.size(); rankNum += 36) {
                            if (event.getInventory().contains(ranks.get(rankNum).getItem((Player) event.getWhoClicked(), (playerRank.get(event.getWhoClicked().getUniqueId().toString()) > rankNum)))) {
                                openGUI((Player) event.getWhoClicked(), rankNum + 2);
                            }
                        }
                    } else if (event.getCurrentItem().equals(back)) {
                        for (int rankNum = 0; rankNum < ranks.size(); rankNum += 36) {
                            if (event.getInventory().contains(ranks.get(rankNum).getItem((Player) event.getWhoClicked(), (playerRank.get(event.getWhoClicked().getUniqueId().toString()) > rankNum)))) {
                                openGUI((Player) event.getWhoClicked(), rankNum);
                            }
                        }
                    }
                ((Player) event.getWhoClicked()).updateInventory();

            }
        }
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
        parsedPrefix = color(parsedPrefix, event.getPlayer());
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
