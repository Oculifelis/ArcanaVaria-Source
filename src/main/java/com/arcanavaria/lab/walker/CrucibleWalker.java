package com.arcanavaria.lab.walker;

import static com.arcanavaria.lab.walker.WalkerAct.*;

import com.arcanavaria.AVDataMaps;
import com.arcanavaria.ArcanaVaria;
import com.arcanavaria.lab.LabRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Crucible Walker, the lab guardian's one body: a four-legged brass and thaumium engine with an essentia crucible
 * under an open cage. The lab archetypes are stat sets on this body and its rolled traits tint its glow
 * ({@link com.arcanavaria.lab.boss.LabBosses} raises it and runs the traits).
 *
 * <p>Its fight runs a loop players can learn ({@link WalkerMode}): 10 s walking with swipe, stomp and lobs; 6 s
 * overclocked (vents glowing, gears racing, 1.5x speed) with lane charges and a lens ray; then a 4 s stall where steam
 * pours out, the lens goes dark, the cage opens and hits do double damage. In phase two (half health) the overclock
 * lasts 8 s and ends with a steam burst. Every hit has a tell of at least 0.6 s and can be walked, sprinted or jumped
 * out of, or blocked with a shield. A trait move first freezes it for 0.8 s in the trait's colour.
 *
 * <p>Numbers are data: {@code data/<ns>/data_maps/entity_type/boss_moves.json} keys {@code swipe_damage},
 * {@code stomp_damage}, {@code lob_damage}, {@code charge_damage}, {@code ray_damage}, {@code burst_damage},
 * {@code normal_ticks}, {@code overclock_ticks}, {@code overclock_ticks_phase_two}, {@code stall_ticks},
 * {@code stall_multiplier}.
 */
public class CrucibleWalker extends Monster {
    private static final EntityDataAccessor<Integer> DATA_ACT = SynchedEntityData.defineId(CrucibleWalker.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACT_START = SynchedEntityData.defineId(CrucibleWalker.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(CrucibleWalker.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MODE_START = SynchedEntityData.defineId(CrucibleWalker.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PHASE_TWO = SynchedEntityData.defineId(CrucibleWalker.class, EntityDataSerializers.BOOLEAN);
    /** The glow colour between moves: thaumic violet, mixed with the first trait's colour. */
    private static final EntityDataAccessor<Integer> DATA_TINT = SynchedEntityData.defineId(CrucibleWalker.class, EntityDataSerializers.INT);
    /** The trait colour of the move being told (only read during {@link WalkerAct#TELL}). */
    private static final EntityDataAccessor<Integer> DATA_TELL_TINT = SynchedEntityData.defineId(CrucibleWalker.class, EntityDataSerializers.INT);

    public static final int VIOLET = 0xB87CFF;
    private static final Identifier OVERCLOCK = ArcanaVaria.id("crucible_walker_overclock");
    private static final ParticleOptions STEAM = ParticleTypes.CLOUD;
    private static final double STOMP_RADIUS = 6.0, CHARGE_LENGTH = 14.0, RAY_LENGTH = 20.0, BURST_RADIUS = 9.0;

    // Not saved: the move in progress and what it owns. A reload resumes walking at the start of the loop.
    private int actTicks;
    private int modeTicks;
    private int gap;
    private WalkerAct lastAttack = IDLE;
    private final Set<UUID> hit = new HashSet<>();
    private final List<Vec3> spots = new ArrayList<>();
    private Vec3 aim = Vec3.ZERO;
    private Vec3 dir = new Vec3(0, 0, 1);
    private @Nullable Runnable pendingTell;
    private int pendingColour;
    /** Client only: the gears' running angle, integrated so a change of speed never jumps them. */
    public float gearPhase, gearPhaseO;

    public CrucibleWalker(EntityType<? extends CrucibleWalker> type, Level level) {
        super(type, level);
        this.xpReward = 80;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 260.0)
            .add(Attributes.ARMOR, 10.0)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACT, START.id());
        builder.define(DATA_ACT_START, 0);
        builder.define(DATA_MODE, WalkerMode.NORMAL.ordinal());
        builder.define(DATA_MODE_START, 0);
        builder.define(DATA_PHASE_TWO, false);
        builder.define(DATA_TINT, VIOLET);
        builder.define(DATA_TELL_TINT, VIOLET);
    }

    // ------------------------------------------------------------------------------------------------ synced state
    public WalkerAct act() {
        return WalkerAct.byId(this.entityData.get(DATA_ACT));
    }

    public float actTime(float pt) {
        return this.tickCount - this.entityData.get(DATA_ACT_START) + pt;
    }

    public WalkerMode mode() {
        return WalkerMode.byId(this.entityData.get(DATA_MODE));
    }

    public float modeTime(float pt) {
        return this.tickCount - this.entityData.get(DATA_MODE_START) + pt;
    }

    public boolean phaseTwo() {
        return this.entityData.get(DATA_PHASE_TWO);
    }

    public int tint() {
        return this.entityData.get(DATA_TINT);
    }

    public int tellTint() {
        return this.entityData.get(DATA_TELL_TINT);
    }

    private void setAct(WalkerAct next) {
        this.entityData.set(DATA_ACT, next.id());
        this.entityData.set(DATA_ACT_START, this.tickCount);
        this.actTicks = 0;
        this.hit.clear();
    }

    private void setMode(WalkerMode next) {
        this.entityData.set(DATA_MODE, next.ordinal());
        this.entityData.set(DATA_MODE_START, this.tickCount);
        this.modeTicks = 0;
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(OVERCLOCK);
            if (next == WalkerMode.OVERCLOCK) {
                speed.addTransientModifier(new AttributeModifier(OVERCLOCK, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }

    /** Called when it is raised: the first trait's colour mixes into its violet glow. */
    public void tintWith(int colour) {
        int r = (((VIOLET >> 16) & 255) + ((colour >> 16) & 255)) / 2;
        int g = (((VIOLET >> 8) & 255) + ((colour >> 8) & 255)) / 2;
        int b = ((VIOLET & 255) + (colour & 255)) / 2;
        this.entityData.set(DATA_TINT, (r << 16) | (g << 8) | b);
    }

    /**
     * A trait asks to use its move: the walker plays it at its next free moment (not mid-move, not while stalled),
     * after a 0.8 s freeze in the trait's colour. Returns false if one is already waiting.
     */
    public boolean queueTraitMove(int colour, Runnable move) {
        if (this.pendingTell != null || this.act() == START || this.isDeadOrDying()) {
            return false;
        }
        this.pendingTell = move;
        this.pendingColour = colour;
        return true;
    }

    // ------------------------------------------------------------------------------------------------ damage rules
    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN)
            || source.is(DamageTypes.CRAMMING)) {
            return true;
        }
        if (this.act() == START && !source.is(DamageTypes.GENERIC_KILL)) {
            return true;
        }
        return super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.mode() == WalkerMode.STALL) {
            damage *= (float) AVDataMaps.move(this, "stall_multiplier", 2.0);
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    // ------------------------------------------------------------------------------------------------ client gears
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.gearPhaseO = this.gearPhase;
            this.gearPhase += this.gearRate();
        }
    }

    /** Gear loops per tick: idle hum, racing when overclocked, winding down in the stall, stopped for a trait tell. */
    private float gearRate() {
        if (this.isDeadOrDying()) {
            return Math.max(0.0F, 0.05F - this.deathTime * 0.002F);
        }
        WalkerAct a = this.act();
        if (a == TELL) {
            return 0.0F;
        }
        if (a == START) {
            return 0.06F * Mth.clamp(this.actTime(0) / 40.0F, 0.0F, 1.0F);
        }
        return switch (this.mode()) {
            case NORMAL -> 0.06F;
            case OVERCLOCK -> 0.22F;
            case STALL -> 0.06F * Math.max(0.0F, 1.0F - this.modeTime(0) / 20.0F);
        };
    }

    // ------------------------------------------------------------------------------------------------ the fight
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || this.distanceToSqr(target) > 40 * 40
            || target instanceof Player p && (p.isSpectator() || p.isCreative()))) {
            target = null;
            this.setTarget(null);
        }
        WalkerAct a = this.act();
        int t = this.actTicks++;
        if (a == START) {
            this.tickStart(level, t);
            return;
        }
        if (!this.phaseTwo() && this.getHealth() < this.getMaxHealth() * 0.5F) {
            this.entityData.set(DATA_PHASE_TWO, true);
            this.playSound(SoundEvents.IRON_GOLEM_DAMAGE, 2.0F, 0.5F);
            level.sendParticles(STEAM, this.getX(), this.getY() + 3.0, this.getZ(), 40, 1.2, 0.8, 1.2, 0.08);
        }
        this.modeTicks++;
        this.modeEffects(level);
        if (a != IDLE) {
            this.getNavigation().stop();
        }
        switch (a) {
            case IDLE -> this.tickIdle(level, target);
            case SWIPE -> this.tickSwipe(level, t, target);
            case STOMP -> this.tickStomp(level, t);
            case LOB -> this.tickLob(level, t, target);
            case CHARGE -> this.tickCharge(level, t, target);
            case RAY -> this.tickRay(level, t, target);
            case BURST -> this.tickBurst(level, t);
            case TELL -> this.tickTell(level, t);
            default -> { }
        }
        if (this.act() == a && a != IDLE && t + 1 >= a.length()) {
            this.setAct(IDLE);
            this.gap = this.mode() == WalkerMode.OVERCLOCK ? 8 : 18;
            if (a == BURST) {
                this.setMode(WalkerMode.STALL);
            }
        }
    }

    private void tickStart(ServerLevel level, int t) {
        if (t % 4 == 0) {
            level.sendParticles(STEAM, this.getX(), this.getY() + 2.5, this.getZ(), 4, 0.8, 0.4, 0.8, 0.02);
            level.sendParticles(this.dust(this.tint(), 1.4F), this.getX(), this.getY() + 3.0, this.getZ(), 6, 0.8, 0.6, 0.8, 0.0);
        }
        if (t == 0) {
            this.playSound(SoundEvents.PISTON_EXTEND, 2.0F, 0.5F);
        }
        if (t == 30) {
            this.playSound(SoundEvents.BEACON_ACTIVATE, 2.0F, 0.6F);
        }
        if (t + 1 >= START.length()) {
            this.setAct(IDLE);
            this.setMode(WalkerMode.NORMAL);
            this.gap = 20;
        }
    }

    /** The loop: walk, overclock, stall. A mode only changes between moves. */
    private void modeEffects(ServerLevel level) {
        WalkerMode m = this.mode();
        if (m == WalkerMode.OVERCLOCK && this.tickCount % 3 == 0) {
            Vec3 f = this.forward(), r = this.right();
            for (int s = -1; s <= 1; s += 2) {
                Vec3 v = this.position().add(r.scale(s * 1.3 * this.getScale())).add(f.scale(-0.4)).add(0, 2.4 * this.getScale(), 0);
                level.sendParticles(ParticleTypes.SMALL_FLAME, v.x, v.y, v.z, 1, 0.1, 0.1, 0.1, 0.01);
                level.sendParticles(this.dust(this.tint(), 1.0F), v.x, v.y, v.z, 1, 0.15, 0.15, 0.15, 0.0);
            }
        }
        if (m == WalkerMode.STALL && this.tickCount % 2 == 0) {
            double h = 3.0 * this.getScale();
            level.sendParticles(STEAM, this.getX(), this.getY() + h, this.getZ(), 3, 0.7, 0.2, 0.7, 0.05);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + h, this.getZ(), 1, 0.4, 0.1, 0.4, 0.02);
        }
        if (this.act() != IDLE) {
            return;
        }
        int normal = (int) AVDataMaps.move(this, "normal_ticks", 200);
        int over = (int) AVDataMaps.move(this, "overclock_ticks", this.phaseTwo(), 120, 160);
        int stall = (int) AVDataMaps.move(this, "stall_ticks", 80);
        if (m == WalkerMode.NORMAL && this.modeTicks >= normal) {
            this.setMode(WalkerMode.OVERCLOCK);
            this.playSound(SoundEvents.BEACON_POWER_SELECT, 2.0F, 0.6F);
            this.playSound(SoundEvents.FIRECHARGE_USE, 1.5F, 0.5F);
            this.gap = 10;
        } else if (m == WalkerMode.OVERCLOCK && this.modeTicks >= over) {
            if (this.phaseTwo()) {
                this.startAct(BURST, null);
            } else {
                this.setMode(WalkerMode.STALL);
            }
        } else if (m == WalkerMode.STALL && this.modeTicks == 1) {
            this.playSound(SoundEvents.FIRE_EXTINGUISH, 2.0F, 0.5F);
            this.playSound(SoundEvents.BEACON_DEACTIVATE, 1.6F, 0.6F);
        } else if (m == WalkerMode.STALL && this.modeTicks >= stall) {
            this.setMode(WalkerMode.NORMAL);
            this.gap = 16;
        }
    }

    private void tickIdle(ServerLevel level, @Nullable LivingEntity target) {
        WalkerMode m = this.mode();
        if (m == WalkerMode.STALL) {
            this.getNavigation().stop();
            return;
        }
        if (this.pendingTell != null && this.gap <= 0) {
            this.entityData.set(DATA_TELL_TINT, this.pendingColour);
            this.setAct(TELL);
            return;
        }
        if (target == null) {
            return;
        }
        this.getNavigation().moveTo(target, 1.0);
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.gap > 0) {
            this.gap--;
            return;
        }
        double d = Math.sqrt(this.distanceToSqr(target));
        double reach = 3.2 * this.getScale() + 1.0;
        List<WalkerAct> bag = new ArrayList<>();
        if (m == WalkerMode.NORMAL) {
            if (d < reach) {
                bag.add(SWIPE);
            }
            if (d < STOMP_RADIUS - 0.5) {
                bag.add(STOMP);
            }
            if (d >= 4.0 && d < 16.0) {
                bag.add(LOB);
            }
        } else {
            if (d < reach) {
                bag.add(SWIPE);
            }
            if (d >= 3.0 && d < CHARGE_LENGTH) {
                bag.add(CHARGE);
            }
            if (d >= 5.0 && d < RAY_LENGTH) {
                bag.add(RAY);
            }
        }
        if (bag.isEmpty()) {
            return;
        }
        if (bag.size() > 1) {
            bag.remove(this.lastAttack);
        }
        this.startAct(bag.get(this.random.nextInt(bag.size())), target);
    }

    private void startAct(WalkerAct a, @Nullable LivingEntity target) {
        this.lastAttack = a;
        this.setAct(a);
        this.spots.clear();
        if (target != null) {
            this.aimAt(target.position());
        }
        if (a == LOB && target != null) {
            this.spots.add(target.position());
            if (this.phaseTwo()) {
                Vec3 side = new Vec3(-this.dir.z, 0, this.dir.x).scale(3.0);
                this.spots.add(target.position().add(side));
                this.spots.add(target.position().subtract(side));
            }
            this.playSound(SoundEvents.BREWING_STAND_BREW, 2.0F, 0.6F);
        }
        if (a == CHARGE) {
            this.playSound(SoundEvents.RAVAGER_ROAR, 1.2F, 1.4F);
        }
        if (a == RAY) {
            this.playSound(SoundEvents.BEACON_AMBIENT, 2.0F, 1.6F);
        }
        if (a == BURST) {
            this.playSound(SoundEvents.LAVA_EXTINGUISH, 2.0F, 0.5F);
        }
    }

    /** Faces a point: sets {@link #dir} and turns the whole body to it. */
    private void aimAt(Vec3 p) {
        Vec3 d = new Vec3(p.x - this.getX(), 0, p.z - this.getZ());
        if (d.lengthSqr() < 1e-4) {
            return;
        }
        this.dir = d.normalize();
        this.aim = p;
        float yaw = (float) (Mth.atan2(this.dir.z, this.dir.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    /** Claw drawn back for 14 ticks (tracking), then a cone in front. */
    private void tickSwipe(ServerLevel level, int t, @Nullable LivingEntity target) {
        if (t < 10 && target != null) {
            this.aimAt(target.position());
        }
        Vec3 claw = this.position().add(this.dir.scale(2.2 * this.getScale())).add(this.right().scale(1.2 * this.getScale())).add(0, 1.8 * this.getScale(), 0);
        if (t < 14 && t % 2 == 0) {
            level.sendParticles(this.dust(this.tint(), 1.2F), claw.x, claw.y, claw.z, 3, 0.3, 0.3, 0.3, 0.0);
        }
        if (t == 2) {
            this.playSound(SoundEvents.PISTON_CONTRACT, 1.6F, 0.6F);
        }
        if (t == 14) {
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.6F, 0.6F);
            double reach = 3.2 * this.getScale() + 1.5;
            for (int i = -3; i <= 3; i++) {
                Vec3 v = this.dir.yRot((float) Math.toRadians(i * 15.0)).scale(reach * 0.7);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + v.x, this.getY() + 1.0, this.getZ() + v.z, 1, 0, 0, 0, 0);
            }
            float dmg = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * (float) AVDataMaps.move(this, "swipe_damage", 1.0);
            for (LivingEntity e : this.victims(level, this.getBoundingBox().inflate(reach, 1.0, reach))) {
                Vec3 to = new Vec3(e.getX() - this.getX(), 0, e.getZ() - this.getZ());
                if (to.length() < reach + e.getBbWidth() / 2 && (to.lengthSqr() < 1e-4 || this.dir.dot(to.normalize()) > 0.35)) {
                    if (e.hurtServer(level, this.damageSources().mobAttack(this), dmg)) {
                        e.knockback(0.8, -this.dir.x, -this.dir.z);
                    }
                }
            }
        }
    }

    /** Front legs up for 16 ticks, then a ring rolls out to 6 blocks along the ground. Jump it. */
    private void tickStomp(ServerLevel level, int t) {
        if (t < 16 && t % 3 == 0) {
            this.ring(level, STOMP_RADIUS, this.dust(this.tint(), 1.0F), 36);
        }
        if (t == 0) {
            this.playSound(SoundEvents.IRON_GOLEM_STEP, 2.0F, 0.5F);
        }
        if (t == 16) {
            this.playSound(SoundEvents.ANVIL_LAND, 1.4F, 0.5F);
        }
        if (t >= 16 && t < 27) {
            double r = 1.0 + (t - 16) * 0.5;
            this.ring(level, r, ParticleTypes.CLOUD, (int) (8 * r));
            float dmg = (float) AVDataMaps.move(this, "stomp_damage", 9.0);
            for (LivingEntity e : this.victims(level, this.getBoundingBox().inflate(STOMP_RADIUS + 1, 1.0, STOMP_RADIUS + 1))) {
                double d = Math.sqrt(e.distanceToSqr(this.getX(), e.getY(), this.getZ()));
                if (e.onGround() && Math.abs(d - r) < 0.9 && this.hit.add(e.getUUID())) {
                    if (e.hurtServer(level, this.damageSources().mobAttack(this), dmg)) {
                        e.push(0, 0.5, 0);
                        e.hurtMarked = true;
                    }
                }
            }
        }
    }

    /** Marks the ground under the target (and two more spots in phase two); globs land 28 ticks later. */
    private void tickLob(ServerLevel level, int t, @Nullable LivingEntity target) {
        if (t < 28 && t % 2 == 0) {
            for (Vec3 s : this.spots) {
                this.circle(level, s, 2.0, this.dust(this.tint(), 1.0F), 16);
            }
        }
        Vec3 from = this.position().add(0, 4.2 * this.getScale(), 0);
        if (t == 8) {
            this.playSound(SoundEvents.BLAZE_SHOOT, 1.4F, 0.5F);
        }
        if (t >= 8 && t < 28) {
            float k = (t - 8) / 20.0F;
            for (Vec3 s : this.spots) {
                Vec3 p = from.lerp(s, k).add(0, Math.sin(k * Math.PI) * 5.0, 0);
                level.sendParticles(this.dust(this.tint(), 2.0F), p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.0);
                level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
        if (t == 28) {
            this.playSound(SoundEvents.SPLASH_POTION_BREAK, 2.0F, 0.6F);
            float dmg = (float) AVDataMaps.move(this, "lob_damage", 7.0);
            for (Vec3 s : this.spots) {
                level.sendParticles(ParticleTypes.EXPLOSION, s.x, s.y + 0.5, s.z, 1, 0, 0, 0, 0);
                level.sendParticles(this.dust(this.tint(), 2.0F), s.x, s.y + 0.4, s.z, 30, 1.2, 0.4, 1.2, 0.0);
                for (LivingEntity e : this.victims(level, new AABB(s, s).inflate(2.2, 2.0, 2.2))) {
                    if (e.distanceToSqr(s) < 2.2 * 2.2 + 1 && this.hit.add(e.getUUID())) {
                        e.hurtServer(level, this.damageSources().mobProjectile(this, this), dmg);
                    }
                }
            }
        }
    }

    /** Overclocked: a lane glows for 24 ticks, then it charges 14 blocks down it. Step out of the lane. */
    private void tickCharge(ServerLevel level, int t, @Nullable LivingEntity target) {
        if (t < 10 && target != null) {
            this.aimAt(target.position());
        }
        Vec3 side = new Vec3(-this.dir.z, 0, this.dir.x);
        double half = 1.6 * this.getScale();
        if (t < 24 && t % 2 == 0) {
            for (double d = 1.0; d <= CHARGE_LENGTH; d += 0.8) {
                Vec3 c = this.position().add(this.dir.scale(d));
                for (int s = -1; s <= 1; s += 2) {
                    Vec3 v = c.add(side.scale(s * half));
                    Vec3 g = ground(level, v.x, this.getY(), v.z);
                    if (g != null) {
                        level.sendParticles(this.dust(this.tint(), 1.0F), g.x, g.y + 0.1, g.z, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
        if (t >= 24 && t < 36) {
            this.setDeltaMovement(this.dir.x * 1.1, this.getDeltaMovement().y, this.dir.z * 1.1);
            if (t % 2 == 0) {
                level.sendParticles(STEAM, this.getX(), this.getY() + 0.6, this.getZ(), 4, 0.8, 0.2, 0.8, 0.02);
            }
            if (this.horizontalCollision && t > 25) {
                this.playSound(SoundEvents.ANVIL_LAND, 1.0F, 0.4F);
                this.setDeltaMovement(Vec3.ZERO);
                this.actTicks = 36;
            }
            float dmg = (float) AVDataMaps.move(this, "charge_damage", 14.0);
            for (LivingEntity e : this.victims(level, this.getBoundingBox().inflate(0.6, 0.5, 0.6))) {
                if (this.hit.add(e.getUUID()) && e.hurtServer(level, this.damageSources().mobAttack(this), dmg)) {
                    e.push(this.dir.x * 1.4 + side.x * 0.4, 0.5, this.dir.z * 1.4 + side.z * 0.4);
                    e.hurtMarked = true;
                }
            }
        }
        if (t == 24) {
            this.playSound(SoundEvents.RAVAGER_ATTACK, 2.0F, 0.6F);
        }
    }

    /** Overclocked: the lens aims for 40 ticks and locks at 26, then a 20-block ray for 20 ticks. */
    private void tickRay(ServerLevel level, int t, @Nullable LivingEntity target) {
        Vec3 eye = this.lens();
        if (t < 26 && target != null) {
            this.aimAt(target.position());
            this.aim = target.getEyePosition().add(0, -0.4, 0);
        }
        Vec3 ray = this.aim.subtract(eye);
        ray = ray.lengthSqr() < 1e-4 ? this.dir : ray.normalize();
        Vec3 end = level.clip(new ClipContext(eye, eye.add(ray.scale(RAY_LENGTH)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getLocation();
        double len = end.distanceTo(eye);
        if (t < 40 && t % (t < 26 ? 4 : 2) == 0) {
            for (double d = 1.0; d < len; d += (t < 26 ? 1.5 : 0.8)) {
                Vec3 p = eye.add(ray.scale(d));
                level.sendParticles(this.dust(this.tint(), 0.6F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
        if (t == 26) {
            this.playSound(SoundEvents.BEACON_POWER_SELECT, 2.0F, 1.8F);
        }
        if (t == 40) {
            this.playSound(SoundEvents.GUARDIAN_ATTACK, 2.0F, 0.7F);
        }
        if (t >= 40) {
            for (double d = 0.5; d < len; d += 0.5) {
                Vec3 p = eye.add(ray.scale(d));
                level.sendParticles(this.dust(this.tint(), 1.6F), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
                if ((int) (d * 2) % 4 == 0) {
                    level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                }
            }
            if (t % 5 == 0) {
                float dmg = (float) AVDataMaps.move(this, "ray_damage", 4.0);
                AABB box = new AABB(eye, end).inflate(1.2);
                for (LivingEntity e : this.victims(level, box)) {
                    Vec3 c = e.getBoundingBox().getCenter();
                    double along = Mth.clamp(c.subtract(eye).dot(ray), 0, len);
                    if (c.distanceTo(eye.add(ray.scale(along))) < 0.9 + e.getBbWidth() / 2) {
                        e.hurtServer(level, this.damageSources().mobAttack(this), dmg);
                    }
                }
            }
        }
    }

    /** Phase two, end of the overclock: vents flare for 20 ticks, then a steam ring rolls out to 9 blocks. */
    private void tickBurst(ServerLevel level, int t) {
        if (t < 20 && t % 2 == 0) {
            double h = 2.6 * this.getScale();
            level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + h, this.getZ(), 6, 1.2, 0.3, 1.2, 0.02);
            level.sendParticles(STEAM, this.getX(), this.getY() + h, this.getZ(), 4, 1.0, 0.2, 1.0, 0.03);
            this.ring(level, BURST_RADIUS, this.dust(this.tint(), 1.0F), 54);
        }
        if (t == 20) {
            this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.6F);
        }
        if (t >= 20 && t < 32) {
            double r = 1.0 + (t - 20) * 0.75;
            this.ring(level, r, STEAM, (int) (7 * r));
            float dmg = (float) AVDataMaps.move(this, "burst_damage", 12.0);
            for (LivingEntity e : this.victims(level, this.getBoundingBox().inflate(BURST_RADIUS + 1, 1.5, BURST_RADIUS + 1))) {
                double d = Math.sqrt(e.distanceToSqr(this.getX(), e.getY(), this.getZ()));
                if (e.onGround() && Math.abs(d - r) < 1.0 && this.hit.add(e.getUUID())) {
                    if (e.hurtServer(level, this.damageSources().mobAttack(this), dmg)) {
                        Vec3 out = new Vec3(e.getX() - this.getX(), 0, e.getZ() - this.getZ());
                        out = out.lengthSqr() > 1e-4 ? out.normalize() : Vec3.ZERO;
                        e.push(out.x, 0.5, out.z);
                        e.hurtMarked = true;
                    }
                }
            }
        }
    }

    /** A trait move: frozen in the trait's colour for 16 ticks, then the move fires. */
    private void tickTell(ServerLevel level, int t) {
        if (t % 2 == 0) {
            level.sendParticles(this.dust(this.tellTint(), 1.8F), this.getX(), this.getY() + 2.6 * this.getScale(), this.getZ(), 4, 0.6, 0.6, 0.6, 0.0);
        }
        if (t == 0) {
            this.playSound(SoundEvents.BEACON_POWER_SELECT, 1.6F, 1.2F);
        }
        if (t + 1 >= TELL.length()) {
            Runnable move = this.pendingTell;
            this.pendingTell = null;
            if (move != null && this.getTarget() != null) {
                move.run();
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ helpers
    private Vec3 lens() {
        return this.position().add(this.forward().scale(1.3 * this.getScale())).add(0, 2.6 * this.getScale(), 0);
    }

    private Vec3 forward() {
        return Vec3.directionFromRotation(0, this.yBodyRot);
    }

    private Vec3 right() {
        return Vec3.directionFromRotation(0, this.yBodyRot + 90.0F);
    }

    private DustParticleOptions dust(int colour, float size) {
        return new DustParticleOptions(colour, size);
    }

    private List<LivingEntity> victims(ServerLevel level, AABB box) {
        return level.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive() && !this.isAlliedTo(e)
            && !(e instanceof CrucibleWalker) && !(e instanceof Mob m && m.getExistingDataOrNull(LabRegistry.BOSS.get()) != null)
            && !(e instanceof Player p && (p.isSpectator() || p.isCreative())));
    }

    private void ring(ServerLevel level, double radius, ParticleOptions particle, int n) {
        this.circle(level, this.position(), radius, particle, n);
    }

    private void circle(ServerLevel level, Vec3 c, double radius, ParticleOptions particle, int n) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2.0 / n;
            double x = c.x + Math.cos(a) * radius, z = c.z + Math.sin(a) * radius;
            Vec3 g = ground(level, x, c.y, z);
            if (g != null) {
                level.sendParticles(particle, g.x, g.y + 0.15, g.z, 1, 0.05, 0.0, 0.05, 0.0);
            }
        }
    }

    /** The top of solid ground near (x, y, z): searches 3 blocks up and 5 down; null over a drop. */
    private static @Nullable Vec3 ground(Level level, double x, double y, double z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(y) + 3, Mth.floor(z));
        for (int i = 0; i < 9; i++) {
            BlockPos below = p.below();
            BlockState under = level.getBlockState(below);
            if (!under.getCollisionShape(level, below).isEmpty() && level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                return new Vec3(x, p.getY(), z);
            }
            p.move(0, -1, 0);
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------ death and saving
    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.level() instanceof ServerLevel level && this.deathTime % 3 == 0) {
            level.sendParticles(STEAM, this.getX(), this.getY() + 2.5, this.getZ(), 8, 1.0, 0.8, 1.0, 0.05);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 2.0, this.getZ(), 4, 0.8, 0.8, 0.8, 0.02);
            if (this.deathTime % 12 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 2.5, this.getZ(), 12, 1.0, 1.0, 1.0, 0.2);
            }
        }
        if (this.deathTime == 1) {
            this.playSound(SoundEvents.IRON_GOLEM_DEATH, 1.6F, 0.5F);
        }
        if (this.deathTime >= 50 && !this.level().isClientSide() && !this.isRemoved()) {
            if (this.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.5, this.getZ(), 1, 0, 0, 0, 0);
            }
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("PhaseTwo", this.phaseTwo());
        output.putInt("Tint", this.tint());
        output.putBoolean("Started", this.act() != START);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_PHASE_TWO, input.getBooleanOr("PhaseTwo", false));
        this.entityData.set(DATA_TINT, input.getIntOr("Tint", VIOLET));
        if (input.getBooleanOr("Started", false)) {
            this.setAct(IDLE);
            this.setMode(WalkerMode.NORMAL);
        }
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }
}
