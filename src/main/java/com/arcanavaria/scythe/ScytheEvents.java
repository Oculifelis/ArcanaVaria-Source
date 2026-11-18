package com.arcanavaria.scythe;

import com.arcanavaria.AVDataMaps;
import com.arcanavaria.AVRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;
import org.jspecify.annotations.Nullable;

/** Scythe kills (Soul Devourer, Grim Harvest, Eclipse's finish), Grim Harvest's damage cut and the sweep's look and sound. */
public final class ScytheEvents {
    // built-in values; data/<ns>/data_maps/enchantment/enchantment_values.json can change each
    private static final double[] HEAL_CHANCE = {0, 0.4, 0.6, 0.8};
    private static final double HEALTH_PER_LEVEL = 2;
    private static final double[] GRIM_CHANCE = {0, 0.3, 0.5, 0.7};
    private static final double[] GRIM_TICKS = {0, 120, 180, 240};
    private static final double GRIM_REDUCTION = 0.2;

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ScytheEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(ScytheEvents::onDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ScytheEvents::onSweep);
    }

    /** The level of an enchantment (by key, since enchantments are data) on a stack. */
    public static int level(Level level, @Nullable ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack)).orElse(0);
    }

    private static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        if (!(victim.level() instanceof ServerLevel level) || !(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack weapon = source.getWeaponItem();
        if (!ScytheItem.isScythe(weapon)) {
            return;
        }
        // Soul Devourer: a slain monster may heal 1 / 2 / 3 hearts (40 / 60 / 80%).
        int devour = Math.min(level(level, weapon, AVRegistry.SOUL_DEVOURER), 3);
        if (devour > 0 && victim instanceof Enemy && player.getRandom().nextFloat() < AVDataMaps.enchant(level, AVRegistry.SOUL_DEVOURER, "chance", devour, HEAL_CHANCE)) {
            player.heal((float) (devour * AVDataMaps.enchant(level, AVRegistry.SOUL_DEVOURER, "heal_per_level", HEALTH_PER_LEVEL)));
            soulDevour(level, victim, player);
        }
        // Grim Harvest: a kill may hasten and harden the reaper for 6 / 9 / 12 s (30 / 50 / 70%).
        int grim = Math.min(level(level, weapon, AVRegistry.GRIM_HARVEST), 3);
        if (grim > 0 && victim.getRandom().nextFloat() < AVDataMaps.enchant(level, AVRegistry.GRIM_HARVEST, "chance", grim, GRIM_CHANCE)) {
            player.addEffect(new MobEffectInstance(AVRegistry.GRIM_HARVEST_EFFECT,
                    (int) AVDataMaps.enchant(level, AVRegistry.GRIM_HARVEST, "ticks", grim, GRIM_TICKS), 0, false, false, true));
            grimHarvest(level, player);
        }
        if (victim.isInvertedHealAndHarm() && ScytheItem.isEclipseCondition(victim) && level(level, weapon, AVRegistry.ECLIPSE) > 0) {
            eclipseKill(level, victim);
        }
    }

    private static void onDamage(LivingDamageEvent.Pre event) {
        MobEffectInstance grim = event.getEntity().getEffect(AVRegistry.GRIM_HARVEST_EFFECT);
        if (grim != null && !event.getSource().is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            double cut = AVDataMaps.enchant(event.getEntity().level(), AVRegistry.GRIM_HARVEST, "damage_reduction_per_level", GRIM_REDUCTION);
            event.setNewDamage(event.getNewDamage() * (float) Math.max(0, 1 - cut * (grim.getAmplifier() + 1)));
        }
    }

    /** A scythe's sweep throws souls along a wide arc and plays its own swish. */
    private static void onSweep(SweepAttackEvent event) {
        Player player = event.getEntity();
        if (!event.isSweeping() || !ScytheItem.isScythe(player.getMainHandItem()) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        double x = player.getX(), y = player.getY() + player.getBbHeight() * 0.5, z = player.getZ();
        double yaw = Math.toRadians(player.getYRot());
        for (int i = -2; i <= 2; i++) {
            double angle = yaw + i * 0.45;
            double px = x - Math.sin(angle) * 1.7, pz = z + Math.cos(angle) * 1.7;
            if (Math.abs(i) == 2) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, px, y, pz, 1, 0, 0, 0, 0);
            }
            level.sendParticles(ParticleTypes.SOUL, px, y + 0.1, pz, 2, 0.1, 0.1, 0.1, 0.01);
        }
        level.playSound(null, x, y, z, AVRegistry.SCYTHE_SWEEP.get(), SoundSource.PLAYERS, 0.8f, 0.9f + player.getRandom().nextFloat() * 0.2f);
    }

    private static void soulDevour(ServerLevel level, LivingEntity victim, Player player) {
        double fx = victim.getX(), fy = victim.getY() + victim.getBbHeight() * 0.5, fz = victim.getZ();
        double tx = player.getX(), ty = player.getY() + player.getBbHeight() * 0.6, tz = player.getZ();
        for (int i = 0; i <= 10; i++) {
            double t = i / 10.0;
            level.sendParticles(ParticleTypes.SOUL, fx + (tx - fx) * t, fy + (ty - fy) * t, fz + (tz - fz) * t, 1, 0.05, 0.05, 0.05, 0);
        }
        level.sendParticles(ParticleTypes.SCULK_SOUL, fx, fy, fz, 8, 0.3, 0.4, 0.3, 0.05);
        level.sendParticles(ParticleTypes.HEART, tx, ty + 0.5, tz, 3, 0.4, 0.3, 0.4, 0);
        level.playSound(null, tx, ty, tz, AVRegistry.SOUL_DEVOURER_HEAL.get(), SoundSource.PLAYERS, 0.5f, 0.9f + level.getRandom().nextFloat() * 0.15f);
    }

    private static void grimHarvest(ServerLevel level, Player player) {
        double x = player.getX(), y = player.getY() + 0.1, z = player.getZ();
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2 * i / 16;
            level.sendParticles(ParticleTypes.SCULK_SOUL, x + Math.cos(angle) * 0.9, y, z + Math.sin(angle) * 0.9, 0, 0, 1, 0, 0.08);
        }
        level.sendParticles(ParticleTypes.SMOKE, x, y + 0.2, z, 12, 0.5, 0.1, 0.5, 0.01);
    }

    private static void eclipseKill(ServerLevel level, LivingEntity victim) {
        double x = victim.getX(), y = victim.getY() + victim.getBbHeight() * 0.5, z = victim.getZ();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 25, 0.25, 0.25, 0.25, 0.01);
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24, dx = Math.cos(angle), dz = Math.sin(angle);
            level.sendParticles(ParticleTypes.END_ROD, x + dx * 0.6, y, z + dz * 0.6, 0, dx, 0, dz, 0.15);
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 10, 0.3, 0.4, 0.3, 0.02);
        level.playSound(null, x, y, z, AVRegistry.ECLIPSE_KILL.get(), SoundSource.PLAYERS, 1f, 1f);
    }

    private ScytheEvents() {
    }
}
