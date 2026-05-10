package com.shiver.wikizoomer.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.shiver.wikizoomer.WikiZoomerUnofficialClient;
import com.shiver.wikizoomer.client.ExportManager;
import com.shiver.wikizoomer.client.ExportTask;
import com.shiver.wikizoomer.tileentity.TileEntityEntityZoomer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;

@OnlyIn(Dist.CLIENT)
public class GuiEntityZoomer extends Screen {
    private final TileEntityEntityZoomer zoomerBase;
    private ExportTask.Background background = ExportTask.Background.GREENSCREEN;
    private float sliderValue = 100;
    private float prevSliderValue = sliderValue;
    private int exportSizeIndex = findDefaultExportSizeIndex();
    private static final int[] EXPORT_SIZES = ExportManager.getExportSizes();
    private float rotX = 30F;
    private float rotY = 45F;
    private ZoomSlider zoomSlider;

    public GuiEntityZoomer(TileEntityEntityZoomer zoomerBase) {
        super(Component.translatable("entity_zoomer"));
        this.zoomerBase = zoomerBase;
    }

    private void setSliderValue(float value) {
        this.sliderValue = Math.round(Mth.clamp(value, 1, 1000F));
        prevSliderValue = this.sliderValue;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        int i = this.width / 2;
        int j = (this.height - 166) / 2;
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

        this.zoomSlider = new ZoomSlider(col1X, row1Y, buttonWidth, buttonHeight);
        this.addRenderableWidget(this.zoomSlider);

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.wikizoomer.background", getBackgroundLabel()), (button) -> {
                    this.background = this.background == ExportTask.Background.GREENSCREEN
                            ? ExportTask.Background.TRANSPARENT : ExportTask.Background.GREENSCREEN;
                    init();
                }).size(buttonWidth, buttonHeight).pos(col2X, row1Y).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.wikizoomer.export_png"), (button) -> {
                    Entity renderEntity = zoomerBase.getCachedEntity();
                    ExportTask task = ExportManager.createEntityTask(renderEntity, sliderValue, background, getExportSize(), false, rotX, rotY);
                    if (task == null) {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.sendSystemMessage(Component.translatable("gui.wikizoomer.export_no_entity"));
                        }
                    } else {
                        ExportManager.enqueue(task);
                    }
                }).size(buttonWidth, buttonHeight).pos(col3X, row1Y).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.wikizoomer.resolution", getExportSize(), getExportSize()), (button) -> {
                    exportSizeIndex = (exportSizeIndex + 1) % EXPORT_SIZES.length;
                    init();
                }).size(buttonWidth, buttonHeight).pos(col1X, row2Y).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.wikizoomer.batch_export"), (button) -> {
                    Minecraft.getInstance().setScreen(new GuiBatchExport());
                }).size(buttonWidth, buttonHeight).pos(col2X, row2Y).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.wikizoomer.close"), (button) -> {
                    Minecraft.getInstance().setScreen(null);
                }).size(buttonWidth, buttonHeight).pos(col3X, row2Y).build());
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderMenuBackground(guiGraphics);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (getMinecraft() != null) {
            try {
                if (background == ExportTask.Background.GREENSCREEN) {
                    guiGraphics.fill(0, 0, this.width, this.height, 0xFF4CFF00);
                } else {
                    this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
                }
            } catch (Exception e) {
                // ignore
            }
        }
        renderFocus(guiGraphics);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 5000F);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.pose().popPose();
    }

    private void renderFocus(GuiGraphics guiGraphics) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 1000F);
        guiGraphics.pose().scale(1.0F, 1.0F, 1.0F);
        Entity renderEntity = zoomerBase.getCachedEntity();
        float scale = prevSliderValue + (sliderValue - prevSliderValue) * Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        if (renderEntity != null) {
            float f1 = Math.max(renderEntity.getBbWidth(), renderEntity.getBbHeight());
            int i = this.width / 2;
            int j = (this.height + (int) ((scale / 100F) * (renderEntity.getBbHeight() * 100F))) / 2;

            boolean isMimic = false;
            if (WikiZoomerUnofficialClient.dataMimic != null) {
                if (renderEntity.getType() == WikiZoomerUnofficialClient.dataMimic.getType()) {
                    renderEntity = WikiZoomerUnofficialClient.dataMimic;
                    isMimic = true;
                }
            }
            if (renderEntity instanceof LivingEntity) {
                guiGraphics.pose().translate(i, j, 10F);
                drawEntityOnScreen(guiGraphics, 100, 0, scale, false, rotX, rotY, 0, 0, 0, renderEntity, isMimic);
            }
        }
        guiGraphics.pose().popPose();
        prevSliderValue = sliderValue;
    }

    public static void drawEntityOnScreen(GuiGraphics guiGraphics, int posX, int posY, float scale, boolean follow,
                                          double xRot, double yRot, double zRot, float mouseX, float mouseY,
                                          Entity entity, boolean isMimic) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
        guiGraphics.pose().scale(scale, scale, scale);
        entity.setOnGround(false);

        Quaternionf quaternion1 = Axis.XP.rotationDegrees((float) xRot);
        Quaternionf quaternion2 = Axis.YP.rotationDegrees((float) yRot);
        Quaternionf quaternion = Axis.ZP.rotationDegrees((float) zRot);
        quaternion.mul(quaternion1);

        float halfHeight = entity.getBbHeight() / 2.0F;
        guiGraphics.pose().translate(0.0F, halfHeight, 0.0F);
        guiGraphics.pose().mulPose(quaternion);
        guiGraphics.pose().mulPose(quaternion2);
        guiGraphics.pose().translate(0.0F, -halfHeight, 0.0F);

        Vector3f light0 = new Vector3f(-0.2F, 0.0F, 1.0F).normalize();
        Vector3f light1 = new Vector3f(-0.2F, -1.0F, 0.0F).normalize();
        RenderSystem.setShaderLights(light0, light1);

        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        quaternion1.conjugate();
        entityRenderDispatcher.overrideCameraOrientation(quaternion1);
        entityRenderDispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        if (!isMimic) {
            entity.setYRot(0.0F);
            entity.setXRot(0.0F);
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.yBodyRot = 0.0F;
                livingEntity.yHeadRotO = 0.0F;
                livingEntity.yHeadRot = 0.0F;
            }
            entity.setOldPosAndRot();
        }

        AbstractTexture entityTex = null;
        boolean origBlur = false;
        boolean origMipmap = false;
        try {
            ResourceLocation texLoc = entityRenderDispatcher.getRenderer(entity).getTextureLocation(entity);
            entityTex = Minecraft.getInstance().getTextureManager().getTexture(texLoc);
            if (entityTex != null) {
                Field blurField = AbstractTexture.class.getDeclaredField("blur");
                Field mipmapField = AbstractTexture.class.getDeclaredField("mipmap");
                blurField.setAccessible(true);
                mipmapField.setAccessible(true);
                origBlur = blurField.getBoolean(entityTex);
                origMipmap = mipmapField.getBoolean(entityTex);
                blurField.setBoolean(entityTex, false);
                mipmapField.setBoolean(entityTex, false);
                entityTex.setFilter(false, false);
            }
        } catch (Exception ignored) {}

        entityRenderDispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), guiGraphics.pose(), bufferSource, 15728880);
        bufferSource.endBatch();

        if (entityTex != null) {
            try {
                Field blurField = AbstractTexture.class.getDeclaredField("blur");
                Field mipmapField = AbstractTexture.class.getDeclaredField("mipmap");
                blurField.setAccessible(true);
                mipmapField.setAccessible(true);
                blurField.setBoolean(entityTex, origBlur);
                mipmapField.setBoolean(entityTex, origMipmap);
                entityTex.setFilter(origBlur, origMipmap);
            } catch (Exception ignored) {}
        }
        guiGraphics.flush();
        entityRenderDispatcher.setRenderShadow(true);
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    @Override
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

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        if (button == 0) {
            rotY -= (float) dragX;
            rotX += (float) dragY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalDelta, double delta) {
        if (super.mouseScrolled(mouseX, mouseY, horizontalDelta, delta)) {
            return true;
        }
        float newSliderValue = this.sliderValue + (float) delta * 10F;
        setSliderValue(newSliderValue);
        if (zoomSlider != null) {
            zoomSlider.updateValue(this.sliderValue);
        }
        return true;
    }

    private class ZoomSlider extends AbstractSliderButton {
        public ZoomSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.translatable("gui.wikizoomer.zoom"), GuiEntityZoomer.this.sliderValue / 1000.0);
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("gui.wikizoomer.zoom").append(": " + (int) GuiEntityZoomer.this.sliderValue + "%"));
        }

        @Override
        protected void applyValue() {
            GuiEntityZoomer.this.setSliderValue((float) (this.value * 1000.0));
        }

        public void updateValue(float newValue) {
            this.value = newValue / 1000.0;
            this.updateMessage();
        }
    }
}
