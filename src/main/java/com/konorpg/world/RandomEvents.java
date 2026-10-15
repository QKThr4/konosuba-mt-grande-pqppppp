package com.konorpg.world;

import com.konorpg.Config;
import com.konorpg.data.PlayerRpgData;
import com.konorpg.data.WorldRpgData;
import com.konorpg.entity.CompanionEntity;
import com.konorpg.entity.TownNpcEntity;
import com.konorpg.registry.ModEntities;
import com.konorpg.registry.ModItems;
import com.konorpg.rpg.CompanionManager;
import com.konorpg.rpg.RpgManager;
import com.konorpg.util.Scheduler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Dynamic world events: ambushes, wandering merchants, wounded adventurers, treasure (sometimes a mimic!),
 * bosses, Aqua incidents and city invasions. Frequency and monster counts are configurable.
 */
public final class RandomEvents {
    private static final class Encounter {
        final UUID player;
        final Set<UUID> mobs = new HashSet<>();
        final long deadline;
        final String winMsg;
        final int xp, money, repCity;

        Encounter(UUID player, long deadline, String winMsg, int xp, int money, int repCity) {
            this.player = player; this.deadline = deadline; this.winMsg = winMsg; this.xp = xp; this.money = money; this.repCity = repCity;
        }
    }

    private static final List<Encounter> ACTIVE = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private static EntityType<? extends Mob> pickMonster(int level, net.minecraft.util.RandomSource r) {
        List<Supplier<? extends EntityType<? extends Mob>>> pool = new ArrayList<>();
        pool.add(ModEntities.GOBLIN); pool.add(ModEntities.KOBOLD); pool.add(ModEntities.GIANT_TOAD); pool.add(ModEntities.FLYING_CABBAGE);
        if (level >= 5) { pool.add(ModEntities.SKELETON_WARRIOR); pool.add(ModEntities.GIANT_SPIDER); pool.add(ModEntities.DIRE_WOLF); }
        if (level >= 10) { pool.add(ModEntities.ORC); pool.add(ModEntities.LIZARDMAN); }
        if (level >= 18) { pool.add(ModEntities.GOLEM); pool.add(ModEntities.DEMON); }
        return pool.get(r.nextInt(pool.size())).get();
    }

    /** Called once per second for each player. */
    public static void tick(ServerPlayer p, PlayerRpgData d) {
        long gt = p.level().getGameTime();
        checkEncounters(p, gt);
        if (!Config.EVENTS_ENABLED.get() || !d.registered || p.level().dimension() != Level.OVERWORLD) return;
        if (p.getY() > 200 || p.isSleeping() || d.downedUntil > 0) return;
        if (d.nextEventAt == 0) { d.nextEventAt = gt + 2400; return; }
        if (gt < d.nextEventAt) return;
        for (Encounter e : ACTIVE) if (e.player.equals(p.getUUID())) return; // one at a time
        double mins = 4 + p.getRandom().nextDouble() * 5;
        d.nextEventAt = gt + (long) (mins * 1200 / Config.EVENT_FREQ_MULT.get());
        start(p, d, (ServerLevel) p.level());
    }

    public static void start(ServerPlayer p, PlayerRpgData d, ServerLevel level) {
        WorldRpgData w = WorldRpgData.get(p.server);
        boolean inCity = RpgManager.nearCity(p, 52);
        boolean hasAqua = d.companion("aqua") != null;
        int roll = p.getRandom().nextInt(100);
        if (inCity) {
            if (roll < 40 && d.level >= 3) invasion(p, d, level, w);
            else if (roll < 70 && hasAqua) aquaIncident(p, d);
            else merchant(p, level);
            return;
        }
        boolean bossOk = d.level >= 10 && level.getGameTime() - w.lastBossAt > 36000 && !d.flags.contains("boss_active");
        if (roll < 30) ambush(p, d, level);
        else if (roll < 45) merchant(p, level);
        else if (roll < 60) wounded(p, level);
        else if (roll < 78) treasure(p, d, level);
        else if (roll < 88 && hasAqua) aquaIncident(p, d);
        else if (bossOk) boss(p, d, level, w);
        else ambush(p, d, level);
    }

    // ---------------- events ----------------

    private static void ambush(ServerPlayer p, PlayerRpgData d, ServerLevel level) {
        int n = Math.max(1, (int) Math.round((3 + p.getRandom().nextInt(3)) * Config.EVENT_SPAWN_MULT.get()));
        Encounter e = new Encounter(p.getUUID(), level.getGameTime() + 4800, "You survived the ambush!", 60 + d.level * 6, 80 + d.level * 8, 0);
        for (int i = 0; i < n; i++) {
            BlockPos pos = Spawn.around(level, p.getX(), p.getZ(), 14, 20, p.getRandom());
            Mob m = Spawn.spawn(level, (EntityType<Mob>) (EntityType<?>) pickMonster(d.level, p.getRandom()), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (m != null) { m.setTarget(p); e.mobs.add(m.getUUID()); }
        }
        ACTIVE.add(e);
        RpgManager.title(p, "AMBUSH!", "Monsters are closing in!");
    }

    private static void merchant(ServerPlayer p, ServerLevel level) {
        BlockPos pos = Spawn.around(level, p.getX(), p.getZ(), 6, 10, p.getRandom());
        TownNpcEntity n = ModEntities.TOWN_NPC.get().create(level);
        if (n == null) return;
        String[] skins = {"homem", "homem_2", "homem_3", "mulher", "mulher_2", "mulher_4"};
        n.setup("wandering_merchant", "Wandering Merchant", skins[p.getRandom().nextInt(skins.length)], 1.0f, "shop", "shopkeeper", "wandering", pos);
        n.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        n.setPersistenceRequired();
        level.addFreshEntity(n);
        Scheduler.later(12000, () -> { if (n.isAlive()) n.discard(); });
        RpgManager.msg(p, "A wandering merchant has set up nearby! (%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
    }

    private static void wounded(ServerPlayer p, ServerLevel level) {
        BlockPos pos = Spawn.around(level, p.getX(), p.getZ(), 10, 18, p.getRandom());
        TownNpcEntity n = ModEntities.TOWN_NPC.get().create(level);
        if (n == null) return;
        n.setup("wounded_adventurer", "Wounded Adventurer", "homem_5", 1.0f, "static", "wounded", "", pos);
        n.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        n.setPersistenceRequired();
        level.addFreshEntity(n);
        Scheduler.later(12000, () -> { if (n.isAlive()) n.discard(); });
        RpgManager.msg(p, "You hear someone calling for help! (%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
    }

    private static void treasure(ServerPlayer p, PlayerRpgData d, ServerLevel level) {
        BlockPos pos = Spawn.around(level, p.getX(), p.getZ(), 25, 40, p.getRandom());
        boolean mimic = p.getRandom().nextInt(4) == 0;
        Encounter e = new Encounter(p.getUUID(), level.getGameTime() + 9600, "The treasure is yours!", 40 + d.level * 4, 0, 0);
        if (mimic) {
            Mob m = Spawn.spawn(level, ModEntities.MIMIC.get(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (m != null) { m.setPersistenceRequired(); e.mobs.add(m.getUUID()); }
        } else {
            level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChestBlockEntity chest) {
                ItemStack[] loot = {new ItemStack(ModItems.HEALTH_POTION.get(), 2 + p.getRandom().nextInt(3)), new ItemStack(ModItems.MANA_POTION.get(), 1 + p.getRandom().nextInt(3)),
                        new ItemStack(ModItems.MAGIC_CRYSTAL.get(), 1 + p.getRandom().nextInt(2)), new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 4 + p.getRandom().nextInt(8)),
                        new ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET, 6 + p.getRandom().nextInt(10)), new ItemStack(ModItems.RETURN_SCROLL.get())};
                for (int i = 0; i < loot.length; i++) chest.setItem(3 + i * 2, loot[i]);
                if (p.getRandom().nextInt(3) == 0) chest.setItem(0, new ItemStack(p.getRandom().nextBoolean() ? ModItems.LUCKY_RING.get() : ModItems.SWIFT_RING.get()));
            }
            for (int i = 0; i < 2; i++) {
                Mob m = Spawn.spawn(level, (EntityType<Mob>) (EntityType<?>) pickMonster(d.level, p.getRandom()), pos.getX() + 3.5 - i * 7, pos.getY(), pos.getZ() + 0.5);
                if (m != null) e.mobs.add(m.getUUID());
            }
        }
        ACTIVE.add(e);
        RpgManager.msg(p, "A treasure was spotted at %d, %d, %d - but it is guarded!", pos.getX(), pos.getY(), pos.getZ());
    }

    private static void boss(ServerPlayer p, PlayerRpgData d, ServerLevel level, WorldRpgData w) {
        BlockPos pos = Spawn.around(level, p.getX(), p.getZ(), 28, 40, p.getRandom());
        boolean queen = p.getRandom().nextBoolean() || d.level < 15;
        Mob m = queen ? Spawn.spawn(level, ModEntities.TOAD_QUEEN.get(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)
                : Spawn.spawn(level, ModEntities.DEMON_GENERAL.get(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        if (m == null) return;
        m.setPersistenceRequired();
        m.setTarget(p);
        w.lastBossAt = level.getGameTime();
        w.setDirty();
        for (ServerPlayer o : level.players()) {
            RpgManager.title(o, "A BOSS APPEARED!", "%s is rampaging near %d, %d", m.getDisplayName().getString(), pos.getX(), pos.getZ());
        }
    }

    private static void aquaIncident(ServerPlayer p, PlayerRpgData d) {
        CompanionEntity aqua = CompanionManager.entityOf(p, "aqua");
        if (aqua == null || aqua.isDowned()) return;
        aqua.incident(p.getRandom().nextInt(2));
    }

    private static void invasion(ServerPlayer p, PlayerRpgData d, ServerLevel level, WorldRpgData w) {
        if (w.city == null) return;
        int n = Math.max(2, (int) Math.round((6 + p.getRandom().nextInt(4)) * Config.EVENT_SPAWN_MULT.get()));
        Encounter e = new Encounter(p.getUUID(), level.getGameTime() + 7200, "You defended the town! The people cheer!", 120 + d.level * 8, 200, 6);
        for (int i = 0; i < n; i++) {
            BlockPos pos = Spawn.around(level, w.city.getX(), w.city.getZ() + 62, 2, 10, p.getRandom());
            Mob m = Spawn.spawn(level, (EntityType<Mob>) (EntityType<?>) pickMonster(Math.max(d.level, 6), p.getRandom()), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (m != null) { m.setPersistenceRequired(); m.setTarget(p); e.mobs.add(m.getUUID()); }
        }
        ACTIVE.add(e);
        for (ServerPlayer o : level.players()) RpgManager.title(o, "TOWN UNDER ATTACK!", "Monsters are attacking the south gate!");
    }

    // ---------------- tracking ----------------

    private static void checkEncounters(ServerPlayer p, long gt) {
        Iterator<Encounter> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Encounter e = it.next();
            if (!e.player.equals(p.getUUID())) continue;
            ServerLevel level = (ServerLevel) p.level();
            e.mobs.removeIf(id -> { Entity m = level.getEntity(id); return m == null || !m.isAlive(); });
            if (e.mobs.isEmpty()) {
                it.remove();
                RpgManager.msg(p, e.winMsg);
                RpgManager.grantXp(p, e.xp);
                if (e.money > 0) RpgManager.grantMoney(p, e.money);
                if (e.repCity > 0) RpgManager.addRep(p, "city", e.repCity);
                RpgManager.sync(p);
            } else if (gt > e.deadline) {
                it.remove();
                for (UUID id : e.mobs) { Entity m = level.getEntity(id); if (m != null && !(m instanceof net.minecraft.world.entity.monster.Enemy && m.hasCustomName())) m.discard(); }
                if (e.repCity > 0) { RpgManager.msg(p, "The attack went on too long... the town suffered."); RpgManager.addRep(p, "city", -4); }
            }
        }
    }

    public static void clear() { ACTIVE.clear(); }

    private RandomEvents() {}
}
