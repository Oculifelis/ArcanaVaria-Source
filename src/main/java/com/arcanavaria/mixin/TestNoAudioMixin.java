package com.arcanavaria.mixin;

import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Test clients only (-Darcanavaria.autotest set): the sound engine never opens a device, so the run is silent. Normal play is untouched. */
@Mixin(SoundEngine.class)
public class TestNoAudioMixin {
    @Inject(method = "loadLibrary", at = @At("HEAD"), cancellable = true)
    private void arcanavaria$silent(CallbackInfo ci) {
        if (System.getProperty("arcanavaria.autotest") != null) {
            ci.cancel();
        }
    }

    @Inject(method = "shouldChangeDevice", at = @At("HEAD"), cancellable = true)
    private void arcanavaria$noDeviceChange(CallbackInfoReturnable<Boolean> cir) {
        if (System.getProperty("arcanavaria.autotest") != null) {
            cir.setReturnValue(false);
        }
    }
}
