package com.wudji.villagertradingoverhanful.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;

import com.wudji.villagertradingoverhanful.client.mixin.MerchantScreenRegistryAccessor;
import com.wudji.villagertradingoverhanful.client.preferences.TradingDeskPreferences;
import com.wudji.villagertradingoverhanful.client.screen.TradingDeskScreen;

public class VillagertradingoverhanfulClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TradingDeskPreferences.load();
        MenuScreens.ScreenConstructor<MerchantMenu, TradingDeskScreen> factory = TradingDeskScreen::new;
        MerchantScreenRegistryAccessor.villagerTradingOverhaul$getScreenFactories().put(MenuType.MERCHANT, factory);
    }
}
