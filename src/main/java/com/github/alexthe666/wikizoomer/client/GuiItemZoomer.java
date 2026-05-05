package com.github.alexthe666.wikizoomer.client;

import com.github.alexthe666.wikizoomer.tileentity.TileEntityZoomerBase;
import com.github.alexthe666.wikizoomer.client.ExportManager;
import com.github.alexthe666.wikizoomer.client.ExportTask;
import com.github.alexthe666.wikizoomer.client.GuiBatchExport;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ForgeSlider;

import java.awt.*;

@OnlyIn(Dist.CLIENT)
public class GuiItemZoomer extends Screen {

    public static final ResourceLocation GREENSCREEN = new ResourceLocation("wikizoomer:textures/gui/greenscreen.png");
    private final TileEntityZoomerBase zoomerBase;
    private ExportTask.Background background = ExportTask.Background.GREENSCREEN;
    private float sliderValue = 100;
    private float prevSliderValue = sliderValue;
    private int exportSizeIndex = findDefaultExportSizeIndex();
    private static final int[] EXPORT_SIZES = ExportManager.getExportSizes();

    public GuiItemZoomer(TileEntityZoomerBase zoomerBase) {
        super(Component.translatable("item_zoomer"));
        this.zoomerBase = zoomerBase;
        this.init();
    }

    private void setSliderValue(int i, float sliderValue) {
        this.sliderValue = Math.round(Mth.clamp(sliderValue, 1, 300F));
        prevSliderValue = this.sliderValue;
    }

    public void init() {
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
        this.addRenderableWidget(new ForgeSlider(col1X, row1Y, buttonWidth, buttonHeight, Component.translatable("gui.wikizoomer.zoom"), Component.literal("%"), 1, 300, sliderValue, 1, 1, true) {
            @Override
            protected void applyValue() {
                GuiItemZoomer.this.setSliderValue(2, (float)getValue());
            }
        });
        this.addRenderableWidget(Button.builder(backgroundLabel, (button) -> {
            GuiItemZoomer.this.background = GuiItemZoomer.this.background == ExportTask.Background.GREENSCREEN
                    ? ExportTask.Background.TRANSPARENT
                    : ExportTask.Background.GREENSCREEN;
            init();
        }).size(buttonWidth, buttonHeight).pos(col2X, row1Y).build());
        this.addRenderableWidget(Button.builder(export, (button) -> {
            ExportTask task = ExportManager.createItemTask(zoomerBase.getItem(0), sliderValue, background, getExportSize(), false);
            if (task == null) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(Component.translatable("gui.wikizoomer.export_no_item"));
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
                    renderGreenscreen(guiGraphics);
                } else {
                    this.renderBackground(guiGraphics);
                }
            } catch (Exception e) {

            }
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
            renderFocus(guiGraphics);
            int i = (this.width - 248) / 2 + 10;
            int j = (this.height - 166) / 2 + 8;
            if(mouseX > (i - sliderValue) && mouseX < (i + sliderValue) && mouseY > (j - sliderValue) && mouseY < (j + sliderValue)){
                ItemStack itemStack = zoomerBase.getItem(0);
                guiGraphics.renderTooltip(font, itemStack, -500, -500);
            }
        }

    }

    private void renderFocus(GuiGraphics guiGraphics) {
        int i = (this.width - 248) / 2 + 10;
        int j = (this.height - 166) / 2 + 8;
        ItemStack itemStack = zoomerBase.getItem(0);
        float scale1 = (sliderValue / 100F);
        float scale = scale1 * 12F;
        if (!itemStack.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(i, j, 10F);
            guiGraphics.pose().translate(113.5F - scale1 * 100, 76 - scale1 * 100, 1000F - sliderValue * 20);
            guiGraphics.pose().scale(scale, scale, scale);
            guiGraphics.renderItem(Minecraft.getInstance().player, itemStack, 0, 0, 1);
            guiGraphics.pose().popPose();
        }
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
