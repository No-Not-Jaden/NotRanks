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
        // %notranks_requirement_progress_<x>%
        if (identifier.equalsIgnoreCase("rank_number")) {
            return plugin.getRank(player) + "";
        } else if (identifier.equalsIgnoreCase("rank")){
            return ChatColor.translateAlternateColorCodes('&', ConfigOptions.ranks.get(plugin.getRank(player)).getName());
        } else {
            int req1 = Integer.parseInt(identifier.substring(identifier.lastIndexOf("_") + 1));
            if (identifier.substring(0, identifier.lastIndexOf("_")).equalsIgnoreCase("requirement_progress")){
                int req;
                try {
                    req = req1;
                } catch (NumberFormatException | IndexOutOfBoundsException ignored){
                    return "";
                }
                String requirement = ConfigOptions.ranks.get(plugin.getRank(player)).getRequirements().get(req);
                return PlaceholderAPI.setPlaceholders(player, requirement.substring(0, requirement.indexOf(" ")));
            } else if (identifier.substring(0, identifier.lastIndexOf("_")).equalsIgnoreCase("requirement")){
                int req;
                try {
                    req = req1;
                } catch (NumberFormatException | IndexOutOfBoundsException ignored){
                    return "";
                }
                String requirement = ConfigOptions.ranks.get(plugin.getRank(player)).getRequirements().get(req);
                String placeholder = requirement.substring(0, requirement.indexOf(" "));
                String value = requirement.substring(requirement.indexOf(" ") + 1);
                String parsed = PlaceholderAPI.setPlaceholders(player, placeholder);
                if (ConfigOptions.ranks.get(plugin.getRank(player)).isRequirementCompleted(requirement, player)){
                    return net.md_5.bungee.api.ChatColor.YELLOW + parsed + net.md_5.bungee.api.ChatColor.DARK_GRAY + " / " + net.md_5.bungee.api.ChatColor.RED + value;
                } else {
                    return net.md_5.bungee.api.ChatColor.GREEN + value + net.md_5.bungee.api.ChatColor.DARK_GRAY + "" + net.md_5.bungee.api.ChatColor.STRIKETHROUGH + " / " + net.md_5.bungee.api.ChatColor.GREEN + value;
                }
            }
        }

        return null;
    }
}
