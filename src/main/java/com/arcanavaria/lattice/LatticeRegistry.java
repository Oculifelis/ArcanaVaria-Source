package com.arcanavaria.lattice;

import static com.arcanavaria.ArcanaVaria.MODID;

import com.arcanavaria.AVRegistry;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Alembic Lattice (0.4.0): glass pipes, intake and outlet, junction, splitter prism and synchronizer. */
public final class LatticeRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredBlock<LatticePipeBlock> PIPE = block("lattice_pipe", LatticePipeBlock::new, 0.4f, SoundType.GLASS);
    public static final DeferredBlock<LatticeDeviceBlock> INTAKE = device("lattice_intake", LatticeKind.INTAKE);
    public static final DeferredBlock<LatticeDeviceBlock> OUTLET = device("lattice_outlet", LatticeKind.OUTLET);
    public static final DeferredBlock<LatticeDeviceBlock> JUNCTION = device("lattice_junction", LatticeKind.JUNCTION);
    public static final DeferredBlock<LatticeDeviceBlock> PRISM = device("lattice_prism", LatticeKind.PRISM);
    public static final DeferredBlock<LatticeDeviceBlock> SYNCHRONIZER = device("lattice_synchronizer", LatticeKind.SYNCHRONIZER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LatticeBlockEntity>> LATTICE_BE =
            BLOCK_ENTITIES.register("lattice", () -> new BlockEntityType<>(LatticeBlockEntity::new, PIPE.get(), INTAKE.get(),
                    OUTLET.get(), JUNCTION.get(), PRISM.get(), SYNCHRONIZER.get()));

    private LatticeRegistry() {
    }

    private static DeferredBlock<LatticeDeviceBlock> device(String name, LatticeKind kind) {
        return block(name, p -> new LatticeDeviceBlock(kind, p), 1.5f, SoundType.COPPER);
    }

    private static <B extends Block> DeferredBlock<B> block(String name, Function<BlockBehaviour.Properties, B> factory, float strength,
                                                            SoundType sound) {
        DeferredBlock<B> block = BLOCKS.registerBlock(name, factory, p -> p.mapColor(MapColor.NONE).strength(strength).sound(sound)
                .noOcclusion().isRedstoneConductor((s, l, pos) -> false).isSuffocating((s, l, pos) -> false)
                .isViewBlocking((s, l, pos) -> false));
        AVRegistry.tab(AVRegistry.ITEMS.registerSimpleBlockItem(block));
        return block;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
