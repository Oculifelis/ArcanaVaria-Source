package com.arcanavaria.lab.walker;

/**
 * The Crucible Walker's moves. {@link #id()} is what the entity data syncs (fixed per move, never the ordinal),
 * {@link #length()} is the move's length in ticks. Every hit has a tell of at least 12 ticks before it lands.
 */
public enum WalkerAct {
    /** Walking, choosing the next move. */
    IDLE(0, 0),
    /** Boots up on the altar: invulnerable, gears spin up, the glow comes on. */
    START(1, 40),
    /** Claw drawn back for 14 ticks, then a cone in front. */
    SWIPE(2, 26),
    /** Front legs lift for 16 ticks, then a ground ring rolls out to 6 blocks: jump it. */
    STOMP(3, 30),
    /** Marks a spot under the target, an essentia glob arcs over and lands 28 ticks later. */
    LOB(4, 34),
    /** Overclocked: a glowing lane for 24 ticks, then it charges down it. */
    CHARGE(5, 38),
    /** Overclocked: the lens aims for 40 ticks (locking 14 ticks before it fires), then a 20-block ray. */
    RAY(6, 60),
    /** Phase two, end of the overclock: the vents flare for 20 ticks, then a steam ring rolls out to 9 blocks. */
    BURST(7, 34),
    /** A trait move: frozen for 16 ticks, gears stopped, lens and essentia in the trait's colour, then the move. */
    TELL(8, 16);

    private final int id;
    private final int length;

    WalkerAct(int id, int length) {
        this.id = id;
        this.length = length;
    }

    public int id() {
        return this.id;
    }

    public int length() {
        return this.length;
    }

    public static WalkerAct byId(int id) {
        for (WalkerAct a : values()) {
            if (a.id == id) {
                return a;
            }
        }
        return IDLE;
    }
}
