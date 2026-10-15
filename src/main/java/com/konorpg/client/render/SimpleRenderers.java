package com.konorpg.client.render;

import com.konorpg.KonoRPG;
import com.konorpg.client.model.CabbageModel;
import com.konorpg.client.model.MimicModel;
import com.konorpg.entity.DireWolfEntity;
import com.konorpg.entity.FlyingCabbageEntity;
import com.konorpg.entity.MimicEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Spider;

/** Renderers for the custom fantasy monsters that do not use the humanoid player model. */
public final class SimpleRenderers {
    public static class Cabbage extends MobRenderer<FlyingCabbageEntity, CabbageModel<FlyingCabbageEntity>> {
        private static final ResourceLocation TEX = KonoRPG.id("textures/entity/cabbage.png");

        public Cabbage(EntityRendererProvider.Context ctx) { super(ctx, new CabbageModel<>(ctx.bakeLayer(CabbageModel.LAYER)), 0.3f); }

        @Override public ResourceLocation getTextureLocation(FlyingCabbageEntity e) { return TEX; }
    }

    public static class Mimic extends MobRenderer<MimicEntity, MimicModel> {
        private static final ResourceLocation CLOSED = KonoRPG.id("textures/entity/mimic_closed.png");
        private static final ResourceLocation OPEN = KonoRPG.id("textures/entity/mimic_open.png");

        public Mimic(EntityRendererProvider.Context ctx) { super(ctx, new MimicModel(ctx.bakeLayer(MimicModel.LAYER)), 0.5f); }

        @Override public ResourceLocation getTextureLocation(MimicEntity e) { return e.isRevealed() ? OPEN : CLOSED; }
    }

    public static class GiantSpider extends SpiderRenderer<Spider> {
        public GiantSpider(EntityRendererProvider.Context ctx) { super(ctx); }

        @Override
        protected void scale(Spider spider, PoseStack ps, float pt) { ps.scale(1.6f, 1.6f, 1.6f); }
    }

    public static class DireWolf extends WolfRenderer {
        private static final ResourceLocation ANGRY = ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_angry.png");

        public DireWolf(EntityRendererProvider.Context ctx) { super(ctx); }

        @Override public ResourceLocation getTextureLocation(Wolf wolf) { return ANGRY; }

        @Override
        protected void scale(Wolf wolf, PoseStack ps, float pt) { ps.scale(1.5f, 1.5f, 1.5f); }
    }

    private SimpleRenderers() {}
}
