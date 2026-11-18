package com.arcanavaria.mixin;

import com.arcanavaria.scythe.ScytheItem;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The scythe packet can arrive separately from vanilla use. Give the dash exclusive ownership of its input. */
@Pseudo
@Mixin(targets = "com.fundamentum.scythe.Scythes", remap = false)
abstract class FundamentumScythesMixin {
    @Inject(method = "use(Lnet/minecraft/server/level/ServerPlayer;ZZ)V", at = @At("HEAD"), cancellable = true)
    private static void arcanavaria$letReapersStepWin(ServerPlayer player, boolean down, boolean sneak, CallbackInfo ci) {
        if (down && ScytheItem.claimsReapersStep(player, sneak || player.isShiftKeyDown())) {
            ci.cancel();
        }
    }
}
