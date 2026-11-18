package com.arcanavaria.lab;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;

/** On a player sealed inside a ward: which ward core holds them. Kept through death, so the respawn knows the exit. */
public record WardLink(GlobalPos core) {
    public static final MapCodec<WardLink> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GlobalPos.CODEC.fieldOf("core").forGetter(WardLink::core)).apply(i, WardLink::new));
}
