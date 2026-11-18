package com.arcanavaria.warden;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A greatwood root that bursts out of the ground along one of the Warden's lanes: rises for {@link #RISE} ticks,
 * hurts and snares whatever stands in it once, holds, then sinks and removes itself. Owned by the encounter, never
 * touches blocks.
 */
public class RootSpike extends Entity {
    public static final int RISE = 4;
    public static final int LIFE = 30;
    private int age;
    private float damage = 6.0F;
    private @Nullable EntityReference<LivingEntity> owner;

    public RootSpike(EntityType<? extends RootSpike> type, Level level) {
        super(type, level);
    }

    public RootSpike(Level level, double x, double y, double z, float yaw, float damage, LivingEntity owner) {
        this(WardenRegistry.ROOT_SPIKE.get(), level);
        this.setPos(x, y, z);
        this.setYRot(yaw);
        this.damage = damage;
        this.owner = EntityReference.of(owner);
    }

    /** 0..1 how far the root stands out of the ground (rises fast, sinks over the last 10 ticks). */
    public float height(float partialTick) {
        float t = this.tickCount + partialTick;
        if (t < RISE) {
            return t / RISE;
        }
        return Math.min(1.0F, Math.max(0.0F, (LIFE - t) / 10.0F));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;
        if (this.level() instanceof ServerLevel level) {
            if (this.age == 1) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()),
                    this.getX(), this.getY() + 0.2, this.getZ(), 10, 0.3, 0.1, 0.3, 0.15);
                this.playSound(SoundEvents.ROOTED_DIRT_BREAK, 0.8F, 0.7F + this.random.nextFloat() * 0.3F);
            }
            if (this.age == RISE) {
                LivingEntity src = EntityReference.getLivingEntity(this.owner, level);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.15, 0.0, 0.15))) {
                    if (e == src || !e.isAlive() || (src != null && src.isAlliedTo(e))) {
                        continue;
                    }
                    DamageSource ds = src == null ? this.damageSources().magic() : this.damageSources().indirectMagic(this, src);
                    if (e.hurtServer(level, ds, this.damage)) {
                        e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2), src);
                    }
                }
            }
            if (this.age >= LIFE) {
                this.discard();
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.discard();          // encounter-owned: a reloaded root is stale
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
