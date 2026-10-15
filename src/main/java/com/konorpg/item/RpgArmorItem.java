package com.konorpg.item;

import com.konorpg.rpg.RpgRarity;
import com.konorpg.rpg.Stat;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** RPG armor piece: normal armor plus stat bonuses that count while it is worn. */
public class RpgArmorItem extends ArmorItem implements RpgGear {
    private final RpgRarity rarity;
    private final Map<Stat, Integer> bonuses;

    public RpgArmorItem(Holder<ArmorMaterial> material, ArmorItem.Type type, int durabilityMul, RpgRarity rarity, Map<Stat, Integer> bonuses) {
        super(material, type, new Item.Properties().durability(type.getDurability(durabilityMul)).rarity(rarity.vanilla));
        this.rarity = rarity;
        this.bonuses = bonuses;
    }

    @Override public RpgRarity rarity() { return rarity; }
    @Override public Map<Stat, Integer> bonuses() { return bonuses; }

    @Override
    public Component getName(ItemStack stack) {
        int plus = RpgGear.reinforce(stack);
        Component base = super.getName(stack).copy().withStyle(rarity.color);
        return plus > 0 ? Component.empty().append(base).append(Component.literal(" +" + plus)) : base;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        RpgGear.tooltip(this, stack, lines);
        super.appendHoverText(stack, ctx, lines, flag);
    }
}
