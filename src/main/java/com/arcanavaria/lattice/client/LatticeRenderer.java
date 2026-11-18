package com.arcanavaria.lattice.client;

import com.arcanavaria.lattice.AspectChemistry;
import com.arcanavaria.lattice.LatticeBlockEntity;
import com.arcanavaria.lattice.Mote;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the motes in a lattice block: a bright core in the aspect's colour inside a fainter glow, gliding from the face
 * it came in through to the centre and on to the face it leaves by. A mote waiting at the centre pulses.
 */
public class LatticeRenderer implements BlockEntityRenderer<LatticeBlockEntity, LatticeRenderer.State> {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final RenderType GLOW = RenderTypes.entityTranslucentEmissive(Identifier.withDefaultNamespace("textures/misc/white.png"));

    public static class State extends BlockEntityRenderState {
        final List<double[]> motes = new ArrayList<>();
    }

    public LatticeRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LatticeBlockEntity be, State s, float partial, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, s, partial, camera, breakProgress);
        s.motes.clear();
        if (be.getLevel() == null) {
            return;
        }
        for (Mote m : be.packets()) {
            float t = Math.min(m.age + (m.waiting() ? 0 : partial), Mote.TRAVEL);
            Vec3 at;
            if (t < Mote.CENTRE) {
                at = lerp(face(m.from), CENTRE, t / Mote.CENTRE);
            } else if (m.to == null) {
                at = CENTRE;
            } else {
                at = lerp(CENTRE, face(m.to), (t - Mote.CENTRE) / (Mote.TRAVEL - Mote.CENTRE));
            }
            double pulse = m.waiting() ? 1 + 0.25 * Math.sin((m.held + partial) * 0.6) : 1;
            s.motes.add(new double[] {at.x, at.y, at.z, pulse, AspectChemistry.colour(be.getLevel(), m.aspect)});
        }
    }

    private static final Vec3 CENTRE = new Vec3(0.5, 0.5, 0.5);

    private static Vec3 face(@Nullable Direction d) {
        return d == null ? CENTRE : CENTRE.add(d.getStepX() * 0.5, d.getStepY() * 0.5, d.getStepZ() * 0.5);
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return a.add(b.subtract(a).scale(t));
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (s.motes.isEmpty()) {
            return;
        }
        collector.submitCustomGeometry(pose, GLOW, (p, vc) -> {
            for (double[] m : s.motes) {
                int rgb = (int) m[4];
                double core = 0.07 * m[3], halo = 0.13 * m[3];
                box(vc, p, m[0], m[1], m[2], core, 0xF0000000 | rgb);
                box(vc, p, m[0], m[1], m[2], halo, 0x55000000 | rgb);
            }
        });
    }

    private static void box(VertexConsumer vc, PoseStack.Pose p, double cx, double cy, double cz, double r, int colour) {
        double x0 = cx - r, y0 = cy - r, z0 = cz - r, x1 = cx + r, y1 = cy + r, z1 = cz + r;
        Vec3[] c = {new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), new Vec3(x1, y0, z1), new Vec3(x0, y0, z1),
                new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1)};
        int[][] faces = {{0, 1, 2, 3}, {4, 7, 6, 5}, {0, 4, 5, 1}, {2, 6, 7, 3}, {1, 5, 6, 2}, {3, 7, 4, 0}};
        for (int[] f : faces) {
            // both windings, so a face shows from inside the glow as well
            vertex(vc, p, c[f[0]], 0, 0, colour);
            vertex(vc, p, c[f[1]], 1, 0, colour);
            vertex(vc, p, c[f[2]], 1, 1, colour);
            vertex(vc, p, c[f[3]], 0, 1, colour);
            vertex(vc, p, c[f[3]], 0, 1, colour);
            vertex(vc, p, c[f[2]], 1, 1, colour);
            vertex(vc, p, c[f[1]], 1, 0, colour);
            vertex(vc, p, c[f[0]], 0, 0, colour);
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, Vec3 at, float u, float v, int colour) {
        vc.addVertex(p, (float) at.x, (float) at.y, (float) at.z).setColor(colour).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT).setNormal(p, 0, 1, 0);
    }
}
