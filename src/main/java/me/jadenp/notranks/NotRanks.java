package me.jadenp.notranks;


import me.jadenp.notranks.gui.GUI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

import static me.jadenp.notranks.ConfigOptions.*;
import static me.jadenp.notranks.LanguageOptions.*;

/**
 * Specific commands for specific requirements
 */

public final class NotRanks extends JavaPlugin implements CommandExecutor, Listener {

    private static File logsFolder;
    private static final Date now = new Date();
    private static final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
    private static final SimpleDateFormat formatExact = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private static File today;
    private static final List<String> logs = new ArrayList<>();

    public static int serverVersion = 20;
    public static int serverSubVersion = 0;

    private static boolean debug = false;

    private static NotRanks instance;

    public static NotRanks getInstance() {
        return instance;
    }

    public static void setInstance(NotRanks instance) {
        NotRanks.instance = instance;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        setInstance(this);
        // try to get the server version
        try {
            // get the text version - ex: 1.20.3
            String fullServerVersion = Bukkit.getBukkitVersion().substring(0, Bukkit.getBukkitVersion().indexOf("-"));
            fullServerVersion = fullServerVersion.substring(2); // remove the '1.' in the version
            if (fullServerVersion.contains(".")) {
                // get the subversion - ex: 3
                serverSubVersion = Integer.parseInt(fullServerVersion.substring(fullServerVersion.indexOf(".") + 1));
                fullServerVersion = fullServerVersion.substring(0, fullServerVersion.indexOf(".")); // remove the subversion
            }
            serverVersion = Integer.parseInt(fullServerVersion);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            Bukkit.getLogger().warning("[NotRanks] Could not get the server version. Some features may not function properly.");
            serverVersion = 20;
            serverSubVersion = 0;
        }

        Commands commands = new Commands();
        Objects.requireNonNull(getCommand("notranks")).setExecutor(commands);
        Objects.requireNonNull(getCommand("notrankup")).setExecutor(commands);
        Objects.requireNonNull(getCommand("notrankinfo")).setExecutor(commands);
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getServer().getPluginManager().registerEvents(new GUI(), this);
        // create logs stuff
        logsFolder = new File(this.getDataFolder() + File.separator + "logs");
        today = new File(logsFolder + File.separator + format.format(now) + ".txt");
        //noinspection ResultOfMethodCallIgnored
        logsFolder.mkdir();
        try {
            if (!today.createNewFile()) {
                    Scanner scanner = new Scanner(today);
                    while (scanner.hasNextLine()) {
                        String data = scanner.nextLine();
                        logs.add(data);
                    }
                    scanner.close();

            }
        } catch (IOException e) {
            Bukkit.getLogger().warning(e.toString());
        }

        try {
            ConfigOptions.loadConfig(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        RankManager.readPlayerData(this);

        if (papiEnabled)
            new RankPlaceholder(this).register();

        new BukkitRunnable() {
            @Override
            public void run() {
                log();
            }
        }.runTaskTimerAsynchronously(this, 5000, 5000);
        logs.add("[" + formatExact.format(now) + "] Plugin Loaded!");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        logs.add("[" + formatExact.format(now) + "] Plugin disabling.");
        try {
            RankManager.saveRanks(this);
        } catch (IOException e) {
            Bukkit.getLogger().warning(e.toString());
        }
        log();
    }


    public void rankup(Player p, String rankType, int newRankIndex) {
        if (debug) {
            Bukkit.getLogger().info("[NotRanks] Ranking player up...");
        }
        Rank newRank = RankManager.getRank(newRankIndex, rankType);
        if (newRank == null){
            Bukkit.getLogger().warning("[NotRanks] " + p.getName() + " is trying to rankup to a rank that doesn't exist! " + rankType + ":" + newRankIndex + "\nThis is a bug. Please contact the developer Not_Jaden.");
            return;
        }
        RankupEvent event = new RankupEvent(p, newRank, RankManager.getRank(p, rankType), newRankIndex);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            if (!rankUp.isEmpty()) {
                String text = rankUp;
                text = text.replace("{player}", p.getName());
                text = text.replace("{rank}", newRank.getName());

                Bukkit.broadcastMessage(parse(prefix + text, p));
            }
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
            newRank.rankup(p);
            logs.add("[" + formatExact.format(now) + "] " + p.getName() + " ranked up to " + newRank.getName() + ".");
            RankManager.addRank(p.getUniqueId(), rankType, newRankIndex);
            RankManager.setLastRankPathUsed(p.getUniqueId(), rankType);
        } else if (debug) {
            Bukkit.getLogger().info("[NotRanks] Event was canceled.");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // check auto rankup
        for (String path : RankManager.getAllRankPaths()) {
            if (RankManager.isAutoRankup(path)) {
                int rankNum = RankManager.getRankNum(event.getPlayer(), path);
                Rank rank = RankManager.getRank(rankNum + 1, path);
                if (rank != null && !rank.checkUncompleted(event.getPlayer(), path)) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            NotRanks.getInstance().rankup(event.getPlayer(), path, RankManager.getRankNum(rank));
                        }
                    }.runTaskLater(NotRanks.getInstance(), 5);
                }
            }
        }
    }




    // control chat if enabled
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!prefixEnabled || !prefixModifyChat)
            return;
        String parsedPrefix = prefixFormat;
        Rank rank = RankManager.getPrefixRank(event.getPlayer());
        String rankName = rank != null ? rank.getPrefix() : noRank;
        parsedPrefix = parsedPrefix.replace("{prefix}", rankName);
        parsedPrefix = parsedPrefix.replace("{name}", "%s");
        if (overwritePrefix)
            parsedPrefix += "%s";
        parsedPrefix = parse(parsedPrefix, event.getPlayer()) + ChatColor.WHITE;
        if (overwritePrefix) {
            event.setFormat(parsedPrefix);
        } else {
            event.setFormat(parsedPrefix + event.getFormat());
        }
    }


    public static void log() {
        try {
            PrintWriter writer = new PrintWriter(today.getPath(), "UTF-8");
            for (String s : logs) {
                writer.println(s);
            }
            writer.close();
        } catch (IOException e) {
            // do something
        }
    }

    public static void writeLog(String text) {
        logs.add("[" + formatExact.format(now) + "] " + text);
    }

    /**
     * Returns if the server version is above the specified version
     * @param majorVersion Major version of the server. In 1.20.4, the major version is 20
     * @param subVersion Sub version of the server. In 1.20.4, the sub version is 4
     * @return True if the current server version is higher than the specified one
     */
    public static boolean isAboveVersion(int majorVersion, int subVersion) {
        return serverVersion > majorVersion || (majorVersion == serverVersion && subVersion < serverSubVersion);
    }

    public static void debugMessage(String message, boolean warning) {
        if (!debug)
            return;
        message = "[NotRanksDebug] " + message;
        NotRanks notRanks = NotRanks.getInstance();
        if (notRanks.isEnabled()) {
            String finalMessage = message;
            new BukkitRunnable() {
                @Override
                public void run() {
                    consoleMessage(finalMessage, warning);
                }
            }.runTask(notRanks);
        } else {
            consoleMessage(message, warning);
        }
    }

    private static void consoleMessage(String message, boolean warning) {
        if (warning)
            Bukkit.getLogger().warning(message);
        else
            Bukkit.getLogger().info(message);
    }

    public static void setDebug(boolean debug) {
        NotRanks.debug = debug;
    }

    public static boolean isDebug() {
        return debug;
    }
}
