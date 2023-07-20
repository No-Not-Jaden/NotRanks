package me.jadenp.notranks;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

public class LanguageOptions {
    public static String rankUp;
    public static String rankUpDeny;
    public static String unknownCommand;
    public static String noAccess;
    public static String guiOpen;
    public static String notOnRank;
    public static String completeRequirement;
    public static String completeRank;
    public static String prefix;
    public static String maxRank;
    public static String unknownRankPath;
    public static String alreadyCompleted;
    public static String unknownRank;
    public static String prefixReset;
    public static String prefixPath;
    public static String prefixRank;

    public static void loadConfig() throws IOException {
        File language = new File(NotRanks.getInstance().getDataFolder() + File.separator + "language.yml");

        if (!language.exists()){
            NotRanks.getInstance().saveResource("language.yml", false);
        }

        YamlConfiguration langConf = YamlConfiguration.loadConfiguration(language);
        if (!langConf.isSet("rankup"))
            langConf.set("rankup", "&e{player} ranked up to {rank}!'");
        if (!langConf.isSet("rankup-deny"))
            langConf.set("rankup-deny", "&cYou do not meet the requirements to rankup!");
        if (!langConf.isSet("unknown-command"))
            langConf.set("unknown-command", "&cUnknown Command!");
        if (!langConf.isSet("no-access"))
            langConf.set("no-access", "&cYou do not have access to this command!");
        if (!langConf.isSet("open-gui"))
            langConf.set("open-gui", "Opening ranks menu.");
        if (!langConf.isSet("not-on-rank"))
            langConf.set("not-on-rank", "&cYou are not on this rank!");
        if (!langConf.isSet("complete-requirement"))
            langConf.set("complete-requirement", "&eYou've completed a requirement towards your next rank on path {path}! &f/rank {path} &7to view your progress.");
        if (!langConf.isSet("complete-rank"))
            langConf.set("complete-rank", "&eA new rank is now available on path {path}!");
        if (!langConf.isSet("prefix"))
            langConf.set("prefix", "&7[&cRanks&7] &8> &r");
        if (!langConf.isSet("max-rank"))
            langConf.set("max-rank", "&cYou are already at the max rank!");
        if (!langConf.isSet("unknown-rank-path"))
            langConf.set("unknown-rank-path", "&cUnknown rank path!");
        if (!langConf.isSet("already-completed"))
            langConf.set("already-completed", "&aYou have already completed this rank!");
        if (!langConf.isSet("unknown-rank"))
            langConf.set("unknown-rank", "&cUnknown rank!");
        if (!langConf.isSet("prefix-reset"))
            langConf.set("prefix-reset", "&fYour prefix has been reset.");
        if (!langConf.isSet("prefix-path"))
            langConf.set("prefix-path", "&aYour prefix has been set to follow the path &2{path}");
        if (!langConf.isSet("prefix-rank"))
            langConf.set("prefix-rank", "&aYour prefix has been set to &2{rank}");

        langConf.save(language);

        rankUp = langConf.getString("rankup");

        rankUpDeny = langConf.getString("rankup-deny");

        unknownCommand = langConf.getString("unknown-command");

        noAccess = langConf.getString("no-access");

        guiOpen = langConf.getString("open-gui");

        notOnRank = langConf.getString("not-on-rank");

        completeRequirement = langConf.getString("complete-requirement");

        completeRank = langConf.getString("complete-rank");

        prefix = color(langConf.getString("prefix"));

        maxRank = langConf.getString("max-rank");

        unknownRankPath = langConf.getString("unknown-rank-path");

        alreadyCompleted = langConf.getString("already-completed");

        unknownRank = langConf.getString("unknown-rank");

        prefixReset = langConf.getString("prefix-reset");

        prefixPath = langConf.getString("prefix-path");

        prefixRank = langConf.getString("prefix-rank");

    }

    public static String parse(String str, OfflinePlayer player){
        return PlaceholderAPI.setPlaceholders(player, color(str));
    }

    public static String color(String str){
        str = ChatColor.translateAlternateColorCodes('&', str);
        return translateHexColorCodes("&#","", str);
    }
    public static String translateHexColorCodes(String startTag, String endTag, String message)
    {
        final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find())
        {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
    }
}
