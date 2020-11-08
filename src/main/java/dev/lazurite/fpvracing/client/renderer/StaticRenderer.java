package dev.lazurite.fpvracing.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.lazurite.fpvracing.client.ClientInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.GlAllocationUtils;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class StaticRenderer extends Thread {
    private final static MinecraftClient client = ClientInitializer.client;
    private static float prevTickDelta;

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(2);
    private static Future<ByteBuffer> copier;
    private static Future<ByteBuffer> stuffer;

    // volatile disables caching values; forces direct memory referencing
    // because these values are modified on two threads, this is desired
    // as two threads could potentially modify the values concurrently
    // resulting in the two threads caching two different values
    private volatile static ByteBuffer prevByteBuffer;
    private volatile static ByteBuffer nextByteBuffer;
    private volatile static int prevCount;
    private volatile static int nextCount;

    private static class StaticBufferCopier implements Runnable {
        @Override
        public void run() {
            int bufferSize = client.getWindow().getFramebufferHeight() * client.getWindow().getFramebufferWidth() * 16;

            if (prevByteBuffer == null || prevByteBuffer.capacity() != bufferSize) {
                prevByteBuffer = GlAllocationUtils.allocateByteBuffer(bufferSize);
            }

            if (nextByteBuffer == null || nextByteBuffer.capacity() != bufferSize) {
                nextByteBuffer = GlAllocationUtils.allocateByteBuffer(bufferSize);
            } else {
                prevByteBuffer.put(nextByteBuffer);
                prevCount = nextCount;
            }
        }
    }

    private static class StaticBufferStuffer implements Runnable {
        private final ByteBuffer buffer;

        private final int minRowSpacing,
                          maxRowSpacing,
                          minColumnSpacing,
                          maxColumnSpacing;

        StaticBufferStuffer(ByteBuffer buffer, int minRowSpacing, int maxRowSpacing, int minColumnSpacing, int maxColumnSpacing) {
            this.buffer = buffer;

            this.minRowSpacing = minRowSpacing;
            this.maxRowSpacing = maxRowSpacing;
            this.minColumnSpacing = minColumnSpacing;
            this.maxColumnSpacing = maxColumnSpacing;
        }

        @Override
        public void run() {
            Random random = new Random();

            boolean b;

            int color = 0;
            int columnSpacing = 0;
            int currentWidth = 0;
            int elementOffset = 0;
            int rowCounter = 0;
            int rowSpacing = 0;

            int frameBufferHeight = client.getWindow().getFramebufferHeight();
            int frameBufferWidth = client.getWindow().getFramebufferWidth();

            int[] lastRowColors = new int[frameBufferWidth];
            int[] lastRowColumnSpacing = new int[frameBufferWidth];

            nextCount = 0;
            buffer.clear();
            for (int currentHeight = 0; currentHeight < frameBufferHeight; ++currentHeight) {
                if (rowCounter == 0 || rowCounter >= rowSpacing) {
                    rowCounter = 0;
                    rowSpacing = random.nextInt(maxRowSpacing - minRowSpacing + 1) + minRowSpacing;

                    lastRowColors = new int[frameBufferWidth];
                    lastRowColumnSpacing = new int[frameBufferWidth];
                }

                currentWidth = 0;
                while (currentWidth < frameBufferWidth) {
                    if (rowCounter == 0) {
                        b = random.nextBoolean();
                        color = b ? 255 : 0;

                        if (maxColumnSpacing <= frameBufferWidth - currentWidth) {
                            columnSpacing = random.nextInt(maxColumnSpacing - minColumnSpacing + 1) + minColumnSpacing;
                        } else if (maxColumnSpacing >= frameBufferWidth - currentWidth &&
                                   minColumnSpacing <= frameBufferWidth - currentWidth) {
                            columnSpacing = random.nextInt(frameBufferWidth - currentWidth - minColumnSpacing + 1) + minColumnSpacing;
                        } else {
                            columnSpacing = frameBufferWidth - currentWidth;
                        }

                        lastRowColors[currentWidth] = color;
                        lastRowColumnSpacing[currentWidth] = columnSpacing;
                    } else {
                        color = lastRowColors[currentWidth];
                        columnSpacing = lastRowColumnSpacing[currentWidth];
                    }

                    for (int rowSectionWidth = currentWidth; rowSectionWidth < currentWidth + columnSpacing; ++rowSectionWidth) {
                        buffer.putFloat(elementOffset, (float) rowSectionWidth);
                        buffer.putFloat(elementOffset + 4, (float) currentHeight);
                        buffer.putFloat(elementOffset + 8, -90.0f);
                        elementOffset += 12;

                        buffer.put(elementOffset, (byte) color);
                        buffer.put(elementOffset + 1, (byte) color);
                        buffer.put(elementOffset + 2, (byte) color);
                        buffer.put(elementOffset + 3, (byte) 255);
                        elementOffset += 4;
                        ++nextCount;
                    }

                    currentWidth += columnSpacing;
                }

                ++rowCounter;
            }
        }
    }

    public static void render(int minRowSpacing, int maxRowSpacing, int minColumnSpacing, int maxColumnSpacing, float tickDelta) {
        int bufferSize = client.getWindow().getFramebufferHeight() * client.getWindow().getFramebufferWidth() * 16;

        if (stuffer == null) {
            nextByteBuffer = GlAllocationUtils.allocateByteBuffer(bufferSize);
            stuffer = threadPool.submit(new StaticBufferStuffer(nextByteBuffer, minRowSpacing, maxRowSpacing, minColumnSpacing, maxColumnSpacing), nextByteBuffer);
        }

        if (tickDelta < prevTickDelta && stuffer.isDone()) {
            if (copier == null || copier.isDone()) {
                copier = threadPool.submit(new StaticBufferCopier(), prevByteBuffer);
            }
        }

        if (prevByteBuffer != null && prevByteBuffer.capacity() == bufferSize && copier.isDone()) {
            if (stuffer.isDone() && prevByteBuffer.equals(nextByteBuffer)) {
                stuffer = threadPool.submit(new StaticBufferStuffer(nextByteBuffer, minRowSpacing, maxRowSpacing, minColumnSpacing, maxColumnSpacing), nextByteBuffer);
            }

            preRenderingSetup();
            BufferRenderer.draw(prevByteBuffer, 0, VertexFormats.POSITION_COLOR, prevCount);
            postRenderingReset();
        } else if (nextByteBuffer != null && nextByteBuffer.capacity() == bufferSize) {
            preRenderingSetup();
            BufferRenderer.draw(nextByteBuffer, 0, VertexFormats.POSITION_COLOR, nextCount);
            postRenderingReset();
        }

        prevTickDelta = tickDelta;
    }

    private static void preRenderingSetup() {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableAlphaTest();
        RenderSystem.matrixMode(5889);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0D, client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight(), 0.0D, 1000.0D, 3000.0D);
        RenderSystem.matrixMode(5888);
    }

    private static void postRenderingReset() {
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableAlphaTest();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.matrixMode(5889);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0D, (double) client.getWindow().getFramebufferWidth() / client.getWindow().getScaleFactor(), (double) client.getWindow().getFramebufferHeight() / client.getWindow().getScaleFactor(), 0.0D, 1000.0D, 3000.0D);
        RenderSystem.matrixMode(5888);
    }
}
