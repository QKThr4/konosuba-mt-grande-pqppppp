package com.konorpg.shop;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.konorpg.KonoRPG;
import com.konorpg.data.PlayerRpgData;
import com.konorpg.item.RpgGear;
import com.konorpg.network.Net;
import com.konorpg.rpg.Factions;
import com.konorpg.rpg.RpgManager;
import com.konorpg.util.J;
import com.konorpg.util.Lang;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Data-driven shops: buy (rotating stock), sell, craft (recipes) and services (repair / reinforce). */
public final class ShopManager extends SimpleJsonResourceReloadListener {
    public static final ShopManager INSTANCE = new ShopManager();

    private record Entry(String item, int price, boolean rotate) {}
    private record Ing(String item, int count) {}
    private record Recipe(String result, int count, int cost, List<Ing> ings) {}
    private record Shop(String name, List<Entry> buy, boolean sell, Set<String> filter, List<Recipe> recipes, boolean repair, boolean reinforce) {}

    private static final Map<String, Shop> SHOPS = new LinkedHashMap<>();
    private static final Map<UUID, String> SESSION = new HashMap<>();
    private static final int MAX_PLUS = 5;

    private ShopManager() { super(new Gson(), "konorpg_shops"); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller pf) {
        SHOPS.clear();
        for (var e : map.entrySet()) {
            try {
                JsonObject o = e.getValue().getAsJsonObject();
                List<Entry> buy = new ArrayList<>();
                for (JsonElement b : J.arr(o, "buy")) {
                    JsonObject bo = b.getAsJsonObject();
                    buy.add(new Entry(J.str(bo, "item", "minecraft:air"), J.i(bo, "price", 10), J.b(bo, "rotate", false)));
                }
                Set<String> filter = null;
                if (o.has("sellFilter")) { filter = new HashSet<>(); for (JsonElement fe : J.arr(o, "sellFilter")) filter.add(fe.getAsString()); }
                List<Recipe> recipes = new ArrayList<>();
                for (JsonElement r : J.arr(o, "recipes")) {
                    JsonObject ro = r.getAsJsonObject();
                    List<Ing> ings = new ArrayList<>();
                    for (JsonElement ie : J.arr(ro, "ingredients")) {
                        JsonObject io = ie.getAsJsonObject();
                        ings.add(new Ing(J.str(io, "item", "minecraft:air"), J.i(io, "count", 1)));
                    }
                    recipes.add(new Recipe(J.str(ro, "result", "minecraft:air"), J.i(ro, "count", 1), J.i(ro, "cost", 0), ings));
                }
                SHOPS.put(e.getKey().getPath(), new Shop(J.str(o, "name", "Shop"), buy, J.b(o, "sell", true), filter, recipes, J.b(o, "repair", false), J.b(o, "reinforce", false)));
            } catch (Exception ex) {
                KonoRPG.LOGGER.error("Bad shop {}", e.getKey(), ex);
            }
        }
    }

    /** Rotating stock: items flagged "rotate" change every in-game day; the others are always available. */
    private static List<Entry> stock(String shopId, Shop s, ServerPlayer p) {
        long day = p.level().getDayTime() / 24000L;
        Random rnd = new Random(day * 31L + shopId.hashCode());
        List<Entry> out = new ArrayList<>();
        for (Entry e : s.buy()) if (!e.rotate() || rnd.nextInt(100) < 60) out.add(e);
        return out;
    }

    private static int discount(PlayerRpgData d) { return Factions.shopDiscount(d.rep("merchants")); }

    private static int price(int base, PlayerRpgData d) { return Math.max(1, base * (100 - discount(d)) / 100); }

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static int count(ServerPlayer p, String id) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(id)) n += s.getCount();
        return n;
    }

    private static void remove(ServerPlayer p, String id, int amount) {
        for (ItemStack s : p.getInventory().items) {
            if (amount <= 0) return;
            if (!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(id)) { int t = Math.min(amount, s.getCount()); s.shrink(t); amount -= t; }
        }
    }

    // ---------------- services ----------------

    private static int repairCost(ItemStack st) { return st.isDamaged() ? st.getDamageValue() * 2 + 20 : -1; }

    private static boolean canReinforce(ItemStack st) { return !st.isEmpty() && (st.getItem() instanceof SwordItem || st.getItem() instanceof ArmorItem); }

    private static int reinforceCost(ItemStack st) { return canReinforce(st) && RpgGear.reinforce(st) < MAX_PLUS ? 400 * (RpgGear.reinforce(st) + 1) : -1; }

    private static void applyReinforce(ItemStack st) {
        int plus = RpgGear.reinforce(st) + 1;
        ResourceLocation id = KonoRPG.id("reinforce");
        Holder<Attribute> attr;
        EquipmentSlotGroup grp;
        if (st.getItem() instanceof ArmorItem ai) { attr = Attributes.ARMOR; grp = EquipmentSlotGroup.bySlot(ai.getEquipmentSlot()); }
        else { attr = Attributes.ATTACK_DAMAGE; grp = EquipmentSlotGroup.MAINHAND; }
        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry e : st.getAttributeModifiers().modifiers()) {
            if (e.attribute().equals(attr) && e.modifier().id().equals(id)) continue;
            b.add(e.attribute(), e.modifier(), e.slot());
        }
        b.add(attr, new AttributeModifier(id, plus, AttributeModifier.Operation.ADD_VALUE), grp);
        st.set(DataComponents.ATTRIBUTE_MODIFIERS, b.build());
        CustomData.update(DataComponents.CUSTOM_DATA, st, tag -> tag.putInt("konorpg_plus", plus));
    }

    // ---------------- open ----------------

    public static void open(ServerPlayer p, String shopId) { open(p, shopId, "", true); }

    public static void open(ServerPlayer p, String shopId, String result, boolean ok) {
        Shop s = SHOPS.get(shopId);
        if (s == null) { RpgManager.msg(p, "This shop is still under construction (COMING SOON)."); return; }
        SESSION.put(p.getUUID(), shopId);
        PlayerRpgData d = RpgManager.data(p);
        CompoundTag t = new CompoundTag();
        t.putString("name", Lang.tr(p, s.name()));
        t.putLong("money", d.money);
        t.putInt("discount", discount(d));
        t.putString("result", result);
        t.putBoolean("ok", ok);
        ListTag buy = new ListTag();
        for (Entry e : stock(shopId, s, p)) {
            CompoundTag c = new CompoundTag();
            c.putString("item", e.item());
            c.putInt("price", price(e.price(), d));
            buy.add(c);
        }
        t.put("buy", buy);
        ListTag sell = new ListTag();
        if (s.sell()) {
            for (int i = 0; i < p.getInventory().items.size(); i++) {
                ItemStack st = p.getInventory().items.get(i);
                int unit = st.isEmpty() ? 0 : EconomyManager.sellPrice(st.getItem());
                if (unit <= 0 || (s.filter() != null && !s.filter().contains(BuiltInRegistries.ITEM.getKey(st.getItem()).toString()))) continue;
                CompoundTag c = new CompoundTag();
                c.putInt("slot", i);
                c.putString("item", BuiltInRegistries.ITEM.getKey(st.getItem()).toString());
                c.putInt("count", st.getCount());
                c.putInt("unit", unit);
                sell.add(c);
            }
        }
        t.put("sell", sell);
        ListTag rec = new ListTag();
        for (Recipe r : s.recipes()) {
            CompoundTag c = new CompoundTag();
            c.putString("item", r.result());
            c.putInt("count", r.count());
            c.putInt("price", r.cost());
            ListTag ings = new ListTag();
            for (Ing ig : r.ings()) {
                CompoundTag ic = new CompoundTag();
                ic.putString("item", ig.item());
                ic.putInt("need", ig.count());
                ic.putInt("have", count(p, ig.item()));
                ings.add(ic);
            }
            c.put("ings", ings);
            rec.add(c);
        }
        t.put("recipes", rec);
        ListTag svc = new ListTag();
        ItemStack held = p.getMainHandItem();
        if (s.repair()) {
            CompoundTag c = new CompoundTag();
            c.putString("id", "repair");
            c.putInt("price", repairCost(held));
            svc.add(c);
        }
        if (s.reinforce()) {
            CompoundTag c = new CompoundTag();
            c.putString("id", "reinforce");
            c.putInt("price", reinforceCost(held));
            c.putInt("plus", RpgGear.reinforce(held));
            svc.add(c);
        }
        t.put("services", svc);
        t.putString("held", held.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
        Net.openScreen(p, "shop", t);
    }

    // ---------------- actions ----------------

    public static void buy(ServerPlayer p, int idx, int qty) {
        String id = SESSION.get(p.getUUID());
        Shop s = id == null ? null : SHOPS.get(id);
        if (s == null) return;
        List<Entry> list = stock(id, s, p);
        if (idx < 0 || idx >= list.size()) { open(p, id, Lang.tr(p, "Item unavailable."), false); return; }
        qty = Math.max(1, Math.min(qty, 64));
        Entry e = list.get(idx);
        PlayerRpgData d = RpgManager.data(p);
        Item it = item(e.item());
        long cost = (long) price(e.price(), d) * qty;
        if (!RpgManager.spend(p, cost)) { open(p, id, Lang.tr(p, "You do not have enough Eris! (missing %d)", cost - d.money), false); return; }
        com.konorpg.rpg.Perks.give(p, new ItemStack(it, qty));
        RpgManager.sync(p);
        open(p, id, Lang.tr(p, "Purchase complete: %dx %s (-%d Eris)", qty, new ItemStack(it).getHoverName().getString(), cost), true);
    }

    public static void sell(ServerPlayer p, int slot, boolean all) {
        String id = SESSION.get(p.getUUID());
        if (id == null || slot < 0 || slot >= p.getInventory().items.size()) return;
        ItemStack st = p.getInventory().items.get(slot);
        Shop sh = SHOPS.get(id);
        int unit = st.isEmpty() ? 0 : EconomyManager.sellPrice(st.getItem());
        if (unit <= 0 || (sh != null && sh.filter() != null && !sh.filter().contains(BuiltInRegistries.ITEM.getKey(st.getItem()).toString()))) return;
        int n = all ? st.getCount() : 1;
        String nm = st.getHoverName().getString();
        st.shrink(n);
        RpgManager.grantMoney(p, (long) unit * n);
        if (p.getRandom().nextInt(4) == 0) RpgManager.addRep(p, "merchants", 1);
        RpgManager.sync(p);
        open(p, id, Lang.tr(p, "Sold: %dx %s (+%d Eris)", n, nm, unit * n), true);
    }

    public static void craft(ServerPlayer p, int idx) {
        String id = SESSION.get(p.getUUID());
        Shop s = id == null ? null : SHOPS.get(id);
        if (s == null || idx < 0 || idx >= s.recipes().size()) return;
        Recipe r = s.recipes().get(idx);
        for (Ing ig : r.ings()) {
            if (count(p, ig.item()) < ig.count()) { open(p, id, Lang.tr(p, "You are missing ingredients!"), false); return; }
        }
        if (!RpgManager.spend(p, r.cost())) { open(p, id, Lang.tr(p, "You do not have enough Eris! (missing %d)", r.cost() - RpgManager.data(p).money), false); return; }
        for (Ing ig : r.ings()) remove(p, ig.item(), ig.count());
        Item it = item(r.result());
        com.konorpg.rpg.Perks.give(p, new ItemStack(it, r.count()));
        RpgManager.sync(p);
        open(p, id, Lang.tr(p, "Crafted: %dx %s", r.count(), new ItemStack(it).getHoverName().getString()), true);
    }

    public static void service(ServerPlayer p, String svc) {
        String id = SESSION.get(p.getUUID());
        Shop s = id == null ? null : SHOPS.get(id);
        if (s == null) return;
        ItemStack held = p.getMainHandItem();
        if (svc.equals("repair") && s.repair()) {
            int cost = repairCost(held);
            if (cost < 0) { open(p, id, Lang.tr(p, "Hold a damaged item in your main hand."), false); return; }
            if (!RpgManager.spend(p, cost)) { open(p, id, Lang.tr(p, "You do not have enough Eris! (missing %d)", cost - RpgManager.data(p).money), false); return; }
            held.setDamageValue(0);
            RpgManager.sync(p);
            open(p, id, Lang.tr(p, "Repaired! (-%d Eris)", cost), true);
        } else if (svc.equals("reinforce") && s.reinforce()) {
            int cost = reinforceCost(held);
            if (cost < 0) { open(p, id, Lang.tr(p, "Hold a weapon or armor piece (below +%d) in your main hand.", MAX_PLUS), false); return; }
            if (!RpgManager.spend(p, cost)) { open(p, id, Lang.tr(p, "You do not have enough Eris! (missing %d)", cost - RpgManager.data(p).money), false); return; }
            applyReinforce(held);
            RpgManager.refreshAttributes(p);
            RpgManager.sync(p);
            open(p, id, Lang.tr(p, "Reinforced to +%d! (-%d Eris)", RpgGear.reinforce(held), cost), true);
        }
    }

    public static void close(ServerPlayer p) { SESSION.remove(p.getUUID()); }
}
