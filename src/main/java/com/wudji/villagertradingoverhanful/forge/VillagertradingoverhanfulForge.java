package com.wudji.villagertradingoverhanful.forge;

import com.wudji.villagertradingoverhanful.Constants;
import com.wudji.villagertradingoverhanful.mixin.MerchantScreenRegistryAccessor;
import com.wudji.villagertradingoverhanful.preferences.TradingDeskPreferences;
import com.wudji.villagertradingoverhanful.screen.TradingDeskScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Constants.MOD_ID)
public class VillagertradingoverhanfulForge {
    public VillagertradingoverhanfulForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onClientSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> ForgePlatformHelper.createConfigScreen(parent)));
        }
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
