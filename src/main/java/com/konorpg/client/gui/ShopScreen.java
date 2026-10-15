package com.konorpg.client.gui;

import com.konorpg.client.ClientRpgData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Shop screen: Buy / Sell / Craft / Services. Never pauses the game; shows the result of every action. */
public class ShopScreen extends Screen {
    private CompoundTag tag;
    private String tab = "buy";
    private int scroll;
    private long lastClick;

    public ShopScreen(CompoundTag tag) { super(Component.literal("Shop")); this.tag = tag; }

    public void update(CompoundTag t) { tag = t; rebuildWidgets(); }

    private int pw() { return Math.min(width - 10, 380); }

    private int px() { return (width - pw()) / 2; }

    private ListTag list() { return tag.getList(tab.equals("sell") ? "sell" : tab.equals("craft") ? "recipes" : tab.equals("services") ? "services" : "buy", Tag.TAG_COMPOUND); }

    private int rowH() { return tab.equals("craft") ? 30 : 22; }

    private int rows() { return Math.max(1, (height - 100) / rowH()); }

    private void tabButton(String id, String label, int x) {
        Button b = Button.builder(Component.literal(Ui.tr(label)), bt -> { tab = id; scroll = 0; rebuildWidgets(); }).bounds(x, 34, 66, 16).build();
        b.active = !tab.equals(id);
        addRenderableWidget(b);
    }

    @Override
    protected void init() {
        int pw = pw(), x = px(), py = 10;
        int tx = x + 8;
        tabButton("buy", "Buy", tx);
        tabButton("sell", "Sell", tx + 70);
        if (!tag.getList("recipes", Tag.TAG_COMPOUND).isEmpty()) { tabButton("craft", "Craft", tx + 140); tx += 70; }
        if (!tag.getList("services", Tag.TAG_COMPOUND).isEmpty()) tabButton("services", "Services", tx + 140);
        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose()).bounds(x + pw - 24, py + 4, 18, 16).build());
        ListTag list = list();
        int rh = rowH();
        for (int i = 0; i < rows() && i + scroll < list.size(); i++) {
            CompoundTag c = list.getCompound(i + scroll);
            int y = py + 46 + 18 + i * rh;
            final int idx = i + scroll;
            switch (tab) {
                case "sell" -> {
                    final int slot = c.getInt("slot");
                    int unit = c.getInt("unit"), cnt = c.getInt("count");
                    addRenderableWidget(Button.builder(Component.literal("1 (" + unit + ")"), b -> sell(slot, false)).bounds(x + pw - 150, y, 66, 18).build());
                    addRenderableWidget(Button.builder(Component.literal(Ui.tr("All (%d)", unit * cnt)), b -> sell(slot, true)).bounds(x + pw - 82, y, 74, 18).build());
                }
                case "craft" -> {
                    Button b = Button.builder(Component.literal(Ui.tr("Craft (%d)", c.getInt("price"))), bt -> action("shop_craft", "idx", idx)).bounds(x + pw - 110, y + 4, 102, 18).build();
                    boolean ok = true;
                    for (Tag it : c.getList("ings", Tag.TAG_COMPOUND)) if (((CompoundTag) it).getInt("have") < ((CompoundTag) it).getInt("need")) ok = false;
                    b.active = ok;
                    addRenderableWidget(b);
                }
                case "services" -> {
                    int price = c.getInt("price");
                    Button b = Button.builder(Component.literal(price < 0 ? Ui.tr("N/A") : Ui.tr("Do it (%d)", price)), bt -> {
                        if (throttle()) return;
                        CompoundTag t = new CompoundTag();
                        t.putString("id", c.getString("id"));
                        ClientRpgData.send("shop_service", t);
                    }).bounds(x + pw - 110, y, 102, 18).build();
                    b.active = price >= 0;
                    addRenderableWidget(b);
                }
                default -> {
                    int price = c.getInt("price");
                    addRenderableWidget(Button.builder(Component.literal("x1 " + price), b -> buy(idx, 1)).bounds(x + pw - 150, y, 66, 18).build());
                    addRenderableWidget(Button.builder(Component.literal("x5 " + price * 5), b -> buy(idx, 5)).bounds(x + pw - 82, y, 74, 18).build());
                }
            }
        }
    }

    private boolean throttle() {
        long now = System.currentTimeMillis();
        if (now - lastClick < 350) return true;
        lastClick = now;
        return false;
    }

    private void action(String act, String key, int v) {
        if (throttle()) return;
        CompoundTag t = new CompoundTag();
        t.putInt(key, v);
        ClientRpgData.send(act, t);
    }

    private void buy(int idx, int qty) {
        if (throttle()) return;
        CompoundTag t = new CompoundTag();
        t.putInt("idx", idx);
        t.putInt("qty", qty);
        ClientRpgData.send("shop_buy", t);
    }

    private void sell(int slot, boolean all) {
        if (throttle()) return;
        CompoundTag t = new CompoundTag();
        t.putInt("slot", slot);
        t.putBoolean("all", all);
        ClientRpgData.send("shop_sell", t);
    }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float pt) { }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int pw = pw(), x = px();
        Ui.panel(g, x, 10, pw, height - 20);
        Ui.text(g, tag.getString("name"), x + 8, 14, Ui.GOLD);
        Ui.text(g, Ui.tr("%d Eris   (discount: %d%%)", tag.getLong("money"), tag.getInt("discount")), x + pw - 170, 14, Ui.TEXT);
        ListTag list = list();
        if (list.isEmpty()) Ui.text(g, Ui.tr(tab.equals("sell") ? "Nothing to sell (monster items are worth Eris)." : "Nothing here right now."), x + 12, 74, Ui.DIM);
        int rh = rowH();
        for (int i = 0; i < rows() && i + scroll < list.size(); i++) {
            CompoundTag c = list.getCompound(i + scroll);
            int y = 10 + 46 + 18 + i * rh;
            if (tab.equals("services")) {
                boolean rep = c.getString("id").equals("repair");
                Ui.text(g, Ui.tr(rep ? "Repair the item in your main hand" : "Reinforce the weapon/armor in your main hand (+%d now)", c.getInt("plus")), x + 12, y + 5, Ui.TEXT);
                continue;
            }
            ItemStack st = Ui.stack(c.getString("item"), Math.max(1, tab.equals("craft") ? c.getInt("count") : c.getInt("count") == 0 ? 1 : c.getInt("count")));
            g.renderItem(st, x + 10, y);
            g.renderItemDecorations(font, st, x + 10, y);
            Ui.text(g, st.getHoverName().getString(), x + 32, y + 4, Ui.TEXT);
            if (tab.equals("craft")) {
                StringBuilder sb = new StringBuilder();
                for (Tag it : c.getList("ings", Tag.TAG_COMPOUND)) {
                    CompoundTag ic = (CompoundTag) it;
                    sb.append(Ui.stack(ic.getString("item"), 1).getHoverName().getString()).append(' ').append(ic.getInt("have")).append('/').append(ic.getInt("need")).append("  ");
                }
                String s = sb.toString();
                if (s.length() > 58) s = s.substring(0, 58) + "...";
                Ui.text(g, s, x + 32, y + 15, Ui.DIM);
            }
        }
        String res = tag.getString("result");
        if (!res.isEmpty()) Ui.text(g, res, x + 10, height - 26, tag.getBoolean("ok") ? 0xFF66FF66 : 0xFFFF6666);
        if (tab.equals("services")) Ui.text(g, Ui.tr("Held item: %s", Ui.stack(tag.getString("held"), 1).getHoverName().getString()), x + 10, height - 38, Ui.DIM);
        super.render(g, mx, my, pt);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int ns = Math.max(0, Math.min(Math.max(0, list().size() - 1), scroll - (int) Math.signum(sy)));
        if (ns != scroll) { scroll = ns; rebuildWidgets(); }
        return true;
    }

    @Override
    public void onClose() {
        ClientRpgData.send("shop_close");
        super.onClose();
    }
}
