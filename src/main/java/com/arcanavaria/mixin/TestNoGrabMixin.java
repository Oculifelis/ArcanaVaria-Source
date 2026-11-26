package com.arcanavaria.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test clients only (-Darcanavaria.autotest set): the cursor is never captured. Normal play is untouched. */
@Mixin(MouseHandler.class)
public class TestNoGrabMixin {
    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void arcanavaria$noGrab(CallbackInfo ci) {
        if (System.getProperty("arcanavaria.autotest") != null) {
            ci.cancel();
        }
    }
}
