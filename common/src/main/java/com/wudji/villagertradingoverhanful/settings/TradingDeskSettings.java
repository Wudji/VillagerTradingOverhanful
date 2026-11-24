package com.wudji.villagertradingoverhanful.settings;

public final class TradingDeskSettings {
    private static int batchLimit = 32;
    private static boolean hideUnavailable;

    private TradingDeskSettings() {
    }

    public static int getBatchLimit() {
        return batchLimit;
    }

    public static void setBatchLimit(int value) {
        batchLimit = Math.max(1, Math.min(128, value));
    }

    public static boolean shouldHideUnavailable() {
        return hideUnavailable;
    }

    public static void setHideUnavailable(boolean value) {
        hideUnavailable = value;
    }
}
