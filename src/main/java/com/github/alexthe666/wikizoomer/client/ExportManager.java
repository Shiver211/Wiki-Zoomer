package com.github.alexthe666.wikizoomer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

@SideOnly(Side.CLIENT)
public class ExportManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int EXPORT_SIZE = 512;
    private static final float DEFAULT_ZOOM = 100F;
    private static final float ITEM_BASE_SCALE = 16F;
    private static final Queue<ExportTask> QUEUE = new ArrayDeque<>();
    private static Framebuffer framebuffer;
    private static int batchRemaining = 0;

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
        sendChat(I18n.format("gui.wikizoomer.batch_started", tasks.size()));
    }

    public static ExportTask createItemTask(ItemStack stack, float zoomPercent, boolean greenscreen, boolean isBatch) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ResourceLocation id = stack.getItem().getRegistryName();
        if (id == null) {
            return null;
        }
        File output = getOutputFile(id);
        return ExportTask.forItem(stack, output, greenscreen, isBatch, zoomPercent);
    }

    public static ExportTask createEntityTask(Entity entity, float zoomPercent, boolean greenscreen, boolean isBatch) {
        if (entity == null) {
            return null;
        }
        ResourceLocation id = EntityList.getKey(entity);
        if (id == null) {
            return null;
        }
        File output = getOutputFile(id);
        return ExportTask.forEntity(entity, output, greenscreen, isBatch, zoomPercent);
    }

    public static ExportTask createEntityIdTask(ResourceLocation entityId, float zoomPercent, boolean greenscreen, boolean isBatch) {
        if (entityId == null) {
            return null;
        }
        File output = getOutputFile(entityId);
        return ExportTask.forEntityId(entityId, output, greenscreen, isBatch, zoomPercent);
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
        ensureFramebuffer();
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
            GL11.glViewport(0, 0, EXPORT_SIZE, EXPORT_SIZE);
            if (task.greenscreen) {
                GlStateManager.clearColor(0.298F, 1.0F, 0.0F, 1.0F);
            } else {
                GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
            }
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GlStateManager.disableCull();
            GlStateManager.enableDepth();
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0D, EXPORT_SIZE, EXPORT_SIZE, 0.0D, 1000.0D, 3000.0D);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            GlStateManager.translate(0.0F, 0.0F, -2000.0F);
            pushedMatrices = true;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (task.type == ExportTask.Type.ITEM) {
                renderItem(task.itemStack, task.zoomPercent);
            } else {
                renderEntity(mc, entity, task.zoomPercent);
            }
            BufferedImage image = ScreenShotHelper.createScreenshot(EXPORT_SIZE, EXPORT_SIZE, framebuffer);
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

    private static void ensureFramebuffer() {
        if (framebuffer == null || framebuffer.framebufferTextureWidth != EXPORT_SIZE || framebuffer.framebufferTextureHeight != EXPORT_SIZE) {
            framebuffer = new Framebuffer(EXPORT_SIZE, EXPORT_SIZE, true);
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

    private static void renderItem(ItemStack stack, float zoomPercent) {
        float scale = ITEM_BASE_SCALE * (zoomPercent / 100F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(EXPORT_SIZE / 2F, EXPORT_SIZE / 2F, 500.0F);
        GlStateManager.scale(scale, scale, scale);
        RenderHelper.enableGUIStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(stack, -8, -8);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    private static void renderEntity(Minecraft mc, Entity entity, float zoomPercent) {
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
        GlStateManager.translate(EXPORT_SIZE / 2F, EXPORT_SIZE * 0.75F, 150.0F + scale);
        GlStateManager.scale(-scale, scale, scale);
        GlStateManager.rotate(-30.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(135.0F, 0.0F, -1.0F, 0.0F);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        RenderManager renderManager = mc.getRenderManager();
        renderManager.setPlayerViewY(180.0F);
        renderManager.setRenderShadow(false);
        renderManager.renderEntity(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, false);
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
}
