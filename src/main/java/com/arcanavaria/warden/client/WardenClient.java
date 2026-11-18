package com.arcanavaria.warden.client;

import com.arcanavaria.ArcanaVaria;
import com.arcanavaria.client.gen.GreatwoodWardenLayer;
import com.arcanavaria.warden.WardenRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ArcanaVaria.MODID, value = Dist.CLIENT)
public final class WardenClient {
    public static final ModelLayerLocation WARDEN_LAYER = new ModelLayerLocation(ArcanaVaria.id("greatwood_warden"), "main");

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WARDEN_LAYER, GreatwoodWardenLayer::create);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WardenRegistry.GREATWOOD_WARDEN.get(), GreatwoodWardenRenderer::new);
        event.registerEntityRenderer(WardenRegistry.ROOT_SPIKE.get(), RootSpikeRenderer::new);
        event.registerEntityRenderer(WardenRegistry.SPLINTER.get(), ThrownItemRenderer::new);
    }

    private WardenClient() {
    }
}
