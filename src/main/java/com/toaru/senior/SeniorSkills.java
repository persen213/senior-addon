package com.toaru.senior;

import io.github.pinchan4273.reacademycraft.api.ActivationOutcome;
import io.github.pinchan4273.reacademycraft.api.mc.AbilityActor;
import io.github.pinchan4273.reacademycraft.api.mc.AcademySafety;
import io.github.pinchan4273.reacademycraft.api.mc.ActivationContext;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The Senior skills. All damage and movement happens here, on the server. Clients only draw what the server tells them. */
final class SeniorSkills {
    private SeniorSkills() { }

    private static ActivationOutcome refuse(ServerPlayer user, String messageKey) {
        user.displayClientMessage(Component.translatable(messageKey), true);
        return ActivationOutcome.REFUSED_INVALID;
    }

    // ============================ 1. Foldstep ============================
    static ActivationOutcome foldstep(ActivationContext c) {
        ServerPlayer user = c.naturalUser();
        if (user == null) return ActivationOutcome.REFUSED_INVALID;
        AbilityActor actor = c.actor();
        ServerLevel level = actor.level();

        Vec3 destination = chooseFoldDestination(actor, user, SeniorConfig.FOLD_RANGE.get(), user.isShiftKeyDown());
        if (destination == null) return refuse(user, "senior.message.no_destination");
        // Ask the safety service first (protected areas, other mods' events): nothing is paid if it says no.
        Vec3 allowed = AcademySafety.checkTeleport(actor, user, destination, Ids.FOLDSTEP);
        if (allowed == null) return refuse(user, "senior.message.refused_place");
        if (!c.pay(SeniorConfig.f(SeniorConfig.FOLD_CP), SeniorConfig.f(SeniorConfig.FOLD_OVERLOAD))) return ActivationOutcome.REFUSED_RESOURCE;

        Vec3 origin = user.position();
        if (!AcademySafety.teleport(actor, user, allowed, Ids.FOLDSTEP)) return refuse(user, "senior.message.refused_place");
        SeniorFx.fold(level, origin, allowed);
        c.addProficiency(0.008f);
        c.setCooldown(SeniorConfig.ticks(SeniorConfig.FOLD_COOLDOWN_SECONDS));
        return ActivationOutcome.PERFORMED;
    }

    /**
     * Where to land. Normally the standing spot nearest to the point you aim at. If that fails, or if you hold sneak
     * ("fold through walls"), the farthest free standing spot along your line of sight within range.
     */
    private static Vec3 chooseFoldDestination(AbilityActor actor, ServerPlayer user, double range, boolean throughObstacles) {
        ServerLevel level = actor.level();
        Vec3 eye = actor.position();
        Vec3 look = actor.look();
        if (!throughObstacles) {
            Vec3 aim = Targeting.aimPoint(actor, range);
            Vec3 nearAim = aim.subtract(look.scale(0.5)); // step back from the surface that was hit
            Vec3 spot = SafeSpot.nearest(level, user, nearAim, 2);
            if (spot != null && spot.distanceTo(eye) <= range + 1.5) return spot;
        }
        for (double d = range; d >= 2.0; d -= 1.0) {
            Vec3 p = eye.add(look.scale(d));
            Vec3 spot = SafeSpot.groundAt(level, user, Math.floor(p.x) + 0.5, p.y - 1.0, Math.floor(p.z) + 0.5, 3);
            if (spot != null) return spot;
        }
        return null;
    }

    // ============================ 2. Spatial Shear ============================
    static ActivationOutcome spatialShear(ActivationContext c) {
        ServerPlayer user = c.naturalUser();
        if (user == null) return ActivationOutcome.REFUSED_INVALID;
        AbilityActor actor = c.actor();
        ServerLevel level = actor.level();

        Vec3 from = actor.position();
        Vec3 look = actor.look();
        Vec3 to = from.add(look.scale(SeniorConfig.SHEAR_RANGE.get()));
        // Every creature whose body the fracture touches. Blocks are ignored: it passes through obstacles.
        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1.5), e -> {
            if (!e.isAlive() || e.isSpectator() || e == user || e instanceof ArmorStand) return false;
            AABB box = e.getBoundingBox().inflate(0.6);
            return box.contains(from) || box.clip(from, to).isPresent();
        });
        if (!c.pay(SeniorConfig.f(SeniorConfig.SHEAR_CP), SeniorConfig.f(SeniorConfig.SHEAR_OVERLOAD))) return ActivationOutcome.REFUSED_RESOURCE;

        float damage = SeniorConfig.f(SeniorConfig.SHEAR_HEARTS) * 2f;
        for (LivingEntity target : hit) { // each creature is hit once; the safety service decides if it may be hurt
            AcademySafety.attack(actor, target, Ids.SHEAR, damage);
        }
        Vec3 start = from.add(look.scale(0.6)).add(0, -0.25, 0);
        SeniorFx.shear(level, start, to);
        c.addProficiency(0.008f);
        c.setCooldown(SeniorConfig.ticks(SeniorConfig.SHEAR_COOLDOWN_SECONDS));
        return ActivationOutcome.PERFORMED;
    }
}
