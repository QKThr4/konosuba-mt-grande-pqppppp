package com.konorpg.rpg;

public enum Stat {
    STR("STR", "Strength"), INT("INT", "Intelligence"), DEX("DEX", "Agility"),
    VIT("VIT", "Vitality"), MAG("MAG", "Magic"), LUCK("LUCK", "Luck");

    public final String code, label;

    Stat(String code, String label) { this.code = code; this.label = label; }

    public static Stat byName(String s) {
        for (Stat x : values()) if (x.name().equalsIgnoreCase(s)) return x;
        return null;
    }
}
