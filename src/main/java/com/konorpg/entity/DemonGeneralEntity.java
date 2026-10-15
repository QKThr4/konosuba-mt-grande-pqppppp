package com.konorpg.entity;

import com.konorpg.Config;
import com.konorpg.registry.ModEntities;
import com.konorpg.world.Spawn;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Demon General (boss): fire bolts, short teleports at half health and summons demons when nearly dead. */
public class DemonGeneralEntity extends RpgHumanoidMonster {
    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.konorpg.demon_general"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private int boltCd = 80, tpCd = 200;
    private boolean summoned;

    public DemonGeneralEntity(EntityType<? extends DemonGeneralEntity> type, Level level) {
        super(type, level, "demon_general", 1.55f);
        this.xpReward = 80;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attrs() { return base(480, 12, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.9); }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData r = super.finalizeSpawn(level, diff, type, data);
        setHealth(getMaxHealth());
        return r;
    }

    @Override protected net.minecraft.world.item.Item weapon() { return net.minecraft.world.item.Items.NETHERITE_SWORD; }

    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }

    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bar.setProgress(getHealth() / getMaxHealth());
        LivingEntity t = getTarget();
        float frac = getHealth() / getMaxHealth();
        if (t == null || !(level() instanceof ServerLevel sl)) return;
        if (--boltCd <= 0 && hasLineOfSight(t) && distanceToSqr(t) < 26 * 26) {
            boltCd = frac < 0.33f ? 45 : 80;
            Vec3 from = getEyePosition();
            Vec3 to = t.getEyePosition();
            Vec3 step = to.subtract(from).normalize().scale(0.8);
            Vec3 cur = from;
            for (double i = 0; i < from.distanceTo(to); i += 0.8) { sl.sendParticles(ParticleTypes.FLAME, cur.x, cur.y, cur.z, 1, 0.02, 0.02, 0.02, 0); cur = cur.add(step); }
            t.hurt(damageSources().mobAttack(this), 9.0f);
            t.igniteForSeconds(5);
        }
        if (frac < 0.66f && --tpCd <= 0) {
            tpCd = 200;
            sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 40, 0.5, 1, 0.5, 0.3);
            moveTo(t.getX() + (random.nextDouble() - 0.5) * 6, t.getY(), t.getZ() + (random.nextDouble() - 0.5) * 6);
        }
        if (frac < 0.33f && !summoned) {
            summoned = true;
            for (int i = 0; i < 3; i++) {
                Monsters.Demon d = Spawn.spawn(sl, ModEntities.DEMON.get(), getX() + random.nextInt(7) - 3, getY(), getZ() + random.nextInt(7) - 3);
                if (d != null) d.setTarget(t);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        BossRewards.onDefeated(this, 900, 2500, 15);
        super.die(source);
    }
}
