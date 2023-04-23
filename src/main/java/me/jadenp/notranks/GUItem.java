package me.jadenp.notranks;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class GUItem {
    private final int[] slot;
    private final List<String> actions;
    private final ItemStack item;

    public GUItem(int[] slot, List<String> actions, ItemStack item){

        this.slot = slot;
        this.actions = actions;
        this.item = item;
    }

    public List<String> getActions() {
        return actions;
    }

    public ItemStack getItem() {
        return item;
    }





    public ItemStack getPapiItem(OfflinePlayer player){
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(PlaceholderAPI.setPlaceholders(player, meta.getDisplayName()));
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            assert lore != null;
            lore.replaceAll(text -> PlaceholderAPI.setPlaceholders(player, text));
            meta.setLore(lore);
        }
        ItemStack newItem = item.clone();
        newItem.setItemMeta(meta);
        return newItem;
    }

    public int[] getSlot() {
        return slot;
    }
}
