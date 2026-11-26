package com.arcanavaria.lab.client;

import com.arcanavaria.ArcanaVaria;
import com.arcanavaria.lab.LabRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(modid = ArcanaVaria.MODID, value = Dist.CLIENT)
public final class LabClient {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(LabRegistry.BEAM_EMITTER_BE.get(), SunbeamRenderer::new);
    }

    @SubscribeEvent
    public static void pipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(SunbeamRender.GLOW);
        event.registerPipeline(SunbeamRender.TINT);
    }

    private LabClient() {
    }
}
