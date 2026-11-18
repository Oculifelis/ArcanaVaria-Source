package com.arcanavaria.thurible;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;

/**
 * What a Thurible cloud of one aspect does to whatever stands in it, once a second: a datapack registry entry
 * ({@code data/<ns>/arcanavaria/thurible_effect/<name>.json}). An aspect with no entry of its own carries the effects of
 * its components, made stronger by how far it is compounded (ThuribleEffects).
 *
 * @param description translation key of the plain sentence the tooltip shows
 */
public record CloudEffect(ResourceKey<IAspect> aspect, Target target, List<Applied> effects, int burn, float heal, float damage,
                          boolean extinguish, String description) {
    public static final Codec<CloudEffect> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(IAspect.REGISTRY_KEY).fieldOf("aspect").forGetter(CloudEffect::aspect),
            Target.CODEC.optionalFieldOf("target", Target.ENEMIES).forGetter(CloudEffect::target),
            Applied.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(CloudEffect::effects),
            Codec.INT.optionalFieldOf("burn_seconds", 0).forGetter(CloudEffect::burn),
            Codec.FLOAT.optionalFieldOf("heal", 0.0F).forGetter(CloudEffect::heal),
            Codec.FLOAT.optionalFieldOf("damage", 0.0F).forGetter(CloudEffect::damage),
            Codec.BOOL.optionalFieldOf("extinguish", false).forGetter(CloudEffect::extinguish),
            Codec.STRING.optionalFieldOf("description", "").forGetter(CloudEffect::description)).apply(i, CloudEffect::new));

    /** Who the cloud touches: its bearer's enemies, the bearer and its allies, or everyone. */
    public enum Target implements StringRepresentable {
        ENEMIES("enemies"), ALLIES("allies"), EVERYONE("everyone");

        public static final Codec<Target> CODEC = StringRepresentable.fromEnum(Target::values);
        private final String name;

        Target(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** A potion effect the cloud gives, for {@code duration} ticks. */
    public record Applied(Holder<MobEffect> effect, int duration, int amplifier) {
        public static final Codec<Applied> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(Applied::effect),
                Codec.INT.optionalFieldOf("duration", 100).forGetter(Applied::duration),
                Codec.INT.optionalFieldOf("amplifier", 0).forGetter(Applied::amplifier)).apply(i, Applied::new));
    }

    public Optional<String> descriptionKey() {
        return description.isEmpty() ? Optional.empty() : Optional.of(description);
    }
}
