package com.wudji.villagertradingoverhanful.neoforge;

import com.wudji.villagertradingoverhanful.Constants;
import com.wudji.villagertradingoverhanful.config.TradingDeskConfig;
import com.wudji.villagertradingoverhanful.mixin.MerchantScreenRegistryAccessor;
import com.wudji.villagertradingoverhanful.preferences.TradingDeskPreferences;
import com.wudji.villagertradingoverhanful.screen.TradingDeskScreen;
import com.wudji.villagertradingoverhanful.settings.TradingDeskSettings;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(Constants.MOD_ID)
public class VillagertradingoverhanfulNeo {
    public VillagertradingoverhanfulNeo(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, TradingDeskConfig.SPEC, TradingDeskConfig.CLIENT_CONFIG_FILE);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener((ModConfigEvent.Loading event) -> syncConfig(event.getConfig()));
        modEventBus.addListener((ModConfigEvent.Reloading event) -> syncConfig(event.getConfig()));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            container.registerExtensionPoint(IConfigScreenFactory.class,
                    (modContainer, parent) -> new ConfigurationScreen(modContainer, parent));
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

    private static void syncConfig(ModConfig config) {
        if (config.getSpec() != TradingDeskConfig.SPEC) {
            return;
        }
        TradingDeskSettings.setBatchLimit(TradingDeskConfig.BATCH_LIMIT.get());
        TradingDeskSettings.setHideUnavailable(TradingDeskConfig.HIDE_UNAVAILABLE.get());
    }
}
