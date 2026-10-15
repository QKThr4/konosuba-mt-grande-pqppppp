package com.konorpg.registry;

import com.konorpg.KonoRPG;
import com.konorpg.block.QuestBoardBlock;
import com.konorpg.block.WorkstationBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KonoRPG.MOD_ID);

    public static final DeferredBlock<QuestBoardBlock> QUEST_BOARD = BLOCKS.register("quest_board",
            () -> new QuestBoardBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<WorkstationBlock> WORKSTATION = BLOCKS.register("workstation",
            () -> new WorkstationBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1.0f, 3600000.0f).sound(SoundType.WOOD).noLootTable()));

    private ModBlocks() {}
}
