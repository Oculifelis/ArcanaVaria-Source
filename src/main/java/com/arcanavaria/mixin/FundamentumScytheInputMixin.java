package com.arcanavaria.mixin;

import com.arcanavaria.scythe.ScytheItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Leave vanilla use available when Reaper's Step owns the main-hand input. */
@Pseudo
@Mixin(targets = "com.fundamentum.flail.FlailsClient", remap = false)
abstract class FundamentumScytheInputMixin {
    @Inject(method = "use(Lnet/neoforged/neoforge/client/event/InputEvent$InteractionKeyMappingTriggered;)V",
            at = @At("HEAD"), cancellable = true)
    private static void arcanavaria$allowReapersStep(InputEvent.InteractionKeyMappingTriggered event, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && event.isUseItem() && event.getHand() == InteractionHand.MAIN_HAND
                && ScytheItem.claimsReapersStep(player, player.isShiftKeyDown())) {
            ci.cancel();
        }
    }
}
