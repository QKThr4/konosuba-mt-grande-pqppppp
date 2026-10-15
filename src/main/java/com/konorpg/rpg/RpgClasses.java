package com.konorpg.rpg;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Class registry. Add new classes with register(...) (also usable by other mods / future content). */
public final class RpgClasses {
    private static final Map<String, RpgClass> ALL = new LinkedHashMap<>();

    public static void register(RpgClass c) { ALL.put(c.id(), c); }

    public static RpgClass get(String id) { return ALL.getOrDefault(id, ALL.get("adventurer")); }

    public static boolean exists(String id) { return ALL.containsKey(id); }

    public static Collection<RpgClass> all() { return ALL.values(); }

    /** Skill groups a class may use: general + own id + every ancestor. */
    public static Set<String> lines(String classId) {
        Set<String> out = new HashSet<>();
        out.add("general");
        String cur = classId;
        int guard = 0;
        while (cur != null && !cur.isEmpty() && guard++ < 8) {
            out.add(cur);
            RpgClass c = ALL.get(cur);
            cur = c == null ? null : c.parent();
        }
        return out;
    }

    static {
        register(new RpgClass("adventurer", "Adventurer", 0, "", "Starting class. Weak at everything, good at nothing... but can learn general tricks.", 1, 0, Rank.F, Stat.LUCK, Stat.DEX));
        register(new RpgClass("warrior", "Warrior", 1, "adventurer", "Melee fighter. Lots of strength and health.", 1, 0, Rank.F, Stat.STR, Stat.VIT));
        register(new RpgClass("knight", "Knight", 1, "adventurer", "Defender with shield and charge. Sturdy.", 1, 0, Rank.F, Stat.VIT, Stat.STR));
        register(new RpgClass("mage", "Mage", 1, "adventurer", "Caster of elemental spells.", 1, 0, Rank.F, Stat.INT, Stat.MAG));
        register(new RpgClass("priest", "Priest", 1, "adventurer", "Healing and blessings. Great against undead.", 1, 0, Rank.F, Stat.MAG, Stat.INT));
        register(new RpgClass("archer", "Archer", 1, "adventurer", "Precise ranged attacks.", 1, 0, Rank.F, Stat.DEX, Stat.LUCK));
        register(new RpgClass("thief", "Thief", 1, "adventurer", "Stealth, stealing and escapes. Relies on luck.", 1, 0, Rank.F, Stat.LUCK, Stat.DEX));

        register(new RpgClass("archwizard", "Arch Wizard", 2, "mage", "Master of Explosion. Absurd power, plenty of mana... for a single shot.", 15, 5000, Rank.D, Stat.INT, Stat.MAG));
        register(new RpgClass("crusader", "Crusader", 2, "knight", "Withstands anything and draws every enemy (Decoy).", 15, 5000, Rank.D, Stat.VIT, Stat.STR));
        register(new RpgClass("swordmaster", "Sword Master", 2, "warrior", "Blade dances and devastating strikes.", 15, 5000, Rank.D, Stat.STR, Stat.DEX));
        register(new RpgClass("highpriest", "High Priest", 2, "priest", "Powerful area healing.", 15, 5000, Rank.D, Stat.MAG, Stat.INT));
        register(new RpgClass("assassin", "Assassin", 2, "thief", "Backstabs with critical damage.", 15, 5000, Rank.D, Stat.DEX, Stat.LUCK));
        register(new RpgClass("necromancer", "Necromancer", 2, "mage", "Drains life from enemies with dark magic.", 18, 7500, Rank.C, Stat.INT, Stat.MAG));
        register(new RpgClass("battlemage", "Battle Mage", 2, "mage", "Mixes sword and magic.", 18, 7500, Rank.C, Stat.STR, Stat.INT));
        register(new RpgClass("paladin", "Paladin", 2, "knight", "Holy knight: light against darkness.", 20, 9000, Rank.C, Stat.VIT, Stat.MAG));
    }

    private RpgClasses() {}
}
