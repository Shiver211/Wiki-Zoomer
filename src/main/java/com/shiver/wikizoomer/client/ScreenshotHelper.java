package com.shiver.wikizoomer.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.nio.file.Files;

public class ScreenshotHelper {

    public static void exportScreenshot(String imageName, RenderInScreenshotFunction inScreenshot) {
        RenderTarget prev = Minecraft.getInstance().getMainRenderTarget();
        int w = Minecraft.getInstance().getWindow().getWidth();
        int h = Minecraft.getInstance().getWindow().getHeight();
        RenderTarget target = new TextureTarget(w, h, true, Minecraft.ON_OSX);
        target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);

        target.clear(true);
        target.bindWrite(true);

        inScreenshot.call();

        IntBuffer pixels = BufferUtils.createIntBuffer(w * h);
        target.bindRead();
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixels);
        int[] vals = new int[w * h];
        pixels.get(vals);
        processPixelValues(vals, w, h);
        BufferedImage bufferedimage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        bufferedimage.setRGB(0, 0, w, h, vals, 0, w);
        File file1 = new File(Minecraft.getInstance().gameDirectory, "screenshots/wikizoomer");
        if (!file1.exists()) {
            try {
                Files.createDirectories(file1.toPath());
            } catch (Exception e) {
                return;
            }
        }
        int sameCount = 0;
        for (File file : file1.listFiles()) {
            String name = file.getName();
            if (name.contains(imageName)) {
                sameCount++;
            }
        }
        File f = new File(file1, imageName + (sameCount == 0 ? ".png" : "(" + sameCount + ").png"));
        try {
            f.createNewFile();
            ImageIO.write(bufferedimage, "png", f);
        } catch (Exception e) {
            // ignore
        }
        try {
            Thread.sleep(10L);
        } catch (InterruptedException e) {
            // ignore
        }
        target.setClearColor(1.0F, 1.0F, 1.0F, 1.0F);
        target.destroyBuffers();
        RenderSystem.bindTexture(Minecraft.getInstance().getMainRenderTarget().getColorTextureId());
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        Minecraft.getInstance().gameRenderer.renderLevel(DeltaTracker.ONE);
        Minecraft.getInstance().levelRenderer.graphicsChanged();
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

    @FunctionalInterface
    public interface RenderInScreenshotFunction {
        void call();
    }
}
