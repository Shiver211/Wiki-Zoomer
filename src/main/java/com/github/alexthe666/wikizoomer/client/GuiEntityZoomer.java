package com.github.alexthe666.wikizoomer.client;

import com.github.alexthe666.wikizoomer.ClientProxy;
import com.github.alexthe666.wikizoomer.tileentity.TileEntityEntityZoomer;
import com.github.alexthe666.wikizoomer.client.ExportManager;
import com.github.alexthe666.wikizoomer.client.ExportTask;
import com.github.alexthe666.wikizoomer.client.GuiBatchExport;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Quaternionf;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class GuiEntityZoomer extends Screen {
    private TileEntityEntityZoomer zoomerBase;
    private ExportTask.Background background = ExportTask.Background.GREENSCREEN;
    private float sliderValue = 100;
    private float prevSliderValue = sliderValue;
    private int exportSizeIndex = findDefaultExportSizeIndex();
    private static final int[] EXPORT_SIZES = ExportManager.getExportSizes();

    public GuiEntityZoomer(TileEntityEntityZoomer zoomerBase) {
        super(Component.translatable("entity_zoomer"));
        this.zoomerBase = zoomerBase;
    }

    private void setSliderValue(int i, float sliderValue) {
        this.sliderValue = Math.round(Mth.clamp(sliderValue, 1, 300F));
        prevSliderValue = this.sliderValue;
    }

    protected void init() {
        super.init();
        this.clearWidgets();
        int i = (this.width) / 2;
        int j = (this.height - 166) / 2;
        MutableComponent exit = Component.translatable("gui.wikizoomer.close");
        MutableComponent backgroundLabel = Component.translatable("gui.wikizoomer.background", getBackgroundLabel());
        MutableComponent export = Component.translatable("gui.wikizoomer.export_png");
        MutableComponent batchExport = Component.translatable("gui.wikizoomer.batch_export");
        MutableComponent resolutionLabel = Component.translatable("gui.wikizoomer.resolution", getExportSize(), getExportSize());
        int buttonWidth = 120;
        int buttonHeight = 20;
        int spacing = 20;
        int rowWidth = buttonWidth * 3 + spacing * 2;
        int startX = i - rowWidth / 2;
        int col1X = startX;
        int col2X = startX + buttonWidth + spacing;
        int col3X = startX + (buttonWidth + spacing) * 2;
        int row1Y = j + 180;
        int row2Y = row1Y + 22;
        this.addRenderableWidget(new ForgeSlider(col1X, row1Y, buttonWidth, buttonHeight, Component.translatable("gui.wikizoomer.zoom"), Component.literal("%"), 1, 300, sliderValue, 1, 1, true){
            @Override
            protected void applyValue() {
                GuiEntityZoomer.this.setSliderValue(2, (float)getValue());
            }
        });
        this.addRenderableWidget(Button.builder(backgroundLabel, (button) -> {
            GuiEntityZoomer.this.background = GuiEntityZoomer.this.background == ExportTask.Background.GREENSCREEN
                    ? ExportTask.Background.TRANSPARENT
                    : ExportTask.Background.GREENSCREEN;
            init();
        }).size(buttonWidth, buttonHeight).pos(col2X, row1Y).build());
        this.addRenderableWidget(Button.builder(export, (button) -> {
            Entity renderEntity = zoomerBase.getCachedEntity();
            ExportTask task = ExportManager.createEntityTask(renderEntity, sliderValue, background, getExportSize(), false);
            if (task == null) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(Component.translatable("gui.wikizoomer.export_no_entity"));
                }
            } else {
                ExportManager.enqueue(task);
            }
        }).size(buttonWidth, buttonHeight).pos(col3X, row1Y).build());
        this.addRenderableWidget(Button.builder(resolutionLabel, (button) -> {
            exportSizeIndex = (exportSizeIndex + 1) % EXPORT_SIZES.length;
            init();
        }).size(buttonWidth, buttonHeight).pos(col1X, row2Y).build());
        this.addRenderableWidget(Button.builder(batchExport, (button) -> {
            Minecraft.getInstance().setScreen(new GuiBatchExport());
        }).size(buttonWidth, buttonHeight).pos(col2X, row2Y).build());
        this.addRenderableWidget(Button.builder(exit, (button) -> {
            Minecraft.getInstance().setScreen(null);
        }).size(buttonWidth, buttonHeight).pos(col3X, row2Y).build());
    }

    public void renderGreenscreen(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF4CFF00);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (getMinecraft() != null) {
            try {
                if (background == ExportTask.Background.GREENSCREEN) {
                    this.renderGreenscreen(guiGraphics);
                } else {
                    this.renderBackground(guiGraphics);
                }
            } catch (Exception e) {

            }
        }
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderFocus(guiGraphics);
    }

    private void renderFocus(GuiGraphics guiGraphics) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 1000F);
        Entity renderEntity = zoomerBase.getCachedEntity();
        float scale = prevSliderValue + (sliderValue - prevSliderValue) * Minecraft.getInstance().getFrameTime();
        if (renderEntity != null) {
            float f = 0.75F;
            float f1 = Math.max(renderEntity.getBbWidth(), renderEntity.getBbHeight());
            if ((double)f1 > 1.0D) {
                f /= f1;
            }
            float scale2 = scale;
            int i = (this.width) / 2;
            int j = (this.height + (int)((scale / 100F) * (renderEntity.getBbHeight() * 100F))) / 2;

            boolean isMimic = false;
            if(ClientProxy.dataMimic != null){
                if(renderEntity.getType() == ClientProxy.dataMimic.getType()){
                    renderEntity = ClientProxy.dataMimic;
                    isMimic = true;
                }
            }
            if(renderEntity instanceof LivingEntity){
                guiGraphics.pose().translate(i, j, 10F);
                drawEntityOnScreen(guiGraphics, 100, 0, scale2,  false, -30, 135, 180, 0, 0, (LivingEntity)renderEntity, isMimic);
            }
        }
        guiGraphics.pose().popPose();
        prevSliderValue = sliderValue;
    }

    public static void drawEntityOnScreen(GuiGraphics guiGraphics, int posX, int posY, float scale, boolean follow, double xRot, double yRot, double zRot, float mouseX, float mouseY, Entity entity, boolean isMimic) {
        guiGraphics.pose().pushPose();
        float f = (float) Math.atan(-mouseX / 40.0F);
        float f1 = (float) Math.atan(mouseY / 40.0F);
        float partialTicksForRender = Minecraft.getInstance().getFrameTime();
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
        guiGraphics.pose().scale(scale, scale, scale);
        entity.setOnGround(false);
        float partialTicks = Minecraft.getInstance().getFrameTime();
        RenderSystem.applyModelViewMatrix();

        Quaternionf quaternion1 = Axis.XP.rotationDegrees(30);
        Quaternionf quaternion2 = Axis.YP.rotationDegrees(45);
        Quaternionf quaternion = Axis.ZP.rotationDegrees(0.0F);
        quaternion.mul(quaternion1);
        guiGraphics.pose().mulPose(quaternion);
        guiGraphics.pose().mulPose(quaternion2);
        Vector3f INVENTORY_DIFFUSE_LIGHT_0 = Util.make(new Vector3f(-0.2F, 0.0F, 1.0F), Vector3f::normalize);
        Vector3f INVENTORY_DIFFUSE_LIGHT_1 = Util.make(new Vector3f(-0.2F, -1.0F, 0.0F), Vector3f::normalize);

        RenderSystem.setShaderLights(INVENTORY_DIFFUSE_LIGHT_0, INVENTORY_DIFFUSE_LIGHT_1);

        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        quaternion1.conjugate();
        entityrenderdispatcher.overrideCameraOrientation(quaternion1);
        entityrenderdispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
        if (!isMimic) {
            entity.setYRot(0.0F);
            entity.setXRot(0.0F);
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).yBodyRot = 0.0F;
                ((LivingEntity) entity).yHeadRotO = 0.0F;
                ((LivingEntity) entity).yHeadRot = 0.0F;
            }
            entity.setOldPosAndRot();
        }
        RenderSystem.runAsFancy(() -> {
            entityrenderdispatcher.render( entity, 0.0D, 0.0D, 0.0D, 0.0F, partialTicksForRender, guiGraphics.pose(), multibuffersource$buffersource, 15728880);
        });
        multibuffersource$buffersource.endBatch();
        guiGraphics.flush();
        entityrenderdispatcher.setRenderShadow(true);
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    public boolean isPauseScreen() {
        return false;
    }

    private static int findDefaultExportSizeIndex() {
        int defaultSize = ExportManager.getDefaultExportSize();
        for (int i = 0; i < EXPORT_SIZES.length; i++) {
            if (EXPORT_SIZES[i] == defaultSize) {
                return i;
            }
        }
        return EXPORT_SIZES.length - 1;
    }

    private int getExportSize() {
        return EXPORT_SIZES[exportSizeIndex];
    }

    private Component getBackgroundLabel() {
        return background == ExportTask.Background.GREENSCREEN
                ? Component.translatable("gui.wikizoomer.background.greenscreen")
                : Component.translatable("gui.wikizoomer.background.transparent");
    }
}
