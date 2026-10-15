package com.konorpg.event;

import com.konorpg.Config;
import com.konorpg.KonoRPG;
import com.konorpg.data.CompanionRecord;
import com.konorpg.data.PlayerRpgData;
import com.konorpg.data.WorldRpgData;
import com.konorpg.dialogue.DialogueManager;
import com.konorpg.entity.CompanionEntity;
import com.konorpg.network.Net;
import com.konorpg.quest.QuestManager;
import com.konorpg.rpg.CompanionManager;
import com.konorpg.rpg.RpgManager;
import com.konorpg.rpg.RpgStats;
import com.konorpg.shop.EconomyManager;
import com.konorpg.shop.ShopManager;
import com.konorpg.util.Scheduler;
import com.konorpg.world.CityBuilder;
import com.konorpg.world.CityInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Gameplay events on the server (game bus). */
@EventBusSubscriber(modid = KonoRPG.MOD_ID)
public final class ServerEvents {
    @SubscribeEvent
    public static void reload(AddReloadListenerEvent e) {
        e.addListener(QuestManager.INSTANCE);
        e.addListener(DialogueManager.INSTANCE);
        e.addListener(ShopManager.INSTANCE);
        e.addListener(EconomyManager.INSTANCE);
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent e) {
        WorldRpgData w = WorldRpgData.get(e.getServer());
        CityInfo.set(w.city);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent e) {
        CityInfo.set(null);
        Scheduler.clear();
        CompanionManager.clearAll();
        com.konorpg.world.RandomEvents.clear();
        DialogueManager.clearSessions();
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post e) { Scheduler.tick(); }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        PlayerRpgData d = RpgManager.data(p);
        RpgManager.refreshAttributes(p);
        RpgManager.sync(p);
        if (!d.introDone && Config.AUTO_INTRO.get()) {
            com.konorpg.world.AfterlifeRoom.enter(p);
            Scheduler.later(50, () -> { if (!p.hasDisconnected() && !RpgManager.data(p).introDone) Net.openScreen(p, "intro", com.konorpg.world.AfterlifeRoom.introTag(p.server)); });
        } else if (d.flags.contains("intro_cutscene")) {
            Scheduler.later(40, () -> com.konorpg.world.AfterlifeRoom.finish(p));
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            CompanionManager.onLogout(p);
            com.konorpg.util.Lang.forget(p.getUUID());
            DialogueManager.forget(p.getUUID());
            ShopManager.close(p);
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        PlayerRpgData d = RpgManager.data(p);
        RpgManager.refreshAttributes(p);
        if (d.registered && p.getRespawnPosition() == null && WorldRpgData.get(p.server).cityBuilt) {
            CityBuilder.teleportToPlaza(p);
            RpgManager.msg(p, "You woke up in Axel's square. (Eris helped, again.)");
        }
        RpgManager.sync(p);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { RpgManager.refreshAttributes(p); RpgManager.sync(p); }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        PlayerRpgData d = RpgManager.data(p);
        if (p.tickCount % 20 == 0) {
            double max = RpgStats.maxMana(p, d);
            d.mana = Math.min(max, d.mana + RpgStats.manaRegen(p, d));
            QuestManager.tickPlayer(p, d);
            com.konorpg.world.RandomEvents.tick(p, d);
        }
        if (d.downedUntil > 0 && p.tickCount % 20 == 0 && p.level().getGameTime() > d.downedUntil) {
            d.flags.add("force_death");
            p.kill();
        }
        if (p.tickCount % 40 == 0 && p.level().isDay() && !p.isSleeping() && com.konorpg.world.Hotel.isInside(p)) com.konorpg.world.Hotel.checkout(p);
        if (p.tickCount % 40 == 0) CompanionManager.tick(p, d);
        if (p.tickCount % 100 == 0) { RpgManager.refreshAttributes(p); RpgManager.sync(p); }
    }

    @SubscribeEvent
    public static void attack(AttackEntityEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.level().getGameTime() < RpgManager.data(p).incapacitatedUntil) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getLevel().getBlockState(e.getPos()).getBlock() instanceof net.minecraft.world.level.block.ButtonBlock
                && com.konorpg.world.Hotel.isHotelBlock(p, e.getPos())) {
            e.setCanceled(true);
            com.konorpg.world.Hotel.checkout(p);
        }
    }

    @SubscribeEvent
    public static void rightClick(PlayerInteractEvent.RightClickItem e) {
        if (e.getEntity() instanceof ServerPlayer p && p.level().getGameTime() < RpgManager.data(p).incapacitatedUntil) e.setCanceled(true);
    }

    /** Critical hits (LUCK/DEX) on melee damage. Magic and skill damage are not modified here. */
    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof ServerPlayer dp && RpgManager.data(dp).downedUntil > 0
                && !e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            e.setCanceled(true); // knocked-down players cannot be hurt while waiting for rescue
            return;
        }
        if (e.getSource().getEntity() instanceof ServerPlayer p && e.getSource().getDirectEntity() == p
                && !e.getSource().is(DamageTypes.INDIRECT_MAGIC) && !(e.getEntity() instanceof com.konorpg.entity.TownNpcEntity)) {
            PlayerRpgData d = RpgManager.data(p);
            if (p.getRandom().nextDouble() < RpgStats.critChance(p, d)) {
                e.setAmount(e.getAmount() * 1.6f);
                RpgManager.bar(p, "CRITICAL!");
            }
        }
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead.level().isClientSide) return;
        if (dead instanceof ServerPlayer p) {
            PlayerRpgData d = RpgManager.data(p);
            long gt = p.level().getGameTime();
            boolean forced = d.flags.remove("force_death");
            if (!forced && d.introDone && d.downedUntil == 0 && CompanionManager.canRescue(p)) {
                // knocked down for 60s: companions come and revive the player
                e.setCanceled(true);
                d.downedUntil = gt + 1200;
                p.setHealth(1.0f);
                p.removeAllEffects();
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 1300, 6));
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 40, 0));
                RpgManager.title(p, "YOU FELL!", "Your companions will come to revive you (60s)");
                RpgManager.sync(p);
                return;
            }
            d.downedUntil = 0;
            d.deaths++;
            long loss = d.money * Config.DEATH_MONEY_LOSS_PERCENT.get() / 100;
            if (loss > 0) {
                d.money -= loss;
                RpgManager.msg(p, "You lost %d Eris when you died...", loss);
            }
            return;
        }
        Entity src = e.getSource().getEntity();
        ServerPlayer killer = null;
        if (src instanceof ServerPlayer sp) killer = sp;
        else if (src instanceof CompanionEntity ce) killer = ce.owner();
        if (killer == null || !(dead instanceof Enemy)) return;
        PlayerRpgData d = RpgManager.data(killer);
        if (!d.registered) return;
        d.monstersKilled++;
        double hp = dead.getMaxHealth();
        long xp = Math.max(3, Math.round(hp * 0.9));
        long money = Math.max(2, Math.round(hp * 0.8));
        RpgManager.grantXp(killer, xp);
        RpgManager.grantMoney(killer, money);
        RpgManager.bar(killer, "+%d XP  +%d Eris", xp, money);
        QuestManager.onKill(killer, dead.getType());
        CompanionManager.react(killer, "kill");
    }

    private ServerEvents() {}
}
