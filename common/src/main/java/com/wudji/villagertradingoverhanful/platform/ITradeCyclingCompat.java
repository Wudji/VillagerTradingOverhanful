package com.wudji.villagertradingoverhanful.platform;

import net.minecraft.client.input.KeyEvent;

public interface ITradeCyclingCompat {
    boolean isLoaded();

    boolean matchesCycleKey(KeyEvent event);

    void cycleTrades();
}
