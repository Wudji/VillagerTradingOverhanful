package com.wudji.villagertradingoverhanful.screen;

import com.wudji.villagertradingoverhanful.Constants;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

public final class TradeCyclingButtonFactory {
    private static final String BUTTON_CLASS = "de.maxhenkel.tradecycling.gui.CycleTradesButton";

    public static AbstractWidget create(int x, int y, MerchantMenu menu, Inventory inventory, Component title, Runnable onPress) {
        try {
            Class<?> buttonClass = Class.forName(BUTTON_CLASS);
            Constructor<?> constructor = buttonClass.getConstructor(int.class, int.class, Consumer.class, MerchantScreen.class);
            Consumer<Object> consumer = consumer(onPress);
            MerchantScreen host = new MerchantScreen(menu, inventory, title);
            return (AbstractWidget) constructor.newInstance(x, y, consumer, host);
        } catch (ReflectiveOperationException | LinkageError exception) {
            Constants.LOG.debug("Trade Cycling cycle button is not available", exception);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Consumer<Object> consumer(Runnable action) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("accept")) {
                action.run();
            }
            return null;
        };
        return (Consumer<Object>) Proxy.newProxyInstance(TradeCyclingButtonFactory.class.getClassLoader(), new Class<?>[]{Consumer.class}, handler);
    }

    private TradeCyclingButtonFactory() {
    }
}
