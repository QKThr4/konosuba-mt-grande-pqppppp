package com.konorpg.entity;

import com.konorpg.Config;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Fantasy monsters. All are humanoid-model creatures with their own skin and size.
 * EM BREVE: Giant Toad custom model, Mimic (disguise), Giant Spider, Golem, Demon, flying monsters, Dragon.
 */
public final class Monsters {
    public static class Goblin extends RpgHumanoidMonster {
        public Goblin(EntityType<? extends Goblin> t, Level l) { super(t, l, "goblin", 0.8f); xpReward = 6; }
        public static AttributeSupplier.Builder attrs() { return base(14, 3, 0.30); }
        @Override protected Item weapon() { return Items.WOODEN_SWORD; }
    }

    public static class Kobold extends RpgHumanoidMonster {
        public Kobold(EntityType<? extends Kobold> t, Level l) { super(t, l, "kobold", 0.7f); xpReward = 5; }
        public static AttributeSupplier.Builder attrs() { return base(10, 2.5, 0.33); }
        @Override protected Item weapon() { return Items.STONE_SWORD; }
    }

    public static class Orc extends RpgHumanoidMonster {
        public Orc(EntityType<? extends Orc> t, Level l) { super(t, l, "orc", 1.2f); xpReward = 15; }
        public static AttributeSupplier.Builder attrs() { return base(40, 7, 0.25); }
        @Override protected Item weapon() { return Items.IRON_AXE; }
    }

    public static class Lizardman extends RpgHumanoidMonster {
        public Lizardman(EntityType<? extends Lizardman> t, Level l) { super(t, l, "lizardman", 1.0f); xpReward = 12; }
        public static AttributeSupplier.Builder attrs() { return base(28, 5, 0.28); }
        @Override protected Item weapon() { return Items.IRON_SWORD; }
    }

    public static class SkeletonWarrior extends RpgHumanoidMonster {
        public SkeletonWarrior(EntityType<? extends SkeletonWarrior> t, Level l) { super(t, l, "skeleton_warrior", 1.0f); xpReward = 10; }
        public static AttributeSupplier.Builder attrs() { return base(22, 5, 0.27); }
        @Override protected Item weapon() { return Items.IRON_SWORD; }
        @Override protected boolean isSunBurnTick() { return false; }
    }

    public static class Golem extends RpgHumanoidMonster {
        public Golem(EntityType<? extends Golem> t, Level l) { super(t, l, "golem", 1.45f); xpReward = 25; }
        public static AttributeSupplier.Builder attrs() { return base(90, 10, 0.2).add(Attributes.KNOCKBACK_RESISTANCE, 0.8); }
    }

    public static class Demon extends RpgHumanoidMonster {
        public Demon(EntityType<? extends Demon> t, Level l) { super(t, l, "demon", 1.1f); xpReward = 30; }
        public static AttributeSupplier.Builder attrs() { return base(60, 9, 0.3); }
        @Override protected Item weapon() { return Items.GOLDEN_SWORD; }
        @Override
        public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
            boolean ok = super.doHurtTarget(target);
            if (ok) target.igniteForSeconds(4);
            return ok;
        }
    }

    /**
     * Giant Toad: a 4x slime (bouncy, more HP) rendered with a toad model. When killed it splits into small toads.
     */
    public static class GiantToad extends Slime {
        public GiantToad(EntityType<? extends GiantToad> t, Level l) { super(t, l); }

        public static AttributeSupplier.Builder attrs() {
            return Monster.createMonsterAttributes();
        }

        @Override
        public void setSize(int size, boolean resetHealth) {
            super.setSize(size, resetHealth);
            AttributeInstance hp = getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) {
                hp.setBaseValue(size * size * 2.0 * Config.MONSTER_HP_MULT.get());
                if (resetHealth) setHealth(getMaxHealth());
            }
            this.xpReward = size * 3;
        }

        @Override
        @Nullable
        public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type, @Nullable SpawnGroupData data) {
            SpawnGroupData r = super.finalizeSpawn(level, diff, type, data);
            setSize(4, true); // always the big version (4 = size of a big slime)
            return r;
        }

        @Override
        protected ParticleOptions getParticleType() { return ParticleTypes.ITEM_SLIME; }
    }

    private Monsters() {}
}
