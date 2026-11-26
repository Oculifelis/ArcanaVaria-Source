package com.arcanavaria.lab.client;

import com.arcanavaria.ArcanaVaria;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

/**
 * Additive, fullbright, double-sided textured quads for the Sun Lens beam and the weapon effects, plus a blended twin
 * ({@link #TINT}) drawn under the added light so colours stay deep on a bright sky.
 */
public final class SunbeamRender {
    public static final RenderPipeline GLOW = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(ArcanaVaria.id("pipeline/sunbeam"))
            .withVertexShader("core/entity")
            .withFragmentShader("core/entity")
            .withShaderDefine("EMISSIVE")
            .withShaderDefine("NO_OVERLAY")
            .withShaderDefine("NO_CARDINAL_LIGHTING")
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build();
    public static final RenderPipeline TINT = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(ArcanaVaria.id("pipeline/fx_tint"))
            .withVertexShader("core/entity")
            .withFragmentShader("core/entity")
            .withShaderDefine("EMISSIVE")
            .withShaderDefine("NO_OVERLAY")
            .withShaderDefine("NO_CARDINAL_LIGHTING")
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build();

    private static final Map<String, RenderType> TYPES = new HashMap<>();
    private static final int FULL_BRIGHT = 0xF000F0;

    private SunbeamRender() {
    }

    public static RenderType type(String texture) {
        return TYPES.computeIfAbsent(texture, t -> RenderType.create("arcanavaria_sunbeam",
                RenderSetup.builder(GLOW).withTexture("Sampler0", ArcanaVaria.id("textures/misc/sunbeam_" + t + ".png")).sortOnUpload().createRenderSetup()));
    }

    /** textures/misc/fx_{texture}.png, added ({@code add}) or blended. */
    public static RenderType fx(String texture, boolean add) {
        return TYPES.computeIfAbsent((add ? "add:" : "tint:") + texture, t -> RenderType.create(add ? "arcanavaria_fx" : "arcanavaria_fx_tint",
                RenderSetup.builder(add ? GLOW : TINT).withTexture("Sampler0", ArcanaVaria.id("textures/misc/fx_" + texture + ".png")).sortOnUpload().createRenderSetup()));
    }

    public static int argb(float alpha, int rgb) {
        return (Math.max(0, Math.min(255, Math.round(alpha * 255))) << 24) | (rgb & 0xFFFFFF);
    }

    /** A ribbon from a to c, {@code side} to each side, with U running along it. */
    public static void ribbon(VertexConsumer b, PoseStack.Pose p, Vector3f a, Vector3f c, Vector3f side, float u0, float u1, int argb) {
        v(b, p, a.x - side.x, a.y - side.y, a.z - side.z, u0, 1, argb);
        v(b, p, c.x - side.x, c.y - side.y, c.z - side.z, u1, 1, argb);
        v(b, p, c.x + side.x, c.y + side.y, c.z + side.z, u1, 0, argb);
        v(b, p, a.x + side.x, a.y + side.y, a.z + side.z, u0, 0, argb);
    }

    /** A square sprite at {@code at}, spanning {@code right} and {@code up} (already scaled), turned by {@code spin}. */
    public static void sprite(VertexConsumer b, PoseStack.Pose p, Vector3f at, Vector3f right, Vector3f up, float spin, int argb) {
        float c = (float) Math.cos(spin), s = (float) Math.sin(spin);
        Vector3f r = new Vector3f(right).mul(c).add(new Vector3f(up).mul(s));
        Vector3f u = new Vector3f(up).mul(c).sub(new Vector3f(right).mul(s));
        v(b, p, at.x - r.x - u.x, at.y - r.y - u.y, at.z - r.z - u.z, 0, 1, argb);
        v(b, p, at.x + r.x - u.x, at.y + r.y - u.y, at.z + r.z - u.z, 1, 1, argb);
        v(b, p, at.x + r.x + u.x, at.y + r.y + u.y, at.z + r.z + u.z, 1, 0, argb);
        v(b, p, at.x - r.x + u.x, at.y - r.y + u.y, at.z - r.z + u.z, 0, 0, argb);
    }

    private static void v(VertexConsumer b, PoseStack.Pose p, float x, float y, float z, float u, float v, int argb) {
        b.addVertex(p, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(p, 0.0F, 1.0F, 0.0F);
    }
}
