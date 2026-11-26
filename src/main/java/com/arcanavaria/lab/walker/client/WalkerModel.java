package com.arcanavaria.lab.walker.client;

import com.arcanavaria.lab.walker.CrucibleWalker;
import com.arcanavaria.lab.walker.WalkerAct;
import com.arcanavaria.lab.walker.WalkerMode;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * The Crucible Walker's cube model (geometry in {@code client/gen/CrucibleWalkerLayer}, exported by
 * tools/bossmodels/crucible_walker.py) with procedural animation on top of the rest pose. Timings match the moves in
 * {@link CrucibleWalker}.
 *
 * <p>Rig: root > body > (gear holders > gears, cage > (turntables, governor, ring), thigh_xx > shin_xx > (knee gear,
 * foot_xx), arm_x > fore_x > (elbow gear, claw_x)). Every gear turns on its own z axis, smaller gears faster, so the
 * meshed pairs read as one train.
 */
public class WalkerModel extends EntityModel<WalkerRenderState> {
    private static final String[] GEARS = {"gear_lens", "gear_grate_r", "gear_side_r", "gear_mesh_r", "gear_sh_r", "gear_grate_l",
        "gear_side_l", "gear_mesh_l", "gear_sh_l", "gear_back_a", "gear_back_b", "turntable", "turntable_top", "knee_fr", "knee_fl",
        "knee_br", "knee_bl", "egear_r", "egear_l"};
    private static final int[] RADIUS = {5, 4, 9, 4, 4, 4, 9, 4, 4, 6, 4, 10, 5, 6, 6, 6, 6, 5, 5};
    private static final int[] SIGN = {1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1};

    private final ModelPart body, cage, ring, governor;
    private final ModelPart[] gears = new ModelPart[GEARS.length];
    /** fr, fl, br, bl. */
    private final ModelPart[] thigh = new ModelPart[4], shin = new ModelPart[4];
    private final ModelPart armR, armL, foreR, foreL, clawR;

    public WalkerModel(ModelPart root) {
        super(root);
        Function<String, @Nullable ModelPart> look = root.createPartLookup();
        this.body = part(look, "body");
        this.cage = part(look, "cage");
        this.ring = part(look, "ring");
        this.governor = part(look, "governor");
        for (int i = 0; i < GEARS.length; i++) {
            this.gears[i] = part(look, GEARS[i]);
        }
        String[] legs = {"fr", "fl", "br", "bl"};
        for (int i = 0; i < 4; i++) {
            this.thigh[i] = part(look, "thigh_" + legs[i]);
            this.shin[i] = part(look, "shin_" + legs[i]);
        }
        this.armR = part(look, "arm_r");
        this.armL = part(look, "arm_l");
        this.foreR = part(look, "fore_r");
        this.foreL = part(look, "fore_l");
        this.clawR = part(look, "claw_r");
    }

    private static ModelPart part(Function<String, @Nullable ModelPart> look, String name) {
        ModelPart p = look.apply(name);
        if (p == null) {
            throw new IllegalStateException("crucible walker model has no part " + name);
        }
        return p;
    }

    private static float ramp(float t, float a, float b) {
        float k = Mth.clamp((t - a) / (b - a), 0.0F, 1.0F);
        return k * k * (3.0F - 2.0F * k);
    }

    /** Rises over [a, b], holds, falls over [c, d]. */
    private static float hold(float t, float a, float b, float c, float d) {
        return ramp(t, a, b) * (1.0F - ramp(t, c, d));
    }

    @Override
    public void setupAnim(WalkerRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float t = s.actTime;
        WalkerAct a = s.act;

        // gears: one running phase from the entity, so speed changes never make them jump
        for (int i = 0; i < this.gears.length; i++) {
            float perLoop = RADIUS[i] < 5 ? Mth.HALF_PI : RADIUS[i] < 9 ? Mth.PI / 4.0F : Mth.PI / 8.0F;
            this.gears[i].zRot += SIGN[i] * perLoop * s.gears;
        }
        this.governor.yRot += s.gears * Mth.TWO_PI * 0.5F;

        // walk: a diagonal trot, fr with bl and fl with br
        float sp = Math.min(1.0F, s.walkAnimationSpeed * 1.6F);
        float ph = s.walkAnimationPos * 0.55F;
        float[] off = {0.0F, Mth.PI, Mth.PI, 0.0F};
        for (int i = 0; i < 4; i++) {
            float sw = Mth.cos(ph + off[i]);
            this.thigh[i].xRot += -0.35F * sw * sp;
            this.shin[i].xRot += 0.25F * Math.max(0.0F, Mth.sin(ph + off[i])) * sp;
        }
        this.body.y += -Mth.abs(Mth.sin(ph)) * 1.2F * sp + Mth.sin(age * 0.08F) * 0.3F;
        this.armR.xRot += 0.15F * Mth.cos(ph) * sp;
        this.armL.xRot += -0.15F * Mth.cos(ph) * sp;

        // mode: the overclock shivers, the stall sags and opens the cage
        if (s.mode == WalkerMode.OVERCLOCK) {
            this.body.x += Mth.sin(age * 2.3F) * 0.25F;
            this.body.xRot += 0.06F;
        } else if (s.mode == WalkerMode.STALL && a == WalkerAct.IDLE) {
            float k = hold(s.modeTime, 0.0F, 8.0F, 72.0F, 80.0F);
            this.body.y += 3.0F * k;
            this.body.xRot += 0.08F * k;
            this.cage.y -= 5.0F * k;
            this.ring.y -= 3.0F * k;
            this.ring.xRot += 0.12F * k * Mth.sin(age * 0.5F);
            this.ring.zRot += 0.12F * k * Mth.cos(age * 0.5F);
            for (int i = 0; i < 4; i++) {
                this.shin[i].xRot += 0.2F * k * (i < 2 ? 1 : -1);
            }
            this.armR.xRot += 0.5F * k;
            this.armL.xRot += 0.5F * k;
        }

        switch (a) {
            case START -> {
                float k = ramp(t, 0.0F, 34.0F);
                this.body.y += 4.0F * (1.0F - k);
                this.cage.y += 2.0F * (1.0F - k);
                this.armR.xRot += 0.6F * (1.0F - k);
                this.armL.xRot += 0.6F * (1.0F - k);
            }
            case SWIPE -> {
                // draw back (0-14), slash across (14-18), recover
                float back = hold(t, 0.0F, 12.0F, 14.0F, 17.0F);
                float slash = hold(t, 14.0F, 17.0F, 20.0F, 26.0F);
                this.armR.xRot += -0.9F * back - 1.2F * slash;
                this.armR.yRot += 0.6F * back - 0.9F * slash;
                this.foreR.xRot += -0.6F * back + 0.3F * slash;
                this.clawR.xRot += 0.4F * back;
                this.body.yRot += 0.15F * back - 0.25F * slash;
            }
            case STOMP -> {
                // front legs lift (0-16), then slam
                float up = hold(t, 0.0F, 14.0F, 15.0F, 17.0F);
                float slam = hold(t, 16.0F, 17.0F, 20.0F, 30.0F);
                this.body.xRot += -0.22F * up + 0.06F * slam;
                this.body.y += -2.0F * up + 1.5F * slam;
                this.thigh[0].xRot += -0.6F * up;
                this.thigh[1].xRot += -0.6F * up;
                this.shin[0].xRot += 0.5F * up;
                this.shin[1].xRot += 0.5F * up;
                this.armR.xRot += -0.8F * up;
                this.armL.xRot += -0.8F * up;
            }
            case LOB -> {
                // the cage tips back and flings at tick 8
                float load = hold(t, 0.0F, 7.0F, 8.0F, 10.0F);
                float fling = hold(t, 8.0F, 10.0F, 14.0F, 24.0F);
                this.cage.xRot += 0.25F * load - 0.2F * fling;
                this.body.xRot += 0.08F * load - 0.06F * fling;
                this.armL.xRot += -0.9F * load - 0.3F * fling;
            }
            case CHARGE -> {
                float crouch = hold(t, 0.0F, 20.0F, 24.0F, 26.0F);
                float run = hold(t, 24.0F, 25.0F, 34.0F, 38.0F);
                this.body.y += 2.0F * crouch;
                this.body.xRot += 0.18F * crouch + 0.25F * run;
                this.armR.xRot += -1.0F * run;
                this.armL.xRot += -1.0F * run;
                this.foreR.xRot += -0.5F * run;
                this.foreL.xRot += -0.5F * run;
                float rph = t * 1.4F;
                for (int i = 0; i < 4; i++) {
                    this.thigh[i].xRot += -0.5F * Mth.cos(rph + (i == 0 || i == 3 ? 0.0F : Mth.PI)) * run;
                }
            }
            case RAY -> {
                // braces and leans the lens onto the target
                float brace = hold(t, 0.0F, 14.0F, 56.0F, 60.0F);
                float fire = hold(t, 40.0F, 41.0F, 58.0F, 60.0F);
                this.body.y += 1.5F * brace;
                this.body.xRot += 0.12F * brace;
                for (int i = 0; i < 4; i++) {
                    this.thigh[i].zRot += (i % 2 == 0 ? 0.12F : -0.12F) * brace;
                }
                this.body.x += Mth.sin(age * 3.0F) * 0.2F * fire;
            }
            case BURST -> {
                // vents flare (0-20): crouch and shake; then the release lifts it
                float flare = hold(t, 0.0F, 18.0F, 20.0F, 22.0F);
                float rel = hold(t, 20.0F, 22.0F, 26.0F, 34.0F);
                this.body.y += 2.5F * flare - 1.5F * rel;
                this.body.x += Mth.sin(age * 2.7F) * 0.4F * flare;
                this.cage.y -= 3.0F * rel;
                this.armR.zRot += 0.5F * rel;
                this.armL.zRot -= 0.5F * rel;
            }
            case TELL -> {
                // frozen, arms half raised
                float k = hold(t, 0.0F, 4.0F, 13.0F, 16.0F);
                this.armR.xRot += -0.5F * k;
                this.armL.xRot += -0.5F * k;
                this.cage.y -= 1.0F * k;
            }
            default -> { }
        }

        // death: legs give, the body settles and tips
        if (s.dyingTime > 0.0F) {
            float k = ramp(s.dyingTime, 0.0F, 30.0F);
            this.body.y += 10.0F * k;
            this.body.zRot += 0.12F * k;
            this.body.xRot += 0.1F * k;
            for (int i = 0; i < 4; i++) {
                this.thigh[i].zRot += (i % 2 == 0 ? 0.6F : -0.6F) * k;
                this.shin[i].xRot += 0.4F * k;
            }
            this.armR.xRot += 0.8F * k;
            this.armL.xRot += 0.8F * k;
            this.cage.y -= 2.0F * k;
        }
    }
}
