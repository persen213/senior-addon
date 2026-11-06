package com.toaru.senior;

import io.github.pinchan4273.reacademycraft.api.mc.AbilityActor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

/** Aiming helpers. */
final class Targeting {
    private Targeting() { }

    /** The point you are aiming at within range: the first block or creature on the line of sight, or the far end of the line. */
    static Vec3 aimPoint(AbilityActor actor, double range) {
        Vec3 from = actor.position();
        Vec3 to = from.add(actor.look().scale(range));
        BlockHitResult block = actor.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor.entity()));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        Vec3 point = end;
        double bestDist = from.distanceToSqr(end);
        for (LivingEntity e : actor.level().getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(1.5),
                x -> x.isAlive() && !x.isSpectator() && x != actor.entity())) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(from, end);
            if (hit.isEmpty()) continue;
            double d = from.distanceToSqr(hit.get());
            if (d < bestDist) { bestDist = d; point = e.position(); }
        }
        return point;
    }
}
