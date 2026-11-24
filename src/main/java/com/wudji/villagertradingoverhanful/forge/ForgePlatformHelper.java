package com.wudji.villagertradingoverhanful.forge;

import com.wudji.villagertradingoverhanful.Constants;
import com.wudji.villagertradingoverhanful.platform.IPlatformHelper;
import com.wudji.villagertradingoverhanful.preferences.TradingDeskPreferences;
import com.wudji.villagertradingoverhanful.settings.TradingDeskSettings;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ForgePlatformHelper implements IPlatformHelper {
    private static final String CONFIG_FILE_NAME = "villager-trading-desk.properties";

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void openConfigScreen(Screen parent) {
        Minecraft.getInstance().setScreen(createConfigScreen(parent));
    }

    @Override
    public void loadConfig() {
        Path file = getConfigDir().resolve(CONFIG_FILE_NAME);
        if (!Files.exists(file)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
            TradingDeskSettings.setBatchLimit(parseInt(properties.getProperty("batchLimit"), 32));
            TradingDeskSettings.setHideUnavailable(Boolean.parseBoolean(properties.getProperty("hideUnavailable", "false")));
        } catch (IOException exception) {
            Constants.LOG.warn("Could not read villager trading desk config", exception);
        }
    }

    @Override
    public void saveConfig() {
        Properties properties = new Properties();
        properties.setProperty("batchLimit", Integer.toString(TradingDeskSettings.getBatchLimit()));
        properties.setProperty("hideUnavailable", Boolean.toString(TradingDeskSettings.shouldHideUnavailable()));
        try {
            Files.createDirectories(getConfigDir());
            try (OutputStream output = Files.newOutputStream(getConfigDir().resolve(CONFIG_FILE_NAME))) {
                properties.store(output, "Villager Trading Desk client preferences");
            }
        } catch (IOException exception) {
            Constants.LOG.warn("Could not save villager trading desk config", exception);
        }
    }

    private Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("screen.villagertradingoverhanful.options_title"));
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("screen.villagertradingoverhanful.general"));
        ConfigEntryBuilder entries = builder.entryBuilder();
        general.addEntry(entries.startIntField(Component.translatable("screen.villagertradingoverhanful.batch_limit"), TradingDeskSettings.getBatchLimit())
                .setDefaultValue(32)
                .setMin(1)
                .setMax(128)
                .setSaveConsumer(TradingDeskSettings::setBatchLimit)
                .build());
        general.addEntry(entries.startBooleanToggle(Component.translatable("screen.villagertradingoverhanful.hide_unavailable"), TradingDeskSettings.shouldHideUnavailable())
                .setDefaultValue(false)
                .setSaveConsumer(TradingDeskSettings::setHideUnavailable)
                .build());
        builder.setSavingRunnable(TradingDeskPreferences::save);
        return builder.build();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
