package com.arcanavaria.thurible;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

/** What a Thurible holds: one aspect and how much of it (none when empty). */
public record ThuribleContents(Optional<ResourceKey<IAspect>> aspect, int amount) {
    public static final ThuribleContents EMPTY = new ThuribleContents(Optional.empty(), 0);
    public static final Codec<ThuribleContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(IAspect.REGISTRY_KEY).optionalFieldOf("aspect").forGetter(ThuribleContents::aspect),
            Codec.INT.optionalFieldOf("amount", 0).forGetter(ThuribleContents::amount)).apply(i, ThuribleContents::new));
    public static final StreamCodec<ByteBuf, ThuribleContents> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ResourceKey.streamCodec(IAspect.REGISTRY_KEY)), ThuribleContents::aspect,
            ByteBufCodecs.VAR_INT, ThuribleContents::amount, ThuribleContents::new);

    public static ThuribleContents of(ResourceKey<IAspect> aspect, int amount) {
        return amount <= 0 ? EMPTY : new ThuribleContents(Optional.of(aspect), amount);
    }

    public boolean isEmpty() {
        return aspect.isEmpty() || amount <= 0;
    }

    public boolean accepts(ResourceKey<IAspect> other) {
        return isEmpty() || aspect.get().equals(other);
    }
}
