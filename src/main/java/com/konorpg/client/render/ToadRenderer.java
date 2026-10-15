package com.konorpg.client.render;

import com.konorpg.KonoRPG;
import com.konorpg.client.model.ToadModel;
import com.konorpg.entity.Monsters;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Giant Toad: bouncy slime body (squash & stretch like a slime) with the toad model. */
public class ToadRenderer extends MobRenderer<Monsters.GiantToad, ToadModel<Monsters.GiantToad>> {
    private static final ResourceLocation TEX = KonoRPG.id("textures/entity/toad_body.png");
    private static final ResourceLocation QUEEN = KonoRPG.id("textures/entity/toad_queen.png");

    public ToadRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ToadModel<>(ctx.bakeLayer(ToadModel.LAYER)), 0.25f);
    }

    @Override
    public ResourceLocation getTextureLocation(Monsters.GiantToad entity) { return entity instanceof com.konorpg.entity.ToadQueenEntity ? QUEEN : TEX; }

    @Override
    protected void scale(Monsters.GiantToad slime, PoseStack poseStack, float partialTick) {
        float size = (float) slime.getSize();
        this.shadowRadius = 0.25f * size;
        poseStack.scale(0.999f, 0.999f, 0.999f);
        poseStack.translate(0.0f, 0.001f, 0.0f);
        float sq = Mth.lerp(partialTick, slime.oSquish, slime.squish) / (size * 0.5f + 1.0f);
        float f = 1.0f / (sq + 1.0f);
        poseStack.scale(f * size, 1.0f / f * size, f * size);
    }
}
