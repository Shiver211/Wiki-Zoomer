package com.github.alexthe666.wikizoomer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

@SideOnly(Side.CLIENT)
public class ExportManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int DEFAULT_EXPORT_SIZE = 512;
    private static final int[] EXPORT_SIZES = new int[]{64, 128, 256, 512, 1024};
    private static final float DEFAULT_ZOOM = 100F;
    private static final float ITEM_BASE_SCALE = 12F;
    private static final Queue<ExportTask> QUEUE = new ArrayDeque<>();
    private static Framebuffer framebuffer;
    private static int batchRemaining = 0;
    private static IntBuffer pixelBuffer;
    private static int[] pixelValues;

    public static float getDefaultZoom() {
        return DEFAULT_ZOOM;
    }

    public static int getDefaultExportSize() {
        return DEFAULT_EXPORT_SIZE;
    }

    public static int[] getExportSizes() {
        return EXPORT_SIZES.clone();
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
        sendChat(I18n.format("gui.wikizoomer.batch_started", tasks.size()));
    }

    public static ExportTask createItemTask(ItemStack stack, float zoomPercent, ExportTask.Background background, int exportSize, boolean isBatch) {
        return createItemTask(stack, zoomPercent, background, exportSize, isBatch, 0F, 0F);
    }

    public static ExportTask createItemTask(ItemStack stack, float zoomPercent, ExportTask.Background background, int exportSize, boolean isBatch, float rotX, float rotY) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ResourceLocation id = stack.getItem().getRegistryName();
        if (id == null) {
            return null;
        }
        File output = getOutputFile(id);
        return ExportTask.forItem(stack, output, background, isBatch, zoomPercent, exportSize, rotX, rotY);
    }

    public static ExportTask createEntityTask(Entity entity, float zoomPercent, ExportTask.Background background, int exportSize, boolean isBatch, int offsetX, int offsetY) {
        return createEntityTask(entity, zoomPercent, background, exportSize, isBatch, -30F, 45F, offsetX, offsetY);
    }

    public static ExportTask createEntityTask(Entity entity, float zoomPercent, ExportTask.Background background, int exportSize, boolean isBatch, float rotX, float rotY, int offsetX, int offsetY) {
        if (entity == null) {
            return null;
        }
        ResourceLocation id = EntityList.getKey(entity);
        if (id == null) {
            return null;
        }
        File output = getOutputFile(id);
        return ExportTask.forEntity(entity, output, background, isBatch, zoomPercent, exportSize, rotX, rotY, offsetX, offsetY);
    }

    public static ExportTask createEntityIdTask(ResourceLocation entityId, float zoomPercent, ExportTask.Background background, int exportSize, boolean isBatch, int offsetX, int offsetY) {
        return createEntityIdTask(entityId, zoomPercent, background, exportSize, isBatch, -30F, 45F, offsetX, offsetY);
    }

    public static ExportTask createEntityIdTask(ResourceLocation entityId, float zoomPercent, ExportTask.Background background, int exportSize, boolean isBatch, float rotX, float rotY, int offsetX, int offsetY) {
        if (entityId == null) {
            return null;
        }
        File output = getOutputFile(entityId);
        return ExportTask.forEntityId(entityId, output, background, isBatch, zoomPercent, exportSize, rotX, rotY, offsetX, offsetY);
    }

    public static void tick() {
        if (QUEUE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            return;
        }
        ExportTask task = QUEUE.poll();
        if (task == null) {
            return;
        }
        boolean success = renderTask(mc, task);
        if (task.isBatch) {
            batchRemaining--;
            if (batchRemaining <= 0) {
                batchRemaining = 0;
                sendChat(I18n.format("gui.wikizoomer.batch_done"));
            }
        } else if (success) {
            sendChat(I18n.format("gui.wikizoomer.export_done", task.outputFile.getName()));
        }
    }

    private static File getOutputFile(ResourceLocation id) {
        Minecraft mc = Minecraft.getMinecraft();
        File baseDir = new File(mc.gameDir, "wiki zoomer");
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
        int exportSize = task.exportSize;
        ensureFramebuffer(exportSize);
        File output = task.outputFile;
        File parent = output.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            LOGGER.warn("Could not create export directory: {}", parent.getAbsolutePath());
        }
        int prevWidth = mc.displayWidth;
        int prevHeight = mc.displayHeight;
        boolean success = false;
        boolean pushedMatrices = false;
        try {
            framebuffer.bindFramebuffer(true);
            GL11.glViewport(0, 0, exportSize, exportSize);
            if (task.background == ExportTask.Background.TRANSPARENT) {
                GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            } else {
                GlStateManager.clearColor(0.298F, 1.0F, 0.0F, 1.0F);
            }
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GlStateManager.disableCull();
            GlStateManager.enableDepth();
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0D, exportSize, exportSize, 0.0D, -10000.0D, 10000.0D);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            GlStateManager.translate(0.0F, 0.0F, -2000.0F);
            pushedMatrices = true;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (task.type == ExportTask.Type.ITEM) {
                renderItem(task.itemStack, task.zoomPercent, exportSize, task.rotX, task.rotY);
            } else {
                renderEntity(mc, entity, task.zoomPercent, exportSize, task.rotX, task.rotY, task.offsetX, task.offsetY);
            }
            boolean transparent = task.background == ExportTask.Background.TRANSPARENT;
            BufferedImage image = readFramebuffer(exportSize, exportSize, framebuffer, transparent);
            ImageIO.write(image, "png", output);
            success = true;
        } catch (Exception e) {
            LOGGER.warn("Failed to export image", e);
        } finally {
            if (pushedMatrices) {
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            }
            framebuffer.unbindFramebuffer();
            mc.getFramebuffer().bindFramebuffer(true);
            GL11.glViewport(0, 0, prevWidth, prevHeight);
        }
        return success;
    }

    private static void ensureFramebuffer(int exportSize) {
        if (framebuffer == null || framebuffer.framebufferTextureWidth != exportSize || framebuffer.framebufferTextureHeight != exportSize) {
            framebuffer = new Framebuffer(exportSize, exportSize, true);
        }
    }

    private static Entity resolveEntity(Minecraft mc, ExportTask task) {
        if (task.entity != null) {
            return task.entity;
        }
        if (task.entityId == null) {
            return null;
        }
        return EntityList.createEntityByIDFromName(task.entityId, mc.world);
    }

    private static void renderItem(ItemStack stack, float zoomPercent, int exportSize, float rotX, float rotY) {
        float scale = ITEM_BASE_SCALE * (zoomPercent / 100F);
        float zOffset = 200.0F;
        RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(exportSize / 2F, exportSize / 2F, zOffset);
        GlStateManager.scale(scale, scale, scale);
        renderItem.zLevel += 50.0F;
        try {
            IBakedModel model = renderItem.getItemModelWithOverrides(stack, null, Minecraft.getMinecraft().player);
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
            GlStateManager.enableRescaleNormal();
            GlStateManager.translate(0.0F, 0.0F, renderItem.zLevel);
            GlStateManager.scale(1.0F, -1.0F, 1.0F);
            GlStateManager.scale(16.0F, 16.0F, 16.0F);
            if (model.isGui3d()) {
                GlStateManager.enableLighting();
            } else {
                GlStateManager.disableLighting();
            }
            RenderHelper.enableGUIStandardItemLighting();
            model = ForgeHooksClient.handleCameraTransforms(model, ItemCameraTransforms.TransformType.GUI, false);
            if (stack.getItem() instanceof ItemBlock) {
                GlStateManager.rotate(rotX, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(rotY, 0.0F, 1.0F, 0.0F);
            }
            renderItem.renderItem(stack, model);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        } finally {
            renderItem.zLevel -= 50.0F;
            GlStateManager.popMatrix();
        }
    }

    private static void renderEntity(Minecraft mc, Entity entity, float zoomPercent, int exportSize, float rotX, float rotY, int offsetX, int offsetY) {
        entity.ticksExisted = 0;
        if (entity instanceof EntityLivingBase) {
            ((EntityLivingBase) entity).rotationYawHead = 0;
        }
        float baseScale = zoomPercent / 100F * 100F;
        float sizeScale = 0.75F;
        float maxSize = Math.max(entity.width, entity.height);
        if (maxSize > 1.0F) {
            sizeScale /= maxSize;
        }
        float scale = baseScale * sizeScale;
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        float centerX = exportSize / 2F + offsetX;
        float centerY = exportSize / 2F + offsetY;
        GlStateManager.translate(centerX, centerY, 150.0F + scale);
        GlStateManager.scale(-scale, scale, scale);
        GlStateManager.rotate(rotX, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(180.0F - rotY, 0.0F, -1.0F, 0.0F);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        RenderManager renderManager = mc.getRenderManager();
        renderManager.setPlayerViewY(180.0F);
        renderManager.setRenderShadow(false);
        renderManager.renderEntity(entity, 0.0D, -entity.height / 2.0D, 0.0D, 0.0F, 1.0F, false);
        renderManager.setRenderShadow(true);
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private static void sendChat(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null) {
            mc.player.sendMessage(new TextComponentString(message));
        }
    }

    private static BufferedImage readFramebuffer(int width, int height, Framebuffer framebufferIn, boolean transparent) {
        if (OpenGlHelper.isFramebufferEnabled()) {
            width = framebufferIn.framebufferTextureWidth;
            height = framebufferIn.framebufferTextureHeight;
        }
        int size = width * height;
        if (pixelBuffer == null || pixelBuffer.capacity() < size) {
            pixelBuffer = BufferUtils.createIntBuffer(size);
            pixelValues = new int[size];
        }
        GlStateManager.glPixelStorei(3333, 1);
        GlStateManager.glPixelStorei(3317, 1);
        pixelBuffer.clear();
        if (OpenGlHelper.isFramebufferEnabled()) {
            GlStateManager.bindTexture(framebufferIn.framebufferTexture);
            GlStateManager.glGetTexImage(3553, 0, 32993, 33639, pixelBuffer);
        } else {
            GlStateManager.glReadPixels(0, 0, width, height, 32993, 33639, pixelBuffer);
        }
        pixelBuffer.get(pixelValues);
        TextureUtil.processPixelValues(pixelValues, width, height);
        BufferedImage image = new BufferedImage(width, height, transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, width, height, pixelValues, 0, width);
        return image;
    }
}
