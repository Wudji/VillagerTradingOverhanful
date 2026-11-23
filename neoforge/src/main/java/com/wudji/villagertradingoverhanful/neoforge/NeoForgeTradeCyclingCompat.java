package com.wudji.villagertradingoverhanful.neoforge;

import com.wudji.villagertradingoverhanful.Constants;
import com.wudji.villagertradingoverhanful.platform.ITradeCyclingCompat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class NeoForgeTradeCyclingCompat implements ITradeCyclingCompat {
    private static final String PACKET_CLASS = "de.maxhenkel.tradecycling.net.CycleTradesPacket";
    private static final String KEY_CLASS = "de.maxhenkel.tradecycling.TradeCyclingClientMod";

    private final boolean loaded;
    private final KeyMapping cycleKey;
    private final Constructor<?> packetConstructor;
    private final Method sendPacket;

    public NeoForgeTradeCyclingCompat() {
        boolean present = false;
        KeyMapping key = null;
        Constructor<?> constructor = null;
        Method sender = null;
        try {
            key = (KeyMapping) Class.forName(KEY_CLASS).getField("CYCLE_TRADES_KEY").get(null);
            Class<?> packetClass = Class.forName(PACKET_CLASS);
            constructor = packetClass.getConstructor();
            sender = Class.forName("net.neoforged.neoforge.client.network.ClientPacketDistributor")
                    .getMethod("sendToServer", CustomPacketPayload.class, CustomPacketPayload[].class);
            present = true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            Constants.LOG.debug("Trade Cycling is not available on NeoForge", exception);
        }
        this.loaded = present;
        this.cycleKey = key;
        this.packetConstructor = constructor;
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
            Object packet = packetConstructor.newInstance();
            sendPacket.invoke(null, packet, new CustomPacketPayload[0]);
        } catch (ReflectiveOperationException exception) {
            Constants.LOG.warn("Could not ask Trade Cycling to cycle trades", exception);
        }
    }
}
