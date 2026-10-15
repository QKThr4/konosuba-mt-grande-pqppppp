package com.konorpg.rpg;

import java.util.List;

public final class Factions {
    public static final List<String> IDS = List.of("guild", "city", "merchants", "axis");

    public static String name(String id) {
        return switch (id) {
            case "guild" -> "Adventurers Guild";
            case "city" -> "Town of Axel";
            case "merchants" -> "Merchants";
            case "axis" -> "Axis Order (Aqua cult)";
            default -> id;
        };
    }

    public static String perk(String id) {
        return switch (id) {
            case "guild" -> "Unlocks higher-rank quests and prestige at the Guild";
            case "city" -> "Friendlier townsfolk, guards respect you";
            case "merchants" -> "Shop discounts (up to 30%)";
            case "axis" -> "Aqua gets more cheerful (and the Order gives gifts)";
            default -> "";
        };
    }

    public static String tier(int rep) {
        if (rep < -20) return "Hated";
        if (rep < 0) return "Distrusted";
        if (rep < 20) return "Neutral";
        if (rep < 60) return "Known";
        if (rep < 120) return "Respected";
        if (rep < 200) return "Honored";
        return "Revered";
    }

    /** Discount (+) or surcharge (-) percent applied to shop prices. */
    public static int shopDiscount(int merchantsRep) {
        return Math.max(-20, Math.min(30, merchantsRep / 4));
    }

    private Factions() {}
}
