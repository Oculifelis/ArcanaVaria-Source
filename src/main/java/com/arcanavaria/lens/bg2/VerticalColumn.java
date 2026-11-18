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

public class VerticalColumn extends BaseMode {
    public VerticalColumn(boolean exchange) {
        super(exchange);
    }

    @Override
    public ArrayList<StatePos> collectWorld(Direction hitSide, Player player, BlockPos start, BlockState state) {
        LensSettings settings = this.settings;
        int range = settings.range();
        ArrayList<StatePos> coordinates = new ArrayList<>();

        // If up or down, full height from start block
        int halfRange = range / 2;

        if (hitSide.getAxis().equals(Direction.Axis.Y)) {
            // The exchanger handles the Y completely differently :sad: means more code
            if (isExchanging) {
                Direction playerFacing = player.getDirection();
                for (int i = -halfRange; i <= halfRange; i++)
                    if (isPosValid(player.level(), player, start.relative(playerFacing, i), state))
                        coordinates.add(new StatePos(state, BlockPos.ZERO.relative(playerFacing, i)));
            } else {
                for (int i = 1; i < range + 1; i++) {
                    if (isPosValid(player.level(), player, start.relative(hitSide, i), state))
                        coordinates.add(new StatePos(state, BlockPos.ZERO.relative(hitSide, i)));
                }
            }
            //Else, half and half
        } else {
            for (int i = -halfRange; i <= halfRange; i++) {
                if (isPosValid(player.level(), player, start.relative(Direction.UP, i), state))
                    coordinates.add(new StatePos(state, BlockPos.ZERO.relative(Direction.UP, i)));
            }
        }
        boolean connected = settings.connected();
        if (isExchanging && connected)
            return removeUnConnected(player.level(), player, start.subtract(start), coordinates, hitSide);
        return coordinates;
    }
}
