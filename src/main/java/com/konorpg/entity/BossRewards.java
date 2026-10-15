package com.konorpg.entity;

import com.konorpg.data.PlayerRpgData;
import com.konorpg.data.WorldRpgData;
import com.konorpg.rpg.RpgManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Shared rewards for boss kills: everyone nearby gets XP, Eris, reputation and the boss counter. */
public final class BossRewards {
    public static void onDefeated(LivingEntity boss, int xp, int money, int rep) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        WorldRpgData w = WorldRpgData.get(level.getServer());
        w.lastBossAt = level.getGameTime();
        w.setDirty();
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) > 70 * 70) continue;
            PlayerRpgData d = RpgManager.data(p);
            if (!d.registered) continue;
            d.bossesDefeated++;
            RpgManager.grantXp(p, xp);
            RpgManager.grantMoney(p, money);
            RpgManager.addRep(p, "guild", rep);
            RpgManager.addRep(p, "city", rep / 2);
            RpgManager.title(p, "BOSS DEFEATED!", "%s has fallen", boss.getDisplayName().getString());
            RpgManager.refreshRank(p);
            RpgManager.sync(p);
        }
    }

    private BossRewards() {}
}
