package com.arcanavaria.thurible;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * A puff of essentia a swung Thurible leaves behind: drifts in its aspect's colour for {@link #LIFE} ticks and, once a
 * second, gives whatever stands in it the aspect's cloud effects (ThuribleEffects). Fades as it thins; never saved.
 */
public class ThuribleCloud extends Entity {
    public static final int LIFE = 100;
    private static final EntityDataAccessor<Integer> COLOUR = SynchedEntityData.defineId(ThuribleCloud.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(ThuribleCloud.class, EntityDataSerializers.FLOAT);
    private @Nullable ResourceKey<IAspect> aspect;
    private @Nullable EntityReference<LivingEntity> owner;

    public ThuribleCloud(EntityType<? extends ThuribleCloud> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static ThuribleCloud puff(ServerLevel level, double x, double y, double z, ResourceKey<IAspect> aspect, float radius,
                                     @Nullable LivingEntity owner) {
        ThuribleCloud c = new ThuribleCloud(ThuribleRegistry.CLOUD.get(), level);
        c.setPos(x, y, z);
        c.aspect = aspect;
        c.owner = owner == null ? null : EntityReference.of(owner);
        c.entityData.set(COLOUR, ThuribleEffects.colour(level.registryAccess(), aspect));
        c.entityData.set(RADIUS, radius);
        level.addFreshEntity(c);
        return c;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOUR, 0xFFFFFF);
        builder.define(RADIUS, 2.0F);
    }

    public int colour() {
        return entityData.get(COLOUR);
    }

    public float radius() {
        return entityData.get(RADIUS);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level) {
            if (tickCount == 10 && aspect != null) {
                com.arcanavaria.exanimator.ExanimatorRite.check(level, this);
            }
            if (tickCount % 20 == 5 && aspect != null) {
                pulse(level);
            }
            if (tickCount >= LIFE) {
                discard();
            }
        } else {
            particles();
        }
    }

    /** A soft ball of motes in the aspect's colour, thinning out over the cloud's life; a few bright sparks at the heart. */
    private void particles() {
        float life = 1.0F - (float) tickCount / LIFE;
        float r = radius() * (0.6F + 0.4F * Math.min(1.0F, tickCount / 8.0F));
        int rgb = colour();
        int n = Math.max(1, (int) (6 * life * r));
        for (int i = 0; i < n; i++) {
            double a = random.nextDouble() * Math.PI * 2, u = random.nextDouble() * 2 - 1, s = Math.cbrt(random.nextDouble()) * r;
            double h = Math.sqrt(1 - u * u);
            double x = getX() + Math.cos(a) * h * s, y = getY() + 0.4 + u * s * 0.55, z = getZ() + Math.sin(a) * h * s;
            level().addParticle(new DustParticleOptions(shade(rgb, 0.85F + random.nextFloat() * 0.3F), 1.6F + random.nextFloat()),
                    x, y, z, 0, 0.01, 0);
        }
        if (random.nextFloat() < life) {
            level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | rgb),
                    getX() + (random.nextDouble() - 0.5) * r, getY() + 0.3 + random.nextDouble() * r * 0.6,
                    getZ() + (random.nextDouble() - 0.5) * r, 0, 0.02, 0);
        }
    }

    private static int shade(int rgb, float k) {
        int r = Mth.clamp((int) (((rgb >> 16) & 255) * k), 0, 255), g = Mth.clamp((int) (((rgb >> 8) & 255) * k), 0, 255);
        int b = Mth.clamp((int) ((rgb & 255) * k), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private void pulse(ServerLevel level) {
        List<ThuribleEffects.Resolved> effects = ThuribleEffects.resolve(level.registryAccess(), aspect);
        if (effects.isEmpty()) {
            return;
        }
        LivingEntity src = EntityReference.getLivingEntity(owner, level);
        float r = radius();
        AABB box = new AABB(getX() - r, getY() - 0.5, getZ() - r, getX() + r, getY() + r, getZ() + r);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (e.distanceToSqr(getX(), Mth.clamp(e.getY(), getY() - 0.5, getY() + r), getZ()) > r * r) {
                continue;
            }
            boolean ally = src == null ? e instanceof Player : isAlly(src, e);
            for (ThuribleEffects.Resolved res : effects) {
                apply(level, res, e, ally, src);
            }
        }
    }

    private static boolean isAlly(LivingEntity src, LivingEntity e) {
        if (e == src || src.isAlliedTo(e)) {
            return true;
        }
        if (e instanceof OwnableEntity pet && pet.getOwner() == src) {
            return true;
        }
        return src instanceof Player && e instanceof Player;     // a censer is no weapon against other players
    }

    private void apply(ServerLevel level, ThuribleEffects.Resolved res, LivingEntity e, boolean ally, @Nullable LivingEntity src) {
        CloudEffect fx = res.effect();
        boolean hits = switch (fx.target()) {
            case ENEMIES -> !ally;
            case ALLIES -> ally;
            case EVERYONE -> true;
        };
        if (!hits) {
            return;
        }
        float k = res.scale();
        for (CloudEffect.Applied a : fx.effects()) {
            e.addEffect(new MobEffectInstance(a.effect(), Math.round(a.duration() * k), a.amplifier() + res.boost() / 2), src);
        }
        if (fx.burn() > 0) {
            e.igniteForSeconds(fx.burn() * k);
        }
        if (fx.extinguish()) {
            e.clearFire();
        }
        if (fx.heal() > 0) {
            e.heal(fx.heal() * k);
        }
        if (fx.damage() > 0) {
            DamageSource ds = src == null ? damageSources().magic() : damageSources().indirectMagic(this, src);
            e.hurtServer(level, ds, fx.damage() * k);
        }
    }

    public Optional<ResourceKey<IAspect>> aspect() {
        return Optional.ofNullable(aspect);
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
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
