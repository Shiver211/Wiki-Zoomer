package com.github.alexthe666.wikizoomer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class GuiBatchExport extends Screen {
    private ModListWidget modList;
    private List<ModEntry> entries = new ArrayList<>();
    private boolean exportItems = true;
    private boolean exportEntities = true;
    private ExportTask.Background background = ExportTask.Background.GREENSCREEN;
    private int exportSizeIndex = findDefaultExportSizeIndex();
    private static final int[] EXPORT_SIZES = ExportManager.getExportSizes();
    private float zoomPercent = ExportManager.getDefaultZoom();

    public GuiBatchExport() {
        super(Component.translatable("gui.wikizoomer.batch_title"));
    }

    @Override
    protected void init() {
        super.init();
        this.entries = buildEntries();
        this.clearWidgets();
        int buttonWidth = 120;
        int buttonHeight = 20;
        int spacing = 20;
        int i = this.width / 2;
        int rowWidth = buttonWidth * 3 + spacing * 2;
        int startX = i - rowWidth / 2;
        int col1X = startX;
        int col2X = startX + buttonWidth + spacing;
        int col3X = startX + (buttonWidth + spacing) * 2;
        int row1Y = this.height - 90;
        int row2Y = row1Y + 22;
        int row3Y = row2Y + 22;
        int listTop = 32;
        int listBottom = row1Y - 10;
        this.modList = new ModListWidget(this.minecraft, this.width, this.height, listTop, listBottom, 20, this.entries);
        this.addRenderableWidget(this.modList);

        Button itemsButton = Button.builder(Component.translatable("gui.wikizoomer.batch_items", exportItems ? "ON" : "OFF"), (button) -> {
            exportItems = !exportItems;
            init();
        }).size(buttonWidth, buttonHeight).pos(col1X, row1Y).build();
        Button entitiesButton = Button.builder(Component.translatable("gui.wikizoomer.batch_entities", exportEntities ? "ON" : "OFF"), (button) -> {
            exportEntities = !exportEntities;
            init();
        }).size(buttonWidth, buttonHeight).pos(col2X, row1Y).build();
        Button backgroundButton = Button.builder(Component.translatable("gui.wikizoomer.background", getBackgroundLabel()), (button) -> {
            background = background == ExportTask.Background.GREENSCREEN ? ExportTask.Background.TRANSPARENT : ExportTask.Background.GREENSCREEN;
            init();
        }).size(buttonWidth, buttonHeight).pos(col3X, row1Y).build();
        this.addRenderableWidget(itemsButton);
        this.addRenderableWidget(entitiesButton);
        this.addRenderableWidget(backgroundButton);

        this.addRenderableWidget(new ForgeSlider(col1X, row2Y, buttonWidth, buttonHeight, Component.translatable("gui.wikizoomer.zoom"), Component.literal("%"), 1, 300, zoomPercent, 1, 1, true) {
            @Override
            protected void applyValue() {
                GuiBatchExport.this.zoomPercent = (float)getValue();
            }
        });
        Button resolutionButton = Button.builder(Component.translatable("gui.wikizoomer.resolution", getExportSize(), getExportSize()), (button) -> {
            exportSizeIndex = (exportSizeIndex + 1) % EXPORT_SIZES.length;
            init();
        }).size(buttonWidth, buttonHeight).pos(col2X, row2Y).build();
        Button selectAllButton = Button.builder(Component.translatable("gui.wikizoomer.batch_select_all"), (button) -> {
            setAllSelected(true);
        }).size(buttonWidth, buttonHeight).pos(col3X, row2Y).build();
        this.addRenderableWidget(resolutionButton);
        this.addRenderableWidget(selectAllButton);

        Button clearButton = Button.builder(Component.translatable("gui.wikizoomer.batch_clear"), (button) -> {
            setAllSelected(false);
        }).size(buttonWidth, buttonHeight).pos(col1X, row3Y).build();
        Button startButton = Button.builder(Component.translatable("gui.wikizoomer.batch_start"), (button) -> {
            startExport();
        }).size(buttonWidth, buttonHeight).pos(col2X, row3Y).build();
        Button closeButton = Button.builder(Component.translatable("gui.wikizoomer.close"), (button) -> {
            Minecraft.getInstance().setScreen(null);
        }).size(buttonWidth, buttonHeight).pos(col3X, row3Y).build();
        this.addRenderableWidget(clearButton);
        this.addRenderableWidget(startButton);
        this.addRenderableWidget(closeButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.wikizoomer.batch_title"), this.width / 2, 12, 0xFFFFFF);
    }

    private void startExport() {
        if (!exportItems && !exportEntities) {
            sendChat(Component.translatable("gui.wikizoomer.batch_no_types"));
            return;
        }
        Set<String> modIds = getSelectedModIds();
        if (modIds.isEmpty()) {
            sendChat(Component.translatable("gui.wikizoomer.batch_no_mods"));
            return;
        }
        List<ExportTask> tasks = new ArrayList<>();
        int exportSize = getExportSize();
        if (exportItems) {
            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                if (item == Items.AIR) {
                    continue;
                }
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id == null || !modIds.contains(id.getNamespace())) {
                    continue;
                }
                ExportTask task = ExportManager.createItemTask(new ItemStack(item), zoomPercent, background, exportSize, true);
                if (task != null) {
                    tasks.add(task);
                }
            }
        }
        if (exportEntities) {
            for (EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES.getValues()) {
                ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
                if (id == null || !modIds.contains(id.getNamespace())) {
                    continue;
                }
                ExportTask task = ExportManager.createEntityIdTask(id, zoomPercent, background, exportSize, true);
                if (task != null) {
                    tasks.add(task);
                }
            }
        }
        if (tasks.isEmpty()) {
            sendChat(Component.translatable("gui.wikizoomer.batch_no_tasks"));
            return;
        }
        ExportManager.enqueueBatch(tasks);
        Minecraft.getInstance().setScreen(null);
    }

    private Set<String> getSelectedModIds() {
        Set<String> modIds = new HashSet<>();
        for (ModEntry entry : entries) {
            if (entry.selected) {
                modIds.add(entry.modId);
            }
        }
        return modIds;
    }

    private void setAllSelected(boolean selected) {
        for (ModEntry entry : entries) {
            entry.selected = selected;
        }
    }

    private List<ModEntry> buildEntries() {
        List<ModContainer> mods = new ArrayList<>(ModList.get().getMods());
        mods.sort(Comparator.comparing(ModContainer::getModId));
        List<ModEntry> list = new ArrayList<>();
        for (ModContainer mod : mods) {
            list.add(new ModEntry(mod.getModId(), mod.getModInfo().getDisplayName()));
        }
        return list;
    }

    private void sendChat(Component message) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.sendSystemMessage(message);
        }
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

    @OnlyIn(Dist.CLIENT)
    private static class ModListWidget extends ObjectSelectionList<ModEntry> {
        public ModListWidget(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight, List<ModEntry> entries) {
            super(minecraft, width, height, top, bottom, itemHeight);
            for (ModEntry entry : entries) {
                this.addEntry(entry);
            }
        }

        @Override
        public int getRowWidth() {
            return Math.min(260, this.width - 20);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowLeft() + this.getRowWidth();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static class ModEntry extends ObjectSelectionList.Entry<ModEntry> {
        private final String modId;
        private final String modName;
        private boolean selected = false;

        public ModEntry(String modId, String modName) {
            this.modId = modId;
            this.modName = modName;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTicks) {
            int color = selected ? 0x00FF00 : 0xFFFFFF;
            String label = (selected ? "[x] " : "[ ] ") + modName + " (" + modId + ")";
            guiGraphics.drawString(Minecraft.getInstance().font, label, x + 2, y + 2, color);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            selected = !selected;
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.literal(modName);
        }
    }
}
