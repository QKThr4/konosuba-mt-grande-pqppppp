package com.konorpg.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;

/** Giant Spider: a bigger, tougher vanilla spider (climbs walls). */
public class GiantSpiderEntity extends Spider {
    public GiantSpiderEntity(EntityType<? extends Spider> type, Level level) {
        super(type, level);
        this.xpReward = 14;
    }

    public static AttributeSupplier.Builder attrs() {
        return Spider.createAttributes().add(Attributes.MAX_HEALTH, 38).add(Attributes.ATTACK_DAMAGE, 5);
    }
}
