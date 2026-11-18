package com.arcanavaria.lens;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

/** Settings are separate from the original selection component, preserving existing Lenses and scrolls. */
public record LensSettings(int range, boolean exchange, boolean fuzzy, boolean connected, boolean onTop,
        int turns, boolean mirror, int width, int height, int depth) {
    public static final LensSettings DEFAULT = new LensSettings(5, false, false, false, true, 0, false, 3, 3, 3);
    public static final Codec<LensSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 15).optionalFieldOf("range", 5).forGetter(LensSettings::range),
            Codec.BOOL.optionalFieldOf("exchange", false).forGetter(LensSettings::exchange),
            Codec.BOOL.optionalFieldOf("fuzzy", false).forGetter(LensSettings::fuzzy),
            Codec.BOOL.optionalFieldOf("connected", false).forGetter(LensSettings::connected),
            Codec.BOOL.optionalFieldOf("on_top", true).forGetter(LensSettings::onTop),
            Codec.intRange(0, 3).optionalFieldOf("turns", 0).forGetter(LensSettings::turns),
            Codec.BOOL.optionalFieldOf("mirror", false).forGetter(LensSettings::mirror),
            Codec.intRange(1, 15).optionalFieldOf("width", 3).forGetter(LensSettings::width),
            Codec.intRange(1, 15).optionalFieldOf("height", 3).forGetter(LensSettings::height),
            Codec.intRange(1, 15).optionalFieldOf("depth", 3).forGetter(LensSettings::depth)).apply(i, LensSettings::new));
    public static final StreamCodec<ByteBuf, LensSettings> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public LensSettings {
        range = Math.clamp(range, 1, 15);
        turns = Math.floorMod(turns, 4);
        width = Math.clamp(width, 1, 15);
        height = Math.clamp(height, 1, 15);
        depth = Math.clamp(depth, 1, 15);
    }

    public Rotation rotation() {
        return Rotation.values()[turns];
    }

    public Mirror reflection() {
        return mirror ? Mirror.FRONT_BACK : Mirror.NONE;
    }

    public int visCost() {
        return LensBuilder.COST * ((range + 4) / 5);
    }
}
