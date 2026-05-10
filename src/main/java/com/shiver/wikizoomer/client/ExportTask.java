package com.shiver.wikizoomer.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
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
    @Nullable
    public final Entity entity;
    @Nullable
    public final ResourceLocation entityId;
    public final File outputFile;
    public final Background background;
    public final boolean isBatch;
    public final float zoomPercent;
    public final int exportSize;
    public final float rotX;
    public final float rotY;

    private ExportTask(Type type, ItemStack itemStack, @Nullable Entity entity, @Nullable ResourceLocation entityId,
                       File outputFile, Background background, boolean isBatch, float zoomPercent, int exportSize,
                       float rotX, float rotY) {
        this.type = type;
        this.itemStack = itemStack;
        this.entity = entity;
        this.entityId = entityId;
        this.outputFile = outputFile;
        this.background = background;
        this.isBatch = isBatch;
        this.zoomPercent = zoomPercent;
        this.exportSize = exportSize;
        this.rotX = rotX;
        this.rotY = rotY;
    }

    public static ExportTask forItem(ItemStack stack, File outputFile, Background background, boolean isBatch,
                                     float zoomPercent, int exportSize, float rotX, float rotY) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return new ExportTask(Type.ITEM, copy, null, null, outputFile, background, isBatch, zoomPercent, exportSize, rotX, rotY);
    }

    public static ExportTask forEntity(Entity entity, File outputFile, Background background, boolean isBatch,
                                       float zoomPercent, int exportSize, float rotX, float rotY) {
        return new ExportTask(Type.ENTITY, ItemStack.EMPTY, entity, null, outputFile, background, isBatch, zoomPercent, exportSize, rotX, rotY);
    }

    public static ExportTask forEntityId(ResourceLocation entityId, File outputFile, Background background, boolean isBatch,
                                         float zoomPercent, int exportSize, float rotX, float rotY) {
        return new ExportTask(Type.ENTITY, ItemStack.EMPTY, null, entityId, outputFile, background, isBatch, zoomPercent, exportSize, rotX, rotY);
    }
}
