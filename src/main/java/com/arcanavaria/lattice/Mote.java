package com.arcanavaria.lattice;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

/**
 * One mote: one essentia travelling the lattice. It crosses a block in {@link #TRAVEL} ticks: in from a face to the centre, where
 * the block decides what happens to it, then out to the face it was sent to. Fixed speed is what makes timing a puzzle:
 * the length of a path is its delay.
 */
public final class Mote {
    public static final int TRAVEL = 10;
    public static final int CENTRE = TRAVEL / 2;

    public static final Codec<Mote> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(IAspect.REGISTRY_KEY).fieldOf("aspect").forGetter(p -> p.aspect),
            Direction.CODEC.optionalFieldOf("from").forGetter(p -> Optional.ofNullable(p.from)),
            Direction.CODEC.optionalFieldOf("to").forGetter(p -> Optional.ofNullable(p.to)),
            Codec.INT.fieldOf("age").forGetter(p -> p.age),
            Codec.INT.optionalFieldOf("held", 0).forGetter(p -> p.held)
    ).apply(i, (a, f, t, age, held) -> {
        Mote p = new Mote(a, f.orElse(null), age);
        p.to = t.orElse(null);
        p.held = held;
        return p;
    }));

    public final ResourceKey<IAspect> aspect;
    /** The face it came in through; null when it was made at this block's centre. */
    public final @Nullable Direction from;
    /** The face it leaves through, once the block has decided; null while it waits at the centre. */
    public @Nullable Direction to;
    /** Ticks in this block, 0 .. TRAVEL. */
    public int age;
    /** Ticks it has waited at the centre. */
    public int held;
    /** Game time it last moved, so a packet handed on is not moved again by the next block in the same tick. */
    transient long movedAt = Long.MIN_VALUE;

    public Mote(ResourceKey<IAspect> aspect, @Nullable Direction from, int age) {
        this.aspect = aspect;
        this.from = from;
        this.age = age;
    }

    /** A packet made at a block's centre and already sent on. */
    static Mote born(ResourceKey<IAspect> aspect, Direction to, long now) {
        Mote p = new Mote(aspect, null, CENTRE);
        p.to = to;
        p.movedAt = now;
        return p;
    }

    public boolean waiting() {
        return age >= CENTRE && to == null;
    }
}
