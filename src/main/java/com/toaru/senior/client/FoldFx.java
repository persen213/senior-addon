package com.toaru.senior.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Foldstep. At the start position a person-sized box folds down into a small geometric shape above a warping patch of
 * space; at the destination the small shape unfolds back to person size inside an expanding ring.
 */
final class FoldFx extends FxInstance {
    private final Vec3 centre;
    private final boolean unfold;

    FoldFx(long id, Vec3 centre, boolean unfold, int duration) {
        super(id, duration);
        this.centre = centre;
        this.unfold = unfold;
    }

    @Override AABB bounds() { return new AABB(centre, centre).inflate(2.5); }

    private float progress(float age) { return Mth.clamp(age / duration, 0f, 1f); }

    /** 1 = person size, small = folded. */
    private float size(float t) {
        float f = unfold ? Geo.easeOut(t) : 1f - Geo.easeIn(t);
        return 0.14f + 0.86f * f;
    }

    private float alpha(float t) { return 1f - Geo.smooth(0.75f, 1f, t); }

    @Override void renderLines(VertexConsumer lines, PoseStack.Pose pose, float age) {
        float t = progress(age);
        float s = size(t);
        float a = alpha(t);
        double yaw = age * 0.4;
        Geo.boxLines(lines, pose, centre, 0.45 * s, 0.9 * s, 0.45 * s, yaw, Geo.PURPLE, a);
        Geo.boxLines(lines, pose, centre, 0.25 * s, 0.5 * s, 0.25 * s, -yaw * 1.6, Geo.BLUE, a * 0.9f);
        Vec3 feet = centre.add(0, -0.85, 0);
        if (!unfold) {
            // The place you left: a patch of space that waves and settles.
            Geo.warpedGrid(lines, pose, feet, 1.25, 6, 0.12 * (1f - t), age, Geo.BLUE, a * 0.8f);
        } else {
            // The place you arrive: a ring that opens outward.
            double radius = 0.2 + 1.3 * Geo.easeOut(t);
            Geo.circle(lines, pose, feet, new Vec3(1, 0, 0), new Vec3(0, 0, 1), radius, 24, Geo.WHITE, a * 0.8f);
            Geo.circle(lines, pose, feet, new Vec3(1, 0, 0), new Vec3(0, 0, 1), radius * 0.6, 20, Geo.BLUE, a * 0.7f);
        }
    }

    @Override void renderQuads(VertexConsumer quads, PoseStack.Pose pose, float age) {
        float t = progress(age);
        float s = size(t);
        Geo.boxFaces(quads, pose, centre, 0.45 * s, 0.9 * s, 0.45 * s, age * 0.4, Geo.PURPLE, 0.10f * alpha(t));
    }
}
