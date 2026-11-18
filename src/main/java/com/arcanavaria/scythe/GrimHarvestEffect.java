package com.arcanavaria.scythe;

import com.arcanavaria.AVRegistry;
import com.arcanavaria.ArcanaVaria;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Grim Harvest, from a scythe kill: 20% faster, and 20% less damage taken (ScytheEvents), with a whisper every three
 * seconds that only the reaper hears.
 */
public class GrimHarvestEffect extends MobEffect {
    private static final int WHISPER_INTERVAL = 60;

    public GrimHarvestEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x4CAF50);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ArcanaVaria.id("effect.grim_harvest.speed"), 0.2,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration > 0 && duration % WHISPER_INTERVAL == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), AVRegistry.GRIM_HARVEST_PROC.get(),
                    SoundSource.PLAYERS, 0.45f, 0.95f + player.getRandom().nextFloat() * 0.1f);
        }
        return true;
    }
}
