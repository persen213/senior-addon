package com.toaru.senior.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Spatial Shear. A thin, angular fracture (a jagged line in the plane across the direction of travel, with short cracks
 * and a faint glowing gap) races from the start to the end, leaving fading echoes behind it, then closes.
 */
final class ShearFx extends FxInstance {
    private static final int TRAVEL_TICKS = 5;
    private static final int POINTS = 10;

    private final Vec3 from;
    private final Vec3 to;
    private final Vec3 dir;
    private final double length;
    private final float size;
    private final Vec3 side;
    private final Vec3 vert;

    ShearFx(long id, Vec3 from, Vec3 to, float size, int duration) {
        super(id, duration);
        this.from = from;
        this.to = to;
        this.size = size;
        Vec3 d = to.subtract(from);
        this.length = d.length();
        this.dir = length > 1.0E-6 ? d.scale(1.0 / length) : new Vec3(0, 0, 1);
        Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        this.side = dir.cross(up).normalize();
        this.vert = side.cross(dir).normalize();
    }

    @Override AABB bounds() { return new AABB(from, to).inflate(2.0 + size); }

    /** How open the fracture still is: 1 while travelling, then shrinking to 0. */
    private float openness(float age) {
        if (age <= TRAVEL_TICKS) return 1f;
        return 1f - Mth.clamp((age - TRAVEL_TICKS) / Math.max(1, duration - TRAVEL_TICKS), 0f, 1f);
    }

    private Vec3 head(float age) {
        float prog = Geo.easeOut(Mth.clamp(age / TRAVEL_TICKS, 0f, 1f));
        return from.add(dir.scale(length * prog));
    }

    private static final int ECHOES = 4;

    /** The jagged points of one copy of the fracture. echo 0 is the head, higher echoes trail behind and are smaller. */
    private List<Vec3> fracture(Vec3 at, double half, int echo) {
        List<Vec3> pts = new ArrayList<>();
        for (int i = 0; i <= POINTS; i++) {
            double taper = Math.sin(Math.PI * i / POINTS);                    // sharp ends
            double v = -half + 2 * half * i / POINTS;
            double sOff = Geo.jag(id + echo, i) * 0.22 * (size / 2.4) * taper;
            double dOff = Geo.jag(id + echo + 17, i) * 0.18 * taper;
            pts.add(at.add(vert.scale(v)).add(side.scale(sOff)).add(dir.scale(dOff)));
        }
        return pts;
    }

    @Override void renderLines(VertexConsumer lines, PoseStack.Pose pose, float age) {
        float open = openness(age);
        Vec3 head = head(age);
        for (int k = 0; k < ECHOES; k++) {
            Vec3 at = head.subtract(dir.scale(k * 0.7));
            if (at.subtract(from).dot(dir) < 0) continue;                     // do not draw before the start
            double half = size * 0.5 * (1.0 - 0.12 * k) * open;
            float alpha = (1f - 0.25f * k) * open;
            List<Vec3> pts = fracture(at, half, k);
            Geo.polyline(lines, pose, pts, Geo.PURPLE, alpha);
            if (k == 0) {
                // bright core
                List<Vec3> core = new ArrayList<>();
                for (Vec3 p : pts) core.add(p.add(side.scale(0.04)));
                Geo.polyline(lines, pose, core, Geo.WHITE, alpha * 0.9f);
                // short side cracks
                for (int i : new int[] {3, 7}) {
                    Vec3 p = pts.get(i);
                    Vec3 branch = side.scale(Geo.jag(id, i + 40)).add(vert.scale(Geo.jag(id, i + 50) * 0.6)).add(dir.scale(Geo.jag(id, i + 60) * 0.5));
                    if (branch.lengthSqr() > 1.0E-6) Geo.line(lines, pose, p, p.add(branch.normalize().scale(0.45 * (size / 2.4) * open)), Geo.BLUE, alpha);
                }
                // bent space around the head: two rings across the direction of travel
                Geo.circle(lines, pose, at, side, vert, size * 0.30 * open, 20, Geo.BLUE, alpha * 0.6f);
                Geo.circle(lines, pose, at, side, vert, size * 0.45 * open, 24, Geo.PURPLE, alpha * 0.4f);
            }
        }
    }

    @Override void renderQuads(VertexConsumer quads, PoseStack.Pose pose, float age) {
        float open = openness(age);
        Vec3 head = head(age);
        for (int k = 0; k < 2; k++) {                                         // the glowing gap: head and first echo
            Vec3 at = head.subtract(dir.scale(k * 0.7));
            if (at.subtract(from).dot(dir) < 0) continue;
            List<Vec3> pts = fracture(at, size * 0.5 * (1.0 - 0.12 * k) * open, k);
            Vec3 w = side.scale(0.12);
            for (int i = 0; i + 1 < pts.size(); i++) {
                Geo.quad(quads, pose, pts.get(i).subtract(w), pts.get(i).add(w), pts.get(i + 1).add(w), pts.get(i + 1).subtract(w),
                        Geo.PURPLE, 0.18f * open * (1f - 0.4f * k));
            }
        }
    }
}
