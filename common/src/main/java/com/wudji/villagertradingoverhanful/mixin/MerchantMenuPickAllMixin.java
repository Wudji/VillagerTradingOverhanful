package com.wudji.villagertradingoverhanful.mixin;

import com.wudji.villagertradingoverhanful.screen.TradingDeskScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantMenu.class)
public class MerchantMenuPickAllMixin {
    @Inject(method = "canTakeItemForPickAll", at = @At("HEAD"), cancellable = true)
    private void villagerTradingOverhaul$allowPickAll(ItemStack stack, Slot slot, CallbackInfoReturnable<Boolean> cir) {
        if (slot.index >= 3 && Minecraft.getInstance().screen instanceof TradingDeskScreen) {
            cir.setReturnValue(true);
        }
    }
}
