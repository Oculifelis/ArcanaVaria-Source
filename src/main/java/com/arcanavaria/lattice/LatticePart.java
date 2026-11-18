package com.arcanavaria.lattice;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** A block of the Alembic Lattice. */
public interface LatticePart {
    LatticeKind kind();

    /** A device's facing; pipes have none and answer north. */
    static Direction facing(BlockState state) {
        return state.hasProperty(BlockStateProperties.FACING) ? state.getValue(BlockStateProperties.FACING) : Direction.NORTH;
    }
}
