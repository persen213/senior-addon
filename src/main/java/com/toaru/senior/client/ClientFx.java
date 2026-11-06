package com.toaru.senior.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.toaru.senior.net.FxPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The client-side effects manager (client only). Receives effect packets, keeps the running effects, and draws them.
 * Performance rules: at most MAX_EFFECTS at once (the oldest is dropped), effects far away or outside the view are not
 * drawn, finished effects are removed every tick, and each frame costs exactly two draw batches however many effects run.
 */
public final class ClientFx {
    private ClientFx() { }

    private static final int MAX_EFFECTS = 32;
    private static final double MAX_DISTANCE = 128.0;
    private static final List<FxInstance> ACTIVE = new ArrayList<>();
    private static final List<FxInstance> VISIBLE = new ArrayList<>();

    /** Called on the client main thread when the server asks for an effect. */
    public static void receive(FxPacket p) {
        FxInstance fx;
        switch (p.type()) {
            case FxPacket.FOLD_COMPRESS -> fx = new FoldFx(p.id(), p.a(), false, p.duration());
            case FxPacket.FOLD_UNFOLD -> fx = new FoldFx(p.id(), p.a(), true, p.duration());
            case FxPacket.SHEAR -> fx = new ShearFx(p.id(), p.a(), p.b(), p.scale(), p.duration());
            default -> fx = null; // unknown type (should not happen): ignore
        }
        if (fx == null) return;
        if (ACTIVE.size() >= MAX_EFFECTS) ACTIVE.remove(0);
        ACTIVE.add(fx);
    }

    static void tick() { ACTIVE.removeIf(FxInstance::finished); }

    static void clear() { ACTIVE.clear(); }

    /** Draws all visible effects. pose has the camera rotation but not yet the camera position. */
    static void render(PoseStack pose, Vec3 camera, Frustum frustum, float partialTick) {
        if (ACTIVE.isEmpty()) return;
        VISIBLE.clear();
        double maxSq = MAX_DISTANCE * MAX_DISTANCE;
        for (FxInstance fx : ACTIVE) {
            AABB box = fx.bounds();
            if (box.getCenter().distanceToSqr(camera) > maxSq) continue;
            if (!frustum.isVisible(box)) continue;
            VISIBLE.add(fx);
        }
        if (VISIBLE.isEmpty()) return;

        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        PoseStack.Pose last = pose.last();

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (FxInstance fx : VISIBLE) fx.renderLines(lines, last, fx.age(partialTick));
        buffers.endBatch(RenderType.lines());

        VertexConsumer quads = buffers.getBuffer(RenderType.lightning());
        for (FxInstance fx : VISIBLE) fx.renderQuads(quads, last, fx.age(partialTick));
        buffers.endBatch(RenderType.lightning());

        pose.popPose();
    }
}
