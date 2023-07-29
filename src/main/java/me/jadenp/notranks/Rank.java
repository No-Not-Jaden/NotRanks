package me.jadenp.notranks;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.jadenp.notranks.ConfigOptions.*;
import static me.jadenp.notranks.LanguageOptions.*;
import static me.jadenp.notranks.NumberFormatting.*;


public class Rank {
    private final String name;
    private final List<String> lore;
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
    private final boolean hideNBT;

    public Rank(String name, List<String> lore, List<String> requirements, double cost, List<String> commands, String head, String finishedHead, String item, boolean completionLoreEnabled, List<String> completionLore, boolean hideNBT) {
        this.name = color(name);
        this.lore = lore;
        this.requirements = requirements;
        this.cost = cost;
        this.commands = commands;
        this.head = head;
        this.finishedHead = finishedHead;
        material = Material.getMaterial(item);
        this.completionLoreEnabled = completionLoreEnabled;
        this.completionLore = completionLore;
        this.hideNBT = hideNBT;
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
            return Double.parseDouble(str);
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

            if (placeholder.contains("%")) {
                String parsed = PlaceholderAPI.setPlaceholders(player, placeholder);
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
                // check if it is a material
                if (player.isOnline()) {
                    assert player.getPlayer() != null;
                    Material m = Material.getMaterial(placeholder);
                    if (m != null) {
                        if (parsedValue instanceof Integer || parsedValue instanceof Double) {
                            int reqValue = parsedValue instanceof Double ? ((Double) parsedValue).intValue() : (int) parsedValue;
                            int playerValue = checkAmount(player.getPlayer(), m);
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
     * @param path Path of the rank - this is only used in the completion notification
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
        boolean costReq = isRequirementCompleted(requirement, p);
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
                        p.sendMessage(prefix + parse(completeRequirement.replaceAll("\\{path}", path), p));
                        sentMessage = true;
                    }
                    // check if all other requirements have been completed
                    if (!completedYet.contains(false)) {
                        p.sendMessage(prefix + parse(completeRank.replaceAll("\\{path}", path), p));
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

    public List<String> getLore(Player p, boolean completed) {
        List<String> lore = completionLoreEnabled && completed ? completionLore : this.lore;
        List<String> text = new ArrayList<>();

        if (lore != null)
            for (String str : lore) {
                str = color(str);
                str = PlaceholderAPI.setPlaceholders(p, str);
                if (str.contains("{cost}")) {
                    double amount = 0;
                    boolean error = false;
                    if (usingPlaceholderCurrency) {
                        try {
                            amount = Double.parseDouble(PlaceholderAPI.setPlaceholders(p, currency));
                        } catch (NumberFormatException ignored) {
                            error = true;
                        }
                    } else {
                        amount = checkAmount(p, Material.valueOf(currency));
                    }
                    String strCost = formatNumber(cost);
                    String strAmount = formatNumber(amount);


                    if (error) {
                        // use smthn else to replace amount
                        str = ChatColor.RED + ChatColor.translateAlternateColorCodes('&', substringBefore(str, "{cost}") + currencyPrefix) + ChatColor.YELLOW + currency + ChatColor.translateAlternateColorCodes('&', currencySuffix) + ChatColor.DARK_GRAY + " / " + ChatColor.translateAlternateColorCodes('&', currencyPrefix) + ChatColor.RED + cost + ChatColor.translateAlternateColorCodes('&', currencySuffix + substringAfter(str, "{cost}"));
                    } else {
                        if (amount < cost && !completed) {
                            str = ChatColor.RED + ChatColor.translateAlternateColorCodes('&', substringBefore(str, "{cost}") + currencyPrefix) + ChatColor.YELLOW + strAmount + ChatColor.translateAlternateColorCodes('&', currencySuffix) + ChatColor.DARK_GRAY + " / " + ChatColor.translateAlternateColorCodes('&', currencyPrefix) + ChatColor.RED + strCost + ChatColor.translateAlternateColorCodes('&', currencySuffix + substringAfter(str, "{cost}"));
                        } else {
                            // STILL GOTTA DO THIS
                            str = color(completionBefore) + ChatColor.stripColor(color( substringBefore(str, "{cost}"))) + color(completionPrefix) + ChatColor.stripColor(color(currencyPrefix)) + strCost + ChatColor.stripColor(color( currencySuffix)) + " / " + ChatColor.stripColor(color(currencyPrefix)) + strCost + ChatColor.stripColor(color( currencySuffix)) + color(completionSuffix)  + ChatColor.stripColor(color(substringAfter(str, "{cost}"))) + color(completionAfter);
                        }
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
                            str = getRequirementString(reqNum, p, completed, str);
                        } else {
                            str = ChatColor.DARK_RED + ChatColor.translateAlternateColorCodes('&', substringBefore(str, "{req")) + "{ERROR}" + ChatColor.translateAlternateColorCodes('&', substringAfter(str, "}"));
                        }

                    }
                } else {
                    str = ChatColor.translateAlternateColorCodes('&', str);
                }
                text.add(str);
            }


        return text;
    }

    public String getRequirementString(int reqNum, OfflinePlayer p, boolean completed, String str) {
        if (!isRequirementCompleted(requirements.get(reqNum - 1), p) && !completed) {
            str = ChatColor.RED + ChatColor.translateAlternateColorCodes('&', substringBefore(str, "{req" + reqNum + "}")) + getRequirementProgress(reqNum, p, false) + ChatColor.translateAlternateColorCodes('&', substringAfter(str, "{req" + reqNum + "}"));
        } else {
            str = color(completionBefore) + ChatColor.stripColor(color(substringBefore(str, "{req" + reqNum + "}"))) + getRequirementProgress(reqNum, p, completed) + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', substringAfter(str, "{req" + reqNum + "}"))) + color(completionAfter);
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
                String parsed = PlaceholderAPI.setPlaceholders(p, placeholder);

                try {
                    // is number
                    value = formatNumber(Double.parseDouble(value));
                    parsed = formatNumber(Double.parseDouble(parsed));
                } catch (NumberFormatException ignored){
                }

                if (!isRequirementCompleted(requirements.get(reqNum - 1), p) && !completed) {
                    str = ChatColor.YELLOW + parsed + ChatColor.DARK_GRAY + " / " + ChatColor.RED + value;
                } else {
                    str = color(completionPrefix) + value + " / " + value + color(completionSuffix);
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
                e.printStackTrace();
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
            } catch (NullPointerException nullPointerException) {
                if (debug)
                    nullPointerException.printStackTrace();
                return null;
            }
        }
        return item;
    }

    public ItemStack getItem(Player p, boolean enchanted) {
        ItemStack item = getBaseItem(enchanted);

        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(getLore(p, enchanted));
        if (hideNBT) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
            meta.addItemFlags(ItemFlag.HIDE_DYE);
            meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        }
        if (!hideNBT && enchanted){
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        if (enchanted)
            item.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
        return item;
    }

    public ItemStack getPrefixItem(boolean enchanted){
        ItemStack item = getBaseItem(false);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Click to set this");
        lore.add(ChatColor.GRAY + "rank as your prefix");
        lore.add("");
        meta.setLore(lore);
        if (enchanted) {
            item.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }


    private ItemStack getBaseItem(boolean enchanted){
        ItemStack item = null;
        if (usingHeads){
            if (enchanted){
                item = createPlayerSkull(finishedHead);
            } else {
                item = createPlayerSkull(head);
            }
            if (debug && item == null){
                Bukkit.getLogger().info("[NotRanks] Could not get head.");
            }
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
        if (!usingPlaceholderCurrency) {
            removeItem(p, Material.valueOf(currency), (int) cost);
        }
        doRemoveCommands(p, cost);
        if (commands != null)
            for (String command : commands) {
                while (command.contains("{player}")) {
                    command = command.replace("{player}", p.getName());
                }
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
            }
    }

    public String getName() {
        return name;
    }



}
