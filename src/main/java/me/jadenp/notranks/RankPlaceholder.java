package me.jadenp.notranks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import static me.jadenp.notranks.ConfigOptions.*;

public class RankPlaceholder extends PlaceholderExpansion {

    private final NotRanks plugin;

    public RankPlaceholder(NotRanks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "notranks";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }


    @Override
    public String onRequest(OfflinePlayer player, String identifier) {

        // %notranks_rank_<path>%
        // %notranks_prefix%
        // %notranks_prefix_raw%
        // %notranks_rank_number_<path>%
        // %notranks_requirement_<x>_<path>%
        // %notranks_rank_progress_<path>%
        // %notranks_rank_progress_bar<length>_<path>%
        // %notranks_rank_cost_<path>%
        // %notranks_ranks_unlocked_<path>%
        if (identifier.startsWith("rank_number")) {
            if (identifier.equalsIgnoreCase("rank_number"))
                return RankManager.getRankNum(player, "default") + "";
            try {
                return RankManager.getRankNum(player, identifier.substring(identifier.lastIndexOf("_") + 1)) + "";
            } catch (IndexOutOfBoundsException e) {
                return "-1";
            }
        } else if (identifier.startsWith("ranks_unlocked")) {
            if (identifier.equals("ranks_unlocked"))
                return RankManager.getAllCompletedRanks(player).size() + "";

            String path;
            if (identifier.length() > 15)
                path = identifier.substring(15);
            else
                path = "default";
            return RankManager.getRankCompletion(player, path).size() + "";

        } else if (identifier.startsWith("rank") && !identifier.startsWith("rank_progress") && !identifier.startsWith("rank_cost")) {
            if (identifier.equalsIgnoreCase("rank")) {
                Rank rank = RankManager.getRank(player, "default");
                if (rank == null)
                    return LanguageOptions.parse(noRank, player);
                return LanguageOptions.parse(rank.getName(), player);
            }
            try {
                Rank rank = RankManager.getRank(player, identifier.substring(identifier.lastIndexOf("_") + 1));
                if (rank == null)
                    return LanguageOptions.parse(noRank, player);
                return LanguageOptions.parse(rank.getName(), player);
            } catch (IndexOutOfBoundsException e) {
                return LanguageOptions.parse(noRank, player);
            }
        } else if (identifier.startsWith("requirement")) {
            String parameters = identifier.substring(identifier.indexOf("_") + 1);
            try {
                int reqNum;
                String path;
                if (parameters.contains("_")) {
                    // has rank number and path
                    reqNum = Integer.parseInt(parameters.substring(0, parameters.indexOf("_")));
                    path = parameters.substring(parameters.indexOf("_") + 1);
                } else {
                    reqNum = Integer.parseInt(parameters);
                    path = "default";
                }
                int nextRank = RankManager.getRankNum(player, path) + 1;
                Rank rank = RankManager.getRank(nextRank, path);
                if (rank == null)
                    return "";
                return LanguageOptions.parse(rank.getRequirementProgress(reqNum, player, false), player);
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                return "";
            }
        } else if (identifier.startsWith("rank_progress")) {
            return parseProgress(identifier, player);
        } else if (identifier.startsWith("rank_cost")) {
            try {
                String path;
                if (identifier.length() > 14) {
                    // has path
                    path = identifier.substring(identifier.lastIndexOf("_") + 1);
                } else {
                    path = "default";
                }
                int nextRank = RankManager.getRankNum(player, path) + 1;
                Rank rank = RankManager.getRank(nextRank, path);
                if (rank == null)
                    return "";
                String strAmount = (rank.getCost() * Math.pow(10, decimals)) / Math.pow(10, decimals) + "";
                if (decimals == 0) {
                    if (strAmount.contains("."))
                        strAmount = strAmount.substring(0, strAmount.indexOf("."));
                }
                return LanguageOptions.parse(strAmount, player);
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                return "";
            }
        } else if (identifier.startsWith("prefix")) {
            Rank rank = RankManager.getPrefixRank(player);
            String prefix = rank == null ? noRank : rank.getPrefix();
            if (identifier.equalsIgnoreCase("prefix_raw")) {
                return prefix;
            } else {
                return LanguageOptions.parse(prefix, player);
            }
        }


        return null;
    }

    private String parseProgress(String identifier, OfflinePlayer player) {
        // %notranks_rank_progress<_bar(x)_<path>/_<path>>%
        if (identifier.startsWith("rank_progress_bar")) {
            int barLength;
            String path;
            if (identifier.length() == 17) {
                barLength = 20;
                path = "default";
            } else if (identifier.substring(17).contains("_")) {
                String lengthString = identifier.substring(17, identifier.indexOf("_", 17));
                path = identifier.substring(18 + lengthString.length());
                barLength = Integer.parseInt(lengthString);
            } else {
                String lengthString = identifier.substring(17);
                path = "default";
                barLength = Integer.parseInt(lengthString);
            }
            int nextRank = RankManager.getRankNum(player, path) + 1;
            Rank rank = RankManager.getRank(nextRank, path);
            if (rank == null)
                return "";
            float progress = rank.getCompletionPercent(player);
            return generateProgressBar(progress, barLength);
        } else {
            String path = identifier.length() <= 14 ? "default" : identifier.substring(14);
            int nextRank = RankManager.getRankNum(player, path) + 1;
            Rank rank = RankManager.getRank(nextRank, path);
            if (rank == null)
                return "";
            return (int) (rank.getCompletionPercent(player) * 100) + "";
        }
    }


    /**
     * Generate a progress bar of '|' characters.
     * @param percent Percentage represented on the progress bar. (0-1)
     * @param length Number of characters used in the progress bar.
     * @return A progress bar representing the percent.
     */
    private String generateProgressBar(double percent, int length) {
        StringBuilder progressBar = new StringBuilder();
        percent = Math.min(percent, 1);
        int greenBars = (int) (percent * length);
        int grayBars = length - greenBars;
        if (greenBars > 0) {
            progressBar.append(ChatColor.GREEN);
            for (int i = 0; i < greenBars; i++) {
                progressBar.append("|");
            }
        }
        if (grayBars > 0) {
            progressBar.append(ChatColor.DARK_GRAY);
            for (int i = 0; i < grayBars; i++) {
                progressBar.append("|");
            }
        }
        return progressBar.toString();
    }
}
