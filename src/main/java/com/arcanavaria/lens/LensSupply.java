package com.arcanavaria.lens;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A server-bound inventory face. It is checked again whenever resources are requested. */
public record LensSupply(GlobalPos location, Direction side) {
    public static final Codec<LensSupply> CODEC = RecordCodecBuilder.create(i -> i.group(
            GlobalPos.CODEC.fieldOf("location").forGetter(LensSupply::location),
            Direction.CODEC.fieldOf("side").forGetter(LensSupply::side)).apply(i, LensSupply::new));
    public static final StreamCodec<ByteBuf, LensSupply> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
