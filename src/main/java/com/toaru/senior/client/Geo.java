package com.toaru.senior.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Geometry toolbox for the visual effects: every shape is built from plain lines and flat quads, drawn by the game's own
 * renderer. Lines go into a "lines" consumer, quads into a "glow quads" consumer; never mix the two, and always pass
 * the consumer you were given (a frame has exactly one lines batch and one quads batch).
 * All coordinates are world coordinates (the camera offset is applied once, in ClientFx).
 */
final class Geo {
    private Geo() { }

    // Colour palette: purple, blue, near-white.
    static final float[] PURPLE = {0.62f, 0.22f, 1.0f};
    static final float[] BLUE = {0.25f, 0.5f, 1.0f};
    static final float[] WHITE = {0.95f, 0.92f, 1.0f};

    // ---------------- easing ----------------
    static float easeOut(float t) { float u = 1f - Mth.clamp(t, 0f, 1f); return 1f - u * u * u; }
    static float easeIn(float t) { float u = Mth.clamp(t, 0f, 1f); return u * u * u; }
    static float smooth(float edge0, float edge1, float x) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    /** A repeatable pseudo-random number in [-1, 1] from a seed and an index (used for jagged shapes). */
    static double jag(long seed, int i) {
        long h = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 31;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 29;
        return ((h & 0xFFFF) / 32767.5) - 1.0;
    }

    static Vec3 rotY(Vec3 v, double yaw) {
        double cos = Math.cos(yaw), sin = Math.sin(yaw);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    // ---------------- lines ----------------
    static void line(VertexConsumer c, PoseStack.Pose pose, Vec3 a, Vec3 b, float[] col, float alpha) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1.0E-6 || alpha <= 0.01f) return;
        Vec3 n = d.scale(1.0 / len);
        c.vertex(pose.pose(), (float) a.x, (float) a.y, (float) a.z).color(col[0], col[1], col[2], alpha)
                .normal(pose.normal(), (float) n.x, (float) n.y, (float) n.z).endVertex();
        c.vertex(pose.pose(), (float) b.x, (float) b.y, (float) b.z).color(col[0], col[1], col[2], alpha)
                .normal(pose.normal(), (float) n.x, (float) n.y, (float) n.z).endVertex();
    }

    static void polyline(VertexConsumer c, PoseStack.Pose pose, List<Vec3> points, float[] col, float alpha) {
        for (int i = 0; i + 1 < points.size(); i++) line(c, pose, points.get(i), points.get(i + 1), col, alpha);
    }

    /** A circle in the plane spanned by the unit vectors u and v. */
    static void circle(VertexConsumer c, PoseStack.Pose pose, Vec3 centre, Vec3 u, Vec3 v, double radius, int segments, float[] col, float alpha) {
        Vec3 previous = centre.add(u.scale(radius));
        for (int i = 1; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            Vec3 next = centre.add(u.scale(Math.cos(angle) * radius)).add(v.scale(Math.sin(angle) * radius));
            line(c, pose, previous, next, col, alpha);
            previous = next;
        }
    }

    /** A flat square grid whose lines wave up and down: space that looks bent. */
    static void warpedGrid(VertexConsumer c, PoseStack.Pose pose, Vec3 centre, double half, int divisions, double amplitude, double time, float[] col, float alpha) {
        int steps = divisions * 2;
        for (int i = 0; i <= divisions; i++) {
            double fixed = -half + 2 * half * i / divisions;
            Vec3 prevX = null, prevZ = null;
            for (int s = 0; s <= steps; s++) {
                double moving = -half + 2 * half * s / steps;
                Vec3 px = centre.add(fixed, wave(fixed, moving, amplitude, time), moving);   // line along z
                Vec3 pz = centre.add(moving, wave(moving, fixed, amplitude, time), fixed);   // line along x
                if (prevX != null) line(c, pose, prevX, px, col, alpha);
                if (prevZ != null) line(c, pose, prevZ, pz, col, alpha);
                prevX = px;
                prevZ = pz;
            }
        }
    }

    private static double wave(double x, double z, double amplitude, double time) {
        return amplitude * Math.sin(time * 0.6 + x * 2.3 + z * 1.9);
    }

    // ---------------- boxes (rotating structures) ----------------
    private static Vec3[] corners(Vec3 centre, double hx, double hy, double hz, double yaw) {
        double[][] s = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        Vec3[] out = new Vec3[8];
        for (int i = 0; i < 4; i++) {
            for (int k = 0; k < 2; k++) {
                double y = k == 0 ? -hy : hy;
                out[i + 4 * k] = centre.add(rotY(new Vec3(s[i][0] * hx, y, s[i][1] * hz), yaw));
            }
        }
        return out;
    }

    private static final int[][] BOX_EDGES = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
    private static final int[][] BOX_FACES = {{0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {1, 2, 6, 5}, {2, 3, 7, 6}, {3, 0, 4, 7}};

    /** The 12 edges of a box with half sizes hx, hy, hz, turned by yaw (radians) around its vertical axis. */
    static void boxLines(VertexConsumer c, PoseStack.Pose pose, Vec3 centre, double hx, double hy, double hz, double yaw, float[] col, float alpha) {
        Vec3[] v = corners(centre, hx, hy, hz, yaw);
        for (int[] e : BOX_EDGES) line(c, pose, v[e[0]], v[e[1]], col, alpha);
    }

    /** The 6 faces of the same box as translucent glowing quads. */
    static void boxFaces(VertexConsumer c, PoseStack.Pose pose, Vec3 centre, double hx, double hy, double hz, double yaw, float[] col, float alpha) {
        Vec3[] v = corners(centre, hx, hy, hz, yaw);
        for (int[] f : BOX_FACES) quad(c, pose, v[f[0]], v[f[1]], v[f[2]], v[f[3]], col, alpha);
    }

    // ---------------- quads ----------------
    /** A flat quad, drawn from both sides. */
    static void quad(VertexConsumer c, PoseStack.Pose pose, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, float[] col, float alpha) {
        if (alpha <= 0.01f) return;
        vertex(c, pose, p1, col, alpha); vertex(c, pose, p2, col, alpha); vertex(c, pose, p3, col, alpha); vertex(c, pose, p4, col, alpha);
        vertex(c, pose, p4, col, alpha); vertex(c, pose, p3, col, alpha); vertex(c, pose, p2, col, alpha); vertex(c, pose, p1, col, alpha);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, Vec3 p, float[] col, float alpha) {
        c.vertex(pose.pose(), (float) p.x, (float) p.y, (float) p.z).color(col[0], col[1], col[2], alpha).endVertex();
    }
}
