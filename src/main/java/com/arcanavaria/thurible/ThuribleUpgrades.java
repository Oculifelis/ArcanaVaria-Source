package com.arcanavaria.thurible;

import com.arcanavaria.AVDataMaps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

/**
 * Blueprint upgrades fitted to a Thurible (found in the Forsaken Laboratories): a longer chain throws the cloud further,
 * a wider bowl makes it bigger, and the orbit ring lets it circle its bearer on its own ({@code orbiting} is the switch).
 */
public record ThuribleUpgrades(boolean chain, boolean cloud, boolean orbit, boolean orbiting) {
    public static final ThuribleUpgrades NONE = new ThuribleUpgrades(false, false, false, false);
    public static final Codec<ThuribleUpgrades> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("chain", false).forGetter(ThuribleUpgrades::chain),
            Codec.BOOL.optionalFieldOf("cloud", false).forGetter(ThuribleUpgrades::cloud),
            Codec.BOOL.optionalFieldOf("orbit", false).forGetter(ThuribleUpgrades::orbit),
            Codec.BOOL.optionalFieldOf("orbiting", false).forGetter(ThuribleUpgrades::orbiting)).apply(i, ThuribleUpgrades::new));
    public static final StreamCodec<ByteBuf, ThuribleUpgrades> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ThuribleUpgrades::chain, ByteBufCodecs.BOOL, ThuribleUpgrades::cloud,
            ByteBufCodecs.BOOL, ThuribleUpgrades::orbit, ByteBufCodecs.BOOL, ThuribleUpgrades::orbiting, ThuribleUpgrades::new);

    public ThuribleUpgrades with(ThuribleBlueprintItem.Kind kind) {
        return switch (kind) {
            case CHAIN -> new ThuribleUpgrades(true, cloud, orbit, orbiting);
            case CLOUD -> new ThuribleUpgrades(chain, true, orbit, orbiting);
            case ORBIT -> new ThuribleUpgrades(chain, cloud, true, orbiting);
        };
    }

    public boolean has(ThuribleBlueprintItem.Kind kind) {
        return switch (kind) {
            case CHAIN -> chain;
            case CLOUD -> cloud;
            case ORBIT -> orbit;
        };
    }

    public ThuribleUpgrades toggleOrbit() {
        return new ThuribleUpgrades(chain, cloud, orbit, orbit && !orbiting);
    }

    /** How far in front of its bearer the cloud lands (the arcanavaria:thurible data map). */
    public double reach(Item item) {
        AVDataMaps.Thurible t = AVDataMaps.Thurible.of(item);
        return chain ? t.chainReach() : t.reach();
    }

    public float radius(Item item) {
        AVDataMaps.Thurible t = AVDataMaps.Thurible.of(item);
        return cloud ? t.cloudRadius() : t.radius();
    }
}
