package com.arcanavaria.warden;

/**
 * The Greatwood Warden's moves (docs/WARDEN.md). {@link #id()} is what the entity data syncs to clients and is fixed per
 * move (never the ordinal); {@link #serialName()} is what saves use. {@link #length()} is the move's length in ticks
 * (SLAM runs 38 in phase two, see {@code GreatwoodWarden#length}).
 */
public enum WardenState {
    START(0, "start", 60),
    IDLE(1, "idle", 30),
    SWIPE(2, "swipe", 16),
    SLAM(3, "slam", 34),
    ROOTS(4, "roots", 44),
    VOLLEY(5, "volley", 30),
    RAY(6, "ray", 70),
    MEND(7, "mend", 80),
    STUN(8, "stun", 40),
    BURROW(9, "burrow", 50),
    ROAR(10, "roar", 50);

    private final int id;
    private final String serialName;
    private final int length;

    WardenState(int id, String serialName, int length) {
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

    public static WardenState byId(int id) {
        for (WardenState s : values()) {
            if (s.id == id) {
                return s;
            }
        }
        return IDLE;
    }

    /** Unknown or missing names give {@code null}. */
    public static WardenState byName(String name) {
        for (WardenState s : values()) {
            if (s.serialName.equals(name)) {
                return s;
            }
        }
        return null;
    }

    /** The one-time finale (eight root lanes below 20% health): not yet earned, earned and waiting, or performed. */
    public enum Finale {
        PENDING("pending"),
        QUEUED("queued"),
        DONE("done");

        private final String serialName;

        Finale(String serialName) {
            this.serialName = serialName;
        }

        public String serialName() {
            return serialName;
        }

        public static Finale byName(String name) {
            for (Finale f : values()) {
                if (f.serialName.equals(name)) {
                    return f;
                }
            }
            return PENDING;
        }
    }
}
