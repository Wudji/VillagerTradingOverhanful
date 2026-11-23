package com.wudji.villagertradingoverhanful.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.wudji.villagertradingoverhanful.Constants;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ConfigurationScreen(Constants.MOD_ID, parent);
    }
}
