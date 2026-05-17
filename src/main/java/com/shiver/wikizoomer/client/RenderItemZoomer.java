package com.shiver.wikizoomer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shiver.wikizoomer.block.BlockZoomer;
import com.shiver.wikizoomer.tileentity.TileEntityItemZoomer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

public class RenderItemZoomer implements BlockEntityRenderer<TileEntityItemZoomer> {

    public RenderItemZoomer(BlockEntityRendererProvider.Context manager) {
    }

    @Override
    public void render(TileEntityItemZoomer tileEntityIn, float partialTicks, PoseStack poseStack,
                        MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        ItemStack stack = ItemStack.EMPTY;
        int ticksExisted = 0;
        if (tileEntityIn != null && tileEntityIn.getLevel() != null
                && tileEntityIn.getLevel().getBlockState(tileEntityIn.getBlockPos()).getBlock() instanceof BlockZoomer) {
            stack = tileEntityIn.getItem(0);
            ticksExisted = tileEntityIn.ticksExisted;
        }
        float rrr = (float) ticksExisted - 1 + partialTicks;
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.25D, 0.5D);
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(rrr * 2F)));
        poseStack.translate(0D, 0.1F + Math.sin(rrr * 0.05F) * 0.1F, 0D);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, tileEntityIn.getLevel(), null, 0);
        Minecraft.getInstance().getItemRenderer().render(stack, ItemDisplayContext.FIXED, false, poseStack, bufferIn, combinedLightIn, OverlayTexture.NO_OVERLAY, model);
        poseStack.popPose();
        poseStack.popPose();
    }
}
