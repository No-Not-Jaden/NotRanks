package me.jadenp.notranks;

import me.jadenp.notranks.gui.GUI;
import me.jadenp.notranks.gui.GUIOptions;
import me.jadenp.notranks.gui.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.regex.Matcher;

import static me.jadenp.notranks.ConfigOptions.*;
import static me.jadenp.notranks.LanguageOptions.prefix;
import static me.jadenp.notranks.NumberFormatting.checkAmount;
import static me.jadenp.notranks.NumberFormatting.tryParse;

public class ActionCommands {
    public static void execute(Player player, List<String> commands) {
        for (String command : commands) {
            execute(player, command);
        }
    }

    public static void execute(Player player, String command) {
        // config.yml
        if (debug)
            Bukkit.getLogger().info("[NotRanks] Executing command: " + command);
        command = command.replace("{player}", player.getName());

        int loops = 100; // to stop an infinite loop if the command isn't formatted correctly
        while (command.startsWith("@") || command.startsWith("!@") || command.startsWith("~player(")) {

            if (command.startsWith("@(player)")) {
                String permission = command.substring(9, command.indexOf(" "));
                if (!player.hasPermission(permission))
                    return;
                command = command.substring(command.indexOf(" ") + 1);
            } else if (command.startsWith("!@(player)")) {
                String permission = command.substring(10, command.indexOf(" "));
                if (player.hasPermission(permission))
                    return;
                command = command.substring(command.indexOf(" ") + 1);
            }

            if (command.startsWith("~player(") && command.contains(") ")) {
                String requirement = command.substring(8, command.indexOf(") "));
                if (isRequirementCanceled(requirement, player))
                    return;
                command = command.substring(command.indexOf(") ") + 2);
            }

            if (command.startsWith("@")){
                String permission = command.substring(1, command.indexOf(" "));
                if (!player.hasPermission(permission))
                    return;
                command = command.substring(command.indexOf(" ") + 1);
            } else if (command.startsWith("!@")){
                String permission = command.substring(2, command.indexOf(" "));
                if (player.hasPermission(permission))
                    return;
                command = command.substring(command.indexOf(" ") + 1);
            }

            if (loops == 0) {
                Bukkit.getLogger().warning("[NotRanks] Could not execute a rank command properly! A conditional is not formatted correctly.");
                Bukkit.getLogger().warning("here -> " + command);
                return;
            }
            loops--;
        }

        PlayerInfo info = GUI.playerInfo.get(player.getUniqueId());
        GUIOptions gui;
        if (info != null) {
            gui = GUI.getGUI(info.getGuiType());
            while (command.contains("{slot") && command.substring(command.indexOf("{slot")).contains("}")) {
                String replacement = "";
                try {
                    int slot = Integer.parseInt(command.substring(command.indexOf("{slot") + 5, command.substring(command.indexOf("{slot")).indexOf("}") + command.substring(0, command.indexOf("{slot")).length()));

                    ItemStack item = player.getOpenInventory().getTopInventory().getContents()[slot];
                    if (item != null && item.hasItemMeta()) {
                        if (info.getRankFormat().length > 0) {
                            // for confirmation GUI
                            String rankFormat = info.getRankFormat()[0];
                            Rank rank = getRank(rankFormat);
                            if (rank != null) {
                                replacement = getRankPath(rank) + " " + (getRankNum(rank) + 1);
                            }
                        } else if (gui.getRankSlots().contains(slot)) {
                            // for rank displays
                            int rankNum = gui.getRankSlots().indexOf(slot) + gui.getRankSlots().size() * (info.getPage() - 1);
                            Rank rank = getRank(rankNum, info.getGuiType());
                            if (rank != null)
                                replacement = getRankPath(rank) + " " + rankNum;
                        }
                        if (replacement.isEmpty()) {
                            ItemMeta meta = item.getItemMeta();
                            assert meta != null;
                            replacement = meta.getDisplayName();
                        }
                    }
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().warning("Error getting slot in command: \n" + command);
                }
                command = command.substring(0, command.indexOf("{slot")) + replacement + command.substring(command.substring(command.indexOf("{slot")).indexOf("}") + command.substring(0, command.indexOf("{slot")).length() + 1);
            }

            if (command.startsWith("[next]")) {
                int amount = 1;
                try {
                    amount = Integer.parseInt(command.substring(7));
                } catch (IndexOutOfBoundsException | NumberFormatException ignored){}
                GUI.openGUI(player, gui.getType(), info.getPage() + amount);
            } else if (command.startsWith("[back]")) {
                int amount = 1;
                try {
                    amount = Integer.parseInt(command.substring(7));
                } catch (IndexOutOfBoundsException | NumberFormatException ignored){}
                GUI.openGUI(player, gui.getType(), info.getPage() - amount);
            }
        }

        if (debug)
            Bukkit.getLogger().info("[NotRanks] parsed command: " + command);

        if (command.startsWith("[close]")) {
            player.getOpenInventory().close();
        } else if (command.startsWith("[p] ")) {
            if (papiEnabled)
                command = new PlaceholderAPIClass().parse(player, command);
            Bukkit.dispatchCommand(player, command.substring(4));
        } else if (command.startsWith("[gui]")){
            int amount = 1;
            String guiName = command.substring(6);
            if (command.substring(6).contains(" ")) {
                try {
                    amount = Integer.parseInt(command.substring(6 + command.substring(6).indexOf(" ")));
                    guiName = guiName.substring(0, guiName.indexOf(" "));
                } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
                }
            }
            GUI.openGUI(player, guiName, amount);
        } else if (command.startsWith("[player] ")) {
            if (papiEnabled)
                command = new PlaceholderAPIClass().parse(player, command);
            Bukkit.dispatchCommand(player, command.substring(9));
        }else if (command.startsWith("[message_player] ")) {
            String message = command.substring(17);
            player.sendMessage(LanguageOptions.parse(prefix + message, player));
        } else if (command.startsWith("[sound_player] ")) {
            command = command.substring(8);
            double volume = 1;
            double pitch = 1;
            String sound;
            if (command.contains(" ")) {
                sound = command.substring(0, command.indexOf(" "));
                command = command.substring(sound.length() + 1);
                try {
                    if (command.contains(" ")) {
                        volume = tryParse(command.substring(0, command.indexOf(" ")));
                        command = command.substring(command.indexOf(" ") + 1);
                        pitch = tryParse(command);
                    } else {
                        volume = tryParse(command);
                    }
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().warning("[NotRanks] Unknown number for [sound_player] command in rank command : " + command);
                    return;
                }
            } else {
                sound = command;
            }
            Sound realSound;
            try {
                realSound = Sound.valueOf(sound.toUpperCase());
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[NotRanks] Unknown sound for [sound_player] command in rank command : " + sound);
                return;
            }
            player.playSound(player.getEyeLocation(), realSound, (float) volume, (float) pitch);
        } else if (command.startsWith("[sound] ")) {
            command = command.substring(8);
            double volume = 1;
            double pitch = 1;
            String sound;
            if (command.contains(" ")) {
                sound = command.substring(0, command.indexOf(" "));
                command = command.substring(sound.length() + 1);
                try {
                    if (command.contains(" ")) {
                        volume = tryParse(command.substring(0, command.indexOf(" ")));
                        command = command.substring(command.indexOf(" ") + 1);
                        pitch = tryParse(command);
                    } else {
                        volume = tryParse(command);
                    }
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().warning("[NotRanks] Unknown number for [sound] command in rank command : " + command);
                    return;
                }
            } else {
                sound = command;
            }
            Sound realSound;
            try {
                realSound = Sound.valueOf(sound.toUpperCase());
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[NotRanks] Unknown sound for [sound] command in rank command : " + sound);
                return;
            }
            player.getWorld().playSound(player.getLocation(), realSound, (float) volume, (float) pitch);
        } else {
            if (papiEnabled)
                command = new PlaceholderAPIClass().parse(player, command);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    private static boolean isRequirementCanceled(String requirement, OfflinePlayer player) {
        return !Rank.isRequirementCompleted(requirement, player);
    }
}
