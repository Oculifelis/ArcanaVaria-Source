package com.arcanavaria.lattice.client;

import com.arcanavaria.ArcanaVaria;
import com.arcanavaria.lattice.LatticeRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ArcanaVaria.MODID, value = Dist.CLIENT)
public final class LatticeClient {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(LatticeRegistry.LATTICE_BE.get(), LatticeRenderer::new);
    }

    private LatticeClient() {
    }
}
