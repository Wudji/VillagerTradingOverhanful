package com.wudji.villagertradingoverhanful.preferences;

import com.wudji.villagertradingoverhanful.Constants;
import com.wudji.villagertradingoverhanful.config.TradingDeskConfig;
import com.wudji.villagertradingoverhanful.platform.Services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public final class TradingDeskPreferences {

    private static final String LEGACY_FILE_NAME = "villager-trading-desk.properties";
    private static final String FAVORITES_FILE_NAME = "villager-trading-desk-favorites.properties";
    private static final Set<String> FAVORITES = new HashSet<>();

    private TradingDeskPreferences() {
    }

    public static void load() {
        FAVORITES.clear();
        Path favoritesFile = favoritesFile();
        if (!Files.exists(favoritesFile)) {
            migrateLegacyFavorites(favoritesFile);
        }
        readFavorites(favoritesFile);
    }

    public static int getBatchLimit() {
        return TradingDeskConfig.BATCH_LIMIT.get();
    }

    public static boolean shouldHideUnavailable() {
        return TradingDeskConfig.HIDE_UNAVAILABLE.get();
    }

    public static boolean isFavorite(String key) {
        return FAVORITES.contains(key);
    }

    public static void toggleFavorite(String key) {
        if (!FAVORITES.add(key)) {
            FAVORITES.remove(key);
        }
        saveFavorites();
    }

    private static void readFavorites(Path file) {
        if (!Files.exists(file)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
            addFavorites(properties.getProperty("favorites", ""));
        } catch (IOException exception) {
            Constants.LOG.warn("Could not read villager trading desk favorites", exception);
        }
    }

    private static void migrateLegacyFavorites(Path favoritesFile) {
        Path legacyFile = configDir().resolve(LEGACY_FILE_NAME);
        if (!Files.exists(legacyFile)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(legacyFile)) {
            properties.load(input);
            addFavorites(properties.getProperty("favorites", ""));
            saveFavorites(favoritesFile);
        } catch (IOException exception) {
            Constants.LOG.warn("Could not migrate villager trading desk favorites", exception);
        }
    }

    private static void addFavorites(String storedFavorites) {
        for (String key : storedFavorites.split(",")) {
            if (!key.isBlank()) {
                FAVORITES.add(key);
            }
        }
    }

    private static void saveFavorites() {
        saveFavorites(favoritesFile());
    }

    private static void saveFavorites(Path file) {
        Properties properties = new Properties();
        properties.setProperty("favorites", String.join(",", FAVORITES));
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream output = Files.newOutputStream(file)) {
                properties.store(output, "Villager Trading Desk favorites");
            }
        } catch (IOException exception) {
            Constants.LOG.warn("Could not save villager trading desk favorites", exception);
        }
    }

    private static Path favoritesFile() {
        return configDir().resolve(FAVORITES_FILE_NAME);
    }

    private static Path configDir() {
        return Services.PLATFORM.getConfigDir();
    }
}
