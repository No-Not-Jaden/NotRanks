package me.jadenp.notranks;

import me.jadenp.notranks.gui.GUI;
import me.jadenp.notranks.gui.GUIOptions;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;

import static me.jadenp.notranks.ConfigOptions.*;
import static me.jadenp.notranks.LanguageOptions.*;

public class Commands implements CommandExecutor, TabCompleter {
    public Commands() {
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("notranks.default") && !sender.hasPermission("notranks.admin")) {
            sender.sendMessage(prefix + parse(noAccess, (Player) sender));
            return true;
        }
        if (command.getName().equalsIgnoreCase("notrankup")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + ChatColor.RED + "Only players can use this command!");
                return true;
            }

            String rankType = args.length > 0 && !containsIgnoreCase("confirm", args[0]) ? args[0] : "default";
            List<Rank> rankPath = ranks.get(rankType);
            List<Integer> rankProgress = getRankCompletion((Player) sender, rankType);
            if (rankPath == null) {
                // unknown rank path
                sender.sendMessage(prefix + parse(unknownRankPath, (Player) sender));
                return true;
            }
            GUIOptions guiOptions = GUI.getGUI(rankType);
            // check if they have permission
            if (!sender.hasPermission("notranks." + rankType) && guiOptions.isPermissionRequired() && !sender.hasPermission("notranks.admin")) {
                sender.sendMessage(prefix + parse(noAccess, (Player) sender));
                return true;
            }
            // get next rank num, if none is specified, +1 last rank
            int nextRank = -1;
            if (args.length > 1) {
                try {
                    nextRank = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    // try and find rank from name
                    for (int i = 0; i < rankPath.size(); i++) {
                        Rank rank = rankPath.get(i);
                        if (ChatColor.stripColor(rank.getName()).equalsIgnoreCase(args[1])) {
                            nextRank = i;
                            break;
                        }
                    }
                }
            }
            if (nextRank == -1)
                nextRank = rankProgress == null || rankProgress.isEmpty() ? 0 : rankProgress.get(rankProgress.size() - 1) + 1;

            if (nextRank > rankPath.size()) {
                if (debug)
                    Bukkit.getLogger().info("[NotRanks] Player has a rank that doesn't exist! Did you reduce the amount of ranks? Allowing player to rankup to first rank.");
                nextRank = 0;
            }

            if (debug)
                Bukkit.getLogger().info("[NotRanks] Attempting to rankup " + sender.getName() + " to " + rankType + ":" + nextRank);
            if (ConfigOptions.isRankUnlocked((Player) sender, rankType, nextRank) == Rank.CompletionStatus.COMPLETE) {
                // rank already unlocked
                sender.sendMessage(prefix + LanguageOptions.parse(LanguageOptions.alreadyCompleted, (Player) sender));
                return true;
            }
            if (rankPath.size() == nextRank) {
                if (debug)
                    Bukkit.getLogger().info("[NotRanks] Rank path size: " + rankPath.size());
                // max rank
                sender.sendMessage(prefix + parse(maxRank, (Player) sender));
                return true;
            }
            if (getRankNum((Player) sender, rankType) + 1 != nextRank && GUI.getGUI(rankType).isOrderlyProgression()) {
                // cant skip ranks
                sender.sendMessage(prefix + parse(notOnRank, (Player) sender));
                return true;
            }
            if (rankPath.get(nextRank).checkUncompleted((Player) sender, rankType)) {
                // requirements not completed
                sender.sendMessage(prefix + parse(rankUpDeny, (Player) sender));
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                return true;
            }
            if (!confirmation || (args.length > 0 && containsIgnoreCase("confirm", args[args.length - 1]))) {
                // rankup
                NotRanks.getInstance().rankup((Player) sender, rankType, nextRank);
            } else {
                // confirmation gui
                GUI.openGUI((Player) sender, "confirmation", 1, getRankFormat(nextRank, rankType));
            }
        } else if (command.getName().equalsIgnoreCase("notranks")) {
            if (args.length > 0)
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("notranks.admin")) {
                        try {
                            NotRanks.getInstance().loadConfig();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        sender.sendMessage(prefix + ChatColor.GREEN + "Reloaded NotRanks version " + NotRanks.getInstance().getDescription().getVersion() + ".");
                    } else {
                        if (sender instanceof Player)
                            sender.sendMessage(prefix + parse(noAccess, (Player) sender));
                    }
                    return true;
                } else if (containsIgnoreCase("prefix", args[0])) {
                    if (debug) {
                        if (prefixSelections.containsKey(((Player) sender).getUniqueId()))
                            Bukkit.getLogger().info("[NotRanks] Prefix string for " + sender.getName() + " is " + prefixSelections.get(((Player) sender).getUniqueId()));
                        else
                            Bukkit.getLogger().info("[NotRanks] Prefix string for " + sender.getName() + " is none");
                    }
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(prefix + ChatColor.RED + "Only players can use this command!");
                        return true;
                    }
                    if (args.length == 1) {
                        // open prefix gui
                        GUI.openGUI((Player) sender, "choose-prefix", 1);
                        return true;
                    }
                    if (containsIgnoreCase("reset", args[1])) {
                        // reset prefix
                        prefixSelections.remove(((Player) sender).getUniqueId());
                        sender.sendMessage(prefix + parse(prefixReset, (Player) sender));
                        return true;
                    }
                    String path = args[1];
                    if (!ranks.containsKey(path)) {
                        // no path found
                        sender.sendMessage(prefix + parse(unknownRankPath, (Player) sender));
                        return true;
                    }
                    if (args.length == 2){
                        // only path
                        prefixSelections.put(((Player) sender).getUniqueId(), "p:" + path);
                        sender.sendMessage(prefix + parse(prefixPath.replaceAll("\\{path}", Matcher.quoteReplacement(path)), (Player) sender));
                        return true;
                    }
                    int rankNum = getRankNumFromText(path, ChatColor.stripColor(parse(args[2], (Player) sender)));

                    switch (rankNum){
                        case -1:
                            // unknown rank
                            sender.sendMessage(prefix + parse(unknownRank, (Player) sender));
                            break;
                        case -2:
                            // reset prefix
                            prefixSelections.remove(((Player) sender).getUniqueId());
                            sender.sendMessage(prefix + parse(prefixReset, (Player) sender));
                            break;
                        default:
                            if (isRankUnlocked((Player) sender, path, rankNum) != Rank.CompletionStatus.COMPLETE){
                                // haven't completed the rank
                                sender.sendMessage(prefix + parse(notOnRank, (Player) sender));
                                return true;
                            }
                            prefixSelections.put(((Player) sender).getUniqueId(), "r:" + rankNum + "p:" + path);
                            sender.sendMessage(prefix + parse(prefixRank.replaceAll("\\{rank}", Matcher.quoteReplacement(color(ranks.get(path).get(rankNum).getName()))), (Player) sender));
                            break;
                    }

                    return true;
                } else if (containsIgnoreCase("set", args[0])) {
                    // /ranks set (player) (path) (rank/#)
                    if (sender.hasPermission("notranks.admin")) {
                        if (args.length > 2) {
                            Player player = Bukkit.getPlayer(args[1]);
                            if (player == null) {
                                sender.sendMessage(prefix + ChatColor.RED + "Unknown Player.");
                                return true;
                            }
                            String path = args.length == 4 ? args[2] : "default";
                            String rankArg = args.length == 4 ? args[3] : args[2];
                            if (!ranks.containsKey(path)) {
                                // no path found
                                sender.sendMessage(prefix + parse(unknownRankPath, (Player) sender));
                                return true;
                            }
                            int rankNum = getRankNumFromText(path, rankArg);

                            List<Integer> rankCompletion = getRankCompletion(player, path);
                            switch (rankNum){
                                case -1:
                                    // unknown rank
                                    sender.sendMessage(prefix + parse(unknownRank, null));
                                    break;
                                case -2:
                                    // clear ranks
                                    rankCompletion.clear();
                                    sender.sendMessage(prefix + ChatColor.GREEN + "Cleared " + player.getName() + "'s rank progress on " + ChatColor.GRAY + path + ChatColor.GREEN + ".");
                                    break;
                                default:
                                    // put this rank number at the top of the rank completion list
                                    rankCompletion.remove((Integer) rankNum);
                                    rankCompletion.add(rankNum);
                                    sender.sendMessage(prefix + ChatColor.GREEN + "Set " + player.getName() + "'s rank to " + parse(Objects.requireNonNull(getRank(rankNum, path)).getName(), player) + ChatColor.GREEN + ".");
                                    break;
                            }

                            setRankCompletion(player, path, rankCompletion);
                        } else {
                            sender.sendMessage(prefix + ChatColor.GOLD + ChatColor.BOLD + "Usage: " + ChatColor.YELLOW + "/ranks " + argumentAliases.get("set").get(0) + " (player) <path> (#/rank)");
                        }
                    } else {
                        sender.sendMessage(prefix + parse(noAccess, (Player) sender));
                    }
                    return true;
                } else if (containsIgnoreCase("help", args[0])) {
                    sender.sendMessage(prefix + ChatColor.YELLOW + "/ranks <path>" + ChatColor.GOLD + "  Opens the rank GUI");
                    sender.sendMessage(prefix + ChatColor.YELLOW + "/rankinfo <path> <#/rank>" + ChatColor.GOLD + "  Opens the rank GUI");
                    sender.sendMessage(prefix + ChatColor.YELLOW + "/rankup <path> <#/rank>" + ChatColor.GOLD + "  Ranks yourself up");
                    sender.sendMessage(prefix + ChatColor.YELLOW + "/ranks " + argumentAliases.get("prefix").get(0) + ChatColor.GOLD + "  Open rank prefix GUI");
                    sender.sendMessage(prefix + ChatColor.YELLOW + "/ranks " + argumentAliases.get("prefix").get(0) + " <path/reset> <#/rank>" + ChatColor.GOLD + "  Changes your prefix");
                    if (sender.hasPermission("notranks.admin")) {
                        sender.sendMessage(prefix + ChatColor.RED + "/ranks reload" + ChatColor.DARK_RED + "  Reloads the plugin");
                        sender.sendMessage(prefix + ChatColor.RED + "/ranks " + argumentAliases.get("set").get(0) + " (player) <path> (#/rank)" + ChatColor.DARK_RED + "  Sets the player's rank");
                        sender.sendMessage(prefix + ChatColor.RED + "/ranks " + argumentAliases.get("remove").get(0) + " (player) <path> (#/rank)" + ChatColor.DARK_RED + "  Removes a player's rank");

                    }
                    sender.sendMessage(prefix + ChatColor.RED + "/ranks " + argumentAliases.get("help").get(0)  + ChatColor.DARK_RED + "  What you just typed in");
                    return true;
                } else if (args[0].equalsIgnoreCase("debug") && sender.hasPermission("notranks.admin")) {
                    debug = !debug;
                    sender.sendMessage(prefix + ChatColor.YELLOW + "Debug mode is now set to: " + debug);
                    if (debug)
                        sender.sendMessage(prefix + ChatColor.YELLOW + "Do" + ChatColor.WHITE + " /ranks reload" + ChatColor.YELLOW + " to check the ranks and gui.");
                    return true;
                } else if (containsIgnoreCase("remove", args[0])) {
                    // /rank remove (player) <path> <rank/#>
                    if (sender.hasPermission("notranks.admin")) {
                        if (args.length > 2) {
                            Player player = Bukkit.getPlayer(args[1]);
                            if (player == null) {
                                sender.sendMessage(prefix + ChatColor.RED + "Unknown Player.");
                                return true;
                            }
                            String path = args.length == 4 ? args[2] : "default";
                            String rankArg = args.length == 4 ? args[3] : args[2];
                            if (!ranks.containsKey(path)) {
                                // no path found
                                sender.sendMessage(prefix + parse(unknownRankPath, (Player) sender));
                                return true;
                            }
                            List<Rank> validRanks = ranks.get(path);
                            List<Integer> rankCompletion = getRankCompletion(player, path);
                            int rankNum = -1;
                            for (Rank rank : validRanks) {
                                if (ChatColor.stripColor(rank.getName()).equalsIgnoreCase(rankArg)) {
                                    rankNum = validRanks.indexOf(rank);
                                    break;
                                }
                            }
                            if (rankNum == -1) {
                                // cannot find rank from string
                                try {
                                    rankNum = Integer.parseInt(rankArg);
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
                            sender.sendMessage(prefix + ChatColor.GOLD + ChatColor.BOLD + "Usage: " + ChatColor.YELLOW + "/ranks remove (player) <path> (#/rank)");
                        }
                    } else {
                        assert sender instanceof Player;
                        sender.sendMessage(prefix + parse(noAccess, (Player) sender));
                    }
                    return true;
                }
            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + ChatColor.RED + "Only players can use this command!");
                return true;
            }
            // open gui
            String rankType = args.length > 0 ? args[0].toLowerCase() : "default";
            // check if the path exists
            if (!ranks.containsKey(rankType)) {
                sender.sendMessage(prefix + parse(unknownRankPath, (Player) sender));
                return true;
            }
            GUIOptions guiOptions = GUI.getGUI(rankType);
            // check if they have permission
            if (!sender.hasPermission("notranks." + rankType) && guiOptions.isPermissionRequired() && !sender.hasPermission("notranks.admin")) {
                sender.sendMessage(prefix + parse(noAccess, (Player) sender));
                return true;
            }
            GUI.openGUI((Player) sender, rankType, 1);
            sender.sendMessage(prefix + parse(guiOpen, (Player) sender));
            return true;
        } else if (command.getName().equalsIgnoreCase("notrankinfo")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + ChatColor.RED + "Only players can use this command!");
                return true;
            }
            // /rankinfo (path) (rank)
            String rankType = args.length > 0 ? args[0].toLowerCase() : "default";
            List<Rank> rankPath = ranks.get(rankType);
            List<Integer> rankProgress = getRankCompletion((Player) sender, rankType);
            if (rankPath == null) {
                // unknown rank path
                sender.sendMessage(prefix + parse(unknownRankPath, (Player) sender));
                return true;
            }
            GUIOptions guiOptions = GUI.getGUI(rankType);
            // check if they have permission
            if (!sender.hasPermission("notranks." + rankType) && guiOptions.isPermissionRequired() && !sender.hasPermission("notranks.admin")) {
                sender.sendMessage(prefix + parse(noAccess, (Player) sender));
                return true;
            }
            // get next rank num, if none is specified, +1 last rank
            int nextRank = -1;
            if (args.length > 1) {
                try {
                    nextRank = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    // try and find rank from name
                    for (int i = 0; i < rankPath.size(); i++) {
                        Rank rank = rankPath.get(i);
                        if (ChatColor.stripColor(rank.getName()).equalsIgnoreCase(args[1])) {
                            nextRank = i;
                            break;
                        }
                    }
                }
            }
            if (nextRank == -1)
                nextRank = rankProgress == null || rankProgress.isEmpty() ? 0 : rankProgress.get(rankProgress.size() - 1) + 1;

            if (rankPath.size() > nextRank) { // check if they are on the max rank
                // display the next rank
                Rank rank = rankPath.get(nextRank);
                List<String> chat = rank.getLore((Player) sender, Rank.CompletionStatus.INCOMPLETE);
                String name = rank.getName();
                sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "            " + ChatColor.RESET + " " + name + ChatColor.RESET + " " + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "            ");

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
        }
        return true;
    }

    private static boolean containsIgnoreCase(String argumentName, String arg) {
        for (String string : argumentAliases.get(argumentName))
            if (string.equalsIgnoreCase(arg))
                return true;
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> tab = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("notranks")) {
            if (args.length == 1) {
                if (sender.hasPermission("notranks.admin")) {
                    tab.add("reload");
                    tab.addAll(argumentAliases.get("set"));
                    tab.addAll(argumentAliases.get("remove"));
                }
                if (sender.hasPermission("notranks.default")) {
                    tab.addAll(argumentAliases.get("help"));
                    tab.addAll(argumentAliases.get("prefix"));
                    for (Map.Entry<String, List<Rank>> entry : ranks.entrySet()) {
                        tab.add(entry.getKey());
                    }
                }
            } else if (args.length == 2) {
                if (containsIgnoreCase("prefix", args[0]) && sender.hasPermission("notranks.default")){
                    tab.addAll(argumentAliases.get("reset"));
                    for (Map.Entry<String, List<Rank>> entry : ranks.entrySet()) {
                        tab.add(entry.getKey());
                    }
                }
                if ((containsIgnoreCase("set", args[0]) || containsIgnoreCase("remove", args[0])) && sender.hasPermission("notranks.admin")) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        tab.add(player.getName());
                    }
                }
            } else if (args.length == 3) {
                if (containsIgnoreCase("prefix", args[0]) && sender.hasPermission("notranks.default")){
                    List<Rank> rankList = ranks.containsKey(args[1]) ? ranks.get(args[1]) : new ArrayList<>();
                    for (int i = 0; i < rankList.size(); i++) {
                        if (isRankUnlocked((OfflinePlayer) sender, args[1].toLowerCase(), i) == Rank.CompletionStatus.COMPLETE)
                            tab.add(ChatColor.stripColor(color(rankList.get(i).getName())));
                    }
                }
                if ((containsIgnoreCase("set", args[0]) || containsIgnoreCase("remove", args[0])) && sender.hasPermission("notranks.admin")) {
                    for (Map.Entry<String, List<Rank>> entry : ranks.entrySet()) {
                        tab.add(entry.getKey());
                    }
                    List<Rank> rankList = ranks.containsKey(args[1]) ? ranks.get(args[1]) : new ArrayList<>();
                    for (Rank rank : rankList)
                        tab.add(ChatColor.stripColor(color(rank.getName())));
                    if (containsIgnoreCase("set", args[0]))
                        tab.addAll(argumentAliases.get("none"));
                }
            } else if (args.length == 4) {
                if ((containsIgnoreCase("set", args[0]) || containsIgnoreCase("remove", args[0])) && sender.hasPermission("notranks.admin")) {
                    if (ranks.containsKey(args[2]))
                        for (Rank rank : ranks.get(args[2])) {
                            tab.add(ChatColor.stripColor(color(rank.getName())));
                        }
                    if (containsIgnoreCase("set", args[0]))
                        tab.addAll(argumentAliases.get("none"));
                }
            }


        } else if (command.getName().equalsIgnoreCase("notrankinfo") || command.getName().equalsIgnoreCase("notrankup")) {
            if (args.length == 1) {
                if (sender.hasPermission("notranks.default")) {
                    for (Map.Entry<String, List<Rank>> entry : ranks.entrySet()) {
                        tab.add(entry.getKey());
                    }
                    if (command.getName().equalsIgnoreCase("notrankup")) {
                        tab.addAll(argumentAliases.get("confirm"));
                    }
                }
            } else if (args.length == 2) {
                if (sender.hasPermission("notranks.default")) {
                    if (ranks.containsKey(args[0]))
                        for (Rank rank : ranks.get(args[0])) {
                            tab.add(ChatColor.stripColor(rank.getName()));
                        }
                }
            } else if (args.length == 3 && command.getName().equalsIgnoreCase("notrankup") && sender.hasPermission("notranks.default")) {
                tab.addAll(argumentAliases.get("none"));
            }
        }
        String typed = args[args.length - 1];
        tab.removeIf(test -> test.toLowerCase(Locale.ROOT).indexOf(typed.toLowerCase(Locale.ROOT)) != 0);
        Collections.sort(tab);
        return tab;
    }

    /**
     * Get a rank number from either a number as a string, or the name of the rank.
     * Note: if a number is used, the rank number returned will be 1 fewer
     * @param path Rank path
     * @param text Text to parse
     * @return rank number, -1 if no rank exists, or -2 if the rank is no-rank
     */
    public static int getRankNumFromText(String path, String text){
        List<Rank> validRanks = ranks.get(path);
        text = ChatColor.stripColor(color(text));

        int rankNum = -1;
        for (Rank rank : validRanks) {
            if (ChatColor.stripColor(color(rank.getName())).equalsIgnoreCase(text)) {
                rankNum = validRanks.indexOf(rank);
                break;
            }
        }
        if (rankNum == -1) {
            // cannot find rank from string
            if (containsIgnoreCase("none", text) || text.equalsIgnoreCase("0") || containsIgnoreCase("reset", text)) {
                // no rank
                return -2;
            } else {
                try {
                    rankNum = Integer.parseInt(text) - 1;
                } catch (NumberFormatException ignored) {
                    // not a number
                    return -1;
                }
                if (rankNum < 0 || rankNum >= validRanks.size()) {
                    // number out of bounds
                    return -1;
                }
            }
        }
        return rankNum;
    }
}
