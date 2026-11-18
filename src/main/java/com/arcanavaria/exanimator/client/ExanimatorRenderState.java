package com.arcanavaria.exanimator.client;

import com.arcanavaria.exanimator.ExanimatorState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class ExanimatorRenderState extends LivingEntityRenderState {
    public ExanimatorState state = ExanimatorState.IDLE;
    /** Ticks since the state began, with partial tick. */
    public float stateTime;
    public boolean phaseTwo;
    /** Ticks since death (0 while alive); the renderer zeroes vanilla's deathTime so the collapse is ours. */
    public float dyingTime;
    /** 0..1 brightness of the eyes, runes and chest rim. */
    public float glow;
}
