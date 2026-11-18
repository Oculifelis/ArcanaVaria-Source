package com.arcanavaria.client;

import com.arcanavaria.lattice.AspectChemistry;
import com.arcanavaria.lattice.LatticeBlockEntity;
import com.arcanavaria.lattice.LatticeRegistry;
import com.arcanavaria.lattice.Mote;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.essentia.jar.BlockEntityJar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.slf4j.Logger;

/**
 * The lattice stage of the client test: two jars, ignis and aer, each with an intake on top, joined by equal pipe runs
 * at a junction whose compound runs on to an outlet over a third jar. Equal runs mean the two motes reach the junction
 * in the same tick, so the third jar should fill with whatever compound Thaumaturge's table makes of the pair.
 */
final class LatticeAutotest {
    private static final int Y = -59;
    private static final BlockPos JAR_A = new BlockPos(-3, Y - 1, 6), JAR_B = new BlockPos(3, Y - 1, 6), JAR_OUT = new BlockPos(0, Y - 1, 9);
    private static final BlockPos JUNCTION = new BlockPos(0, Y, 6);

    private LatticeAutotest() {
    }

    static void build(ServerPlayer p, Logger log) {
        ServerLevel level = (ServerLevel) p.level();
        ResourceKey<IAspect> a = TTAspects.IGNIS, b = TTAspects.AER;
        log.info("AVTEST lattice table size={} {}+{}={} split(lux)={}", AspectChemistry.size(level), a.identifier(), b.identifier(),
                AspectChemistry.combine(level, a, b), AspectChemistry.split(level, TTAspects.LUX));
        Block jar = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "jar_normal"));
        for (BlockPos at : new BlockPos[] {JAR_A, JAR_B, JAR_OUT}) {
            level.setBlock(at, jar.defaultBlockState(), 3);
        }
        fill(level, JAR_A, a, 32, log);
        fill(level, JAR_B, b, 32, log);
        place(level, JAR_A.above(), LatticeRegistry.INTAKE.get().defaultBlockState(), Direction.DOWN);
        place(level, JAR_B.above(), LatticeRegistry.INTAKE.get().defaultBlockState(), Direction.DOWN);
        place(level, JUNCTION, LatticeRegistry.JUNCTION.get().defaultBlockState(), Direction.SOUTH);
        place(level, JAR_OUT.above(), LatticeRegistry.OUTLET.get().defaultBlockState(), Direction.DOWN);
        BlockPos[] pipes = {new BlockPos(-2, Y, 6), new BlockPos(-1, Y, 6), new BlockPos(1, Y, 6), new BlockPos(2, Y, 6),
            new BlockPos(0, Y, 7), new BlockPos(0, Y, 8)};
        for (BlockPos at : pipes) {
            level.setBlock(at, LatticeRegistry.PIPE.get().defaultBlockState(), 3);
        }
        for (BlockPos at : pipes) {
            level.setBlock(at, Block.updateFromNeighbourShapes(level.getBlockState(at), level, at), 3);
        }
        log.info("AVTEST lattice built: pipe states {}", level.getBlockState(pipes[0]));
        // hover back from the lattice and look down at it (flying, or the player drops and the lattice leaves the frame)
        p.getAbilities().mayfly = true;
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        var server = p.level().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kill @e[type=minecraft:item]");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 -55 1.5 0 42");
    }

    private static void place(ServerLevel level, BlockPos at, BlockState state, Direction facing) {
        level.setBlock(at, state.setValue(BlockStateProperties.FACING, facing), 3);
    }

    private static void fill(ServerLevel level, BlockPos at, ResourceKey<IAspect> aspect, int amount, Logger log) {
        if (level.getBlockEntity(at) instanceof BlockEntityJar jar) {
            jar.setStorageContents(aspect, amount);
            jar.setChanged();
        } else {
            log.error("AVTEST lattice FAIL no jar at {}", at.toShortString());
        }
    }

    /** One line: what each jar holds and how many motes are in flight. */
    static void report(ServerPlayer p, Logger log, String tag) {
        ServerLevel level = (ServerLevel) p.level();
        int motes = 0;
        StringBuilder kinds = new StringBuilder();
        for (int x = -3; x <= 3; x++) {
            for (int z = 6; z <= 9; z++) {
                if (level.getBlockEntity(new BlockPos(x, Y, z)) instanceof LatticeBlockEntity be) {
                    motes += be.packets().size();
                    for (Mote m : be.packets()) {
                        kinds.append(m.aspect.identifier().getPath()).append(' ');
                    }
                }
            }
        }
        log.info("AVTEST lattice {} a={} b={} out={} motes={} [{}]", tag, jar(level, JAR_A), jar(level, JAR_B), jar(level, JAR_OUT),
                motes, kinds.toString().trim());
    }

    private static String jar(ServerLevel level, BlockPos at) {
        return level.getBlockEntity(at) instanceof BlockEntityJar jar
                ? (jar.aspectKey() == null ? "empty" : jar.aspectKey().identifier().getPath() + "x" + jar.amount()) : "missing";
    }
}
