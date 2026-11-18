package com.arcanavaria.lab.boss;

import com.arcanavaria.thurible.CloudEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * A procedural mini-boss trait (data/&lt;ns&gt;/arcanavaria/lab_trait/*.json), rolled per spawn. Every trait shows: motes
 * of its colour and its particle stream off the body, and it may borrow one of Thaumaturge's champion traits
 * ({@code champion}, e.g. thaumaturge:fiery) for that trait's own look and behaviour. The boss bar names it.
 *
 * @param name     lang key of its name on the boss bar
 * @param self     effects the boss always has
 * @param on_hit   what its hits do to the one hit
 * @param aura     an effect on players near it, every second
 * @param ability  one active power on a timer
 */
public record LabTrait(String name, int colour, Optional<ParticleOptions> particle, Optional<Identifier> champion,
                       List<CloudEffect.Applied> self, List<Modifier> attributes, OnHit onHit, Optional<Aura> aura,
                       Optional<Ability> ability) {
    public static final Codec<LabTrait> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(LabTrait::name),
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("colour").forGetter(LabTrait::colour),
            ParticleTypes.CODEC.optionalFieldOf("particle").forGetter(LabTrait::particle),
            Identifier.CODEC.optionalFieldOf("champion").forGetter(LabTrait::champion),
            CloudEffect.Applied.CODEC.listOf().optionalFieldOf("self", List.of()).forGetter(LabTrait::self),
            Modifier.CODEC.listOf().optionalFieldOf("attributes", List.of()).forGetter(LabTrait::attributes),
            OnHit.CODEC.optionalFieldOf("on_hit", OnHit.NONE).forGetter(LabTrait::onHit),
            Aura.CODEC.optionalFieldOf("aura").forGetter(LabTrait::aura),
            Ability.CODEC.optionalFieldOf("ability").forGetter(LabTrait::ability)
    ).apply(i, LabTrait::new));

    public record Modifier(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        public static final Codec<Modifier> CODEC = RecordCodecBuilder.create(i -> i.group(
                Attribute.CODEC.fieldOf("attribute").forGetter(Modifier::attribute),
                Codec.DOUBLE.fieldOf("amount").forGetter(Modifier::amount),
                AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                        .forGetter(Modifier::operation)).apply(i, Modifier::new));
    }

    /** @param lifesteal share of the damage dealt the boss heals */
    public record OnHit(List<CloudEffect.Applied> effects, int burnSeconds, float lifesteal, float knockback) {
        public static final OnHit NONE = new OnHit(List.of(), 0, 0, 0);
        public static final Codec<OnHit> CODEC = RecordCodecBuilder.create(i -> i.group(
                CloudEffect.Applied.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(OnHit::effects),
                Codec.INT.optionalFieldOf("burn_seconds", 0).forGetter(OnHit::burnSeconds),
                Codec.FLOAT.optionalFieldOf("lifesteal", 0F).forGetter(OnHit::lifesteal),
                Codec.FLOAT.optionalFieldOf("knockback", 0F).forGetter(OnHit::knockback)).apply(i, OnHit::new));
    }

    public record Aura(float radius, CloudEffect.Applied effect) {
        public static final Codec<Aura> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(1, 32).fieldOf("radius").forGetter(Aura::radius),
                CloudEffect.Applied.CODEC.fieldOf("effect").forGetter(Aura::effect)).apply(i, Aura::new));
    }

    /**
     * pulse: a shockwave hurting and throwing back players within {@code radius} for {@code power} damage.
     * summon: {@code count} of {@code entity} round it (never more than twice that alive near it).
     * blink: it vanishes and reappears beside its target, at most {@code radius} from its altar.
     * heal: below half health it heals {@code power}.
     * volley: {@code count} small fireballs at its target.
     */
    public record Ability(Kind kind, int every, float radius, float power, Optional<EntityType<?>> entity, int count) {
        public static final Codec<Ability> CODEC = RecordCodecBuilder.create(i -> i.group(
                Kind.CODEC.fieldOf("kind").forGetter(Ability::kind),
                Codec.intRange(20, 6000).optionalFieldOf("every", 160).forGetter(Ability::every),
                Codec.FLOAT.optionalFieldOf("radius", 6F).forGetter(Ability::radius),
                Codec.FLOAT.optionalFieldOf("power", 6F).forGetter(Ability::power),
                BuiltInRegistries.ENTITY_TYPE.byNameCodec().optionalFieldOf("entity").forGetter(Ability::entity),
                Codec.intRange(1, 8).optionalFieldOf("count", 2).forGetter(Ability::count)).apply(i, Ability::new));
    }

    public enum Kind implements StringRepresentable {
        PULSE, SUMMON, BLINK, HEAL, VOLLEY;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
