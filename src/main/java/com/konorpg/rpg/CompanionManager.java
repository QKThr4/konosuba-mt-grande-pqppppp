package com.konorpg.rpg;

import com.konorpg.Config;
import com.konorpg.data.CompanionRecord;
import com.konorpg.data.PlayerRpgData;
import com.konorpg.entity.CompanionEntity;
import com.konorpg.registry.ModEntities;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Materializes companions from player data next to their owner; feeding, rescue and events. */
public final class CompanionManager {
    private static final Map<String, UUID> ENTITIES = new HashMap<>();

    public static void tick(ServerPlayer p, PlayerRpgData d) {
        if (!Config.COMPANIONS_ENABLED.get() || !d.introDone) return;
        if (d.flags.remove("aqua_pending")) addCompanion(d, "aqua", "Aqua", p.level().getGameTime());
        ServerLevel level = p.serverLevel();
        long gt = level.getGameTime();
        for (CompanionRecord rec : d.companions) {
            UUID eid = ENTITIES.get(rec.uid);
            Entity e = eid == null ? null : level.getEntity(eid);
            if (e instanceof CompanionEntity ce && ce.isAlive() && ce.distanceToSqr(p) < 160 * 160) {
                ce.refresh(rec);
                continue;
            }
            discard(p.server, eid);
            CompanionEntity ce = ModEntities.COMPANION.get().create(level);
            if (ce == null) continue;
            ce.moveTo(p.getX() + 1.5, p.getY(), p.getZ() + 1.5, p.getYRot(), 0);
            ce.setup(rec, p);
            level.addFreshEntity(ce);
            ENTITIES.put(rec.uid, ce.getUUID());
            if (rec.kind.equals("aqua") && d.flags.add("aqua_welcomed")) ce.rant();
        }
    }

    public static CompanionRecord addCompanion(PlayerRpgData d, String kind, String name, long gt) {
        CompanionRecord r = new CompanionRecord();
        r.kind = kind;
        r.name = name;
        r.lastFed = gt;
        r.lastSlept = gt;
        d.companions.add(r);
        return r;
    }

    private static void discard(MinecraftServer server, UUID id) {
        if (id == null) return;
        for (ServerLevel l : server.getAllLevels()) {
            Entity e = l.getEntity(id);
            if (e != null) e.discard();
        }
    }

    public static void onLogout(ServerPlayer p) {
        PlayerRpgData d = RpgManager.data(p);
        for (CompanionRecord r : d.companions) discard(p.server, ENTITIES.remove(r.uid));
    }

    public static void clearAll() { ENTITIES.clear(); }

    private static CompanionEntity active(ServerPlayer p, CompanionRecord r) {
        UUID id = ENTITIES.get(r.uid);
        Entity e = id == null ? null : p.serverLevel().getEntity(id);
        return e instanceof CompanionEntity ce && ce.isAlive() ? ce : null;
    }

    public static CompanionEntity entityOf(ServerPlayer p, String kind) {
        CompanionRecord r = RpgManager.data(p).companion(kind);
        return r == null ? null : active(p, r);
    }

    /** True if at least one companion can still come and revive the player (alive, standing, in this level). */
    public static boolean canRescue(ServerPlayer p) {
        PlayerRpgData d = RpgManager.data(p);
        for (CompanionRecord r : d.companions) {
            CompanionEntity ce = active(p, r);
            if (ce != null && !ce.isDowned() && ce.level() == p.level()) return true;
        }
        return false;
    }

    /** Companion comments + small friendship gains on gameplay events. */
    public static void react(ServerPlayer p, String trigger) {
        PlayerRpgData d = RpgManager.data(p);
        for (CompanionRecord r : d.companions) {
            CompanionEntity ce = active(p, r);
            if (ce == null || ce.isDowned()) continue;
            if (trigger.equals("quest_done") || trigger.equals("rank") || trigger.equals("levelup")) r.friendship += 1;
            if (p.getRandom().nextInt(trigger.equals("kill") ? 6 : 1) == 0) ce.speak(trigger);
        }
    }

    /** Companion slept in a real bed: resets tiredness. */
    public static void markSlept(ServerPlayer p, String kind) {
        CompanionRecord r = RpgManager.data(p).companion(kind);
        if (r != null) r.lastSlept = p.level().getGameTime();
    }

    public static void addFriendship(ServerPlayer p, String kind, int amount) {
        RpgManager.data(p).addFriend(kind, amount);
    }

    /** A companion brings the downed player back with full health. */
    public static void revivePlayer(ServerPlayer p, CompanionEntity by) {
        PlayerRpgData d = RpgManager.data(p);
        d.downedUntil = 0;
        p.removeAllEffects();
        p.setHealth(p.getMaxHealth());
        p.invulnerableTime = 40;
        p.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0.2);
        RpgManager.title(p, "REVIVED!", "%s brought you back with full health", by.displayName());
        by.speak("revived");
        d.addFriend(by.kind(), 1);
        RpgManager.sync(p);
    }

    public static boolean isFood(ItemStack s) {
        return !s.isEmpty() && (s.has(DataComponents.FOOD) || s.getItem() == com.konorpg.registry.ModItems.SAKE_BOTTLE.get());
    }

    /** Feed a companion with the item in the player's main hand. */
    public static boolean feed(ServerPlayer p, String kind) {
        PlayerRpgData d = RpgManager.data(p);
        CompanionRecord r = d.companion(kind);
        ItemStack held = p.getMainHandItem();
        if (r == null) return false;
        if (!isFood(held)) { RpgManager.msg(p, "Hold some food in your hand to feed %s.", r.name); return false; }
        boolean sake = held.getItem() == com.konorpg.registry.ModItems.SAKE_BOTTLE.get();
        if (!p.getAbilities().instabuild) held.shrink(1);
        r.lastFed = p.level().getGameTime();
        d.addFriend(kind, sake && kind.equals("aqua") ? 3 : 1);
        CompanionEntity ce = active(p, r);
        if (ce != null) { ce.refresh(r); if (sake && kind.equals("aqua")) ce.sakeScene(); else ce.speak("fed"); }
        RpgManager.msg(p, "%s was fed!", r.name);
        RpgManager.sync(p);
        return true;
    }

    private CompanionManager() {}
}
