package com.toaru.senior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Finding places where a creature can safely be put: solid floor, free space, no lava or fire, inside the world, loaded. */
final class SafeSpot {
    private SafeSpot() { }

    static boolean standable(ServerLevel level, Entity entity, double x, double y, double z) {
        BlockPos feet = BlockPos.containing(x, y, z);
        if (!level.isLoaded(feet) || !level.getWorldBorder().isWithinBounds(feet)) return false;
        if (y < level.getMinBuildHeight() + 1 || y > level.getMaxBuildHeight() - 2) return false;
        AABB box = entity.getDimensions(entity.getPose()).makeBoundingBox(x, y, z);
        if (!level.noCollision(entity, box)) return false;
        BlockPos below = feet.below();
        BlockState floor = level.getBlockState(below);
        if (!floor.isFaceSturdy(level, below, Direction.UP)) return false;
        if (floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CACTUS) || floor.is(Blocks.CAMPFIRE) || floor.is(Blocks.SOUL_CAMPFIRE)) return false;
        for (BlockPos p : new BlockPos[] {feet, feet.above()}) {
            if (level.getFluidState(p).is(FluidTags.LAVA)) return false;
            if (level.getBlockState(p).is(BlockTags.FIRE)) return false;
        }
        return true;
    }

    /** A standing spot at (x, z) within maxDy blocks of height y (nearest height first), or null. */
    static Vec3 groundAt(ServerLevel level, Entity entity, double x, double y, double z, int maxDy) {
        double base = Math.floor(y);
        for (int i = 0; i <= maxDy; i++) {
            if (standable(level, entity, x, base + i, z)) return new Vec3(x, base + i, z);
            if (i > 0 && standable(level, entity, x, base - i, z)) return new Vec3(x, base - i, z);
        }
        return null;
    }

    /** The nearest standing spot around a point, searching outward up to radius blocks, or null. */
    static Vec3 nearest(ServerLevel level, Entity entity, Vec3 around, int radius) {
        double cx = Math.floor(around.x) + 0.5;
        double cz = Math.floor(around.z) + 0.5;
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    Vec3 spot = groundAt(level, entity, cx + dx, around.y, cz + dz, 6);
                    if (spot != null) return spot;
                }
            }
        }
        return null;
    }
}
