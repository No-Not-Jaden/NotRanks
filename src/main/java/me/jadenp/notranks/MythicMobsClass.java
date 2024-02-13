package me.jadenp.notranks;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MythicMobsClass {
    private static Map<UUID, Map<String, Long>> mobsDefeated = new HashMap<>();
    private static String getMythicMobType(LivingEntity entity) {
        ActiveMob mythicMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId()).orElse(null);
        if(mythicMob != null){
            return mythicMob.getMobType();
        }
        return "Bukkit";
    }

    public static void killMob(Player player, LivingEntity entity) {
        if (ConfigOptions.mythicMobsEnabled) {
            String mobType = getMythicMobType(entity);
            if (!mobType.equals("Bukkit")) {
                Map<String, Long> playerRecord = mobsDefeated.containsKey(player.getUniqueId()) ? mobsDefeated.get(player.getUniqueId()) : new HashMap<>();
                long kills = playerRecord.containsKey(mobType) ? playerRecord.get(mobType) : 0;
                playerRecord.put(mobType, kills + 1);
                mobsDefeated.put(player.getUniqueId(), playerRecord);
            }
        }
    }

    public static void loadMobsDefeated(Map<UUID, Map<String, Long>> mobsDefeated) {
        MythicMobsClass.mobsDefeated = mobsDefeated;
    }

    public static Map<UUID, Map<String, Long>> getMobsDefeated() {
        return mobsDefeated;
    }
}
