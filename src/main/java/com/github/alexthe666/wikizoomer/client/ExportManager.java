package com.github.alexthe666.wikizoomer.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

@OnlyIn(Dist.CLIENT)
public class ExportManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int[] EXPORT_SIZES = new int[]{64, 128, 256, 512};
    private static final int DEFAULT_EXPORT_SIZE = 512;
    private static final float DEFAULT_ZOOM = 100F;
    private static final Queue<ExportTask> QUEUE = new ArrayDeque<>();
    private static RenderTarget renderTarget;
    private static int renderTargetSize = -1;
    private static boolean renderQueued = false;
    private static int batchRemaining = 0;
    private static IntBuffer pixelBuffer;
    private static int[] pixelValues;

    public static int[] getExportSizes() {
        return EXPORT_SIZES.clone();
    }

    public static int getDefaultExportSize() {
        return DEFAULT_EXPORT_SIZE;
    }

    public static float getDefaultZoom() {
        return DEFAULT_ZOOM;
    }

    public static void enqueue(ExportTask task) {
        if (task != null) {
            QUEUE.add(task);
        }
    }

    public static void enqueueBatch(List<ExportTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        QUEUE.addAll(tasks);
        batchRemaining += tasks.size();
        sendChat(Component.translatable("gui.wikizoomer.batch_started", tasks.size()));
    }

    public static ExportTask createItemTask(ItemStack stack, float zoomPercent, ExportTask.Background background, int exportSize, boolean isBatch) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return null;
        }
        File output = getOutputFile(id);
        return ExportTask.forItem(stack, output, background, isBatch, zoomPercent, exportSize);
    }

    public static ExportTask createEntityTask(Entity entity, float zoomPercent, ExportTask.Background background, int exportSize, boolean isBatch) {
        if (entity == null) {
            return null;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) {
            return null;
        }
        File output = getOutputFile(id);
        return ExportTask.forEntity(entity, output, background, isBatch, zoomPercent, exportSize);
    }

    public static ExportTask createEntityIdTask(ResourceLocation entityId, float zoomPercent, ExportTask.Background background, int exportSize, boolean isBatch) {
        if (entityId == null) {
            return null;
        }
        File output = getOutputFile(entityId);
        return ExportTask.forEntityId(entityId, output, background, isBatch, zoomPercent, exportSize);
    }

    public static void tick() {
        if (renderQueued || QUEUE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        ExportTask task = QUEUE.poll();
        if (task == null) {
            return;
        }
        renderQueued = true;
        Runnable render = () -> {
            boolean success = renderTask(mc, task);
            Minecraft.getInstance().execute(() -> handleResult(task, success));
            renderQueued = false;
        };
        if (RenderSystem.isOnRenderThread()) {
            render.run();
        } else {
            RenderSystem.recordRenderCall(render);
        }
    }

    private static void handleResult(ExportTask task, boolean success) {
        if (task.isBatch) {
            batchRemaining--;
            if (batchRemaining <= 0) {
                batchRemaining = 0;
                sendChat(Component.translatable("gui.wikizoomer.batch_done"));
            }
        } else if (success) {
            sendChat(Component.translatable("gui.wikizoomer.export_done", task.outputFile.getName()));
        }
    }

    private static File getOutputFile(ResourceLocation id) {
        Minecraft mc = Minecraft.getInstance();
        File baseDir = new File(mc.gameDirectory, "wiki zoomer");
        File modDir = new File(baseDir, id.getNamespace());
        return new File(modDir, id.getPath() + ".png");
    }

    private static boolean renderTask(Minecraft mc, ExportTask task) {
        if (task.type == ExportTask.Type.ITEM && (task.itemStack == null || task.itemStack.isEmpty())) {
            return false;
        }
        Entity entity = null;
        if (task.type == ExportTask.Type.ENTITY) {
            entity = resolveEntity(mc, task);
            if (entity == null) {
                return false;
            }
        }
        ensureRenderTarget(task.exportSize);
        File output = task.outputFile;
        File parent = output.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            LOGGER.warn("Could not create export directory: {}", parent.getAbsolutePath());
        }
        RenderTarget previous = mc.getMainRenderTarget();
        boolean transparent = task.background == ExportTask.Background.TRANSPARENT;
        renderTarget.setClearColor(transparent ? 0.0F : 0.298F, transparent ? 0.0F : 1.0F, 0.0F, transparent ? 0.0F : 1.0F);
        renderTarget.clear(Minecraft.ON_OSX);
        renderTarget.bindWrite(true);
        RenderSystem.viewport(0, 0, task.exportSize, task.exportSize);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        Matrix4f previousProjection = RenderSystem.getProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, task.exportSize, task.exportSize, 0.0F, 1000.0F, 3000.0F), VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack poseStack = new PoseStack();
        GuiGraphics guiGraphics = new GuiGraphics(mc, poseStack, mc.renderBuffers().bufferSource());
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, -2000.0F);
        if (task.type == ExportTask.Type.ITEM) {
            renderItemCentered(guiGraphics, task.itemStack, task.exportSize, task.zoomPercent);
        } else {
            renderEntityCentered(guiGraphics, entity, task.exportSize, task.zoomPercent);
        }
        guiGraphics.flush();
        poseStack.popPose();
        RenderSystem.setProjectionMatrix(previousProjection, VertexSorting.ORTHOGRAPHIC_Z);
        BufferedImage image = readPixels(task.exportSize, task.exportSize, renderTarget, transparent);
        try {
            ImageIO.write(image, "png", output);
        } catch (Exception e) {
            LOGGER.warn("Failed to export image", e);
            return false;
        } finally {
            previous.bindWrite(true);
            RenderSystem.viewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());
        }
        return true;
    }

    private static void ensureRenderTarget(int exportSize) {
        if (renderTarget == null || renderTargetSize != exportSize) {
            if (renderTarget != null) {
                renderTarget.destroyBuffers();
            }
            renderTarget = new TextureTarget(exportSize, exportSize, true, Minecraft.ON_OSX);
            renderTargetSize = exportSize;
        }
    }

    private static Entity resolveEntity(Minecraft mc, ExportTask task) {
        if (task.entity != null) {
            return task.entity;
        }
        if (task.entityId == null) {
            return null;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(task.entityId);
        if (type == null) {
            return null;
        }
        Entity created = type.create(mc.level);
        if (created == null) {
            return null;
        }
        return created;
    }

    private static void renderItemCentered(GuiGraphics guiGraphics, ItemStack stack, int exportSize, float zoomPercent) {
        float baseScale = (exportSize / 512.0F) * 12.0F;
        float scale = baseScale * (zoomPercent / 100.0F);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(exportSize / 2.0F, exportSize / 2.0F, 100.0F);
        guiGraphics.pose().scale(scale, scale, scale);
        guiGraphics.renderItem(Minecraft.getInstance().player, stack, -8, -8, 0);
        guiGraphics.pose().popPose();
    }

    private static void renderEntityCentered(GuiGraphics guiGraphics, Entity entity, int exportSize, float zoomPercent) {
        float scale = zoomPercent;
        int centerX = exportSize / 2;
        int centerY = (exportSize + (int)((zoomPercent / 100.0F) * (entity.getBbHeight() * 100.0F))) / 2;
        Entity renderEntity = entity;
        if (ClientProxy.dataMimic != null && renderEntity.getType() == ClientProxy.dataMimic.getType()) {
            renderEntity = ClientProxy.dataMimic;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, centerY, 10.0F);
        GuiEntityZoomer.drawEntityOnScreen(guiGraphics, 0, 0, scale, false, -30.0D, 135.0D, 180.0D, 0.0F, 0.0F, renderEntity);
        guiGraphics.pose().popPose();
    }

    private static BufferedImage readPixels(int width, int height, RenderTarget target, boolean transparent) {
        int size = width * height;
        if (pixelBuffer == null || pixelBuffer.capacity() < size) {
            pixelBuffer = BufferUtils.createIntBuffer(size);
            pixelValues = new int[size];
        }
        target.bindRead();
        RenderSystem.bindTexture(target.getColorTextureId());
        pixelBuffer.clear();
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
        pixelBuffer.get(pixelValues);
        processPixelValues(pixelValues, width, height);
        BufferedImage image = new BufferedImage(width, height, transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, width, height, pixelValues, 0, width);
        return image;
    }

    private static void processPixelValues(int[] pixels, int width, int height) {
        int[] row = new int[width];
        int half = height / 2;
        for (int y = 0; y < half; ++y) {
            System.arraycopy(pixels, y * width, row, 0, width);
            System.arraycopy(pixels, (height - 1 - y) * width, pixels, y * width, width);
            System.arraycopy(row, 0, pixels, (height - 1 - y) * width, width);
        }
    }

    private static void sendChat(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(message);
        }
    }
}
