package com.clipvault.model;

public class AppSettings {
    private String hotkey = "CTRL+SHIFT+V";
    private boolean autoStart = false;
    private boolean darkTheme = true;
    private boolean clearHistoryOnExit = false;
    private int maxHistoryCount = 0;
    private boolean firstRun = true;
    private String savePath = "";

    public String getHotkey() {
        return hotkey;
    }

    public void setHotkey(String hotkey) {
        this.hotkey = hotkey;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public boolean isDarkTheme() {
        return darkTheme;
    }

    public void setDarkTheme(boolean darkTheme) {
        this.darkTheme = darkTheme;
    }

    public boolean isClearHistoryOnExit() {
        return clearHistoryOnExit;
    }

    public void setClearHistoryOnExit(boolean clearHistoryOnExit) {
        this.clearHistoryOnExit = clearHistoryOnExit;
    }

    public int getMaxHistoryCount() {
        return maxHistoryCount;
    }

    public void setMaxHistoryCount(int maxHistoryCount) {
        this.maxHistoryCount = maxHistoryCount;
    }

    public boolean isFirstRun() {
        return firstRun;
    }

    public void setFirstRun(boolean firstRun) {
        this.firstRun = firstRun;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }
}
