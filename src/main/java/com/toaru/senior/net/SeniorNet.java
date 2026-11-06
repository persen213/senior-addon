package com.toaru.senior.net;

import com.toaru.senior.SeniorMod;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** The add-on's own network channel (server to client only) and the helper used by the abilities to send effects. */
public final class SeniorNet {
    private SeniorNet() { }

    private static final String PROTOCOL = "1";
    /** Effects are only sent to players this close to the effect, in the same dimension. */
    private static final double SEND_RADIUS = 96.0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SeniorMod.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static final AtomicLong NEXT_ID = new AtomicLong(1);

    public static void register() {
        CHANNEL.registerMessage(0, FxPacket.class, FxPacket::encode, FxPacket::decode, FxPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static long nextId() { return NEXT_ID.getAndIncrement(); }

    /** Sends an effect to everyone near its first position. */
    public static void send(ServerLevel level, FxPacket packet) {
        Vec3 at = packet.a();
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(at.x, at.y, at.z, SEND_RADIUS, level.dimension())), packet);
    }
}
