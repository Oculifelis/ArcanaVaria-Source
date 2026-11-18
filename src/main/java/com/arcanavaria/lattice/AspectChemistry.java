package com.arcanavaria.lattice;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The lattice's chemistry, read from Thaumaturge's aspect registry: every aspect with two components is the compound of
 * that pair. Nothing is listed here, so aspects other mods or datapacks add combine and split like Thaumaturge's own.
 * The table is rebuilt whenever the level's aspect registry is a new object (a new world or a datapack reload).
 */
public final class AspectChemistry {
    private static @Nullable Registry<IAspect> builtFor;
    private static Map<Pair, ResourceKey<IAspect>> compounds = Map.of();

    private record Pair(ResourceKey<IAspect> a, ResourceKey<IAspect> b) {
        static Pair of(ResourceKey<IAspect> x, ResourceKey<IAspect> y) {
            return x.identifier().compareTo(y.identifier()) <= 0 ? new Pair(x, y) : new Pair(y, x);
        }
    }

    private AspectChemistry() {
    }

    public static Registry<IAspect> registry(Level level) {
        return level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY);
    }

    private static synchronized Map<Pair, ResourceKey<IAspect>> table(Level level) {
        Registry<IAspect> reg = registry(level);
        if (reg != builtFor) {
            Map<Pair, ResourceKey<IAspect>> map = new HashMap<>();
            reg.listElements().forEach(h -> {
                List<Holder<IAspect>> parts = h.value().components();
                if (parts.size() == 2) {
                    Optional<ResourceKey<IAspect>> a = parts.get(0).unwrapKey(), b = parts.get(1).unwrapKey();
                    if (a.isPresent() && b.isPresent()) {
                        map.putIfAbsent(Pair.of(a.get(), b.get()), h.key());
                    }
                }
            });
            compounds = map;
            builtFor = reg;
        }
        return compounds;
    }

    /** The compound the two aspects make, or null if they make none. */
    public static @Nullable ResourceKey<IAspect> combine(Level level, ResourceKey<IAspect> a, ResourceKey<IAspect> b) {
        return table(level).get(Pair.of(a, b));
    }

    /** The two parents of a compound in Thaumaturge's order, or an empty list for a primal. */
    public static List<ResourceKey<IAspect>> split(Level level, ResourceKey<IAspect> aspect) {
        return registry(level).get(aspect).map(h -> h.value().components()).filter(c -> c.size() == 2)
                .map(c -> c.stream().map(p -> p.unwrapKey().orElseThrow()).toList()).orElse(List.of());
    }

    /** The aspect's colour as 0xRRGGBB, white if unknown. */
    public static int colour(Level level, ResourceKey<IAspect> aspect) {
        return registry(level).getOptional(aspect).map(IAspect::color).orElse(0xFFFFFF) & 0xFFFFFF;
    }

    /** How many compounds the table holds (the client test logs it). */
    public static int size(Level level) {
        return table(level).size();
    }
}
