package com.toaru.senior.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;

/**
 * One running visual effect. It lives for a fixed number of game ticks, is drawn every frame in two passes (lines, then
 * glowing quads), and removes itself when finished. Subclasses only describe their geometry for a given age.
 */
abstract class FxInstance {
    final long id;
    final int duration;
    private final long startTick;

    FxInstance(long id, int duration) {
        this.id = id;
        this.duration = Math.max(1, duration);
        this.startTick = now();
    }

    static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }

    /** Age in ticks, with the fraction of the current tick for smooth animation. */
    float age(float partialTick) { return (now() - startTick) + partialTick; }

    boolean finished() { return now() - startTick >= duration; }

    /** A box that contains everything this effect draws (used to skip it when it is far away or off screen). */
    abstract AABB bounds();

    abstract void renderLines(VertexConsumer lines, PoseStack.Pose pose, float age);

    abstract void renderQuads(VertexConsumer quads, PoseStack.Pose pose, float age);
}
