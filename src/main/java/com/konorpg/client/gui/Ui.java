package com.konorpg.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

public final class Ui {
    public static final int GOLD = 0xFFC9A227, TEXT = 0xFFEEDDBB, DIM = 0xFF998877, BG = 0xE8201810;

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BG);
        g.renderOutline(x, y, w, h, GOLD);
        g.renderOutline(x + 2, y + 2, w - 4, h - 4, 0xFF4A3A18);
    }

    public static void bar(GuiGraphics g, int x, int y, int w, int h, double frac, int color) {
        frac = Math.max(0, Math.min(1, frac));
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + 1 + (int) ((w - 2) * frac), y + h - 1, color);
    }

    public static void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(Minecraft.getInstance().font, s, x, y, color, false);
    }

    public static Component targetName(String target) {
        try {
            ResourceLocation rl = ResourceLocation.parse(target);
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) return BuiltInRegistries.ENTITY_TYPE.get(rl).getDescription();
            if (BuiltInRegistries.ITEM.containsKey(rl)) return BuiltInRegistries.ITEM.get(rl).getDescription();
        } catch (Exception ignored) { }
        return Component.literal(target);
    }

    public static ItemStack stack(String id, int count) {
        try {
            return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)), count);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public static String tr(String en, Object... args) { return com.konorpg.client.ClientLang.tr(en, args); }

    public static String objective(String type, String target, int amount) {
        return switch (type) {
            case "kill", "boss" -> tr("Defeat %dx %s", amount, targetName(target).getString());
            case "collect" -> tr("Collect %dx %s", amount, targetName(target).getString());
            case "explore" -> tr("Reach: %s", tr(switch (target) {
                case "city" -> "the town";
                case "guild" -> "the Guild";
                case "gate" -> "the south gate";
                case "dungeon" -> "the dungeon";
                default -> target;
            }));
            default -> tr("Special objective");
        };
    }

    private Ui() {}
}
