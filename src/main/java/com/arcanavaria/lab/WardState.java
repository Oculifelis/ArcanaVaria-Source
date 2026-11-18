package com.arcanavaria.lab;

import net.minecraft.util.StringRepresentable;

/** Dormant until a player walks in, sealed until the guardian dies, then cleared for good. */
public enum WardState implements StringRepresentable {
    DORMANT, SEALED, CLEARED;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
