package com.konorpg.world;

import com.konorpg.data.WorldRpgData;
import com.konorpg.rpg.RpgManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * Inn rooms: private sealed rooms high in the sky. You can only get in by renting one at the innkeeper.
 * You choose 1 to 6 beds (price grows with the number of beds) so your companions can sleep too.
 * Check-out: the button by the wall, or automatically in the morning.
 */
public final class Hotel {
    private static final int Y = 250;
    public static final int MAX_BEDS = 6;
    private static final Map<UUID, BlockPos> RETURN = new HashMap<>();
    /** {x, headDz, footDz, facing(0=north,1=south)} in room order. */
    private static final int[][] BEDS = {{-3, -3, -2, 0}, {3, -3, -2, 0}, {-1, -3, -2, 0}, {1, -3, -2, 0}, {-3, 2, 1, 1}, {3, 2, 1, 1}};

    private static BlockState b(Block bl) { return bl.defaultBlockState(); }

    public static int price(int beds) { return 100 + 60 * (beds - 1); }

    private static BlockPos ensure(MinecraftServer server) {
        WorldRpgData w = WorldRpgData.get(server);
        if (w.hotel != null) return w.hotel;
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos base = new BlockPos(spawn.getX() + 90, Y, spawn.getZ());
        for (int x = (base.getX() - 8) >> 4; x <= (base.getX() + 48) >> 4; x++)
            for (int z = (base.getZ() - 8) >> 4; z <= (base.getZ() + 8) >> 4; z++) level.getChunk(x, z);
        for (int r = 0; r < 4; r++) {
            BlockPos c = base.offset(r * 10, 0, 0);
            for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) for (int y = -1; y <= 5; y++) {
                boolean shell = Math.abs(x) == 4 || Math.abs(z) == 4 || y == -1 || y == 5;
                BlockState st = !shell ? b(Blocks.AIR) : (y == -1 ? b(Blocks.DARK_OAK_PLANKS) : b(Blocks.SPRUCE_PLANKS));
                level.setBlock(c.offset(x, y, z), st, 2);
            }
            level.setBlock(c.offset(0, 4, 0), b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 2);
            level.setBlock(c.offset(0, 1, 3), b(Blocks.OAK_BUTTON).setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, Direction.NORTH), 2);
            level.setBlock(c.offset(0, 0, 0), b(Blocks.RED_CARPET), 2);
        }
        w.hotel = base;
        w.setDirty();
        return base;
    }

    /** Places exactly {@code beds} beds in the room (the others are removed). */
    private static void configure(ServerLevel level, BlockPos c, int beds) {
        for (int i = 0; i < BEDS.length; i++) {
            int[] bd = BEDS[i];
            BlockPos head = c.offset(bd[0], 0, bd[1]), foot = c.offset(bd[0], 0, bd[2]);
            if (i < beds) {
                Direction face = bd[3] == 0 ? Direction.NORTH : Direction.SOUTH;
                BlockState bed = b(Blocks.RED_BED).setValue(BedBlock.FACING, face);
                level.setBlock(foot, bed.setValue(BedBlock.PART, BedPart.FOOT), 2);
                level.setBlock(head, bed.setValue(BedBlock.PART, BedPart.HEAD), 2);
            } else {
                level.setBlock(foot, b(Blocks.AIR), 2);
                level.setBlock(head, b(Blocks.AIR), 2);
            }
        }
    }

    public static boolean rent(ServerPlayer p, int beds) {
        beds = Math.max(1, Math.min(MAX_BEDS, beds));
        if (isInside(p)) { RpgManager.msg(p, "You are already in a room."); return false; }
        int cost = price(beds);
        if (!RpgManager.spend(p, cost)) { RpgManager.msg(p, "That room costs %d Eris.", cost); return false; }
        BlockPos base = ensure(p.server);
        int room = Math.floorMod(p.getUUID().hashCode(), 4);
        BlockPos c = base.offset(room * 10, 0, 0);
        configure(p.server.overworld(), c, beds);
        RETURN.put(p.getUUID(), p.blockPosition());
        p.teleportTo(p.server.overworld(), c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 180f, 0f);
        RpgManager.msg(p, "Room rented with %d bed(s)! Sleep in one and let your companions use the others. Check-out: the button by the wall, or in the morning.", beds);
        return true;
    }

    public static boolean isInside(ServerPlayer p) {
        WorldRpgData w = WorldRpgData.get(p.server);
        return w.hotel != null && p.getY() >= Y - 3 && Math.abs(p.getX() - w.hotel.getX() - 15) < 40 && Math.abs(p.getZ() - w.hotel.getZ()) < 10;
    }

    public static boolean isHotelBlock(ServerPlayer p, BlockPos pos) {
        WorldRpgData w = WorldRpgData.get(p.server);
        return w.hotel != null && pos.getY() >= Y - 3 && Math.abs(pos.getX() - w.hotel.getX() - 15) < 40 && Math.abs(pos.getZ() - w.hotel.getZ()) < 10;
    }

    public static void checkout(ServerPlayer p) {
        BlockPos ret = RETURN.remove(p.getUUID());
        if (ret == null) { CityBuilder.teleportToPlaza(p); }
        else p.teleportTo(p.server.overworld(), ret.getX() + 0.5, ret.getY(), ret.getZ() + 0.5, p.getYRot(), 0f);
        RpgManager.msg(p, "Checked out. Good morning!");
    }

    private Hotel() {}
}
