package com.arcanavaria.lab.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

/**
 * A mini-boss body (data/&lt;ns&gt;/arcanavaria/lab_archetype/*.json): which mob it is built on, its stats and gear, how
 * big, and its boss-bar colour. Wizard, beast, demon, eldritch, construct and the rest are just entries here. Gear is
 * plain item ids ("mainhand": "minecraft:trident"): stacks cannot be read while datapack registries load.
 *
 * @param name   lang key of its name, shown first on the boss bar
 * @param speed  multiplier on the mob's own movement speed
 * @param scale  the mob's size (minecraft:scale)
 */
public record LabArchetype(String name, EntityType<?> entity, float health, float damage, float armor, float speed, float scale,
                           Map<EquipmentSlot, Item> equipment, BossEvent.BossBarColor bar) {
    public static final Codec<LabArchetype> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(LabArchetype::name),
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(LabArchetype::entity),
            Codec.floatRange(1, 4096).fieldOf("health").forGetter(LabArchetype::health),
            Codec.FLOAT.optionalFieldOf("damage", 6F).forGetter(LabArchetype::damage),
            Codec.FLOAT.optionalFieldOf("armor", 4F).forGetter(LabArchetype::armor),
            Codec.FLOAT.optionalFieldOf("speed", 1F).forGetter(LabArchetype::speed),
            Codec.floatRange(0.25F, 8F).optionalFieldOf("scale", 1.5F).forGetter(LabArchetype::scale),
            Codec.unboundedMap(EquipmentSlot.CODEC, BuiltInRegistries.ITEM.byNameCodec()).optionalFieldOf("equipment", Map.of()).forGetter(LabArchetype::equipment),
            BossEvent.BossBarColor.CODEC.optionalFieldOf("bar", BossEvent.BossBarColor.PURPLE).forGetter(LabArchetype::bar)
    ).apply(i, LabArchetype::new));
}
