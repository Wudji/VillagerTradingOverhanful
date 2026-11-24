package com.wudji.villagertradingoverhanful.fabric;

import com.wudji.villagertradingoverhanful.Constants;
import com.wudji.villagertradingoverhanful.platform.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

import java.nio.file.Path;

public final class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void openConfigScreen(Screen parent) {
        Minecraft.getInstance().gui.setScreen(new ConfigurationScreen(Constants.MOD_ID, parent));
    }
}
