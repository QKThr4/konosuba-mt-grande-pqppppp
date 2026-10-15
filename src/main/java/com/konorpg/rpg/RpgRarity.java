package com.konorpg.rpg;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;

public enum RpgRarity {
    COMMON("Common", ChatFormatting.WHITE, Rarity.COMMON),
    UNCOMMON("Uncommon", ChatFormatting.GREEN, Rarity.UNCOMMON),
    RARE("Rare", ChatFormatting.AQUA, Rarity.RARE),
    EPIC("Epic", ChatFormatting.LIGHT_PURPLE, Rarity.EPIC),
    LEGENDARY("Legendary", ChatFormatting.GOLD, Rarity.EPIC),
    MYTHIC("Mythic", ChatFormatting.RED, Rarity.EPIC);

    public final String label;
    public final ChatFormatting color;
    public final Rarity vanilla;

    RpgRarity(String label, ChatFormatting color, Rarity vanilla) {
        this.label = label; this.color = color; this.vanilla = vanilla;
    }
}
