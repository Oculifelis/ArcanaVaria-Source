package com.arcanavaria.lattice;

import net.minecraft.core.Direction;

/**
 * The six parts of the Alembic Lattice. Devices face one way ({@code facing} below); which faces each opens to and
 * takes packets in through follows from that.
 */
public enum LatticeKind {
    /** Glass pipe: carries packets straight on where it can, else round the bend; forks take turns. */
    PIPE(4),
    /** Sits against a jar or other essentia store ({@code facing} points at it) and draws one essentia per pulse. */
    INTAKE(4),
    /** Sits against a jar ({@code facing} points at it) and fills it; waits while the jar is full. */
    OUTLET(4),
    /** Two packets from different faces within the window combine into their compound, sent out {@code facing}. */
    JUNCTION(4),
    /** Takes a packet in at the back; a compound splits into its parents out left and right, a primal goes on ahead. */
    PRISM(4),
    /** Holds one packet; side-by-side synchronizers facing the same way let go only when every one of them holds one. */
    SYNCHRONIZER(1);

    public final int capacity;

    LatticeKind(int capacity) {
        this.capacity = capacity;
    }

    /** Whether a lattice neighbour on {@code side} connects here. */
    public boolean opensTo(Direction side, Direction facing) {
        return switch (this) {
            case PIPE, JUNCTION -> true;
            case INTAKE, OUTLET -> side != facing;
            case PRISM -> side.getAxis().isHorizontal();
            case SYNCHRONIZER -> side.getAxis() == facing.getAxis();
        };
    }

    /** Whether a packet may come in through {@code side}. */
    public boolean takesFrom(Direction side, Direction facing) {
        return switch (this) {
            case PIPE -> true;
            case INTAKE -> false;
            case OUTLET, JUNCTION -> side != facing;
            case PRISM, SYNCHRONIZER -> side == facing.getOpposite();
        };
    }

    /** A prism's left output, looking along its facing. */
    public static Direction left(Direction facing) {
        return facing.getAxis().isHorizontal() ? facing.getCounterClockWise() : Direction.WEST;
    }

    public static Direction right(Direction facing) {
        return facing.getAxis().isHorizontal() ? facing.getClockWise() : Direction.EAST;
    }
}
