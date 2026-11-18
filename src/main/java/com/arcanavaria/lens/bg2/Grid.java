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

public class Grid extends BaseMode {
    public Grid(boolean exchanging) {
        super(exchanging);
    }

    @Override
    public ArrayList<StatePos> collectWorld(Direction hitSide, Player player, BlockPos start, BlockState state) {
        LensSettings settings = this.settings;
        ArrayList<StatePos> coordinates = new ArrayList<>();
        int range = settings.range() + 1;
        boolean placeontop = settings.onTop();
        BlockPos startAt = placeontop ? start.above() : start;

        for (int x = range * -7 / 5; x <= range * 7 / 5; x++) {
            for (int z = range * -7 / 5; z <= range * 7 / 5; z++) {
                if (x % (((range - 2) % 6) + 2) != 0 || z % (((range - 2) % 6) + 2) != 0)
                    continue;
                BlockPos coord = new BlockPos(startAt.getX() + x, startAt.getY(), startAt.getZ() + z);
                if (isPosValid(player.level(), player, coord, state))
                    coordinates.add(new StatePos(state, coord.subtract(start)));
            }
        }

        return coordinates;
    }
}
