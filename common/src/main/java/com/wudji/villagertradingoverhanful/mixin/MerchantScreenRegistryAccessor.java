package com.wudji.villagertradingoverhanful.mixin;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(MenuScreens.class)
public interface MerchantScreenRegistryAccessor {

    @Accessor("SCREENS")
    static Map<MenuType<?>, MenuScreens.ScreenConstructor<?, ?>> villagerTradingOverhaul$getScreenFactories() {
        throw new AssertionError();
    }
}
