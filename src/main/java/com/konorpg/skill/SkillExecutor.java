package com.konorpg.skill;

import com.konorpg.Config;
import com.konorpg.data.PlayerRpgData;
import com.konorpg.rpg.RpgManager;
import com.konorpg.rpg.RpgStats;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

public final class SkillExecutor {
    public static void cast(ServerPlayer p, String id) {
        PlayerRpgData d = RpgManager.data(p);
        Skill s = Skills.get(id);
        if (s == null || s.action() == null || !d.skills.contains(id)) return;
        if (!Skills.usable(d, s)) {
            RpgManager.bar(p, "Your current class cannot use %s.", s.name());
            return;
        }
        long gt = p.level().getGameTime();
        if (gt < d.incapacitatedUntil) {
            RpgManager.bar(p, "You are incapacitated!");
            return;
        }
        long cd = d.cooldowns.getOrDefault(id, 0L);
        if (cd > gt) {
            RpgManager.bar(p, "%s is recharging: %ds", s.name(), (cd - gt) / 20 + 1);
            return;
        }
        if (d.mana < s.mana()) {
            RpgManager.bar(p, "Not enough mana (%d)", s.mana());
            return;
        }
        SkillCtx ctx = new SkillCtx(p, d);
        ctx.slv = d.skillLevel(id);
        if (!s.action().cast(ctx)) return;
        d.mana = Math.max(0, d.mana - s.mana());
        double cdMul = Math.max(0.55, 1.0 - 0.09 * (ctx.slv - 1)); // upgrades shorten the cooldown
        d.cooldowns.put(id, gt + (long) (s.cooldown() * Config.COOLDOWN_MULT.get() * cdMul));
        RpgManager.sync(p);
    }

    /** Basic staff bolt. Returns true if it was cast. */
    public static boolean castStaff(ServerPlayer p) {
        PlayerRpgData d = RpgManager.data(p);
        if (p.level().getGameTime() < d.incapacitatedUntil) return false;
        if (d.mana < 4) {
            RpgManager.bar(p, "Not enough mana");
            return false;
        }
        SkillCtx c = new SkillCtx(p, d);
        SkillCtx.Hit h = c.ray(24);
        Vec3 from = p.getEyePosition().add(p.getLookAngle().scale(0.8)).add(0, -0.2, 0);
        c.beam(from, h.end(), ParticleTypes.END_ROD);
        if (h.target() != null) c.magic(h.target(), (float) (3 + 2.5 * c.spell()));
        c.sound(SoundEvents.EVOKER_CAST_SPELL, 1.4f);
        d.mana -= 4;
        RpgManager.sync(p);
        return true;
    }

    private SkillExecutor() {}
}
