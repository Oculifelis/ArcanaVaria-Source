package com.arcanavaria.exanimator.client;

import com.arcanavaria.ArcanaVaria;
import com.arcanavaria.exanimator.Exanimator;
import com.arcanavaria.exanimator.ExanimatorState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class ExanimatorRenderer extends MobRenderer<Exanimator, ExanimatorRenderState, ExanimatorModel> {
    private static final Identifier TEXTURE = ArcanaVaria.id("textures/entity/exanimator.png");
    private static final Identifier GLOW = ArcanaVaria.id("textures/entity/exanimator_glow.png");

    public ExanimatorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ExanimatorModel(ctx.bakeLayer(ExanimatorClient.EXANIMATOR_LAYER)), 1.6F);
        // eyes, runes and the rim of the chest void: a slow pulse, flaring when the maw opens or the eclipse falls
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW, (s, age) -> s.glow,
            new ExanimatorModel(ctx.bakeLayer(ExanimatorClient.EXANIMATOR_LAYER)), RenderTypes::eyes, false));
    }

    @Override
    public Identifier getTextureLocation(ExanimatorRenderState s) {
        return TEXTURE;
    }

    @Override
    public ExanimatorRenderState createRenderState() {
        return new ExanimatorRenderState();
    }

    @Override
    public void extractRenderState(Exanimator e, ExanimatorRenderState s, float pt) {
        super.extractRenderState(e, s, pt);
        s.state = e.state();
        s.stateTime = e.stateTime(pt);
        s.phaseTwo = e.phaseTwo();
        s.dyingTime = e.deathTime > 0 ? e.deathTime + pt : 0.0F;
        s.deathTime = 0.0F;
        s.hasRedOverlay = e.hurtTime > 0;
        float glow = (s.phaseTwo ? 0.85F : 0.7F) + 0.12F * Mth.sin(s.ageInTicks * 0.12F);
        if (s.state == ExanimatorState.MAW) {
            glow = 0.75F + 0.25F * Mth.clamp(s.stateTime / 14.0F, 0.0F, 1.0F) * (1.0F - Mth.clamp((s.stateTime - 60.0F) / 8.0F, 0.0F, 1.0F));
        } else if (s.state == ExanimatorState.ECLIPSE) {
            glow = 0.6F + 0.4F * Mth.abs(Mth.sin(s.stateTime * 0.3F));
        } else if (s.state == ExanimatorState.STUN) {
            glow = 0.3F;
        } else if (s.state == ExanimatorState.START) {
            glow *= Mth.clamp(s.stateTime / 60.0F, 0.0F, 1.0F);
        }
        if (s.dyingTime > 0.0F) {
            glow *= Mth.clamp(1.0F - s.dyingTime / 40.0F, 0.0F, 1.0F);
        }
        s.glow = glow;
    }
}
