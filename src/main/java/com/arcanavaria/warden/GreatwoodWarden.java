package com.arcanavaria.warden;

import com.arcanavaria.AVDataMaps;
import static com.arcanavaria.warden.WardenState.*;

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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Greatwood Warden, Arcana Varia's early boss. A server-owned state machine (docs/WARDEN.md has the full table):
 * every attack has a visible warning, a damage window and a recovery. Moves come from the Terraria boss research pack:
 * Eaglecrest Golem's awakening, charged eye ray and ground slam, Thorn's sprouting root lanes, root teleport,
 * health-gated phase change and one-time finale, and Corpse Bloom's melee interrupt (here, of the leaf heal).
 *
 * <p>Clients get only the state and the tick it started ({@link #DATA_STATE}, {@link #DATA_STATE_START}) and animate
 * from those.
 */
public class GreatwoodWarden extends Monster {
    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(GreatwoodWarden.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE_START = SynchedEntityData.defineId(GreatwoodWarden.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PHASE_TWO = SynchedEntityData.defineId(GreatwoodWarden.class, EntityDataSerializers.BOOLEAN);

    private static final double SLAM_RADIUS = 4.5, BURST_RADIUS = 3.5;
    private static final int WIPE_TICKS = 1200, MEND_COOLDOWN = 400;
    private static final ParticleOptions AMBER = new DustParticleOptions(0xFFB028, 1.4F);
    private static final ParticleOptions LEAF = new DustParticleOptions(0x5E9A3A, 1.2F);

    private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(),
        BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);

    // ---- server state. Saved: the latches (phase two, finale), the rotation (last attack), timers and home. Not saved:
    // the attack in progress and everything it owns (aim, lanes, burrow spot, mend damage); a reload resumes in IDLE,
    // and a finale cut off by a reload is still QUEUED, so it plays again.
    private int stateTicks;
    private WardenState lastAttack;
    private WardenState.Finale finale = WardenState.Finale.PENDING;
    private int wipeTicks;
    private int mendCooldown;
    private float mendDamage;
    private BlockPos home = BlockPos.ZERO;
    private Vec3 aim = Vec3.ZERO;
    private final List<Vec3> lanes = new ArrayList<>();
    private Vec3 burrowTo = Vec3.ZERO;

    public GreatwoodWarden(EntityType<? extends GreatwoodWarden> type, Level level) {
        super(type, level);
        this.xpReward = 60;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 220.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.ATTACK_DAMAGE, 9.0)
            .add(Attributes.ATTACK_KNOCKBACK, 1.5)
            .add(Attributes.MOVEMENT_SPEED, 0.23)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 48.0)
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
        builder.define(DATA_STATE, START.id());
        builder.define(DATA_STATE_START, 0);
        builder.define(DATA_PHASE_TWO, false);
    }

    public WardenState state() {
        return WardenState.byId(this.entityData.get(DATA_STATE));
    }

    public WardenState.Finale finale() {
        return this.finale;
    }

    /** Ticks since the current state began, from the synced start tick (works the same on both sides). */
    public float stateTime(float partialTick) {
        return this.tickCount - this.entityData.get(DATA_STATE_START) + partialTick;
    }

    public boolean phaseTwo() {
        return this.entityData.get(DATA_PHASE_TWO);
    }

    /**
     * The one place the Warden changes move. Clears what the move it leaves owned (root lanes, burrow spot, mend damage,
     * aim), marks a finished finale DONE, then starts the new move at tick 0 and syncs it.
     */
    private void setState(WardenState next) {
        WardenState old = this.state();
        if (old == ROOTS) {
            this.lanes.clear();
            if (this.finale == WardenState.Finale.QUEUED) {
                this.finale = WardenState.Finale.DONE;       // the finale is only ever the ROOTS that follows QUEUED
            }
        }
        if (old == MEND) {
            this.mendDamage = 0.0F;
        }
        if (old == BURROW) {
            this.burrowTo = Vec3.ZERO;
        }
        if (old != IDLE && old != START) {
            this.aim = Vec3.ZERO;
        }
        this.entityData.set(DATA_STATE, next.id());
        this.entityData.set(DATA_STATE_START, this.tickCount);
        this.stateTicks = 0;
    }

    /** Called by the Heartseed: remember where the grove is. */
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
        WardenState s = this.state();
        boolean underground = s == BURROW && this.stateTicks >= 8 && this.stateTicks < 35;
        if ((s == START || s == ROAR || underground) && !source.is(DamageTypes.GENERIC_KILL)) {
            return true;
        }
        return super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.state() == STUN) {
            damage *= 1.5F;
        }
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.state() == MEND) {
            this.mendDamage += damage;
            if (this.mendDamage >= 15.0F) {          // Corpse Bloom's interrupt: hit it hard enough and it staggers
                this.playSound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.2F, 0.6F);
                level.sendParticles(LEAF, this.getX(), this.getY() + 2.0, this.getZ(), 30, 1.0, 1.0, 1.0, 0.1);
                this.setState(STUN);
            }
        }
        return hurt;
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
        if (this.mendCooldown > 0) {
            this.mendCooldown--;
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
                this.retreat(level);
                return;
            }
        } else {
            this.wipeTicks = 0;
        }
        WardenState s = this.state();
        int t = this.stateTicks++;
        if (s != IDLE && s != START) {
            this.getNavigation().stop();
        }
        if (target != null && s != START && s != BURROW && s != STUN && s != MEND) {
            this.getLookControl().setLookAt(target, 20.0F, 20.0F);
        }
        switch (s) {
            case START -> this.tickStart(level, t);
            case IDLE -> this.tickIdle(level, t, target);
            case SWIPE -> this.tickSwipe(level, t);
            case SLAM -> this.tickSlam(level, t);
            case ROOTS -> this.tickRoots(level, t, target);
            case VOLLEY -> this.tickVolley(level, t, target);
            case RAY -> this.tickRay(level, t, target);
            case MEND -> this.tickMend(level, t);
            case STUN -> {
                if (t % 6 == 0) {
                    level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 3.2, this.getZ(), 4, 0.5, 0.1, 0.5, 0.0);
                }
            }
            case BURROW -> this.tickBurrow(level, t, target);
            case ROAR -> this.tickRoar(level, t);
            default -> { }
        }
        if (s != IDLE && s != START && t + 1 >= this.length(s)) {
            this.setState(IDLE);
        }
    }

    private int length(WardenState s) {
        if (s == SLAM && this.phaseTwo()) {
            return 38;
        }
        return s.length();
    }

    private void tickStart(ServerLevel level, int t) {
        if (t % 4 == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()),
                this.getX(), this.getY() + 0.3, this.getZ(), 20, 1.0, 0.2, 1.0, 0.2);
            this.playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 0.6F);
        }
        if (t == 40) {
            this.playSound(SoundEvents.RAVAGER_ROAR, 1.6F, 0.6F);
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
        // phase change and finale latches come first, then the leaf heal, then the bag
        if (!this.phaseTwo() && this.getHealth() < this.getMaxHealth() * 0.5F) {
            this.setState(ROAR);
            return;
        }
        if (this.finale == WardenState.Finale.PENDING && this.getHealth() < this.getMaxHealth() * 0.2F) {
            this.finale = WardenState.Finale.QUEUED;
        }
        boolean far = this.home.distSqr(this.blockPosition()) > 32 * 32;
        if (far) {
            this.getNavigation().moveTo(this.home.getX() + 0.5, this.home.getY(), this.home.getZ() + 0.5, 1.0);
        } else {
            this.getNavigation().moveTo(target, 1.0);
        }
        if (t < (this.phaseTwo() ? 18 : 30)) {
            return;
        }
        double d = Math.sqrt(this.distanceToSqr(target));
        WardenState next;
        if (this.finale == WardenState.Finale.QUEUED) {
            next = ROOTS;
            this.lanes.clear();
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4.0;
                this.lanes.add(new Vec3(Math.cos(a), 0, Math.sin(a)));
            }
            this.startAttack(next, target, true);
            return;
        }
        if (this.mendCooldown == 0 && this.getHealth() < this.getMaxHealth() * 0.7F && this.leavesNearby(level) >= 6) {
            next = MEND;
        } else {
            List<WardenState> bag = new ArrayList<>();
            if (d < 4.0) {
                bag.add(SWIPE);
            }
            if (d < 6.0) {
                bag.add(SLAM);
            }
            if (d >= 4.0) {
                bag.add(ROOTS);
                bag.add(VOLLEY);
            }
            if (d >= 6.0 && this.hasLineOfSight(target)) {
                bag.add(RAY);
            }
            if (this.phaseTwo() && d >= 5.0) {
                bag.add(BURROW);
            }
            if (bag.size() > 1) {
                bag.remove(this.lastAttack);
            }
            next = bag.get(this.random.nextInt(bag.size()));
        }
        this.startAttack(next, target, false);
    }

    private void startAttack(WardenState s, LivingEntity target, boolean keepLanes) {
        this.lastAttack = s;
        this.setState(s);
        Vec3 to = target.getEyePosition().subtract(this.heart());
        this.aim = to.lengthSqr() > 1e-4 ? to.normalize() : this.getLookAngle();
        if (s == ROOTS && !keepLanes) {
            this.lanes.clear();
            Vec3 flat = new Vec3(target.getX() - this.getX(), 0, target.getZ() - this.getZ());
            double base = Math.atan2(flat.z, flat.x);
            int n = this.phaseTwo() ? 5 : 3;
            for (int i = 0; i < n; i++) {
                double a = base + Math.toRadians(22.0) * (i - (n - 1) / 2.0);
                this.lanes.add(new Vec3(Math.cos(a), 0, Math.sin(a)));
            }
        }
        if (s == MEND) {
            this.mendDamage = 0.0F;
            this.mendCooldown = (int) AVDataMaps.move(this, "mend_cooldown", MEND_COOLDOWN);
        }
        if (s == BURROW) {
            double a = this.random.nextDouble() * Math.PI * 2.0;
            this.burrowTo = target.position().add(Math.cos(a) * 3.0, 0, Math.sin(a) * 3.0);
            Vec3 ground = this.ground(this.level(), this.burrowTo.x, target.getY(), this.burrowTo.z);
            this.burrowTo = ground == null ? target.position() : ground;
        }
    }

    private void tickSwipe(ServerLevel level, int t) {
        if (t == 4) {
            this.playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 0.7F);
        }
        if (t == 9) {
            Vec3 look = Vec3.directionFromRotation(0, this.getYRot());
            for (LivingEntity e : this.victims(level, this.getBoundingBox().inflate(3.5, 0.5, 3.5))) {
                Vec3 to = e.position().subtract(this.position());
                if (to.horizontalDistance() < 4.2 && look.dot(new Vec3(to.x, 0, to.z).normalize()) > 0.2) {
                    this.doHurtTarget(level, e);
                }
            }
        }
    }

    private double slamRadius() {
        return AVDataMaps.move(this, "slam_radius", SLAM_RADIUS);
    }

    private void tickSlam(ServerLevel level, int t) {
        if (t < 20 && t % 4 == 0) {
            this.ring(level, this.slamRadius(), LEAF, 28);
            if (t == 0) {
                this.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 1.1F);
            }
        }
        if (t == 20) {
            this.impact(level, 0.0, this.slamRadius(), AVDataMaps.move(this, "slam_damage", this.phaseTwo(), 10.0, 12.0), 0.6);
        }
        if (this.phaseTwo() && t > 20 && t < 28 && t % 3 == 0) {
            this.ring(level, AVDataMaps.move(this, "shockwave_radius", 8.0), LEAF, 40);
        }
        if (this.phaseTwo() && t == 28) {
            this.impact(level, this.slamRadius(), AVDataMaps.move(this, "shockwave_radius", 8.0), (float) AVDataMaps.move(this, "shockwave_damage", 7.0), 0.4);
        }
    }

    /** Ground impact: everyone standing on the ground in the band [inner, outer] is hurt and thrown up. */
    private void impact(ServerLevel level, double inner, double outer, float damage, double lift) {
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.6F);
        BlockState below = this.level().getBlockState(this.blockPosition().below());
        for (int i = 0; i < 3; i++) {
            this.ring(level, inner + (outer - inner) * (i + 1) / 3.0, new BlockParticleOption(ParticleTypes.BLOCK,
                below.isAir() ? Blocks.DIRT.defaultBlockState() : below), 24);
        }
        for (LivingEntity e : this.victims(level, this.getBoundingBox().inflate(outer, 1.5, outer))) {
            double h = e.position().subtract(this.position()).horizontalDistance();
            if (h >= inner && h <= outer && (e.onGround() || e.getY() - this.getY() < 1.2)) {
                if (e.hurtServer(level, this.damageSources().mobAttack(this), damage)) {
                    Vec3 push = new Vec3(e.getX() - this.getX(), 0, e.getZ() - this.getZ());
                    push = push.lengthSqr() > 1e-4 ? push.normalize().scale(0.5) : Vec3.ZERO;
                    e.push(push.x, lift, push.z);
                    e.hurtMarked = true;
                }
            }
        }
    }

    private void tickRoots(ServerLevel level, int t, LivingEntity target) {
        if (t == 0) {
            this.playSound(SoundEvents.ROOTS_BREAK, 1.5F, 0.5F);
        }
        float damage = AVDataMaps.move(this, "roots_damage", this.phaseTwo(), 6.0, 7.0);
        for (Vec3 dir : this.lanes) {
            for (int j = 0; j < 12; j++) {
                double dist = 1.8 + j;
                double x = this.getX() + dir.x * dist, z = this.getZ() + dir.z * dist;
                if (t < 24 && t % 4 == j % 4) {                  // the sprout warning crawls along the lane
                    Vec3 g = this.ground(level, x, this.getY(), z);
                    if (g != null) {
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()),
                            g.x, g.y + 0.1, g.z, 2, 0.2, 0.0, 0.2, 0.0);
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, g.x, g.y + 0.2, g.z, 1, 0.2, 0.0, 0.2, 0.0);
                    }
                }
                if (t == 24 + j / 2) {                          // erupts nearest first, two roots per tick
                    Vec3 g = this.ground(level, x, this.getY(), z);
                    if (g != null) {
                        level.addFreshEntity(new RootSpike(level, g.x, g.y, g.z, (float) Math.toDegrees(Math.atan2(dir.z, dir.x)), damage, this));
                    }
                }
            }
        }
    }

    private void tickVolley(ServerLevel level, int t, LivingEntity target) {
        boolean throwNow = t == 12 || t == 18 || t == 24 || (this.phaseTwo() && (t == 15 || t == 21));
        if (!throwNow || target == null) {
            return;
        }
        WardenSplinter s = new WardenSplinter(level, this);
        Vec3 from = this.position().add(0, 2.6, 0);
        s.setPos(from);
        double dx = target.getX() - from.x, dz = target.getZ() - from.z;
        double dy = target.getY(0.5) - from.y;
        double h = Math.sqrt(dx * dx + dz * dz);
        s.shoot(dx, dy + h * 0.12, dz, 1.4F, 6.0F);
        level.addFreshEntity(s);
        this.playSound(SoundEvents.WITCH_THROW, 1.0F, 0.6F);
    }

    private void tickRay(ServerLevel level, int t, LivingEntity target) {
        Vec3 heart = this.heart();
        boolean tracking = target != null && (t < 30 || this.phaseTwo());
        if (tracking) {
            Vec3 want = target.getEyePosition().subtract(heart).normalize();
            double maxTurn = Math.toRadians(t < 30 ? 4.0 : 1.0);
            this.aim = turn(this.aim, want, maxTurn);
        }
        if (t < 30) {
            if (t % 2 == 0) {                                   // amber gathering into the heart
                for (int i = 0; i < 3; i++) {
                    Vec3 o = new Vec3(this.random.nextGaussian(), this.random.nextGaussian(), this.random.nextGaussian()).normalize().scale(1.8);
                    level.sendParticles(AMBER, heart.x + o.x, heart.y + o.y, heart.z + o.z, 0, -o.x, -o.y, -o.z, 0.08);
                }
            }
            if (t == 0) {
                this.playSound(SoundEvents.BEACON_ACTIVATE, 1.5F, 0.7F);
            }
            return;
        }
        if (t >= 55) {
            return;
        }
        Vec3 end = heart.add(this.aim.scale(20.0));
        HitResult hit = level.clip(new ClipContext(heart, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.MISS) {
            end = hit.getLocation();
        }
        double len = end.distanceTo(heart);
        if (t % 2 == 0) {
            for (double d = 0.8; d < len; d += 0.6) {
                Vec3 p = heart.add(this.aim.scale(d));
                level.sendParticles(AMBER, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
            level.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 3, 0.1, 0.1, 0.1, 0.05);
        }
        if (t == 30) {
            this.playSound(SoundEvents.BEACON_POWER_SELECT, 1.5F, 0.6F);
        }
        if ((t - 30) % 5 == 0) {
            float damage = AVDataMaps.move(this, "ray_damage", this.phaseTwo(), 4.0, 5.0);
            for (LivingEntity e : this.victims(level, new AABB(heart, end).inflate(1.0))) {
                if (e.getBoundingBox().inflate(0.4).clip(heart, end).isPresent()) {
                    e.hurtServer(level, this.damageSources().indirectMagic(this, this), damage);
                }
            }
        }
    }

    private void tickMend(ServerLevel level, int t) {
        Vec3 heart = this.heart();
        if (t % 5 == 0) {
            for (int i = 0; i < 4; i++) {
                Vec3 o = new Vec3(this.random.nextDouble() - 0.5, this.random.nextDouble() * 0.6, this.random.nextDouble() - 0.5).normalize().scale(5.0);
                level.sendParticles(LEAF, heart.x + o.x, heart.y + o.y, heart.z + o.z, 0, -o.x, -o.y, -o.z, 0.12);
            }
        }
        if (t % 10 == 9 && this.leavesNearby(level) >= 6) {
            this.heal(AVDataMaps.move(this, "mend_heal", this.phaseTwo(), 3.0, 4.0));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, heart.x, heart.y, heart.z, 6, 0.5, 0.5, 0.5, 0.0);
            this.playSound(SoundEvents.AZALEA_LEAVES_BREAK, 1.0F, 0.6F);
        }
    }

    private void tickBurrow(ServerLevel level, int t, LivingEntity target) {
        BlockState dirt = Blocks.ROOTED_DIRT.defaultBlockState();
        if (t < 15 && t % 3 == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, dirt), this.getX(), this.getY() + 0.2, this.getZ(), 16, 1.0, 0.1, 1.0, 0.15);
            this.playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 0.5F);
        }
        if (t >= 15 && t < 35 && t % 2 == 0) {                 // Thorn's destination tell: leaves and roots stirring
            level.sendParticles(LEAF, this.burrowTo.x, this.burrowTo.y + 0.3, this.burrowTo.z, 6, 1.2, 0.1, 1.2, 0.0);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, dirt), this.burrowTo.x, this.burrowTo.y + 0.1, this.burrowTo.z, 4, 1.0, 0.0, 1.0, 0.05);
        }
        if (t == 35) {
            this.teleportTo(this.burrowTo.x, this.burrowTo.y, this.burrowTo.z);
            this.playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.6F, 0.5F);
        }
        if (t == 38) {
            this.impact(level, 0.0, AVDataMaps.move(this, "burrow_radius", BURST_RADIUS), (float) AVDataMaps.move(this, "burrow_damage", 8.0), 0.7);
        }
    }

    private void tickRoar(ServerLevel level, int t) {
        if (t == 10) {
            this.playSound(SoundEvents.RAVAGER_ROAR, 2.0F, 0.5F);
            level.sendParticles(new DustParticleOptions(0x78E1CD, 1.6F), this.getX(), this.getY() + 3.0, this.getZ(), 60, 1.5, 1.0, 1.5, 0.2);
            for (LivingEntity e : this.victims(level, this.getBoundingBox().inflate(6.0))) {
                Vec3 push = new Vec3(e.getX() - this.getX(), 0, e.getZ() - this.getZ());
                if (push.lengthSqr() > 1e-4) {
                    push = push.normalize().scale(1.4);
                    e.push(push.x, 0.4, push.z);
                    e.hurtMarked = true;
                }
            }
        }
        if (t + 1 >= ROAR.length()) {
            this.entityData.set(DATA_PHASE_TWO, true);
        }
    }

    /** Wipe timeout: nobody left to fight, so it sinks back and gives the Heartseed back. */
    private void retreat(ServerLevel level) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()),
            this.getX(), this.getY() + 0.5, this.getZ(), 60, 1.0, 0.5, 1.0, 0.2);
        level.addFreshEntity(new ItemEntity(level, this.getX(), this.getY() + 0.5, this.getZ(), new ItemStack(com.arcanavaria.AVRegistry.HEARTSEED.get())));
        this.discard();
    }

    // ------------------------------------------------------------------------------------------------ helpers
    /** The amber heart in the chest, where the ray starts. */
    public Vec3 heart() {
        Vec3 fwd = Vec3.directionFromRotation(0, this.yBodyRot);
        return this.position().add(0, 2.25, 0).add(fwd.scale(0.7));
    }

    private List<LivingEntity> victims(ServerLevel level, AABB box) {
        return level.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive() && !this.isAlliedTo(e)
            && !(e instanceof GreatwoodWarden) && !(e instanceof Player p && (p.isSpectator() || p.isCreative())));
    }

    private void ring(ServerLevel level, double radius, ParticleOptions particle, int n) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2.0 / n;
            double x = this.getX() + Math.cos(a) * radius, z = this.getZ() + Math.sin(a) * radius;
            Vec3 g = this.ground(level, x, this.getY(), z);
            if (g != null) {
                level.sendParticles(particle, g.x, g.y + 0.15, g.z, 1, 0.05, 0.0, 0.05, 0.0);
            }
        }
    }

    /** The top of solid ground near (x, y, z): searches 3 blocks up and 5 down; null over a drop. */
    private Vec3 ground(Level level, double x, double y, double z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(y) + 3, Mth.floor(z));
        for (int i = 0; i < 9; i++) {
            BlockState below = level.getBlockState(p.below());
            if (!below.getCollisionShape(level, p.below()).isEmpty() && level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                return new Vec3(x, p.getY(), z);
            }
            p.move(0, -1, 0);
        }
        return null;
    }

    private int leavesNearby(ServerLevel level) {
        int n = 0;
        BlockPos c = this.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-6, -2, -6), c.offset(6, 8, 6))) {
            if (level.getBlockState(p).is(BlockTags.LEAVES) && ++n >= 12) {
                break;
            }
        }
        return n;
    }

    private static Vec3 turn(Vec3 from, Vec3 to, double maxAngle) {
        double dot = Mth.clamp(from.dot(to), -1.0, 1.0);
        double angle = Math.acos(dot);
        if (angle <= maxAngle || angle < 1e-6) {
            return to;
        }
        double k = maxAngle / angle;
        return from.scale(1.0 - k).add(to.scale(k)).normalize();
    }

    // ------------------------------------------------------------------------------------------------ death and saving
    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.level() instanceof ServerLevel level && this.deathTime % 4 == 0) {
            level.sendParticles(LEAF, this.getX(), this.getY() + 2.0, this.getZ(), 12, 1.0, 1.2, 1.0, 0.05);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_LOG.defaultBlockState()),
                this.getX(), this.getY() + 1.0, this.getZ(), 8, 0.8, 0.8, 0.8, 0.1);
        }
        if (this.deathTime >= 40 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("PhaseTwo", this.phaseTwo());
        output.putString("Finale", this.finale.serialName());
        if (this.lastAttack != null) {
            output.putString("LastAttack", this.lastAttack.serialName());
        }
        output.putInt("MendCooldown", this.mendCooldown);
        output.putInt("WipeTicks", this.wipeTicks);
        output.putInt("HomeX", this.home.getX());
        output.putInt("HomeY", this.home.getY());
        output.putInt("HomeZ", this.home.getZ());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_PHASE_TWO, input.getBooleanOr("PhaseTwo", false));
        this.finale = WardenState.Finale.byName(input.getStringOr("Finale", WardenState.Finale.PENDING.serialName()));
        this.lastAttack = WardenState.byName(input.getStringOr("LastAttack", ""));
        this.mendCooldown = input.getIntOr("MendCooldown", 0);
        this.wipeTicks = input.getIntOr("WipeTicks", 0);
        this.home = new BlockPos(input.getIntOr("HomeX", 0), input.getIntOr("HomeY", 0), input.getIntOr("HomeZ", 0));
        this.setState(IDLE);                 // a save mid-attack resumes in IDLE
    }

    /** Saved fight fields in one line, for the save/load round-trip self-test. */
    public String snapshot() {
        return "state=" + this.state().serialName() + " phaseTwo=" + this.phaseTwo() + " finale=" + this.finale.serialName()
            + " lastAttack=" + (this.lastAttack == null ? null : this.lastAttack.serialName()) + " mendCooldown=" + this.mendCooldown
            + " wipeTicks=" + this.wipeTicks + " home=" + this.home.toShortString();
    }

    /** Self-test setup: a phase-two Warden with the finale earned but not yet played, mid-RAY. */
    public void selfTestMidFight(BlockPos at) {
        this.home = at;
        this.entityData.set(DATA_PHASE_TWO, true);
        this.finale = WardenState.Finale.QUEUED;
        this.lastAttack = RAY;
        this.mendCooldown = 123;
        this.wipeTicks = 45;
        this.setState(RAY);
    }

    /** Self-test: runs the finale ROOTS to its end through the real transition; the finale must come out DONE. */
    public void selfTestFinishFinale() {
        this.setState(ROOTS);
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
