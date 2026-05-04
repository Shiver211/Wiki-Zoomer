package com.github.alexthe666.wikizoomer.client;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.io.File;

public class ExportTask {
    public enum Type {
        ITEM,
        ENTITY
    }

    public enum Background {
        GREENSCREEN,
        TRANSPARENT
    }

    public final Type type;
    public final ItemStack itemStack;
    public final Entity entity;
    public final ResourceLocation entityId;
    public final File outputFile;
    public final Background background;
    public final boolean isBatch;
    public final float zoomPercent;
    public final int exportSize;

    private ExportTask(Type type, ItemStack itemStack, Entity entity, ResourceLocation entityId, File outputFile, Background background, boolean isBatch, float zoomPercent, int exportSize) {
        this.type = type;
        this.itemStack = itemStack;
        this.entity = entity;
        this.entityId = entityId;
        this.outputFile = outputFile;
        this.background = background;
        this.isBatch = isBatch;
        this.zoomPercent = zoomPercent;
        this.exportSize = exportSize;
    }

    public static ExportTask forItem(ItemStack stack, File outputFile, Background background, boolean isBatch, float zoomPercent, int exportSize) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return new ExportTask(Type.ITEM, copy, null, null, outputFile, background, isBatch, zoomPercent, exportSize);
    }

    public static ExportTask forEntity(Entity entity, File outputFile, Background background, boolean isBatch, float zoomPercent, int exportSize) {
        return new ExportTask(Type.ENTITY, ItemStack.EMPTY, entity, null, outputFile, background, isBatch, zoomPercent, exportSize);
    }

    public static ExportTask forEntityId(ResourceLocation entityId, File outputFile, Background background, boolean isBatch, float zoomPercent, int exportSize) {
        return new ExportTask(Type.ENTITY, ItemStack.EMPTY, null, entityId, outputFile, background, isBatch, zoomPercent, exportSize);
    }
}
