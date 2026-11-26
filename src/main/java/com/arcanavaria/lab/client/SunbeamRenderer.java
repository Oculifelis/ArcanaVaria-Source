package com.arcanavaria.lab.client;

import com.arcanavaria.lab.BeamBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * The Sun Lens beam: camera-facing ribbons along the path {@link BeamBlocks#trace} works out (the same one the server
 * uses), a white-gold core inside a golden glow, with a flare where it leaves the lens, turns at a mirror and lands.
 * The sunshaft look: a soft amber haze round the beam, drifting motes of light, and slow sun rays where it lands.
 */
public class SunbeamRenderer implements BlockEntityRenderer<BeamBlocks.EmitterTick, SunbeamRenderer.State> {
    static final int CORE = 0xFFF6DA, GOLD = 0xFFBE3C, AMBER = 0xFF8A1E, MOTE = 0xFFE08A;

    public static class State extends BlockEntityRenderState {
        final List<Vector3f> path = new ArrayList<>();
        float time;
        boolean landed;
    }

    public SunbeamRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BeamBlocks.EmitterTick be, State s, float partial, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, s, partial, camera, breakProgress);
        s.path.clear();
        if (be.getLevel() == null) {
            return;
        }
        BlockPos pos = be.getBlockPos();
        Direction dir = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos[] hit = new BlockPos[1];
        List<Vec3> pts = BeamBlocks.trace(be.getLevel(), pos, dir, hit);
        // start at the lens face, not inside the block
        Vec3 first = pts.get(0).add(dir.getStepX() * 0.45, 0, dir.getStepZ() * 0.45);
        pts.set(0, first);
        for (Vec3 p : pts) {
            s.path.add(new Vector3f((float) (p.x - pos.getX()), (float) (p.y - pos.getY()), (float) (p.z - pos.getZ())));
        }
        s.landed = hit[0] != null;
        s.time = (be.getLevel().getGameTime() % 24000L) + partial;
    }

    @Override
    public AABB getRenderBoundingBox(BeamBlocks.EmitterTick be) {
        return new AABB(be.getBlockPos()).inflate(BeamBlocks.RANGE + 1, 1, BeamBlocks.RANGE + 1);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (s.path.size() < 2) {
            return;
        }
        Vector3f cam = new Vector3f((float) (camera.pos.x - s.blockPos.getX()), (float) (camera.pos.y - s.blockPos.getY()),
                (float) (camera.pos.z - s.blockPos.getZ()));
        Vector3f right = camera.orientation.transform(new Vector3f(1, 0, 0));
        Vector3f up = camera.orientation.transform(new Vector3f(0, 1, 0));
        float t = s.time;
        float pulse = 1.0F + 0.12F * (float) Math.sin(t * 0.9F) + 0.05F * (float) Math.sin(t * 2.3F);
        float w = 0.085F * pulse;
        float scroll = t * 0.04F;

        collector.submitCustomGeometry(pose, SunbeamRender.type("beam"), (p, b) -> {
            float along = 0;
            for (int i = 0; i + 1 < s.path.size(); i++) {
                Vector3f a = s.path.get(i), c = s.path.get(i + 1);
                float len = a.distance(c);
                int steps = Math.max(1, (int) Math.ceil(len));
                for (int k = 0; k < steps; k++) {
                    Vector3f p0 = new Vector3f(a).lerp(c, (float) k / steps), p1 = new Vector3f(a).lerp(c, (float) (k + 1) / steps);
                    Vector3f side = facing(p0, p1, cam);
                    if (side == null) {
                        continue;
                    }
                    float u0 = (along + len * k / steps) * 0.5F - scroll, u1 = (along + len * (k + 1) / steps) * 0.5F - scroll;
                    // a wide soft haze that shimmers slowly, scrolling the other way
                    float shimmer = 0.22F + 0.06F * (float) Math.sin(t * 0.35F + (along + k) * 0.6F);
                    SunbeamRender.ribbon(b, p, p0, p1, new Vector3f(side).mul(w * 5.5F), u0 * 0.5F + scroll * 1.7F, u1 * 0.5F + scroll * 1.7F,
                            SunbeamRender.argb(shimmer, AMBER));
                    SunbeamRender.ribbon(b, p, p0, p1, new Vector3f(side).mul(w * 2.4F), u0, u1, SunbeamRender.argb(0.75F, GOLD));
                    SunbeamRender.ribbon(b, p, p0, p1, new Vector3f(side).mul(w * 0.75F), u0, u1, SunbeamRender.argb(1.0F, CORE));
                }
                along += len;
            }
        });

        // flares at the lens, at every mirror turn and where the beam lands, with drifting motes along the shaft
        collector.submitCustomGeometry(pose, SunbeamRender.type("flare"), (p, b) -> {
            for (int i = 0; i < s.path.size(); i++) {
                boolean end = i == s.path.size() - 1;
                float size = (i == 0 ? 2.6F : end ? (s.landed ? 4.0F : 2.4F) : 3.2F) * w;
                SunbeamRender.sprite(b, p, s.path.get(i), new Vector3f(right).mul(size), new Vector3f(up).mul(size), 0, SunbeamRender.argb(0.85F, GOLD));
                SunbeamRender.sprite(b, p, s.path.get(i), new Vector3f(right).mul(size * 0.45F), new Vector3f(up).mul(size * 0.45F), 0,
                        SunbeamRender.argb(1.0F, CORE));
            }
            motes(b, p, s, t, right, up);
        });
        // slow turning sun rays where the beam lands
        collector.submitCustomGeometry(pose, SunbeamRender.type("rays"), (p, b) -> {
            Vector3f at = s.path.get(s.path.size() - 1);
            float size = (s.landed ? 9.0F : 6.0F) * w * (1.0F + 0.08F * (float) Math.sin(t * 0.5F));
            SunbeamRender.sprite(b, p, at, new Vector3f(right).mul(size), new Vector3f(up).mul(size), t * 0.02F, SunbeamRender.argb(0.55F, GOLD));
            SunbeamRender.sprite(b, p, at, new Vector3f(right).mul(size * 0.7F), new Vector3f(up).mul(size * 0.7F), -t * 0.013F,
                    SunbeamRender.argb(0.4F, MOTE));
        });
    }

    /** The unit width direction for a ribbon from p0 to p1 that faces the camera, or null when seen end-on. */
    private static @Nullable Vector3f facing(Vector3f p0, Vector3f p1, Vector3f cam) {
        Vector3f seg = new Vector3f(p1).sub(p0);
        Vector3f mid = new Vector3f(p0).add(p1).mul(0.5F);
        Vector3f side = seg.cross(new Vector3f(cam).sub(mid));
        if (side.lengthSquared() < 1.0E-8F) {
            return null;
        }
        return side.normalize();
    }

    /** Sunshaft: specks of light drifting slowly along and around the beam, each twinkling on its own beat. */
    private static void motes(VertexConsumer b, PoseStack.Pose p, State s, float t, Vector3f right, Vector3f up) {
        for (int i = 0; i + 1 < s.path.size(); i++) {
            Vector3f a = s.path.get(i), c = s.path.get(i + 1);
            float len = a.distance(c);
            Vector3f dir = new Vector3f(c).sub(a);
            if (len < 0.05F) {
                continue;
            }
            dir.div(len);
            Vector3f pa = new Vector3f(dir).cross(Math.abs(dir.y) > 0.9F ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0)).normalize();
            Vector3f pb = new Vector3f(dir).cross(pa).normalize();
            int n = (int) (len * 2.5F);
            for (int m = 0; m < n; m++) {
                long h = (i * 7919L + m) * 0x9E3779B97F4A7C15L;
                float r1 = ((h >>> 11) & 0xFFFF) / 65535.0F, r2 = ((h >>> 27) & 0xFFFF) / 65535.0F, r3 = ((h >>> 43) & 0xFFFF) / 65535.0F;
                float d = ((r1 + t * (0.0025F + 0.003F * r2)) % 1.0F) * len;
                float ang = r3 * 6.283F + t * 0.02F * (r2 - 0.5F);
                float rad = 0.06F + 0.22F * r2 + 0.03F * (float) Math.sin(t * 0.07F + r1 * 20);
                float lift = 0.04F * (float) Math.sin(t * 0.05F + r3 * 30);
                Vector3f at = new Vector3f(a).add(new Vector3f(dir).mul(d))
                        .add(new Vector3f(pa).mul((float) Math.cos(ang) * rad)).add(new Vector3f(pb).mul((float) Math.sin(ang) * rad + lift));
                float tw = (float) Math.sin(t * (0.12F + 0.1F * r3) + r1 * 40);
                float alpha = 0.25F + 0.75F * tw * tw;
                float size = 0.035F + 0.03F * r3;
                SunbeamRender.sprite(b, p, at, new Vector3f(right).mul(size), new Vector3f(up).mul(size), 0, SunbeamRender.argb(alpha, MOTE));
            }
        }
    }
}
