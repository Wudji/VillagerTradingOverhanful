package com.wudji.villagertradingoverhanful.fabric;

import com.wudji.villagertradingoverhanful.Constants;
import com.wudji.villagertradingoverhanful.platform.ITradeCyclingCompat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;

public final class FabricTradeCyclingCompat implements ITradeCyclingCompat {
    private static final String CLIENT_MOD_CLASS = "de.maxhenkel.tradecycling.FabricTradeCyclingClientMod";
    private static final String KEY_CLASS = "de.maxhenkel.tradecycling.TradeCyclingClientMod";

    private final boolean loaded;
    private final KeyMapping cycleKey;
    private final Method sendPacket;

    public FabricTradeCyclingCompat() {
        boolean present = false;
        KeyMapping key = null;
        Method sender = null;
        try {
            Class<?> clientClass = Class.forName(CLIENT_MOD_CLASS);
            key = (KeyMapping) Class.forName(KEY_CLASS).getField("CYCLE_TRADES_KEY").get(null);
            sender = clientClass.getMethod("sendCycleTradesPacket");
            present = true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            Constants.LOG.debug("Trade Cycling is not available on Fabric", exception);
        }
        this.loaded = present;
        this.cycleKey = key;
        this.sendPacket = sender;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public boolean matchesCycleKey(KeyEvent event) {
        return loaded && cycleKey != null && cycleKey.matches(event);
    }

    @Override
    public Component keyDisplay() {
        return cycleKey == null ? Component.literal("?") : cycleKey.getTranslatedKeyMessage();
    }

    @Override
    public void cycleTrades() {
        if (!loaded) {
            return;
        }
        try {
            Class<?> clientClass = Class.forName(CLIENT_MOD_CLASS);
            Object instance = clientClass.getMethod("instance").invoke(null);
            sendPacket.invoke(instance);
        } catch (ReflectiveOperationException exception) {
            Constants.LOG.warn("Could not ask Trade Cycling to cycle trades", exception);
        }
    }
}
