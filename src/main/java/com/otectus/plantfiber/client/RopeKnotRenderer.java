package com.otectus.plantfiber.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.otectus.plantfiber.PlantFiberMod;
import com.otectus.plantfiber.entity.RopeKnotEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class RopeKnotRenderer extends EntityRenderer<RopeKnotEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(PlantFiberMod.MODID, "textures/entity/rope_knot.png");

    private final RopeKnotModel model;

    public RopeKnotRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RopeKnotModel(context.bakeLayer(RopeKnotModel.LAYER));
    }

    @Override
    public void render(RopeKnotEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // The model is authored with its side strand toward +Z (south); rotate so it points at the
        // exit direction. Standard entity-model Y flip; origin is already at the post centre.
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getExitDirection().toYRot()));
        VertexConsumer vc = buffer.getBuffer(this.model.renderType(TEXTURE));
        this.model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RopeKnotEntity entity) {
        return TEXTURE;
    }
}
