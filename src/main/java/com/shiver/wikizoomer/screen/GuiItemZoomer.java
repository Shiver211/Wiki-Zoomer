package com.shiver.wikizoomer.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.shiver.wikizoomer.client.ExportManager;
import net.minecraft.client.renderer.texture.TextureAtlas;

import java.lang.reflect.Field;
import com.shiver.wikizoomer.client.ExportTask;
import com.shiver.wikizoomer.tileentity.TileEntityZoomerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class GuiItemZoomer extends Screen {

    public static final ResourceLocation GREENSCREEN = Objects.requireNonNull(ResourceLocation.tryParse("wikizoomer:textures/gui/greenscreen.png"));
    private final TileEntityZoomerBase zoomerBase;
    private ExportTask.Background background = ExportTask.Background.GREENSCREEN;
    private float sliderValue = 100;
    private float prevSliderValue = sliderValue;
    private int exportSizeIndex = findDefaultExportSizeIndex();
    private static final int[] EXPORT_SIZES = ExportManager.getExportSizes();
    private float rotX = 0F;
    private float rotY = 0F;
    private ZoomSlider zoomSlider;
    private static final int FRAME_COLOR = 0xFFFF0000;

    public GuiItemZoomer(TileEntityZoomerBase zoomerBase) {
        super(Component.translatable("item_zoomer"));
        this.zoomerBase = zoomerBase;
        ExportManager.ExportSettings settings = ExportManager.getLastItemSettings();
        this.background = settings.background;
        this.sliderValue = settings.zoomPercent;
        this.prevSliderValue = this.sliderValue;
        this.exportSizeIndex = findExportSizeIndex(settings.exportSize);
        this.rotX = settings.rotX;
        this.rotY = settings.rotY;
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
        int row3Y = row2Y + 22;

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
                    ExportManager.rememberItemSettings(sliderValue, background, getExportSize(), rotX, rotY);
                    ExportTask task = ExportManager.createItemTask(zoomerBase.getItem(0), sliderValue, background, getExportSize(), false, rotX, rotY);
                    if (task == null) {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.sendSystemMessage(Component.translatable("gui.wikizoomer.export_no_item"));
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
                    ExportManager.rememberItemSettings(sliderValue, background, getExportSize(), rotX, rotY);
                    Minecraft.getInstance().setScreen(new GuiBatchExport());
                }).size(buttonWidth, buttonHeight).pos(col2X, row2Y).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.wikizoomer.clear_config"), (button) -> {
                    resetSettings();
                    init();
                }).size(buttonWidth, buttonHeight).pos(col2X, row3Y).build());

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
            renderFocus(guiGraphics);
            renderCropFrame(guiGraphics);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 5000F);
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
            guiGraphics.pose().popPose();
            int i = (this.width - 248) / 2 + 10;
            int j = (this.height - 166) / 2 + 8;
            if (mouseX > (i - sliderValue) && mouseX < (i + sliderValue) && mouseY > (j - sliderValue) && mouseY < (j + sliderValue)) {
                ItemStack itemStack = zoomerBase.getItem(0);
                guiGraphics.renderTooltip(this.font, itemStack, mouseX, mouseY);
            }
        }
    }

    private void renderCropFrame(GuiGraphics guiGraphics) {
        int size = getPreviewSize();
        int left = (this.width - size) / 2;
        int top = getPreviewTop(size);
        guiGraphics.fill(left, top, left + size, top + 2, FRAME_COLOR);
        guiGraphics.fill(left, top + size - 2, left + size, top + size, FRAME_COLOR);
        guiGraphics.fill(left, top, left + 2, top + size, FRAME_COLOR);
        guiGraphics.fill(left + size - 2, top, left + size, top + size, FRAME_COLOR);
    }

    private int getPreviewSize() {
        return Math.max(64, Math.min(Math.min(this.width - 40, this.height - 130), 512));
    }

    private int getPreviewTop(int previewSize) {
        return Math.max(8, (this.height - previewSize) / 2);
    }

    private void renderFocus(GuiGraphics guiGraphics) {
        int i = (this.width - 248) / 2 + 10;
        int j = (this.height - 166) / 2 + 8;
        ItemStack itemStack = zoomerBase.getItem(0);
        float scale1 = sliderValue / 100F;
        float scale = scale1 * 12F;
        if (!itemStack.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(i, j, 10F);
            guiGraphics.pose().scale(1.0F, 1.0F, 1.0F);
            guiGraphics.pose().translate(113.5F - scale1 * 100, 76 - scale1 * 100, 2500F - sliderValue * 20);
            guiGraphics.pose().scale(scale, scale, scale);
            guiGraphics.pose().translate(8.0F, 8.0F, 150.0F);
            guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(rotX));
            guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(rotY));
            guiGraphics.pose().translate(-8.0F, -8.0F, -150.0F);

            TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
            try {
                Field blurField = atlas.getClass().getSuperclass().getDeclaredField("blur");
                Field mipmapField = atlas.getClass().getSuperclass().getDeclaredField("mipmap");
                blurField.setAccessible(true);
                mipmapField.setAccessible(true);
                boolean origBlur = blurField.getBoolean(atlas);
                boolean origMipmap = mipmapField.getBoolean(atlas);
                blurField.setBoolean(atlas, false);
                mipmapField.setBoolean(atlas, false);
                atlas.setFilter(false, false);

                guiGraphics.renderItem(Minecraft.getInstance().player, itemStack, 0, 0, 1);

                blurField.setBoolean(atlas, origBlur);
                mipmapField.setBoolean(atlas, origMipmap);
                atlas.setFilter(origBlur, origMipmap);
            } catch (Exception e) {
                guiGraphics.renderItem(Minecraft.getInstance().player, itemStack, 0, 0, 1);
            }

            guiGraphics.pose().popPose();
        }
    }

    private void resetSettings() {
        ExportManager.resetItemSettings();
        ExportManager.ExportSettings settings = ExportManager.getLastItemSettings();
        this.background = settings.background;
        this.sliderValue = settings.zoomPercent;
        this.prevSliderValue = this.sliderValue;
        this.exportSizeIndex = findExportSizeIndex(settings.exportSize);
        this.rotX = settings.rotX;
        this.rotY = settings.rotY;
        if (zoomSlider != null) {
            zoomSlider.updateValue(this.sliderValue);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int findDefaultExportSizeIndex() {
        int defaultSize = ExportManager.getDefaultExportSize();
        return findExportSizeIndex(defaultSize);
    }

    private static int findExportSizeIndex(int exportSize) {
        for (int i = 0; i < EXPORT_SIZES.length; i++) {
            if (EXPORT_SIZES[i] == exportSize) {
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
            rotY += (float) dragX;
            rotX -= (float) dragY;
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
            super(x, y, width, height, Component.translatable("gui.wikizoomer.zoom"), GuiItemZoomer.this.sliderValue / 1000.0);
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("gui.wikizoomer.zoom").append(": " + (int) GuiItemZoomer.this.sliderValue + "%"));
        }

        @Override
        protected void applyValue() {
            GuiItemZoomer.this.setSliderValue((float) (this.value * 1000.0));
        }

        public void updateValue(float newValue) {
            this.value = newValue / 1000.0;
            this.updateMessage();
        }
    }
}
