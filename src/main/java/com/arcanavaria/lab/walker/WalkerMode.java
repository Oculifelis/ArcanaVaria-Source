package com.arcanavaria.lab.walker;

/** The three beats of the Crucible Walker's loop, in order: walk, overclock, stall, then round again. */
public enum WalkerMode {
    /** 10 s: walks and uses swipe, stomp and lobs. */
    NORMAL,
    /** 6 s (8 s in phase two): vents glow, gears race, 1.5x speed; charges and lens rays. */
    OVERCLOCK,
    /** 4 s: steam pours out, the lens goes dark, the cage opens, hits do double damage. */
    STALL;

    public static WalkerMode byId(int id) {
        WalkerMode[] v = values();
        return id >= 0 && id < v.length ? v[id] : NORMAL;
    }
}
