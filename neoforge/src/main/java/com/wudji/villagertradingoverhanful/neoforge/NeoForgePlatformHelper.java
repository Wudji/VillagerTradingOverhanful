package com.wudji.villagertradingoverhanful.neoforge;

import com.wudji.villagertradingoverhanful.Constants;
import com.wudji.villagertradingoverhanful.platform.IPlatformHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

import java.nio.file.Path;

public final class NeoForgePlatformHelper implements IPlatformHelper {
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
        ModContainer container = ModList.get().getModContainerById(Constants.MOD_ID).orElseThrow();
        Minecraft.getInstance().gui.setScreen(new ConfigurationScreen(container, parent));
    }
}
