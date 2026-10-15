package com.konorpg.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Mimic: looks exactly like a chest until you get close or hit it. */
public class MimicEntity extends Monster {
    private static final EntityDataAccessor<Boolean> REVEALED = SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    public MimicEntity(EntityType<? extends MimicEntity> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder attrs() { return RpgHumanoidMonster.base(45, 8, 0.28); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(REVEALED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean isRevealed() { return entityData.get(REVEALED); }

    public void reveal() {
        if (isRevealed()) return;
        entityData.set(REVEALED, true);
        level().playSound(null, blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.HOSTILE, 1.0f, 0.6f);
        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.5, getZ(), 15, 0.4, 0.3, 0.4, 0.05);
    }

    @Override
    protected boolean isImmobile() { return !isRevealed() || super.isImmobile(); }

    @Override
    public boolean isPushable() { return isRevealed() && super.isPushable(); }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && !isRevealed() && tickCount % 5 == 0) {
            Player p = level().getNearestPlayer(this, 3.2);
            if (p != null && !p.isCreative() && !p.isSpectator()) { reveal(); setTarget(p); }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isRevealed()) reveal();
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putBoolean("revealed", isRevealed()); }

    @Override
    public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); entityData.set(REVEALED, t.getBoolean("revealed")); }
}
