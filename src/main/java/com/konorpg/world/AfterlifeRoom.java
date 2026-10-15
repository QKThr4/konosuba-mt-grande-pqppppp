package com.konorpg.world;

import com.konorpg.data.PlayerRpgData;
import com.konorpg.util.Lang;
import com.konorpg.data.WorldRpgData;
import com.konorpg.entity.TownNpcEntity;
import com.konorpg.quest.QuestManager;
import com.konorpg.registry.ModEntities;
import com.konorpg.rpg.Perks;
import com.konorpg.rpg.RpgManager;
import com.konorpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** The white "afterlife" room where the goddess Aqua sends you to another world (KonoSuba style). */
public final class AfterlifeRoom {
    private static final int Y = 240;

    private static BlockState b(Block bl) { return bl.defaultBlockState(); }

    public static BlockPos ensure(MinecraftServer server) {
        WorldRpgData w = WorldRpgData.get(server);
        if (w.afterlife != null) return w.afterlife;
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos c = new BlockPos(spawn.getX(), Y, spawn.getZ());
        for (int x = (c.getX() - 12) >> 4; x <= (c.getX() + 12) >> 4; x++)
            for (int z = (c.getZ() - 12) >> 4; z <= (c.getZ() + 12) >> 4; z++) level.getChunk(x, z);
        BlockState white = b(Blocks.WHITE_CONCRETE), quartz = b(Blocks.QUARTZ_BLOCK), lamp = b(Blocks.SEA_LANTERN), air = b(Blocks.AIR);
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) for (int y = -1; y <= 8; y++) {
            boolean shell = Math.abs(x) == 8 || Math.abs(z) == 8 || y == -1 || y == 8;
            BlockState st = shell ? white : air;
            if (y == 8 && Math.abs(x) % 3 == 0 && Math.abs(z) % 3 == 0) st = lamp;
            if (y == -1 && (x + z) % 2 == 0) st = quartz;
            level.setBlock(c.offset(x, y, z), st, 2);
        }
        for (int x = -2; x <= 2; x++) for (int z = 0; z <= 6; z++) level.setBlock(c.offset(x, 0, z), b(Blocks.LIGHT_BLUE_CARPET), 2);
        // the goddess' desk and chair
        for (int x = -3; x <= 3; x++) { level.setBlock(c.offset(x, 0, -3), quartz, 2); level.setBlock(c.offset(x, 1, -3), b(Blocks.QUARTZ_SLAB), 2); }
        level.setBlock(c.offset(0, 0, -5), b(Blocks.QUARTZ_STAIRS).setValue(StairBlock.FACING, Direction.SOUTH), 2);
        level.setBlock(c.offset(0, 0, 2), b(Blocks.QUARTZ_STAIRS).setValue(StairBlock.FACING, Direction.NORTH), 2);
        for (int[] p : new int[][]{{-7, -7}, {7, -7}, {-7, 7}, {7, 7}}) {
            for (int y = 0; y <= 6; y++) level.setBlock(c.offset(p[0], y, p[1]), b(Blocks.CHISELED_QUARTZ_BLOCK), 2);
        }
        w.afterlife = c;
        w.setDirty();
        TownNpcEntity aqua = ModEntities.TOWN_NPC.get().create(level);
        if (aqua != null) {
            boolean taken = w.aquaTaken;
            aqua.setup("room_host", taken ? "Eris" : "Aqua", taken ? "mulher_2" : "aqua", taken ? 1.0f : 0.93f, "static", "", "", BlockPos.ZERO);
            aqua.moveTo(c.getX() + 0.5, c.getY(), c.getZ() - 3.5, 0f, 0f);
            aqua.setPersistenceRequired();
            level.addFreshEntity(aqua);
        }
        return c;
    }

    /** If Aqua was already taken by someone else, whoever arrives later meets Eris instead. */
    private static void refreshHost(MinecraftServer server) {
        WorldRpgData w = WorldRpgData.get(server);
        if (w.afterlife == null || !w.aquaTaken) return;
        ServerLevel level = server.overworld();
        for (ServerPlayer other : level.players()) if (RpgManager.data(other).flags.contains("aqua_chosen")) return; // cutscene in progress
        TownNpcEntity h = find(level, w.afterlife, "room_host");
        if (h != null && h.npcName().equals("Aqua")) h.setup("room_host", "Eris", "mulher_2", 1.0f, "static", "", "", BlockPos.ZERO);
    }

    public static void enter(ServerPlayer p) {
        BlockPos c = ensure(p.server);
        refreshHost(p.server);
        p.teleportTo(p.server.overworld(), c.getX() + 0.5, c.getY(), c.getZ() + 4.5, 180f, 0f);
    }

    private static TownNpcEntity find(ServerLevel level, BlockPos c, String id) {
        for (TownNpcEntity e : level.getEntitiesOfClass(TownNpcEntity.class, new AABB(c).inflate(10))) if (e.npcId().equals(id)) return e;
        return null;
    }

    private static String hostName(ServerPlayer p) {
        BlockPos c = WorldRpgData.get(p.server).afterlife;
        TownNpcEntity h = c == null ? null : find(p.serverLevel(), c, "room_host");
        return h == null ? "Aqua" : h.npcName();
    }

    public static net.minecraft.nbt.CompoundTag introTag(MinecraftServer server) {
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.putBoolean("aquaTaken", WorldRpgData.get(server).aquaTaken);
        return t;
    }

    /** Removes the sky room once nobody is inside (it is rebuilt when a new player arrives). */
    public static void cleanup(MinecraftServer server) {
        WorldRpgData w = WorldRpgData.get(server);
        BlockPos c = w.afterlife;
        if (c == null) return;
        ServerLevel level = server.overworld();
        for (ServerPlayer pl : level.players()) {
            if (pl.getY() > Y - 3 && pl.getY() < Y + 12 && Math.abs(pl.getX() - c.getX()) < 12 && Math.abs(pl.getZ() - c.getZ()) < 12) return;
        }
        for (TownNpcEntity e : level.getEntitiesOfClass(TownNpcEntity.class, new AABB(c).inflate(12))) {
            if (e.npcId().startsWith("room_")) e.discard();
        }
        BlockState air = b(Blocks.AIR);
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) for (int y = -1; y <= 8; y++) level.setBlock(c.offset(x, y, z), air, 2);
        w.afterlife = null;
        w.setDirty();
    }

    private static void speak(ServerPlayer p, String who, ChatFormatting color, String en, Object... args) {
        String text = Lang.tr(p, en, args);
        p.sendSystemMessage(Component.literal("<" + who + "> ").withStyle(color).append(Component.literal(text).withStyle(ChatFormatting.WHITE)));
        TownNpcEntity n = find(p.serverLevel(), WorldRpgData.get(p.server).afterlife, who.equals("Eris") && hostName(p).equals("Aqua") ? "room_eris" : "room_host");
        if (n != null) n.say(text.length() > 40 ? text.substring(0, 40) + "..." : text);
    }

    /** The goddess reacts to the chosen gift, then casts the teleport spell. */
    public static void cutscene(ServerPlayer p, String perk) {
        PlayerRpgData d = RpgManager.data(p);
        d.flags.add("intro_cutscene");
        WorldRpgData w = WorldRpgData.get(p.server);
        BlockPos c = w.afterlife;
        String perkName = Perks.ALL.get(perk).name();
        String host = hostName(p);
        ChatFormatting hc = host.equals("Eris") ? ChatFormatting.GREEN : ChatFormatting.AQUA;
        if (!perk.equals("aqua")) {
            speak(p, host, hc, "Hmm, \"%s\"? Great choice! Consider yourself blessed by a goddess.", Lang.tr(p, perkName));
            Scheduler.later(60, () -> speak(p, host, hc, "Now stay still... I am going to cast the teleport spell!"));
            Scheduler.later(110, () -> magic(p, c));
            Scheduler.later(150, () -> finish(p));
        } else {
            speak(p, "Aqua", ChatFormatting.AQUA, "Sure, sure, here you go... wait a second...");
            Scheduler.later(50, () -> speak(p, "Aqua", ChatFormatting.AQUA, "ME?!?! You want to take ME with you?!"));
            Scheduler.later(100, () -> {
                ServerLevel level = p.serverLevel();
                TownNpcEntity eris = ModEntities.TOWN_NPC.get().create(level);
                if (eris != null) {
                    eris.setup("room_eris", "Eris", "mulher_2", 1.0f, "static", "", "", BlockPos.ZERO);
                    eris.moveTo(c.getX() + 3.5, c.getY(), c.getZ() - 3.5, 0f, 0f);
                    eris.setPersistenceRequired();
                    level.addFreshEntity(eris);
                    level.sendParticles(ParticleTypes.END_ROD, eris.getX(), eris.getY() + 1, eris.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
                }
                speak(p, "Eris", ChatFormatting.GREEN, "Aqua, the rule is clear: whoever chooses the goddess takes the goddess. I will take your place here.");
            });
            Scheduler.later(160, () -> speak(p, "Aqua", ChatFormatting.AQUA, "NO!! I do not want to go! Hey, let go of me!! I am a goddess!!"));
            Scheduler.later(220, () -> magic(p, c));
            Scheduler.later(260, () -> finish(p));
        }
    }

    private static void magic(ServerPlayer p, BlockPos c) {
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 2.0f, 0.8f);
        level.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 200, 0.8, 1.2, 0.8, 0.5);
        level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 80, 0.6, 1.2, 0.6, 0.1);
    }

    /** Teleports to the city and starts the adventure (also used when relogging mid-cutscene). */
    public static void finish(ServerPlayer p) {
        if (p.hasDisconnected()) return;
        PlayerRpgData d = RpgManager.data(p);
        if (!d.flags.remove("intro_cutscene")) return;
        WorldRpgData w = WorldRpgData.get(p.server);
        ServerLevel level = p.server.overworld();
        if (w.afterlife != null) {
            TownNpcEntity eris = find(level, w.afterlife, "room_eris");
            if (eris != null) eris.discard();
        }
        CityBuilder.ensureCity(p.server);
        CityBuilder.teleportToPlaza(p);
        if (d.flags.remove("aqua_chosen")) d.flags.add("aqua_pending");
        cleanup(p.server);
        QuestManager.accept(p, "main_01_guild", true);
        QuestManager.accept(p, "main_02_class", true);
        RpgManager.title(p, "Welcome to Axel", "Find the Adventurers Guild (the big building north of the square)");
        RpgManager.msg(p, "You were reborn in another world! Go to the Guild, talk to Luna and register. Press K to open your panel.");
        RpgManager.sync(p);
    }

    private AfterlifeRoom() {}
}
