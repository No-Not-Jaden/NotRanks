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

    public final File playerdata = new File(this.getDataFolder() + File.separator + "playerdata.yml");
    public final File logsFolder = new File(this.getDataFolder() + File.separator + "logs");
    final Date now = new Date();
    final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
    final SimpleDateFormat formatExact = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    final File today = new File(logsFolder + File.separator + format.format(now) + ".txt");
    public final ArrayList<String> logs = new ArrayList<>();

    public static int serverVersion = 20;
    public static int serverSubVersion = 0;
    public static NotRanks instance;


    public static NotRanks getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
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
        // creating file to store player's ranks if the file hadn't already been created
        try {
            if (playerdata.createNewFile()) {
                Bukkit.getLogger().info("[NotRanks] Creating a new player data file.");
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning(e.toString());
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(playerdata);
        if (!configuration.isSet("version")) {
            // load old config
            for (int i = 1; configuration.isSet(i + ".uuid"); i++) {
                List<Integer> completedRanks = new ArrayList<>();
                int currentRank = configuration.getInt(i + ".rank");
                for (int j = 0; j < currentRank; j++) {
                    completedRanks.add(i);
                }
                Map<String, List<Integer>> completedRank = new HashMap<>();
                completedRank.put("default", completedRanks);
                rankData.put(UUID.fromString(Objects.requireNonNull(configuration.getString(i + ".uuid"))), completedRank);
            }
        } else {
            // load new config
            if (configuration.isConfigurationSection("data")) {
                for (String uuid : Objects.requireNonNull(configuration.getConfigurationSection("data")).getKeys(false)) {
                    Map<String, List<Integer>> playerRankInfo = new HashMap<>();
                    for (String rankType : Objects.requireNonNull(configuration.getConfigurationSection("data." + uuid)).getKeys(false)) {
                        String completedRanks = configuration.getString("data." + uuid + "." + rankType);
                        assert completedRanks != null;
                        // completed ranks will be prefix selection or last rank path if that is the rankType
                        if (rankType.equalsIgnoreCase("prefix")){
                            prefixSelections.put(UUID.fromString(uuid), completedRanks);
                            continue;
                        }
                        if (rankType.equalsIgnoreCase("last-path")){
                            lastRankPathUsed.put(UUID.fromString(uuid), completedRanks);
                            continue;
                        }
                        String[] separatedRanks = completedRanks.split(",");
                        List<Integer> rankList = new ArrayList<>();
                        for (String separatedRank : separatedRanks) {
                            rankList.add(Integer.parseInt(separatedRank));
                        }
                        playerRankInfo.put(rankType, rankList);
                    }
                    rankData.put(UUID.fromString(uuid), playerRankInfo);
                }
            }
        }
        try {
            this.loadConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (papiEnabled)
            new RankPlaceholder(this).register();

        // auto save every 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    saveRanks();
                } catch (IOException e) {
                    Bukkit.getLogger().warning(e.toString());
                }
                // clean out notifyThroughGUIDelay
                GUI.notifyThroughGUIDelay.entrySet().removeIf(entries -> entries.getValue() < System.currentTimeMillis());
            }
        }.runTaskTimer(this, 6000L, 6000L);

        // check if they completed a rank requirement
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    for (Map.Entry<String, List<Rank>> rankPaths : ranks.entrySet()) {
                        int rankNum = getRankNum(p, rankPaths.getKey());
                        if (rankNum < rankPaths.getValue().size() - 1) { // make sure they aren't on the max rank
                             Rank rank = getRank(rankNum + 1, rankPaths.getKey());
                             if (rank != null)
                                rank.checkRankCompletion(p, rankPaths.getKey(), false); // check completion on next rank
                        }
                    }
                }
            }
        }.runTaskTimer(this, 500L, 60);
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
            saveRanks();
        } catch (IOException e) {
            Bukkit.getLogger().warning(e.toString());
        }
        log();
    }


    public void rankup(Player p, String rankType, int newRankIndex) {
        Rank newRank = getRank(newRankIndex, rankType);
        if (newRank == null){
            Bukkit.getLogger().warning("[NotRanks] " + p.getName() + " is trying to rankup to a rank that doesn't exist! " + rankType + ":" + newRankIndex + "\nThis is a bug. Please contact the developer Not_Jaden.");
            return;
        }
        RankupEvent event = new RankupEvent(p, newRank, getRank(p, rankType), newRankIndex);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            if (!rankUp.isEmpty()) {
                String text = rankUp;
                text = text.replaceAll("\\{player}", p.getName());
                text = text.replaceAll("\\{rank}", newRank.getName());

                Bukkit.broadcastMessage(prefix + parse(text, p));
            }
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
            newRank.rankup(p);
            logs.add("[" + formatExact.format(now) + "] " + p.getName() + " ranked up to " + newRank.getName() + ".");
            addRank(p, rankType, newRankIndex);
            lastRankPathUsed.put(p.getUniqueId(), rankType);
        }
    }


    public void saveRanks() throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("version", 1);
        for (Map.Entry<UUID, Map<String, List<Integer>>> playerEntry : rankData.entrySet()) {
            String uuid = playerEntry.getKey().toString();
            for (Map.Entry<String, List<Integer>> rankEntry : playerEntry.getValue().entrySet()) {
                StringBuilder builder = new StringBuilder();
                List<Integer> completedRanks = rankEntry.getValue();
                for (int i = 0; i < completedRanks.size() - 1; i++) {
                    builder.append(completedRanks.get(i)).append(",");
                }
                if (!completedRanks.isEmpty()) {
                    builder.append(completedRanks.get(completedRanks.size() - 1));
                    configuration.set("data." + uuid + "." + rankEntry.getKey(), builder.toString());
                }
            }
            if (prefixSelections.containsKey(UUID.fromString(uuid)))
                configuration.set("data." + uuid + ".prefix", prefixSelections.get(UUID.fromString(uuid)));
            if (lastRankPathUsed.containsKey(UUID.fromString(uuid)))
                configuration.set("data." + uuid + ".last-path", lastRankPathUsed.get(UUID.fromString(uuid)));
        }
        configuration.save(playerdata);
    }

    public void loadConfig() throws IOException {
        log();
        LanguageOptions.loadConfig();
        ConfigOptions.loadConfig();
    }

    // control chat if enabled
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!addPrefix)
            return;
        String parsedPrefix = prefixFormat;
        Rank rank = getPrefixRank(event.getPlayer()); //getRank(event.getPlayer(), "default");
        String rankName = rank != null ? rank.getName() : noRank;
        parsedPrefix = parsedPrefix.replaceAll("\\{prefix}", rankName);
        parsedPrefix = parsedPrefix.replaceAll("\\{name}", "%s");
        if (overwritePrefix)
            parsedPrefix += "%s";
        parsedPrefix = parse(parsedPrefix, event.getPlayer()) + ChatColor.WHITE;
        if (overwritePrefix) {
            event.setFormat(parsedPrefix);
        } else {
            event.setFormat(parsedPrefix + event.getFormat());
        }
    }


    public void log() {
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

    public void writeLog(String text) {
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
}
