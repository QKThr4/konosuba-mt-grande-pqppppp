package com.konorpg.client.gui;

import com.konorpg.Config;
import com.konorpg.client.ClientRpgData;
import com.konorpg.data.CompanionRecord;
import com.konorpg.data.PlayerRpgData;
import com.konorpg.rpg.Factions;
import com.konorpg.rpg.Friendship;
import com.konorpg.rpg.RpgClass;
import com.konorpg.rpg.RpgClasses;
import com.konorpg.rpg.RpgStats;
import com.konorpg.rpg.Stat;
import com.konorpg.skill.Skill;
import com.konorpg.skill.Skills;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * General hub: one screen with a button per panel (Status, Skills, Quests, Reputation, Companion, Card,
 * Equipment, Guild, Settings/HUD) plus a shortcut to the vanilla inventory.
 */
public class RpgHubScreen extends Screen {
    private static final String[][] TABS = {
            {"status", "STATUS"}, {"skills", "SKILLS"}, {"quests", "QUESTS"}, {"inventory", "INVENTORY"},
            {"companion", "COMPANION"}, {"reputation", "REPUTATION"}, {"card", "ADVENTURER CARD"},
            {"equip", "EQUIPMENT"}, {"guild", "GUILD"}, {"settings", "HUD / SETTINGS"}};

    private String tab;
    private int scroll;
    private int px, py, pw, ph, cx, cw;

    public RpgHubScreen(String tab) {
        super(Component.literal("Adventurer Panel"));
        this.tab = tab == null || tab.isEmpty() ? "status" : tab;
    }

    public void switchTab(String t) { tab = t; scroll = 0; rebuildWidgets(); }

    private PlayerRpgData d() { return ClientRpgData.DATA; }

    private static String tr(String en, Object... a) { return Ui.tr(en, a); }

    @Override
    protected void init() {
        pw = Math.min(width - 6, 480);
        ph = Math.min(height - 6, 255);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        cx = px + 100;
        cw = pw - 108;
        int y = py + 8;
        for (String[] t : TABS) {
            final String id = t[0];
            Button b = Button.builder(Component.literal(tr(t[1])), x -> {
                if (id.equals("inventory")) { Minecraft.getInstance().setScreen(new InventoryScreen(Minecraft.getInstance().player)); return; }
                switchTab(id);
            }).bounds(px + 6, y, 88, 18).build();
            b.active = !id.equals(tab);
            addRenderableWidget(b);
            y += 20;
        }
        initTab();
    }

    private void send(String act, String key, String val) {
        CompoundTag t = new CompoundTag();
        t.putString(key, val);
        ClientRpgData.send(act, t);
    }

    private List<Skill> skillList() {
        List<Skill> out = new ArrayList<>();
        var lines = RpgClasses.lines(d().classId);
        for (Skill s : Skills.all()) if (lines.contains(s.group()) || d().skills.contains(s.id())) out.add(s);
        return out;
    }

    private List<RpgClass> classList() {
        List<RpgClass> cl = new ArrayList<>();
        for (RpgClass c : RpgClasses.all()) if (!c.id().equals("adventurer")) cl.add(c);
        return cl;
    }

    private void initTab() {
        PlayerRpgData d = d();
        switch (tab) {
            case "status" -> {
                if (d.statPoints > 0) {
                    int y = py + 70;
                    for (Stat s : Stat.values()) {
                        addRenderableWidget(Button.builder(Component.literal("+"), b -> send("stat_add", "stat", s.name())).bounds(cx + 170, y - 2, 14, 12).build());
                        y += 14;
                    }
                }
            }
            case "skills" -> {
                List<Skill> list = skillList();
                int rows = Math.max(1, (ph - 56) / 22);
                for (int i = 0; i < rows && i + scroll < list.size(); i++) {
                    Skill s = list.get(i + scroll);
                    int y = py + 40 + i * 22;
                    if (d.skills.contains(s.id())) {
                        if (s.type() != Skill.Type.PASSIVE) {
                            int cur = -1;
                            for (int k = 0; k < 4; k++) if (d.slots[k].equals(s.id())) cur = k;
                            final int next = cur + 1;
                            addRenderableWidget(Button.builder(Component.literal(cur < 0 ? tr("Assign key") : tr("Key %s", String.valueOf("ZXCV".charAt(cur)))), b -> {
                                CompoundTag t = new CompoundTag();
                                t.putString("id", next > 3 ? "" : s.id());
                                t.putInt("slot", next > 3 ? 0 : next);
                                if (next > 3) { for (int k = 0; k < 4; k++) if (d.slots[k].equals(s.id())) t.putInt("slot", k); }
                                ClientRpgData.send("skill_bind", t);
                            }).bounds(cx + cw - 90, y, 86, 18).build());
                            int lv = d.skillLevel(s.id());
                            int cost = s.spCost() + lv;
                            Button up = Button.builder(Component.literal(lv >= 5 ? tr("Max level") : tr("Upgrade (%d SP)", cost)), x -> send("skill_upgrade", "id", s.id())).bounds(cx + cw - 196, y, 102, 18).build();
                            up.active = lv < 5 && d.skillPoints >= cost;
                            addRenderableWidget(up);
                        }
                    } else {
                        Button b = Button.builder(Component.literal(tr("Learn (%d SP)", s.spCost())), x -> send("skill_learn", "id", s.id())).bounds(cx + cw - 110, y, 106, 18).build();
                        b.active = Skills.canLearn(d, s);
                        addRenderableWidget(b);
                    }
                }
            }
            case "quests" -> {
                addRenderableWidget(Button.builder(Component.literal(tr("Verify")), b -> ClientRpgData.send("quest_check")).bounds(cx + cw - 70, py + 6, 66, 16).build());
                int y = py + 60;
                for (String id : d.quests.keySet()) {
                    final String qid = id;
                    var qi = ClientRpgData.QINFO.get(id);
                    if (qi == null) continue;
                    Button ab = Button.builder(Component.literal(tr("Abandon")), b -> send("quest_abandon", "id", qid)).bounds(cx + cw - 70, y, 66, 14).build();
                    ab.active = !qi.getString("cat").equals("main");
                    addRenderableWidget(ab);
                    y += 30;
                    if (y > py + ph - 20) break;
                }
            }
            case "companion" -> {
                CompanionRecord r = d.companion("aqua");
                if (r != null) {
                    final String kind = r.kind;
                    addRenderableWidget(Button.builder(Component.literal(r.stay ? tr("Make her follow") : tr("Make her stay")), b -> ClientRpgData.send("companion_stay")).bounds(cx + 4, py + 76, 100, 16).build());
                    addRenderableWidget(Button.builder(Component.literal(tr("Teleport: %s", r.noTeleport ? "OFF" : "ON")), b -> send("comp_teleport", "kind", kind)).bounds(cx + 108, py + 76, 100, 16).build());
                    addRenderableWidget(Button.builder(Component.literal(tr("Feed (item in hand)")), b -> send("comp_feed", "kind", kind)).bounds(cx + 4, py + 96, 204, 16).build());
                }
            }
            case "equip" -> {
                for (int i = 0; i < 3; i++) {
                    final int slot = i;
                    addRenderableWidget(Button.builder(Component.literal(tr("Remove")), b -> {
                        CompoundTag t = new CompoundTag();
                        t.putInt("slot", slot);
                        ClientRpgData.send("acc_unequip", t);
                    }).bounds(cx + cw - 60, py + 46 + i * 26, 54, 16).build());
                }
                addRenderableWidget(Button.builder(Component.literal(tr("Equip item in hand")), b -> ClientRpgData.send("acc_equip")).bounds(cx + 4, py + 130, 130, 16).build());
            }
            case "guild" -> {
                if (!d.registered) {
                    addRenderableWidget(Button.builder(Component.literal(tr("Pay fee and register (%d Eris)", Config.REG_FEE.get())), b -> ClientRpgData.send("register")).bounds(cx + 4, py + 90, 220, 18).build());
                } else {
                    List<RpgClass> cl = classList();
                    int rows = Math.max(1, (ph - 64) / 20);
                    for (int i = 0; i < rows && i + scroll < cl.size(); i++) {
                        RpgClass c = cl.get(i + scroll);
                        Button b = Button.builder(Component.literal(c.id().equals(d.classId) ? tr("Current") : tr("Choose")), x -> send("class_set", "id", c.id())).bounds(cx + cw - 64, py + 44 + i * 20, 60, 16).build();
                        b.active = !c.id().equals(d.classId);
                        addRenderableWidget(b);
                    }
                }
            }
            case "settings" -> {
                addToggle(py + 40, "Stats (HP/Mana/XP)", Config.HUD_STATS);
                addToggle(py + 62, "Adventure progress", Config.HUD_PROGRESS);
                addToggle(py + 84, "Skill bar", Config.HUD_SKILLS);
                addToggle(py + 106, "Quest tracker", Config.HUD_TRACKER);
            }
            default -> { }
        }
    }

    private void addToggle(int y, String label, ModConfigSpec.BooleanValue v) {
        addRenderableWidget(Button.builder(Component.literal(tr(label) + ": " + (v.get() ? "ON" : "OFF")), b -> {
            v.set(!v.get());
            Config.CLIENT_SPEC.save();
            rebuildWidgets();
        }).bounds(cx + 4, y, 200, 18).build());
    }

    private static String clip(String s, int n) { return s.length() > n ? s.substring(0, n) + "..." : s; }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float pt) { }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x88000000);
        Ui.panel(g, px, py, pw, ph);
        g.fill(px + 96, py + 6, px + 97, py + ph - 6, Ui.GOLD);
        PlayerRpgData d = d();
        int y = py + 8;
        var p = Minecraft.getInstance().player;
        switch (tab) {
            case "status" -> {
                RpgClass c = RpgClasses.get(d.classId);
                Ui.text(g, p.getGameProfile().getName() + " - " + c.name(), cx, y, Ui.GOLD);
                Ui.text(g, tr("Level %d   Rank %s   %d Eris", d.level, d.rank().name(), d.money), cx, y + 12, Ui.TEXT);
                Ui.bar(g, cx, y + 26, 150, 8, (double) d.xp / RpgStats.xpForNext(d.level), 0xFF66BB44);
                Ui.text(g, "XP " + d.xp + "/" + RpgStats.xpForNext(d.level), cx + 156, y + 26, Ui.DIM);
                Ui.bar(g, cx, y + 38, 150, 8, p.getHealth() / p.getMaxHealth(), 0xFFCC2222);
                Ui.text(g, "HP " + (int) p.getHealth() + "/" + (int) p.getMaxHealth(), cx + 156, y + 38, Ui.DIM);
                double mm = RpgStats.maxMana(p, d);
                Ui.bar(g, cx, y + 50, 150, 8, d.mana / mm, 0xFF2266DD);
                Ui.text(g, "MP " + (int) d.mana + "/" + (int) mm, cx + 156, y + 50, Ui.DIM);
                int sy = py + 70;
                for (Stat s : Stat.values()) {
                    int eff = RpgStats.eff(p, d, s);
                    Ui.text(g, s.code + " (" + tr(s.label) + "): " + eff + (eff != d.stat(s) ? tr(" (base %d)", d.stat(s)) : ""), cx, sy, Ui.TEXT);
                    sy += 14;
                }
                Ui.text(g, tr("Attribute points: %d   Skill points: %d", d.statPoints, d.skillPoints), cx, sy + 4, Ui.GOLD);
                Ui.text(g, tr("Critical: %d%%   Spell power x%s", (int) (RpgStats.critChance(p, d) * 100), String.format("%.2f", RpgStats.spellPower(p, d))), cx, sy + 18, Ui.DIM);
            }
            case "skills" -> {
                Ui.text(g, tr("SKILLS - %s   (SP: %d)", RpgClasses.get(d.classId).name(), d.skillPoints), cx, y, Ui.GOLD);
                Ui.text(g, tr("Default keys: Z X C V (configurable)"), cx, y + 12, Ui.DIM);
                List<Skill> list = skillList();
                int rows = Math.max(1, (ph - 56) / 22);
                for (int i = 0; i < rows && i + scroll < list.size(); i++) {
                    Skill s = list.get(i + scroll);
                    int sy = py + 40 + i * 22;
                    boolean learned = d.skills.contains(s.id());
                    String lv = learned && s.type() != Skill.Type.PASSIVE ? " [Lv " + d.skillLevel(s.id()) + "/5]" : "";
                    Ui.text(g, (learned ? "* " : "  ") + s.name() + lv + "  [" + tr(s.type().name().toLowerCase()) + ", " + tr("req. lvl %d", s.minLevel()) + (s.mana() > 0 ? ", " + s.mana() + " MP" : "") + "]", cx, sy, learned ? 0xFFAAFFAA : Ui.TEXT);
                    Ui.text(g, clip(tr(s.desc()), 58), cx, sy + 10, Ui.DIM);
                }
            }
            case "quests" -> {
                Ui.text(g, tr("ACTIVE QUESTS"), cx, y, Ui.GOLD);
                Ui.text(g, tr("Main Quest: %d/%d   Completed: %d", d.mainDone, ClientRpgData.mainTotal, d.questsCompleted), cx, y + 12, Ui.TEXT);
                Ui.text(g, tr("Rewards: talk to the receptionist Luna."), cx, y + 24, Ui.DIM);
                int qy = py + 60;
                if (d.quests.isEmpty()) Ui.text(g, tr("No active quests. Accept quests at the Guild's QUEST BOARD."), cx, qy, Ui.DIM);
                for (var e : d.quests.entrySet()) {
                    var qi = ClientRpgData.QINFO.get(e.getKey());
                    if (qi == null) continue;
                    Ui.text(g, qi.getString("name") + (qi.getString("cat").equals("main") ? tr(" [MAIN]") : ""), cx, qy - 12, Ui.GOLD);
                    Ui.text(g, Ui.objective(qi.getString("type"), qi.getString("target"), qi.getInt("amount")) + "  (" + e.getValue().progress + "/" + qi.getInt("amount") + ")", cx, qy + 1, Ui.TEXT);
                    qy += 30;
                    if (qy > py + ph - 20) break;
                }
            }
            case "reputation" -> {
                Ui.text(g, tr("REPUTATION"), cx, y, Ui.GOLD);
                int ry = py + 30;
                for (String f : Factions.IDS) {
                    int v = d.rep(f);
                    Ui.text(g, tr(Factions.name(f)) + ": " + v + " (" + tr(Factions.tier(v)) + ")", cx, ry, Ui.TEXT);
                    Ui.bar(g, cx, ry + 11, 160, 6, (v + 20) / 240.0, v < 0 ? 0xFFCC2222 : 0xFFCCAA22);
                    Ui.text(g, tr(Factions.perk(f)), cx, ry + 20, Ui.DIM);
                    ry += 36;
                }
            }
            case "companion" -> {
                Ui.text(g, tr("COMPANIONS"), cx, y, Ui.GOLD);
                if (d.companions.isEmpty()) Ui.text(g, tr(d.perk.equals("aqua") ? "Aqua is with you in Axel." : "No companions yet."), cx, y + 16, Ui.DIM);
                int cy = py + 26;
                long gt = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
                for (CompanionRecord r : d.companions) {
                    Ui.text(g, r.name + "  " + tr("Lv %d", r.level) + "   " + tr(Friendship.name(r.friendship)) + " (" + r.friendship + ")", cx, cy, Ui.TEXT);
                    Ui.text(g, tr(r.stay ? "Mode: staying" : "Mode: following"), cx, cy + 12, Ui.DIM);
                    long since = gt - r.lastFed;
                    String hunger = since > 48000 ? "STARVING (-80%, very slow!)" : since > 24000 ? "Hungry (-50% strength)" : "Fed";
                    Ui.text(g, tr("Hunger: %s", tr(hunger)), cx, cy + 24, since > 48000 ? 0xFFFF5555 : since > 24000 ? 0xFFFFAA33 : 0xFF66FF66);
                    long awake = gt - r.lastSlept;
                    String tired = awake > 72000 ? "Exhausted (weak and slow)" : awake > 48000 ? "Very tired (slower)" : awake > 24000 ? "Tired (aching)" : "Rested";
                    Ui.text(g, tr("Sleep: %s", tr(tired)), cx, cy + 36, awake > 48000 ? 0xFFFF5555 : awake > 24000 ? 0xFFFFAA33 : 0xFF66FF66);
                    long cd = r.healCdUntil - gt;
                    Ui.text(g, tr("Heal request: %s", cd > 0 ? tr("cooldown %ds", cd / 20 + 1) : tr("ready")), cx, cy + 48, Ui.TEXT);
                    cy += 130;
                }
                Ui.text(g, tr("No food ~20 min: -50%; ~40 min: -80% and slow."), cx, py + ph - 44, Ui.DIM);
                Ui.text(g, tr("No bed for 1+ day: aches, then slow and weak."), cx, py + ph - 34, Ui.DIM);
                Ui.text(g, tr("Fallen companions are revived within 1 min."), cx, py + ph - 24, Ui.DIM);
                Ui.text(g, tr("COMING SOON: gifts, equipping weapons, recruiting adventurers."), cx, py + ph - 14, Ui.DIM);
            }
            case "card" -> {
                RpgClass c = RpgClasses.get(d.classId);
                g.fill(cx, py + 8, cx + cw, py + ph - 8, 0xFF3A2C10);
                g.renderOutline(cx + 2, py + 10, cw - 4, ph - 20, Ui.GOLD);
                Ui.text(g, tr("ADVENTURER CARD"), cx + 8, py + 16, Ui.GOLD);
                Ui.text(g, tr("Name: %s", p.getGameProfile().getName()), cx + 8, py + 32, Ui.TEXT);
                Ui.text(g, tr("Class: %s   Level: %d", c.name(), d.level), cx + 8, py + 44, Ui.TEXT);
                Ui.text(g, tr("XP: %d/%d   Rank: %s", d.xp, RpgStats.xpForNext(d.level), d.rank().name()), cx + 8, py + 56, Ui.TEXT);
                Ui.text(g, tr("Reputation (Guild): %d", d.rep("guild")), cx + 8, py + 68, Ui.TEXT);
                int sy = py + 84;
                for (Stat s : Stat.values()) Ui.text(g, s.code + ": " + RpgStats.eff(p, d, s), cx + 8 + (s.ordinal() % 3) * 90, sy + (s.ordinal() / 3) * 12, Ui.TEXT);
                StringBuilder sb = new StringBuilder(tr("Skills: "));
                d.skills.forEach(k -> { Skill s = Skills.get(k); if (s != null) sb.append(s.name()).append(", "); });
                for (var seq : font.split(Component.literal(sb.toString()), cw - 16)) { g.drawString(font, seq, cx + 8, sy + 34, Ui.TEXT, false); sy += 10; }
                Ui.text(g, tr("Achievements: Quests %d | Monsters %d | Bosses %d | Deaths %d", d.questsCompleted, d.monstersKilled, d.bossesDefeated, d.deaths), cx + 8, py + ph - 24, Ui.DIM);
                if (!d.registered) Ui.text(g, tr("(Not registered - visit the Guild)"), cx + 8, py + ph - 36, 0xFFFF7777);
            }
            case "equip" -> {
                Ui.text(g, tr("ACCESSORIES (2 rings, 1 necklace)"), cx, y, Ui.GOLD);
                String[] names = {"Ring 1", "Ring 2", "Necklace"};
                for (int i = 0; i < 3; i++) {
                    int ey = py + 44 + i * 26;
                    ItemStack st = d.accessories[i];
                    Ui.text(g, tr(names[i]) + ":", cx, ey + 4, Ui.TEXT);
                    g.fill(cx + 56, ey - 1, cx + 74, ey + 17, 0xFF000000);
                    g.renderItem(st, cx + 58, ey);
                    Ui.text(g, st.isEmpty() ? tr("(empty)") : st.getHoverName().getString(), cx + 80, ey + 4, st.isEmpty() ? Ui.DIM : Ui.TEXT);
                }
                Ui.text(g, tr("Hold a ring/necklace and click 'Equip item in hand'."), cx, py + 112, Ui.DIM);
                Ui.text(g, tr("Bonuses from weapons, armor and accessories add to your attributes."), cx, py + 152, Ui.DIM);
                Ui.text(g, tr("Reinforce and repair gear at the Blacksmith."), cx, py + 164, Ui.DIM);
            }
            case "guild" -> {
                Ui.text(g, tr("ADVENTURERS GUILD"), cx, y, Ui.GOLD);
                if (!d.registered) {
                    Ui.text(g, tr("You are not a registered adventurer yet."), cx, py + 30, Ui.TEXT);
                    Ui.text(g, tr("Registration fee: %d Eris. Go to the Guild (in town).", Config.REG_FEE.get()), cx, py + 44, Ui.TEXT);
                    Ui.text(g, tr("Your money: %d Eris", d.money), cx, py + 58, Ui.DIM);
                } else {
                    Ui.text(g, tr("Choose/advance your class (at the Guild). Current class: %s", RpgClasses.get(d.classId).name()), cx, y + 12, Ui.TEXT);
                    List<RpgClass> cl = classList();
                    int rows = Math.max(1, (ph - 64) / 20);
                    for (int i = 0; i < rows && i + scroll < cl.size(); i++) {
                        RpgClass c = cl.get(i + scroll);
                        String req = c.tier() == 1 ? tr("free (2000 Eris to switch)") : tr("Lv %d, Rank %s, %d Eris, from %s", c.minLevel(), c.minRank().name(), c.cost(), RpgClasses.get(c.parent()).name());
                        Ui.text(g, c.name() + " - " + req, cx, py + 44 + i * 20, Ui.TEXT);
                        Ui.text(g, clip(tr(c.desc()), 58), cx, py + 54 + i * 20, Ui.DIM);
                    }
                    Ui.text(g, tr("COMING SOON: create your own guild and adventurer parties."), cx, py + ph - 14, Ui.DIM);
                }
            }
            case "settings" -> {
                Ui.text(g, tr("HUD AND SETTINGS"), cx, y, Ui.GOLD);
                Ui.text(g, tr("Key K: this panel   J: progress   Z X C V: skills"), cx, py + 130, Ui.DIM);
                Ui.text(g, tr("Difficulty/XP/money: mod config (config/konorpg-common.toml)"), cx, py + 142, Ui.DIM);
                Ui.text(g, tr("COMING SOON: dungeons, more cities, professions."), cx, py + 160, Ui.DIM);
            }
            default -> { }
        }
        super.render(g, mx, my, pt);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (tab.equals("skills") || tab.equals("guild")) {
            int size = tab.equals("skills") ? skillList().size() : classList().size();
            int ns = Math.max(0, Math.min(Math.max(0, size - 1), scroll - (int) Math.signum(sy)));
            if (ns != scroll) { scroll = ns; rebuildWidgets(); }
            return true;
        }
        return false;
    }
}
