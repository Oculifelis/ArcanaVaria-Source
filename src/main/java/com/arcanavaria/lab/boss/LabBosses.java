package com.arcanavaria.lab.boss;

import static com.arcanavaria.ArcanaVaria.LOG;
import static com.arcanavaria.ArcanaVaria.id;

import com.arcanavaria.lab.LabRegistry;
import com.arcanavaria.lab.walker.CrucibleWalker;
import com.arcanavaria.thurible.CloudEffect;
import com.leclowndu93150.thaumaturge.api.entity.trait.MobTraits;
import com.leclowndu93150.thaumaturge.registry.TTMobTraits;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * Lab mini-bosses: raising one from an altar's pool (a vanilla body, scaled and armed by its archetype, with rolled
 * traits), and running it: its boss bar (named after its body and every trait, each in the trait's colour), each
 * trait's motes, aura, on-hit and ability, and telling its altar when it dies.
 */
public final class LabBosses {
    private record Live(ServerBossEvent bar, LabArchetype body, List<LabTrait> traits) {
    }

    private static final Map<UUID, Live> LIVE = new ConcurrentHashMap<>();
    private static final double BAR_RANGE = 48;

    private LabBosses() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LabBosses::tick);
        NeoForge.EVENT_BUS.addListener(LabBosses::hit);
        NeoForge.EVENT_BUS.addListener(LabBosses::died);
        NeoForge.EVENT_BUS.addListener(LabBosses::left);
    }

    // ---- raising ----------------------------------------------------------------------------------------------------

    public static @Nullable Mob raise(ServerLevel level, BlockPos altar, ResourceKey<LabPool> poolKey) {
        var access = level.registryAccess();
        LabPool pool = access.lookupOrThrow(LabRegistry.POOL_KEY).get(poolKey).map(Holder::value).orElse(null);
        if (pool == null) {
            LOG.warn("Guardian altar at {} names a missing pool {}", altar, poolKey.identifier());
            return null;
        }
        RandomSource r = level.getRandom();
        ResourceKey<LabArchetype> bodyKey = pool.rollArchetype(level.dimension(), r).orElse(null);
        LabArchetype body = bodyKey == null ? null : access.lookupOrThrow(LabRegistry.ARCHETYPE_KEY).get(bodyKey).map(Holder::value).orElse(null);
        if (body == null || !(body.entity().create(level, EntitySpawnReason.SPAWNER) instanceof Mob mob)) {
            LOG.warn("Pool {} rolled no usable archetype ({})", poolKey.identifier(), bodyKey);
            return null;
        }
        List<ResourceKey<LabTrait>> traitKeys = pool.rollTraits(level.dimension(), r);
        BlockPos at = altar.above();
        mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, r.nextFloat() * 360, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.SPAWNER, null);
        base(mob, Attributes.MAX_HEALTH, body.health());
        base(mob, Attributes.ATTACK_DAMAGE, body.damage());
        base(mob, Attributes.ARMOR, body.armor());
        base(mob, Attributes.SCALE, body.scale());
        base(mob, Attributes.FOLLOW_RANGE, 40);
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * body.speed());
        }
        for (Map.Entry<EquipmentSlot, Item> e : body.equipment().entrySet()) {
            mob.setItemSlot(e.getKey(), new ItemStack(e.getValue()));
            mob.setDropChance(e.getKey(), 0F);
        }
        var traitLookup = access.lookupOrThrow(LabRegistry.TRAIT_KEY);
        for (ResourceKey<LabTrait> key : traitKeys) {
            traitLookup.get(key).map(Holder::value).ifPresent(t -> apply(mob, key, t));
        }
        if (mob instanceof CrucibleWalker walker && !traitKeys.isEmpty()) {
            traitLookup.get(traitKeys.get(0)).map(Holder::value).ifPresent(t -> walker.tintWith(t.colour()));
        }
        mob.setHealth(mob.getMaxHealth());
        mob.setPersistenceRequired();
        mob.setData(LabRegistry.BOSS.get(), new BossData(altar, bodyKey, traitKeys));
        level.addFreshEntity(mob);
        Vec3 c = mob.position();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.5F, 0.6F);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 1.0F, 0.5F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 1, c.z, 80, 0.6, 1.2, 0.6, 0.05);
        return mob;
    }

    private static void base(Mob mob, Holder<Attribute> attribute, double value) {
        AttributeInstance a = mob.getAttribute(attribute);
        if (a != null) {
            a.setBaseValue(value);
        }
    }

    private static void apply(Mob mob, ResourceKey<LabTrait> key, LabTrait t) {
        int n = 0;
        for (LabTrait.Modifier m : t.attributes()) {
            AttributeInstance a = mob.getAttribute(m.attribute());
            if (a != null) {
                a.addPermanentModifier(new AttributeModifier(id("lab_trait/%s/%d".formatted(key.identifier().getPath(), n++)), m.amount(), m.operation()));
            }
        }
        for (CloudEffect.Applied e : t.self()) {
            mob.addEffect(new MobEffectInstance(e.effect(), MobEffectInstance.INFINITE_DURATION, e.amplifier(), false, false));
        }
        t.champion().flatMap(TTMobTraits.registry()::get).ifPresent(h -> MobTraits.add(mob, h));
    }

    // ---- running ----------------------------------------------------------------------------------------------------

    private static @Nullable Live live(LivingEntity boss, BossData data) {
        Live live = LIVE.get(boss.getUUID());
        if (live != null) {
            return live;
        }
        var access = boss.level().registryAccess();
        LabArchetype body = access.lookupOrThrow(LabRegistry.ARCHETYPE_KEY).get(data.archetype()).map(Holder::value).orElse(null);
        if (body == null) {
            return null;
        }
        List<LabTrait> traits = new ArrayList<>();
        MutableComponent name = Component.translatable(body.name());
        var lookup = access.lookupOrThrow(LabRegistry.TRAIT_KEY);
        for (int i = 0; i < data.traits().size(); i++) {
            LabTrait t = lookup.get(data.traits().get(i)).map(Holder::value).orElse(null);
            if (t != null) {
                traits.add(t);
                name.append(Component.literal(traits.size() == 1 ? " — " : ", "));
                name.append(Component.translatable(t.name()).withStyle(Style.EMPTY.withColor(t.colour())));
            }
        }
        live = new Live(new ServerBossEvent(boss.getUUID(), name, body.bar(), BossEvent.BossBarOverlay.NOTCHED_10), body, traits);
        LIVE.put(boss.getUUID(), live);
        return live;
    }

    private static void tick(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof Mob boss) || !(boss.level() instanceof ServerLevel level) || !boss.isAlive()) {
            return;
        }
        BossData data = boss.getExistingDataOrNull(LabRegistry.BOSS.get());
        if (data == null) {
            return;
        }
        Live live = live(boss, data);
        if (live == null) {
            return;
        }
        live.bar.setProgress(boss.getHealth() / boss.getMaxHealth());
        if (boss.tickCount % 20 == 0) {
            for (ServerPlayer p : List.copyOf(live.bar.getPlayers())) {
                if (p.level() != level || !p.isAlive() || p.distanceToSqr(boss) > BAR_RANGE * BAR_RANGE) {
                    live.bar.removePlayer(p);
                }
            }
            for (ServerPlayer p : level.getPlayers(p -> p.distanceToSqr(boss) <= BAR_RANGE * BAR_RANGE)) {
                live.bar.addPlayer(p);
            }
        }
        AABB box = boss.getBoundingBox();
        for (LabTrait t : live.traits) {
            if (boss.tickCount % 4 == 0) {
                Vec3 c = box.getCenter();
                level.sendParticles(new DustParticleOptions(t.colour(), 1.3F), c.x, c.y, c.z, 3,
                        box.getXsize() * 0.4, box.getYsize() * 0.4, box.getZsize() * 0.4, 0);
                t.particle().ifPresent(p -> level.sendParticles(p, c.x, c.y, c.z, 1, box.getXsize() * 0.35,
                        box.getYsize() * 0.35, box.getZsize() * 0.35, 0.01));
            }
            if (boss.tickCount % 20 == 0) {
                t.aura().ifPresent(a -> level.getPlayers(p -> p.isAlive() && !p.isCreative() && p.distanceTo(boss) <= a.radius())
                        .forEach(p -> p.addEffect(new MobEffectInstance(a.effect().effect(), a.effect().duration(), a.effect().amplifier()))));
            }
            t.ability().ifPresent(a -> {
                if (boss.tickCount % a.every() == 0 && boss.getTarget() != null) {
                    if (boss instanceof CrucibleWalker walker) {
                        // the walker freezes in the trait's colour first, so the move can be seen coming
                        walker.queueTraitMove(t.colour(), () -> {
                            LivingEntity target = boss.getTarget();
                            if (target != null && boss.isAlive()) {
                                use(level, boss, data, t, a, target);
                            }
                        });
                    } else {
                        use(level, boss, data, t, a, boss.getTarget());
                    }
                }
            });
        }
    }

    private static void use(ServerLevel level, Mob boss, BossData data, LabTrait t, LabTrait.Ability a, LivingEntity target) {
        RandomSource r = level.getRandom();
        Vec3 c = boss.position();
        DustParticleOptions dust = new DustParticleOptions(t.colour(), 1.6F);
        switch (a.kind()) {
            case PULSE -> {
                for (LivingEntity v : level.getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(a.radius()),
                        v -> v != boss && v.distanceTo(boss) <= a.radius() && !(v instanceof Mob m && m.getExistingDataOrNull(LabRegistry.BOSS.get()) != null))) {
                    v.hurtServer(level, level.damageSources().indirectMagic(boss, boss), a.power());
                    v.knockback(1.2, c.x - v.getX(), c.z - v.getZ());
                }
                for (int k = 0; k < 48; k++) {
                    double ang = k * Math.PI * 2 / 48;
                    level.sendParticles(dust, c.x + Math.cos(ang) * a.radius(), c.y + 0.3, c.z + Math.sin(ang) * a.radius(), 1, 0, 0.1, 0, 0);
                }
                level.playSound(null, c.x, c.y, c.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.4F, 0.6F);
            }
            case SUMMON -> a.entity().ifPresent(type -> {
                if (level.getEntities(type, boss.getBoundingBox().inflate(16), x -> true).size() >= a.count() * 2) {
                    return;
                }
                for (int k = 0; k < a.count(); k++) {
                    BlockPos at = BlockPos.containing(c.add(r.nextGaussian() * 2, 0, r.nextGaussian() * 2));
                    Entity minion = ((EntityType<?>) type).spawn(level, at, EntitySpawnReason.MOB_SUMMONED);
                    if (minion instanceof Mob m) {
                        m.setTarget(target);
                        level.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + 0.5, m.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
                    }
                }
                level.playSound(null, c.x, c.y, c.z, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.0F, 1.0F);
            });
            case BLINK -> {
                Vec3 home = data.altar().above().getBottomCenter();
                for (int k = 0; k < 10; k++) {
                    Vec3 to = target.position().add(r.nextGaussian() * 2.5, 0, r.nextGaussian() * 2.5);
                    if (to.distanceTo(home) > Math.max(a.radius(), 8) || !level.noCollision(boss, boss.getBoundingBox().move(to.subtract(c)))) {
                        continue;
                    }
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1, c.z, 30, 0.4, 0.8, 0.4, 0.05);
                    boss.teleportTo(to.x, to.y, to.z);
                    level.sendParticles(ParticleTypes.PORTAL, to.x, to.y + 1, to.z, 30, 0.4, 0.8, 0.4, 0.3);
                    level.playSound(null, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.7F);
                    break;
                }
            }
            case HEAL -> {
                if (boss.getHealth() < boss.getMaxHealth() / 2) {
                    boss.heal(a.power());
                    level.sendParticles(dust, c.x, c.y + boss.getBbHeight() / 2, c.z, 30, 0.5, 0.8, 0.5, 0);
                    level.playSound(null, c.x, c.y, c.z, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 0.8F);
                }
            }
            case VOLLEY -> {
                Vec3 eye = boss.getEyePosition();
                for (int k = 0; k < a.count(); k++) {
                    Vec3 dir = target.getEyePosition().subtract(eye).normalize().add(r.nextGaussian() * 0.08, r.nextGaussian() * 0.05,
                            r.nextGaussian() * 0.08);
                    SmallFireball ball = new SmallFireball(level, boss, dir);
                    ball.setPos(eye.x + dir.x, eye.y, eye.z + dir.z);
                    level.addFreshEntity(ball);
                }
                level.playSound(null, c.x, c.y, c.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 0.8F);
            }
        }
    }

    private static void hit(LivingDamageEvent.Post e) {
        if (!(e.getSource().getEntity() instanceof Mob boss) || !(boss.level() instanceof ServerLevel)) {
            return;
        }
        BossData data = boss.getExistingDataOrNull(LabRegistry.BOSS.get());
        Live live = data == null ? null : live(boss, data);
        if (live == null) {
            return;
        }
        LivingEntity victim = e.getEntity();
        for (LabTrait t : live.traits) {
            LabTrait.OnHit h = t.onHit();
            h.effects().forEach(a -> victim.addEffect(new MobEffectInstance(a.effect(), a.duration(), a.amplifier())));
            if (h.burnSeconds() > 0) {
                victim.igniteForSeconds(h.burnSeconds());
            }
            if (h.lifesteal() > 0) {
                boss.heal(e.getHealthDamage() * h.lifesteal());
            }
            if (h.knockback() > 0) {
                victim.knockback(h.knockback(), boss.getX() - victim.getX(), boss.getZ() - victim.getZ());
            }
        }
    }

    private static void died(LivingDeathEvent e) {
        LivingEntity boss = e.getEntity();
        BossData data = boss.getExistingDataOrNull(LabRegistry.BOSS.get());
        if (data == null || !(boss.level() instanceof ServerLevel level)) {
            return;
        }
        drop(boss.getUUID());
        if (level.getBlockEntity(data.altar()) instanceof GuardianAltarBlockEntity altar) {
            altar.defeated(level);
        }
    }

    private static void left(EntityLeaveLevelEvent e) {
        if (!e.getLevel().isClientSide()) {
            drop(e.getEntity().getUUID());
        }
    }

    private static void drop(UUID id) {
        Live live = LIVE.remove(id);
        if (live != null) {
            live.bar.removeAllPlayers();
        }
    }
}
