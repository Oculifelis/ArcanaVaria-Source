package com.arcanavaria.lab.boss;

import com.arcanavaria.lab.LabRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * A guardian altar's roll table (data/&lt;ns&gt;/arcanavaria/lab_pool/*.json): weighted archetypes and traits, each
 * optionally only in some dimensions, and how many traits to roll. Worldforge's structures name a pool per structure
 * kind (tower_overworld, lab_nether, ...).
 */
public record LabPool(List<Entry<LabArchetype>> archetypes, List<Entry<LabTrait>> traits, int minTraits, int maxTraits) {
    public static final Codec<LabPool> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.codec(LabRegistry.ARCHETYPE_KEY).listOf().fieldOf("archetypes").forGetter(LabPool::archetypes),
            Entry.codec(LabRegistry.TRAIT_KEY).listOf().optionalFieldOf("traits", List.of()).forGetter(LabPool::traits),
            Codec.intRange(0, 6).optionalFieldOf("min_traits", 1).forGetter(LabPool::minTraits),
            Codec.intRange(0, 6).optionalFieldOf("max_traits", 3).forGetter(LabPool::maxTraits)
    ).apply(i, LabPool::new));

    public record Entry<T>(ResourceKey<T> id, int weight, List<ResourceKey<Level>> dimensions) {
        static <T> Codec<Entry<T>> codec(ResourceKey<? extends net.minecraft.core.Registry<T>> registry) {
            return RecordCodecBuilder.create(i -> i.group(
                    ResourceKey.codec(registry).fieldOf("id").forGetter(Entry<T>::id),
                    Codec.intRange(1, 1000).optionalFieldOf("weight", 10).forGetter(Entry<T>::weight),
                    Level.RESOURCE_KEY_CODEC.listOf().optionalFieldOf("dimensions", List.of()).forGetter(Entry<T>::dimensions)
            ).apply(i, Entry::new));
        }

        boolean fits(ResourceKey<Level> dim) {
            return dimensions.isEmpty() || dimensions.contains(dim);
        }
    }

    public Optional<ResourceKey<LabArchetype>> rollArchetype(ResourceKey<Level> dim, RandomSource r) {
        return pick(new ArrayList<>(archetypes), dim, r);
    }

    /** Distinct traits, between min and max of them. */
    public List<ResourceKey<LabTrait>> rollTraits(ResourceKey<Level> dim, RandomSource r) {
        List<Entry<LabTrait>> left = new ArrayList<>(traits);
        int n = minTraits + (maxTraits > minTraits ? r.nextInt(maxTraits - minTraits + 1) : 0);
        List<ResourceKey<LabTrait>> out = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            Optional<ResourceKey<LabTrait>> t = pick(left, dim, r);
            if (t.isEmpty()) {
                break;
            }
            out.add(t.get());
            left.removeIf(e -> e.id().equals(t.get()));
        }
        return out;
    }

    private static <T> Optional<ResourceKey<T>> pick(List<Entry<T>> entries, ResourceKey<Level> dim, RandomSource r) {
        entries.removeIf(e -> !e.fits(dim));
        int total = entries.stream().mapToInt(Entry::weight).sum();
        if (total <= 0) {
            return Optional.empty();
        }
        int roll = r.nextInt(total);
        for (Entry<T> e : entries) {
            roll -= e.weight();
            if (roll < 0) {
                return Optional.of(e.id());
            }
        }
        return Optional.empty();
    }
}
