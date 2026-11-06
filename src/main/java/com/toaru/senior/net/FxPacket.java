package com.toaru.senior.net;

import com.toaru.senior.client.ClientFx;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Tells clients to draw one visual effect. The server decides everything that matters for gameplay; this packet only
 * carries what a client needs to draw: the kind of effect, where, how big and how long.
 */
public record FxPacket(int type, long id, Vec3 a, Vec3 b, float scale, int duration) {
    public static final int FOLD_COMPRESS = 1;
    public static final int FOLD_UNFOLD = 2;
    public static final int SHEAR = 3;

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(type);
        buf.writeLong(id);
        buf.writeDouble(a.x); buf.writeDouble(a.y); buf.writeDouble(a.z);
        buf.writeDouble(b.x); buf.writeDouble(b.y); buf.writeDouble(b.z);
        buf.writeFloat(scale);
        buf.writeVarInt(duration);
    }

    public static FxPacket decode(FriendlyByteBuf buf) {
        int type = buf.readVarInt();
        long id = buf.readLong();
        Vec3 a = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 b = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        float scale = buf.readFloat();
        int duration = buf.readVarInt();
        return new FxPacket(type, id, a, b, scale, duration);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFx.receive(this)));
        ctx.setPacketHandled(true);
    }
}
