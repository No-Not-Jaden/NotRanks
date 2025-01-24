package me.jadenp.notranks;

import io.lumine.mythic.bukkit.utils.lib.jooq.impl.QOM;
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
            List<Rank> rankPath = RankManager.getRanks(rankType);
            List<Integer> rankProgress = RankManager.getRankCompletion((Player) sender, rankType);
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
                NotRanks.debugMessage("Player has a rank that doesn't exist! Did you reduce the amount of ranks? Allowing player to rankup to first rank.", false);
                nextRank = 0;
            }

            NotRanks.debugMessage("Attempting to rankup " + sender.getName() + " to " + rankType + ":" + nextRank, false);
            if (RankManager.isRankUnlocked((Player) sender, rankType, nextRank) == Rank.CompletionStatus.COMPLETE) {
                // rank already unlocked
                sender.sendMessage(parse(prefix + LanguageOptions.alreadyCompleted, (Player) sender));
                return true;
            }
            if (rankPath.size() == nextRank) {
                NotRanks.debugMessage("Rank path size: " + rankPath.size(), false);
                // max rank
                sender.sendMessage(parse(prefix + maxRank, (Player) sender));
                return true;
            }
            if (RankManager.getRankNum((Player) sender, rankType) + 1 != nextRank && GUI.getGUI(rankType).isOrderlyProgression()) {
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
                GUI.openGUI((Player) sender, "confirmation", 1, RankManager.getRankFormat(nextRank, rankType));
            }
        } else if (command.getName().equalsIgnoreCase("notranks")) {
            if (args.length > 0)
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("notranks.admin")) {
                        try {
                            ConfigOptions.loadConfig(NotRanks.getInstance());
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
                } else if (prefixEnabled && args[0].equalsIgnoreCase("prefix") && sender instanceof Player) {
                    // /rank prefix <path/reset> <#/rank>
                    NotRanks.debugMessage("Prefix string for " + sender.getName() + " is \"" + RankManager.getPrefixSelection(((Player) sender).getUniqueId()) + "\"", false);
                    if (args.length == 1) {
                        // open prefix gui
                        GUI.openGUI((Player) sender, "choose-prefix", 1);
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("reset")) {
                        // reset prefix
                        RankManager.removePrefix(((Player) sender).getUniqueId());
                        sender.sendMessage(parse(prefix + prefixReset, (Player) sender));
                        return true;
                    }
                    String path = args[1].toLowerCase();
                    if (!RankManager.isRankPath(path)) {
                        // no path found
                        sender.sendMessage(parse(prefix + unknownRankPath, (Player) sender));
                        return true;
                    }
                    if (args.length == 2){
                        // only path
                        RankManager.setPrefix(((Player) sender).getUniqueId(), path);
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
                            RankManager.removePrefix(((Player) sender).getUniqueId());
                            sender.sendMessage(parse(prefix + prefixReset, (Player) sender));
                            break;
                        default:
                            if (RankManager.isRankUnlocked((Player) sender, path, rankNum) != Rank.CompletionStatus.COMPLETE){
                                // haven't completed the rank
                                sender.sendMessage(parse(prefix + notOnRank, (Player) sender));
                                return true;
                            }
                            RankManager.setPrefix(((Player) sender).getUniqueId(), path, rankNum);
                            sender.sendMessage(parse(prefix + prefixRank.replace("{rank}", color(RankManager.getRank(rankNum, path).getName())), (Player) sender));
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
                            String path = args.length == 4 ? args[2].toLowerCase() : "default";
                            String rankArg = args.length == 4 ? args[3] : args[2];
                            if (!RankManager.isRankPath(path)) {
                                // no path found
                                sender.sendMessage(parse(prefix + unknownRankPath, (Player) sender));
                                return true;
                            }
                            int rankNum = getRankNumFromText(path, rankArg);

                            switch (rankNum){
                                case -2:
                                    // unknown rank
                                    sender.sendMessage(parse(prefix + unknownRank, null));
                                    break;
                                case -1:
                                    // clear ranks
                                    if (RankManager.removeRank(player.getUniqueId(), path))
                                        sender.sendMessage(prefix + ChatColor.GREEN + "Cleared " + player.getName() + "'s rank progress on " + ChatColor.GRAY + path + ChatColor.GREEN + ".");
                                    else
                                        sender.sendMessage(prefix + ChatColor.RED + player.getName() + " doesn't have any ranks on " + ChatColor.GRAY + path + ChatColor.RED + ".");
                                    break;
                                default:
                                    // put this rank number at the top of the rank completion list
                                    RankManager.removeRank(player.getUniqueId(), path, rankNum);
                                    RankManager.addRank(player.getUniqueId(), path, rankNum);
                                    sender.sendMessage(prefix + ChatColor.GREEN + "Set " + player.getName() + "'s rank to " + parse(Objects.requireNonNull(RankManager.getRank(rankNum, path)).getName(), player) + ChatColor.GREEN + ".");
                                    break;
                            }
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
                    NotRanks.setDebug(!NotRanks.isDebug());
                    sender.sendMessage(prefix + ChatColor.YELLOW + "Debug mode is now set to: " + NotRanks.isDebug());
                    if (NotRanks.isDebug())
                        sender.sendMessage(prefix + ChatColor.YELLOW + "Do" + ChatColor.WHITE + " /ranks reload" + ChatColor.YELLOW + " to check the ranks and gui.");
                    return true;
                } else if (args[0].equalsIgnoreCase("remove")) {
                    // /rank remove (player) <path> <rank/#>
                    if (sender.hasPermission("notranks.admin")) {
                        if (args.length > 1) {
                            Player player = Bukkit.getPlayer(args[1]);
                            if (player == null) {
                                sender.sendMessage(prefix + ChatColor.RED + "Unknown Player.");
                                return true;
                            }
                            String path = null; // null path means remove all ranks in every path
                            String rankArg = null; // null rank arg means remove all ranks in path
                            if (args.length > 2) {
                                if (RankManager.isRankPath(args[2])) {
                                    path = args[2].toLowerCase();
                                    if (args.length > 3)
                                        rankArg = args[3];
                                } else {
                                    rankArg = args[2];
                                    if (args.length > 3)
                                        path = args[3];
                                }
                            }
                            if (rankArg != null && path == null)
                                // specified a rank argument, so they must mean the default rank
                                // if rankArg is not null, then path must not be null
                                path = "default";
                            if (path == null) {
                                // remove all ranks
                                if (RankManager.removeRank(player.getUniqueId())) {
                                    sender.sendMessage(prefix + ChatColor.GREEN + "Removed " + player.getName() + "'s ranks.");
                                } else {
                                    sender.sendMessage(prefix + ChatColor.RED + player.getName() + " does not have any ranks.");
                                }
                                return true;
                            }
                            if (!RankManager.isRankPath(path)) {
                                // no path found
                                sender.sendMessage(parse(prefix + unknownRankPath, (Player) sender));
                                return true;
                            }
                            if (rankArg == null) {
                                // remove all ranks in path
                                if (RankManager.removeRank(player.getUniqueId(), path)) {
                                    sender.sendMessage(prefix + ChatColor.GREEN + "Removed " + player.getName() + "'s ranks on " + path + ".");
                                } else {
                                    sender.sendMessage(prefix + ChatColor.RED + player.getName() + " does not have any ranks on " + path + ".");
                                }
                                return true;
                            }
                            // remove a specific rank
                            int rankNum = getRankNumFromText(path, rankArg);
                            if (rankNum < 0) {
                                sender.sendMessage(prefix + ChatColor.RED + "Unknown Rank");
                                return true;
                            }
                            Rank rank = RankManager.getRank(rankNum, path);
                            if (rank != null && RankManager.removeRank(player.getUniqueId(), path,  rankNum)) {
                                sender.sendMessage(prefix + ChatColor.GREEN + "Removed " + player.getName() + "'s rank of " + rank.getName() + ChatColor.GREEN + " on " + ChatColor.GRAY + path + ChatColor.GREEN + ".");
                            } else {
                                sender.sendMessage(prefix + ChatColor.RED + player.getName() + " does not have the rank of " + rankArg + ChatColor.RED + ".");
                            }
                        } else {
                            sender.sendMessage(prefix + ChatColor.GOLD + ChatColor.BOLD + "Usage: " + ChatColor.YELLOW + "/ranks remove (player) <path> <#/rank>");
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
            if (!RankManager.isRankPath(rankType)) {
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
            List<Rank> rankPath = RankManager.getRanks(rankType);
            List<Integer> rankProgress = RankManager.getRankCompletion((Player) sender, rankType);
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
                    if (prefixEnabled)
                        tab.addAll(argumentAliases.get("prefix"));
                    tab.addAll(RankManager.getAllRankPaths());
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("prefix") && sender.hasPermission("notranks.default")){
                    tab.addAll(argumentAliases.get("reset"));
                    tab.addAll(RankManager.getAllRankPaths());
                }
                if ((args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove")) && sender.hasPermission("notranks.admin")) {
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(tab::add);
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("prefix") && sender.hasPermission("notranks.default") && sender instanceof OfflinePlayer){
                    tab.addAll(getUnlockedRankAliases(args[1], (OfflinePlayer) sender));
                }
                if ((args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove")) && sender.hasPermission("notranks.admin")) {
                    tab.addAll(RankManager.getAllRankPaths());
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
                    tab.addAll(RankManager.getAllRankPaths());
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
        if (RankManager.isRankPath(path)) {
            for (Rank rank : RankManager.getRanks(path)) {
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
        if (RankManager.isRankPath(path)) {
            List<Rank> get = RankManager.getRanks(path);
            for (int i = 0; i < get.size(); i++) {
                Rank rank = get.get(i);
                if (RankManager.isRankUnlocked(player, path, i) == Rank.CompletionStatus.COMPLETE) {
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
        List<Rank> validRanks = RankManager.getRanks(path);
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
