package com.arcanavaria.lab.boss;

import com.arcanavaria.lab.LabRegistry;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;

/** On a living mini-boss: the altar that raised it, its body and its rolled traits. */
public record BossData(BlockPos altar, ResourceKey<LabArchetype> archetype, List<ResourceKey<LabTrait>> traits) {
    public static final MapCodec<BossData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockPos.CODEC.fieldOf("altar").forGetter(BossData::altar),
            ResourceKey.codec(LabRegistry.ARCHETYPE_KEY).fieldOf("archetype").forGetter(BossData::archetype),
            ResourceKey.codec(LabRegistry.TRAIT_KEY).listOf().fieldOf("traits").forGetter(BossData::traits)
    ).apply(i, BossData::new));
}
