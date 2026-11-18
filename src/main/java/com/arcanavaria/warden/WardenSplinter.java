package com.arcanavaria.warden;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A jagged splinter of greatwood the Warden hurls in its volley; drawn as a stick, breaks into wood chips on impact. */
public class WardenSplinter extends ThrowableItemProjectile {
    public static final float DAMAGE = 5.0F;

    public WardenSplinter(EntityType<? extends WardenSplinter> type, Level level) {
        super(type, level);
    }

    public WardenSplinter(Level level, LivingEntity owner) {
        super(WardenRegistry.SPLINTER.get(), owner, level, new ItemStack(Items.STICK));
    }

    @Override
    protected Item getDefaultItem() {
        return Items.STICK;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (this.level() instanceof ServerLevel level && hit.getEntity() != this.getOwner()) {
            hit.getEntity().hurtServer(level, this.damageSources().thrown(this, this.getOwner()),
                    this.getOwner() == null ? DAMAGE : (float) com.arcanavaria.AVDataMaps.move(this.getOwner(), "splinter_damage", DAMAGE));
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_LOG.defaultBlockState()),
                this.getX(), this.getY(), this.getZ(), 8, 0.1, 0.1, 0.1, 0.1);
            this.playSound(SoundEvents.WOOD_BREAK, 0.6F, 1.3F);
            this.discard();
        }
    }
}
