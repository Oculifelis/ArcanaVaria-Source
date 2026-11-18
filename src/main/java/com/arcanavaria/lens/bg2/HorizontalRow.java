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
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

public class HorizontalRow extends BaseMode {
    public HorizontalRow(boolean exchanging) {
        super(exchanging);
    }

    @Override
    public ArrayList<StatePos> collectWorld(Direction hitSide, Player player, BlockPos start, BlockState state) {
        LensSettings settings = this.settings;
        ArrayList<StatePos> coordinates = new ArrayList<>();
        int range = settings.range();
        boolean placeontop = settings.onTop();
        BlockPos startAt = placeontop ? start.above() : start;

        Direction side = hitSide.getAxis() == Direction.Axis.Y ? player.getDirection() : hitSide.getOpposite();
        if (!isExchanging) {
            for (int i = 0; i < range; i++)
                if (isPosValid(player.level(), player, startAt.relative(side, i), state))
                    coordinates.add(new StatePos(state, startAt.relative(side, i).subtract(start)));
        } else {
            side = side.getClockWise();
            int halfRange = range / 2;
            for (int i = -halfRange; i <= halfRange; i++)
                if (isPosValid(player.level(), player, startAt.relative(side, i), state))
                    coordinates.add(new StatePos(state, startAt.relative(side, i).subtract(start)));
        }
        boolean connected = settings.connected();
        if (isExchanging && connected)
            return removeUnConnected(player.level(), player, startAt.subtract(start), coordinates, hitSide);
        return coordinates;
    }
}
