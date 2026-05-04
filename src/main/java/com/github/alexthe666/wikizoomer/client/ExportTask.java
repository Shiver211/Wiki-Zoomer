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

    public final Type type;
    public final ItemStack itemStack;
    public final Entity entity;
    public final ResourceLocation entityId;
    public final File outputFile;
    public final boolean greenscreen;
    public final boolean isBatch;
    public final float zoomPercent;

    private ExportTask(Type type, ItemStack itemStack, Entity entity, ResourceLocation entityId, File outputFile, boolean greenscreen, boolean isBatch, float zoomPercent) {
        this.type = type;
        this.itemStack = itemStack;
        this.entity = entity;
        this.entityId = entityId;
        this.outputFile = outputFile;
        this.greenscreen = greenscreen;
        this.isBatch = isBatch;
        this.zoomPercent = zoomPercent;
    }

    public static ExportTask forItem(ItemStack stack, File outputFile, boolean greenscreen, boolean isBatch, float zoomPercent) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return new ExportTask(Type.ITEM, copy, null, null, outputFile, greenscreen, isBatch, zoomPercent);
    }

    public static ExportTask forEntity(Entity entity, File outputFile, boolean greenscreen, boolean isBatch, float zoomPercent) {
        return new ExportTask(Type.ENTITY, ItemStack.EMPTY, entity, null, outputFile, greenscreen, isBatch, zoomPercent);
    }

    public static ExportTask forEntityId(ResourceLocation entityId, File outputFile, boolean greenscreen, boolean isBatch, float zoomPercent) {
        return new ExportTask(Type.ENTITY, ItemStack.EMPTY, null, entityId, outputFile, greenscreen, isBatch, zoomPercent);
    }
}
