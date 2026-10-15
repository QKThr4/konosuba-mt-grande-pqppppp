package com.konorpg.entity;

import com.konorpg.Config;
import com.konorpg.world.Spawn;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Giant Toad Queen (boss): huge bouncing toad that slams the ground and calls her children. */
public class ToadQueenEntity extends Monsters.GiantToad {
    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.konorpg.toad_queen"),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
    private int phase = 1;
    private int slamCooldown = 120;

    public ToadQueenEntity(EntityType<? extends ToadQueenEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData r = super.finalizeSpawn(level, diff, type, data);
        setSize(7, true);
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(420 * Config.MONSTER_HP_MULT.get());
        var atk = getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(9 * Config.MONSTER_DMG_MULT.get());
        setHealth(getMaxHealth());
        return r;
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }

    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bar.setProgress(getHealth() / getMaxHealth());
        float frac = getHealth() / getMaxHealth();
        if (phase == 1 && frac < 0.66f) { phase = 2; summon(3); }
        if (phase == 2 && frac < 0.33f) { phase = 3; summon(4); }
        if (--slamCooldown <= 0) {
            slamCooldown = phase == 3 ? 100 : 160;
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(7))) {
                if (p.isCreative() || p.isSpectator()) continue;
                p.hurt(damageSources().mobAttack(this), 8.0f);
                Vec3 push = p.position().subtract(position()).normalize().scale(1.2);
                p.setDeltaMovement(push.x, 0.6, push.z);
                p.hurtMarked = true;
            }
        }
    }

    private void summon(int n) {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel sl)) return;
        for (int i = 0; i < n; i++) {
            Monsters.GiantToad t = Spawn.spawn(sl, com.konorpg.registry.ModEntities.GIANT_TOAD.get(), getX() + (random.nextDouble() - 0.5) * 8, getY(), getZ() + (random.nextDouble() - 0.5) * 8);
            if (t != null && getTarget() != null) t.setTarget(getTarget());
        }
    }

    @Override
    public void die(DamageSource source) {
        BossRewards.onDefeated(this, 700, 1800, 12);
        super.setSize(1, false); // no giant split on death
        super.die(source);
    }
}
