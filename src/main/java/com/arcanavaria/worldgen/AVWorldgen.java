package com.arcanavaria.worldgen;

import static com.arcanavaria.ArcanaVaria.MODID;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The structure processors the Forsaken Laboratories and the Wizard Tower run (their data comes from Worldforge,
 * python -m worldforge.arcana): {@code arcanavaria:markers} first, then {@code arcanavaria:block_swap} for the tower's
 * palette and the shared block library.
 */
public final class AVWorldgen {
    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, MODID);
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<MarkerProcessor>> MARKERS =
            PROCESSORS.register("markers", () -> () -> MarkerProcessor.CODEC);
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<BlockSwap>> BLOCK_SWAP =
            PROCESSORS.register("block_swap", () -> () -> BlockSwap.CODEC);

    private AVWorldgen() {
    }

    public static void register(IEventBus bus) {
        MarkerProcessor.TYPE = MARKERS;
        BlockSwap.TYPE = BLOCK_SWAP;
        PROCESSORS.register(bus);
    }
}
