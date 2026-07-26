package com.wudji.villagertradingoverhanful;

import com.mojang.logging.LogUtils;
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
import org.slf4j.Logger;

@Mod(Villagertradingoverhanful.MODID)
public class Villagertradingoverhanful {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "villagertradingoverhanful";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public Villagertradingoverhanful() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            TradingDeskPreferences.load();

            MenuScreens.ScreenConstructor<MerchantMenu, TradingDeskScreen> factory =
                    TradingDeskScreen::new;

            MerchantScreenRegistryAccessor
                    .villagerTradingOverhaul$getScreenFactories()
                    .put(MenuType.MERCHANT, factory);
        });
    }

}
