package me.jadenp.notranks;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.IntStream;

import static me.jadenp.notranks.ConfigOptions.*;
import static me.jadenp.notranks.LanguageOptions.*;
import static me.jadenp.notranks.NumberFormatting.*;


public class Rank {
    private final String name;
    private final List<String> lore;
    private final int customModelData;
    private final List<String> requirements;
    private final double cost;
    private final List<String> commands;
    private final String head;
    private final Map<String, List<Boolean>> completed = new HashMap<>();
    private final Map<String, List<Boolean>> firstTimeCompletion = new HashMap<>();
    private final String finishedHead;
    private final Material material;
    private final boolean completionLoreEnabled;
    private final List<String> completionLore;
    private final int completionCustomModelData;
    private final String completionHead;
    private final Material completionMaterial;
    private final boolean notOnRankLoreEnabled;
    private final List<String> notOnRankLore;
    private final int notOnRankCustomModelData;
    private final String notOnRankHead;
    private final Material notOnRankMaterial;
    private final boolean hideNBT;
    private final Map<Integer, List<String>> requirementCommands = new HashMap<>();
    private final String prefix;
    private final String configurationName;

    public enum CompletionStatus {
        COMPLETE, INCOMPLETE, NO_ACCESS
    }

    public Rank(ConfigurationSection configurationSection, String completedHead) {
        Material material1;
        configurationName = configurationSection.getName();
        name = configurationSection.isSet("name") ? configurationSection.getString("name") : "&6&lUnnamed Rank";
        lore = configurationSection.isSet("lore") ? configurationSection.getStringList("lore") : new ArrayList<>();
        customModelData = configurationSection.isSet("custom-model-data") ? configurationSection.getInt("custom-model-data") : -1;
        requirements = configurationSection.isSet("requirements") ? configurationSection.getStringList("requirements") : new ArrayList<>();
        cost = configurationSection.isSet("cost") ? configurationSection.getInt("cost") : 0;
        commands = configurationSection.isSet("commands") ? configurationSection.getStringList("commands") : new ArrayList<>();
        head = configurationSection.isSet("head") ? configurationSection.getString("head") : "-1";
        String item = configurationSection.isSet("item") ? configurationSection.getString("item") : "LIME_CONCRETE";
        try {
            assert item != null;
            material1 = Material.valueOf(item.toUpperCase());
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[NotRanks] Could not get material \"" + item + "\" for rank: " + name);
            material1 = Material.LIME_CONCRETE;
        }
        material = material1;
        completionLoreEnabled = configurationSection.isSet("completion-lore.enabled") && configurationSection.getBoolean("completion-lore.enabled");
        completionHead = configurationSection.isSet("completion-lore.head") ? configurationSection.getString("completion-lore.head") : "-1";
        completionLore = configurationSection.isSet("completion-lore.lore") ? configurationSection.getStringList("completion-lore.lore") : new ArrayList<>();
        completionCustomModelData = configurationSection.isSet("completion-lore.custom-model-data") ? configurationSection.getInt("completion-lore.custom-model-data") : -1;
        item = configurationSection.isSet("completion-lore.item") ? configurationSection.getString("completion-lore.item") : "EMERALD_BLOCK";
        try {
            assert item != null;
            material1 = Material.valueOf(item.toUpperCase());
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[NotRanks] Could not get completion material \"" + item + "\" for rank: " + name);
            material1 = Material.EMERALD_BLOCK;
        }
        completionMaterial = material1;
        hideNBT = configurationSection.isSet("hide-nbt") && configurationSection.getBoolean("hide-nbt");
        notOnRankLoreEnabled = configurationSection.isSet("not-on-rank.enabled") && configurationSection.getBoolean("not-on-rank.enabled");
        notOnRankHead = configurationSection.isSet("not-on-rank.head") ? configurationSection.getString("not-on-rank.head") : "-1";
        notOnRankLore = configurationSection.isSet("not-on-rank.lore") ? configurationSection.getStringList("not-on-rank.lore") : new ArrayList<>();
        notOnRankCustomModelData = configurationSection.isSet("not-on-rank.custom-model-data") ? configurationSection.getInt("not-on-rank.custom-model-data") : -1;
        item = configurationSection.isSet("not-on-rank.item") ? configurationSection.getString("not-on-rank.item") : "SCULK";
        try {
            assert item != null;
            material1 = Material.valueOf(item.toUpperCase());
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[NotRanks] Could not get not on rank material \"" + item + "\" for rank: " + name);
            material1 = Material.SCULK;
        }
        notOnRankMaterial = material1;
        finishedHead = completedHead;
        if (configurationSection.isConfigurationSection("requirement-commands")) {
            for (String reqNumber : Objects.requireNonNull(configurationSection.getConfigurationSection("requirement-commands")).getKeys(false)) {
                if (configurationSection.isList("requirement-commands." + reqNumber)) {
                    try {
                        requirementCommands.put(Integer.parseInt(reqNumber), configurationSection.getStringList("requirement-commands." + reqNumber));
                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().warning(() -> "[NotRanks] Requirement command \"" + reqNumber + "\" for rank: " + name + " is not a number.");
                    }
                } else {
                    Bukkit.getLogger().warning(() -> "[NotRanks] Requirement command #" + reqNumber + " for rank: " + name + " is not in list format");
                }
            }
        }
        if (configurationSection.isSet("prefix")) {
            prefix = configurationSection.getString("prefix");
        } else {
            prefix = name;
        }
    }

    public double getCost() {
        return cost;
    }

    private Object parseValue(String str) {
        if (str.equalsIgnoreCase("true"))
            return true;
        if (str.equalsIgnoreCase("false"))
            return false;

        try{
            return tryParse(str);
        } catch (NumberFormatException e){
            return str;
        }
    }

    private boolean compareObjects(Object parsedValue, Object parsedPlaceholder, String operator) {
        if (parsedValue instanceof Boolean) {
            boolean a = (boolean) parsedValue;
            boolean b = (boolean) parsedPlaceholder;
            if (operator.equalsIgnoreCase("!=")) {
                return a != b;
            }
            return a == b;
        } else if (parsedValue instanceof Integer) {
            int a = (int) parsedValue;
            int b = (int) parsedPlaceholder;
            if (operator.equalsIgnoreCase("!=")) {
                return a != b;
            }
            if (operator.equalsIgnoreCase("=")) {
                return a == b;
            }
            if (operator.equalsIgnoreCase(">=")) {
                return b >= a;
            }
            if (operator.equalsIgnoreCase("<=")) {
                return b <= a;
            }
            if (operator.equalsIgnoreCase(">")) {
                return b > a;
            }
            if (operator.equalsIgnoreCase("<")) {
                return b < a;
            }
            return b >= a;
        } else if (parsedValue instanceof Double) {
            double a = (double) parsedValue;
            double b = (double) parsedPlaceholder;
            if (operator.equalsIgnoreCase("!=")) {
                return a != b;
            }
            if (operator.equalsIgnoreCase("=")) {
                return a == b;
            }
            if (operator.equalsIgnoreCase(">=")) {
                return b >= a;
            }
            if (operator.equalsIgnoreCase("<=")) {
                return b <= a;
            }
            if (operator.equalsIgnoreCase(">")) {
                return b > a;
            }
            if (operator.equalsIgnoreCase("<")) {
                return b < a;
            }
            return b >= a;
        } else if (parsedValue instanceof String) {
            String a = (String) parsedValue;
            String b = (String) parsedPlaceholder;
            if (operator.equalsIgnoreCase("!=")) {
                return !a.equalsIgnoreCase(b);
            }
            return a.equalsIgnoreCase(b);
        }
        return false;
    }



    public boolean isRequirementCompleted(String requirement, OfflinePlayer player) {
        try {
            String placeholder = requirement.substring(0, requirement.indexOf(" "));
            String operator = requirement.substring(requirement.indexOf(" ") + 1, requirement.lastIndexOf(" "));
            String value = requirement.substring(requirement.lastIndexOf(" ") + 1);
            Object parsedValue = parseValue(value);

            if (placeholder.contains("%") && papiEnabled) {
                String parsed = parse(placeholder, player);
                Object parsedPlaceholder = parseValue(parsed);

                // value types don't match
                if (parsedValue instanceof Boolean && !(parsedPlaceholder instanceof Boolean)) {
                    return false;
                } else if (parsedValue instanceof Integer && !(parsedPlaceholder instanceof Integer)) {
                    return false;
                } else if (parsedValue instanceof Double && !(parsedPlaceholder instanceof Double)) {
                    return false;
                } else if (parsedValue instanceof String && !(parsedPlaceholder instanceof String)) {
                    return false;
                }
                return compareObjects(parsedValue, parsedPlaceholder, operator);
            } else {
                int customModelData = -1;
                if (placeholder.contains("<") && placeholder.contains(">"))
                    try {
                        customModelData = (int) tryParse(placeholder.substring(placeholder.indexOf("<") + 1, placeholder.indexOf(">")));
                        placeholder = placeholder.substring(0, placeholder.indexOf("<"));
                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().warning("[NotRanks] Could not get custom model data from " + placeholder);
                        Bukkit.getLogger().warning(e.toString());
                    }
                // check if it is a material
                if (player.isOnline()) {
                    assert player.getPlayer() != null;
                    Material m = Material.getMaterial(placeholder);
                    if (m != null) {
                        if (parsedValue instanceof Integer || parsedValue instanceof Double) {
                            int reqValue = parsedValue instanceof Double ? ((Double) parsedValue).intValue() : (int) parsedValue;
                            int playerValue = checkAmount(player.getPlayer(), m, customModelData);
                            return compareObjects(reqValue, playerValue, operator);
                        }
                    }
                }
            }
        } catch (IndexOutOfBoundsException e){
            Bukkit.getLogger().warning("Could not check requirement: " + requirement + "\nIs it formatted properly?");
        }
        return false;
    }


    /**
     * Check rank completion and send a message to the player if there is an update
     * @param p Player to check completion
     * @param path Path of the rank
     */
    public void checkRankCompletion(Player p, String path, boolean log) {
        List<Boolean> progress;
        progress = new ArrayList<>();
        if (requirements != null) {
            if (!requirements.isEmpty()) {
                for (String req : requirements) {
                    if (!req.isEmpty()) {
                        boolean result = isRequirementCompleted(req, p);

                        progress.add(result);
                        if (log)
                            Bukkit.getLogger().info("[NotRanks] " + req + " -> " + result);
                    }
                }
            }
        }

        // checking cost
        String requirement = currency + " >= " + cost;

        boolean costReq = checkBalance(p, cost);
        progress.add(costReq);
        if (log)
            Bukkit.getLogger().info("[NotRanks] " + requirement + " -> " + costReq);

        List<String> req = requirements != null ? new ArrayList<>(requirements) : new ArrayList<>();
        req.add(requirement);



        // go through and see if any change?
        if (firstTimeCompletion.containsKey(p.getUniqueId().toString())) {
            List<Boolean> completedYet = firstTimeCompletion.get(p.getUniqueId().toString());
            boolean sentMessage = false;
            for (int i = 0; i < progress.size(); i++) {
                if (progress.get(i) && !completedYet.get(i)) {
                    completedYet.set(i, true);
                    // completed requirement
                    if (!sentMessage) {
                        CompleteRequirementEvent event = new CompleteRequirementEvent(p, this, req.get(i));
                        Bukkit.getPluginManager().callEvent(event);
                        p.sendMessage(parse(prefix + completeRequirement.replace("{path}", path), p));
                        sentMessage = true;
                    }
                    // run requirement commands
                    if (requirementCommands.containsKey(i+1)) {
                        List<String> commands = requirementCommands.get(i+1);
                        IntStream.range(0, commands.size()).forEach(j -> commands.set(j, commands.get(j).replace("{path}", path)));
                        ActionCommands.execute(p, commands);
                    }
                    // check if all other requirements have been completed
                    if (!completedYet.contains(false)) {
                        p.sendMessage(parse(prefix + completeRank.replace("{path}", path), p));
                        if (autoRankup.get(path)) {
                            Rank rank = this;
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    NotRanks.getInstance().rankup(p, path, getRankNum(rank));
                                }
                            }.runTaskLater(NotRanks.getInstance(), 5);
                        }

                    }
                }
            }
            if (log)
                Bukkit.getLogger().info("[NotRanks] Total Rank Progress: " + completedYet.toString());
            firstTimeCompletion.replace(p.getUniqueId().toString(), completedYet);
        } else {
            firstTimeCompletion.put(p.getUniqueId().toString(), progress);
        }

        if (completed.containsKey(p.getUniqueId().toString())) {
            completed.replace(p.getUniqueId().toString(), progress);
        } else {
            completed.put(p.getUniqueId().toString(), progress);
        }

    }

    public float getCompletionPercent(OfflinePlayer player) {
        if (!firstTimeCompletion.containsKey(player.getUniqueId().toString()))
            return 0;
        List<Boolean> completion = firstTimeCompletion.get(player.getUniqueId().toString());
        int tru = 0;
        for (boolean b : completion)
            if (b)
                tru++;
        return (float) tru / completion.size();
    }

    public List<String> getLore(Player p, CompletionStatus completionStatus) {

        List<String> lore = null;
        switch (completionStatus) {
            case COMPLETE:
                if (completionLoreEnabled)
                    lore = completionLore;
                else
                    lore = this.lore;
                break;
            case INCOMPLETE:
                lore = this.lore;
                break;
            case NO_ACCESS:
                if (notOnRankLoreEnabled)
                    lore = notOnRankLore;
                else
                    lore = this.lore;
                break;
        }
        List<String> text = new ArrayList<>();

        if (lore != null)
            for (String str : lore) {
                str = parse(str, p);
                if (str.contains("{cost}")) {
                    double amount = getBalance(p);
                    String strCost = formatNumber(cost);
                    String strAmount = formatNumber(amount);


                    if (amount < cost && completionStatus != CompletionStatus.COMPLETE) {
                        str = ChatColor.RED + parse(substringBefore(str, "{cost}") + currencyPrefix, p) + ChatColor.YELLOW + strAmount + parse(currencySuffix, p) + ChatColor.DARK_GRAY + " / " + parse(currencyPrefix, p) + ChatColor.RED + strCost + parse(currencySuffix + substringAfter(str, "{cost}"), p);
                    } else {
                        // Completed
                        str = parse(completionBefore, p) + ChatColor.stripColor(parse(substringBefore(str, "{cost}"), p)) + parse(completionPrefix, p) + ChatColor.stripColor(parse(currencyPrefix, p)) + strCost + ChatColor.stripColor(parse( currencySuffix, p)) + " / " + ChatColor.stripColor(parse(currencyPrefix, p)) + strCost + ChatColor.stripColor(parse( currencySuffix, p)) + parse(completionSuffix, p)  + ChatColor.stripColor(parse(substringAfter(str, "{cost}"), p)) + parse(completionAfter, p);
                    }
                } else if (str.contains("{req") && str.contains("}")) {
                    int reqNum = Integer.parseInt(substringBefore(substringAfter(str, "{req"), "}"));
                    if (reqNum == 0)
                        reqNum = 1;
                    if (requirements != null) {
                        for (int i = 0; i < 100; i++) {
                            if (requirements.size() < reqNum)
                                break;
                            if (requirements.get(reqNum - 1).isEmpty())
                                reqNum++;
                        }
                        if (requirements.size() >= reqNum) {
                            str = getRequirementString(reqNum, p, completionStatus == CompletionStatus.COMPLETE, str);
                        } else {
                            str = ChatColor.DARK_RED + parse(substringBefore(str, "{req"), p) + "{ERROR}" + parse(substringAfter(str, "}"), p);
                        }

                    }
                } else {
                    str = parse(str, p);
                }
                text.add(str);
            }


        return text;
    }

    public String getRequirementString(int reqNum, OfflinePlayer p, boolean completed, String str) {
        if (!isRequirementCompleted(requirements.get(reqNum - 1), p) && !completed) {
            str = ChatColor.RED + parse(substringBefore(str, "{req" + reqNum + "}"), p) + getRequirementProgress(reqNum, p, false) + parse(substringAfter(str, "{req" + reqNum + "}"), p);
        } else {
            str = parse(completionBefore, p) + ChatColor.stripColor(parse(substringBefore(str, "{req" + reqNum + "}"), p)) + getRequirementProgress(reqNum, p, completed) + ChatColor.stripColor(parse(substringAfter(str, "{req" + reqNum + "}"), p)) + parse(completionAfter, p);
        }
        return str;
    }

    /**
     * Get the progress of a specific requirement
     * @param reqNum Requirement number
     * @param p Player to check requirements against
     * @param completed Whether the requirement should be marked completed regardless of staus
     * @return Requirement completion string that replaces {reqx} in rank lore
     */
    public String getRequirementProgress(int reqNum, OfflinePlayer p, boolean completed) {
        String str = "";
        if (reqNum == 0)
            reqNum = 1;
        if (requirements != null) {
            for (int i = 0; i < 100; i++) {
                if (requirements.get(reqNum - 1).isEmpty())
                    reqNum++;
            }
            if (requirements.size() >= reqNum) {

                String placeholder = requirements.get(reqNum - 1).substring(0, requirements.get(reqNum - 1).indexOf(" "));
                String value = requirements.get(reqNum - 1).substring(requirements.get(reqNum - 1).lastIndexOf(" ") + 1);
                String parsed = parse(placeholder, p);

                try {
                    // is number
                    value = formatNumber(tryParse(value));
                    parsed = formatNumber(tryParse(parsed));
                } catch (NumberFormatException ignored){
                }

                if (!isRequirementCompleted(requirements.get(reqNum - 1), p) && !completed) {
                    str = ChatColor.YELLOW + parsed + ChatColor.DARK_GRAY + " / " + ChatColor.RED + value;
                } else {
                    str = parse(completionPrefix, p) + value + " / " + value + parse(completionSuffix, p);
                }
            }
        }
        return str;
    }


    private boolean usingBase64(String str){
        try {
            Integer.parseInt(str);
            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public PlayerProfile createProfile(String base64){
        try {
            String urlString = new String(Base64.getDecoder().decode(base64));
            String before = "{\"textures\":{\"SKIN\":{\"url\":\"";
            String after = "\"}}}";

            urlString = urlString.substring(before.length(), urlString.length() - after.length());
            URL url = new URL(urlString);
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(url);
            profile.setTextures(textures);
            return profile;
        } catch (IllegalArgumentException | MalformedURLException e){
            if (debug)
                Bukkit.getLogger().warning(e.toString());
            return null;
        }
    }

    public ItemStack createPlayerSkull(String data){
        if (debug)
            Bukkit.getLogger().info("[NotRanks] Attempting to get player skull from: " + data);
        ItemStack item = null;
        if (usingBase64(data)){
            item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            assert meta != null;
            meta.setOwnerProfile(createProfile(data));
            item.setItemMeta(meta);
        } else if (HDBEnabled){
            HeadDatabaseAPI hdb = new HeadDatabaseAPI();
            try {
                item = hdb.getItemHead(data);
            } catch (NullPointerException e) {
                if (debug)
                    Bukkit.getLogger().warning(e.toString());
                return null;
            }
        }
        return item;
    }

    public ItemStack getItem(Player p, CompletionStatus completionStatus) {
        ItemStack item = getBaseItem(completionStatus);

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.setDisplayName(parse(name, p));
        meta.setLore(getLore(p, completionStatus));
        if (hideNBT) {
            meta.getItemFlags().clear();
            Multimap<Attribute, AttributeModifier> attributes = HashMultimap.create();
            meta.setAttributeModifiers(attributes);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
            meta.addItemFlags(ItemFlag.HIDE_DYE);
            meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.addItemFlags(ItemFlag.values()[5]); // hide potion effect / advanced tooltips
        }
        if (customModelData != -1) {
            meta.setCustomModelData(customModelData);
        }
        switch (completionStatus) {
            case COMPLETE:
                if (completionLoreEnabled && completionCustomModelData != -1)
                    meta.setCustomModelData(completionCustomModelData);
                if (NotRanks.isAboveVersion(20, 4)) {
                    if (!meta.hasEnchantmentGlintOverride())
                        meta.setEnchantmentGlintOverride(true);
                } else if (!hideNBT) {
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                break;
            case INCOMPLETE:
                // custom model data already set
                break;
            case NO_ACCESS:
                if (notOnRankLoreEnabled && notOnRankCustomModelData != -1)
                    meta.setCustomModelData(notOnRankCustomModelData);
                break;
        }
        item.setItemMeta(meta);
        if (completionStatus == CompletionStatus.COMPLETE && !NotRanks.isAboveVersion(20, 4))
            item.addUnsafeEnchantment(Enchantment.CHANNELING, 1);
        return item;
    }

    public ItemStack getPrefixItem(CompletionStatus completionStatus, OfflinePlayer player){
        ItemStack item = getBaseItem(completionStatus);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.setDisplayName(parse(name, player));
        List<String> itemLore = new ArrayList<>();
        prefixLore.forEach(str -> itemLore.add(parse(str, player)));
        meta.setLore(itemLore);
        if (completionStatus == CompletionStatus.COMPLETE) {
            item.addUnsafeEnchantment(Enchantment.CHANNELING, 1);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        switch (completionStatus) {
            case COMPLETE:
                if (completionLoreEnabled && completionCustomModelData != -1)
                    meta.setCustomModelData(completionCustomModelData);
                break;
            case INCOMPLETE:
                if (customModelData != -1)
                    meta.setCustomModelData(customModelData);
                break;
            case NO_ACCESS:
                if (notOnRankLoreEnabled && notOnRankCustomModelData != -1)
                    meta.setCustomModelData(notOnRankCustomModelData);
                break;
        }
        item.setItemMeta(meta);
        return item;
    }



    private ItemStack getBaseItem(CompletionStatus completionStatus){
        ItemStack item = null;
        if (completionStatus == CompletionStatus.COMPLETE) {
            if (usingHeads) {
                if (completionLoreEnabled && !completionHead.equals("-1")) {
                    item = createPlayerSkull(completionHead);
                } else if (!finishedHead.equals("-1")) {
                    item = createPlayerSkull(finishedHead);
                }
            }
            if (completionLoreEnabled) {
                if (item == null)
                    item = new ItemStack(completionMaterial);
            }
        } else if (completionStatus == CompletionStatus.INCOMPLETE) {
            if (usingHeads && !head.equals("-1"))
                item = createPlayerSkull(head);
        } else if (completionStatus == CompletionStatus.NO_ACCESS) {
            if (usingHeads) {
                if (notOnRankLoreEnabled && !notOnRankHead.equals("-1")) {
                    item = createPlayerSkull(notOnRankHead);
                }
            }
            if (item == null && notOnRankLoreEnabled)
                item = new ItemStack(notOnRankMaterial);
        }
        if (item == null)
            item = new ItemStack(material);
        return item;
    }

    public String substringBefore(String str, String compare) {
        if (str.contains(compare)) {
            return str.substring(0, str.indexOf(compare));
        }
        return str;
    }

    public String substringAfter(String str, String compare) {
        if (str.contains(compare)) {
            int i = str.indexOf(compare) + compare.length();
            if (i < str.length()) {
                return str.substring(i);
            } else if (i == str.length()) {
                return "";
            }
        }
        return str;
    }


    /**
     * Check to see if a player has uncompleted requirements
     * @param p Player to view rank progress
     * @return true if a player has at least one uncompleted requirement
     */
    public boolean checkUncompleted(Player p, String path) {
        if (debug)
            Bukkit.getLogger().info("[NotRanks] Checking rank completion for " + p.getName() + " in path " + path + ".");
        checkRankCompletion(p, path, debug);
        if (completed.containsKey(p.getUniqueId().toString())) {
            return completed.get(p.getUniqueId().toString()).contains(false);
        } else {
            return true;
        }
    }

    public void rankup(Player p) {
        doRemoveCommands(p, cost, new ArrayList<>());
        if (commands != null)
            ActionCommands.execute(p, commands);
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getConfigurationName() {
        return configurationName;
    }
}
