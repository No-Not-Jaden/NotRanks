package me.jadenp.notranks.gui;

public class PlayerInfo {
    private final int page;
    private final String guiType;
    private final String[] rankFormat;

    public PlayerInfo(int page, String guiType, String[] rankFormat) {
        this.page = page;
        this.guiType = guiType;
        this.rankFormat = rankFormat;
    }

    public String getGuiType() {
        return guiType;
    }

    public int getPage() {
        return page;
    }

    public String[] getRankFormat() {
        return rankFormat;
    }
}
