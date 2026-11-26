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
        // Build is Ordo pale gold and white, exchange is Permutatio green, destroy is Perditio dark.
        boolean destroy = LensClient.destructionPreview(), exchange = LensClient.exchangePreview();
        float[] fill = destroy ? new float[] {0.16F, 0.12F, 0.2F, 0.32F} : exchange ? new float[] {0.42F, 0.72F, 0.46F, 0.16F}
                : new float[] {1F, 0.93F, 0.72F, 0.14F};
        for (var position : LensClient.preview()) {
            LensGhostBoxes.renderBoxSolid(pose.last().pose(), vertices, position, fill[0], fill[1], fill[2], fill[3]);
        }
        buffers.endBatch(GHOST);
        java.awt.Color outline = destroy ? new java.awt.Color(0.36F, 0.24F, 0.44F, 0.9F) : exchange ? new java.awt.Color(0.6F, 0.86F, 0.6F, 0.85F)
                : new java.awt.Color(1F, 0.97F, 0.85F, 0.85F);
        for (var position : LensClient.preview()) {
            LensGhostBoxes.renderLines(pose, position, position, outline, buffers);
        }
        buffers.endBatch(net.minecraft.client.renderer.rendertype.RenderTypes.lines());
        pose.popPose();
    }
}
