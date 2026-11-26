package com.arcanavaria.whip;

import com.arcanavaria.ArcanaVaria;
import com.arcanavaria.fx.SwingFxPacket;
import com.fundamentum.whip.WhipCordFx;
import com.fundamentum.whip.WhipDefinition;
import com.fundamentum.whip.WhipHit;
import com.fundamentum.whip.WhipHooks;
import com.fundamentum.whip.Whips;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

/**
 * Arcana Varia's cord whips on Fundamentum's whip system, a family beside the scythes: Thaumium (crafted), Void (crafted,
 * warp 1: hits weaken, a cracking tip drinks a little life back) and Elemental (infused from the thaumium one: every hit
 * carries one of the six primals), plus the Amberbark Lash, a summoner's whip whose marks make summons hit hard. Only
 * loaded when Fundamentum is installed (AVRegistry.FUNDAMENTUM). Numbers are set against Fundamentum's leather whip
 * (4.5 blocks, 6 at the tip) and Ostentum's (Vine 5, Ender 9). Like Fundamentum's own cord whips (1.22.1) they sit
 * coiled under the handle and on the hip between swings, turn a little toward a hostile in the look (homing 20 degrees),
 * and burst a ring in their colour when the tip cracks.
 */
public final class AVWhips {
    /** The six primals in Thaumaturge's colours: aer, ignis, aqua, terra, ordo, perditio. */
    static final int[] PRIMALS = {0xFFFF7E, 0xFF5A01, 0x3CD4FC, 0x56C000, 0xD5D4EC, 0x404040};

    public static void init() {
        Whips.register(ArcanaVaria.id("thaumium_whip"), thaumium());
        Whips.register(ArcanaVaria.id("void_whip"), voidWhip());
        Whips.register(ArcanaVaria.id("elemental_whip"), elemental());
        Whips.register(ArcanaVaria.id("amberbark_lash"), amberbark());
    }

    /** The cord texture of a whip, in {@code textures/entity/whip/}. */
    static Identifier lash(String whip) {
        return ArcanaVaria.id("textures/entity/whip/" + whip + ".png");
    }

    /** The base: a braided thaumium-wire cord on a greatwood grip. */
    static WhipDefinition thaumium() {
        return WhipDefinition.builder(ArcanaVaria.id("thaumium_whip"))
            .tuning(t -> t.length(5.0).damage(6.5))
            .coiled().homing(20).shockwave(0xC0B48CE6).texture(lash("thaumium_whip")).width(0.12F, 0.04F)
            .summonTag(new WhipDefinition.SummonTag(240, 2, 1.0, 0.0))
            .sounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.CHAIN_HIT)
            .crackParticle(ParticleTypes.WITCH)
            .build();
    }

    /** Void metal links: hits leave Weakness, a cracking tip heals the wielder 1 heart. */
    static WhipDefinition voidWhip() {
        return WhipDefinition.builder(ArcanaVaria.id("void_whip"))
            .tuning(t -> t.length(5.5).damage(8))
            .coiled().homing(20).shockwave(0xC03C1A5A).texture(lash("void_whip")).width(0.12F, 0.04F)
            .summonTag(new WhipDefinition.SummonTag(240, 3, 1.0, 0.05))
            .sounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.SCULK_CLICKING)
            .crackParticle(ParticleTypes.REVERSE_PORTAL)
            .particles((level, strand, segment, along, pos, speed, random) -> {
                if (random.nextFloat() < 0.05F * (float) along) {
                    level.addParticle(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 0.0, 0.02, 0.0);
                }
            })
            .hooks(new WhipHooks() {
                @Override
                public void landed(WhipHit hit, boolean hurt) {
                    LivingEntity target = hit.living();
                    if (!hurt || target == null) {
                        return;
                    }
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), hit.player);
                    if (hit.crit) {
                        hit.player.heal(2.0F);
                    }
                }
            })
            .build();
    }

    /** A cord banded in the six primals; each hit carries one of them, picked at random, and sheds its colour. */
    static WhipDefinition elemental() {
        return WhipDefinition.builder(ArcanaVaria.id("elemental_whip"))
            .tuning(t -> t.length(6.0).damage(8.5))
            .coiled().homing(20).shockwave(0xC0F0F0FF).cordFx(WhipCordFx.GLOW).texture(lash("elemental_whip")).width(0.12F, 0.04F)
            .summonTag(new WhipDefinition.SummonTag(240, 4, 1.1, 0.10))
            .sounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.AMETHYST_BLOCK_CHIME)
            .crackParticle(ParticleTypes.END_ROD)
            .particles((level, strand, segment, along, pos, speed, random) -> {
                if (random.nextFloat() < 0.08F) {
                    level.addParticle(new DustParticleOptions(PRIMALS[segment % PRIMALS.length], 0.8F), pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
                }
            })
            .hooks(new WhipHooks() {
                @Override
                public void landed(WhipHit hit, boolean hurt) {
                    LivingEntity target = hit.living();
                    if (hurt && target != null) {
                        int which = hit.player.getRandom().nextInt(6);
                        primal(hit, target, which);
                        jump(hit, target, which);
                    }
                }
            })
            .build();
    }

    /** Aer lifts, ignis burns, aqua drenches (Slowness II), terra shoves, ordo marks (Glowing), perditio withers. */
    static void primal(WhipHit hit, LivingEntity target, int which) {
        switch (which) {
            case 0 -> {
                target.push(0.0, 0.55, 0.0);
                target.hurtMarked = true;
            }
            case 1 -> target.igniteForSeconds(3.0F);
            case 2 -> effect(hit, target, MobEffects.SLOWNESS, 60, 1);
            case 3 -> {
                Vec3 away = target.position().subtract(hit.player.position()).multiply(1.0, 0.0, 1.0).normalize().scale(0.8);
                target.push(away.x, 0.1, away.z);
                target.hurtMarked = true;
            }
            case 4 -> effect(hit, target, MobEffects.GLOWING, 120, 0);
            default -> effect(hit, target, MobEffects.WITHER, 60, 0);
        }
        int c = PRIMALS[which];
        hit.player.level().sendParticles(new DustParticleOptions(c, 1.2F), target.getX(), target.getY() + target.getBbHeight() * 0.6,
            target.getZ(), 10, 0.3, 0.3, 0.3, 0.0);
    }

    /** The primal jumps on to the nearest other enemy within 4 blocks of the target, drawn as a vis arc (client/SwingFx). */
    static void jump(WhipHit hit, LivingEntity target, int which) {
        LivingEntity next = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4.0),
                e -> e != target && e != hit.player && e.isAlive() && e instanceof Enemy)) {
            double d = e.distanceToSqr(target);
            if (d < best) {
                best = d;
                next = e;
            }
        }
        if (next == null) {
            return;
        }
        primal(hit, next, which);
        SwingFxPacket.jump(hit.player, which, target.position().add(0, target.getBbHeight() * 0.6, 0),
            next.position().add(0, next.getBbHeight() * 0.6, 0));
    }

    private static void effect(WhipHit hit, LivingEntity target, Holder<MobEffect> effect, int ticks, int amplifier) {
        target.addEffect(new MobEffectInstance(effect, ticks, amplifier), hit.player);
    }

    /** The summoner's whip: weak itself, but its mark lasts 15 s and makes summons hit hard; a cracking tip roots. */
    static WhipDefinition amberbark() {
        return WhipDefinition.builder(ArcanaVaria.id("amberbark_lash"))
            .tuning(t -> t.length(5.0).damage(4))
            .coiled().homing(20).shockwave(0xC0FFA826).texture(lash("amberbark_lash")).width(0.13F, 0.05F)
            .summonTag(new WhipDefinition.SummonTag(300, 7, 1.25, 0.15))
            .sounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.ROOTED_DIRT_BREAK)
            .crackParticle(ParticleTypes.WAX_ON)
            .hooks(new WhipHooks() {
                @Override
                public void landed(WhipHit hit, boolean hurt) {
                    LivingEntity target = hit.living();
                    if (hurt && target != null && hit.crit) {
                        effect(hit, target, MobEffects.SLOWNESS, 30, 3);
                    }
                }
            })
            .build();
    }

    private AVWhips() {
    }
}
