package com.arcanavaria.warden.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class GreatwoodWardenRenderState extends LivingEntityRenderState {
    public com.arcanavaria.warden.WardenState state = com.arcanavaria.warden.WardenState.IDLE;
    /** Ticks since the state began, with partial tick. */
    public float stateTime;
    public boolean phaseTwo;
    /** Ticks since death (0 while alive); the renderer zeroes vanilla's deathTime so the collapse is ours. */
    public float dyingTime;
    /** 0..1 brightness of the heart, eyes and crystals. */
    public float glow;
}
