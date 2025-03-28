package me.jadenp.notranks;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
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
    public static List<String> prefixLore;

    public static void loadConfig() throws IOException {
        File language = new File(NotRanks.getInstance().getDataFolder() + File.separator + "language.yml");

        if (!language.exists()){
            NotRanks.getInstance().saveResource("language.yml", false);
        }

        YamlConfiguration langConf = YamlConfiguration.loadConfiguration(language);
        // fill in any default options that aren't present
        langConf.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NotRanks.getInstance().getResource("language.yml")))));
        for (String key : Objects.requireNonNull(langConf.getDefaults()).getKeys(true)) {
            if (!langConf.isSet(key))
                langConf.set(key, langConf.getDefaults().get(key));
        }

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

        prefixLore = langConf.getStringList("prefix-lore");

    }

    public static String parse(String str, OfflinePlayer player){
        str = color(str);
        if (ConfigOptions.papiEnabled)
            return new PlaceholderAPIClass().parse(player, str);
        return str;
    }

    public static String color(String str){
        str = ChatColor.translateAlternateColorCodes('&', str);
        return cancelColorCodes("</#", ">", translateHexColorCodes("<#", ">", translateHexColorCodes("&#","", str)));
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
    public static String cancelColorCodes(String startTag, String endTag, String message)
    {
        final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find())
        {
            matcher.appendReplacement(buffer, org.bukkit.ChatColor.RESET + ""
            );
        }
        return matcher.appendTail(buffer).toString();
    }
}
