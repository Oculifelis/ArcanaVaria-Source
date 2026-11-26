package com.arcanavaria.client;

import com.arcanavaria.ArcanaVaria;
import com.arcanavaria.lab.client.SunbeamRender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * Swing and whip effects drawn as real geometry (after Ostentum's trails and Ultima Ratio's beams): soft crescents that
 * sweep with the scythe, sparks, blooms, rings and the whip's jump to a second enemy. Every piece is drawn twice, once
 * blended in its primal colour so the colour stays deep in daylight, and once added on top for the glow. Effects live a
 * few ticks on the client and are never synced.
 */
@EventBusSubscriber(modid = ArcanaVaria.MODID, value = Dist.CLIENT)
public final class SwingFx {
    /** aer, ignis, aqua, terra, ordo, perditio, as in AVWhips. */
    public static final int[] PRIMALS = {0xFFFF7E, 0xFF5A01, 0x3CD4FC, 0x56C000, 0xD5D4EC, 0x404040};
    /** The colour laid along the Elemental Whip's cord as a halo (WhipGlowStyle), or -1 for none. */
    public static int cordRgb = -1;

    private interface Fx {
        float life();

        void draw(Mesh m, float age);
    }

    private record Live(Fx fx, double start) {
    }

    private static final List<Live> LIVE = new ArrayList<>();
    private static final RandomSource SEEDS = RandomSource.create();

    private SwingFx() {
    }

    /** The Elemental Whip's cord keeps the halo of the primal it last carried until this game time; 0 when unset. */
    private static long cordUntil;

    /** Draws what the server sent (fx/SwingFxPacket). */
    public static void play(com.arcanavaria.fx.SwingFxPacket p) {
        if (p.kind() == 0) {
            scythe(p.look(), p.primal(), p.a(), p.yaw(), p.points());
        } else if (p.kind() == 1 && !p.points().isEmpty()) {
            chain(p.look(), p.primal(), p.a(), p.points().get(0));
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (p.kind() == 1 && level != null) {
            cordRgb = PRIMALS[p.primal()];
            cordUntil = level.getGameTime() + 40;
        }
    }

    // ---------------------------------------------------------------- scythe

    /**
     * A scythe sweep around {@code centre} (chest height) facing {@code yawDeg}. Looks: 0 primal crescent, 1 prism sweep
     * (six bands), 2 crescent with an aspect ring closing on each hit.
     */
    public static void scythe(int look, int primal, Vec3 centre, float yawDeg, List<Vec3> hits) {
        double yaw = Math.toRadians(yawDeg);
        if (look == 1) {
            for (int b = 0; b < 6; b++) {
                int band = b;
                add(0.25 * b, 14F, (m, age) -> crescent(m, centre, yaw, 1.8 + 0.17 * band, 0.2, age, PRIMALS[band], band == 5, 0.85F));
            }
        } else {
            int rgb = look == 0 ? PRIMALS[primal] : 0xFFE9B0;
            add(0, 14F, (m, age) -> crescent(m, centre, yaw, 2.6, 1.1, age, rgb, true, 1.0F));
        }
        for (Vec3 hit : hits) {
            long seed = SEEDS.nextLong();
            int rgb = look == 1 ? PRIMALS[(int) Math.floorMod(seed, 6L)] : PRIMALS[primal];
            add(1.2, 9F, (m, age) -> burst(m, hit, age, rgb, seed, 1.0F));
            if (look == 2) {
                add(1.2, 12F, (m, age) -> aspectRing(m, hit, age));
            }
        }
    }

    /** The crescent's angles match ScytheEvents' sweep; it slants down from right to left like the swing. */
    private static Vec3 arcPoint(Vec3 c, double yaw, double f, double r) {
        double d = 1.1 - 2.2 * f, a = yaw + d;
        return new Vec3(c.x - Math.sin(a) * r, c.y + 0.32 * d, c.z + Math.cos(a) * r);
    }

    private static void crescent(Mesh m, Vec3 c, double yaw, double rOut, double thick, float age, int rgb, boolean edge, float strength) {
        float head = Math.min(1.0F, age / 2.2F);
        float tail = Mth.clamp((age - 3.0F) / 7.0F, 0.0F, 1.0F);
        float fade = strength * (age < 6.0F ? 1.0F : Math.max(0.0F, 1.0F - (age - 6.0F) / 6.0F));
        if (head <= tail || fade <= 0.0F) {
            return;
        }
        double r = rOut + age * 0.025;
        int n = 28;
        for (int i = 0; i < n; i++) {
            float f0 = Math.max(tail, i / (float) n), f1 = Math.min(head, (i + 1) / (float) n);
            if (f1 <= f0) {
                continue;
            }
            float a0 = fade * glowAlong(f0, head, tail), a1 = fade * glowAlong(f1, head, tail);
            double t0 = thick * Math.max(0.12, Math.pow(Math.sin(Math.PI * f0), 0.6));
            double t1 = thick * Math.max(0.12, Math.pow(Math.sin(Math.PI * f1), 0.6));
            Vec3 o0 = arcPoint(c, yaw, f0, r), o1 = arcPoint(c, yaw, f1, r);
            Vec3 i0 = arcPoint(c, yaw, f0, r - t0), i1 = arcPoint(c, yaw, f1, r - t1);
            m.quad("swing", false, o0, o1, i1, i0, f0 * 2, f1 * 2, 0, 1, SunbeamRender.argb(a0 * 0.55F, rgb), SunbeamRender.argb(a1 * 0.55F, rgb));
            m.quad("swing", true, o0, o1, i1, i0, f0 * 2, f1 * 2, 0, 1, SunbeamRender.argb(a0 * 0.9F, rgb), SunbeamRender.argb(a1 * 0.9F, rgb));
            if (edge) {
                Vec3 e0 = arcPoint(c, yaw, f0, r - t0 * 0.22), e1 = arcPoint(c, yaw, f1, r - t1 * 0.22);
                m.quad("swing", true, o0, o1, e1, e0, f0 * 2, f1 * 2, 0, 1, SunbeamRender.argb(a0 * 0.8F, 0xFFFFFF), SunbeamRender.argb(a1 * 0.8F, 0xFFFFFF));
            }
        }
        if (age < 3.5F) {
            Vec3 tip = arcPoint(c, yaw, head, r - thick * 0.15);
            m.sprite("glow", true, tip, 0.55F * fade, age, SunbeamRender.argb(0.8F * fade, rgb));
            m.sprite("glow", true, tip, 0.25F * fade, age, SunbeamRender.argb(0.9F * fade, 0xFFFFFF));
        }
    }

    /** Bright at the leading edge of the sweep, fading toward its tail. */
    private static float glowAlong(float f, float head, float tail) {
        float x = (f - tail) / Math.max(1.0E-3F, head - tail);
        return (float) Math.pow(Mth.clamp(x, 0.0F, 1.0F), 0.8);
    }

    /** A ring of the six primals lying flat round a hit target, closing in and spinning, then fading. */
    private static void aspectRing(Mesh m, Vec3 at, float age) {
        float grow = 1.0F - (float) Math.pow(1.0F - Math.min(1.0F, age / 4.0F), 3);
        float fade = age < 6.0F ? 1.0F : Math.max(0.0F, 1.0F - (age - 6.0F) / 6.0F);
        float r = 1.25F - 0.6F * grow;
        float spin = age * 0.22F;
        Vec3 right = new Vec3(Math.cos(spin) * r, 0, Math.sin(spin) * r), forward = new Vec3(-Math.sin(spin) * r, 0, Math.cos(spin) * r);
        m.flat("ring", false, at, right, forward, SunbeamRender.argb(0.45F * fade, 0xE0B850));
        m.flat("ring", true, at, right, forward, SunbeamRender.argb(0.85F * fade, 0xFFF0C0));
        for (int k = 0; k < 6; k++) {
            double a = spin + k * Math.PI / 3;
            Vec3 p = at.add(Math.cos(a) * r * 0.8, 0, Math.sin(a) * r * 0.8);
            m.sprite("glow", false, p, 0.22F, 0, SunbeamRender.argb(0.7F * fade, PRIMALS[k]));
            m.sprite("glow", true, p, 0.2F, 0, SunbeamRender.argb(0.9F * fade, PRIMALS[k]));
        }
    }

    // ---------------------------------------------------------------- whip

    /**
     * The whip's primal jumping from the first target to a second. Looks: 0 vis arc (a crackling arc), 1 seeker mote (a
     * bright mote flies over in an arc), 2 linked rings (a chain of glowing links snaps across).
     */
    public static void chain(int look, int primal, Vec3 from, Vec3 to) {
        int rgb = PRIMALS[primal];
        long seed = SEEDS.nextLong();
        add(0, 9F, (m, age) -> burst(m, from, age, rgb, seed, 1.0F));
        if (look == 0) {
            add(0.5, 10F, (m, age) -> visArc(m, from, to, age, rgb, seed));
            add(1.0, 9F, (m, age) -> burst(m, to, age, rgb, seed + 1, 0.9F));
        } else if (look == 1) {
            add(0.5, 12F, (m, age) -> seeker(m, from, to, age, rgb));
            add(5.5, 10F, (m, age) -> {
                burst(m, to, age, rgb, seed + 1, 1.2F);
                shockRing(m, to, age, rgb);
            });
        } else {
            add(0.5, 13F, (m, age) -> links(m, from, to, age, rgb));
            add(4.0, 9F, (m, age) -> burst(m, to, age, rgb, seed + 1, 0.9F));
        }
    }

    private static void visArc(Mesh m, Vec3 from, Vec3 to, float age, int rgb, long seed) {
        float t = age / 10.0F;
        float fade = t < 0.25F ? 1.0F : (float) Math.pow((1.0F - t) / 0.75F, 1.3);
        RandomSource r = RandomSource.create(seed * 7919L + (long) (age / 1.5F));
        fade *= 0.7F + 0.3F * r.nextFloat();
        List<Vec3> path = jag(from, to, from.distanceTo(to) * 0.14, 5, r);
        m.path("line", false, path, 0.9F, SunbeamRender.argb(0.5F * fade, rgb));
        m.path("line", true, path, 0.55F, SunbeamRender.argb(1.0F * fade, rgb));
        m.path("line", true, path, 0.16F, SunbeamRender.argb(1.0F * fade, 0xFFFFFF));
        for (int k = 0; k < 2; k++) {
            int i = 1 + r.nextInt(Math.max(1, path.size() - 3));
            Vec3 at = path.get(i);
            Vec3 dir = path.get(i + 1).subtract(at).normalize().add(r.nextDouble() - 0.5, r.nextDouble() - 0.3, r.nextDouble() - 0.5).normalize();
            List<Vec3> fork = jag(at, at.add(dir.scale(0.6 + r.nextDouble() * 0.6)), 0.12, 3, r);
            m.path("line", true, fork, 0.24F, SunbeamRender.argb(0.7F * fade, rgb));
        }
    }

    private static Vec3 arc(Vec3 from, Vec3 to, double f, double lift) {
        return from.lerp(to, f).add(0, Math.sin(f * Math.PI) * lift, 0);
    }

    private static void seeker(Mesh m, Vec3 from, Vec3 to, float age, int rgb) {
        float head = 1.0F - (float) Math.pow(1.0F - Math.min(1.0F, age / 5.0F), 2);
        float fade = age < 5.0F ? 1.0F : Math.max(0.0F, 1.0F - (age - 5.0F) / 4.0F);
        float tail = Math.max(0.0F, head - 0.5F) + Math.max(0.0F, age - 5.0F) * 0.12F;
        List<Vec3> pts = new ArrayList<>();
        List<Float> w = new ArrayList<>();
        int n = 16;
        for (int i = 0; i <= n; i++) {
            float f = tail + (head - tail) * i / n;
            pts.add(arc(from, to, f, 1.3));
            w.add(i / (float) n);
        }
        if (head > tail) {
            m.strip("line", false, pts, w, 0.45F, rgb, 0.45F * fade);
            m.strip("line", true, pts, w, 0.3F, rgb, 0.9F * fade);
            m.strip("line", true, pts, w, 0.08F, 0xFFFFFF, 0.8F * fade);
        }
        if (age < 5.5F) {
            Vec3 mote = arc(from, to, head, 1.3);
            m.sprite("glow", false, mote, 0.5F, 0, SunbeamRender.argb(0.6F, rgb));
            m.sprite("glow", true, mote, 0.55F, 0, SunbeamRender.argb(1.0F, rgb));
            m.sprite("glow", true, mote, 0.22F, 0, SunbeamRender.argb(1.0F, 0xFFFFFF));
        }
    }

    private static void shockRing(Mesh m, Vec3 at, float age, int rgb) {
        float grow = 1.0F - (float) Math.pow(1.0F - Math.min(1.0F, age / 6.0F), 2);
        float fade = Math.max(0.0F, 1.0F - age / 9.0F);
        float r = 0.3F + 1.3F * grow;
        m.sprite("ring", false, at, r, 0, SunbeamRender.argb(0.5F * fade, rgb));
        m.sprite("ring", true, at, r, 0, SunbeamRender.argb(0.9F * fade, rgb));
    }

    private static void links(Mesh m, Vec3 from, Vec3 to, float age, int rgb) {
        double len = from.distanceTo(to);
        int n = Math.max(4, (int) Math.round(len / 0.42));
        Vec3 up = new Vec3(0, 1, 0);
        float fadeAll = age < 7.0F ? 1.0F : Math.max(0.0F, 1.0F - (age - 7.0F) / 6.0F);
        for (int i = 0; i < n; i++) {
            float appear = Mth.clamp((age - i * 0.45F) / 1.2F, 0.0F, 1.0F);
            if (appear <= 0.0F) {
                break;
            }
            double f0 = i / (double) n, f1 = (i + 1) / (double) n, fm = (f0 + f1) * 0.5;
            Vec3 a = arc(from, to, f0, -0.35), b = arc(from, to, f1, -0.35);
            Vec3 centre = arc(from, to, fm, -0.35);
            Vec3 d = b.subtract(a).normalize();
            Vec3 p1 = d.cross(up);
            p1 = p1.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : p1.normalize();
            Vec3 p2 = d.cross(p1).normalize();
            Vec3 along = d.scale(len / n * 0.8), across = (i % 2 == 0 ? p1 : p2).scale(0.24);
            float pulse = 1.0F + 0.6F * (float) Math.exp(-Math.pow(i - (age - 1.0F) * 1.6F, 2) / 2.0);
            float alpha = appear * fadeAll;
            m.flat("ring", false, centre, along, across, SunbeamRender.argb(0.7F * alpha, rgb));
            m.flat("ring", true, centre, along, across, SunbeamRender.argb(Math.min(1.0F, alpha * pulse), rgb));
            m.sprite("glow", true, centre, 0.3F, 0, SunbeamRender.argb(0.5F * alpha, rgb));
            if (appear < 1.0F) {
                m.sprite("glow", true, centre, 0.35F, 0, SunbeamRender.argb(0.9F * (1.0F - appear), 0xFFFFFF));
            }
        }
    }

    // ---------------------------------------------------------------- shared

    /** A hit: a bloom that swells and fades, a white heart and sparks flung out and falling. */
    private static void burst(Mesh m, Vec3 at, float age, int rgb, long seed, float size) {
        float t = Math.min(1.0F, age / 8.0F);
        float fade = (float) Math.pow(1.0F - t, 1.5);
        float bloom = size * (0.45F + 0.75F * (1.0F - (float) Math.pow(1.0F - Math.min(1.0F, age / 4.0F), 2)));
        m.sprite("glow", false, at, bloom * 1.1F, 0, SunbeamRender.argb(0.5F * fade, rgb));
        m.sprite("glow", true, at, bloom, 0, SunbeamRender.argb(0.85F * fade, rgb));
        m.sprite("glow", true, at, size * 0.35F * (1.0F - t), 0, SunbeamRender.argb(fade, 0xFFFFFF));
        RandomSource r = RandomSource.create(seed);
        for (int k = 0; k < 9; k++) {
            Vec3 v = new Vec3(r.nextDouble() - 0.5, r.nextDouble() * 0.8 - 0.1, r.nextDouble() - 0.5).normalize().scale(0.16 + r.nextDouble() * 0.12);
            Vec3 p = at.add(v.scale(age)).add(0, -0.012 * age * age, 0);
            Vec3 back = v.add(0, -0.024 * age, 0).normalize().scale(0.18 * size);
            m.streak("glow", true, p.subtract(back), p, 0.05F * size, SunbeamRender.argb(0.9F * fade, k % 3 == 0 ? 0xFFFFFF : rgb));
        }
    }

    private static List<Vec3> jag(Vec3 from, Vec3 to, double spread, int depth, RandomSource r) {
        List<Vec3> pts = new ArrayList<>(List.of(from, to));
        double s = spread;
        for (int d = 0; d < depth; d++) {
            List<Vec3> next = new ArrayList<>();
            for (int i = 0; i < pts.size() - 1; i++) {
                Vec3 a = pts.get(i), b = pts.get(i + 1);
                next.add(a);
                next.add(a.lerp(b, 0.5).add((r.nextDouble() - 0.5) * s, (r.nextDouble() - 0.5) * s, (r.nextDouble() - 0.5) * s));
            }
            next.add(pts.get(pts.size() - 1));
            pts = next;
            s *= 0.55;
        }
        return pts;
    }

    private static void add(double delay, float life, java.util.function.BiConsumer<Mesh, Float> draw) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        LIVE.add(new Live(new Fx() {
            @Override
            public float life() {
                return life;
            }

            @Override
            public void draw(Mesh m, float age) {
                draw.accept(m, age);
            }
        }, level.getGameTime() + delay));
    }

    @SubscribeEvent
    public static void draw(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            LIVE.clear();
            return;
        }
        if (cordUntil > 0 && level.getGameTime() > cordUntil) {
            cordUntil = 0;
            cordRgb = -1;
        }
        if (LIVE.isEmpty()) {
            return;
        }
        double now = level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        LIVE.removeIf(l -> now - l.start > l.fx.life() || now < l.start - 200);
        Mesh mesh = new Mesh(event.getLevelRenderState().cameraRenderState.pos);
        for (Live l : LIVE) {
            if (now >= l.start) {
                l.fx.draw(mesh, (float) (now - l.start));
            }
        }
        mesh.submit(event);
    }

    /** Camera-relative quads grouped by texture, blended ones first. */
    private static final class Mesh {
        private final Vec3 cam;
        private final Map<String, List<float[]>> quads = new LinkedHashMap<>();

        Mesh(Vec3 cam) {
            this.cam = cam;
            for (String t : new String[] {"swing", "glow", "ring", "line"}) {
                quads.put("tint:" + t, new ArrayList<>());
            }
        }

        /** a-b along U (u0 to u1) at V v0, d-c at V v1; colours ca at a and d, cb at b and c. */
        void quad(String tex, boolean add, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float u0, float u1, float v0, float v1, int ca, int cb) {
            float[] q = new float[24];
            put(q, 0, a, u0, v0, ca);
            put(q, 1, b, u1, v0, cb);
            put(q, 2, c, u1, v1, cb);
            put(q, 3, d, u0, v1, ca);
            quads.computeIfAbsent((add ? "add:" : "tint:") + tex, k -> new ArrayList<>()).add(q);
        }

        private void put(float[] q, int i, Vec3 p, float u, float v, int argb) {
            q[i * 6] = (float) (p.x - cam.x);
            q[i * 6 + 1] = (float) (p.y - cam.y);
            q[i * 6 + 2] = (float) (p.z - cam.z);
            q[i * 6 + 3] = u;
            q[i * 6 + 4] = v;
            q[i * 6 + 5] = Float.intBitsToFloat(argb);
        }

        /** A square facing the camera, {@code half} to each side, turned by {@code spin}. */
        void sprite(String tex, boolean add, Vec3 at, float half, float spin, int argb) {
            Vec3 view = at.subtract(cam);
            view = view.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : view.normalize();
            Vec3 right = view.cross(new Vec3(0, 1, 0));
            right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
            Vec3 up = right.cross(view).normalize();
            double c = Math.cos(spin), s = Math.sin(spin);
            Vec3 r = right.scale(c).add(up.scale(s)).scale(half), u = up.scale(c).subtract(right.scale(s)).scale(half);
            flat(tex, add, at, r, u, argb);
        }

        /** A quad spanning {@code right} and {@code up} (already scaled) round {@code at}. */
        void flat(String tex, boolean add, Vec3 at, Vec3 right, Vec3 up, int argb) {
            quad(tex, add, at.subtract(right).subtract(up), at.add(right).subtract(up), at.add(right).add(up), at.subtract(right).add(up), 0, 1, 1, 0, argb, argb);
        }

        /** A thin camera-facing quad from a to b, stretched along its length. */
        void streak(String tex, boolean add, Vec3 a, Vec3 b, float half, int argb) {
            Vec3 along = b.subtract(a), side = along.cross(a.subtract(cam));
            if (side.lengthSqr() < 1.0E-10) {
                return;
            }
            side = side.normalize().scale(half);
            Vec3 ext = along.normalize().scale(half);
            Vec3 a2 = a.subtract(ext), b2 = b.add(ext);
            quad(tex, add, a2.subtract(side), b2.subtract(side), b2.add(side), a2.add(side), 0, 1, 1, 0, argb, argb);
        }

        /** A camera-facing band of even width along {@code pts}, the texture's U running across it. */
        void path(String tex, boolean add, List<Vec3> pts, float width, int argb) {
            List<Float> w = new ArrayList<>();
            for (int i = 0; i < pts.size(); i++) {
                w.add(1.0F);
            }
            strip(tex, add, pts, w, width, argb & 0xFFFFFF, (argb >>> 24) / 255.0F);
        }

        /** A camera-facing band along {@code pts}; {@code w} (0-1) scales the width and alpha at each point. */
        void strip(String tex, boolean add, List<Vec3> pts, List<Float> w, float width, int rgb, float alpha) {
            int n = pts.size();
            for (int i = 0; i < n - 1; i++) {
                Vec3 a = pts.get(i), b = pts.get(i + 1);
                Vec3 sa = side(pts, i).scale(width * 0.5 * w.get(i)), sb = side(pts, i + 1).scale(width * 0.5 * w.get(i + 1));
                int ca = SunbeamRender.argb(alpha * w.get(i), rgb), cb = SunbeamRender.argb(alpha * w.get(i + 1), rgb);
                float[] q = new float[24];
                put(q, 0, a.subtract(sa), 0, 0, ca);
                put(q, 1, a.add(sa), 1, 0, ca);
                put(q, 2, b.add(sb), 1, 1, cb);
                put(q, 3, b.subtract(sb), 0, 1, cb);
                quads.computeIfAbsent((add ? "add:" : "tint:") + tex, k -> new ArrayList<>()).add(q);
            }
        }

        private Vec3 side(List<Vec3> pts, int i) {
            Vec3 tangent = pts.get(Math.min(pts.size() - 1, i + 1)).subtract(pts.get(Math.max(0, i - 1)));
            Vec3 s = tangent.cross(pts.get(i).subtract(cam));
            return s.lengthSqr() < 1.0E-10 ? new Vec3(0, 1, 0) : s.normalize();
        }

        void submit(SubmitCustomGeometryEvent event) {
            for (Map.Entry<String, List<float[]>> e : quads.entrySet()) {
                List<float[]> list = e.getValue();
                if (list.isEmpty()) {
                    continue;
                }
                String key = e.getKey();
                boolean add = key.startsWith("add:");
                String tex = key.substring(key.indexOf(':') + 1);
                event.getSubmitNodeCollector().submitCustomGeometry(event.getPoseStack(), SunbeamRender.fx(tex, add), (pose, buffer) -> emit(pose, buffer, list));
            }
        }

        private static void emit(PoseStack.Pose pose, VertexConsumer buffer, List<float[]> list) {
            for (float[] q : list) {
                for (int i = 0; i < 4; i++) {
                    buffer.addVertex(pose, q[i * 6], q[i * 6 + 1], q[i * 6 + 2]).setColor(Float.floatToRawIntBits(q[i * 6 + 5]))
                        .setUv(q[i * 6 + 3], q[i * 6 + 4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
                        .setNormal(pose, 0.0F, 1.0F, 0.0F);
                }
            }
        }
    }
}
