package com.arcanavaria.client;

import com.arcanavaria.ArcanaVaria;
import com.fundamentum.whip.WhipStyle;
import com.fundamentum.whip.WhipsClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;

/**
 * Glowing overlays on whip cords (after Ostentum's WhipGlow), laid over Fundamentum's own ribbon. The Elemental Whip's
 * whole cord is unlit through Fundamentum's {@code cordFx(GLOW)} (AVWhips; Oculifelis: "elemental whip whip part must be
 * emissive" - drawing it ourselves with a full-bright light value still went through the lightmap and looked dim at
 * night), and beads of primal light flow over it from handle to tip. The Amberbark Lash lays its amber over its cord.
 * Overlays sit a hair nearer the camera than the cord so they never z-fight it. The elemental bead texture is 64 rows, periodic
 * over 32, so the ribbon reads half of it and scrolls its offset through the other half.
 */
final class WhipGlowStyle implements WhipStyle {
    private static final long FLOW_MILLIS = 1200;
    private static boolean logged;

    private final Identifier glow;
    private final boolean flows;

    private WhipGlowStyle(Identifier glow, boolean flows) {
        this.glow = glow;
        this.flows = flows;
    }

    static void register() {
        WhipsClient.style(ArcanaVaria.id("amberbark_lash"), new WhipGlowStyle(tex("amberbark_lash_glow"), false));
        WhipsClient.style(ArcanaVaria.id("elemental_whip"), new WhipGlowStyle(tex("elemental_whip_glow"), true));
        ArcanaVaria.LOG.info("Arcana Varia whip glow styles registered");
    }

    private static Identifier tex(String name) {
        return ArcanaVaria.id("textures/entity/whip/" + name + ".png");
    }

    @Override
    public void draw(Cord cord, PoseStack pose, SubmitNodeCollector collector) {
        if (!logged) {
            logged = true;
            ArcanaVaria.LOG.info("Arcana Varia whip glow drawn for {}", cord.whip().id);
        }
        float offset = this.flows ? 0.5F - (Util.getMillis() % FLOW_MILLIS) / (float) FLOW_MILLIS * 0.5F : 0.0F;
        float scale = this.flows ? 0.5F : 1.0F;
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(this.glow, false),
            (at, buffer) -> ribbon(at, buffer, cord, offset, scale, 0.995));
    }

    /** The camera-facing strip Fundamentum draws (u across, v from handle to tip, tapering), pulled `near` toward the camera. */
    private static void ribbon(PoseStack.Pose at, VertexConsumer buffer, Cord cord, float offset, float scale, double near) {
        Vec3[] p = cord.points();
        int n = p.length;
        Vec3[] left = new Vec3[n], right = new Vec3[n];
        for (int i = 0; i < n; i++) {
            Vec3 tangent = p[Math.min(n - 1, i + 1)].subtract(p[Math.max(0, i - 1)]);
            Vec3 side = tangent.cross(p[i].scale(-1));
            side = side.lengthSqr() < 1.0E-10 ? new Vec3(0, 1, 0) : side.normalize();
            double half = Mth.lerp(cord.along(i), cord.whip().width, cord.whip().tipWidth) * 0.5;
            Vec3 at0 = p[i].scale(near);
            left[i] = at0.add(side.scale(half));
            right[i] = at0.subtract(side.scale(half));
        }
        for (int i = 0; i < n - 1; i++) {
            float v0 = offset + (float) cord.along(i) * scale, v1 = offset + (float) cord.along(i + 1) * scale;
            vertex(at, buffer, left[i], 0, v0);
            vertex(at, buffer, right[i], 1, v0);
            vertex(at, buffer, right[i + 1], 1, v1);
            vertex(at, buffer, left[i + 1], 0, v1);
        }
    }

    private static void vertex(PoseStack.Pose at, VertexConsumer buffer, Vec3 p, float u, float v) {
        buffer.addVertex(at, (float) p.x, (float) p.y, (float) p.z).setColor(0xFFFFFFFF).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(at, 0, 1, 0);
    }
}
