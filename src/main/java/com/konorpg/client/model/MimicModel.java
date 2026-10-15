package com.konorpg.client.model;

import com.konorpg.KonoRPG;
import com.konorpg.entity.MimicEntity;
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

/** Chest-shaped model; the lid snaps open and shut when the mimic is revealed. */
public class MimicModel extends EntityModel<MimicEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(KonoRPG.id("mimic"), "main");
    private final ModelPart root, lid;

    public MimicModel(ModelPart root) {
        this.root = root;
        this.lid = root.getChild("lid");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p = mesh.getRoot();
        p.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 19).addBox(-7.0f, -10.0f, -7.0f, 14.0f, 10.0f, 14.0f), PartPose.offset(0.0f, 24.0f, 0.0f));
        p.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0f, -5.0f, -14.0f, 14.0f, 5.0f, 14.0f), PartPose.offset(0.0f, 14.0f, 7.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MimicEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        lid.xRot = e.isRevealed() ? -0.75f - Mth.sin(ageInTicks * 0.45f) * 0.3f : 0.0f;
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer buf, int light, int overlay, int color) {
        root.render(ps, buf, light, overlay, color);
    }
}
