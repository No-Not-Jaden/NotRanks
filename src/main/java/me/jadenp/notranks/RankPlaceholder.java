package me.jadenp.notranks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import static me.jadenp.notranks.ConfigOptions.*;

public class RankPlaceholder extends PlaceholderExpansion {

    private final NotRanks plugin;

    public RankPlaceholder(NotRanks plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean persist(){
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
    public String onRequest(OfflinePlayer player, String identifier){

        // %notranks_rank%
        // %notranks_rank_number%
        // %notranks_requirement_<x>%
        // %notranks_rank_progress%
        // %notranks_rank_cost%
        if (identifier.equalsIgnoreCase("rank_number")) {
            return plugin.getRank(player) + "";
        } else if (identifier.equalsIgnoreCase("rank")){
            int rank = plugin.getRank(player) - 1;
            String rankName = rank != -1 ? ranks.get(rank).getName() : noRank;
            return plugin.parse(rankName, player);
        } else if (identifier.startsWith("requirement")){
            try {
                int rankNum = Integer.parseInt(identifier.substring(identifier.lastIndexOf("_") + 1));
                return plugin.parse(ranks.get(plugin.getRank(player)).getRequirementProgress(rankNum, player, (plugin.playerRank.get(player.getUniqueId().toString()) >= rankNum)), player);
            } catch (NumberFormatException | IndexOutOfBoundsException e){
                return "";
            }
        } else if (identifier.equalsIgnoreCase("rank_progress")){
            try {
                return plugin.parse(((int) (ranks.get(plugin.getRank(player)).getCompletionPercent(player) * 100)) + "", player);
            } catch (NumberFormatException | IndexOutOfBoundsException e){
                return "";
            }
        } else if (identifier.equalsIgnoreCase("rank_cost")){
            try {
                String strAmount = ((double) Math.round(ranks.get(plugin.getRank(player)).getCost() * Math.pow(10, decimals)) / Math.pow(10, decimals)) + "";
                if (decimals == 0) {
                    if (strAmount.contains("."))
                        strAmount = strAmount.substring(0, strAmount.indexOf("."));
                }
                return plugin.parse(strAmount, player);
            } catch (NumberFormatException | IndexOutOfBoundsException e){
                return "";
            }
        }


        return null;
    }
}
