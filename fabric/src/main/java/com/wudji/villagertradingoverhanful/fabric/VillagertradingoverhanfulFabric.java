package com.wudji.villagertradingoverhanful.fabric;

import com.wudji.villagertradingoverhanful.Constants;
import com.wudji.villagertradingoverhanful.config.TradingDeskConfig;
import com.wudji.villagertradingoverhanful.preferences.TradingDeskPreferences;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;

public class VillagertradingoverhanfulFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.CLIENT, (IConfigSpec) TradingDeskConfig.SPEC, TradingDeskConfig.CLIENT_CONFIG_FILE);
        TradingDeskPreferences.load();
        Constants.LOG.info("Villager Trading Overhaul initialized on Fabric");
    }
}
