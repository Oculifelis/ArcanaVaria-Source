/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2023 Direwolf20-MC
 * Adapted from Building Gadgets 2 v26.1.2.3, commit 529c2962ffe0f5d2ef20125ccab9ad2426bc7a56.
 * The full MIT notice is in LICENSE-third-party.md.
 */
package com.arcanavaria.lens.bg2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import com.arcanavaria.lens.LensSettings;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;

public class Surface extends BaseMode {
    public Surface(boolean isExchanging) {
        super(isExchanging);
    }

    @Override
    public ArrayList<StatePos> collectWorld(Direction hitSide, Player player, BlockPos start, BlockState state) {
        LensSettings settings = this.settings;
        int range = settings.range();
        int bound = range / 2;
        Level level = player.level();

        ArrayList<StatePos> coordinates = new ArrayList<>();
        BlockState lookingAtState = level.getBlockState(start);
        BlockPos startAt = isExchanging ? start : start.relative(hitSide);
        AABB box = squareArea(startAt, hitSide, bound);
        BlockPos.betweenClosedStream(box).map(BlockPos::immutable).forEach(pos -> {
            if (isPosValid(level, player, pos, state) && isPosValidCustom(level, pos, lookingAtState, settings, hitSide))
                coordinates.add(new StatePos(state, pos.subtract(start)));
        });

        boolean connected = settings.connected();
        if (isExchanging && connected)
            return removeUnConnected(level, player, startAt.subtract(start), coordinates, hitSide);
        return coordinates;
    }

    public boolean isPosValidCustom(Level level, BlockPos pos, BlockState compareState, LensSettings settings, Direction hitSide) {
        if (isExchanging) return true; //Handled by isExchangingValid
        if (!level.isLoaded(pos.relative(hitSide.getOpposite()))) return false;
        boolean fuzzy = settings.fuzzy();
        BlockState belowState = level.getBlockState(pos.relative(hitSide.getOpposite()));
        if (fuzzy) {
            if (belowState.isAir()) return false;
        } else {
            if (!belowState.equals(compareState)) return false;
        }

        return true;
    }
}
