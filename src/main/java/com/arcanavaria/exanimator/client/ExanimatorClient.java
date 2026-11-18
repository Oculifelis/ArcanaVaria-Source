package com.arcanavaria.exanimator.client;

import com.arcanavaria.ArcanaVaria;
import com.arcanavaria.client.gen.ExanimatorLayer;
import com.arcanavaria.exanimator.ExanimatorRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ArcanaVaria.MODID, value = Dist.CLIENT)
public final class ExanimatorClient {
    public static final ModelLayerLocation EXANIMATOR_LAYER = new ModelLayerLocation(ArcanaVaria.id("exanimator"), "main");

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(EXANIMATOR_LAYER, ExanimatorLayer::create);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ExanimatorRegistry.EXANIMATOR.get(), ExanimatorRenderer::new);
    }

    private ExanimatorClient() {
    }
}
