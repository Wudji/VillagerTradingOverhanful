package com.wudji.villagertradingoverhanful.client.preferences;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;

public final class TradingDeskPreferences {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("villager-trading-desk.properties");
    private static final Set<String> FAVORITES = new HashSet<>();
    private static int batchLimit = 32;
    private static boolean hideUnavailable = false;

    private TradingDeskPreferences() {
    }

    public static void load() {
        if (!Files.exists(FILE)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(FILE)) {
            properties.load(input);
            batchLimit = clamp(parseInt(properties.getProperty("batchLimit"), batchLimit), 1, 128);
            hideUnavailable = Boolean.parseBoolean(properties.getProperty("hideUnavailable", "false"));
            String storedFavorites = properties.getProperty("favorites", "");
            for (String key : storedFavorites.split(",")) {
                if (!key.isBlank()) {
                    FAVORITES.add(key);
                }
            }
        } catch (IOException exception) {
            System.err.println("Could not read villager trading desk preferences: " + exception.getMessage());
        }
    }

    public static void save() {
        Properties properties = new Properties();
        properties.setProperty("batchLimit", Integer.toString(batchLimit));
        properties.setProperty("hideUnavailable", Boolean.toString(hideUnavailable));
        properties.setProperty("favorites", String.join(",", FAVORITES));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream output = Files.newOutputStream(FILE)) {
                properties.store(output, "Villager Trading Desk client preferences");
            }
        } catch (IOException exception) {
            System.err.println("Could not save villager trading desk preferences: " + exception.getMessage());
        }
    }

    public static int getBatchLimit() {
        return batchLimit;
    }

    public static void setBatchLimit(int value) {
        batchLimit = clamp(value, 1, 128);
    }

    public static boolean shouldHideUnavailable() {
        return hideUnavailable;
    }

    public static void setHideUnavailable(boolean value) {
        hideUnavailable = value;
    }

    public static boolean isFavorite(String key) {
        return FAVORITES.contains(key);
    }

    public static void toggleFavorite(String key) {
        if (!FAVORITES.add(key)) {
            FAVORITES.remove(key);
        }
        save();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
