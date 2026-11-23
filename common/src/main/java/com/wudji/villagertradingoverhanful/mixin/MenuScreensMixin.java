package com.wudji.villagertradingoverhanful.mixin;

import com.wudji.villagertradingoverhanful.screen.TradingDeskScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MenuScreens.class)
public class MenuScreensMixin {

    @Inject(
            method = "create(Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/client/Minecraft;ILnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private static void villagerTradingOverhaul$create(MenuType<?> type, Minecraft minecraft, int windowId, Component title, CallbackInfo ci) {
        if (type != MenuType.MERCHANT || minecraft.player == null) {
            return;
        }
        MerchantMenu menu = MenuType.MERCHANT.create(windowId, minecraft.player.getInventory());
        TradingDeskScreen screen = new TradingDeskScreen(menu, minecraft.player.getInventory(), title);
        minecraft.player.containerMenu = screen.getMenu();
        minecraft.gui.setScreen(screen);
        ci.cancel();
    }
}
