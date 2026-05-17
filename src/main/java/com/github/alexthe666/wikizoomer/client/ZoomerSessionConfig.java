package com.github.alexthe666.wikizoomer.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZoomerSessionConfig {
    private static final ItemConfig ITEM_CONFIG = ItemConfig.defaults();
    private static final EntityConfig ENTITY_CONFIG = EntityConfig.defaults();

    public static ItemConfig getItemConfig() {
        return ITEM_CONFIG.copy();
    }

    public static EntityConfig getEntityConfig() {
        return ENTITY_CONFIG.copy();
    }

    public static void saveItem(float zoomPercent, ExportTask.Background background, int exportSize, float rotX, float rotY) {
        ITEM_CONFIG.zoomPercent = zoomPercent;
        ITEM_CONFIG.background = background;
        ITEM_CONFIG.exportSize = exportSize;
        ITEM_CONFIG.rotX = rotX;
        ITEM_CONFIG.rotY = rotY;
    }

    public static void saveEntity(float zoomPercent, ExportTask.Background background, int exportSize, float rotX, float rotY, float offsetX, float offsetY) {
        ENTITY_CONFIG.zoomPercent = zoomPercent;
        ENTITY_CONFIG.background = background;
        ENTITY_CONFIG.exportSize = exportSize;
        ENTITY_CONFIG.rotX = rotX;
        ENTITY_CONFIG.rotY = rotY;
        ENTITY_CONFIG.offsetX = offsetX;
        ENTITY_CONFIG.offsetY = offsetY;
    }

    public static ItemConfig resetItem() {
        ItemConfig defaults = ItemConfig.defaults();
        saveItem(defaults.zoomPercent, defaults.background, defaults.exportSize, defaults.rotX, defaults.rotY);
        return getItemConfig();
    }

    public static EntityConfig resetEntity() {
        EntityConfig defaults = EntityConfig.defaults();
        saveEntity(defaults.zoomPercent, defaults.background, defaults.exportSize, defaults.rotX, defaults.rotY, defaults.offsetX, defaults.offsetY);
        return getEntityConfig();
    }

    public static class ItemConfig {
        public float zoomPercent;
        public ExportTask.Background background;
        public int exportSize;
        public float rotX;
        public float rotY;

        private static ItemConfig defaults() {
            ItemConfig config = new ItemConfig();
            config.zoomPercent = ExportManager.getDefaultZoom();
            config.background = ExportTask.Background.GREENSCREEN;
            config.exportSize = ExportManager.getDefaultExportSize();
            config.rotX = 0.0F;
            config.rotY = 0.0F;
            return config;
        }

        private ItemConfig copy() {
            ItemConfig config = new ItemConfig();
            config.zoomPercent = this.zoomPercent;
            config.background = this.background;
            config.exportSize = this.exportSize;
            config.rotX = this.rotX;
            config.rotY = this.rotY;
            return config;
        }
    }

    public static class EntityConfig extends ItemConfig {
        public float offsetX;
        public float offsetY;

        private static EntityConfig defaults() {
            EntityConfig config = new EntityConfig();
            config.zoomPercent = ExportManager.getDefaultZoom();
            config.background = ExportTask.Background.GREENSCREEN;
            config.exportSize = ExportManager.getDefaultExportSize();
            config.rotX = 30.0F;
            config.rotY = 45.0F;
            config.offsetX = 0.0F;
            config.offsetY = 0.0F;
            return config;
        }

        private EntityConfig copy() {
            EntityConfig config = new EntityConfig();
            config.zoomPercent = this.zoomPercent;
            config.background = this.background;
            config.exportSize = this.exportSize;
            config.rotX = this.rotX;
            config.rotY = this.rotY;
            config.offsetX = this.offsetX;
            config.offsetY = this.offsetY;
            return config;
        }
    }
}
