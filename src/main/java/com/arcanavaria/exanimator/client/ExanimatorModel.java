package com.arcanavaria.exanimator.client;

import com.arcanavaria.exanimator.Exanimator;
import com.arcanavaria.exanimator.ExanimatorState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The cube model (geometry written into {@code client/gen/ExanimatorLayer} by tools/bossmodels/export_exanimator.py)
 * with procedural animation: every pose is a function of the synced state and the ticks since it began, laid on top of
 * the rest pose. Timings match the server's moves in {@link Exanimator}.
 *
 * <p>Rig: root > body > (skirt > tent0..3 chains, chest > (ribs, humps, head, arm_lo_l/r > fore > hand, arm_hi_r >
 * fore > hand, arm_hi_l > fore, tent4..6 chains), scythe). Each tentacle chain has 7 segments, _0 to _6.
 */
public class ExanimatorModel extends EntityModel<ExanimatorRenderState> {
    private static final int SEGS = 7;
    private final ModelPart body, skirt, chest, head, scythe;
    private final ModelPart armLoL, armLoR, foreLoL, foreLoR, armHiL, armHiR, foreHiL, foreHiR;
    private final ModelPart humpL, humpR;
    private final ModelPart[] ribL = new ModelPart[5], ribR = new ModelPart[5], ribLb = new ModelPart[5], ribRb = new ModelPart[5];
    /** tent[k][j]: 0-3 hang from the skirt, 4-6 rise from the back. */
    private final ModelPart[][] tent = new ModelPart[7][SEGS];

    public ExanimatorModel(ModelPart root) {
        super(root);
        this.body = root.getChild("root").getChild("body");
        this.skirt = this.body.getChild("skirt");
        this.chest = this.body.getChild("chest");
        this.scythe = this.body.getChild("scythe");
        this.head = this.chest.getChild("head");
        this.humpL = this.chest.getChild("hump_l");
        this.humpR = this.chest.getChild("hump_r");
        this.armLoL = this.chest.getChild("arm_lo_l");
        this.armLoR = this.chest.getChild("arm_lo_r");
        this.foreLoL = this.armLoL.getChild("fore_lo_l");
        this.foreLoR = this.armLoR.getChild("fore_lo_r");
        this.armHiL = this.chest.getChild("arm_hi_l");
        this.armHiR = this.chest.getChild("arm_hi_r");
        this.foreHiL = this.armHiL.getChild("fore_hi_l");
        this.foreHiR = this.armHiR.getChild("fore_hi_r");
        for (int i = 0; i < 5; i++) {
            this.ribL[i] = this.chest.getChild("rib" + i + "l");
            this.ribR[i] = this.chest.getChild("rib" + i + "r");
            this.ribLb[i] = this.ribL[i].getChild("rib" + i + "lb");
            this.ribRb[i] = this.ribR[i].getChild("rib" + i + "rb");
        }
        for (int k = 0; k < 7; k++) {
            ModelPart p = k < 4 ? this.skirt.getChild("tent" + k + "_0") : this.chest.getChild("tent" + k + "_0");
            this.tent[k][0] = p;
            for (int j = 1; j < SEGS; j++) {
                p = p.getChild("tent" + k + "_" + j);
                this.tent[k][j] = p;
            }
        }
    }

    /** Smooth 0..1 between a and b. */
    private static float ramp(float t, float a, float b) {
        float k = Mth.clamp((t - a) / (b - a), 0.0F, 1.0F);
        return k * k * (3.0F - 2.0F * k);
    }

    /** Rises over [a, b], holds, falls over [c, d]. */
    private static float hold(float t, float a, float b, float c, float d) {
        return ramp(t, a, b) * (1.0F - ramp(t, c, d));
    }

    @Override
    public void setupAnim(ExanimatorRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float t = s.stateTime;
        ExanimatorState st = s.state;
        boolean dying = s.dyingTime > 0.0F;
        float speed = s.phaseTwo ? 1.35F : 1.0F;

        // ---- always: hover bob, breathing ribs, tentacles that never stop moving, a slow lean into the walk
        float sp = Math.min(1.0F, s.walkAnimationSpeed * 1.5F);
        float wp = s.walkAnimationPos * 0.35F;
        this.body.y += Mth.sin(age * 0.06F) * 1.2F - Math.abs(Mth.sin(wp)) * 1.0F * sp;
        this.body.xRot += 0.12F * sp;
        this.chest.xRot += 0.03F * Mth.sin(age * 0.07F);
        this.chest.zRot += Mth.sin(wp) * 0.05F * sp;
        float breath = 0.06F * Mth.sin(age * 0.07F);
        for (int i = 0; i < 5; i++) {
            this.ribL[i].yRot += breath;
            this.ribR[i].yRot -= breath;
        }
        this.humpL.zRot -= 0.04F * Mth.sin(age * 0.05F);
        this.humpR.zRot += 0.04F * Mth.sin(age * 0.05F + 0.7F);
        for (int k = 0; k < 7; k++) {
            boolean back = k >= 4;
            float amp = (back ? 0.09F : 0.12F) * speed;
            float rate = back ? 0.07F : 0.09F + 0.05F * sp;
            for (int j = 1; j < SEGS; j++) {
                float ph = age * rate * speed - j * 0.55F + k * 1.7F;
                float grow = 0.6F + 0.12F * j;                   // the tips whip more than the roots
                this.tent[k][j].xRot += Mth.sin(ph) * amp * grow;
                this.tent[k][j].yRot += Mth.cos(ph * 0.8F) * amp * 0.8F * grow;
            }
            this.tent[k][0].xRot += Mth.sin(age * 0.05F + k) * 0.05F;
        }
        // lower arms swing a little; the scythe arm keeps its grip
        this.armLoL.xRot -= Mth.cos(wp) * 0.25F * sp + 0.05F * Mth.sin(age * 0.06F);
        this.armLoR.xRot += Mth.cos(wp) * 0.25F * sp - 0.05F * Mth.sin(age * 0.06F + 1.0F);
        this.armHiR.xRot += 0.06F * Mth.sin(age * 0.05F + 2.0F);

        // head follows the target
        if (!dying && st != ExanimatorState.START && st != ExanimatorState.STUN) {
            this.head.yRot += Mth.clamp(s.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD * 0.6F;
            this.head.xRot += Mth.clamp(s.xRot, -25.0F, 30.0F) * Mth.DEG_TO_RAD * 0.5F;
        }

        switch (st) {
            case START -> this.start(t);
            case REAP -> this.reap(t, s.phaseTwo);
            case HARVEST -> this.harvest(t);
            case LASH -> this.lash(t);
            case STEP -> this.step(t);
            case MAW -> this.maw(t, age);
            case STUN -> this.stun(t, age);
            case ECLIPSE -> this.eclipse(t, age);
            default -> { }
        }
        if (dying) {
            this.die(s.dyingTime);
        }
    }

    /** Claws its way up out of the floor, ribs closed, then spreads everything and screams. */
    private void start(float t) {
        float k = ramp(t, 0.0F, 58.0F);
        this.body.y += (1.0F - k) * 100.0F;
        this.chest.xRot += (1.0F - k) * 0.7F;
        this.head.xRot += (1.0F - k) * 0.5F;
        float spread = hold(t, 56.0F, 64.0F, 72.0F, 80.0F);
        this.head.xRot -= spread * 0.5F;
        this.chest.xRot -= spread * 0.2F;
        this.armLoL.zRot -= spread * 0.6F;
        this.armLoR.zRot += spread * 0.6F;
        this.armHiR.zRot += spread * 0.3F;
        this.ribsOpen(spread * 0.5F);
        for (int i = 0; i < 7; i++) {
            this.tent[i][0].xRot -= spread * (i >= 4 ? 0.5F : 0.3F);
        }
    }

    /** Overhead scythe chop at t16; phase two adds a backhand at t26. */
    private void reap(float t, boolean two) {
        float wind = hold(t, 0.0F, 12.0F, 14.0F, 17.0F);
        float hit = ramp(t, 14.0F, 17.0F) * (1.0F - ramp(t, two ? 22.0F : 24.0F, two ? 26.0F : 34.0F));
        // wind-up: scythe swept back over the shoulder, body coils
        this.scythe.zRot -= wind * 0.6F;
        this.scythe.xRot += wind * 0.35F;
        this.scythe.y -= wind * 6.0F;
        this.armHiL.zRot -= wind * 0.45F;
        this.chest.yRot += wind * 0.35F;
        this.chest.xRot -= wind * 0.12F;
        this.head.xRot -= wind * 0.1F;
        // the chop
        this.scythe.zRot += hit * 1.35F;
        this.scythe.x -= hit * 6.0F;
        this.scythe.y -= hit * 14.0F;
        this.scythe.z -= hit * 9.0F;
        this.armHiL.zRot += hit * 1.2F;
        this.armHiR.zRot -= hit * 0.5F;
        this.chest.xRot += hit * 0.15F;
        this.chest.yRot -= hit * 0.25F;
        this.head.xRot -= hit * 0.1F;
        this.head.yRot += hit * 0.2F;
        if (two) {                                   // backhand: rip it back across
            float back = hold(t, 22.0F, 26.0F, 29.0F, 34.0F);
            this.scythe.zRot += back * 0.4F;
            this.scythe.yRot += back * 0.9F;
            this.scythe.x -= back * 4.0F;
            this.chest.yRot += back * 0.45F;
            this.armHiL.zRot += back * 0.3F;
        }
    }

    /** Arms flung wide while it drags you in, then a full spin at t26. */
    private void harvest(float t) {
        float pull = hold(t, 0.0F, 8.0F, 22.0F, 26.0F);
        this.armLoL.zRot -= pull * 0.7F;
        this.armLoR.zRot += pull * 0.7F;
        this.armLoL.xRot -= pull * 0.6F;
        this.armLoR.xRot -= pull * 0.6F;
        this.armHiR.zRot += pull * 0.3F;
        this.chest.xRot -= pull * 0.15F;
        this.head.xRot -= pull * 0.25F;
        this.ribsOpen(pull * 0.25F);
        for (int k = 0; k < 4; k++) {               // skirt tentacles splay out
            this.tent[k][0].xRot -= pull * 0.5F;
        }
        float spin = ramp(t, 23.0F, 30.0F);
        this.body.yRot += spin * Mth.TWO_PI;
        float swing = hold(t, 22.0F, 25.0F, 30.0F, 40.0F);
        this.scythe.zRot += swing * 1.0F;
        this.scythe.y -= swing * 8.0F;
        this.armHiL.zRot += swing * 0.8F;
    }

    /** The three back tentacles coil up over its shoulders and spear forward at t19. */
    private void lash(float t) {
        float coil = hold(t, 0.0F, 14.0F, 17.0F, 19.0F);
        float strike = hold(t, 17.0F, 19.0F, 26.0F, 36.0F);
        for (int k = 4; k < 7; k++) {
            this.tent[k][0].xRot -= coil * 0.4F;
            this.tent[k][0].xRot += strike * 1.3F;
            for (int j = 1; j < SEGS; j++) {
                this.tent[k][j].xRot -= coil * 0.32F;       // curl tight
                this.tent[k][j].xRot += strike * 0.12F;     // snap straight
                this.tent[k][j].yRot *= 1.0F - strike;
            }
        }
        this.chest.xRot += strike * 0.3F - coil * 0.12F;
        this.head.xRot += strike * 0.15F;
        this.humpL.zRot -= strike * 0.2F;
        this.humpR.zRot += strike * 0.2F;
        this.armLoL.zRot -= coil * 0.3F;
        this.armLoR.zRot += coil * 0.3F;
    }

    /** Folds in on itself as it fades (t0-12), steps out of the smoke (t20) and cuts at t25. */
    private void step(float t) {
        float fold = hold(t, 0.0F, 10.0F, 18.0F, 22.0F);
        this.chest.xRot += fold * 0.5F;
        this.head.xRot += fold * 0.4F;
        this.armLoL.zRot += fold * 0.3F;
        this.armLoR.zRot -= fold * 0.3F;
        this.body.y += fold * 8.0F;
        for (int k = 0; k < 7; k++) {
            this.tent[k][0].xRot += fold * 0.4F;
        }
        float cut = hold(t, 21.0F, 25.0F, 28.0F, 38.0F);
        this.scythe.zRot += cut * 1.1F;
        this.scythe.yRot -= cut * 0.6F;
        this.scythe.y -= cut * 10.0F;
        this.scythe.z -= cut * 7.0F;
        this.armHiL.zRot += cut * 0.9F;
        this.chest.yRot -= cut * 0.4F;
    }

    /** The ribs swing open like doors around the void and it leans back into the pull; snaps shut at t60. */
    private void maw(float t, float age) {
        float open = hold(t, 0.0F, 14.0F, 58.0F, 62.0F);
        float shake = open * 0.02F * Mth.sin(age * 1.7F);
        this.ribsOpen(open + shake * 5.0F);
        this.chest.xRot -= open * 0.25F;
        this.head.xRot -= open * 0.35F;
        this.armLoL.zRot -= open * 0.55F;
        this.armLoR.zRot += open * 0.55F;
        this.armHiR.zRot += open * 0.25F;
        this.humpL.zRot -= open * 0.25F;
        this.humpR.zRot += open * 0.25F;
        for (int k = 0; k < 7; k++) {
            this.tent[k][0].xRot -= open * (k >= 4 ? 0.6F : 0.35F);
            for (int j = 1; j < SEGS; j++) {
                this.tent[k][j].xRot += open * 0.05F * Mth.sin(age * 0.5F + j + k);
            }
        }
        float snap = hold(t, 60.0F, 62.0F, 64.0F, 70.0F);
        this.chest.xRot += snap * 0.35F;
        this.head.xRot += snap * 0.3F;
    }

    /** Light-blind: reels, sags, tentacles go limp, the head shakes. */
    private void stun(float t, float age) {
        float k = hold(t, 0.0F, 5.0F, 42.0F, 50.0F);
        this.chest.xRot += k * 0.45F;
        this.chest.zRot += k * 0.15F * Mth.sin(age * 0.3F);
        this.head.xRot += k * 0.35F;
        this.head.yRot += k * 0.35F * Mth.sin(age * 0.45F);
        this.armLoL.zRot += k * 0.35F;
        this.armLoR.zRot -= k * 0.35F;
        this.armLoL.xRot -= k * 0.5F;               // hands up against the light
        this.armLoR.xRot -= k * 0.5F;
        this.body.y += k * 6.0F;
        for (int i = 0; i < 7; i++) {
            this.tent[i][0].xRot += k * 0.4F;
            for (int j = 1; j < SEGS; j++) {
                this.tent[i][j].xRot *= 1.0F - 0.7F * k;
                this.tent[i][j].yRot *= 1.0F - 0.7F * k;
            }
        }
    }

    /** Doubles over, then rears up, ribs and arms flung open, at t10 when the light dies. */
    private void eclipse(float t, float age) {
        float crouch = hold(t, 0.0F, 8.0F, 9.0F, 12.0F);
        float rear = hold(t, 9.0F, 13.0F, 45.0F, 60.0F);
        this.chest.xRot += crouch * 0.5F - rear * 0.35F;
        this.head.xRot += crouch * 0.4F - rear * 0.6F;
        this.body.y += crouch * 6.0F - rear * 4.0F;
        this.ribsOpen(rear * 0.8F);
        this.armLoL.zRot -= rear * 0.9F;
        this.armLoR.zRot += rear * 0.9F;
        this.armHiR.zRot += rear * 0.4F;
        this.armHiL.zRot -= rear * 0.3F;
        for (int k = 0; k < 7; k++) {
            this.tent[k][0].xRot -= rear * (k >= 4 ? 0.8F : 0.5F);
            for (int j = 1; j < SEGS; j++) {
                this.tent[k][j].xRot += rear * 0.08F * Mth.sin(age * 0.9F + j * 0.7F + k);
            }
        }
    }

    /** Sags, tips forward and sinks into the floor; the scythe falls away. */
    private void die(float d) {
        float k = ramp(d, 0.0F, 25.0F);
        float sink = ramp(d, 15.0F, 60.0F);
        this.chest.xRot += k * 0.6F;
        this.head.xRot += k * 0.5F;
        this.armLoL.zRot += k * 0.5F;
        this.armLoR.zRot -= k * 0.5F;
        this.armHiR.zRot -= k * 0.8F;
        this.armHiL.zRot += k * 0.8F;
        this.scythe.zRot -= k * 0.9F;
        this.scythe.y += k * 16.0F;
        this.ribsOpen(k * 0.6F);
        this.body.y += sink * 90.0F;
        for (int i = 0; i < 7; i++) {
            this.tent[i][0].xRot += k * 0.5F;
            for (int j = 1; j < SEGS; j++) {
                this.tent[i][j].xRot = Mth.lerp(k, this.tent[i][j].xRot, this.tent[i][j].xRot * 0.3F + 0.08F);
            }
        }
    }

    /** 0 = closed, 1 = fully open: front ribs swing forward from the sternum like doors, their back halves straighten. */
    private void ribsOpen(float open) {
        for (int i = 0; i < 5; i++) {
            float k = open * (1.0F - i * 0.08F);
            this.ribL[i].yRot += k * 0.9F;
            this.ribR[i].yRot -= k * 0.9F;
            this.ribLb[i].yRot += k * 0.5F;
            this.ribRb[i].yRot -= k * 0.5F;
        }
    }
}
