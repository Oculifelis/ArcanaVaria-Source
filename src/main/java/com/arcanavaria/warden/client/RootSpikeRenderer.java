package com.arcanavaria.warden.client;

import com.arcanavaria.warden.RootSpike;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * A root spike drawn as three tapering boxes of greatwood bark (Thaumaturge's own log texture, so no new art), leaning
 * out along its lane, pushed up out of the ground by {@link RootSpike#height}.
 */
public class RootSpikeRenderer extends EntityRenderer<RootSpike, RootSpikeRenderer.State> {
    public static class State extends EntityRenderState {
        public float height;
        public float yaw;
        public int seed;
    }

    private static final RenderType TYPE = RenderTypes.entityCutout(Identifier.fromNamespaceAndPath("thaumaturge", "textures/block/log_greatwood.png"));

    public RootSpikeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RootSpike e, State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.height = e.height(pt);
        s.yaw = e.getYRot();
        s.seed = e.getId();
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (s.height <= 0.01F) {
            return;
        }
        pose.pushPose();
        pose.translate(0.0F, (s.height - 1.0F) * 1.6F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(-s.yaw + 90.0F + (s.seed % 7) * 9.0F));
        pose.mulPose(Axis.XP.rotationDegrees(14.0F + (s.seed % 5) * 3.0F));
        int light = s.lightCoords;
        collector.submitCustomGeometry(pose, TYPE, (p, buf) -> {
            box(buf, p, 0.22F, 0.0F, 0.6F, light);
            box(buf, p, 0.15F, 0.55F, 1.15F, light);
            box(buf, p, 0.08F, 1.1F, 1.6F, light);
        });
        pose.popPose();
        super.submit(s, pose, collector, camera);
    }

    /** An upright box of half-width r from y0 to y1, each face mapped to the full log texture. */
    private static void box(VertexConsumer b, PoseStack.Pose p, float r, float y0, float y1, int light) {
        float v1 = (y1 - y0) / 1.6F;
        face(b, p, -r, y0, -r, r, y1, -r, 0, 0, -1, v1, light);
        face(b, p, r, y0, r, -r, y1, r, 0, 0, 1, v1, light);
        face(b, p, -r, y0, r, -r, y1, -r, -1, 0, 0, v1, light);
        face(b, p, r, y0, -r, r, y1, r, 1, 0, 0, v1, light);
        // top cap
        b.addVertex(p, -r, y1, -r).setColor(-1).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
        b.addVertex(p, -r, y1, r).setColor(-1).setUv(0, 0.3F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
        b.addVertex(p, r, y1, r).setColor(-1).setUv(0.3F, 0.3F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
        b.addVertex(p, r, y1, -r).setColor(-1).setUv(0.3F, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
    }

    private static void face(VertexConsumer b, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1,
                             float nx, float ny, float nz, float v1, int light) {
        b.addVertex(p, x0, y0, z0).setColor(-1).setUv(0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        b.addVertex(p, x1, y0, z1).setColor(-1).setUv(0.5F, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        b.addVertex(p, x1, y1, z1).setColor(-1).setUv(0.5F, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        b.addVertex(p, x0, y1, z0).setColor(-1).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }
}
