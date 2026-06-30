package com.otectus.plantfiber.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.otectus.plantfiber.PlantFiberMod;
import com.otectus.plantfiber.entity.RopeKnotEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * A small collar of rope hugging the central fence post (≈ the 6–10 px core) plus a 4×4 side
 * strand toward +Z that matches the descending rope column's thickness; the renderer rotates the
 * whole model so the strand points to the exit direction. Coordinates are in model pixels around
 * the origin (the post centre).
 */
public class RopeKnotModel extends EntityModel<RopeKnotEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(PlantFiberMod.MODID, "rope_knot"), "main");

    private final ModelPart root;

    public RopeKnotModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition part = mesh.getRoot();
        // Authored near the post top: the renderer's scale(-1,-1,1) flips Y, so the more-negative
        // y here (−8..−4) renders at the upper part of the fence (world y 12..16), where the rope
        // drapes over the top edge. The four bands wrap the post (Supplementaries rope_knot_post
        // style) and the +Z stub reaches out to the fence's outer face to meet the block drape arm.
        part.addOrReplaceChild("knot", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -8.0F, -3.0F, 6.0F, 4.0F, 1.0F)   // -Z band
                        .texOffs(0, 0).addBox(-3.0F, -8.0F, 2.0F, 6.0F, 4.0F, 1.0F)    // +Z band
                        .texOffs(0, 0).addBox(-3.0F, -8.0F, -2.0F, 1.0F, 4.0F, 4.0F)   // -X band
                        .texOffs(0, 0).addBox(2.0F, -8.0F, -2.0F, 1.0F, 4.0F, 4.0F)    // +X band
                        .texOffs(0, 12).addBox(-2.0F, -8.0F, 3.0F, 4.0F, 4.0F, 5.0F),  // +Z stub to fence outer face, 4x4
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(RopeKnotEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // static decoration
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
