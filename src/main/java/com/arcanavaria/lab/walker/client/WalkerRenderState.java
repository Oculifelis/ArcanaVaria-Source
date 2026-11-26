package com.arcanavaria.lab.walker.client;

import com.arcanavaria.lab.walker.WalkerAct;
import com.arcanavaria.lab.walker.WalkerMode;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class WalkerRenderState extends LivingEntityRenderState {
    public WalkerAct act = WalkerAct.IDLE;
    /** Ticks since the move began, with partial tick. */
    public float actTime;
    public WalkerMode mode = WalkerMode.NORMAL;
    public float modeTime;
    public boolean phaseTwo;
    /** Ticks since death (0 while alive); vanilla's tip-over is replaced by a slump. */
    public float dyingTime;
    /** Gear loops so far, with partial tick. */
    public float gears;
    /** Glow colour (RGB) and brightness 0..1. */
    public int glowColour;
    public float glow;
}
