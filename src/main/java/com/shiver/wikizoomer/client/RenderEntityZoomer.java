package com.shiver.wikizoomer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shiver.wikizoomer.WikiZoomerUnofficialClient;
import com.shiver.wikizoomer.block.BlockZoomer;
import com.shiver.wikizoomer.tileentity.TileEntityEntityZoomer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;

public class RenderEntityZoomer implements BlockEntityRenderer<TileEntityEntityZoomer> {

    public RenderEntityZoomer(BlockEntityRendererProvider.Context manager) {
    }

    @Override
    public void render(TileEntityEntityZoomer tileEntityIn, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        Entity renderEntity = null;
        int ticksExisted = 0;
        if (tileEntityIn != null && tileEntityIn.getLevel() != null
                && tileEntityIn.getLevel().getBlockState(tileEntityIn.getBlockPos()).getBlock() instanceof BlockZoomer) {
            renderEntity = tileEntityIn.getCachedEntity();
            ticksExisted = tileEntityIn.ticksExisted;
        }
        float rrr = (float) ticksExisted - 1 + partialTicks;
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.0D, 0.5D);
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(rrr * 2F)));
        poseStack.translate(0D, 0.1F + Math.sin(rrr * 0.05F) * 0.1F, 0D);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        if (renderEntity != null) {
            boolean isMimic = false;
            if (WikiZoomerUnofficialClient.dataMimic != null) {
                if (renderEntity.getType() == WikiZoomerUnofficialClient.dataMimic.getType()) {
                    renderEntity = WikiZoomerUnofficialClient.dataMimic;
                    isMimic = true;
                }
            }
            float f = 0.75F;
            float f1 = Math.max(renderEntity.getBbWidth(), renderEntity.getBbHeight());
            if ((double) f1 > 1.0D) {
                f /= f1;
            }
            poseStack.translate(0.0D, 0.4D, 0.0D);
            poseStack.translate(0.0D, -0.2D, 0.0D);
            poseStack.scale(f, f, f);
            if (!isMimic) {
                renderEntity.setYRot(0.0F);
                renderEntity.setXRot(0.0F);
                if (renderEntity instanceof LivingEntity livingEntity) {
                    livingEntity.yBodyRot = 0.0F;
                    livingEntity.yHeadRotO = 0.0F;
                    livingEntity.yHeadRot = 0.0F;
                }
            }
            Minecraft.getInstance().getEntityRenderDispatcher().render(renderEntity, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, poseStack, bufferIn, combinedLightIn);
        }
        poseStack.popPose();
        poseStack.popPose();
    }
}
