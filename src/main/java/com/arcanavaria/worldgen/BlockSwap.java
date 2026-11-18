package com.arcanavaria.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.fml.ModList;
import org.jspecify.annotations.Nullable;

/**
 * {@code arcanavaria:block_swap}: Worldforge's shared block library on Arcana Varia's labs and tower. A copy of
 * Worldforge's {@code worldforge_dungeons:block_swap} without its on/off switches. {@code sets} is an ordered list of
 * {@code {"requires": modid, "swap": {from: to}}}; the first set whose mod is loaded (and, with {@code "whole": true},
 * all of whose blocks exist) is used. A key matches a block id exactly, or as a prefix followed by a shape suffix
 * (s, _stairs, _slab, _wall, _fence, _fence_gate). The block's matching properties carry over.
 * Register: {@code PROCESSORS.register("block_swap", () -> () -> BlockSwap.CODEC)} on
 * {@code DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, MODID)}, and set {@link #TYPE} to it.
 * <p>With {@code "mode": "random"} every usable set is a candidate instead, drawn by its {@code weight} (default 1),
 * once per structure: the draw is seeded by the world seed, the structure's start chunk and the processor's own
 * blocks, so one structure is consistent inside and the same world rebuilds it the same, while the next structure of
 * the same kind draws again. A set whose {@code requires} is "minecraft" with an empty swap keeps the vanilla block.
 */
public class BlockSwap extends StructureProcessor {
    public record Set(String requires, Map<String, String> swap, float weight) {
        public static final Codec<Set> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("requires").forGetter(Set::requires),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("swap").forGetter(Set::swap),
                Codec.FLOAT.optionalFieldOf("weight", 1.0f).forGetter(Set::weight)
        ).apply(i, Set::new));
    }

    public static final MapCodec<BlockSwap> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Set.CODEC.listOf().fieldOf("sets").forGetter(b -> b.sets),
            Codec.BOOL.optionalFieldOf("whole", false).forGetter(b -> b.whole),
            Codec.STRING.optionalFieldOf("mode", "first").forGetter(b -> b.mode)
    ).apply(i, BlockSwap::new));
    /** The registered processor type. */
    public static java.util.function.Supplier<StructureProcessorType<BlockSwap>> TYPE;
    private static final List<String> SUFFIXES = List.of("", "s", "_stairs", "_slab", "_wall", "_fence", "_fence_gate");
    private final List<Set> sets;
    private final boolean whole;
    private final String mode;
    /** A usable set's map, its keys longest first, and its weight. */
    private record Active(@Nullable Map<String, String> swap, List<String> keys, float weight) {
    }

    private static final Active NONE = new Active(null, List.of(), 0f);
    /** The usable sets, in order; worked out once, on first use (the config and mods are loaded by then). */
    private volatile @Nullable List<Active> usable;
    /** Seed by chunk, so the structure start lookup runs once per chunk and processor. */
    private final Map<Long, Long> seeds = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Long> e) {
            return size() > 256;
        }
    };
    private final long salt;

    public BlockSwap(List<Set> sets, boolean whole, String mode) {
        this.sets = sets;
        this.whole = whole;
        this.mode = mode;
        // the processor's own blocks, so the roles of one structure draw independently but reproducibly
        this.salt = String.join(",", new TreeSet<>(sets.isEmpty() ? java.util.Set.of() : sets.get(0).swap().keySet())).hashCode();
    }

    private List<Active> usable() {
        List<Active> u = usable;
        if (u == null) {
            u = new ArrayList<>();
            {
                for (Set s : sets) {
                    if (ModList.get().isLoaded(s.requires())
                            && (!whole || s.swap().values().stream().allMatch(BlockSwap::exists))) {
                        List<String> keys = new ArrayList<>(s.swap().keySet());
                        keys.sort(Comparator.comparingInt(String::length).reversed());
                        u.add(new Active(s.swap(), keys, Math.max(0f, s.weight())));
                        if (!"random".equals(mode)) break;
                    }
                }
            }
            usable = u;
        }
        return u;
    }

    private Active active(LevelReader level, BlockPos offset, BlockPos pos) {
        List<Active> u = usable();
        if (u.isEmpty()) return NONE;
        if (u.size() == 1 || !"random".equals(mode)) return u.get(0);
        float total = 0f;
        for (Active a : u) total += a.weight();
        if (total <= 0f) return u.get(0);
        float r = new Random(seed(level, offset, pos) ^ salt).nextFloat() * total;
        for (Active a : u) {
            r -= a.weight();
            if (r < 0f) return a;
        }
        return u.get(u.size() - 1);
    }

    /** The world seed mixed with the start chunk of the structure this block belongs to (of the structures referenced
     * in the block's chunk, the one starting nearest the piece). */
    private long seed(LevelReader level, BlockPos offset, BlockPos pos) {
        long chunk = ((long) (pos.getZ() >> 4) << 32) | ((pos.getX() >> 4) & 0xffffffffL);
        Long known;
        synchronized (seeds) {
            known = seeds.get(chunk);
        }
        if (known != null) return known;
        long world = level instanceof WorldGenLevel w ? w.getSeed() : 0L;
        int ox = offset.getX() >> 4, oz = offset.getZ() >> 4;
        long start = ((long) oz << 32) | (ox & 0xffffffffL);
        try {
            ChunkAccess c = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_REFERENCES, false);
            if (c != null) {
                long best = Long.MAX_VALUE;
                for (var refs : c.getAllReferences().values()) {
                    for (long ref : refs) {   // ChunkPos.asLong: x low, z high
                        long dx = (int) ref - ox, dz = (int) (ref >> 32) - oz;
                        if (dx * dx + dz * dz < best) {
                            best = dx * dx + dz * dz;
                            start = ref;
                        }
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // outside the generating region: the piece's own chunk stands in
        }
        long s = mix(world ^ mix(start));
        synchronized (seeds) {
            seeds.put(chunk, s);
        }
        return s;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private @Nullable String target(Active a, String id) {
        if (a.swap() == null) return null;
        String to = a.swap().get(id);
        if (to != null) return to;
        for (String k : a.keys()) {
            if (id.startsWith(k) && SUFFIXES.contains(id.substring(k.length()))) {
                return a.swap().get(k) + id.substring(k.length());
            }
        }
        return null;
    }

    @Override
    public StructureTemplate.@Nullable StructureBlockInfo processBlock(LevelReader level, BlockPos offset,
            BlockPos pos, StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo current,
            StructurePlaceSettings settings) {
        if (current == null) return null;
        String t = target(active(level, offset, pos), BuiltInRegistries.BLOCK.getKey(current.state().getBlock()).toString());
        if (t == null) return current;
        Identifier to = Identifier.tryParse(t);
        if (to == null || !BuiltInRegistries.BLOCK.containsKey(to)) return current;
        BlockState st = BuiltInRegistries.BLOCK.getValue(to).defaultBlockState();
        for (Property<?> p : current.state().getProperties()) {
            st = copy(current.state(), st, p);
        }
        return new StructureTemplate.StructureBlockInfo(current.pos(), st, current.nbt());
    }

    private static boolean exists(String id) {
        Identifier rl = Identifier.tryParse(id);
        return rl != null && BuiltInRegistries.BLOCK.containsKey(rl);
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> p) {
        Property<?> q = to.getBlock().getStateDefinition().getProperty(p.getName());
        if (q == null) return to;
        return q.getValue(p.getName(from.getValue(p))).map(v -> setRaw(to, q, v)).orElse(to);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState setRaw(BlockState st, Property q, Object v) {
        return st.setValue(q, (Comparable) v);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return TYPE.get();
    }
}
