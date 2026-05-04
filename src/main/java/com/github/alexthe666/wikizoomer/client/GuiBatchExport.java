package com.github.alexthe666.wikizoomer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class GuiBatchExport extends GuiScreen {
    private ModListWidget modList;
    private List<ModEntry> entries = new ArrayList<>();
    private boolean exportItems = true;
    private boolean exportEntities = true;
    private boolean greenscreen = true;
    private GuiButton itemsButton;
    private GuiButton entitiesButton;
    private GuiButton greenscreenButton;
    private GuiButton selectAllButton;
    private GuiButton clearButton;
    private GuiButton startButton;
    private GuiButton closeButton;

    @Override
    public void initGui() {
        super.initGui();
        this.entries = buildEntries();
        int listTop = 32;
        int listBottom = this.height - 90;
        this.modList = new ModListWidget(this.mc, this.width, this.height, listTop, listBottom, 20, this.entries);
        this.buttonList.clear();
        int buttonWidth = 100;
        int buttonHeight = 20;
        int spacing = 10;
        int rowWidth = buttonWidth * 3 + spacing * 2;
        int startX = this.width / 2 - rowWidth / 2;
        int row1Y = this.height - 80;
        int row2Y = this.height - 55;
        int row3Y = this.height - 30;
        this.itemsButton = new GuiButton(0, startX, row1Y, buttonWidth, buttonHeight, "");
        this.entitiesButton = new GuiButton(1, startX + buttonWidth + spacing, row1Y, buttonWidth, buttonHeight, "");
        this.greenscreenButton = new GuiButton(2, startX + (buttonWidth + spacing) * 2, row1Y, buttonWidth, buttonHeight, "");
        this.selectAllButton = new GuiButton(3, startX, row2Y, buttonWidth, buttonHeight, I18n.format("gui.wikizoomer.batch_select_all"));
        this.clearButton = new GuiButton(4, startX + buttonWidth + spacing, row2Y, buttonWidth, buttonHeight, I18n.format("gui.wikizoomer.batch_clear"));
        this.startButton = new GuiButton(5, startX + (buttonWidth + spacing) * 2, row2Y, buttonWidth, buttonHeight, I18n.format("gui.wikizoomer.batch_start"));
        this.closeButton = new GuiButton(6, this.width / 2 - 60, row3Y, 120, buttonHeight, I18n.format("gui.wikizoomer.close"));
        this.buttonList.add(this.itemsButton);
        this.buttonList.add(this.entitiesButton);
        this.buttonList.add(this.greenscreenButton);
        this.buttonList.add(this.selectAllButton);
        this.buttonList.add(this.clearButton);
        this.buttonList.add(this.startButton);
        this.buttonList.add(this.closeButton);
        updateButtonLabels();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            exportItems = !exportItems;
            updateButtonLabels();
        } else if (button.id == 1) {
            exportEntities = !exportEntities;
            updateButtonLabels();
        } else if (button.id == 2) {
            greenscreen = !greenscreen;
            updateButtonLabels();
        } else if (button.id == 3) {
            setAllSelected(true);
        } else if (button.id == 4) {
            setAllSelected(false);
        } else if (button.id == 5) {
            startExport();
        } else if (button.id == 6) {
            Minecraft.getMinecraft().displayGuiScreen(null);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.modList.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(this.fontRenderer, I18n.format("gui.wikizoomer.batch_title"), this.width / 2, 12, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.modList.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        this.modList.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.modList.handleMouseInput();
    }

    private void updateButtonLabels() {
        this.itemsButton.displayString = I18n.format("gui.wikizoomer.batch_items", exportItems ? "ON" : "OFF");
        this.entitiesButton.displayString = I18n.format("gui.wikizoomer.batch_entities", exportEntities ? "ON" : "OFF");
        this.greenscreenButton.displayString = I18n.format("gui.wikizoomer.batch_greenscreen", greenscreen ? "ON" : "OFF");
    }

    private void startExport() {
        if (!exportItems && !exportEntities) {
            sendChat(I18n.format("gui.wikizoomer.batch_no_types"));
            return;
        }
        Set<String> modIds = getSelectedModIds();
        if (modIds.isEmpty()) {
            sendChat(I18n.format("gui.wikizoomer.batch_no_mods"));
            return;
        }
        List<ExportTask> tasks = new ArrayList<>();
        float zoom = ExportManager.getDefaultZoom();
        if (exportItems) {
            for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
                if (item == Items.AIR) {
                    continue;
                }
                ResourceLocation id = item.getRegistryName();
                if (id == null || !modIds.contains(id.getNamespace())) {
                    continue;
                }
                ExportTask task = ExportManager.createItemTask(new ItemStack(item), zoom, greenscreen, true);
                if (task != null) {
                    tasks.add(task);
                }
            }
        }
        if (exportEntities) {
            for (EntityEntry entry : ForgeRegistries.ENTITIES.getValuesCollection()) {
                ResourceLocation id = entry.getRegistryName();
                if (id == null || !modIds.contains(id.getNamespace())) {
                    continue;
                }
                if (entry.getEntityClass() == null || Modifier.isAbstract(entry.getEntityClass().getModifiers())) {
                    continue;
                }
                ExportTask task = ExportManager.createEntityIdTask(id, zoom, greenscreen, true);
                if (task != null) {
                    tasks.add(task);
                }
            }
        }
        if (tasks.isEmpty()) {
            sendChat(I18n.format("gui.wikizoomer.batch_no_tasks"));
            return;
        }
        ExportManager.enqueueBatch(tasks);
        Minecraft.getMinecraft().displayGuiScreen(null);
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
        List<ModContainer> mods = new ArrayList<>(Loader.instance().getModList());
        mods.sort(Comparator.comparing(ModContainer::getModId));
        List<ModEntry> list = new ArrayList<>();
        for (ModContainer mod : mods) {
            list.add(new ModEntry(mod.getModId(), mod.getName()));
        }
        return list;
    }

    private void sendChat(String message) {
        if (this.mc.player != null) {
            this.mc.player.sendMessage(new TextComponentString(message));
        }
    }

    @SideOnly(Side.CLIENT)
    private static class ModListWidget extends GuiListExtended {
        private final List<ModEntry> entries;

        public ModListWidget(Minecraft mcIn, int widthIn, int heightIn, int topIn, int bottomIn, int slotHeightIn, List<ModEntry> entries) {
            super(mcIn, widthIn, heightIn, topIn, bottomIn, slotHeightIn);
            this.entries = entries;
        }

        @Override
        public GuiListExtended.IGuiListEntry getListEntry(int index) {
            return entries.get(index);
        }

        @Override
        protected int getSize() {
            return entries.size();
        }
    }

    @SideOnly(Side.CLIENT)
    private static class ModEntry implements GuiListExtended.IGuiListEntry {
        private final String modId;
        private final String modName;
        private boolean selected = false;

        public ModEntry(String modId, String modName) {
            this.modId = modId;
            this.modName = modName;
        }

        @Override
        public void updatePosition(int slotIndex, int x, int y, float partialTicks) {
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
            Minecraft mc = Minecraft.getMinecraft();
            String label = (selected ? "[x] " : "[ ] ") + modName + " (" + modId + ")";
            mc.fontRenderer.drawString(label, x + 2, y + 2, selected ? 0x00FF00 : 0xFFFFFF);
        }

        @Override
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
            selected = !selected;
            return true;
        }

        @Override
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
        }
    }
}
