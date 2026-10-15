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
import net.minecraft.world.entity.Entity;

/** Small toad model; the Giant Toad renderer scales it by the slime size (x3). */
public class ToadModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(KonoRPG.id("toad"), "main");
    private final ModelPart root;

    public ToadModel(ModelPart root) { this.root = root; }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p = mesh.getRoot();
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -4.0f, -4.0f, 8.0f, 4.0f, 8.0f), PartPose.offset(0.0f, 24.0f, 0.0f));
        p.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 12).addBox(-3.0f, -3.0f, -3.0f, 6.0f, 3.0f, 4.0f), PartPose.offset(0.0f, 22.0f, -3.0f));
        p.addOrReplaceChild("eye_l", CubeListBuilder.create().texOffs(0, 20).addBox(-3.0f, -2.0f, -1.0f, 2.0f, 2.0f, 2.0f), PartPose.offset(0.0f, 19.0f, -4.0f));
        p.addOrReplaceChild("eye_r", CubeListBuilder.create().texOffs(0, 20).addBox(1.0f, -2.0f, -1.0f, 2.0f, 2.0f, 2.0f), PartPose.offset(0.0f, 19.0f, -4.0f));
        p.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(24, 12).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 3.0f), PartPose.offset(-4.5f, 21.0f, 3.0f));
        p.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(24, 12).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 3.0f), PartPose.offset(4.5f, 21.0f, 3.0f));
        p.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(24, 12).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 2.0f, 2.0f), PartPose.offset(-3.5f, 22.0f, -3.0f));
        p.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(24, 12).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 2.0f, 2.0f), PartPose.offset(3.5f, 22.0f, -3.0f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) { }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
