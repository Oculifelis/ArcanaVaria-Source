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
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;

public class VerticalWall extends BaseMode {
    public VerticalWall() {
        super(false);
    }

    @Override
    public ArrayList<StatePos> collectWorld(Direction hitSide, Player player, BlockPos start, BlockState state) {
        LensSettings settings = this.settings;
        int range = settings.range();
        int size = range / 2;
        ArrayList<StatePos> coordinates = new ArrayList<>();

        Direction.Axis side = hitSide.getAxis() == Direction.Axis.Y ? player.getDirection().getOpposite().getAxis() : hitSide.getAxis();

        int startY = hitSide.getAxis() == Direction.Axis.Y ? start.getY() + 1 : start.getY() - size;
        int endY = hitSide.getAxis() == Direction.Axis.Y ? start.getY() + ((range - 1) * (hitSide == Direction.DOWN ? -1 : 1)) + 1 : start.getY() + size;

        AABB box = new AABB(
                start.getX() - (side == Direction.Axis.Z ? size : 0), startY, start.getZ() - (side == Direction.Axis.X ? size : 0),
                start.getX() + (side == Direction.Axis.Z ? size : 0), endY, start.getZ() + (side == Direction.Axis.X ? size : 0)
        );
        BlockPos.betweenClosedStream(box).map(BlockPos::immutable).forEach(pos -> {
            if (isPosValid(player.level(), player, pos, state))
                coordinates.add(new StatePos(state, pos.subtract(start)));
        });
        return coordinates;
    }
}
