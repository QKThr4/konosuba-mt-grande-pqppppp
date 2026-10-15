package com.konorpg.entity;

import com.konorpg.dialogue.DialogueManager;
import com.konorpg.rpg.RpgManager;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Townsfolk, guild staff, shopkeepers, guards. Like villagers, workers have an (unbreakable) workstation
 * block and walk back to it when they wander too far. Weapons are only drawn while fighting.
 * EM BREVE: sleeping in beds, tavern visits, recruitable adventurer parties.
 */
public class TownNpcEntity extends PathfinderMob implements SkinProvider {
    private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(TownNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(TownNpcEntity.class, EntityDataSerializers.FLOAT);

    private String npcId = "";
    private String npcName = "";
    private String role = "villager";
    private String dialogue = "villager";
    private String shop = "";
    private BlockPos home = BlockPos.ZERO;
    private int speechTicks;
    private int punchCount;
    private BlockPos bed;

    public TownNpcEntity(EntityType<? extends TownNpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attrs() {
        return net.minecraft.world.entity.Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 6).add(Attributes.FOLLOW_RANGE, 24);
    }

    public TownNpcEntity setup(String id, String name, String skin, float scale, String role, String dialogue, String shop, BlockPos home) {
        this.npcId = id; this.npcName = name; this.role = role; this.dialogue = dialogue; this.shop = shop; this.home = home;
        entityData.set(SKIN, skin);
        entityData.set(SCALE, scale);
        setCustomName(nameComponent());
        setCustomNameVisible(true);
        rebuildGoals();
        return this;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN, "villager_a");
        builder.define(SCALE, 1.0f);
    }

    private void rebuildGoals() {
        goalSelector.removeAllGoals(g -> true);
        targetSelector.removeAllGoals(g -> true);
        registerGoals();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if ("guard".equals(role)) {
            goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
            targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.LivingEntity.class, 10, true, false, e -> e instanceof Enemy));
        }
        if (!"static".equals(role)) goalSelector.addGoal(4, new HomeGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    private int leash() {
        return switch (role == null ? "" : role) {
            case "guard" -> 6;
            case "shop", "guild" -> 4;
            case "villager" -> 14;
            default -> 8;
        };
    }

    /** Walk back to the work/home block when too far; otherwise wander a little near it. */
    private static final class HomeGoal extends Goal {
        private final TownNpcEntity npc;
        private Vec3 target;

        HomeGoal(TownNpcEntity npc) { this.npc = npc; setFlags(EnumSet.of(Flag.MOVE)); }

        @Override
        public boolean canUse() {
            if (npc.home == BlockPos.ZERO || npc.getTarget() != null) return false;
            double d2 = npc.distanceToSqr(npc.home.getX() + 0.5, npc.home.getY(), npc.home.getZ() + 0.5);
            int r = npc.leash();
            if (d2 > (double) r * r) {
                target = Vec3.atBottomCenterOf(npc.home);
                return true;
            }
            if (npc.getRandom().nextInt(120) != 0) return false;
            Vec3 v = DefaultRandomPos.getPos(npc, Math.max(3, r / 2), 2);
            if (v == null || v.distanceToSqr(npc.home.getX(), v.y, npc.home.getZ()) > (double) r * r) return false;
            target = v;
            return true;
        }

        @Override public boolean canContinueToUse() { return !npc.getNavigation().isDone() && npc.getTarget() == null; }

        @Override public void start() { npc.getNavigation().moveTo(target.x, target.y, target.z, 0.85); }
    }

    /** Translatable name: named NPCs have a lang key (npc.konorpg.<id>), residents use their plain name. */
    public Component nameComponent() {
        return Component.translatableWithFallback("npc.konorpg." + npcId, npcName);
    }

    public void setBed(BlockPos b) { this.bed = b; }

    @Override
    protected boolean isImmobile() { return isSleeping() || super.isImmobile(); }

    /** Night routine: residents walk to their bed and sleep until morning. */
    private void sleepRoutine() {
        if (bed == null) return;
        long t = level().getDayTime() % 24000L;
        boolean night = t >= 13000L && t <= 23000L;
        if (night) {
            if (!isSleeping()) {
                if (distanceToSqr(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5) <= 4.0) {
                    if (level().getBlockState(bed).getBlock() instanceof net.minecraft.world.level.block.BedBlock) startSleeping(bed);
                } else if (getNavigation().isDone()) {
                    getNavigation().moveTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, 0.9);
                }
            }
        } else if (isSleeping()) {
            stopSleeping();
        }
    }

    public void say(String text) {
        setCustomName(Component.empty().append(nameComponent()).append(Component.literal(": " + text)).withStyle(net.minecraft.ChatFormatting.YELLOW));
        speechTicks = 100;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (speechTicks > 0 && --speechTicks == 0) setCustomName(nameComponent());
        if (tickCount % 20 == 0) sleepRoutine();
        if (tickCount % 10 == 0 && "guard".equals(role)) {
            ItemStack want = getTarget() != null ? new ItemStack(Items.IRON_SWORD) : ItemStack.EMPTY;
            if (getMainHandItem().isEmpty() != want.isEmpty()) setItemSlot(EquipmentSlot.MAINHAND, want);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide && isSleeping()) {
            if (player instanceof ServerPlayer sp0) RpgManager.bar(sp0, "%s is sleeping... Zzz.", com.konorpg.util.Lang.tr(sp0, npcName));
            return InteractionResult.sidedSuccess(false);
        }
        if (!level().isClientSide && player instanceof ServerPlayer sp) {
            getNavigation().stop();
            getLookControl().setLookAt(player);
            DialogueManager.start(sp, this, dialogue);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** NPCs cannot be killed by gameplay damage; hitting them costs reputation. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (!level().isClientSide && source.getEntity() instanceof ServerPlayer sp && speechTicks < 60) {
            say(com.konorpg.util.Lang.tr(sp, "Ouch! Why did you do that?!"));
            RpgManager.addRep(sp, "city", -1);
        }
        return false;
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putString("npcId", npcId);
        t.putString("npcName", npcName);
        t.putString("role", role);
        t.putString("dialogue", dialogue);
        t.putString("shop", shop);
        t.putString("skin", entityData.get(SKIN));
        t.putFloat("scale", entityData.get(SCALE));
        t.putLong("home", home.asLong());
        if (bed != null) t.putLong("bed", bed.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        npcId = t.getString("npcId");
        npcName = t.getString("npcName");
        role = t.getString("role").isEmpty() ? "villager" : t.getString("role");
        dialogue = t.getString("dialogue");
        shop = t.getString("shop");
        entityData.set(SKIN, t.getString("skin").isEmpty() ? "villager_a" : t.getString("skin"));
        entityData.set(SCALE, t.contains("scale") ? t.getFloat("scale") : 1.0f);
        home = t.contains("home") ? BlockPos.of(t.getLong("home")) : BlockPos.ZERO;
        bed = t.contains("bed") ? BlockPos.of(t.getLong("bed")) : null;
        if (!npcName.isEmpty()) { setCustomName(nameComponent()); setCustomNameVisible(true); }
        rebuildGoals();
    }

    public String npcId() { return npcId; }
    public String npcName() { return npcName; }
    public String shopId() { return shop; }
    public String role() { return role; }

    @Override public String skinName() { return entityData.get(SKIN); }
    @Override public float modelScale() { return entityData.get(SCALE); }
}
