package com.arcanavaria.exanimator;

/**
 * The Exanimator's moves. {@link #id()} is what the entity data syncs (fixed per move, never the ordinal),
 * {@link #serialName()} is what saves use, {@link #length()} is the move's length in ticks.
 */
public enum ExanimatorState {
    /** Rises out of the rite: invulnerable. */
    START(0, "start", 80),
    IDLE(1, "idle", 30),
    /** Overhead scythe chop in a cone in front; phase two follows with a backhand. */
    REAP(2, "reap", 34),
    /** Pulls everyone in, then a full-circle scythe sweep. */
    HARVEST(3, "harvest", 46),
    /** The back tentacles spear forward along marked lanes. */
    LASH(4, "lash", 36),
    /** Fades to smoke and steps out behind its target, then cuts. */
    STEP(5, "step", 40),
    /** The ribcage opens and the void in its chest drags everything toward it. */
    MAW(6, "maw", 70),
    /** Burned by light: reels, takes extra damage. */
    STUN(7, "stun", 50),
    /** Half health: snuffs the light around it and fights on in the dark. Invulnerable. */
    ECLIPSE(8, "eclipse", 60);

    private final int id;
    private final String serialName;
    private final int length;

    ExanimatorState(int id, String serialName, int length) {
        this.id = id;
        this.serialName = serialName;
        this.length = length;
    }

    public int id() {
        return id;
    }

    public String serialName() {
        return serialName;
    }

    public int length() {
        return length;
    }

    public static ExanimatorState byId(int id) {
        for (ExanimatorState s : values()) {
            if (s.id == id) {
                return s;
            }
        }
        return IDLE;
    }

    /** Unknown or missing names give {@code null}. */
    public static ExanimatorState byName(String name) {
        for (ExanimatorState s : values()) {
            if (s.serialName.equals(name)) {
                return s;
            }
        }
        return null;
    }
}
