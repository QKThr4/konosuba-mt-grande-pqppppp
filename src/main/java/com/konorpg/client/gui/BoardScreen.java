package com.konorpg.client.gui;

import com.konorpg.client.ClientRpgData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

/** The Guild quest board. */
public class BoardScreen extends Screen {
    private CompoundTag tag;
    private String selected = "";
    private int scroll;

    public BoardScreen(CompoundTag tag) { super(Component.literal("Quest Board")); this.tag = tag; }

    public void update(CompoundTag t) { tag = t; rebuildWidgets(); }

    private CompoundTag find(String id) {
        ListTag l = tag.getList("quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) if (l.getCompound(i).getString("id").equals(id)) return l.getCompound(i);
        return null;
    }

    @Override
    protected void init() {
        int pw = Math.min(width - 10, 440), x = (width - pw) / 2;
        ListTag l = tag.getList("quests", Tag.TAG_COMPOUND);
        int rows = Math.max(1, (height - 60) / 20);
        for (int i = 0; i < rows && i + scroll < l.size(); i++) {
            CompoundTag q = l.getCompound(i + scroll);
            String id = q.getString("id");
            int st = q.getInt("status");
            String mark = st == 1 ? Ui.tr("[ACTIVE] ") : st == 2 ? Ui.tr("[READY] ") : st == 3 ? Ui.tr("[LOCKED] ") : "";
            addRenderableWidget(Button.builder(Component.literal(mark + q.getString("name")), b -> { selected = id; rebuildWidgets(); })
                    .bounds(x + 6, 34 + i * 20, 170, 18).build());
        }
        CompoundTag q = find(selected);
        if (q != null) {
            int st = q.getInt("status");
            int bx = x + 184, by = height - 38;
            if (st == 0) addRenderableWidget(Button.builder(Component.literal(Ui.tr("Accept")), b -> send("quest_accept")).bounds(bx, by, 80, 20).build());
            if (st == 1 || st == 2) {
                if (!q.getString("cat").equals("main")) addRenderableWidget(Button.builder(Component.literal(Ui.tr("Abandon")), b -> send("quest_abandon")).bounds(bx, by, 80, 20).build());
            }
        }
        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose()).bounds(x + pw - 24, 8, 18, 16).build());
    }

    private void send(String act) {
        CompoundTag t = new CompoundTag();
        t.putString("id", selected);
        ClientRpgData.send(act, t);
        ClientRpgData.send("board_open");
    }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float pt) { }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int pw = Math.min(width - 10, 440), x = (width - pw) / 2;
        Ui.panel(g, x, 4, pw, height - 8);
        Ui.text(g, Ui.tr("QUEST BOARD - accept here; collect rewards from Luna"), x + 8, 10, Ui.GOLD);
        CompoundTag q = find(selected);
        int dx = x + 184, dw = pw - 192;
        if (q == null) {
            Ui.text(g, Ui.tr("Select a quest."), dx, 40, Ui.DIM);
        } else {
            Ui.text(g, q.getString("name"), dx, 36, Ui.GOLD);
            int ly = 50;
            for (var seq : font.split(Component.literal(q.getString("desc")), dw)) { g.drawString(font, seq, dx, ly, Ui.TEXT, false); ly += 10; }
            ly += 4;
            Ui.text(g, Ui.tr("Objective: %s", Ui.objective(q.getString("type"), q.getString("target"), q.getInt("amount"))), dx, ly, 0xFF88CCFF); ly += 11;
            Ui.text(g, Ui.tr("Progress: %d/%d", q.getInt("progress"), q.getInt("amount")), dx, ly, Ui.TEXT); ly += 11;
            Ui.text(g, Ui.tr("Location: %s", q.getString("loc")), dx, ly, Ui.TEXT); ly += 11;
            Ui.text(g, Ui.tr("Reward: %d XP, %d Eris", q.getInt("xp"), q.getInt("money")), dx, ly, 0xFFAAFFAA); ly += 11;
            if (!q.getString("rep").isEmpty()) { Ui.text(g, Ui.tr("Reputation: %s", q.getString("rep")), dx, ly, 0xFFAAFFAA); ly += 11; }
            Ui.text(g, Ui.tr("Difficulty: %s   Min. rank: %s", "*".repeat(Math.max(1, q.getInt("diff"))), q.getString("rank"))
                    + (q.getInt("timeMin") > 0 ? Ui.tr("   Time: %d min", q.getInt("timeMin")) : ""), dx, ly, Ui.TEXT); ly += 12;
            if (!q.getString("humor").isEmpty())
                for (var seq : font.split(Component.literal(q.getString("humor")), dw)) { g.drawString(font, seq, dx, ly, Ui.DIM, false); ly += 10; }
            if (q.getInt("status") == 3) Ui.text(g, q.getString("reason"), dx, height - 52, 0xFFFF7777);
            if (q.getInt("status") == 2) Ui.text(g, Ui.tr("Ready! Talk to Luna (receptionist) to collect."), dx, height - 52, 0xFF66FF66);
        }
        super.render(g, mx, my, pt);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int size = tag.getList("quests", Tag.TAG_COMPOUND).size();
        int ns = Math.max(0, Math.min(Math.max(0, size - 1), scroll - (int) Math.signum(sy)));
        if (ns != scroll) { scroll = ns; rebuildWidgets(); }
        return true;
    }
}
