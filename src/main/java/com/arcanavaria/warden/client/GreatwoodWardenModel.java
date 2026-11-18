package com.arcanavaria.warden.client;

import com.arcanavaria.warden.WardenState;
import com.arcanavaria.warden.GreatwoodWarden;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The cube model (geometry generated into {@code client/gen/GreatwoodWardenLayer} by tools/gen_bosses.py) with
 * procedural animation: every pose is a function of the synced state and the ticks since it began, laid on top of the
 * rest pose. Timings match the server's state table in {@link GreatwoodWarden}.
 *
 * <p>Rig: root > body > (torso > (head > antler_l/r, arm_l > fore_l, arm_r > fore_r, crystals*), leg_l > shin_l,
 * leg_r > shin_r).
 */
public class GreatwoodWardenModel extends EntityModel<GreatwoodWardenRenderState> {
    private final ModelPart body;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart armL, armR, foreL, foreR;
    private final ModelPart legL, legR;
    private final ModelPart antlerL, antlerR;

    public GreatwoodWardenModel(ModelPart root) {
        super(root);
        this.body = root.getChild("root").getChild("body");
        this.torso = this.body.getChild("torso");
        this.head = this.torso.getChild("head");
        this.armL = this.torso.getChild("arm_l");
        this.armR = this.torso.getChild("arm_r");
        this.foreL = this.armL.getChild("fore_l");
        this.foreR = this.armR.getChild("fore_r");
        this.legL = this.body.getChild("leg_l");
        this.legR = this.body.getChild("leg_r");
        this.antlerL = this.head.getChild("antler_l");
        this.antlerR = this.head.getChild("antler_r");
    }

    /** Smooth 0..1 between a and b. */
    private static float ramp(float t, float a, float b) {
        float k = Mth.clamp((t - a) / (b - a), 0.0F, 1.0F);
        return k * k * (3.0F - 2.0F * k);
    }

    @Override
    public void setupAnim(GreatwoodWardenRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float t = s.stateTime;
        WardenState st = s.state;
        boolean dying = s.dyingTime > 0.0F;

        // look, breathing, antler sway, walking
        if (!dying && st != WardenState.BURROW && st != WardenState.START) {
            this.head.yRot += Mth.clamp(s.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD * 0.6F;
            this.head.xRot += Mth.clamp(s.xRot, -20.0F, 30.0F) * Mth.DEG_TO_RAD * 0.5F;
        }
        float breath = Mth.sin(age * 0.07F);
        this.torso.xRot += 0.025F * breath;
        this.antlerL.zRot += 0.03F * Mth.sin(age * 0.05F);
        this.antlerR.zRot -= 0.03F * Mth.sin(age * 0.05F + 1.0F);
        float sp = Math.min(1.0F, s.walkAnimationSpeed * 1.5F);
        float wp = s.walkAnimationPos * 0.5F;
        this.legL.xRot += Mth.cos(wp) * 0.55F * sp;
        this.legR.xRot -= Mth.cos(wp) * 0.55F * sp;
        this.armL.xRot -= Mth.cos(wp) * 0.3F * sp;
        this.armR.xRot += Mth.cos(wp) * 0.3F * sp;
        this.torso.yRot += Mth.sin(wp) * 0.06F * sp;
        this.body.y += Math.abs(Mth.sin(wp)) * -1.0F * sp;

        switch (st) {
            case START -> {
                float k = ramp(t, 0.0F, 55.0F);
                this.body.y += (1.0F - k) * 40.0F;              // grows up out of the ground
                this.torso.xRot += (1.0F - k) * 0.6F;
                this.armL.zRot -= ramp(t, 40.0F, 50.0F) * 0.5F * (1.0F - ramp(t, 52.0F, 60.0F));
                this.armR.zRot += ramp(t, 40.0F, 50.0F) * 0.5F * (1.0F - ramp(t, 52.0F, 60.0F));
                this.head.xRot -= ramp(t, 40.0F, 46.0F) * 0.4F * (1.0F - ramp(t, 52.0F, 60.0F));
            }
            case SWIPE -> {
                float wind = ramp(t, 0.0F, 9.0F) * (1.0F - ramp(t, 9.0F, 11.0F));
                float follow = ramp(t, 9.0F, 11.0F) * (1.0F - ramp(t, 12.0F, 16.0F));
                this.armR.xRot += -1.9F * wind + 0.5F * follow;
                this.armR.zRot += 0.5F * wind - 0.6F * follow;
                this.torso.yRot += 0.35F * wind - 0.45F * follow;
            }
            case SLAM -> {
                float up = ramp(t, 0.0F, 18.0F) * (1.0F - ramp(t, 19.0F, 21.0F));
                float down = ramp(t, 19.0F, 21.0F) * (1.0F - ramp(t, 26.0F, s.phaseTwo ? 38.0F : 34.0F));
                this.armL.xRot += -2.7F * up + 0.2F * down;
                this.armR.xRot += -2.7F * up + 0.2F * down;
                this.foreL.xRot += 0.4F * up;
                this.foreR.xRot += 0.4F * up;
                this.torso.xRot += -0.25F * up + 0.45F * down;
                this.body.y += 3.0F * down;
            }
            case ROOTS -> {
                float reach = ramp(t, 0.0F, 10.0F) * (1.0F - ramp(t, 34.0F, 44.0F));
                float drive = ramp(t, 22.0F, 25.0F);
                this.torso.xRot += 0.3F * reach + 0.2F * drive * reach;
                this.armL.xRot += -1.1F * reach + 0.8F * drive * reach;
                this.armR.xRot += -1.1F * reach + 0.8F * drive * reach;
                this.armL.zRot -= 0.3F * reach;
                this.armR.zRot += 0.3F * reach;
                this.body.y += 2.0F * drive * reach;
            }
            case VOLLEY -> {
                float throwArm = 0.0F;
                int[] throwsAt = s.phaseTwo ? new int[] {12, 15, 18, 21, 24} : new int[] {12, 18, 24};
                for (int at : throwsAt) {
                    throwArm = Math.max(throwArm, ramp(t, at - 5.0F, at - 1.0F) * (1.0F - ramp(t, at, at + 2.0F)));
                }
                float ready = ramp(t, 0.0F, 6.0F) * (1.0F - ramp(t, 26.0F, 30.0F));
                this.armR.xRot += -0.6F * ready - 2.0F * throwArm;
                this.torso.yRot += 0.25F * throwArm;
            }
            case RAY -> {
                float open = ramp(t, 0.0F, 20.0F) * (1.0F - ramp(t, 55.0F, 70.0F));
                this.torso.xRot -= 0.2F * open;
                this.armL.zRot -= 0.75F * open;
                this.armR.zRot += 0.75F * open;
                this.armL.xRot -= 0.3F * open;
                this.armR.xRot -= 0.3F * open;
                this.head.xRot -= 0.15F * open;
                if (t >= 30.0F && t < 55.0F) {
                    this.torso.xRot += 0.02F * Mth.sin(age * 1.7F);   // shudders while the beam fires
                }
            }
            case MEND -> {
                float kneel = ramp(t, 0.0F, 12.0F) * (1.0F - ramp(t, 70.0F, 80.0F));
                this.body.y += 6.0F * kneel;
                this.legL.xRot -= 0.9F * kneel;
                this.legR.xRot -= 0.2F * kneel;
                this.torso.xRot += 0.3F * kneel;
                this.head.xRot += 0.35F * kneel;
                this.armL.xRot += -0.5F * kneel;
                this.armR.xRot += -0.5F * kneel;
                this.armL.zRot -= 0.4F * kneel + 0.05F * Mth.sin(age * 0.2F) * kneel;
                this.armR.zRot += 0.4F * kneel + 0.05F * Mth.sin(age * 0.2F) * kneel;
            }
            case STUN -> {
                float slump = ramp(t, 0.0F, 6.0F) * (1.0F - ramp(t, 32.0F, 40.0F));
                this.torso.xRot += 0.45F * slump;
                this.head.xRot += 0.4F * slump;
                this.head.zRot += 0.15F * Mth.sin(age * 0.3F) * slump;
                this.armL.zRot -= 0.1F * slump;
                this.armR.zRot += 0.1F * slump;
                this.body.y += 2.0F * slump;
            }
            case BURROW -> {
                float sink = ramp(t, 0.0F, 15.0F) * (1.0F - ramp(t, 35.0F, 40.0F));
                this.body.y += 48.0F * sink;
                this.torso.xRot += 0.5F * sink;
                float burst = ramp(t, 35.0F, 38.0F) * (1.0F - ramp(t, 40.0F, 50.0F));
                this.armL.zRot -= 0.9F * burst;
                this.armR.zRot += 0.9F * burst;
                this.head.xRot -= 0.3F * burst;
            }
            case ROAR -> {
                float roar = ramp(t, 4.0F, 12.0F) * (1.0F - ramp(t, 38.0F, 50.0F));
                this.torso.xRot -= 0.35F * roar;
                this.head.xRot -= 0.55F * roar;
                this.armL.zRot -= 1.0F * roar;
                this.armR.zRot += 1.0F * roar;
                this.armL.xRot -= 0.4F * roar;
                this.armR.xRot -= 0.4F * roar;
                this.head.yRot += 0.06F * Mth.sin(age * 1.3F) * roar;
            }
            default -> { }
        }

        if (dying) {
            // the heart gutters, the body sags to its knees and topples forward
            float k = ramp(s.dyingTime, 0.0F, 40.0F);
            this.body.y += 10.0F * k;
            this.legL.xRot -= 1.2F * ramp(s.dyingTime, 0.0F, 15.0F);
            this.legR.xRot -= 1.2F * ramp(s.dyingTime, 0.0F, 15.0F);
            this.torso.xRot += 1.1F * k;
            this.head.xRot += 0.5F * k;
            this.armL.zRot -= 0.3F * k;
            this.armR.zRot += 0.3F * k;
        }
    }
}
