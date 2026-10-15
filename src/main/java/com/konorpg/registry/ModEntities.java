package com.konorpg.registry;

import com.konorpg.KonoRPG;
import com.konorpg.entity.CompanionEntity;
import com.konorpg.entity.DemonGeneralEntity;
import com.konorpg.entity.DireWolfEntity;
import com.konorpg.entity.FlyingCabbageEntity;
import com.konorpg.entity.GiantSpiderEntity;
import com.konorpg.entity.MimicEntity;
import com.konorpg.entity.ToadQueenEntity;
import com.konorpg.entity.Monsters;
import com.konorpg.entity.TownNpcEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, KonoRPG.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<Monsters.Goblin>> GOBLIN = ENTITIES.register("goblin",
            () -> EntityType.Builder.of(Monsters.Goblin::new, MobCategory.MONSTER).sized(0.5f, 1.4f).clientTrackingRange(8).build("goblin"));
    public static final DeferredHolder<EntityType<?>, EntityType<Monsters.Kobold>> KOBOLD = ENTITIES.register("kobold",
            () -> EntityType.Builder.of(Monsters.Kobold::new, MobCategory.MONSTER).sized(0.5f, 1.3f).clientTrackingRange(8).build("kobold"));
    public static final DeferredHolder<EntityType<?>, EntityType<Monsters.Orc>> ORC = ENTITIES.register("orc",
            () -> EntityType.Builder.of(Monsters.Orc::new, MobCategory.MONSTER).sized(0.8f, 2.2f).clientTrackingRange(8).build("orc"));
    public static final DeferredHolder<EntityType<?>, EntityType<Monsters.Lizardman>> LIZARDMAN = ENTITIES.register("lizardman",
            () -> EntityType.Builder.of(Monsters.Lizardman::new, MobCategory.MONSTER).sized(0.6f, 1.9f).clientTrackingRange(8).build("lizardman"));
    public static final DeferredHolder<EntityType<?>, EntityType<Monsters.SkeletonWarrior>> SKELETON_WARRIOR = ENTITIES.register("skeleton_warrior",
            () -> EntityType.Builder.of(Monsters.SkeletonWarrior::new, MobCategory.MONSTER).sized(0.6f, 1.9f).clientTrackingRange(8).build("skeleton_warrior"));
    public static final DeferredHolder<EntityType<?>, EntityType<Monsters.GiantToad>> GIANT_TOAD = ENTITIES.register("giant_toad",
            () -> EntityType.Builder.of(Monsters.GiantToad::new, MobCategory.MONSTER).sized(0.52f, 0.52f).eyeHeight(0.325f).spawnDimensionsScale(4.0f).clientTrackingRange(10).build("giant_toad"));

    public static final DeferredHolder<EntityType<?>, EntityType<Monsters.Golem>> GOLEM = ENTITIES.register("golem",
            () -> EntityType.Builder.of(Monsters.Golem::new, MobCategory.MONSTER).sized(0.9f, 2.7f).clientTrackingRange(10).build("golem"));
    public static final DeferredHolder<EntityType<?>, EntityType<Monsters.Demon>> DEMON = ENTITIES.register("demon",
            () -> EntityType.Builder.of(Monsters.Demon::new, MobCategory.MONSTER).sized(0.6f, 2.0f).clientTrackingRange(10).fireImmune().build("demon"));
    public static final DeferredHolder<EntityType<?>, EntityType<GiantSpiderEntity>> GIANT_SPIDER = ENTITIES.register("giant_spider",
            () -> EntityType.Builder.of(GiantSpiderEntity::new, MobCategory.MONSTER).sized(1.9f, 1.3f).clientTrackingRange(8).build("giant_spider"));
    public static final DeferredHolder<EntityType<?>, EntityType<DireWolfEntity>> DIRE_WOLF = ENTITIES.register("dire_wolf",
            () -> EntityType.Builder.of(DireWolfEntity::new, MobCategory.MONSTER).sized(1.1f, 1.2f).clientTrackingRange(8).build("dire_wolf"));
    public static final DeferredHolder<EntityType<?>, EntityType<FlyingCabbageEntity>> FLYING_CABBAGE = ENTITIES.register("flying_cabbage",
            () -> EntityType.Builder.of(FlyingCabbageEntity::new, MobCategory.CREATURE).sized(0.6f, 0.6f).clientTrackingRange(8).build("flying_cabbage"));
    public static final DeferredHolder<EntityType<?>, EntityType<MimicEntity>> MIMIC = ENTITIES.register("mimic",
            () -> EntityType.Builder.of(MimicEntity::new, MobCategory.MONSTER).sized(0.95f, 0.95f).clientTrackingRange(8).build("mimic"));
    public static final DeferredHolder<EntityType<?>, EntityType<ToadQueenEntity>> TOAD_QUEEN = ENTITIES.register("toad_queen",
            () -> EntityType.Builder.of(ToadQueenEntity::new, MobCategory.MONSTER).sized(0.52f, 0.52f).eyeHeight(0.325f).spawnDimensionsScale(4.0f).clientTrackingRange(16).build("toad_queen"));
    public static final DeferredHolder<EntityType<?>, EntityType<DemonGeneralEntity>> DEMON_GENERAL = ENTITIES.register("demon_general",
            () -> EntityType.Builder.of(DemonGeneralEntity::new, MobCategory.MONSTER).sized(0.9f, 3.1f).clientTrackingRange(16).fireImmune().build("demon_general"));

    public static final DeferredHolder<EntityType<?>, EntityType<TownNpcEntity>> TOWN_NPC = ENTITIES.register("town_npc",
            () -> EntityType.Builder.of(TownNpcEntity::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10).build("town_npc"));
    public static final DeferredHolder<EntityType<?>, EntityType<CompanionEntity>> COMPANION = ENTITIES.register("companion",
            () -> EntityType.Builder.of(CompanionEntity::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10).build("companion"));

    private ModEntities() {}
}
