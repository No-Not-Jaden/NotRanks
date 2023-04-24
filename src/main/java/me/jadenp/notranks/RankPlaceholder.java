package me.jadenp.notranks;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import static me.jadenp.notranks.ConfigOptions.noRank;
import static me.jadenp.notranks.ConfigOptions.ranks;

public class RankPlaceholder extends PlaceholderExpansion {

    private NotRanks plugin;

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
        // %notranks_requirement_progress%
        if (identifier.equalsIgnoreCase("rank_number")) {
            return plugin.getRank(player) + "";
        } else if (identifier.equalsIgnoreCase("rank")){
            int rank = plugin.getRank(player) - 1;
            String rankName = rank != -1 ? ranks.get(rank).getName() : noRank;
            return plugin.parse(rankName, player);
        } else if (identifier.startsWith("requirement")){
            try {
                return plugin.parse(ranks.get(plugin.getRank(player)).getRequirements().get(Integer.parseInt(identifier.substring(identifier.lastIndexOf("_") + 1))), player);
            } catch (NumberFormatException | IndexOutOfBoundsException e){
                return "";
            }
        } else if (identifier.equalsIgnoreCase("requirement_progress")){
            try {
                return plugin.parse(((int) (ranks.get(plugin.getRank(player)).getCompletionPercent(player) * 100)) + "", player);
            } catch (NumberFormatException | IndexOutOfBoundsException e){
                return "";
            }
        }


        return null;
    }
}
