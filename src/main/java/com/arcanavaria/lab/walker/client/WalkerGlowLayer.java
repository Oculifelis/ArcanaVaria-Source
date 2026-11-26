package com.arcanavaria.lab.walker.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * The lens, vents, crucible and runes, full bright. The glow texture is greyscale, so the colour comes from the walker:
 * thaumic violet mixed with its first trait, or a trait's own colour while it tells that trait's move.
 */
public class WalkerGlowLayer extends RenderLayer<WalkerRenderState, WalkerModel> {
    private final Identifier texture;
    private final WalkerModel model;

    public WalkerGlowLayer(RenderLayerParent<WalkerRenderState, WalkerModel> parent, Identifier texture, WalkerModel model) {
        super(parent);
        this.texture = texture;
        this.model = model;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, WalkerRenderState state, float yRot, float xRot) {
        if (state.isInvisible || state.glow <= 1.0E-3F) {
            return;
        }
        int colour = ARGB.color(Mth.clamp((int) (state.glow * 255.0F), 0, 255), state.glowColour & 0xFFFFFF);
        collector.order(1).submitModel(this.model, state, poseStack, RenderTypes.eyes(this.texture), light,
            LivingEntityRenderer.getOverlayCoords(state, 0.0F), colour, null, state.outlineColor, null);
    }
}
