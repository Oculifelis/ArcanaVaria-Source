package com.arcanavaria.lab;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

/** Finds block entities of a kind in a box, looking only at chunks already loaded (never loads one). */
public final class LabScan {
    private LabScan() {
    }

    public static <T> List<T> in(Level level, AABB box, Class<T> kind) {
        List<T> out = new ArrayList<>();
        int x0 = SectionPos.blockToSectionCoord(box.minX), x1 = SectionPos.blockToSectionCoord(box.maxX);
        int z0 = SectionPos.blockToSectionCoord(box.minZ), z1 = SectionPos.blockToSectionCoord(box.maxZ);
        for (int cx = x0; cx <= x1; cx++) {
            for (int cz = z0; cz <= z1; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (kind.isInstance(be) && box.contains(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5)) {
                        out.add(kind.cast(be));
                    }
                }
            }
        }
        return out;
    }

    public static <T> List<T> near(Level level, BlockPos pos, int radius, Class<T> kind) {
        return in(level, new AABB(pos).inflate(radius), kind);
    }
}
