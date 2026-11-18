package com.arcanavaria.lens;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The Lens's mode, its symmetry anchor and its two selection corners, kept on the item so the client can preview. */
public record LensState(LensMode mode, Optional<BlockPos> anchor, Optional<BlockPos> a, Optional<BlockPos> b) {
    public static final LensState DEFAULT = new LensState(LensMode.MIRROR_X, Optional.empty(), Optional.empty(), Optional.empty());
    public static final Codec<LensState> CODEC = RecordCodecBuilder.create(i -> i.group(
            LensMode.CODEC.optionalFieldOf("mode", LensMode.MIRROR_X).forGetter(LensState::mode),
            BlockPos.CODEC.optionalFieldOf("anchor").forGetter(LensState::anchor),
            BlockPos.CODEC.optionalFieldOf("a").forGetter(LensState::a),
            BlockPos.CODEC.optionalFieldOf("b").forGetter(LensState::b)).apply(i, LensState::new));
    public static final StreamCodec<ByteBuf, LensState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> LensMode.values()[i], LensMode::ordinal), LensState::mode,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), LensState::anchor,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), LensState::a,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), LensState::b, LensState::new);

    public LensState withMode(LensMode m) {
        return new LensState(m, anchor, Optional.empty(), Optional.empty());
    }

    public LensState withAnchor(BlockPos p) {
        return new LensState(mode, Optional.of(p), a, b);
    }

    /** First click sets corner A, the second corner B; a click with both set starts over at A. */
    public LensState click(BlockPos p) {
        if (a.isEmpty() || b.isPresent() || mode == LensMode.PASTE) {
            return new LensState(mode, anchor, Optional.of(p), Optional.empty());
        }
        return new LensState(mode, anchor, a, Optional.of(p));
    }

    public LensState cleared() {
        return new LensState(mode, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public boolean ready() {
        return mode == LensMode.PASTE ? a.isPresent() : a.isPresent() && b.isPresent();
    }
}
