package com.konorpg.client.model;

import com.konorpg.KonoRPG;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Flying cabbage: a round leafy head with two flapping leaves. */
public class CabbageModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(KonoRPG.id("cabbage"), "main");
    private final ModelPart root, leafL, leafR;

    public CabbageModel(ModelPart root) {
        this.root = root;
        this.leafL = root.getChild("leaf_l");
        this.leafR = root.getChild("leaf_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p = mesh.getRoot();
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f), PartPose.offset(0.0f, 24.0f, 0.0f));
        p.addOrReplaceChild("leaf_l", CubeListBuilder.create().texOffs(0, 16).addBox(-6.0f, -1.0f, -2.0f, 6.0f, 1.0f, 4.0f), PartPose.offset(-4.0f, 19.0f, 0.0f));
        p.addOrReplaceChild("leaf_r", CubeListBuilder.create().texOffs(0, 16).addBox(0.0f, -1.0f, -2.0f, 6.0f, 1.0f, 4.0f), PartPose.offset(4.0f, 19.0f, 0.0f));
        return LayerDefinition.create(mesh, 32, 24);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float flap = Mth.cos(ageInTicks * 1.3f) * 0.7f;
        leafL.zRot = -flap;
        leafR.zRot = flap;
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer buf, int light, int overlay, int color) {
        root.render(ps, buf, light, overlay, color);
    }
}
