package com.konorpg.entity;

import com.konorpg.data.CompanionRecord;
import com.konorpg.data.PlayerRpgData;
import com.konorpg.dialogue.DialogueManager;
import com.konorpg.registry.ModItems;
import com.konorpg.rpg.CompanionManager;
import com.konorpg.rpg.RpgManager;
import com.konorpg.util.Lang;
import com.konorpg.util.Scheduler;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A materialized companion (currently Aqua). Data lives in the owner's CompanionRecord; the entity is created
 * next to the owner and discarded on logout. Pet-like: only fights what the owner hits / what hits or targets the owner.
 * They never die: at 0 HP they are knocked down for 60s and get revived by the owner, another companion or by time.
 * Needs: food (hunger) and sleep in a real bed. Without them they complain and get slower and weaker.
 */
public class CompanionEntity extends PathfinderMob implements SkinProvider {
    private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final long HUNGRY = 24000L, STARVING = 48000L;       // ~20 / ~40 minutes without food
    private static final long TIRED1 = 24000L, TIRED2 = 48000L, TIRED3 = 72000L; // 1 / 2 / 3 days without a bed
    public static final int DOWN_TICKS = 1200;

    private static final String[] AQUA_IDLE = {
            "Hey, you do know I'm a GODDESS, right? You could treat me better!",
            "I'm thirsty... isn't there a tavern around here?",
            "That town should have a statue of me. Bigger than Eris's.",
            "I could purify this entire place, you know? When I feel like it.",
            "Shall we go back to the Guild? I'm bored and my feet hurt.",
            "If a giant toad shows up, you go first, okay?"};
    private static final String[] AQUA_FRIEND = {
            "You know... you're not so bad, adventurer.",
            "If you fall I'll pick you up. For free! ...This time."};
    private static final String[] AQUA_BEST = {
            "You're my best friend... don't tell Eris!",
            "Thanks for taking me along. Really."};

    private String uid = "";
    private String kind = "aqua";
    private String displayName = "Aqua";
    private UUID ownerUuid;
    private int level = 1;
    public boolean stay, noTeleport;
    private int hunger, tired;
    private long lastSpeak, nextHeal, nextPurify, nextIdle, nextBolt, nextTiredTalk;
    private int speechTicks;
    private int downedTicks;
    private int channel;
    private long retryAt;
    private ItemStack weapon = ItemStack.EMPTY;
    private UUID swallowedBy;
    private boolean floorSleep;

    public CompanionEntity(EntityType<? extends CompanionEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attrs() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 2).add(Attributes.FOLLOW_RANGE, 32);
    }

    public void setup(CompanionRecord rec, ServerPlayer owner) {
        this.uid = rec.uid;
        this.kind = rec.kind;
        this.displayName = rec.name;
        this.ownerUuid = owner.getUUID();
        entityData.set(SKIN, kind.equals("aqua") ? "aqua" : "mulher");
        setCustomName(Component.literal(displayName));
        setCustomNameVisible(true);
        refresh(rec);
        setHealth(getMaxHealth());
    }

    public void refresh(CompanionRecord rec) {
        this.level = rec.level;
        this.stay = rec.stay;
        this.noTeleport = rec.noTeleport;
        long gt = level().getGameTime();
        long since = gt - rec.lastFed;
        int newHunger = since > STARVING ? 2 : since > HUNGRY ? 1 : 0;
        if (newHunger > hunger && !level().isClientSide) speak(newHunger == 2 ? "starving" : "hungry");
        hunger = newHunger;
        long awake = gt - rec.lastSlept;
        int newTired = awake > TIRED3 ? 3 : awake > TIRED2 ? 2 : awake > TIRED1 ? 1 : 0;
        if (newTired > tired && !level().isClientSide) speak("tired" + newTired);
        tired = newTired;
        double dmgMul = Math.min(hunger == 2 ? 0.2 : hunger == 1 ? 0.5 : 1.0, tired == 3 ? 0.5 : tired == 2 ? 0.75 : tired == 1 ? 0.9 : 1.0);
        double spdMul = Math.min(hunger == 2 ? 0.45 : hunger == 1 ? 0.8 : 1.0, tired == 3 ? 0.6 : tired == 2 ? 0.85 : 1.0);
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(30 + rec.level * 2.0);
        var atk = getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue((2 + rec.level * 0.4) * dmgMul);
        var spd = getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) spd.setBaseValue(0.3 * spdMul);
        if (!rec.weapon.isEmpty()) weapon = rec.weapon.copy();
        else if (kind.equals("aqua")) weapon = new ItemStack(ModItems.APPRENTICE_STAFF.get());
        else weapon = ItemStack.EMPTY;
    }

    public int hungerStage() { return hunger; }

    public int tiredStage() { return tired; }

    public boolean isDowned() { return downedTicks > 0; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN, "aqua");
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(4, new FollowOwnerGoal(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected boolean isImmobile() { return downedTicks > 0 || swallowedBy != null || floorSleep || isSleeping() || super.isImmobile(); }

    private static final class FollowOwnerGoal extends Goal {
        private final CompanionEntity c;
        private int recalc;

        FollowOwnerGoal(CompanionEntity c) { this.c = c; setFlags(EnumSet.of(Flag.MOVE)); }

        @Override public boolean canUse() {
            ServerPlayer o = c.owner();
            return o != null && !c.stay && !c.isDowned() && c.getTarget() == null && c.distanceToSqr(o) > 25;
        }

        @Override public boolean canContinueToUse() {
            ServerPlayer o = c.owner();
            return o != null && !c.stay && !c.isDowned() && c.getTarget() == null && c.distanceToSqr(o) > 6;
        }

        @Override public void tick() {
            ServerPlayer o = c.owner();
            if (o == null || --recalc > 0) return;
            recalc = 10;
            c.goTo(o, 1.3);
        }
    }

    /** Walk toward an entity; teleport next to it when far away (if the owner allowed teleporting). */
    void goTo(LivingEntity target, double speed) {
        if (distanceToSqr(target) > 24 * 24 && !noTeleport && target.onGround()) {
            getNavigation().stop();
            moveTo(target.getX() + 1.0, target.getY(), target.getZ() + 1.0, getYRot(), getXRot());
        } else {
            getNavigation().moveTo(target, speed);
        }
    }

    public ServerPlayer owner() {
        if (ownerUuid == null) return null;
        Player p = level().getPlayerByUUID(ownerUuid);
        return p instanceof ServerPlayer sp ? sp : null;
    }

    public boolean isOwnedBy(Player p) { return ownerUuid != null && ownerUuid.equals(p.getUUID()); }

    public UUID ownerUuid() { return ownerUuid; }

    public String uid() { return uid; }

    public String kind() { return kind; }

    public String displayName() { return displayName; }

    // ---------------- speech (translated to the owner's language) ----------------

    public void say(String en, Object... args) {
        ServerPlayer o = owner();
        String line = Lang.tr(o, en, args);
        if (o != null) o.sendSystemMessage(Component.literal("<" + displayName + "> ").withStyle(ChatFormatting.AQUA).append(Component.literal(line).withStyle(ChatFormatting.WHITE)));
        setCustomName(Component.literal(displayName + ": " + line).withStyle(ChatFormatting.AQUA));
        speechTicks = 100;
    }

    /** Context-aware comments, personality inspired by the original goddess. */
    public void speak(String trigger) {
        long gt = level().getGameTime();
        boolean urgent = trigger.equals("revived") || trigger.equals("rescue_start") || trigger.equals("downed") || trigger.equals("fed");
        if (!urgent && gt - lastSpeak < 100) return;
        lastSpeak = gt;
        int fl = 0;
        ServerPlayer o = owner();
        if (o != null) fl = RpgManager.data(o).friendLevel(kind);
        String line = switch (trigger) {
            case "idle" -> fl >= 5 ? pick(AQUA_BEST) : fl >= 3 ? pick(AQUA_FRIEND) : pick(AQUA_IDLE);
            case "kill" -> pick(new String[]{"Nice! That was thanks to my blessing, obviously.", "I helped! See? I helped!", "Another one! The goddess approves."});
            case "hurt" -> pick(new String[]{"Careful!! If you fall, who's going to buy my drinks?!", "Aaah! Heal yourself quickly, I'm... nearby!"});
            case "levelup" -> "Leveled up? Obviously thanks to me. Thank me with sake.";
            case "rank" -> "New rank! I deserve half the credit.";
            case "quest_accept" -> "A quest? Easy! I'll handle everything... you go first.";
            case "quest_done" -> "A reward! Let's spend it all on sake! ...Okay, on food.";
            case "night" -> "It's dark... I'm NOT scared. It's just that the undead stink.";
            case "rain" -> "Rain? I'm the goddess of water! This puts me in a good mood.";
            case "heal" -> "Heal! You can thank me later.";
            case "heal_fail" -> "Oops! I healed the monster instead of you... It was an accident!";
            case "purify" -> "Turn Undead!! Feel the power of a goddess!";
            case "rescue_start" -> "Hang in there! The goddess has arrived! Sacred Resurrection!";
            case "rescue_fail" -> "Uh... the spell failed?! Wait! I'll try again!";
            case "revived" -> "Done! Full health! You can call me a goddess from now on.";
            case "downed" -> "Ow ow ow... somebody help me...";
            case "hungry" -> "I'm hungry... goddesses need to eat too, you know?";
            case "starving" -> "I... am going to faint... please, food...";
            case "fed" -> "Delicious! You're the best adventurer ever!";
            case "hit" -> "You hit me?! I'm telling Eris!";
            case "sleepy" -> pick(new String[]{"I'm sleepy... adventurers need to sleep too, you know?", "It's already night! Where's a bed? I'm a goddess, I deserve a soft bed."});
            case "floor" -> "Sleeping on the FLOOR is awful! My back! A goddess deserves a bed!!";
            case "bed" -> "Zzz... finally a bed... Zzz...";
            case "tired1" -> "Ugh, my back hurts a little... I miss sleeping in a real bed.";
            case "tired2" -> "I'm so sleepy... everything hurts and my legs feel heavy. A bed, please!";
            case "tired3" -> "I... can't even lift my staff... I haven't slept in forever... I need a BED. Now.";
            case "tired_talk" -> tired >= 3 ? "Zzz... huh? I'm awake! ...I need to sleep in a bed." : tired == 2 ? "My neck hurts... and my eyes are closing..." : "I could use a nap in a proper bed...";
            default -> null;
        };
        if (line != null) say(line);
    }

    private String pick(String[] a) { return a[getRandom().nextInt(a.length)]; }

    // ---------------- state ----------------

    public void knockDown() {
        downedTicks = DOWN_TICKS;
        setTarget(null);
        getNavigation().stop();
        setHealth(1.0f);
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        ServerPlayer o = owner();
        setCustomName(Component.literal(displayName + Lang.tr(o, " (downed)")).withStyle(ChatFormatting.RED));
        speak("downed");
    }

    public void revive(float fraction, boolean announce) {
        downedTicks = 0;
        channel = 0;
        setHealth(Math.max(2.0f, getMaxHealth() * fraction));
        setCustomName(Component.literal(displayName));
        if (announce) speak("revived");
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (ownerUuid == null) { discard(); return; } // loaded from disk: the manager already created a fresh one
        if (swallowedBy != null) { tickSwallowed(); return; }
        if (downedTicks > 0) {
            if (--downedTicks == 0) revive(0.5f, true);
            return;
        }
        if (speechTicks > 0 && --speechTicks == 0) setCustomName(Component.literal(displayName));
        if (tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(0.5f);
        if (tickCount % 10 == 0) {
            ItemStack want = getTarget() != null ? weapon : ItemStack.EMPTY; // weapon only drawn in combat
            if (!ItemStack.matches(getMainHandItem(), want)) setItemSlot(EquipmentSlot.MAINHAND, want.copy());
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (downedTicks > 0 || swallowedBy != null) return;
        ServerPlayer o = owner();
        if (o == null || o.level() != level()) return;
        // sleeping: with a bed nearby she sleeps next to you; on the floor she complains in the morning
        if (o.isSleeping()) {
            if (!isSleeping() && !floorSleep) tryBed(o);
            return;
        } else if (isSleeping() || floorSleep) {
            if (isSleeping()) stopSleeping();
            if (floorSleep) { floorSleep = false; speak("floor"); }
        }
        long gt = level().getGameTime();
        PlayerRpgData od = RpgManager.data(o);
        if (od.downedUntil > gt) { rescue(o, od, gt); return; }
        if (tickCount % 10 == 0) {
            if (rescueCompanion()) return;
            LivingEntity t = getTarget();
            if (t == null || !t.isAlive() || t.distanceToSqr(o) > 24 * 24 || !valid(t, o)) t = pickTarget(o);
            if (!stay || (t != null && t.distanceToSqr(this) < 36)) setTarget(t);
            if (kind.equals("aqua")) aquaAbilities(o, gt);
            if (gt >= nextIdle) {
                nextIdle = gt + 1200 + getRandom().nextInt(1800);
                speak("idle");
            }
            if (tired > 0 && gt >= nextTiredTalk) {
                nextTiredTalk = gt + 3600 + getRandom().nextInt(2400);
                speak("tired_talk");
            }
        }
        if (tickCount % 400 == 0 && getRandom().nextInt(3) == 0) {
            if (level().isNight()) speak(getRandom().nextBoolean() ? "night" : "sleepy");
            else if (level().isRaining()) speak("rain");
        }
        LivingEntity t = getTarget();
        if (kind.equals("aqua") && t != null && gt >= nextBolt && distanceToSqr(t) > 6 && distanceToSqr(t) < 169 && hasLineOfSight(t)) {
            nextBolt = gt + 50;
            t.hurt(level().damageSources().indirectMagic(this, this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) + 1.5f);
            ((ServerLevel) level()).sendParticles(ParticleTypes.SPLASH, t.getX(), t.getY() + 1, t.getZ(), 12, 0.3, 0.4, 0.3, 0.1);
        }
    }

    /** Pet-like targeting: what the owner hit, what hit the owner, or what is hunting the owner/me. */
    private LivingEntity pickTarget(ServerPlayer o) {
        LivingEntity a = o.getLastHurtMob();
        if (a != null && a.isAlive() && o.tickCount - o.getLastHurtMobTimestamp() < 100 && valid(a, o)) return a;
        LivingEntity b = o.getLastHurtByMob();
        if (b != null && b.isAlive() && o.tickCount - o.getLastHurtByMobTimestamp() < 100 && valid(b, o)) return b;
        Mob best = null;
        for (Mob m : level().getEntitiesOfClass(Mob.class, o.getBoundingBox().inflate(14))) {
            if ((m.getTarget() == o || m.getTarget() == this) && m.isAlive() && valid(m, o)
                    && (best == null || m.distanceToSqr(o) < best.distanceToSqr(o))) best = m;
        }
        return best;
    }

    private boolean valid(LivingEntity e, ServerPlayer o) {
        if (e == o || e == this || e instanceof Player || e instanceof TownNpcEntity || e instanceof CompanionEntity) return false;
        if (e.distanceToSqr(o) > 20 * 20 || Math.abs(e.getY() - o.getY()) > 8) return false; // ignore mobs far/underground
        return hasLineOfSight(e) || o.hasLineOfSight(e);
    }

    private void rescue(ServerPlayer o, PlayerRpgData od, long gt) {
        setTarget(null);
        if (gt < retryAt) return;
        if (distanceToSqr(o) > 9) {
            if (tickCount % 10 == 0) goTo(o, 1.5);
            channel = 0;
            return;
        }
        getNavigation().stop();
        getLookControl().setLookAt(o);
        if (channel == 0) speak("rescue_start");
        channel++;
        ((ServerLevel) level()).sendParticles(ParticleTypes.END_ROD, o.getX(), o.getY() + 1, o.getZ(), 3, 0.3, 0.6, 0.3, 0.02);
        if (channel >= 60) {
            channel = 0;
            if (kind.equals("aqua") && getRandom().nextFloat() < 0.15f) { speak("rescue_fail"); retryAt = gt + 100; return; }
            CompanionManager.revivePlayer(o, this);
        }
    }

    /** Companions revive each other. */
    private boolean rescueCompanion() {
        List<CompanionEntity> list = level().getEntitiesOfClass(CompanionEntity.class, getBoundingBox().inflate(24),
                c -> c != this && c.isDowned() && c.ownerUuid != null && c.ownerUuid.equals(ownerUuid));
        if (list.isEmpty()) { channel = 0; return false; }
        CompanionEntity d = list.get(0);
        if (distanceToSqr(d) > 4) { getNavigation().moveTo(d, 1.4); channel = 0; return true; }
        getNavigation().stop();
        channel += 10;
        ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, d.getX(), d.getY() + 1.5, d.getZ(), 2, 0.3, 0.3, 0.3, 0);
        if (channel >= 80) { channel = 0; d.revive(1.0f, true); }
        return true;
    }

    private void aquaAbilities(ServerPlayer o, long gt) {
        if (gt >= nextHeal && o.getHealth() < o.getMaxHealth() * 0.5f && distanceToSqr(o) < 100) {
            nextHeal = gt + 900; // 45s cooldown
            if (getRandom().nextFloat() < 0.12f) {
                speak("heal_fail");
            } else {
                o.heal(6 + level * 0.5f);
                ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, o.getX(), o.getY() + 1.8, o.getZ(), 6, 0.3, 0.3, 0.3, 0);
                speak("heal");
            }
        }
        if (gt >= nextPurify) {
            List<Mob> undead = level().getEntitiesOfClass(Mob.class, o.getBoundingBox().inflate(8),
                    m -> m.isAlive() && m.getType().is(EntityTypeTags.UNDEAD) && m.getTarget() != null);
            if (!undead.isEmpty()) {
                nextPurify = gt + 1200; // 60s cooldown
                for (Mob m : undead) m.hurt(level().damageSources().magic(), 8 + level);
                ((ServerLevel) level()).sendParticles(ParticleTypes.END_ROD, o.getX(), o.getY() + 1, o.getZ(), 40, 2, 0.5, 2, 0.05);
                speak("purify");
            }
        }
    }

    // ---------------- toad swallow / sleep / scenes ----------------

    private void swallow(Monsters.GiantToad toad) {
        swallowedBy = toad.getUUID();
        setTarget(null);
        getNavigation().stop();
        setInvisible(true);
        toad.addEffect(new MobEffectInstance(MobEffects.GLOWING, 6000, 0));
        setCustomName(Component.literal(displayName + Lang.tr(owner(), " (swallowed!)")).withStyle(ChatFormatting.RED));
        say("HELP!! The toad swallowed me!! It's all slimy in here!! Kill it, quick!!");
    }

    private void tickSwallowed() {
        Entity t = ((ServerLevel) level()).getEntity(swallowedBy);
        if (t == null || !t.isAlive()) {
            swallowedBy = null;
            setInvisible(false);
            setCustomName(Component.literal(displayName));
            say("Blergh... I'm covered in slime! I'm never going near a toad again!");
            return;
        }
        moveTo(t.getX(), t.getY() + 0.4, t.getZ(), getYRot(), getXRot());
        setDeltaMovement(Vec3.ZERO);
        if (tickCount % 80 == 0) say(pick(new String[]{"GET ME OUT OF HERE!!", "I'm a goddess!! This is outrageous!!", "What a horrible smell!! Help!!"}));
    }

    private void tryBed(ServerPlayer o) {
        BlockPos c = o.blockPosition(), found = null;
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-6, -2, -6), c.offset(6, 2, 6))) {
            BlockState st = level().getBlockState(pos);
            if (st.getBlock() instanceof BedBlock && !st.getValue(BedBlock.OCCUPIED)) { found = pos.immutable(); break; }
        }
        getNavigation().stop();
        if (found != null) {
            startSleeping(found);
            CompanionManager.markSlept(o, kind);
            speak("bed");
        } else {
            floorSleep = true;
        }
    }

    /** Comic trouble caused by Aqua (random event): 0 = drunk, 1 = shouts and attracts monsters. */
    public void incident(int variant) {
        ServerPlayer o = owner();
        if (o == null || !(level() instanceof ServerLevel sl)) return;
        if (variant == 0) {
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 1));
            say("Hic! I found some sake... hic... it was for religious purposes!");
            Scheduler.later(120, () -> { if (isAlive()) say("Everything is... spinning... but I'm FINE! Hic!"); });
        } else {
            say("HEEEEELP!! I saw a spider!! EVERYBODY COME HERE!!");
            for (int i = 0; i < 3; i++) {
                BlockPos pos = com.konorpg.world.Spawn.around(sl, getX(), getZ(), 12, 16, getRandom());
                Mob m = com.konorpg.world.Spawn.spawn(sl, com.konorpg.registry.ModEntities.GOBLIN.get(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                if (m != null) m.setTarget(o);
            }
        }
    }

    /** Dramatic reaction when she gets taken to the other world as "the item". */
    public void rant() {
        String[] lines = {
                "Wait... WHERE am I?! Is this Axel?! I was chosen as... an ITEM?!",
                "You traded me for an adventure bonus!! I'm a GODDESS, you know?!",
                "I want to go back to heaven!! Eris must be laughing at me right now!!",
                "Just you wait! When I get powerful, you'll regret this!",
                "...Fine. At least is there sake here? If there is, I'll forgive you. Maybe."};
        for (int i = 0; i < lines.length; i++) {
            final String l = lines[i];
            Scheduler.later(60 + i * 110, () -> { if (isAlive() && !isRemoved() && owner() != null) say(l); });
        }
    }

    /** Special multi-line reaction when you give her sake. */
    public void sakeScene() {
        String[] lines = {
                "This... this is... SAKE?! From the Axis Order?!",
                "Glub glub glub... PUAH! Now I feel like a goddess again!",
                "Listen here, adventurer... you're the BEST person in this world. I've never told anyone that!",
                "Hic! I promise... hic... to protect you with my divine life! ...But can I have another?"};
        for (int i = 0; i < lines.length; i++) {
            final String l = lines[i];
            Scheduler.later(i * 70, () -> { if (isAlive() && !isRemoved()) say(l); });
        }
    }

    @Override
    public boolean hurt(DamageSource s, float amount) {
        if (swallowedBy != null) return s.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(s, amount);
        if (!level().isClientSide && s.getEntity() instanceof Monsters.GiantToad toad && getRandom().nextFloat() < 0.30f) { swallow(toad); return false; }
        if (downedTicks > 0) return s.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(s, amount);
        if (s.getEntity() instanceof Player) {
            if (!level().isClientSide) { speak("hit"); ServerPlayer o = owner(); if (o != null) CompanionManager.addFriendship(o, kind, -3); }
            return false;
        }
        if (!s.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && getHealth() - amount <= 1.0f) {
            if (!level().isClientSide) knockDown();
            return false;
        }
        return super.hurt(s, amount);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide && player instanceof ServerPlayer sp) {
            if (!isOwnedBy(sp)) say("I belong to another adventurer!");
            else if (isDowned()) revive(0.5f, true);
            else DialogueManager.start(sp, this, kind);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); }

    @Override public String skinName() { return entityData.get(SKIN); }

    @Override public float modelScale() { return 0.93f; } // a bit smaller than the player
}
