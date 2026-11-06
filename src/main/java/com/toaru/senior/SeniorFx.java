package com.toaru.senior;

import com.toaru.senior.net.FxPacket;
import com.toaru.senior.net.SeniorNet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Server side of the visual effects: decides which effect to show, then tells the clients. Nothing here draws anything. */
final class SeniorFx {
    private SeniorFx() { }

    static final int FOLD_TICKS = 8;
    static final int SHEAR_TICKS = 9;

    /** Foldstep: the shape compresses where you were and unfolds where you arrive. */
    static void fold(ServerLevel level, Vec3 origin, Vec3 destination) {
        Vec3 up = new Vec3(0, 0.9, 0);
        SeniorNet.send(level, new FxPacket(FxPacket.FOLD_COMPRESS, SeniorNet.nextId(), origin.add(up), Vec3.ZERO, 1f, FOLD_TICKS));
        SeniorNet.send(level, new FxPacket(FxPacket.FOLD_UNFOLD, SeniorNet.nextId(), destination.add(up), Vec3.ZERO, 1f, FOLD_TICKS));
        sound(level, origin, 1.5f);
        sound(level, destination, 1.2f);
    }

    /** Spatial Shear: a fracture travels from start to end. */
    static void shear(ServerLevel level, Vec3 start, Vec3 end) {
        SeniorNet.send(level, new FxPacket(FxPacket.SHEAR, SeniorNet.nextId(), start, end, 2.4f, SHEAR_TICKS));
        sound(level, start, 0.6f);
    }

    private static void sound(ServerLevel level, Vec3 at, float pitch) {
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, pitch);
    }
}
