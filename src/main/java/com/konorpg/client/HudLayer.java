package com.konorpg.client;

import com.konorpg.Config;
import com.konorpg.client.gui.Ui;
import com.konorpg.data.PlayerRpgData;
import com.konorpg.rpg.RpgClasses;
import com.konorpg.rpg.RpgStats;
import com.konorpg.skill.Skill;
import com.konorpg.skill.Skills;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.nbt.CompoundTag;

/** Always-on RPG HUD: stats, skill bar, quest tracker and optional adventure progress. */
public class HudLayer implements LayeredDraw.Layer {
    @Override
    public void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        PlayerRpgData d = ClientRpgData.DATA;
        if (!d.introDone) return;
        double max = RpgStats.maxMana(mc.player, d);
        double secs = (System.currentTimeMillis() - ClientRpgData.syncRealMs) / 1000.0;
        double mana = Math.min(max, d.mana + RpgStats.manaRegen(mc.player, d) * Math.min(secs, 3));
        int x = 4, y = 4;
        if (Config.HUD_STATS.get()) {
            Ui.panel(g, x, y, 130, 56);
            Ui.text(g, Ui.tr("Lv %d  %s", d.level, RpgClasses.get(d.classId).name()), x + 6, y + 5, Ui.GOLD);
            Ui.bar(g, x + 6, y + 17, 118, 7, mc.player.getHealth() / mc.player.getMaxHealth(), 0xFFCC2222);
            Ui.bar(g, x + 6, y + 26, 118, 7, mana / Math.max(1, max), 0xFF2266DD);
            Ui.bar(g, x + 6, y + 35, 118, 5, (double) d.xp / RpgStats.xpForNext(d.level), 0xFF66BB44);
            Ui.text(g, Ui.tr("Rank %s   %d Eris", d.rank().name(), d.money), x + 6, y + 43, Ui.TEXT);
            y += 60;
        }
        if (Config.HUD_TRACKER.get() && !d.quests.isEmpty()) {
            CompoundTag q = null;
            for (CompoundTag c : ClientRpgData.QINFO.values()) {
                if (q == null || c.getString("cat").equals("main")) q = c;
            }
            if (q != null) {
                int prog = d.quests.get(q.getString("id")) == null ? 0 : d.quests.get(q.getString("id")).progress;
                Ui.panel(g, x, y, 170, 28);
                Ui.text(g, q.getString("name"), x + 6, y + 5, Ui.GOLD);
                Ui.text(g, prog + "/" + q.getInt("amount") + " " + Ui.objective(q.getString("type"), q.getString("target"), q.getInt("amount")), x + 6, y + 16, Ui.TEXT);
                y += 32;
            }
        }
        if (Config.HUD_PROGRESS.get()) {
            Ui.panel(g, x, y, 150, 62);
            Ui.text(g, Ui.tr("ADVENTURE PROGRESS"), x + 6, y + 5, Ui.GOLD);
            Ui.text(g, Ui.tr("Main Quest: %d/%d", d.mainDone, ClientRpgData.mainTotal), x + 6, y + 17, Ui.TEXT);
            Ui.text(g, Ui.tr("Quests Completed: %d", d.questsCompleted), x + 6, y + 27, Ui.TEXT);
            Ui.text(g, Ui.tr("Dungeons: %d  Bosses: %d", d.dungeonsCleared, d.bossesDefeated), x + 6, y + 37, Ui.TEXT);
            Ui.text(g, Ui.tr("Reputation: %d  Rank: %s", d.rep("guild"), d.rank().name()), x + 6, y + 47, Ui.TEXT);
        }
        if (Config.HUD_SKILLS.get()) {
            int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
            long gt = mc.level == null ? 0 : mc.level.getGameTime();
            String[] keys = {"Z", "X", "C", "V"};
            for (int i = 0; i < 4; i++) {
                int bx = sw - 3 - (4 - i) * 25, by = sh - 21;
                g.fill(bx, by, bx + 23, by + 19, 0xAA000000);
                g.renderOutline(bx, by, 23, 19, Ui.GOLD);
                Skill s = d.slots[i].isEmpty() ? null : Skills.get(d.slots[i]);
                Ui.text(g, keys[i], bx + 2, by + 2, Ui.GOLD);
                if (s != null) {
                    String n = s.name().length() > 3 ? s.name().substring(0, 3) : s.name();
                    long cd = d.cooldowns.getOrDefault(s.id(), 0L) - gt;
                    Ui.text(g, n, bx + 11, by + 2, cd > 0 ? Ui.DIM : Ui.TEXT);
                    Ui.text(g, cd > 0 ? (cd / 20 + 1) + "s" : String.valueOf(s.mana()), bx + 2, by + 10, cd > 0 ? 0xFFCC4444 : 0xFF6688FF);
                }
            }
        }
        // health of whatever you are looking at (NPCs, companions, monsters)
        if (mc.crosshairPickEntity instanceof net.minecraft.world.entity.LivingEntity le && le.isAlive()) {
            int sw = mc.getWindow().getGuiScaledWidth();
            int w = 130, tx = sw / 2 - w / 2, ty = 4;
            Ui.panel(g, tx, ty, w, 24);
            String nm = le.getDisplayName().getString();
            if (nm.length() > 22) nm = nm.substring(0, 22);
            Ui.text(g, nm, tx + 6, ty + 4, Ui.GOLD);
            Ui.bar(g, tx + 6, ty + 14, w - 40, 6, le.getHealth() / Math.max(1f, le.getMaxHealth()), 0xFFCC2222);
            Ui.text(g, (int) Math.ceil(le.getHealth()) + "/" + (int) le.getMaxHealth(), tx + w - 30, ty + 13, Ui.TEXT);
        }
    }
}
