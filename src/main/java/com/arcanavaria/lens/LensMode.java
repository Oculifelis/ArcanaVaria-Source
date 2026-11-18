package com.arcanavaria.lens;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * What the Architect's Lens does. The mirror and radial modes copy every block its bearer places about an anchor; the
 * shape modes fill the space between two corners with the block in the other hand; copy and paste go through a
 * Blueprint Scroll.
 */
public enum LensMode implements StringRepresentable {
    MIRROR_X("mirror_x", true), MIRROR_Z("mirror_z", true), MIRROR_XZ("mirror_xz", true), RADIAL_4("radial_4", true),
    RADIAL_8("radial_8", true), LINE("line", false), WALL("wall", false), FILL("fill", false), REPLACE("replace", false),
    COPY("copy", false), PASTE("paste", false),
    SURFACE("surface", false), COLUMN("column", false), ROW("row", false), GRID("grid", false),
    STAIRS("stairs", false), VERTICAL_WALL("vertical_wall", false), HORIZONTAL_WALL("horizontal_wall", false),
    DESTROY("destroy", false), BIND("bind", false);

    public static final Codec<LensMode> CODEC = StringRepresentable.fromEnum(LensMode::values);
    private final String name;
    private final boolean symmetry;

    LensMode(String name, boolean symmetry) {
        this.name = name;
        this.symmetry = symmetry;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** Copies placed blocks about an anchor (rather than working on a two-corner selection). */
    public boolean symmetry() {
        return symmetry;
    }

    public boolean gadgetShape() {
        return ordinal() >= SURFACE.ordinal() && ordinal() <= HORIZONTAL_WALL.ordinal();
    }

    public LensMode step(int delta) {
        LensMode[] all = values();
        return all[Math.floorMod(ordinal() + delta, all.length)];
    }
}
