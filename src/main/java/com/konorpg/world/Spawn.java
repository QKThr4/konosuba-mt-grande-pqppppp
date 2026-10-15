package com.konorpg.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

/** Safe spawning helpers using registered EntityType objects (never string ids). */
public final class Spawn {
    public static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, double x, double y, double z) {
        T e = type.create(level);
        if (e == null) return null;
        e.moveTo(x, y, z, level.random.nextFloat() * 360f, 0f);
        e.finalizeSpawn(level, level.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.EVENT, null);
        level.addFreshEntity(e);
        return e;
    }

    /** Surface position at a random distance/angle around (cx, cz). */
    public static BlockPos around(ServerLevel level, double cx, double cz, int min, int max, RandomSource rnd) {
        double ang = rnd.nextDouble() * Math.PI * 2;
        double dist = min + rnd.nextDouble() * (max - min);
        BlockPos p = new BlockPos((int) (cx + Math.cos(ang) * dist), 0, (int) (cz + Math.sin(ang) * dist));
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p);
    }

    private Spawn() {}
}
