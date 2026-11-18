package com.arcanavaria;

import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.Item;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Datapack tables. {@code data/<ns>/data_maps/entity_type/base_attributes.json}: base attribute values for a creature
 * (the Greatwood Warden's and Exanimator's health, armour, damage, speed and so on ship there), set when it joins a
 * world. A creature that spawns fresh starts at its new full health.
 * {@code data/<ns>/data_maps/item/thurible.json}: how far a Thurible throws its cloud and how big the cloud is, without and
 * with the chain and bowl blueprints.
 */
public final class AVDataMaps {
    public static final Codec<Map<Holder<Attribute>, Double>> ATTRIBUTES =
            Codec.unboundedMap(BuiltInRegistries.ATTRIBUTE.holderByNameCodec(), Codec.DOUBLE);

    public static final DataMapType<EntityType<?>, Map<Holder<Attribute>, Double>> BASE_ATTRIBUTES = DataMapType.builder(
            ArcanaVaria.id("base_attributes"), Registries.ENTITY_TYPE, ATTRIBUTES).build();

    /** A Thurible's numbers: cloud distance in front of the bearer and cloud radius, plain and upgraded. */
    public record Thurible(double reach, double chainReach, float radius, float cloudRadius) {
        public static final Thurible DEFAULT = new Thurible(3.0, 4.5, 2.0F, 3.0F);
        public static final Codec<Thurible> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(0, 32).optionalFieldOf("reach", DEFAULT.reach).forGetter(Thurible::reach),
                Codec.doubleRange(0, 32).optionalFieldOf("chain_reach", DEFAULT.chainReach).forGetter(Thurible::chainReach),
                Codec.floatRange(0.25F, 16).optionalFieldOf("radius", DEFAULT.radius).forGetter(Thurible::radius),
                Codec.floatRange(0.25F, 16).optionalFieldOf("cloud_radius", DEFAULT.cloudRadius).forGetter(Thurible::cloudRadius))
                .apply(i, Thurible::new));

        public static Thurible of(Item item) {
            Thurible t = item.builtInRegistryHolder().getData(THURIBLE);
            return t == null ? DEFAULT : t;
        }
    }

    /**
     * {@code data/<ns>/data_maps/entity_type/boss_moves.json}: a boss's move numbers by name (damage, radius,
     * cooldown in ticks, ...). A name left out keeps the built-in value.
     */
    public static final DataMapType<EntityType<?>, Map<String, Double>> BOSS_MOVES = DataMapType.builder(
            ArcanaVaria.id("boss_moves"), Registries.ENTITY_TYPE, Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)).build();

    /** The named move number for this creature's type, or {@code def}. */
    public static double move(net.minecraft.world.entity.Entity entity, String key, double def) {
        Map<String, Double> m = entity.getType().builtInRegistryHolder().getData(BOSS_MOVES);
        Double v = m == null ? null : m.get(key);
        return v == null ? def : v;
    }

    /** The phase-one or phase-two number: the phase-two one is named {@code key + "_phase_two"}. */
    public static float move(net.minecraft.world.entity.Entity entity, String key, boolean phaseTwo, double one, double two) {
        return (float) (phaseTwo ? move(entity, key + "_phase_two", two) : move(entity, key, one));
    }

    /**
     * {@code data/<ns>/data_maps/enchantment/enchantment_values.json}: the numbers behind the scythe enchantments by
     * name (chance, damage, ticks, ...). Names ending in {@code _1}, {@code _2}, {@code _3} are per level. A name left
     * out keeps the built-in value.
     */
    public static final DataMapType<net.minecraft.world.item.enchantment.Enchantment, Map<String, Double>> ENCHANTMENT_VALUES =
            DataMapType.builder(ArcanaVaria.id("enchantment_values"), Registries.ENCHANTMENT,
                    Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)).build();

    /** The named number of an enchantment in this world's data, or {@code def}. */
    public static double enchant(net.minecraft.world.level.Level level,
                                 net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key,
                                 String name, double def) {
        Map<String, Double> m = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key)
                .map(h -> h.getData(ENCHANTMENT_VALUES)).orElse(null);
        Double v = m == null ? null : m.get(name);
        return v == null ? def : v;
    }

    /** The per-level number {@code name_<lvl>}, or {@code defs[lvl]}. */
    public static double enchant(net.minecraft.world.level.Level level,
                                 net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key,
                                 String name, int lvl, double[] defs) {
        int i = Math.max(0, Math.min(lvl, defs.length - 1));
        return enchant(level, key, name + "_" + lvl, defs[i]);
    }

    public static final DataMapType<Item, Thurible> THURIBLE = DataMapType.builder(
            ArcanaVaria.id("thurible"), Registries.ITEM, Thurible.CODEC).build();

    private AVDataMaps() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterDataMapTypesEvent e) -> {
            e.register(BASE_ATTRIBUTES);
            e.register(THURIBLE);
            e.register(BOSS_MOVES);
            e.register(ENCHANTMENT_VALUES);
        });
        NeoForge.EVENT_BUS.addListener(AVDataMaps::joined);
        if (Boolean.getBoolean("arcanavaria.datatest")) {   // server override test: report what the loaded data says
            NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
                Thurible t = Thurible.of(com.arcanavaria.thurible.ThuribleRegistry.THURIBLE.get());
                ArcanaVaria.LOG.info("DATATEST thurible reach={} chain_reach={} radius={} cloud_radius={}",
                        t.reach(), t.chainReach(), t.radius(), t.cloudRadius());
                BuiltInRegistries.ENTITY_TYPE.getDataMap(BOSS_MOVES).forEach((key, moves) ->
                        ArcanaVaria.LOG.info("DATATEST boss_moves {} {}", key.identifier(), new java.util.TreeMap<>(moves)));
                e.getServer().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getDataMap(ENCHANTMENT_VALUES).forEach((key, values) ->
                        ArcanaVaria.LOG.info("DATATEST enchantment_values {} {}", key.identifier(), new java.util.TreeMap<>(values)));
            });
        }
    }

    private static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        Map<Holder<Attribute>, Double> values = living.getType().builtInRegistryHolder().getData(BASE_ATTRIBUTES);
        if (values == null) {
            return;
        }
        float max = living.getMaxHealth();
        values.forEach((attribute, value) -> {
            AttributeInstance instance = living.getAttribute(attribute);
            if (instance != null && instance.getBaseValue() != value) {
                instance.setBaseValue(value);
            }
        });
        if (!event.loadedFromDisk() && living.getMaxHealth() != max) {
            living.setHealth(living.getMaxHealth());
        }
    }
}
