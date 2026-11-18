package com.arcanavaria.thurible;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;

/**
 * Looks up what a cloud of an aspect does. An aspect's own {@link CloudEffect} entry wins; failing that, the entries of its
 * components (and theirs, down to the primals), each made stronger by how many steps of compounding lie between that
 * entry's aspect and the loaded one. So lattice-made compounds hit harder than their parents, with no table of them.
 */
public final class ThuribleEffects {
    /** An entry and how many steps it was carried up to reach the loaded aspect. */
    public record Resolved(CloudEffect effect, int boost) {
        public float scale() {
            return 1.0F + 0.5F * boost;
        }
    }

    private ThuribleEffects() {
    }

    /** Works on either side: the effect registry is synced to clients for the tooltip. */
    public static List<Resolved> resolve(HolderLookup.Provider access, ResourceKey<IAspect> aspect) {
        Optional<? extends HolderLookup.RegistryLookup<CloudEffect>> effects = access.lookup(ThuribleRegistry.EFFECT_KEY);
        Optional<? extends HolderLookup.RegistryLookup<IAspect>> aspects = access.lookup(IAspect.REGISTRY_KEY);
        List<Resolved> out = new ArrayList<>();
        if (effects.isEmpty() || aspects.isEmpty()) {
            return out;
        }
        collect(effects.get(), aspects.get(), aspect, 0, out, new HashSet<>());
        return out;
    }

    private static void collect(HolderLookup.RegistryLookup<CloudEffect> effects, HolderLookup.RegistryLookup<IAspect> aspects, ResourceKey<IAspect> aspect,
                                int boost, List<Resolved> out, Set<ResourceKey<IAspect>> seen) {
        if (!seen.add(aspect) || boost > 6) {
            return;
        }
        for (CloudEffect e : effects.listElements().map(Holder::value).toList()) {
            if (e.aspect().equals(aspect)) {
                out.add(new Resolved(e, boost));
                return;
            }
        }
        Optional<Holder.Reference<IAspect>> holder = aspects.get(aspect);
        if (holder.isEmpty()) {
            return;
        }
        for (Holder<IAspect> part : holder.get().value().components()) {
            part.unwrapKey().ifPresent(k -> collect(effects, aspects, k, boost + 1, out, seen));
        }
    }

    public static int colour(HolderLookup.Provider access, ResourceKey<IAspect> aspect) {
        return access.lookup(IAspect.REGISTRY_KEY).flatMap(r -> r.get(aspect)).map(h -> h.value().color()).orElse(0xC8B8E8);
    }
}
