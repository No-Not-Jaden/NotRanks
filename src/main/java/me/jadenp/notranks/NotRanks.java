package me.jadenp.notranks;


import me.jadenp.notranks.gui.GUI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
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
 * Migrating files works -
 * Actions with [command] and [gui] change -
 * ranks move to rank-slots -
 * change pages work -
 * set rank works - all commands -
 * add warning if there is no rank path for a gui -
 * placeholder changes work -
 * confirmation gui -
 */

public final class NotRanks extends JavaPlugin implements CommandExecutor, Listener {

    public File playerdata = new File(this.getDataFolder() + File.separator + "playerdata.yml");

    public File logsFolder = new File(this.getDataFolder() + File.separator + "logs");
    Date now = new Date();
    SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
    SimpleDateFormat formatExact = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    File today = new File(logsFolder + File.separator + format.format(now) + ".txt");
    public ArrayList<String> logs = new ArrayList<>();


    public static NotRanks instance;


    public static NotRanks getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        Objects.requireNonNull(getCommand("ranks")).setExecutor(this);
        Objects.requireNonNull(getCommand("rankup")).setExecutor(this);
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getServer().getPluginManager().registerEvents(new GUI(), this);
        new RankPlaceholder(this).register();
        saveDefaultConfig();
        // create logs stuff
        //noinspection ResultOfMethodCallIgnored
        logsFolder.mkdir();
        try {
            if (!today.createNewFile()) {
                try {
                    Scanner scanner = new Scanner(today);
                    while (scanner.hasNextLine()) {
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
            if (playerdata.createNewFile()) {
                Bukkit.getLogger().info("Creating a new player data file.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(playerdata);
        if (!configuration.isSet("version")) {
            // load old config
            for (int i = 1; configuration.isSet(i + ".uuid"); i++) {
                List<Integer> completedRanks = new ArrayList<>();
                int currentRank = configuration.getInt(i + ".rank");
                for (int j = 0; j < currentRank; j++) {
                    completedRanks.add(i);
                }
                Map<String, List<Integer>> completedRank = new HashMap<>();
                completedRank.put("default", completedRanks);
                rankData.put(UUID.fromString(Objects.requireNonNull(configuration.getString(i + ".uuid"))), completedRank);
            }
        } else {
            // load new config
            if (configuration.isConfigurationSection("data")) {
                for (String uuid : Objects.requireNonNull(configuration.getConfigurationSection("data")).getKeys(false)) {
                    Map<String, List<Integer>> playerRankInfo = new HashMap<>();
                    for (String rankType : Objects.requireNonNull(configuration.getConfigurationSection("data." + uuid)).getKeys(false)) {
                        String completedRanks = configuration.getString("data." + uuid + "." + rankType);
                        assert completedRanks != null;
                        String[] separatedRanks = completedRanks.split(",");
                        List<Integer> rankList = new ArrayList<>();
                        for (String separatedRank : separatedRanks) {
                            rankList.add(Integer.parseInt(separatedRank));
                        }
                        playerRankInfo.put(rankType, rankList);
                    }
                    rankData.put(UUID.fromString(uuid), playerRankInfo);
                }
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
                // clean out notifyThroughGUIDelay
                GUI.notifyThroughGUIDelay.entrySet().removeIf(entries -> entries.getValue() < System.currentTimeMillis());
            }
        }.runTaskTimer(this, 6000L, 6000L);

        // check if they completed a rank requirement
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    for (Map.Entry<String, List<Rank>> rankPaths : ranks.entrySet()) {
                        int rankNum = getRankNum(p, rankPaths.getKey());
                        if (rankNum < rankPaths.getValue().size() - 1) { // make sure they aren't on the max rank
                            getRank(rankNum + 1, rankPaths.getKey()).checkRankCompletion(p, rankPaths.getKey()); // check completion on next rank
                        }
                    }
                }
            }
        }.runTaskTimer(this, 500L, 60);
        new BukkitRunnable() {
            @Override
            public void run() {
                log();
            }
        }.runTaskTimerAsynchronously(this, 5000, 5000);
        logs.add("[" + formatExact.format(now) + "] Plugin Loaded!");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        logs.add("[" + formatExact.format(now) + "] Plugin disabling.");
        try {
            saveRanks();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log();
    }


    public void rankup(Player p, String rankType, int newRankIndex) {
        Rank newRank = getRank(newRankIndex, rankType);
        RankupEvent event = new RankupEvent(p, newRank, getRank(p, rankType), newRankIndex);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            if (rankUp.length() > 0) {
                String text = rankUp;
                text = text.replaceAll("\\{player}", p.getName());
                text = text.replaceAll("\\{rank}", newRank.getName());

                Bukkit.broadcastMessage(prefix + parse(text, p));
            }
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
            newRank.rankup(p);
            logs.add("[" + formatExact.format(now) + "] " + p.getName() + " ranked up to " + newRank.getName() + ".");
            addRank(p, rankType, newRankIndex);
        }
    }


    public void saveRanks() throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, List<Integer>>> playerEntry : rankData.entrySet()) {
            String uuid = playerEntry.getKey().toString();
            for (Map.Entry<String, List<Integer>> rankEntry : playerEntry.getValue().entrySet()) {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < rankEntry.getValue().size() - 1; i++) {
                    builder.append(rankEntry.getValue().get(i)).append(",");
                }
                if (!rankEntry.getValue().isEmpty()) {
                    builder.append(rankEntry.getValue().get(rankEntry.getValue().size() - 1));
                    configuration.set("data." + uuid + "." + rankEntry.getKey(), builder.toString());
                }
            }
        }
        configuration.save(playerdata);
    }

    public void loadConfig() throws IOException {
        log();
        LanguageOptions.loadConfig();
        ConfigOptions.loadConfig();
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("rankup")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + ChatColor.RED + "Only players can use this command!");
                return true;
            }
            if (sender.hasPermission("notranks.default")) {
                String rankType = args.length > 0 ? args[0] : "default";
                List<Rank> rankPath = ranks.get(rankType);
                List<Integer> rankProgress = rankData.get(((Player) sender).getUniqueId()).get(rankType);
                if (rankPath == null) {
                    // unknown rank path
                    sender.sendMessage(prefix + parse(unknownRankPath, (Player) sender));
                    return true;
                }
                // 0 if they have no rank, otherwise, 1+ last rank they have gotten
                int nextRank = rankProgress == null ? 0 : rankProgress.get(rankProgress.size() - 1) + 1;
                if (rankPath.size() > nextRank) { // check if they are on the max rank
                    // check requirements
                    if (rankPath.get(nextRank).checkRequirements((Player) sender, rankType)) {
                        rankup((Player) sender, rankType, nextRank);
                    } else {
                        // rankup deny
                        sender.sendMessage(prefix + parse(rankUpDeny, (Player) sender));
                        ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    }
                } else {
                    sender.sendMessage(prefix + parse(maxRank, (Player) sender));
                }
            } else {
                sender.sendMessage(prefix + parse(noAccess, (Player) sender));
            }
        } else if (command.getName().equalsIgnoreCase("ranks")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + ChatColor.RED + "Only players can use this command!");
                return true;
            }
            if (args.length > 0)
                if (args[0].equalsIgnoreCase("reload")) {
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
                    return true;
                } else if (args[0].equalsIgnoreCase("set")) {
                    // /ranks set (player) (path) (rank/#)
                    if (sender.hasPermission("notranks.admin")) {
                        if (args.length > 2) {
                            Player player = Bukkit.getPlayer(args[1]);
                            if (player == null) {
                                sender.sendMessage(prefix + ChatColor.RED + "Unknown Player.");
                                return true;
                            }
                            String path = args.length == 4 ? args[2] : "default";
                            if (!ranks.containsKey(path)) {
                                // no path found
                                sender.sendMessage(prefix + parse(unknownRankPath, (Player) sender));
                                return true;
                            }
                            List<Rank> validRanks = ranks.get(path);
                            List<Integer> rankCompletion = getRankCompletion(player, path);
                            int rankNum = -1;
                            for (Rank rank : validRanks) {
                                if (ChatColor.stripColor(rank.getName()).equalsIgnoreCase(args[2])) {
                                    rankNum = validRanks.indexOf(rank);
                                    break;
                                }
                            }
                            if (rankNum == -1) {
                                // cannot find rank from string
                                if (args[2].equalsIgnoreCase("none")) {
                                    // clear ranks
                                    rankCompletion.clear();
                                    sender.sendMessage(prefix + ChatColor.GREEN + "Cleared " + player.getName() + "'s rank progress on " + ChatColor.GRAY + path + ChatColor.GREEN + ".");
                                } else {
                                    try {
                                        rankNum = Integer.parseInt(args[2]);
                                    } catch (NumberFormatException ignored) {
                                        // not a number
                                        sender.sendMessage(prefix + ChatColor.RED + "Unknown Rank");
                                        return true;
                                    }
                                    if (rankNum < 0 || rankNum >= validRanks.size()) {
                                        // number out of bounds
                                        sender.sendMessage(prefix + ChatColor.RED + "Unknown Rank");
                                        return true;
                                    }
                                }
                            }
                            if (rankNum != -1) {
                                // put this rank number at the top of the rank completion list
                                rankCompletion.remove((Integer) rankNum);
                                rankCompletion.add(rankNum);
                                sender.sendMessage(prefix + ChatColor.GREEN + "Set " + player.getName() + "'s rank to " + validRanks.get(rankNum).getName() + ChatColor.GREEN + ".");
                            }
                            setRankCompletion(player, path, rankCompletion);
                        } else {
                            sender.sendMessage(prefix + ChatColor.GOLD + "" + ChatColor.BOLD + "Usage: " + ChatColor.YELLOW + "/ranks set (player) <path> (#/rank)");
                        }
                    } else {
                        sender.sendMessage(prefix + parse(noAccess, (Player) sender));
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("help")) {
                    sender.sendMessage(prefix + ChatColor.YELLOW + "/ranks <path>" + ChatColor.GOLD + "  Opens the rank gui");
                    sender.sendMessage(prefix + ChatColor.YELLOW + "/rankup <path>" + ChatColor.GOLD + "  Ranks yourself up");
                    if (sender.hasPermission("notranks.admin")) {
                        sender.sendMessage(prefix + ChatColor.RED + "/ranks reload" + ChatColor.DARK_RED + "  Reloads the plugin");
                        sender.sendMessage(prefix + ChatColor.RED + "/ranks set (player) <path> (#/rank)" + ChatColor.DARK_RED + "  Sets the player's rank");
                        sender.sendMessage(prefix + ChatColor.RED + "/ranks remove (player) <path> (#/rank)" + ChatColor.DARK_RED + "  Removes a player's rank");

                    }
                    sender.sendMessage(prefix + ChatColor.RED + "/ranks help" + ChatColor.DARK_RED + "  What you just typed in");
                    return true;
                } else if (args[0].equalsIgnoreCase("debug") && sender.hasPermission("notranks.admin")) {
                    debug = !debug;
                    sender.sendMessage(prefix + ChatColor.YELLOW + "Debug mode is now set to: " + debug);
                    return true;
                } else if (args[0].equalsIgnoreCase("remove")) {
                    // /rank remove (player) <path> <rank/#>
                    if (sender.hasPermission("notranks.admin")) {
                        if (args.length > 2) {
                            Player player = Bukkit.getPlayer(args[1]);
                            if (player == null) {
                                sender.sendMessage(prefix + ChatColor.RED + "Unknown Player.");
                                return true;
                            }
                            String path = args.length == 4 ? args[2] : "default";
                            if (!ranks.containsKey(path)) {
                                // no path found
                                sender.sendMessage(prefix + parse(unknownRankPath, (Player) sender));
                                return true;
                            }
                            List<Rank> validRanks = ranks.get(path);
                            List<Integer> rankCompletion = getRankCompletion(player, path);
                            int rankNum = -1;
                            for (Rank rank : validRanks) {
                                if (ChatColor.stripColor(rank.getName()).equalsIgnoreCase(args[2])) {
                                    rankNum = validRanks.indexOf(rank);
                                    break;
                                }
                            }
                            if (rankNum == -1) {
                                // cannot find rank from string
                                try {
                                    rankNum = Integer.parseInt(args[2]);
                                } catch (NumberFormatException ignored) {
                                    // not a number
                                    sender.sendMessage(prefix + ChatColor.RED + "Unknown Rank");
                                    return true;
                                }
                                if (rankNum < 0 || rankNum >= validRanks.size()) {
                                    // number out of bounds
                                    sender.sendMessage(prefix + ChatColor.RED + "Unknown Rank");
                                    return true;
                                }
                            }

                            if (rankCompletion.remove((Integer) rankNum)) {
                                sender.sendMessage(prefix + ChatColor.GREEN + "Removed " + player.getName() + "'s rank of " + validRanks.get(rankNum).getName() + ChatColor.GREEN + " on " + ChatColor.GRAY + path + ChatColor.GREEN + ".");
                                setRankCompletion(player, path, rankCompletion);
                            } else {
                                sender.sendMessage(prefix + ChatColor.RED + player.getName() + " does not have the rank of " + validRanks.get(rankNum).getName() + ChatColor.RED + ".");
                            }
                        } else {
                            sender.sendMessage(prefix + ChatColor.GOLD + "" + ChatColor.BOLD + "Usage: " + ChatColor.YELLOW + "/ranks set (player) <path> (#/rank)");
                        }
                    } else {
                        sender.sendMessage(prefix + parse(noAccess, (Player) sender));
                    }
                }
            // open gui
            String rankType = args.length > 0 ? args[0].toLowerCase() : "default";
            if (sender.hasPermission("notranks.default")) {
                if (ranks.containsKey(rankType)) {
                    GUI.openGUI((Player) sender, rankType, 1);
                    sender.sendMessage(prefix + parse(guiOpen, (Player) sender));
                } else {
                    // no rank path found
                    sender.sendMessage(prefix + parse(unknownRankPath, (Player) sender));
                }
            } else {
                // no permission
                sender.sendMessage(prefix + parse(noAccess, (Player) sender));
            }

        } else if (command.getName().equalsIgnoreCase("rankinfo")) {
            if (sender.hasPermission("notranks.default")) {
                String rankType = args.length > 0 ? args[0].toLowerCase() : "default";
                List<Rank> rankPath = ranks.get(rankType);
                List<Integer> rankProgress = getRankCompletion((Player) sender, rankType);
                if (rankPath == null) {
                    // unknown rank path
                    sender.sendMessage(prefix + parse(unknownRankPath, (Player) sender));
                    return true;
                }
                // 0 if they have no rank, otherwise, 1+ last rank they have gotten
                int nextRank = rankProgress == null || rankProgress.isEmpty() ? 0 : rankProgress.get(rankProgress.size() - 1) + 1;
                if (rankPath.size() > nextRank) { // check if they are on the max rank
                    // display the next rank
                    Rank rank = rankPath.get(nextRank);
                    List<String> chat = rank.getLore((Player) sender, false);
                    String name = ChatColor.translateAlternateColorCodes('&', rank.getName());
                    sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "            " + ChatColor.RESET + " " + name + ChatColor.RESET + " " + ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "            ");

                    for (String str : chat) {
                        sender.sendMessage(str);
                    }
                    StringBuilder str = new StringBuilder("                        ");
                    int multiplier = name.contains("&l") ? 2 : 1;
                    for (int i = 0; i < ChatColor.stripColor(name).length() * multiplier; i++) {
                        str.append(" ");
                    }
                    sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + str);
                } else {
                    sender.sendMessage(prefix + parse(maxRank, (Player) sender));
                }
            } else {
                sender.sendMessage(prefix + parse(noAccess, (Player) sender));
            }
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("ranks")) {

            List<String> tab = new ArrayList<>();
            if (args.length == 1) {
                if (sender.hasPermission("notranks.admin")) {
                    tab.add("reload");
                    tab.add("set");
                    tab.add("remove");
                }
                if (sender.hasPermission("notranks.default")) {
                    tab.add("help");
                    for (Map.Entry<String, List<Rank>> entry : ranks.entrySet()) {
                        tab.add(entry.getKey());
                    }
                }
            } else if (args.length == 2) {
                if ((args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove")) && sender.hasPermission("notranks.admin")) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        tab.add(player.getName());
                    }
                }
            } else if (args.length == 3) {
                if ((args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove")) && sender.hasPermission("notranks.admin")) {
                    for (Map.Entry<String, List<Rank>> entry : ranks.entrySet()) {
                        tab.add(entry.getKey());
                    }
                    if (ranks.containsKey("default"))
                        for (Rank rank : ranks.get("default")) {
                            tab.add(ChatColor.stripColor(rank.getName()));
                        }
                    tab.add("none");
                }
            } else if (args.length == 4) {
                if ((args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove")) && sender.hasPermission("notranks.admin")) {
                    if (ranks.containsKey(args[2]))
                        for (Rank rank : ranks.get(args[2])) {
                            tab.add(ChatColor.stripColor(rank.getName()));
                        }
                    tab.add("none");
                }
            }
            String typed = args[args.length - 1];
            tab.removeIf(test -> test.toLowerCase(Locale.ROOT).indexOf(typed.toLowerCase(Locale.ROOT)) != 0);
            Collections.sort(tab);
            return tab;

        }

        return super.onTabComplete(sender, command, alias, args);
    }


    // control chat if enabled
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!addPrefix)
            return;
        String parsedPrefix = prefixFormat;
        Rank rank = getRank(event.getPlayer(), "default");
        String rankName = rank != null ? rank.getName() : noRank;
        parsedPrefix = parsedPrefix.replaceAll("\\{prefix}", rankName);
        parsedPrefix = parsedPrefix.replaceAll("\\{name}", "%s");
        if (overwritePrefix)
            parsedPrefix += "%s";
        parsedPrefix = parse(parsedPrefix, event.getPlayer());
        if (overwritePrefix) {
            event.setFormat(parsedPrefix);
        } else {
            event.setFormat(parsedPrefix + event.getFormat());
        }
    }


    public void log() {
        try {
            PrintWriter writer = new PrintWriter(today.getPath(), "UTF-8");
            for (String s : logs) {
                writer.println(s);
            }
            writer.close();
        } catch (IOException e) {
            // do something
        }
    }

    public void writeLog(String text) {
        logs.add("[" + formatExact.format(now) + "] " + text);
    }
}
