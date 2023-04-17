package me.jadenp.notranks;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class CompleteRequirementEvent extends Event implements Cancellable {

    private Player player;
    private Rank rank;
    private String requirementName;
    private String value;
    public CompleteRequirementEvent(Player player, Rank rank, String requirementName, String value, NotRanks notRanks){
        this.player = player;
        this.rank = rank;
        this.requirementName = requirementName;
        this.value = value;
        notRanks.writeLog(player.getName() + " completed requirement " + requirementName + " for " + rank.getName() + ". (" + value + ")");
    }

    public Player getPlayer() {
        return player;
    }

    public Rank getRank() {
        return rank;
    }

    public String getRequirementName() {
        return requirementName;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void setCancelled(boolean cancel) {

    }

    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
