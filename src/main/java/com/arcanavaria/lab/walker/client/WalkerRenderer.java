package com.arcanavaria.lab.walker.client;

import com.arcanavaria.ArcanaVaria;
import com.arcanavaria.lab.walker.CrucibleWalker;
import com.arcanavaria.lab.walker.WalkerAct;
import com.arcanavaria.lab.walker.WalkerMode;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class WalkerRenderer extends MobRenderer<CrucibleWalker, WalkerRenderState, WalkerModel> {
    private static final Identifier TEXTURE = ArcanaVaria.id("textures/entity/crucible_walker.png");
    private static final Identifier GLOW = ArcanaVaria.id("textures/entity/crucible_walker_glow_grey.png");

    public WalkerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WalkerModel(ctx.bakeLayer(WalkerClient.LAYER)), 2.0F);
        this.addLayer(new WalkerGlowLayer(this, GLOW, new WalkerModel(ctx.bakeLayer(WalkerClient.LAYER))));
    }

    @Override
    public Identifier getTextureLocation(WalkerRenderState s) {
        return TEXTURE;
    }

    @Override
    public WalkerRenderState createRenderState() {
        return new WalkerRenderState();
    }

    @Override
    public void extractRenderState(CrucibleWalker e, WalkerRenderState s, float pt) {
        super.extractRenderState(e, s, pt);
        s.act = e.act();
        s.actTime = e.actTime(pt);
        s.mode = e.mode();
        s.modeTime = e.modeTime(pt);
        s.phaseTwo = e.phaseTwo();
        s.dyingTime = e.deathTime > 0 ? e.deathTime + pt : 0.0F;
        s.deathTime = 0.0F;
        s.gears = Mth.lerp(pt, e.gearPhaseO, e.gearPhase);
        s.glowColour = s.act == WalkerAct.TELL ? e.tellTint() : e.tint();
        float glow = 0.72F + 0.1F * Mth.sin(s.ageInTicks * 0.15F);
        if (s.act == WalkerAct.TELL) {
            glow = 1.0F;
        } else if (s.act == WalkerAct.START) {
            glow *= Mth.clamp(s.actTime / 40.0F, 0.0F, 1.0F);
        } else if (s.act == WalkerAct.BURST) {
            glow = 0.8F + 0.2F * Mth.abs(Mth.sin(s.actTime * 0.6F));
        } else if (s.mode == WalkerMode.OVERCLOCK) {
            glow = 0.92F + 0.08F * Mth.sin(s.ageInTicks * 0.7F);
        } else if (s.mode == WalkerMode.STALL) {
            glow = Mth.lerp(Mth.clamp(s.modeTime / 10.0F, 0.0F, 1.0F), 0.72F, 0.12F);
        }
        if (s.act == WalkerAct.RAY && s.actTime < 40.0F) {
            glow = Math.max(glow, 0.6F + 0.4F * (s.actTime / 40.0F));
        }
        if (s.dyingTime > 0.0F) {
            glow *= Mth.clamp(1.0F - s.dyingTime / 30.0F, 0.0F, 1.0F);
        }
        s.glow = glow;
    }
}
