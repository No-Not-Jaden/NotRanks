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

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("notranks.default") && !sender.hasPermission("notranks.admin")) {
            sender.sendMessage(parse(prefix + noAccess, (Player) sender));
            return true;
        }
        if (command.getName().equalsIgnoreCase("notrankup")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(parse(prefix + ChatColor.RED + "Only players can use this command!", null));
                return true;
            }

            String rankType = args.length > 0 && !argumentAliases.get("confirm").contains(args[0].toLowerCase()) ? args[0] : "default";
            List<Rank> rankPath = ranks.get(rankType);
            List<Integer> rankProgress = getRankCompletion((Player) sender, rankType);
            if (rankPath == null) {
                // unknown rank path
                sender.sendMessage(parse(prefix + unknownRankPath, (Player) sender));
                return true;
            }
            GUIOptions guiOptions = GUI.getGUI(rankType);
            // check if they have permission
            if (!sender.hasPermission("notranks." + rankType) && guiOptions.isPermissionRequired() && !sender.hasPermission("notranks.admin")) {
                sender.sendMessage(parse(prefix + noAccess, (Player) sender));
                return true;
            }
            // get next rank num, if none is specified, +1 last rank
            int nextRank = -1;
            if (args.length > 1 && !argumentAliases.get("confirm").contains(args[1].toLowerCase())) {
                nextRank = getRankNumFromText(rankType, args[1]);
            }
            if (nextRank < 0)
                // get next rank
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
                sender.sendMessage(parse(prefix + LanguageOptions.alreadyCompleted, (Player) sender));
                return true;
            }
            if (rankPath.size() == nextRank) {
                if (debug)
                    Bukkit.getLogger().info("[NotRanks] Rank path size: " + rankPath.size());
                // max rank
                sender.sendMessage(parse(prefix + maxRank, (Player) sender));
                return true;
            }
            if (getRankNum((Player) sender, rankType) + 1 != nextRank && GUI.getGUI(rankType).isOrderlyProgression()) {
                // cant skip ranks
                sender.sendMessage(parse(prefix + notOnRank, (Player) sender));
                return true;
            }
            if (rankPath.get(nextRank).checkUncompleted((Player) sender, rankType)) {
                // requirements not completed
                sender.sendMessage(parse(prefix + rankUpDeny, (Player) sender));
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                return true;
            }
            if (!confirmation || (args.length > 0 && argumentAliases.get("confirm").contains(args[args.length - 1].toLowerCase()))) {
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
                            sender.sendMessage(prefix + ChatColor.GREEN + "Reloaded NotRanks version " + NotRanks.getInstance().getDescription().getVersion() + ".");
                        } catch (IOException e) {
                            sender.sendMessage(prefix + ChatColor.RED + "Error loading the config.");
                            Bukkit.getLogger().warning(e.toString());
                        }
                    } else {
                        if (sender instanceof Player)
                            sender.sendMessage(parse(prefix + noAccess, (Player) sender));
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("prefix")) {
                    // /rank prefix <path/reset> <#/rank>
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
                    if (args[1].equalsIgnoreCase("reset")) {
                        // reset prefix
                        prefixSelections.remove(((Player) sender).getUniqueId());
                        sender.sendMessage(parse(prefix + prefixReset, (Player) sender));
                        return true;
                    }
                    String path = args[1];
                    if (!ranks.containsKey(path)) {
                        // no path found
                        sender.sendMessage(parse(prefix + unknownRankPath, (Player) sender));
                        return true;
                    }
                    if (args.length == 2){
                        // only path
                        prefixSelections.put(((Player) sender).getUniqueId(), "p:" + path);
                        sender.sendMessage(parse(prefix + prefixPath.replace("{path}", path), (Player) sender));
                        return true;
                    }
                    int rankNum = getRankNumFromText(path, ChatColor.stripColor(parse(args[2], (Player) sender)));

                    switch (rankNum){
                        case -2:
                            // unknown rank
                            sender.sendMessage(parse(prefix + unknownRank, (Player) sender));
                            break;
                        case -1:
                            // reset prefix
                            prefixSelections.remove(((Player) sender).getUniqueId());
                            sender.sendMessage(parse(prefix + prefixReset, (Player) sender));
                            break;
                        default:
                            if (isRankUnlocked((Player) sender, path, rankNum) != Rank.CompletionStatus.COMPLETE){
                                // haven't completed the rank
                                sender.sendMessage(parse(prefix + notOnRank, (Player) sender));
                                return true;
                            }
                            prefixSelections.put(((Player) sender).getUniqueId(), "r:" + rankNum + "p:" + path);
                            sender.sendMessage(parse(prefix + prefixRank.replace("{rank}", color(ranks.get(path).get(rankNum).getName())), (Player) sender));
                            break;
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
                            String rankArg = args.length == 4 ? args[3] : args[2];
                            if (!ranks.containsKey(path)) {
                                // no path found
                                sender.sendMessage(parse(prefix + unknownRankPath, (Player) sender));
                                return true;
                            }
                            int rankNum = getRankNumFromText(path, rankArg);

                            List<Integer> rankCompletion = getRankCompletion(player, path);
                            switch (rankNum){
                                case -2:
                                    // unknown rank
                                    sender.sendMessage(parse(prefix + unknownRank, null));
                                    break;
                                case -1:
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
                        sender.sendMessage(parse(prefix + noAccess, (Player) sender));
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("help")) {
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
                            String rankArg = args.length == 4 ? args[3] : args[2];
                            if (!ranks.containsKey(path)) {
                                // no path found
                                sender.sendMessage(parse(prefix + unknownRankPath, (Player) sender));
                                return true;
                            }
                            List<Rank> validRanks = ranks.get(path);
                            List<Integer> rankCompletion = getRankCompletion(player, path);
                            int rankNum = getRankNumFromText(path, rankArg);
                            if (rankNum < 0) {
                                sender.sendMessage(prefix + ChatColor.RED + "Unknown Rank");
                                return true;
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
                        if (sender instanceof Player)
                            sender.sendMessage(parse(prefix + noAccess, (Player) sender));
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
                sender.sendMessage(parse(prefix + unknownRankPath, (Player) sender));
                return true;
            }
            GUIOptions guiOptions = GUI.getGUI(rankType);
            // check if they have permission
            if (!sender.hasPermission("notranks." + rankType) && guiOptions.isPermissionRequired() && !sender.hasPermission("notranks.admin")) {
                sender.sendMessage(parse(prefix + noAccess, (Player) sender));
                return true;
            }
            GUI.openGUI((Player) sender, rankType, 1);
            sender.sendMessage(parse(prefix + guiOpen, (Player) sender));
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
                sender.sendMessage(parse(prefix + unknownRankPath, (Player) sender));
                return true;
            }
            GUIOptions guiOptions = GUI.getGUI(rankType);
            // check if they have permission
            if (!sender.hasPermission("notranks." + rankType) && guiOptions.isPermissionRequired() && !sender.hasPermission("notranks.admin")) {
                sender.sendMessage(parse(prefix + noAccess, (Player) sender));
                return true;
            }
            // get next rank num, if none is specified, +1 last rank
            int nextRank = -1;
            if (args.length > 1) {
                nextRank = getRankNumFromText(rankType, args[1]);
            }
            if (nextRank < 0)
                nextRank = rankProgress == null || rankProgress.isEmpty() ? 0 : rankProgress.get(rankProgress.size() - 1) + 1;

            if (rankPath.size() > nextRank) { // check if they are on the max rank
                // display the next rank
                Rank rank = rankPath.get(nextRank);
                List<String> chat = rank.getLore((Player) sender, Rank.CompletionStatus.INCOMPLETE);
                String name = parse(rank.getName(), (Player) sender);
                String fill = new String(new char[11]).replace("\0", " ");
                sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + fill + ChatColor.RESET + " " + name + ChatColor.RESET + " " + ChatColor.GRAY + ChatColor.STRIKETHROUGH + fill);

                for (String str : chat) {
                    sender.sendMessage(str);
                }
                StringBuilder str = new StringBuilder("                        ");
                int multiplier = name.contains(ChatColor.BOLD + "") ? 2 : 1;
                for (int i = 0; i < ChatColor.stripColor(name).length() * multiplier; i++) {
                    str.append(" ");
                }
                sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + str);
            } else {
                sender.sendMessage(parse(prefix + maxRank, (Player) sender));
            }
        }
        return true;
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
                    tab.addAll(ranks.keySet());
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("prefix") && sender.hasPermission("notranks.default")){
                    tab.addAll(argumentAliases.get("reset"));
                    tab.addAll(ranks.keySet());
                }
                if ((args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove")) && sender.hasPermission("notranks.admin")) {
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(tab::add);
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("prefix") && sender.hasPermission("notranks.default") && sender instanceof OfflinePlayer){
                    tab.addAll(getUnlockedRankAliases(args[1], (OfflinePlayer) sender));
                }
                if ((args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove")) && sender.hasPermission("notranks.admin")) {
                    tab.addAll(ranks.keySet());
                    tab.addAll(getRankAliases(args[1]));
                    if (args[0].equalsIgnoreCase("set"))
                        tab.addAll(argumentAliases.get("none"));
                }
            } else if (args.length == 4) {
                if ((args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove")) && sender.hasPermission("notranks.admin")) {
                    tab.addAll(getRankAliases(args[2]));
                    if (args[0].equalsIgnoreCase("set"))
                        tab.addAll(argumentAliases.get("none"));
                }
            }


        } else if (command.getName().equalsIgnoreCase("notrankinfo") || command.getName().equalsIgnoreCase("notrankup")) {
            if (args.length == 1) {
                if (sender.hasPermission("notranks.default")) {
                    tab.addAll(ranks.keySet());
                    if (command.getName().equalsIgnoreCase("notrankup")) {
                        tab.addAll(argumentAliases.get("confirm"));
                    }
                }
            } else if (args.length == 2) {
                if (sender.hasPermission("notranks.default")) {
                    tab.addAll(getRankAliases(args[0]));
                    if (command.getName().equalsIgnoreCase("notrankup")) {
                        tab.addAll(argumentAliases.get("confirm"));
                    }
                }
            } else if (args.length == 3 && command.getName().equalsIgnoreCase("notrankup") && sender.hasPermission("notranks.default")) {
                if (command.getName().equalsIgnoreCase("notrankup")) {
                    tab.addAll(argumentAliases.get("confirm"));
                }
            }
        }
        String typed = args[args.length - 1];
        tab.removeIf(test -> test.toLowerCase(Locale.ROOT).indexOf(typed.toLowerCase(Locale.ROOT)) != 0);
        Collections.sort(tab);
        return tab;
    }

    /**
     * Get the set of strings that represent ranks for a specified rank path.
     * This is to be used in commands.
     * @param path Rank Path.
     * @return A set of strings that would be used in a rank command.
     */
    private Set<String> getRankAliases(String path) {
        path = path.toLowerCase();
        Set<String> rankText = new HashSet<>();
        if (ranks.containsKey(path)) {
            for (Rank rank : ranks.get(path)) {
                rankText.add(ChatColor.stripColor(color(rank.getName())).replace(" ", "_"));
                rankText.add(rank.getConfigurationName());
            }
        }
        return rankText;
    }

    /**
     * Get the set of strings that represent ranks that the player has unlocked for a specified rank path.
     * This is to be used in commands.
     * @param path Rank Path.
     * @return A set of strings that would be used in a rank command.
     */
    private Set<String> getUnlockedRankAliases(String path, OfflinePlayer player) {
        path = path.toLowerCase();
        Set<String> rankText = new HashSet<>();
        if (ranks.containsKey(path)) {
            List<Rank> get = ranks.get(path);
            for (int i = 0; i < get.size(); i++) {
                Rank rank = get.get(i);
                if (isRankUnlocked(player, path, i) == Rank.CompletionStatus.COMPLETE) {
                    rankText.add(ChatColor.stripColor(color(rank.getName())).replace(" ", "_"));
                    rankText.add(rank.getConfigurationName());
                }
            }
        }
        return rankText;
    }

    /**
     * Get a rank number from either a number as a string, or the name of the rank.
     * Note: if a number is used, the rank number returned will be 1 fewer. Ex: rank 1 will return index 0.
     * @param path Rank path
     * @param text Text to parse
     * @return rank number, -1 if the rank is no-rank, or -2 if no rank exists
     */
    public static int getRankNumFromText(String path, String text){
        List<Rank> validRanks = ranks.get(path);
        text = ChatColor.stripColor(color(text));

        int rankNum;
        for (Rank rank : validRanks) {
            if (ChatColor.stripColor(color(rank.getName())).replace(" ", "_").equalsIgnoreCase(text) || rank.getConfigurationName().equalsIgnoreCase(text)) {
                return validRanks.indexOf(rank);
            }
        }
        // cannot find rank from string
        if (text.equalsIgnoreCase("none") || text.equalsIgnoreCase("0") || text.equalsIgnoreCase("reset")) {
            // no rank
            return -1;
        } else {
            try {
                rankNum = Integer.parseInt(text) - 1;
            } catch (NumberFormatException ignored) {
                // not a number
                return -2;
            }
            if (rankNum < 0 || rankNum >= validRanks.size()) {
                // number out of bounds
                return -2;
            }
        }
        return rankNum;
    }
}
