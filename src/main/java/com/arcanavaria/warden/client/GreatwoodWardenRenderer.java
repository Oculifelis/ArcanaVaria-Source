package com.arcanavaria.warden.client;

import com.arcanavaria.warden.WardenState;
import com.arcanavaria.ArcanaVaria;
import com.arcanavaria.warden.GreatwoodWarden;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class GreatwoodWardenRenderer extends MobRenderer<GreatwoodWarden, GreatwoodWardenRenderState, GreatwoodWardenModel> {
    private static final Identifier TEXTURE = ArcanaVaria.id("textures/entity/greatwood_warden.png");
    private static final Identifier GLOW = ArcanaVaria.id("textures/entity/greatwood_warden_glow.png");

    public GreatwoodWardenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GreatwoodWardenModel(ctx.bakeLayer(WardenClient.WARDEN_LAYER)), 2.0F);
        // the amber heart, eyes, sap veins, fungi and crystals: steady, flaring while it charges the ray or mends
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW, (s, age) -> s.glow,
            new GreatwoodWardenModel(ctx.bakeLayer(WardenClient.WARDEN_LAYER)), RenderTypes::eyes, false));
    }

    @Override
    public Identifier getTextureLocation(GreatwoodWardenRenderState s) {
        return TEXTURE;
    }

    @Override
    public GreatwoodWardenRenderState createRenderState() {
        return new GreatwoodWardenRenderState();
    }

    @Override
    public void extractRenderState(GreatwoodWarden e, GreatwoodWardenRenderState s, float pt) {
        super.extractRenderState(e, s, pt);
        s.state = e.state();
        s.stateTime = e.stateTime(pt);
        s.phaseTwo = e.phaseTwo();
        s.dyingTime = e.deathTime > 0 ? e.deathTime + pt : 0.0F;
        s.deathTime = 0.0F;
        s.hasRedOverlay = e.hurtTime > 0;
        float glow = 0.75F + 0.1F * Mth.sin(s.ageInTicks * 0.1F);
        if (s.state == WardenState.RAY) {
            glow = s.stateTime < 30.0F ? 0.75F + 0.25F * s.stateTime / 30.0F : (s.stateTime < 55.0F ? 1.0F : 0.85F);
        } else if (s.state == WardenState.MEND) {
            glow = 0.8F + 0.2F * Mth.sin(s.ageInTicks * 0.4F);
        } else if (s.state == WardenState.STUN) {
            glow = 0.35F;
        }
        if (s.dyingTime > 0.0F) {
            glow *= Mth.clamp(1.0F - s.dyingTime / 30.0F, 0.0F, 1.0F);
        }
        s.glow = glow;
    }
}
