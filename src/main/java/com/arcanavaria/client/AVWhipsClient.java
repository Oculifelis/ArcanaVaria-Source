package com.arcanavaria.client;

import com.arcanavaria.AVRegistry;
import com.arcanavaria.ArcanaVaria;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * Client side of the whips: registers the full-bright glow ribbons (client/WhipGlowStyle) when Fundamentum is installed.
 * Separate from WhipGlowStyle so nothing here loads a Fundamentum class without it.
 */
@Mod(value = ArcanaVaria.MODID, dist = Dist.CLIENT)
public final class AVWhipsClient {
    public AVWhipsClient() {
        if (AVRegistry.FUNDAMENTUM) {
            WhipGlowStyle.register();
        }
    }
}
