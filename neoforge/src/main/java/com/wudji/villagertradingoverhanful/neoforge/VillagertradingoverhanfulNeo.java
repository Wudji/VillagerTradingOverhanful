package com.wudji.villagertradingoverhanful.neoforge;

import com.wudji.villagertradingoverhanful.Constants;
import com.wudji.villagertradingoverhanful.config.TradingDeskConfig;
import com.wudji.villagertradingoverhanful.preferences.TradingDeskPreferences;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(Constants.MOD_ID)
public class VillagertradingoverhanfulNeo {
    public VillagertradingoverhanfulNeo(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, TradingDeskConfig.SPEC, TradingDeskConfig.CLIENT_CONFIG_FILE);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            container.registerExtensionPoint(IConfigScreenFactory.class,
                    (modContainer, parent) -> new ConfigurationScreen(modContainer, parent));
        }
        TradingDeskPreferences.load();
        Constants.LOG.info("Villager Trading Overhaul initialized on NeoForge");
    }
}
