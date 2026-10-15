package com.konorpg.world;

import com.konorpg.Config;
import com.konorpg.KonoRPG;
import com.konorpg.block.QuestBoardBlock;
import com.konorpg.data.WorldRpgData;
import com.konorpg.entity.TownNpcEntity;
import com.konorpg.registry.ModBlocks;
import com.konorpg.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Builds the starting city (Axel) procedurally with vanilla blocks and populates it with NPCs.
 * One city per world; position is stored in WorldRpgData.
 * EM BREVE: nicer roofs, beds and NPC sleep routines, second district, dungeon entrance outside the gates.
 */
public final class CityBuilder {
    private static final int R = 50;
    private final ServerLevel level;
    private final int cx, y0, cz;
    private final BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
    private final java.util.List<int[]> houseSpots = new java.util.ArrayList<>();

    private CityBuilder(ServerLevel level, int cx, int y0, int cz) {
        this.level = level; this.cx = cx; this.y0 = y0; this.cz = cz;
    }

    public static boolean ensureCity(MinecraftServer server) {
        WorldRpgData w = WorldRpgData.get(server);
        if (w.cityBuilt) return true;
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        int cx = spawn.getX() + Config.CITY_DISTANCE.get();
        int cz = spawn.getZ() + 40;
        for (int x = (cx - R - 6) >> 4; x <= (cx + R + 6) >> 4; x++)
            for (int z = (cz - R - 6) >> 4; z <= (cz + R + 6) >> 4; z++) level.getChunk(x, z);
        int[] hs = new int[9];
        int k = 0;
        for (int dx = -20; dx <= 20; dx += 20)
            for (int dz = -20; dz <= 20; dz += 20) hs[k++] = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx + dx, cz + dz);
        java.util.Arrays.sort(hs);
        int y0 = Math.max(hs[4], level.getSeaLevel() + 1);
        y0 = Math.min(y0, 200);
        CityBuilder b = new CityBuilder(level, cx, y0, cz);
        b.build();
        w.cityBuilt = true;
        w.city = new BlockPos(cx, y0, cz);
        w.cityY = y0;
        w.guildHall = new BlockPos(cx, y0, cz - 22);
        w.board = new BlockPos(cx + 4, y0, cz - 11);
        w.gate = new BlockPos(cx, y0, cz + R - 1);
        w.setDirty();
        CityInfo.set(w.city);
        KonoRPG.LOGGER.info("City of Axel built at {} {} {}", cx, y0, cz);
        return true;
    }

    public static void teleportToPlaza(ServerPlayer p) {
        WorldRpgData w = WorldRpgData.get(p.server);
        if (w.city == null) return;
        ServerLevel level = p.server.overworld();
        p.teleportTo(level, w.city.getX() + 0.5, w.city.getY(), w.city.getZ() - 8.5, 180f, 0f);
    }

    // ---------------- primitives ----------------

    private void set(int x, int y, int z, BlockState s) { level.setBlock(mp.set(cx + x, y0 + y, cz + z), s, 2); }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, s);
    }

    private static BlockState b(Block bl) { return bl.defaultBlockState(); }

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    // ---------------- build ----------------

    private void build() {
        terrain();
        wall();
        plaza();
        guildHall();
        BlockState wallA = b(Blocks.STONE_BRICKS), wallB = b(Blocks.WHITE_TERRACOTTA), wallC = b(Blocks.SMOOTH_SANDSTONE);
        BlockState log = b(Blocks.DARK_OAK_LOG), roofR = b(Blocks.BRICK_SLAB), roofG = b(Blocks.DEEPSLATE_TILE_SLAB), roofS = b(Blocks.SPRUCE_SLAB);
        BlockState pl = b(Blocks.SPRUCE_PLANKS);
        // west shops (door east)
        shop(-30, -10, -20, -2, wallB, log, roofR, true);
        shop(-30, 2, -20, 10, wallA, log, roofG, true);
        shop(-30, 14, -20, 22, wallB, log, roofR, true);
        // east shops (door west)
        shop(20, -10, 30, -2, wallA, log, roofG, false);
        shop(20, 2, 30, 10, wallC, log, roofS, false);
        shop(20, 14, 30, 22, wallB, log, roofR, false);
        // inn and tavern (door north)
        building(-16, 16, -4, 28, 6, 'N', wallC, log, roofR, pl);
        building(4, 16, 16, 28, 6, 'N', wallA, log, roofG, pl);
        barCounter(-16, 16, -4, 28);
        barCounter(4, 16, 16, 28);
        // residential districts (door facing the nearest street)
        for (int[] z : new int[][]{{-42, -34}, {-30, -22}, {-18, -10}, {6, 14}, {18, 26}, {30, 38}}) {
            house(-44, z[0], -36, z[1], 'E', pickWall(z[0]), pickRoof(z[1]));
            house(36, z[0], 44, z[1], 'W', pickWall(z[0] + 1), pickRoof(z[1] + 1));
        }
        for (int x : new int[]{-30, -18, 10, 22}) house(x, 34, x + 8, 42, 'N', pickWall(x), pickRoof(x));
        for (int x : new int[]{-30, 22}) house(x, -46, x + 8, -38, 'S', pickWall(x + 3), pickRoof(x + 3));
        // lamp posts along the streets
        for (int i = -28; i <= 28; i += 8) { post(i, -2); post(i, 2); }
        for (int z = -40; z <= 40; z += 8) { post(-35, z); post(35, z); post(-31, z); post(31, z); }
        for (int x = -30; x <= 30; x += 10) { post(x, 30); post(x, 32); post(x, -35); }
        gardens();
        npcs();
    }

    private void terrain() {
        BlockState dirt = b(Blocks.DIRT);
        for (int x = -R - 2; x <= R + 2; x++) {
            for (int z = -R - 2; z <= R + 2; z++) {
                int h = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx + x, cz + z);
                int surfaceTop = h - 1;
                // always a solid dirt sub-floor (no holes from caves/water), deeper if the terrain was lower
                int bottom = Math.max(level.getMinBuildHeight() - y0, Math.min(surfaceTop - 3 - y0, -5));
                fill(x, bottom, z, x, -2, z, dirt);
                boolean edge = Math.abs(x) > R || Math.abs(z) > R;
                set(x, -1, z, edge ? b(Blocks.GRASS_BLOCK) : paving(x, z));
                int top = Math.min(Math.max(h, y0 + 12), y0 + 45);
                for (int y = y0; y <= top; y++) {
                    mp.set(cx + x, y, cz + z);
                    if (!level.getBlockState(mp).isAir()) level.setBlock(mp, AIR, 2);
                }
            }
        }
    }

    private static boolean isRoad(int x, int z) {
        int ax = Math.abs(x);
        return ax <= 2 || Math.abs(z) <= 1
                || (ax >= 32 && ax <= 34 && z >= -45 && z <= 41)
                || (Math.abs(z - 31) <= 1 && ax <= 34)
                || (z >= -36 && z <= -34 && ax <= 34);
    }

    /** Streets are stone; everything else is grass (houses, gardens). */
    private BlockState paving(int x, int z) {
        if (isRoad(x, z) || (x >= -9 && x <= 9 && z >= -12 && z <= 8)) {
            int hsh = Math.floorMod(x * 7 + z * 13 + x * z, 11);
            return hsh < 6 ? b(Blocks.STONE_BRICKS) : hsh < 9 ? b(Blocks.COBBLESTONE) : b(Blocks.POLISHED_ANDESITE);
        }
        return b(Blocks.GRASS_BLOCK);
    }

    private void wall() {
        BlockState sb = b(Blocks.STONE_BRICKS), mossy = b(Blocks.MOSSY_STONE_BRICKS);
        for (int i = -R + 1; i <= R - 1; i++) {
            for (int y = 0; y <= 4; y++) {
                BlockState s = (i + y) % 5 == 0 ? mossy : sb;
                set(i, y, -R + 1, s); set(i, y, R - 1, s); set(-R + 1, y, i, s); set(R - 1, y, i, s);
            }
            if (Math.floorMod(i, 2) == 0) { set(i, 5, -R + 1, sb); set(i, 5, R - 1, sb); set(-R + 1, 5, i, sb); set(R - 1, 5, i, sb); }
        }
        // corner towers
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) {
            int tx = sx * (R - 2), tz = sz * (R - 2);
            fill(tx - 1, 0, tz - 1, tx + 1, 8, tz + 1, sb);
            fill(tx - 1, 9, tz - 1, tx + 1, 9, tz + 1, b(Blocks.STONE_BRICK_SLAB));
            set(tx, 10, tz, b(Blocks.LANTERN));
        }
        // south gate (exit of the city) and east gate
        fill(-2, 0, R - 1, 2, 3, R - 1, AIR);
        fill(-3, 4, R - 1, 3, 4, R - 1, b(Blocks.DARK_OAK_PLANKS));
        for (int y = 0; y <= 4; y++) { set(-3, y, R - 1, b(Blocks.DARK_OAK_LOG)); set(3, y, R - 1, b(Blocks.DARK_OAK_LOG)); }
        set(-3, 5, R - 1, b(Blocks.LANTERN)); set(3, 5, R - 1, b(Blocks.LANTERN));
        fill(R - 1, 0, -2, R - 1, 3, 2, AIR);
        fill(R - 1, 4, -3, R - 1, 4, 3, b(Blocks.DARK_OAK_PLANKS));
    }

    private void plaza() {
        BlockState pol = b(Blocks.POLISHED_ANDESITE), sb = b(Blocks.STONE_BRICKS);
        for (int x = -9; x <= 9; x++) for (int z = -12; z <= 8; z++) set(x, -1, z, (x + z) % 2 == 0 ? pol : sb);
        // fountain
        fill(-2, 0, -4, 2, 0, 0, sb);
        fill(-1, 0, -3, 1, 0, -1, b(Blocks.WATER));
        fill(-1, -1, -3, 1, -1, -1, sb);
        fill(0, 0, -2, 0, 2, -2, b(Blocks.CHISELED_STONE_BRICKS));
        set(0, 3, -2, b(Blocks.LANTERN));
        // plaza quest board ("mural")
        set(4, 0, -11, b(Blocks.STONE_BRICKS));
        set(4, 1, -11, ModBlocks.QUEST_BOARD.get().defaultBlockState().setValue(QuestBoardBlock.FACING, Direction.SOUTH));
    }

    private void guildHall() {
        building(-10, -30, 10, -14, 7, 'S', b(Blocks.SMOOTH_SANDSTONE), b(Blocks.DARK_OAK_LOG), b(Blocks.BRICK_SLAB), b(Blocks.SPRUCE_PLANKS));
        // reception counter
        fill(-5, 0, -24, 5, 0, -24, b(Blocks.SPRUCE_PLANKS));
        fill(-5, 1, -24, 5, 1, -24, b(Blocks.SPRUCE_SLAB));
        set(-4, 2, -24, b(Blocks.LANTERN)); set(4, 2, -24, b(Blocks.LANTERN));
        // guild board against the west wall
        // guild quest wall: four boards side by side at eye level
        for (int bz = -23; bz <= -20; bz++) set(-9, 1, bz, ModBlocks.QUEST_BOARD.get().defaultBlockState().setValue(QuestBoardBlock.FACING, Direction.EAST));
        // tables / bar
        for (int[] t : new int[][]{{6, -18}, {8, -21}, {6, -26}}) { set(t[0], 0, t[1], b(Blocks.SPRUCE_FENCE)); set(t[0], 1, t[1], b(Blocks.SPRUCE_PRESSURE_PLATE)); }
        fill(-9, 0, -28, -7, 2, -28, b(Blocks.BOOKSHELF));
        fill(7, 0, -29, 9, 0, -29, b(Blocks.BARREL));
        for (int z = -29; z <= -15; z++) if (z != -24) fill(-1, 0, z, 1, 0, z, b(Blocks.RED_CARPET));
        set(-8, 3, -16, b(Blocks.LANTERN)); set(8, 3, -16, b(Blocks.LANTERN));
        // chandelier-ish lanterns hanging from ceiling beams
        fill(-9, 6, -29, 9, 6, -29, b(Blocks.DARK_OAK_PLANKS));
        set(0, 5, -20, b(Blocks.CHAIN)); set(0, 4, -20, b(Blocks.LANTERN));
    }

    private void building(int x1, int z1, int x2, int z2, int h, char door, BlockState wall, BlockState frame, BlockState roof, BlockState floor) {
        fill(x1, -1, z1, x2, -1, z2, floor);
        fill(x1, 0, z1, x2, h - 1, z2, AIR);
        fill(x1, 0, z1, x2, h - 1, z1, wall);
        fill(x1, 0, z2, x2, h - 1, z2, wall);
        fill(x1, 0, z1, x1, h - 1, z2, wall);
        fill(x2, 0, z1, x2, h - 1, z2, wall);
        for (int[] c : new int[][]{{x1, z1}, {x1, z2}, {x2, z1}, {x2, z2}}) fill(c[0], 0, c[1], c[0], h - 1, c[1], frame);
        fill(x1, h - 1, z1, x2, h - 1, z1, frame); fill(x1, h - 1, z2, x2, h - 1, z2, frame);
        fill(x1, h - 1, z1, x1, h - 1, z2, frame); fill(x2, h - 1, z1, x2, h - 1, z2, frame);
        BlockState glass = b(Blocks.GLASS);
        for (int x = x1 + 2; x <= x2 - 2; x += 3) { fill(x, 1, z1, x, 2, z1, glass); fill(x, 1, z2, x, 2, z2, glass); }
        for (int z = z1 + 2; z <= z2 - 2; z += 3) { fill(x1, 1, z, x1, 2, z, glass); fill(x2, 1, z, x2, 2, z, glass); }
        int dx = (x1 + x2) / 2, dz = (z1 + z2) / 2;
        switch (door) {
            case 'S' -> { fill(dx - 1, 0, z2, dx + 1, 2, z2, AIR); fill(dx - 1, 3, z2, dx + 1, 3, z2, frame); }
            case 'N' -> { fill(dx - 1, 0, z1, dx + 1, 2, z1, AIR); fill(dx - 1, 3, z1, dx + 1, 3, z1, frame); }
            case 'E' -> { fill(x2, 0, dz - 1, x2, 2, dz + 1, AIR); fill(x2, 3, dz - 1, x2, 3, dz + 1, frame); }
            default -> { fill(x1, 0, dz - 1, x1, 2, dz + 1, AIR); fill(x1, 3, dz - 1, x1, 3, dz + 1, frame); }
        }
        fill(x1 - 1, h, z1 - 1, x2 + 1, h, z2 + 1, roof);
        fill(x1, h, z1, x2, h, z2, b(Blocks.DARK_OAK_PLANKS));
        if (x2 - x1 > 2 && z2 - z1 > 2) fill(x1 + 1, h + 1, z1 + 1, x2 - 1, h + 1, z2 - 1, roof);
        set(x1 + 1, 0, z1 + 1, b(Blocks.LANTERN)); set(x2 - 1, 0, z2 - 1, b(Blocks.LANTERN));
    }

    private void shop(int x1, int z1, int x2, int z2, BlockState wall, BlockState frame, BlockState roof, boolean doorEast) {
        building(x1, z1, x2, z2, 6, doorEast ? 'E' : 'W', wall, frame, roof, b(Blocks.SPRUCE_PLANKS));
        int cxs = doorEast ? x1 + 4 : x2 - 4;
        fill(cxs, 0, z1 + 2, cxs, 0, z2 - 2, b(Blocks.SPRUCE_PLANKS));
        fill(cxs, 1, z1 + 2, cxs, 1, z2 - 2, b(Blocks.SPRUCE_SLAB));
        int shelf = doorEast ? x1 + 1 : x2 - 1;
        fill(shelf, 0, z1 + 2, shelf, 2, z2 - 2, b(Blocks.BOOKSHELF));
        set(doorEast ? x2 - 2 : x1 + 2, 0, (z1 + z2) / 2, b(Blocks.RED_CARPET));
    }

    private void barCounter(int x1, int z1, int x2, int z2) {
        int z = z2 - 4;
        fill(x1 + 2, 0, z, x2 - 2, 0, z, b(Blocks.SPRUCE_PLANKS));
        fill(x1 + 2, 1, z, x2 - 2, 1, z, b(Blocks.SPRUCE_SLAB));
        fill(x1 + 2, 0, z2 - 1, x2 - 2, 1, z2 - 1, b(Blocks.BARREL));
        for (int x = x1 + 3; x <= x2 - 3; x += 3) { set(x, 0, z1 + 3, b(Blocks.SPRUCE_FENCE)); set(x, 1, z1 + 3, b(Blocks.SPRUCE_PRESSURE_PLATE)); }
    }

    private void house(int x1, int z1, int x2, int z2, char door, BlockState wall, BlockState roof) {
        building(x1, z1, x2, z2, 5, door, wall, b(Blocks.DARK_OAK_LOG), roof, b(Blocks.SPRUCE_PLANKS));
        set(door == 'E' ? x1 + 1 : x2 - 1, 0, z1 + 2, b(Blocks.CRAFTING_TABLE));
        set(door == 'E' ? x1 + 1 : x2 - 1, 0, z2 - 2, b(Blocks.BARREL));
        int zc = (z1 + z2) / 2;
        int bx = door == 'W' ? x2 - 2 : x1 + 2;
        Direction bdir = door == 'W' ? Direction.EAST : Direction.WEST;
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, bdir);
        set(bx, 0, zc, bed.setValue(BedBlock.PART, BedPart.FOOT));
        set(bx + bdir.getStepX(), 0, zc, bed.setValue(BedBlock.PART, BedPart.HEAD));
        houseSpots.add(new int[]{(x1 + x2) / 2, (z1 + z2) / 2 + 1, bx, zc});
    }

    private BlockState pickWall(int k) {
        int i = Math.floorMod(k, 3);
        return i == 0 ? b(Blocks.WHITE_TERRACOTTA) : i == 1 ? b(Blocks.SMOOTH_SANDSTONE) : b(Blocks.STONE_BRICKS);
    }

    private BlockState pickRoof(int k) {
        int i = Math.floorMod(k, 3);
        return i == 0 ? b(Blocks.BRICK_SLAB) : i == 1 ? b(Blocks.SPRUCE_SLAB) : b(Blocks.DEEPSLATE_TILE_SLAB);
    }

    private void post(int x, int z) {
        fill(x, 0, z, x, 2, z, b(Blocks.SPRUCE_FENCE));
        set(x, 3, z, b(Blocks.LANTERN));
    }

    private void gardens() {
        BlockState[] flowers = {b(Blocks.POPPY), b(Blocks.DANDELION), b(Blocks.AZURE_BLUET), b(Blocks.CORNFLOWER), b(Blocks.SHORT_GRASS)};
        for (int x = -R + 2; x <= R - 2; x++) for (int z = -R + 2; z <= R - 2; z++) {
            int hsh = Math.floorMod(x * 31 + z * 17 + x * z * 3, 13);
            if (hsh > 3) continue;
            mp.set(cx + x, y0 - 1, cz + z);
            if (!level.getBlockState(mp).is(Blocks.GRASS_BLOCK)) continue;
            mp.set(cx + x, y0, cz + z);
            if (level.getBlockState(mp).isAir()) level.setBlock(mp, flowers[Math.floorMod(x + z * 2, flowers.length)], 2);
        }
        // empty plots for future expansion (fenced)
        for (int x = -20; x <= -12; x++) for (int z : new int[]{-30, -22}) set(x, 0, z, b(Blocks.OAK_FENCE));
        for (int z = -30; z <= -22; z++) for (int x : new int[]{-20, -12}) set(x, 0, z, b(Blocks.OAK_FENCE));
        for (int x = 12; x <= 20; x++) for (int z : new int[]{-30, -22}) set(x, 0, z, b(Blocks.OAK_FENCE));
        for (int z = -30; z <= -22; z++) for (int x : new int[]{12, 20}) set(x, 0, z, b(Blocks.OAK_FENCE));
    }

    // ---------------- NPCs ----------------

    private static final String[] MALE_SKINS = {"homem", "homem_2", "homem_3", "homem_4", "homem_5"};
    private static final String[] FEMALE_SKINS = {"mulher", "mulher_2", "mulher_3", "mulher_4", "mulher_5", "mulher_6"};
    private static final String[] MALE_NAMES = {"Tomé", "Beto", "Zeca", "Hugo", "Caio", "Davi", "Leo", "Otto", "Rui", "Nico", "Dino", "Teo", "Vitor", "Edu", "Gil", "Ivo", "Max", "Bruno"};
    private static final String[] FEMALE_NAMES = {"Nina", "Rosa", "Lili", "Duda", "Mia", "Bia", "Cléo", "Nana", "Ana", "Lua", "Vera", "Zélia", "Paty", "Rita", "Dora", "Cris", "Gabi", "Mel"};
    private static final String[] FAVORS = {"villager_flowers", "villager_sticks", "villager_sake", "villager_wheat", "villager_gossip", "villager_apples"};

    private TownNpcEntity npc(int x, int z, String id, String name, String skin, float scale, String role, String dialogue, String shop) {
        TownNpcEntity n = ModEntities.TOWN_NPC.get().create(level);
        if (n == null) return null;
        BlockPos home = new BlockPos(cx + x, y0, cz + z);
        n.setup(id, name, skin, scale, role, dialogue, shop, home);
        n.moveTo(cx + x + 0.5, y0, cz + z + 0.5, 0f, 0f);
        n.setPersistenceRequired();
        level.addFreshEntity(n);
        // unbreakable workstation next to workers (like a villager's job-site block)
        if (!role.equals("villager")) {
            mp.set(cx + x, y0, cz + z + 1);
            if (level.getBlockState(mp).isAir()) level.setBlock(mp, ModBlocks.WORKSTATION.get().defaultBlockState(), 2);
        }
        return n;
    }

    private void npcs() {
        npc(0, -26, "luna", "Luna", "luna", 1.0f, "guild", "luna", "");
        npc(-27, -6, "bork", "Bork (Weapons)", "homem_3", 1.04f, "shop", "shopkeeper", "weapons");
        npc(-27, 6, "greta", "Greta (Armor)", "mulher_3", 1.0f, "shop", "shopkeeper", "armor");
        npc(-27, 18, "wiz", "Wiz (Magic)", "wiz", 1.0f, "shop", "shopkeeper", "magic");
        npc(27, -6, "tomas", "Tomás (Items)", "homem", 0.96f, "shop", "shopkeeper", "items");
        npc(27, 6, "grom", "Grom (Blacksmith)", "blacksmith", 1.06f, "shop", "blacksmith", "smith");
        npc(27, 18, "lyra", "Lyra (Alchemist)", "mulher_5", 1.0f, "shop", "alchemist", "alchemy");
        npc(-10, 25, "marta", "Marta (Inn)", "mulher_2", 1.0f, "shop", "innkeeper", "");
        npc(10, 25, "joe", "Joe (Tavern)", "homem_4", 1.03f, "shop", "tavern", "tavern");
        npc(-4, 30, "guard_1", "Guard", "homem_2", 1.0f, "guard", "guard", "");
        npc(4, 30, "guard_2", "Guard", "homem_5", 1.0f, "guard", "guard", "");
        npc(8, -6, "guard_3", "Square Guard", "homem_2", 1.0f, "guard", "guard", "");
        npc(-8, -6, "guard_4", "Square Guard", "homem_5", 1.0f, "guard", "guard", "");
        npc(46, 0, "guard_5", "East Gate Guard", "homem_3", 1.0f, "guard", "guard", "");
        // residents: one per house, living inside
        int mi = 0, fi = 0;
        for (int i = 0; i < houseSpots.size(); i++) {
            int[] h = houseSpots.get(i);
            boolean female = i % 2 == 0;
            String name = female ? FEMALE_NAMES[fi % FEMALE_NAMES.length] : MALE_NAMES[mi % MALE_NAMES.length];
            String skin = female ? FEMALE_SKINS[(fi * 2 + i) % FEMALE_SKINS.length] : MALE_SKINS[(mi * 2 + i) % MALE_SKINS.length];
            if (female) fi++; else mi++;
            float sc = i % 7 == 3 ? 0.72f : i % 5 == 2 ? 1.04f : 0.96f + (i % 3) * 0.02f; // the player is the standard size; a few kids / slightly taller folks
            TownNpcEntity r = npc(h[0], h[1], "resident_" + i, name, skin, sc, "villager", FAVORS[i % FAVORS.length], "");
            if (r != null) r.setBed(new BlockPos(cx + h[2], y0, cz + h[3]));
        }
        // some people out in the streets too
        int[][] street = {{-6, 3}, {6, -3}, {0, 14}, {-14, 31}, {14, 31}, {-33, 0}, {33, 0}, {0, -35}};
        for (int i = 0; i < street.length; i++) {
            boolean female = i % 2 == 1;
            String name = female ? FEMALE_NAMES[(fi + i) % FEMALE_NAMES.length] : MALE_NAMES[(mi + i) % MALE_NAMES.length];
            String skin = female ? FEMALE_SKINS[(i * 5) % FEMALE_SKINS.length] : MALE_SKINS[(i * 3) % MALE_SKINS.length];
            npc(street[i][0], street[i][1], "walker_" + i, name, skin, 1.0f, "villager", FAVORS[(i + 3) % FAVORS.length], "");
        }
    }
}
