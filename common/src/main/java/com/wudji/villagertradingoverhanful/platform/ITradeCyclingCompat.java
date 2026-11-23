package com.wudji.villagertradingoverhanful.platform;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public interface ITradeCyclingCompat {
    boolean isLoaded();

    boolean matchesCycleKey(KeyEvent event);

    Component keyDisplay();

    void cycleTrades();
}
