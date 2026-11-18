package com.arcanavaria.lens;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/** The Lens's shapes (both sides use them: the server builds, the client previews) and its symmetry copies. */
public final class LensGeometry {
    public static final int MAX = 4096;

    private LensGeometry() {
    }

    /** The blocks a shape mode covers between corners a and b, at most {@link #MAX}. */
    public static List<BlockPos> shape(LensMode mode, BlockPos a, BlockPos b) {
        return switch (mode) {
            case LINE -> line(a, b);
            case WALL -> wall(a, b);
            case FILL, REPLACE, COPY -> box(a, b);
            default -> List.of();
        };
    }

    public static List<BlockPos> box(BlockPos a, BlockPos b) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(a, b)) {
            out.add(p.immutable());
            if (out.size() >= MAX) {
                break;
            }
        }
        return out;
    }

    /** A 3D line, one block per step along its longest axis. */
    public static List<BlockPos> line(BlockPos a, BlockPos b) {
        int dx = b.getX() - a.getX(), dy = b.getY() - a.getY(), dz = b.getZ() - a.getZ();
        int n = Math.min(MAX - 1, Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))));
        Set<BlockPos> out = new LinkedHashSet<>();
        for (int i = 0; i <= n; i++) {
            double t = n == 0 ? 0 : (double) i / n;
            out.add(BlockPos.containing(a.getX() + 0.5 + dx * t, a.getY() + 0.5 + dy * t, a.getZ() + 0.5 + dz * t));
        }
        return new ArrayList<>(out);
    }

    /** An upright wall: the floor line from a to b, raised from the lower corner's height to the higher one's. */
    public static List<BlockPos> wall(BlockPos a, BlockPos b) {
        List<BlockPos> foot = line(a.atY(0), b.atY(0));
        int y0 = Math.min(a.getY(), b.getY()), y1 = Math.max(a.getY(), b.getY());
        List<BlockPos> out = new ArrayList<>();
        for (int y = y0; y <= y1 && out.size() < MAX; y++) {
            for (BlockPos p : foot) {
                out.add(p.atY(y));
            }
        }
        return out.size() > MAX ? out.subList(0, MAX) : out;
    }

    /** A copy of a placed block: where it goes and the state turned or mirrored to match. */
    public record Copy(BlockPos at, BlockState state) {
    }

    /** The other blocks a symmetry mode adds when one is placed at {@code p} (never p itself). */
    public static List<Copy> copies(LensMode mode, BlockPos anchor, BlockPos p, BlockState state) {
        int dx = p.getX() - anchor.getX(), dz = p.getZ() - anchor.getZ();
        List<Copy> out = new ArrayList<>();
        switch (mode) {
            case MIRROR_X -> out.add(new Copy(at(anchor, p, -dx, dz), state.mirror(Mirror.FRONT_BACK)));
            case MIRROR_Z -> out.add(new Copy(at(anchor, p, dx, -dz), state.mirror(Mirror.LEFT_RIGHT)));
            case MIRROR_XZ -> {
                out.add(new Copy(at(anchor, p, -dx, dz), state.mirror(Mirror.FRONT_BACK)));
                out.add(new Copy(at(anchor, p, dx, -dz), state.mirror(Mirror.LEFT_RIGHT)));
                out.add(new Copy(at(anchor, p, -dx, -dz), state.rotate(Rotation.CLOCKWISE_180)));
            }
            case RADIAL_4, RADIAL_8 -> {
                out.add(new Copy(at(anchor, p, -dz, dx), state.rotate(Rotation.CLOCKWISE_90)));
                out.add(new Copy(at(anchor, p, -dx, -dz), state.rotate(Rotation.CLOCKWISE_180)));
                out.add(new Copy(at(anchor, p, dz, -dx), state.rotate(Rotation.COUNTERCLOCKWISE_90)));
                if (mode == LensMode.RADIAL_8) {
                    BlockState m = state.mirror(Mirror.FRONT_BACK);
                    out.add(new Copy(at(anchor, p, -dx, dz), m));
                    out.add(new Copy(at(anchor, p, -dz, -dx), m.rotate(Rotation.CLOCKWISE_90)));
                    out.add(new Copy(at(anchor, p, dx, -dz), m.rotate(Rotation.CLOCKWISE_180)));
                    out.add(new Copy(at(anchor, p, dz, dx), m.rotate(Rotation.COUNTERCLOCKWISE_90)));
                }
            }
            default -> {
            }
        }
        out.removeIf(c -> c.at().equals(p));
        return out.stream().distinct().toList();
    }

    private static BlockPos at(BlockPos anchor, BlockPos p, int dx, int dz) {
        return new BlockPos(anchor.getX() + dx, p.getY(), anchor.getZ() + dz);
    }
}
