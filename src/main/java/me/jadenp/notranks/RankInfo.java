package me.jadenp.notranks;

public class RankInfo {
    private final String type;
    private final int index;
    private final Rank rank;

    public RankInfo(String type, int index, Rank rank){

        this.type = type;
        this.index = index;
        this.rank = rank;
    }

    public int getIndex() {
        return index;
    }

    public Rank getRank() {
        return rank;
    }

    public String getType() {
        return type;
    }
}
