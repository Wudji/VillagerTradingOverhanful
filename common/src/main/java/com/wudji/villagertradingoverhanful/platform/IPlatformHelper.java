package com.wudji.villagertradingoverhanful.platform;

import net.minecraft.client.gui.screens.Screen;

import java.nio.file.Path;

public interface IPlatformHelper {
    boolean isModLoaded(String modId);

    Path getConfigDir();

    void openConfigScreen(Screen parent);
}
