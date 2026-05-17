package com.github.alexthe666.wikizoomer.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ZoomerConfigCache {
    public float zoomPercent = 100F;
    public ExportTask.Background background = ExportTask.Background.GREENSCREEN;
    public int exportSize = ExportManager.getDefaultExportSize();
    public float rotX = 0F;
    public float rotY = 0F;
    public int offsetX = 0;
    public int offsetY = 0;

    public static ZoomerConfigCache getItemDefaults() {
        ZoomerConfigCache cache = new ZoomerConfigCache();
        cache.zoomPercent = 100F;
        cache.background = ExportTask.Background.GREENSCREEN;
        cache.exportSize = ExportManager.getDefaultExportSize();
        cache.rotX = 0F;
        cache.rotY = 0F;
        return cache;
    }

    public static ZoomerConfigCache getEntityDefaults() {
        ZoomerConfigCache cache = new ZoomerConfigCache();
        cache.zoomPercent = 100F;
        cache.background = ExportTask.Background.GREENSCREEN;
        cache.exportSize = ExportManager.getDefaultExportSize();
        cache.rotX = -30F;
        cache.rotY = 45F;
        cache.offsetX = 0;
        cache.offsetY = 0;
        return cache;
    }

    public static ZoomerConfigCache lastItemConfig = getItemDefaults();
    public static ZoomerConfigCache lastEntityConfig = getEntityDefaults();
}
