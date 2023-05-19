package me.jadenp.notranks;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CompleteRequirementEvent extends Event{

    private final Player player;
    private final Rank rank;
    private final String requirementName;
    public CompleteRequirementEvent(Player player, Rank rank, String requirementName){
        this.player = player;
        this.rank = rank;
        this.requirementName = requirementName;
        NotRanks.getInstance().writeLog(player.getName() + " completed requirement '" + requirementName + "' for " + rank.getName() + ".");
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

    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
