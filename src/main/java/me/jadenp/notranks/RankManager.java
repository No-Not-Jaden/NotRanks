package me.jadenp.notranks;

import me.jadenp.notranks.gui.GUI;
import me.jadenp.notranks.gui.GUIOptions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RankManager {
    // <Rank Path Name (Lower Case), Ordered Ranks in Path>
    private static final Map<String, List<Rank>> ranks = new HashMap<>();
    // <Player UUID, <Rank Path, Completed Rank Index (starting from 0)>>
    private static final Map<UUID, Map<String, List<Integer>>> rankData = new HashMap<>();
    // p:PathName - prefix changes as player ranks up through the path
    // r:(rankNum) - shouldn't be used on its own, but will be default rank path
    // r:1p:default - rank 1 of default rank path - will not change when player ranks up
    // nothing - prefix changes with last rankup
    private static final Map<UUID, String> prefixSelections = new HashMap<>();
    private static final Map<String, Boolean> autoRankup = new HashMap<>();
    private static final Map<UUID, String> lastRankPathUsed = new HashMap<>();
    private static final Map<String, String> rankPathNames = new HashMap<>();

    public static Map<String, List<Rank>> getRanks() {
        return ranks;
    }

    public static List<Rank> getRanks(String path) {
        path = path.toLowerCase();
        if (ranks.containsKey(path))
            return ranks.get(path);
        return new ArrayList<>();
    }

    public static boolean isAutoRankup(String path) {
        return autoRankup.containsKey(path) && autoRankup.get(path);
    }

    public static Collection<String> getAllRankPaths() {
        return rankPathNames.values();
    }

    public static void readConfig(Plugin plugin) {
        File ranksFile = new File(plugin.getDataFolder() + File.separator + "ranks.yml");
        if (!ranksFile.exists())
            plugin.saveResource("ranks.yml", false);
        YamlConfiguration ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);


        // loading rank info from the config
        ranks.clear();
        autoRankup.clear();
        rankPathNames.clear();
        String completedHead = plugin.getConfig().getString("head.completed");
        for (String path : ranksConfig.getKeys(false)){
            List<Rank> rankPath = new ArrayList<>();
            for (String rankName : Objects.requireNonNull(ranksConfig.getConfigurationSection(path)).getKeys(false)) {
                if (ranksConfig.isConfigurationSection(path + "." + rankName)) {
                    // rank
                    rankPath.add(new Rank(Objects.requireNonNull(ranksConfig.getConfigurationSection(path + "." + rankName)), completedHead));
                    NotRanks.debugMessage("[NotRanks] Registered rank: " + path + ":" + rankName, false);
                }
            }
            autoRankup.put(path.toLowerCase(), ranksConfig.getBoolean(path + ".auto-rankup"));
            ranks.put(path.toLowerCase(), rankPath);
            rankPathNames.put(path.toLowerCase(), path);
        }
    }

    public static void readPlayerData(Plugin plugin) {
        File playerData = new File(plugin.getDataFolder() + File.separator + "playerdata.yml");
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(playerData);
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

        scheduleTasks(plugin);
    }

    private static void scheduleTasks(Plugin plugin) {
        // auto save every 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    saveRanks(plugin);
                } catch (IOException e) {
                    Bukkit.getLogger().warning(e.toString());
                }
                // clean out notifyThroughGUIDelay
                GUI.notifyThroughGUIDelay.entrySet().removeIf(entries -> entries.getValue() < System.currentTimeMillis());
            }
        }.runTaskTimer(plugin, 6000L, 6000L);
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
        }.runTaskTimer(plugin, 500L, 60);

    }

    public static void saveRanks(Plugin plugin) throws IOException {
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
        File playerData = new File(plugin.getDataFolder() + File.separator + "playerdata.yml");
        if (playerData.createNewFile())
            Bukkit.getLogger().info("[NotRanks] Creating a new player data file.");
        configuration.save(playerData);
    }

    public static @Nullable Rank getRank(OfflinePlayer p, String rankType) {
        int rankNum = getRankNum(p, rankType);
        if (rankNum != -1)
            return getRank(rankNum, rankType);
        return null;
    }

    public static @Nullable Rank getRank(int index, String rankType){
        if (ranks.isEmpty()) {
            Bukkit.getLogger().warning(() -> "[NotRanks] Rank " + rankType + ":" + index + " does not exist. Is ranks.yml formatted correctly?");
            return null;
        }
        List<Rank> ranksList = ranks.get(rankType);
        if (ranksList == null) {
            NotRanks.debugMessage("[NotRanks] " + rankType + " does not exist!", false);
            return null;
        }
        if (index >= ranksList.size()){
            NotRanks.debugMessage("[NotRanks] Rank " + index + " of " + rankType + " does not exist! There are not that many ranks.", false);
            return null;
        }
        return ranksList.get(index);
    }

    /**
     * Get a rank from rank format
     *
     * <p>p:PathName - last rank player had in path</p>
     * <p>r:(rankNum) - rank in default path</p>
     * <p>r:1p:default - rank 1 of default rank path</p>
     * <p>nothing - last rank</p>
     * @param rankFormat String in rank format
     * @param player Player that the request is for
     * @return Requested Rank or null if the rank format was incorrect or no rank existed
     */
    public static @Nullable Rank getRank(String rankFormat, OfflinePlayer player){
        if (rankFormat.isEmpty())
            return getRank(player, getLastRankPath(player));
        if (rankFormat.startsWith("p:")) {
            String path = rankFormat.substring(2);
            return getRank(player, path);
        }
        return getRank(rankFormat);
    }
    /**
     * Get a rank from rank format
     *
     * <p>r:(rankNum) - rank in default path</p>
     * <p>r:1p:default - rank 1 of default rank path</p>
     * @param rankFormat String in rank format
     * @return Requested Rank or null if the rank format was incorrect or no rank existed
     */
    public static @Nullable Rank getRank(String rankFormat){
        boolean hasPath = rankFormat.contains("p");
        try {
            String path = hasPath ? rankFormat.substring(rankFormat.indexOf("p") + 2) : "default";
            String rank = hasPath ? rankFormat.substring(2, rankFormat.indexOf("p")) : rankFormat.substring(2);
            return getRank(Integer.parseInt(rank), path);
        } catch (IndexOutOfBoundsException | NumberFormatException e){
            // incorrect format
            return null;
        }
    }


    public static String getRankFormat(int rankNum, String path) {
        return "r:" + rankNum + "p:" + path;
    }

    /**
     * Get the prefix rank the player is using
     * @param player Player to get prefix rank of
     * @return Rank that the player wants to be their prefix or null for no rank
     */
    public static @Nullable Rank getPrefixRank(OfflinePlayer player){
        if (prefixSelections.containsKey(player.getUniqueId()))
            return getRank(prefixSelections.get(player.getUniqueId()), player);
        return getRank(player, getLastRankPath(player)); // get last rank
    }

    public static String getLastRankPath(OfflinePlayer player) {
        if (lastRankPathUsed.containsKey(player.getUniqueId()))
            return lastRankPathUsed.get(player.getUniqueId());
        return "default";
    }

    public static void addRank(UUID uuid, String rankType, int index){
        Map<String, List<Integer>> playerRankInfo = rankData.containsKey(uuid) ? rankData.get(uuid) : new HashMap<>();
        List<Integer> completedRanks = playerRankInfo.containsKey(rankType) ? playerRankInfo.get(rankType) : new ArrayList<>();
        completedRanks.add(index);
        playerRankInfo.put(rankType, completedRanks);
        rankData.put(uuid, playerRankInfo);
    }

    public static List<Integer> getRankCompletion(OfflinePlayer p, String rankType){
        Map<String, List<Integer>> playerRankInfo = rankData.containsKey(p.getUniqueId()) ? rankData.get(p.getUniqueId()) : new HashMap<>();
        return playerRankInfo.containsKey(rankType) ? playerRankInfo.get(rankType) : new ArrayList<>();
    }

    public static Rank.CompletionStatus isRankUnlocked(OfflinePlayer p, String rankType, int index){
        if (rankData.containsKey(p.getUniqueId()) && rankData.get(p.getUniqueId()).containsKey(rankType)) {
            List<Integer> completedRanks = rankData.get(p.getUniqueId()).get(rankType);
            if (completedRanks.contains(index))
                return Rank.CompletionStatus.COMPLETE;
            if (completedRanks.contains(index-1))
                return Rank.CompletionStatus.INCOMPLETE;
        }
        GUIOptions gui = GUI.getGUI(rankType);
        if(gui != null && (!gui.isOrderlyProgression() || index == 0))
            return Rank.CompletionStatus.INCOMPLETE;
        return Rank.CompletionStatus.NO_ACCESS;
    }

    public static int getRankNum(OfflinePlayer p, String rankType){
        if (rankData.containsKey(p.getUniqueId()) && rankData.get(p.getUniqueId()).containsKey(rankType)) {
            List<Integer> completedRanks = rankData.get(p.getUniqueId()).get(rankType);
            if (completedRanks.isEmpty()) {
                return -1;
            }
            return completedRanks.get(completedRanks.size()-1);
        }
        return -1;
    }

    /**
     * Find rank number from rank object
     * @param rank Rank to find number of
     * @return Rank number or -1 if no rank was found
     */
    public static int getRankNum(Rank rank){
        for (Map.Entry<String, List<Rank>> entry : ranks.entrySet()) {
            for (Rank compareRank : entry.getValue()) {
                if (compareRank.equals(rank)){
                    return entry.getValue().indexOf(compareRank);
                }
            }
        }
        return -1;
    }

    public static RankInfo getRankInfo(String rankName){
        for (Map.Entry<String, List<Rank>> entry : ranks.entrySet()){
            for (Rank rank : entry.getValue()){
                if (rank.getName().equals(rankName)){
                    return new RankInfo(entry.getKey(), entry.getValue().indexOf(rank), rank);
                }
            }
        }
        return null;
    }

    public static List<Rank> getAllCompletedRanks(OfflinePlayer player){
        List<Rank> completed = new ArrayList<>();
        for (Map.Entry<String, List<Rank>> entry : ranks.entrySet()){
            for (int i = 0; i < entry.getValue().size(); i++) {
                if (isRankUnlocked(player, entry.getKey(), i) == Rank.CompletionStatus.COMPLETE){
                    completed.add(entry.getValue().get(i));
                }
            }
        }
        return completed;
    }

    /**
     * Get rank path from rank
     * @param rank to find path of
     * @return Rank path or an empty string if no rank matches
     */
    public static String getRankPath(Rank rank){
        for (Map.Entry<String, List<Rank>> entry : ranks.entrySet()) {
            for (Rank compareRank : entry.getValue()) {
                if (compareRank.equals(rank)){
                    return entry.getKey();
                }
            }
        }
        return "";
    }

    /**
     * Check if a rank path exists
     * @param path The path to check. Case does not matter.
     * @return True if the rank path exists.
     */
    public static boolean isRankPath(String path) {
        return ranks.containsKey(path.toLowerCase());
    }

    /**
     * Removes all ranks from a player.
     * @param uuid UUID of the player.
     * @return True if ranks were removed.
     */
    public static boolean removeRank(UUID uuid) {
        if (!rankData.containsKey(uuid))
            return false;
        Map<String, List<Integer>> rankCompletion = rankData.get(uuid);
        // clear out data from ranks
        for (Map.Entry<String, List<Integer>> entry : rankCompletion.entrySet()) {
            List<Integer> rankIndices = entry.getValue();
            for (int i : rankIndices) {
                Rank rank = getRank(i, entry.getKey());
                if (rank != null)
                    rank.clearRankData(uuid);
            }
        }
        rankData.remove(uuid);

        return true;
    }

    /**
     * Remove the ranks on a path from a player.
     * @param uuid UUID of the player.
     * @param path Rank path to be removed.
     * @return True if ranks were removed.
     */
    public static boolean removeRank(UUID uuid, String path) {
        path = path.toLowerCase();
        if (!rankData.containsKey(uuid) || !rankData.get(uuid).containsKey(path))
            return false;
        Map<String, List<Integer>> rankCompletion = rankData.get(uuid);
        List<Integer> rankIndices = rankCompletion.get(path);
        // clear out data from ranks
        for (int i : rankIndices) {
            Rank rank = getRank(i, path);
            if (rank != null)
                rank.clearRankData(uuid);
        }
        rankCompletion.remove(path);

        return true;
    }

    public static boolean removeRank(UUID uuid, String path, int rankNum) {
        path = path.toLowerCase();
        if (!rankData.containsKey(uuid) || !rankData.get(uuid).containsKey(path) || !rankData.get(uuid).get(path).contains(rankNum))
            return false;
        Map<String, List<Integer>> rankCompletion = rankData.get(uuid);
        List<Integer> rankIndices = rankCompletion.get(path);
        rankIndices.remove(rankNum);
        Rank rank = getRank(rankNum, path);
        if (rank != null)
            rank.clearRankData(uuid);

        return true;
    }

    public static void removePrefix(UUID uuid) {
        prefixSelections.remove(uuid);
    }

    public static void setPrefix(UUID uuid, String path) {
        prefixSelections.put(uuid, "p:" + path);
    }

    public static void setPrefix(UUID uuid, String path, int rankNum) {
        prefixSelections.put(uuid, "r:" + rankNum + "p:" + path);
    }

    public static String getPrefixSelection(UUID uuid) {
        if (prefixSelections.containsKey(uuid))
            return prefixSelections.get(uuid);
        return "";
    }

    public static void setLastRankPathUsed(UUID uuid, String path) {
        lastRankPathUsed.put(uuid, path);
    }
}
