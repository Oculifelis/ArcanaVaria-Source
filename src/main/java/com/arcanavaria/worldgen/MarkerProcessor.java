package com.arcanavaria.worldgen;

import static com.arcanavaria.ArcanaVaria.MODID;

import com.arcanavaria.lab.FloorGateBlock;
import com.arcanavaria.lab.LabRegistry;
import com.arcanavaria.lab.PhaseBlock;
import com.arcanavaria.lab.RuneStoneBlock;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

/**
 * {@code arcanavaria:markers}: turns Worldforge's data markers (a carrier block whose template NBT holds
 * {@code wf_marker}, see Worldforge's lab_rooms.MARKERS) into Arcana Varia's lab blocks as a lab or tower is placed.
 * Every pool's processor list runs it first. Markers with no lab block yet (guardian spawners, broken devices, the door
 * seal, floating platforms) stay their carrier. Template-frame offsets (a ward's box) are turned with the piece; block
 * facings are left in the template frame, since the template turns those itself after the processors run.
 */
public class MarkerProcessor extends StructureProcessor {
    public static final MapCodec<MarkerProcessor> CODEC = MapCodec.unit(MarkerProcessor::new);
    /** The registered processor type. */
    public static Supplier<StructureProcessorType<MarkerProcessor>> TYPE;

    @Override
    public StructureTemplate.@Nullable StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
            StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo current, StructurePlaceSettings settings) {
        if (current == null || current.nbt() == null) {
            return current;
        }
        CompoundTag nbt = current.nbt();
        String marker = nbt.getStringOr("wf_marker", "");
        if (marker.isEmpty()) {
            return current;
        }
        String id = marker.substring(marker.indexOf(':') + 1);
        return switch (id) {
            case "ward/core" -> {
                CompoundTag be = new CompoundTag();
                BlockPos a = turn(at(nbt, "min", -8, -1, -8), settings), b = turn(at(nbt, "max", 8, 24, 8), settings);
                put(be, "min", new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ())));
                put(be, "max", new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())));
                put(be, "entry", turn(at(nbt, "entry", 0, 1, 2), settings));
                put(be, "exit", turn(at(nbt, "exit", 0, 0, -12), settings));
                yield info(current, LabRegistry.WARD_CORE.get().defaultBlockState(), be);
            }
            case "boss" -> {
                CompoundTag be = new CompoundTag();
                be.putString("pool", MODID + ":" + pool(level, nbt.getStringOr("pool", "")));
                yield info(current, LabRegistry.GUARDIAN_ALTAR.get().defaultBlockState(), be);
            }
            case "puzzle/aspect_lock" -> lock(current, aspect(nbt.getStringOr("aspect", "ordo")));
            case "puzzle/lattice_sink" -> {
                // the lattice floor wants the compound its two sources make
                ListTag from = nbt.getListOrEmpty("from");
                String a = aspect(from.getStringOr(0, "ignis")), b = aspect(from.getStringOr(1, "aer"));
                yield lock(current, compound(level, a, b).orElse(a));
            }
            case "puzzle/lattice_source" -> {
                Identifier jar = Identifier.fromNamespaceAndPath("thaumaturge", "jar_normal");
                if (!BuiltInRegistries.BLOCK.containsKey(jar)) {
                    yield current;
                }
                CompoundTag be = new CompoundTag();
                be.putString("Aspect", aspect(nbt.getStringOr("aspect", "ignis")));
                be.putInt("Amount", 64);
                yield info(current, BuiltInRegistries.BLOCK.getValue(jar).defaultBlockState(), be);
            }
            case "puzzle/rune" -> info(current, LabRegistry.RUNE_STONE.get().defaultBlockState()
                    .setValue(RuneStoneBlock.RUNE, Math.clamp(nbt.getIntOr("index", 0), 0, 7)), null);
            case "puzzle/rune_lock" -> {
                // stone i carries glyph i, so the sequence is the glyphs in order; which stone is which is the puzzle
                CompoundTag be = new CompoundTag();
                be.put("sequence", new IntArrayTag(java.util.stream.IntStream.range(0, Math.clamp(nbt.getIntOr("count", 4), 1, 8)).toArray()));
                yield info(current, LabRegistry.RUNE_LOCK.get().defaultBlockState(), be);
            }
            case "puzzle/beam_emitter" -> info(current, LabRegistry.BEAM_EMITTER.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                    Optional.ofNullable(Direction.byName(nbt.getStringOr("facing", "east"))).filter(d -> d.getAxis().isHorizontal()).orElse(Direction.EAST)), new CompoundTag());
            case "puzzle/beam_mirror" -> info(current, LabRegistry.BEAM_MIRROR.get().defaultBlockState(), null);
            case "puzzle/beam_target" -> info(current, LabRegistry.BEAM_RECEPTOR.get().defaultBlockState(), new CompoundTag());
            case "parkour/vis_lift" -> info(current, LabRegistry.VIS_LIFT.get().defaultBlockState(), new CompoundTag());
            case "parkour/timed" -> info(current, LabRegistry.PHASE_BLOCK.get().defaultBlockState()
                    .setValue(PhaseBlock.OFFSET, Math.floorMod(nbt.getIntOr("phase", 0), 4)), new CompoundTag());
            case "floor/gate" -> info(current, LabRegistry.FLOOR_GATE.get().defaultBlockState().setValue(FloorGateBlock.OPEN, false), new CompoundTag());
            case "journal" -> {
                if (!current.state().hasProperty(LecternBlock.HAS_BOOK)) {
                    yield current;
                }
                CompoundTag book = new CompoundTag();
                book.putString("id", LabRegistry.LAB_JOURNAL.getId().toString());
                book.putInt("count", 1);
                CompoundTag be = new CompoundTag();
                be.put("Book", book);
                be.putInt("Page", 0);
                yield info(current, current.state().setValue(LecternBlock.HAS_BOOK, true), be);
            }
            default -> strip(current);
        };
    }

    private static StructureTemplate.StructureBlockInfo lock(StructureTemplate.StructureBlockInfo current, String aspect) {
        CompoundTag be = new CompoundTag();
        be.putString("aspect", aspect);
        be.putInt("need", 16);
        return info(current, LabRegistry.ASPECT_LOCK.get().defaultBlockState(), be);
    }

    /** The carrier stays, its NBT less the marker (a spawner keeps its mob). */
    private static StructureTemplate.StructureBlockInfo strip(StructureTemplate.StructureBlockInfo current) {
        CompoundTag nbt = current.nbt().copy();
        for (String k : List.copyOf(nbt.keySet())) {
            if (!k.equals("id") && !k.equals("SpawnData") && !k.equals("LootTable") && !k.equals("SpawnPotentials")) {
                nbt.remove(k);
            }
        }
        return new StructureTemplate.StructureBlockInfo(current.pos(), current.state(), nbt.size() > 0 ? nbt : null);
    }

    private static StructureTemplate.StructureBlockInfo info(StructureTemplate.StructureBlockInfo current, BlockState state, @Nullable CompoundTag be) {
        return new StructureTemplate.StructureBlockInfo(current.pos(), state, be);
    }

    /** The mini-boss pool for a "boss" marker: the tower's own, or a lab's by the dimension it stands in. */
    private static String pool(LevelReader level, String named) {
        if (named.startsWith("wizard_tower")) {
            return "tower_overworld";
        }
        if (level instanceof WorldGenLevel w) {
            var dim = w.getLevel().dimension();
            if (dim == Level.NETHER) {
                return "lab_nether";
            }
            if (dim == Level.END) {
                return "lab_end";
            }
        }
        return "lab_overworld";
    }

    private static String aspect(String name) {
        return name.contains(":") ? name : "thaumaturge:" + name;
    }

    /** Thaumaturge's compound of two aspects, either way round (the same table the Alembic Lattice uses). */
    private static Optional<String> compound(LevelReader level, String a, String b) {
        var reg = level.registryAccess().lookup(IAspect.REGISTRY_KEY);
        if (reg.isEmpty()) {
            return Optional.empty();
        }
        return reg.get().listElements().filter(h -> {
            List<Holder<IAspect>> parts = h.value().components();
            if (parts.size() != 2) {
                return false;
            }
            String p = key(parts.get(0)), q = key(parts.get(1));
            return (p.equals(a) && q.equals(b)) || (p.equals(b) && q.equals(a));
        }).map(h -> h.key().identifier().toString()).findFirst();
    }

    private static String key(Holder<IAspect> h) {
        return h.unwrapKey().map(k -> k.identifier().toString()).orElse("");
    }

    private static BlockPos at(CompoundTag nbt, String key, int x, int y, int z) {
        Optional<int[]> arr = nbt.getIntArray(key);
        if (arr.isPresent() && arr.get().length == 3) {
            return new BlockPos(arr.get()[0], arr.get()[1], arr.get()[2]);
        }
        ListTag list = nbt.getListOrEmpty(key);
        if (list.size() == 3) {
            return new BlockPos(list.getIntOr(0, x), list.getIntOr(1, y), list.getIntOr(2, z));
        }
        return new BlockPos(x, y, z);
    }

    /** A template-frame offset turned the way the piece was placed. */
    private static BlockPos turn(BlockPos p, StructurePlaceSettings settings) {
        BlockPos m = switch (settings.getMirror()) {
            case LEFT_RIGHT -> new BlockPos(p.getX(), p.getY(), -p.getZ());
            case FRONT_BACK -> new BlockPos(-p.getX(), p.getY(), p.getZ());
            default -> p;
        };
        return m.rotate(settings.getRotation());
    }

    private static void put(CompoundTag tag, String key, BlockPos p) {
        tag.put(key, new IntArrayTag(new int[] {p.getX(), p.getY(), p.getZ()}));
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return TYPE.get();
    }
}
