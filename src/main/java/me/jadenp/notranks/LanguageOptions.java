package me.jadenp.notranks;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

public class LanguageOptions {

    public static String guiName;
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

    public static void loadConfig() throws IOException {
        File language = new File(NotRanks.getInstance().getDataFolder() + File.separator + "language.yml");

        if (!language.exists()){
            NotRanks.getInstance().saveResource("language.yml", false);
        }

        YamlConfiguration langConf = YamlConfiguration.loadConfiguration(language);
        if (!langConf.isSet("gui-name"))
            langConf.set("gui-name", "&cRanks");
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
            langConf.set("complete-requirement", "&eYou've completed a requirement towards your next rank! &f/rank &7to view your progress.");
        if (!langConf.isSet("complete-rank"))
            langConf.set("complete-rank", "&eA new rank is now available!");
        if (!langConf.isSet("prefix"))
            langConf.set("prefix", "&7[&cRanks&7] &8> &r");
        if (!langConf.isSet("max-rank"))
            langConf.set("max-rank", "&cYou are already at the max rank!");

        langConf.save(language);


        // 0
        guiName = langConf.getString("gui-name");
        // 1
        rankUp = langConf.getString("rankup");
        // 2
        rankUpDeny = langConf.getString("rankup-deny");
        // 3
        unknownCommand = langConf.getString("unknown-command");
        // 4
        noAccess = langConf.getString("no-access");
        // 5
        guiOpen = langConf.getString("open-gui");
        // 6
        notOnRank = langConf.getString("not-on-rank");
        // 7
        completeRequirement = langConf.getString("complete-requirement");
        // 8
        completeRank = langConf.getString("complete-rank");
        // 9
        prefix = color(langConf.getString("prefix"));
        //10
        maxRank = langConf.getString("max-rank");


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
