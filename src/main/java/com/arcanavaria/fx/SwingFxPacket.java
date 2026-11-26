package com.arcanavaria.fx;

import com.arcanavaria.ArcanaVaria;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Tells nearby clients to draw a scythe sweep or an Elemental Whip jump (client/SwingFx). Kind 0 is a sweep: {@code a} is
 * the centre at chest height, {@code yaw} the facing and {@code points} the enemies hit. Kind 1 is a whip jump from
 * {@code a} to {@code points[0]}. The looks come from the review rounds (scythe look 2, whip look 0 for now).
 */
public record SwingFxPacket(int kind, int look, int primal, Vec3 a, float yaw, List<Vec3> points) implements CustomPacketPayload {
    public static final int SCYTHE_LOOK = 2;
    public static final int WHIP_LOOK = 0;

    public static final Type<SwingFxPacket> TYPE = new Type<>(ArcanaVaria.id("swing_fx"));
    public static final StreamCodec<FriendlyByteBuf, SwingFxPacket> STREAM_CODEC = StreamCodec.of(SwingFxPacket::write, SwingFxPacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buf, SwingFxPacket p) {
        buf.writeByte(p.kind);
        buf.writeByte(p.look);
        buf.writeByte(p.primal);
        vec(buf, p.a);
        buf.writeFloat(p.yaw);
        int n = Math.min(16, p.points.size());
        buf.writeByte(n);
        for (int i = 0; i < n; i++) {
            vec(buf, p.points.get(i));
        }
    }

    private static SwingFxPacket read(FriendlyByteBuf buf) {
        int kind = buf.readByte(), look = buf.readByte(), primal = buf.readByte();
        Vec3 a = vec(buf);
        float yaw = buf.readFloat();
        int n = buf.readByte();
        List<Vec3> points = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            points.add(vec(buf));
        }
        return new SwingFxPacket(kind, look, Math.floorMod(primal, 6), a, yaw, points);
    }

    private static void vec(FriendlyByteBuf buf, Vec3 v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    private static Vec3 vec(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    /** A sweep by {@code source} (sent to everyone tracking it). */
    public static void sweep(Entity source, int primal, Vec3 centre, float yaw, List<Vec3> hits) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(source, new SwingFxPacket(0, SCYTHE_LOOK, primal, centre, yaw, hits));
    }

    /** A whip jump seen around {@code source}. */
    public static void jump(Entity source, int primal, Vec3 from, Vec3 to) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(source, new SwingFxPacket(1, WHIP_LOOK, primal, from, 0F, List.of(to)));
    }

    public static void register(IEventBus bus) {
        bus.addListener(SwingFxPacket::payloads);
    }

    private static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, STREAM_CODEC, (p, context) -> context.enqueueWork(() -> ClientHook.play(p)));
    }

    /** Kept apart so a dedicated server never loads a client class. */
    private static final class ClientHook {
        static void play(SwingFxPacket p) {
            com.arcanavaria.client.SwingFx.play(p);
        }
    }
}
