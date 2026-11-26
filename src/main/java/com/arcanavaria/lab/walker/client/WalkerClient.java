package com.arcanavaria.lab.walker.client;

import com.arcanavaria.ArcanaVaria;
import com.arcanavaria.client.gen.CrucibleWalkerLayer;
import com.arcanavaria.lab.walker.WalkerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ArcanaVaria.MODID, value = Dist.CLIENT)
public final class WalkerClient {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcanaVaria.id("crucible_walker"), "main");

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, CrucibleWalkerLayer::create);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WalkerRegistry.CRUCIBLE_WALKER.get(), WalkerRenderer::new);
    }

    private WalkerClient() {
    }
}
