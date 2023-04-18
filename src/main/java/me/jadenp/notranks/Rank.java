package me.jadenp.notranks;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.awt.*;
import java.util.*;
import java.util.List;
import static me.jadenp.notranks.ConfigOptions.*;



public class Rank {
    private final String name;
    private final List<String> lore;
    private final List<String> requirements;
    private final double cost;
    private final List<String> commands;
    private final int hdbNum;
    private final Map<String,List<Boolean>> completed = new HashMap<>();
    private final Map<String,List<Boolean>> firstTimeCompletion = new HashMap<>();
    private final int finishedHead;
    private Material material;

    public Rank(String name, List<String> lore, List<String> requirements, double cost, List<String> commands, int hdbNum, int finishedHead, String item){
        this.name = decodeHex(name);
        this.lore = lore;
        this.requirements = requirements;
        this.cost = cost;
        this.commands = commands;
        this.hdbNum = hdbNum;
        this.finishedHead = finishedHead;
        material = Material.getMaterial(item);
        /*Bukkit.getLogger().warning(name);
        if (requirements == null){
            Bukkit.getLogger().warning("mt");
        } else {
            Bukkit.getLogger().warning(requirements.toString());
        }*/

    }

    private Object parseValue(String str){
        if (str.equalsIgnoreCase("true"))
            return true;
        if (str.equalsIgnoreCase("false"))
            return false;

        int number;
        try {
            number = Integer.parseInt(str);
        } catch (NumberFormatException ignored){
            double alsoNumber;
            try {
                alsoNumber = Double.parseDouble(str);
            } catch (NumberFormatException ignored2){
                return str;
            }
            return alsoNumber;
        }
        return number;
    }

    private boolean compareObjects(Object parsedValue, Object parsedPlaceholder, String operator){
        if (parsedValue instanceof Boolean){
            boolean a = (boolean) parsedValue;
            boolean b = (boolean) parsedPlaceholder;
            if (operator.equalsIgnoreCase("!=")){
                return a != b;
            }
            return a == b;
        } else if (parsedValue instanceof Integer){
            int a = (int) parsedValue;
            int b = (int) parsedPlaceholder;
            if (operator.equalsIgnoreCase("!=")){
                return a != b;
            }
            if (operator.equalsIgnoreCase("=")){
                return a == b;
            }
            if (operator.equalsIgnoreCase(">=")){
                return b >= a;
            }
            if (operator.equalsIgnoreCase("<=")){
                return b <= a;
            }
            if (operator.equalsIgnoreCase(">")){
                return b > a;
            }
            if (operator.equalsIgnoreCase("<")){
                return b < a;
            }
            return b >= a;
        } else if (parsedValue instanceof Double){
            double a = (double) parsedValue;
            double b = (double) parsedPlaceholder;
            if (operator.equalsIgnoreCase("!=")){
                return a != b;
            }
            if (operator.equalsIgnoreCase("=")){
                return a == b;
            }
            if (operator.equalsIgnoreCase(">=")){
                return b >= a;
            }
            if (operator.equalsIgnoreCase("<=")){
                return b <= a;
            }
            if (operator.equalsIgnoreCase(">")){
                return b > a;
            }
            if (operator.equalsIgnoreCase("<")){
                return b < a;
            }
            return b >= a;
        } else if (parsedValue instanceof String){
            String a = (String) parsedValue;
            String b = (String) parsedPlaceholder;
            if (operator.equalsIgnoreCase("!=")){
                return !a.equalsIgnoreCase(b);
            }
            return a.equalsIgnoreCase(b);
        }
        return false;
    }

    public void doRemoveCommands(Player player, double amount){
        String strAmount = ((double) Math.round(amount * Math.pow(10, decimals)) / Math.pow(10, decimals)) + "";
        if (decimals == 0){
            if (strAmount.contains("."))
                strAmount = strAmount.substring(0, strAmount.indexOf("."));
        }
        if (removeCommands != null)
        for (String str : removeCommands){
            while (str.contains("{player}")){
                str = str.replace("{player}", player.getName());
            }
            while (str.contains("{amount}")){
                str = str.replace("{amount}", strAmount);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), str);
        }
    }
    public int checkItemAmount(Player player, Material material){
        int amount = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack content : contents) {
            if (content != null) {
                if (content.getType() == material) {
                    amount += content.getAmount();
                }
            }
        }
        return amount;
    }

    public void removeItem(Player player, Material material, int amount){
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++){
            if (contents[i] != null) {
                if (contents[i].getType().equals(material)) {
                    if (contents[i].getAmount() > amount) {
                        contents[i] = new ItemStack(contents[i].getType(), contents[i].getAmount() - amount);
                        break;
                    } else if (contents[i].getAmount() < amount) {
                        amount -= contents[i].getAmount();
                        contents[i] = null;
                    } else {
                        contents[i] = null;
                        break;
                    }
                }
            }
        }
        player.getInventory().setContents(contents);
        player.updateInventory();
    }

    public boolean isRequirementCompleted(String requirement, OfflinePlayer player){
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
                        int reqValue = parsedValue instanceof Double ? ((Double) parsedValue).intValue() : (int) parsedValue ;
                        int playerValue = checkItemAmount(player.getPlayer(), m);
                        return compareObjects(reqValue, playerValue, operator);
                    }
                }
            }
        }
        return false;
    }



    public void checkRankCompletion(Player p){
        List<Boolean> progress;
        /*if (!completed.containsKey(p.getUniqueId().toString())){
            progress = new ArrayList<>();
         else {
            progress = completed.get(p.getUniqueId().toString());
        }*/
        progress = new ArrayList<>();
        if (requirements != null){
            if (!requirements.isEmpty()){
                for (String req : requirements){
                    progress.add(isRequirementCompleted(req, p));
                }
            }
        }

        // checking cost
        String requirement = currency + " >= " + cost;
        progress.add(isRequirementCompleted(requirement, p));

        /*if (usingPlaceholderCurrency){
            String requirement = currency + " >= " + cost;
            progress.add(isRequirementCompleted(requirement, p));
        } else {
            if (checkItemAmount(p, Material.valueOf(currency)) >= cost){
                progress.add(true);
            } else {
                progress.add(false);
            }
        }*/

        // go through and see if any change?
        if (firstTimeCompletion.containsKey(p.getUniqueId().toString())){
            List<Boolean> completedYet = firstTimeCompletion.get(p.getUniqueId().toString());
            boolean sentMessage = false;
            for (int i = 0; i < progress.size(); i++){
                if (progress.get(i) && !completedYet.get(i)){
                    completedYet.set(i, true);
                    // completed requirement
                    if (!sentMessage) {
                        p.sendMessage(decodeHex(NotRanks.getInstance().speakings.get(7)));
                        sentMessage = true;
                    }
                }
            }
            firstTimeCompletion.replace(p.getUniqueId().toString(), completedYet);
        } else {
            firstTimeCompletion.put(p.getUniqueId().toString(), progress);
        }

        if (completed.containsKey(p.getUniqueId().toString())){
            completed.replace(p.getUniqueId().toString(), progress);
        } else {
            completed.put(p.getUniqueId().toString(), progress);
        }

    }

    public List<String> getLore(Player p, boolean completed){
        List<String> text = new ArrayList<>();

        if (lore != null)
            for (String str : lore){
                str = decodeHex(str);
                str = PlaceholderAPI.setPlaceholders(p, str);
                if (str.contains("{cost}")){
                    double amount = 0;
                    boolean error = false;
                    if (usingPlaceholderCurrency) {
                        try {
                            amount = Double.parseDouble(PlaceholderAPI.setPlaceholders(p, currency));
                        } catch (NumberFormatException ignored) {
                            error = true;
                        }
                    } else {
                        amount = checkItemAmount(p, Material.valueOf(currency));
                    }
                    String strCost = ((double) Math.round(cost * Math.pow(10, decimals)) / Math.pow(10, decimals)) + "";
                    if (decimals == 0){
                        if (strCost.contains("."))
                            strCost = strCost.substring(0, strCost.indexOf("."));
                    }
                    String strAmount = ((double) Math.round(amount * Math.pow(10, decimals)) / Math.pow(10, decimals)) + "";
                    if (decimals == 0){
                        if (strAmount.contains("."))
                            strAmount = strAmount.substring(0, strAmount.indexOf("."));
                    }
                    if (error){
                        // use smthn else to replace amount
                        str = ChatColor.RED + ChatColor.translateAlternateColorCodes('&', substringBefore(str, "{cost}") + currencyPrefix) + ChatColor.YELLOW + currency + ChatColor.translateAlternateColorCodes('&', currencySuffix) + ChatColor.DARK_GRAY + " / " + ChatColor.translateAlternateColorCodes('&', currencyPrefix) + ChatColor.RED  + cost + ChatColor.translateAlternateColorCodes('&',currencySuffix + substringAfter(str,"{cost}"));
                    } else {
                        if (amount < cost && !completed){
                            str = ChatColor.RED + ChatColor.translateAlternateColorCodes('&', substringBefore(str, "{cost}") + currencyPrefix) + ChatColor.YELLOW + strAmount + ChatColor.translateAlternateColorCodes('&', currencySuffix) + ChatColor.DARK_GRAY + " / " + ChatColor.translateAlternateColorCodes('&', currencyPrefix) + ChatColor.RED  + strCost + ChatColor.translateAlternateColorCodes('&',currencySuffix + substringAfter(str,"{cost}"));
                        } else {
                            // STILL GOTTA DO THIS
                            str = ChatColor.GREEN + "" + ChatColor.STRIKETHROUGH + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',substringBefore(str, "{cost}") + currencyPrefix)) + strCost + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', currencySuffix)) + ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + " / " + ChatColor.GREEN + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', currencyPrefix)) + "" + ChatColor.STRIKETHROUGH + "" + strCost + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',currencySuffix + substringAfter(str, "{cost}")));
                        }
                    }
                } else if (str.contains("{req") && str.contains("}")) {
                    int reqNum = Integer.parseInt(substringBefore(substringAfter(str, "{req"), "}"));
                    if (requirements != null)
                        if (requirements.size() >= reqNum) {
                            String placeholder = requirements.get(reqNum - 1).substring(0, requirements.get(reqNum - 1).indexOf(" "));
                            String value = requirements.get(reqNum - 1).substring(requirements.get(reqNum - 1).lastIndexOf(" ") + 1);
                            String parsed = PlaceholderAPI.setPlaceholders(p, placeholder);


                            if (!isRequirementCompleted(requirements.get(reqNum-1), p) && !completed){
                                str = ChatColor.RED + ChatColor.translateAlternateColorCodes('&',substringBefore(str, "{req" + reqNum + "}")) + ChatColor.YELLOW + parsed + ChatColor.DARK_GRAY + " / " + ChatColor.RED + value + ChatColor.translateAlternateColorCodes('&',substringAfter(str, "{req" + reqNum + "}"));
                            } else {
                                str = ChatColor.GREEN + "" + ChatColor.STRIKETHROUGH + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',substringBefore(str, "{req" + reqNum + "}"))) + value + ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + " / " + ChatColor.GREEN + "" + ChatColor.STRIKETHROUGH + value + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',substringAfter(str, "{req" + reqNum + "}")));
                            }

                        } else {
                            str = ChatColor.DARK_RED + ChatColor.translateAlternateColorCodes('&',substringBefore(str, "{req")) + "{ERROR}" + ChatColor.translateAlternateColorCodes('&',substringAfter(str, "}"));
                        }
                } else {
                    str = ChatColor.translateAlternateColorCodes('&', str);
                }
                text.add(str);
            }


        return text;
    }

    public ItemStack getItem(Player p, boolean enchanted){
        ItemStack item;
        if (HDBEnabled && hdbNum != -1) {
            HeadDatabaseAPI hdb = new HeadDatabaseAPI();
            if (enchanted) {
                try {
                    item = hdb.getItemHead(finishedHead + "");
                } catch (NullPointerException nullPointerException) {
                    item = new ItemStack(material);
                }
            } else {
                try {
                    item = hdb.getItemHead(hdbNum + "");
                } catch (NullPointerException nullPointerException) {
                    item = new ItemStack(material);
                }
            }
        } else {
            item = new ItemStack(material);
        }
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(getLore(p, enchanted));
        item.setItemMeta(meta);
        return item;
    }

    public String substringBefore(String str, String compare){
        if (str.contains(compare)){
            return str.substring(0,str.indexOf(compare));
        }
        return str;
    }

    public String substringAfter(String str, String compare){
        if (str.contains(compare)){
            int i = str.indexOf(compare) + compare.length();
            if (i < str.length()) {
                return str.substring(i);
            } else if (i == str.length()){
                return "";
            }
        }
        return str;
    }



    public boolean checkRequirements(Player p){
        checkRankCompletion(p);
        if (completed.containsKey(p.getUniqueId().toString())){
            return !(completed.get(p.getUniqueId().toString()).contains(false));
        } else {
            return false;
        }
    }

    public void rankup(Player p){
        if (!usingPlaceholderCurrency){
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

    public List<String> getRequirements() {
        return requirements;
    }

    public String getName() {
        return name;
    }

    public static String decodeHex(String text){
        while (text.contains("[#")){
            String hex = text.substring(text.indexOf('#'), text.indexOf(']'));
            text = text.substring(0,text.indexOf('[')) + ChatColor.of(hex2Rgb(hex)) + text.substring(text.indexOf(']') + 1);
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static Color hex2Rgb(String colorStr) {
        return new Color(
                Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
                Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
                Integer.valueOf( colorStr.substring( 5, 7 ), 16 ) );
    }

}
