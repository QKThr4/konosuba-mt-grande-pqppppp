package com.konorpg.registry;

import com.konorpg.KonoRPG;
import com.konorpg.item.RpgArmorItem;
import com.konorpg.rpg.RpgRarity;
import com.konorpg.rpg.Stat;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** RPG armor sets: Adventurer (common), Mage robes, Knight (steel) and Crusader (rare). */
public final class ModArmor {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, KonoRPG.MOD_ID);

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> material(String name, int boots, int legs, int chest, int helmet, int ench, float toughness) {
        return MATERIALS.register(name, () -> {
            Map<ArmorItem.Type, Integer> def = new EnumMap<>(ArmorItem.Type.class);
            def.put(ArmorItem.Type.BOOTS, boots);
            def.put(ArmorItem.Type.LEGGINGS, legs);
            def.put(ArmorItem.Type.CHESTPLATE, chest);
            def.put(ArmorItem.Type.HELMET, helmet);
            def.put(ArmorItem.Type.BODY, chest);
            return new ArmorMaterial(def, ench, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Items.IRON_INGOT),
                    List.of(new ArmorMaterial.Layer(KonoRPG.id(name))), toughness, 0.0f);
        });
    }

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ADVENTURER = material("adventurer", 1, 2, 3, 1, 12, 0.0f);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MAGE = material("mage", 1, 2, 3, 1, 22, 0.0f);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> KNIGHT = material("knight", 2, 4, 5, 2, 15, 1.0f);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CRUSADER = material("crusader", 3, 5, 7, 3, 18, 2.0f);

    private static DeferredItem<RpgArmorItem> piece(String name, Holder<ArmorMaterial> mat, ArmorItem.Type type, int dur, RpgRarity r, Map<Stat, Integer> b) {
        return ModItems.ITEMS.register(name, () -> new RpgArmorItem(mat, type, dur, r, b));
    }

    // Adventurer
    public static final DeferredItem<RpgArmorItem> ADVENTURER_HELMET = piece("adventurer_helmet", ADVENTURER, ArmorItem.Type.HELMET, 12, RpgRarity.COMMON, Map.of());
    public static final DeferredItem<RpgArmorItem> ADVENTURER_CHESTPLATE = piece("adventurer_chestplate", ADVENTURER, ArmorItem.Type.CHESTPLATE, 12, RpgRarity.COMMON, Map.of(Stat.VIT, 1));
    public static final DeferredItem<RpgArmorItem> ADVENTURER_LEGGINGS = piece("adventurer_leggings", ADVENTURER, ArmorItem.Type.LEGGINGS, 12, RpgRarity.COMMON, Map.of());
    public static final DeferredItem<RpgArmorItem> ADVENTURER_BOOTS = piece("adventurer_boots", ADVENTURER, ArmorItem.Type.BOOTS, 12, RpgRarity.COMMON, Map.of(Stat.DEX, 1));
    // Mage
    public static final DeferredItem<RpgArmorItem> MAGE_HELMET = piece("mage_helmet", MAGE, ArmorItem.Type.HELMET, 14, RpgRarity.UNCOMMON, Map.of(Stat.INT, 2));
    public static final DeferredItem<RpgArmorItem> MAGE_CHESTPLATE = piece("mage_chestplate", MAGE, ArmorItem.Type.CHESTPLATE, 14, RpgRarity.UNCOMMON, Map.of(Stat.INT, 3, Stat.MAG, 2));
    public static final DeferredItem<RpgArmorItem> MAGE_LEGGINGS = piece("mage_leggings", MAGE, ArmorItem.Type.LEGGINGS, 14, RpgRarity.UNCOMMON, Map.of(Stat.MAG, 1));
    public static final DeferredItem<RpgArmorItem> MAGE_BOOTS = piece("mage_boots", MAGE, ArmorItem.Type.BOOTS, 14, RpgRarity.UNCOMMON, Map.of(Stat.INT, 1, Stat.DEX, 1));
    // Knight
    public static final DeferredItem<RpgArmorItem> KNIGHT_HELMET = piece("knight_helmet", KNIGHT, ArmorItem.Type.HELMET, 22, RpgRarity.UNCOMMON, Map.of(Stat.VIT, 1));
    public static final DeferredItem<RpgArmorItem> KNIGHT_CHESTPLATE = piece("knight_chestplate", KNIGHT, ArmorItem.Type.CHESTPLATE, 22, RpgRarity.UNCOMMON, Map.of(Stat.VIT, 2, Stat.STR, 1));
    public static final DeferredItem<RpgArmorItem> KNIGHT_LEGGINGS = piece("knight_leggings", KNIGHT, ArmorItem.Type.LEGGINGS, 22, RpgRarity.UNCOMMON, Map.of(Stat.VIT, 1));
    public static final DeferredItem<RpgArmorItem> KNIGHT_BOOTS = piece("knight_boots", KNIGHT, ArmorItem.Type.BOOTS, 22, RpgRarity.UNCOMMON, Map.of(Stat.DEX, 1));
    // Crusader
    public static final DeferredItem<RpgArmorItem> CRUSADER_HELMET = piece("crusader_helmet", CRUSADER, ArmorItem.Type.HELMET, 32, RpgRarity.RARE, Map.of(Stat.VIT, 2));
    public static final DeferredItem<RpgArmorItem> CRUSADER_CHESTPLATE = piece("crusader_chestplate", CRUSADER, ArmorItem.Type.CHESTPLATE, 32, RpgRarity.RARE, Map.of(Stat.VIT, 3, Stat.STR, 2));
    public static final DeferredItem<RpgArmorItem> CRUSADER_LEGGINGS = piece("crusader_leggings", CRUSADER, ArmorItem.Type.LEGGINGS, 32, RpgRarity.RARE, Map.of(Stat.VIT, 2, Stat.STR, 1));
    public static final DeferredItem<RpgArmorItem> CRUSADER_BOOTS = piece("crusader_boots", CRUSADER, ArmorItem.Type.BOOTS, 32, RpgRarity.RARE, Map.of(Stat.DEX, 2, Stat.LUCK, 1));

    private ModArmor() {}
}
