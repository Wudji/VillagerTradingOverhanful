package com.wudji.villagertradingoverhanful.fabric;

import com.wudji.villagertradingoverhanful.Constants;
import com.wudji.villagertradingoverhanful.mixin.MerchantScreenRegistryAccessor;
import com.wudji.villagertradingoverhanful.preferences.TradingDeskPreferences;
import com.wudji.villagertradingoverhanful.screen.TradingDeskScreen;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;

public class VillagertradingoverhanfulFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        TradingDeskPreferences.load();
        MenuScreens.ScreenConstructor<MerchantMenu, TradingDeskScreen> factory = TradingDeskScreen::new;
        MerchantScreenRegistryAccessor.villagerTradingOverhaul$getScreenFactories().put(MenuType.MERCHANT, factory);
        Constants.LOG.info("Villager Trading Overhaul initialized on Fabric");
    }
}
