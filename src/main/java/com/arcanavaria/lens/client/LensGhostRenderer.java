/* SPDX-License-Identifier: MIT
 * Copyright (c) 2023 Direwolf20-MC
 * Render-stage and overlay setup adapted from Building Gadgets 2 v26.1.2.3 RenderLevelLast and OurRenderTypes.
 * Full notice in LICENSE-third-party.md.
 */
package com.arcanavaria.lens.client;

import com.arcanavaria.ArcanaVaria;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = ArcanaVaria.MODID, value = Dist.CLIENT)
public final class LensGhostRenderer {
    private static final RenderType GHOST = RenderType.create("arcanavaria_lens_ghost",
            RenderSetup.builder(RenderPipelines.DEBUG_QUADS)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .sortOnUpload().createRenderSetup());

    private LensGhostRenderer() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || LensClient.preview().isEmpty()) {
            return;
        }
        var camera = minecraft.gameRenderer.getMainCamera().position();
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        var buffers = minecraft.renderBuffers().bufferSource();
        var vertices = buffers.getBuffer(GHOST);
        for (var position : LensClient.preview()) {
            LensGhostBoxes.renderBoxSolid(pose.last().pose(), vertices, position, 1F,
                    LensClient.destructionPreview() ? 0.3F : 0.75F, 0.2F, 0.035F);
        }
        buffers.endBatch(GHOST);
        java.awt.Color outline = LensClient.destructionPreview() ? new java.awt.Color(1F, 0.3F, 0.2F, 0.45F)
                : new java.awt.Color(1F, 0.75F, 0.2F, 0.45F);
        for (var position : LensClient.preview()) {
            LensGhostBoxes.renderLines(pose, position, position, outline, buffers);
        }
        buffers.endBatch(net.minecraft.client.renderer.rendertype.RenderTypes.lines());
        pose.popPose();
    }
}
