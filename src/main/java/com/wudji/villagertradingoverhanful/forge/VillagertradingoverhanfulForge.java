package com.wudji.villagertradingoverhanful.forge;

import com.wudji.villagertradingoverhanful.Constants;
import com.wudji.villagertradingoverhanful.mixin.MerchantScreenRegistryAccessor;
import com.wudji.villagertradingoverhanful.preferences.TradingDeskPreferences;
import com.wudji.villagertradingoverhanful.screen.TradingDeskScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
public class VillagertradingoverhanfulForge {
    public VillagertradingoverhanfulForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            TradingDeskPreferences.load();
            MenuScreens.ScreenConstructor<MerchantMenu, TradingDeskScreen> factory = TradingDeskScreen::new;
            MerchantScreenRegistryAccessor
                    .villagerTradingOverhaul$getScreenFactories()
                    .put(MenuType.MERCHANT, factory);
        });
    }
}
