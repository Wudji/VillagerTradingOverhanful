package com.wudji.villagertradingoverhanful;

import com.mojang.logging.LogUtils;
import com.wudji.villagertradingoverhanful.mixin.MerchantScreenRegistryAccessor;
import com.wudji.villagertradingoverhanful.preferences.TradingDeskPreferences;
import com.wudji.villagertradingoverhanful.screen.TradingDeskScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

@Mod(Villagertradingoverhanful.MODID)
public class Villagertradingoverhanful {

    public static final String MODID = "villagertradingoverhanful";

    private static final Logger LOGGER = LogUtils.getLogger();

    public Villagertradingoverhanful(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {
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