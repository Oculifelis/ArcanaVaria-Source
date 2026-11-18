package com.arcanavaria.exanimator;

import com.arcanavaria.AVDataMaps;
import static com.arcanavaria.exanimator.ExanimatorState.*;

import com.arcanavaria.thurible.ThuribleCloud;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Exanimator, Arcana Varia's mid-game boss: a robed reaper with a void for a heart, called by burning Mortuus and
 * Tenebrae together at night in a wizard tower ({@link ExanimatorRite}). A server-owned state machine like the Greatwood
 * Warden: every move has a visible warning, a damage window and a recovery.
 *
 * <p>Light is its weakness. A Lux cloud from a Thurible burns it and makes it reel (once per {@link #STUN_COOLDOWN}).
 * Below half health it turns ECLIPSE: it mends in the dark, burns in bright block light, and keeps nearby players in
 * Darkness, so the fight becomes about carrying light to it.
 */
public class Exanimator extends Monster {
    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(Exanimator.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE_START = SynchedEntityData.defineId(Exanimator.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PHASE_TWO = SynchedEntityData.defineId(Exanimator.class, EntityDataSerializers.BOOLEAN);

    private static final int WIPE_TICKS = 1200, MAW_COOLDOWN = 360, STUN_COOLDOWN = 300;
    private static final double LASH_LENGTH = 11.0;
    private static final ParticleOptions VOID = new DustParticleOptions(0x2A0F3A, 1.8F);
    private static final ParticleOptions GRAVE = new DustParticleOptions(0x7FE0C8, 1.3F);

    private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(),
        BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    // Saved: phase two, the last attack, timers and home. Not saved: the move in progress and what it owns (lanes, step
    // spot); a reload resumes in IDLE.
    private int stateTicks;
    private ExanimatorState lastAttack;
    private int wipeTicks;
    private int mawCooldown;
    private int stunCooldown;
    private BlockPos home = BlockPos.ZERO;
    private final List<Vec3> lanes = new ArrayList<>();
    private Vec3 stepTo = Vec3.ZERO;

    public Exanimator(EntityType<? extends Exanimator> type, Level level) {
        super(type, level);
        this.xpReward = 150;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 400.0)
            .add(Attributes.ARMOR, 10.0)
            .add(Attributes.ARMOR_TOUGHNESS, 4.0)
            .add(Attributes.ATTACK_DAMAGE, 12.0)
            .add(Attributes.MOVEMENT_SPEED, 0.24)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 48.0)
            .add(Attributes.STEP_HEIGHT, 2.0);
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
        builder.define(DATA_STATE, START.id());
        builder.define(DATA_STATE_START, 0);
        builder.define(DATA_PHASE_TWO, false);
    }

    public ExanimatorState state() {
        return ExanimatorState.byId(this.entityData.get(DATA_STATE));
    }

    /** Ticks since the current state began, from the synced start tick (the same on both sides). */
    public float stateTime(float partialTick) {
        return this.tickCount - this.entityData.get(DATA_STATE_START) + partialTick;
    }

    public boolean phaseTwo() {
        return this.entityData.get(DATA_PHASE_TWO);
    }

    private void setState(ExanimatorState next) {
        ExanimatorState old = this.state();
        if (old == LASH) {
            this.lanes.clear();
        }
        if (old == STEP) {
            this.stepTo = Vec3.ZERO;
        }
        this.entityData.set(DATA_STATE, next.id());
        this.entityData.set(DATA_STATE_START, this.tickCount);
        this.stateTicks = 0;
    }

    /** Called by the rite: remember where it was called. */
    public void awaken(BlockPos at) {
        this.home = at;
        this.setState(START);
    }

    // ------------------------------------------------------------------------------------------------ damage rules
    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN)
            || source.is(DamageTypes.CRAMMING)) {
            return true;
        }
        ExanimatorState s = this.state();
        boolean faded = s == STEP && this.stateTicks >= 10 && this.stateTicks < 20;
        if ((s == START || s == ECLIPSE || faded) && !source.is(DamageTypes.GENERIC_KILL)) {
            return true;
        }
        return super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.state() == STUN) {
            damage *= 1.5F;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.removeAllPlayers();
    }

    // ------------------------------------------------------------------------------------------------ the fight
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F));
        if (this.mawCooldown > 0) {
            this.mawCooldown--;
        }
        if (this.stunCooldown > 0) {
            this.stunCooldown--;
        }
        LivingEntity target = this.getTarget();
        boolean valid = target != null && target.isAlive() && this.distanceToSqr(target) < 48 * 48
            && !(target instanceof Player p && (p.isSpectator() || p.isCreative()));
        if (!valid) {
            target = null;
            this.setTarget(null);
        }
        if (target == null && this.state() != START) {
            if (++this.wipeTicks >= AVDataMaps.move(this, "reset_ticks", WIPE_TICKS)) {
                this.fadeAway(level);
                return;
            }
        } else {
            this.wipeTicks = 0;
        }
        this.lightRules(level);
        ExanimatorState s = this.state();
        int t = this.stateTicks++;
        if (s != IDLE && s != START) {
            this.getNavigation().stop();
        }
        if (target != null && s != START && s != STUN && s != ECLIPSE && !(s == STEP && t < 20)) {
            this.getLookControl().setLookAt(target, 20.0F, 20.0F);
        }
        switch (s) {
            case START -> this.tickStart(level, t);
            case IDLE -> this.tickIdle(level, t, target);
            case REAP -> this.tickReap(level, t);
            case HARVEST -> this.tickHarvest(level, t);
            case LASH -> this.tickLash(level, t);
            case STEP -> this.tickStep(level, t, target);
            case MAW -> this.tickMaw(level, t);
            case STUN -> {
                if (t % 5 == 0) {
                    level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 5.2, this.getZ(), 5, 0.6, 0.1, 0.6, 0.0);
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 3.5, this.getZ(), 4, 0.6, 0.8, 0.6, 0.02);
                }
            }
            case ECLIPSE -> this.tickEclipse(level, t);
            default -> { }
        }
        if (this.state() == s && s != IDLE && s != START && t + 1 >= s.length()) {
            this.setState(IDLE);
        }
    }

    /**
     * Light is its weakness: a Lux cloud touching it burns it and (off cooldown) makes it reel. In phase two it also
     * mends in deep shadow, burns in bright block light, and keeps everyone near it in Darkness.
     */
    private void lightRules(ServerLevel level) {
        ExanimatorState s = this.state();
        if (this.tickCount % 10 == 0 && s != START && s != ECLIPSE) {
            AABB box = this.getBoundingBox().inflate(6.0);
            for (ThuribleCloud c : level.getEntitiesOfClass(ThuribleCloud.class, box, c -> c.aspect().map(ExanimatorRite::isLux).orElse(false))) {
                double r = c.radius() + 1.4;
                double dx = c.getX() - this.getX(), dz = c.getZ() - this.getZ();
                double dy = c.getY() - Mth.clamp(c.getY(), this.getY(), this.getY() + 5.0);
                if (dx * dx + dy * dy + dz * dz > r * r) {
                    continue;
                }
                this.hurtServer(level, this.damageSources().magic(), (float) AVDataMaps.move(this, "lux_damage", 5.0));
                level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 3.0, this.getZ(), 12, 0.8, 1.2, 0.8, 0.05);
                if (this.stunCooldown == 0 && s != STUN) {
                    this.stunCooldown = (int) AVDataMaps.move(this, "stun_cooldown", STUN_COOLDOWN);
                    this.playSound(SoundEvents.WITHER_HURT, 1.6F, 0.5F);
                    this.setState(STUN);
                }
                break;
            }
        }
        if (!this.phaseTwo() || this.tickCount % 20 != 0 || s == ECLIPSE) {
            return;
        }
        int light = level.getBrightness(LightLayer.BLOCK, BlockPos.containing(this.getX(), this.getY() + 2.5, this.getZ()));
        if (light >= 12) {
            this.hurtServer(level, this.damageSources().magic(), (float) AVDataMaps.move(this, "light_damage", 3.0));
            level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 3.0, this.getZ(), 10, 0.7, 1.4, 0.7, 0.02);
        } else if (light <= 4 && this.getHealth() < this.getMaxHealth()) {
            this.heal((float) AVDataMaps.move(this, "dark_heal", 2.0));
            level.sendParticles(VOID, this.getX(), this.getY() + 3.2, this.getZ(), 6, 0.6, 0.8, 0.6, 0.0);
        }
        if (this.tickCount % 100 == 0) {
            for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(24.0), p -> !p.isSpectator() && !p.isCreative())) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0, false, false), this);
            }
        }
    }

    private void tickStart(ServerLevel level, int t) {
        if (t % 3 == 0) {
            level.sendParticles(VOID, this.getX(), this.getY() + 0.4 + t * 0.05, this.getZ(), 14, 1.2, 0.4 + t * 0.02, 1.2, 0.02);
            level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 0.3, this.getZ(), 3, 1.4, 0.1, 1.4, 0.03);
        }
        if (t == 0) {
            this.playSound(SoundEvents.AMBIENT_CAVE.value(), 2.0F, 0.6F);
        }
        if (t == 60) {
            this.playSound(SoundEvents.WITHER_SPAWN, 1.4F, 0.7F);
            level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 4.0, this.getZ(), 40, 1.5, 1.5, 1.5, 0.08);
        }
        if (t + 1 >= START.length()) {
            if (this.home.equals(BlockPos.ZERO)) {
                this.home = this.blockPosition();
            }
            this.setState(IDLE);
        }
    }

    private void tickIdle(ServerLevel level, int t, LivingEntity target) {
        if (target == null) {
            if (this.home.distSqr(this.blockPosition()) > 9) {
                this.getNavigation().moveTo(this.home.getX() + 0.5, this.home.getY(), this.home.getZ() + 0.5, 0.8);
            }
            return;
        }
        if (!this.phaseTwo() && this.getHealth() < this.getMaxHealth() * 0.5F) {
            this.setState(ECLIPSE);
            return;
        }
        boolean far = this.home.distSqr(this.blockPosition()) > 32 * 32;
        if (far) {
            this.getNavigation().moveTo(this.home.getX() + 0.5, this.home.getY(), this.home.getZ() + 0.5, 1.0);
        } else {
            this.getNavigation().moveTo(target, 1.0);
        }
        if (t < (this.phaseTwo() ? 16 : 26)) {
            return;
        }
        double d = Math.sqrt(this.distanceToSqr(target));
        List<ExanimatorState> bag = new ArrayList<>();
        if (d < 6.0) {
            bag.add(REAP);
        }
        if (d < 8.0) {
            bag.add(HARVEST);
        }
        if (d >= 4.0 && d < 14.0) {
            bag.add(LASH);
        }
        if (d >= 7.0) {
            bag.add(STEP);
        }
        if (this.mawCooldown == 0 && d < 14.0) {
            bag.add(MAW);
        }
        if (bag.isEmpty()) {
            return;                                    // out of reach: keep walking
        }
        if (bag.size() > 1) {
            bag.remove(this.lastAttack);
        }
        this.startAttack(bag.get(this.random.nextInt(bag.size())), target);
    }

    private void startAttack(ExanimatorState s, LivingEntity target) {
        this.lastAttack = s;
        this.setState(s);
        if (s == LASH) {
            this.lanes.clear();
            double base = Math.atan2(target.getZ() - this.getZ(), target.getX() - this.getX());
            int n = this.phaseTwo() ? 5 : 3;
            for (int i = 0; i < n; i++) {
                double a = base + Math.toRadians(16.0) * (i - (n - 1) / 2.0);
                this.lanes.add(new Vec3(Math.cos(a), 0, Math.sin(a)));
            }
        }
        if (s == STEP) {
            Vec3 away = new Vec3(target.getX() - this.getX(), 0, target.getZ() - this.getZ());
            away = away.lengthSqr() > 1e-4 ? away.normalize() : new Vec3(0, 0, 1);
            Vec3 behind = target.position().add(away.scale(3.0));
            Vec3 g = ground(this.level(), behind.x, target.getY(), behind.z);
            this.stepTo = g == null ? target.position() : g;
        }
        if (s == MAW) {
            this.mawCooldown = (int) AVDataMaps.move(this, "maw_cooldown", MAW_COOLDOWN);
        }
    }

    /** Scythe chop: a cone 6 blocks deep in front. Phase two answers with a backhand. */
    private void tickReap(ServerLevel level, int t) {
        Vec3 blade = this.position().add(0, 6.0, 0).add(this.forward().scale(-0.5)).add(this.right().scale(1.6));
        if (t < 15 && t % 2 == 0) {
            level.sendParticles(GRAVE, blade.x, blade.y, blade.z, 3, 0.4, 0.4, 0.4, 0.0);
        }
        if (t == 2) {
            this.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 1.2F, 0.5F);
        }
        if (t == 16) {
            this.cleave(level, 6.0, 0.3, (float) AVDataMaps.move(this, "reap_damage", 14.0), true);
        }
        if (this.phaseTwo() && t == 26) {
            this.cleave(level, 5.5, 0.1, (float) AVDataMaps.move(this, "reap_second_damage", 10.0), false);
        }
    }

    private void cleave(ServerLevel level, double reach, double cone, float damage, boolean wither) {
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.6F, 0.5F);
        Vec3 look = this.forward();
        for (int i = -4; i <= 4; i++) {
            Vec3 d = look.yRot((float) Math.toRadians(i * 12.0)).scale(reach * 0.65);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + d.x, this.getY() + 1.2, this.getZ() + d.z, 1, 0, 0, 0, 0);
        }
        for (LivingEntity e : this.victims(level, this.getBoundingBox().inflate(reach, 1.0, reach))) {
            Vec3 to = new Vec3(e.getX() - this.getX(), 0, e.getZ() - this.getZ());
            if (to.length() < reach + e.getBbWidth() / 2 && (to.lengthSqr() < 1e-4 || look.dot(to.normalize()) > cone)) {
                if (e.hurtServer(level, this.damageSources().mobAttack(this), damage) && wither) {
                    e.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1), this);
                }
            }
        }
    }

    /** Drags everyone within 10 blocks in, then a full-circle sweep. */
    private void tickHarvest(ServerLevel level, int t) {
        if (t == 0) {
            this.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 1.6F, 0.6F);
        }
        if (t < 24) {
            double r = 10.0 - t * 0.25;
            if (t % 3 == 0) {
                this.ring(level, r, ParticleTypes.SCULK_SOUL, 30);
            }
            for (LivingEntity e : this.victims(level, this.getBoundingBox().inflate(10.0, 2.0, 10.0))) {
                Vec3 to = new Vec3(this.getX() - e.getX(), 0, this.getZ() - e.getZ());
                double dist = to.length();
                if (dist > 2.5 && dist < 10.0) {
                    Vec3 pull = to.normalize().scale(this.phaseTwo() ? 0.11 : 0.08);
                    e.push(pull.x, 0.0, pull.z);
                    e.hurtMarked = true;
                }
            }
        }
        if (t == 26) {
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.4F);
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8.0;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + Math.cos(a) * 4.0, this.getY() + 1.2, this.getZ() + Math.sin(a) * 4.0, 1, 0, 0, 0, 0);
            }
            for (LivingEntity e : this.victims(level, this.getBoundingBox().inflate(6.5, 1.5, 6.5))) {
                Vec3 out = new Vec3(e.getX() - this.getX(), 0, e.getZ() - this.getZ());
                if (out.length() < 6.5 && e.hurtServer(level, this.damageSources().mobAttack(this), AVDataMaps.move(this, "harvest_damage", this.phaseTwo(), 12.0, 14.0))) {
                    out = out.lengthSqr() > 1e-4 ? out.normalize().scale(1.2) : Vec3.ZERO;
                    e.push(out.x, 0.45, out.z);
                    e.hurtMarked = true;
                }
            }
        }
    }

    /** The back tentacles spear along lanes: soul fire crawls along each lane first. */
    private void tickLash(ServerLevel level, int t) {
        final double LASH_LENGTH = AVDataMaps.move(this, "lash_length", Exanimator.LASH_LENGTH);
        if (t == 0) {
            this.playSound(SoundEvents.WARDEN_TENDRIL_CLICKS, 2.0F, 0.5F);
        }
        for (Vec3 dir : this.lanes) {
            if (t < 18 && t % 2 == 0) {
                for (double d = 1.5; d < LASH_LENGTH; d += 1.0) {
                    if ((int) d % 3 != (t / 2) % 3) {
                        continue;
                    }
                    Vec3 g = ground(level, this.getX() + dir.x * d, this.getY(), this.getZ() + dir.z * d);
                    if (g != null) {
                        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, g.x, g.y + 0.1, g.z, 1, 0.15, 0.0, 0.15, 0.0);
                    }
                }
            }
            if (t == 19) {
                for (double d = 1.5; d < LASH_LENGTH; d += 0.7) {
                    level.sendParticles(ParticleTypes.SQUID_INK, this.getX() + dir.x * d, this.getY() + 1.0, this.getZ() + dir.z * d, 2, 0.2, 0.3, 0.2, 0.02);
                }
            }
        }
        if (t == 19) {
            this.playSound(SoundEvents.TRIDENT_THROW.value(), 1.6F, 0.4F);
            Vec3 from = this.position();
            for (LivingEntity e : this.victims(level, this.getBoundingBox().inflate(LASH_LENGTH, 2.0, LASH_LENGTH))) {
                Vec3 to = new Vec3(e.getX() - from.x, 0, e.getZ() - from.z);
                for (Vec3 dir : this.lanes) {
                    double along = to.dot(dir);
                    double side = to.subtract(dir.scale(along)).length();
                    if (along > 1.0 && along < LASH_LENGTH && side < 1.2 + e.getBbWidth() / 2) {
                        if (e.hurtServer(level, this.damageSources().mobAttack(this), (float) AVDataMaps.move(this, "lash_damage", 10.0))) {
                            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
                        }
                        break;
                    }
                }
            }
        }
    }

    /** Fades to smoke, walks out of the smoke behind its target and cuts. Untouchable while faded. */
    private void tickStep(ServerLevel level, int t, LivingEntity target) {
        if (t < 12 && t % 2 == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 2.5, this.getZ(), 10, 0.8, 1.6, 0.8, 0.01);
        }
        if (t == 2) {
            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.4F, 0.4F);
        }
        if (t >= 8 && t < 20 && t % 2 == 0) {             // where it will step out: soul fire gathering on the ground
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI / 5.0 + t * 0.2;
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.stepTo.x + Math.cos(a) * 1.4, this.stepTo.y + 0.1,
                    this.stepTo.z + Math.sin(a) * 1.4, 1, 0, 0.02, 0, 0.0);
            }
        }
        if (t == 20) {
            this.teleportTo(this.stepTo.x, this.stepTo.y, this.stepTo.z);
            if (target != null) {
                this.lookAt(target, 360.0F, 360.0F);
                this.setYBodyRot(this.getYRot());
                this.setYHeadRot(this.getYRot());
            }
            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.4F, 0.6F);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 2.5, this.getZ(), 30, 0.8, 1.6, 0.8, 0.03);
        }
        if (t == 25) {
            this.cleave(level, 4.5, -0.2, (float) AVDataMaps.move(this, "step_damage", 12.0), true);
        }
    }

    /** The ribs open and the void in its chest drags everything in; up close it eats at you, then snaps shut. */
    private void tickMaw(ServerLevel level, int t) {
        Vec3 heart = this.heart();
        if (t == 0) {
            this.playSound(SoundEvents.PORTAL_TRIGGER, 1.4F, 0.4F);
        }
        if (t >= 14 && t < 60) {
            if (t % 2 == 0) {
                for (int i = 0; i < 4; i++) {
                    Vec3 o = new Vec3(this.random.nextGaussian(), this.random.nextGaussian() * 0.6, this.random.nextGaussian()).normalize().scale(6.0);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, heart.x + o.x, heart.y + o.y, heart.z + o.z, 0, -o.x, -o.y, -o.z, 0.15);
                }
            }
            if (t % 20 == 14) {
                this.playSound(SoundEvents.WARDEN_HEARTBEAT, 2.0F, 0.5F);
            }
            for (LivingEntity e : this.victims(level, this.getBoundingBox().inflate(12.0, 4.0, 12.0))) {
                Vec3 to = heart.subtract(e.position().add(0, e.getBbHeight() / 2, 0));
                double dist = to.length();
                if (dist > 1.5 && dist < 12.0) {
                    Vec3 pull = to.normalize().scale((this.phaseTwo() ? 0.10 : 0.075) * (1.0 - dist / 16.0));
                    e.push(pull.x, Math.max(0.0, pull.y) * 0.5, pull.z);
                    e.hurtMarked = true;
                }
                if (dist < 3.5 && t % 10 == 4) {
                    e.hurtServer(level, this.damageSources().indirectMagic(this, this), (float) AVDataMaps.move(this, "maw_damage", 4.0));
                    e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0), this);
                }
            }
        }
        if (t == 60) {
            this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.4F, 0.7F);
            level.sendParticles(VOID, heart.x, heart.y, heart.z, 40, 1.0, 1.0, 1.0, 0.1);
            for (LivingEntity e : this.victims(level, this.getBoundingBox().inflate(3.5, 2.0, 3.5))) {
                if (e.hurtServer(level, this.damageSources().mobAttack(this), (float) AVDataMaps.move(this, "maw_burst_damage", 8.0))) {
                    Vec3 out = new Vec3(e.getX() - this.getX(), 0, e.getZ() - this.getZ());
                    out = out.lengthSqr() > 1e-4 ? out.normalize() : Vec3.ZERO;
                    e.push(out.x, 0.3, out.z);
                    e.hurtMarked = true;
                }
            }
        }
    }

    /** Half health: the light dies around it. From here it mends in the dark and burns in bright light. */
    private void tickEclipse(ServerLevel level, int t) {
        if (t == 10) {
            this.playSound(SoundEvents.WITHER_SPAWN, 2.0F, 0.5F);
            level.sendParticles(VOID, this.getX(), this.getY() + 3.5, this.getZ(), 80, 2.0, 2.0, 2.0, 0.2);
            for (LivingEntity e : this.victims(level, this.getBoundingBox().inflate(7.0))) {
                Vec3 push = new Vec3(e.getX() - this.getX(), 0, e.getZ() - this.getZ());
                if (push.lengthSqr() > 1e-4) {
                    push = push.normalize().scale(1.3);
                    e.push(push.x, 0.4, push.z);
                    e.hurtMarked = true;
                }
            }
            for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(24.0), p -> !p.isSpectator())) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0, false, false), this);
            }
        }
        if (t % 4 == 0) {
            this.ring(level, 2.0 + t * 0.15, VOID, 24);
        }
        if (t + 1 >= ECLIPSE.length()) {
            this.entityData.set(DATA_PHASE_TWO, true);
            this.bossEvent.setDarkenScreen(true);
        }
    }

    /** Wipe timeout: nobody left to reap, so it sinks back into the dark. */
    private void fadeAway(ServerLevel level) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 2.5, this.getZ(), 60, 1.0, 2.0, 1.0, 0.05);
        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.4F, 0.3F);
        this.discard();
    }

    // ------------------------------------------------------------------------------------------------ helpers
    /** The void in its chest. */
    public Vec3 heart() {
        return this.position().add(0, 3.6, 0).add(this.forward().scale(0.6));
    }

    private Vec3 forward() {
        return Vec3.directionFromRotation(0, this.yBodyRot);
    }

    private Vec3 right() {
        return Vec3.directionFromRotation(0, this.yBodyRot + 90.0F);
    }

    private List<LivingEntity> victims(ServerLevel level, AABB box) {
        return level.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive() && !this.isAlliedTo(e)
            && !(e instanceof Exanimator) && !(e instanceof Player p && (p.isSpectator() || p.isCreative())));
    }

    private void ring(ServerLevel level, double radius, ParticleOptions particle, int n) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2.0 / n;
            double x = this.getX() + Math.cos(a) * radius, z = this.getZ() + Math.sin(a) * radius;
            Vec3 g = ground(level, x, this.getY(), z);
            if (g != null) {
                level.sendParticles(particle, g.x, g.y + 0.15, g.z, 1, 0.05, 0.0, 0.05, 0.0);
            }
        }
    }

    /** The top of solid ground near (x, y, z): searches 3 blocks up and 5 down; null over a drop. */
    static Vec3 ground(Level level, double x, double y, double z) {
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
            level.sendParticles(VOID, this.getX(), this.getY() + 3.0, this.getZ(), 14, 1.0, 1.8, 1.0, 0.05);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()),
                this.getX(), this.getY() + 1.5, this.getZ(), 10, 1.0, 1.0, 1.0, 0.1);
            if (this.deathTime % 15 == 0) {
                level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 3.5, this.getZ(), 12, 0.6, 0.6, 0.6, 0.06);
            }
        }
        if (this.deathTime == 1) {
            this.playSound(SoundEvents.WITHER_DEATH, 1.2F, 0.6F);
        }
        if (this.deathTime >= 60 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("PhaseTwo", this.phaseTwo());
        if (this.lastAttack != null) {
            output.putString("LastAttack", this.lastAttack.serialName());
        }
        output.putInt("MawCooldown", this.mawCooldown);
        output.putInt("StunCooldown", this.stunCooldown);
        output.putInt("WipeTicks", this.wipeTicks);
        output.putInt("HomeX", this.home.getX());
        output.putInt("HomeY", this.home.getY());
        output.putInt("HomeZ", this.home.getZ());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        boolean two = input.getBooleanOr("PhaseTwo", false);
        this.entityData.set(DATA_PHASE_TWO, two);
        this.bossEvent.setDarkenScreen(two);
        this.lastAttack = ExanimatorState.byName(input.getStringOr("LastAttack", ""));
        this.mawCooldown = input.getIntOr("MawCooldown", 0);
        this.stunCooldown = input.getIntOr("StunCooldown", 0);
        this.wipeTicks = input.getIntOr("WipeTicks", 0);
        this.home = new BlockPos(input.getIntOr("HomeX", 0), input.getIntOr("HomeY", 0), input.getIntOr("HomeZ", 0));
        this.setState(IDLE);
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
