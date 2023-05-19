package me.jadenp.notranks;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class RankupEvent extends Event implements Cancellable{

    private final Player player;
    private final Rank newRank;
    private final Rank oldRank;
    private final int rankNumber;
    private boolean isCancelled;
    public RankupEvent(Player player, Rank newRank, Rank oldRank, int rankNumber){
        this.player = player;
        this.newRank = newRank;
        this.oldRank = oldRank;
        this.rankNumber = rankNumber;
    }

    public Player getPlayer() {
        return player;
    }

    public int getRankNumber() {
        return rankNumber;
    }

    public Rank getNewRank() {
        return newRank;
    }

    public Rank getOldRank() {
        return oldRank;
    }

    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
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
