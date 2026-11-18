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

public class HorizontalWall extends BaseMode {
    public HorizontalWall() {
        super(false);
    }

    @Override
    public ArrayList<StatePos> collectWorld(Direction hitSide, Player player, BlockPos start, BlockState state) {
        LensSettings settings = this.settings;
        int range = settings.range();
        int halfRange = range / 2;
        ArrayList<StatePos> coordinates = new ArrayList<>();
        boolean placeontop = settings.onTop();

        if (hitSide.getAxis() == Direction.Axis.Y) {
            for (int i = -halfRange; i <= halfRange; i++) {
                for (int j = -halfRange; j <= halfRange; j++) {
                    BlockPos coord = new BlockPos(start.getX() - i, start.getY() + (placeontop ? 1 : 0), start.getZ() + j);
                    if (isPosValid(player.level(), player, coord, state))
                        coordinates.add(new StatePos(state, coord.subtract(start)));
                }
            }

            return coordinates;
        }

        // Draw complete column then expand by half the range on both sides :D
        Direction.Axis axis = hitSide.getAxis();
        for (int i = 1; i < range + 1; i++) {
            for (int j = -halfRange; j <= halfRange; j++) {
                int value = (hitSide.getAxisDirection() == Direction.AxisDirection.POSITIVE ? i : -i);
                if (isPosValid(player.level(), player, start.relative(hitSide, i), state)) {
                    BlockPos coord =
                            axis == Direction.Axis.X
                                    ? new BlockPos(start.getX() + value, start.getY(), start.getZ() + j)
                                    : new BlockPos(start.getX() + j, start.getY(), start.getZ() + value);
                    if (isPosValid(player.level(), player, coord, state))
                        coordinates.add(new StatePos(state, coord.subtract(start)));
                }
            }
        }
        return coordinates;
    }
}
