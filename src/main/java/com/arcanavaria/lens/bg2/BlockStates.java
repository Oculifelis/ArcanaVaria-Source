/* SPDX-License-Identifier: MIT
 * Copyright (c) 2023 Direwolf20-MC
 * Adapted from Building Gadgets 2 v26.1.2.3 GadgetUtils.cleanBlockState.
 * Full notice in LICENSE-third-party.md.
 */
package com.arcanavaria.lens.bg2;

import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

public final class BlockStates {
    private BlockStates() {}

    private static final java.util.List<Property<?>> DENY_PROPERTIES = java.util.List.of(
            BlockStateProperties.AGE_1, BlockStateProperties.AGE_2, BlockStateProperties.AGE_3, BlockStateProperties.AGE_4,
            BlockStateProperties.AGE_5, BlockStateProperties.AGE_7, BlockStateProperties.AGE_15, BlockStateProperties.AGE_25,
            DoublePlantBlock.HALF, BlockStateProperties.WATERLOGGED, BlockStateProperties.LIT, BlockStateProperties.HAS_RECORD,
            BlockStateProperties.HAS_BOOK, BlockStateProperties.OPEN, BlockStateProperties.STAGE
    );

    public static BlockState cleanBlockState(BlockState sourceState) {
        BlockState placeState = sourceState.getBlock().defaultBlockState();
        for (Property<?> prop : sourceState.getProperties()) {
            if (!DENY_PROPERTIES.contains(prop)) {
                placeState = applyProperty(placeState, sourceState, prop);
            }
        }
        return placeState;
    }

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, BlockState from, Property<T> prop) {
        return state.setValue(prop, from.getValue(prop));
    }

}
