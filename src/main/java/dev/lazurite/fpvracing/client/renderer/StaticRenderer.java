package dev.lazurite.fpvracing.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.lazurite.fpvracing.client.ClientInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.GlAllocationUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class StaticRenderer extends Thread {
    private final static MinecraftClient client = ClientInitializer.client;
    private static float prevTickDelta;
    private static int scalar = 1;
    private static int maxPointSize = 1;

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(2);
    private static Future<ByteBuffer> copier;
    private static Future<ByteBuffer> stuffer;

    // look into synchronized
    private volatile static ByteBuffer prevByteBuffer;
    private volatile static ByteBuffer nextByteBuffer;

    private static final AtomicInteger prevCount = new AtomicInteger();
    private static final AtomicInteger nextCount = new AtomicInteger();

    private static class StaticBufferCopier implements Runnable {
        @Override
        public void run() {
            prevByteBuffer.put(nextByteBuffer);
            prevCount.set(nextCount.get());
        }
    }

    private static class StaticBufferStuffer implements Runnable {
        private final ByteBuffer buffer;

        private final float minRowHeight;
        private final float maxRowHeight;
        private final float minColumnWidth;
        private final float maxColumnWidth;

        StaticBufferStuffer(ByteBuffer buffer, float minRowHeight, float maxRowHeight, float minColumnWidth, float maxColumnWidth) {
            this.buffer = buffer;

            this.minRowHeight = minRowHeight;
            this.maxRowHeight = maxRowHeight;
            this.minColumnWidth = minColumnWidth;
            this.maxColumnWidth = maxColumnWidth;
        }

        @Override
        public void run() {
            Random random = new Random();

            int i_elementOffset = 0; // running buffer offset

            byte b_color = 0; // randomized grayscale pixel color
            int i_rowCounter = 0; // keeps count of rows for rowSpacing to work
            float f_rowHeight = 0.0f; // randomized row height between minRowHeight and maxRowHeight
            float f_columnWidth = 0.0f; // randomized column width between minColumnWidth and maxColumnWidth

            int scaledFrameBufferHeight = calculateScaledFrameBufferSize(client.getWindow().getFramebufferHeight(), scalar);
            int scaledFrameBufferWidth = calculateScaledFrameBufferSize(client.getWindow().getFramebufferWidth(), scalar);

            byte[] lastRowColors = new byte[scaledFrameBufferWidth];
            float[] lastRowColumnWidth = new float[scaledFrameBufferWidth];

            nextCount.set(0);
            buffer.clear();

            // loop through pixel rows
            for (float f_currentHeight = 0.5f; f_currentHeight <= scaledFrameBufferHeight; ++f_currentHeight) {
                // determines the height of each pseudo-pixel
                if (f_rowHeight == 0.0f || i_rowCounter >= f_rowHeight) {
                    i_rowCounter = 0;
                    f_rowHeight = random.nextFloat() * (maxRowHeight - minRowHeight) + minRowHeight;
                }

                float f_currentWidth = 0.5f;

                // loop through individual pixels of each row
                while (f_currentWidth <= scaledFrameBufferWidth) {
                    // determines whether to setup for a new pseudo-pixel or continue on the current pseudo-pixel
                    if (i_rowCounter == 0) {
                        b_color = (byte) random.nextInt(256);

                        // determines the width of each pseudo-pixel
                        if (maxColumnWidth <= (float) scaledFrameBufferWidth - f_currentWidth) {
                            f_columnWidth = random.nextFloat() * (maxColumnWidth - minColumnWidth) + minColumnWidth;
                        } else if (minColumnWidth <= ((float) scaledFrameBufferWidth - f_currentWidth)) {
                            f_columnWidth = random.nextFloat() * ((float) scaledFrameBufferWidth - f_currentWidth - minColumnWidth) + minColumnWidth;
                        } else {
                            f_columnWidth = (float) scaledFrameBufferWidth - f_currentWidth;
                        }

                        lastRowColors[(int) f_currentWidth] = b_color;
                        lastRowColumnWidth[(int) f_currentWidth] = f_columnWidth;
                    } else {
                        b_color = lastRowColors[(int) f_currentWidth];
                        f_columnWidth = lastRowColumnWidth[(int) f_currentWidth];
                    }

                    float rowSectionWidth = f_currentWidth;

                    for (; rowSectionWidth <= f_currentWidth + f_columnWidth; ++rowSectionWidth) {
                        buffer.putFloat(i_elementOffset, rowSectionWidth);
                        buffer.putFloat(i_elementOffset + 4, f_currentHeight);
                        buffer.putFloat(i_elementOffset + 8, -90.0f);

                        buffer.put(i_elementOffset + 12, b_color);
                        buffer.put(i_elementOffset + 13, b_color);
                        buffer.put(i_elementOffset + 14, b_color);
                        buffer.put(i_elementOffset + 15, (byte) 255);
                        i_elementOffset += 16;
                        nextCount.incrementAndGet();
                    }

//                    ++f_currentWidth; // for use without randomizing code
                    f_currentWidth = rowSectionWidth;
                }

                ++i_rowCounter;
            }
        }
    }

    public static void render(float minRowHeight, float maxRowHeight, float minColumnWidth, float maxColumnWidth, float tickDelta) {
        // get the max viable point size
        int[] pointer = new int[2];
        GL11.glGetIntegerv(GL11.GL_POINT_SIZE_RANGE, pointer);
        maxPointSize = pointer[1];

        scalar = gcdl(client.getWindow().getFramebufferHeight(), client.getWindow().getFramebufferWidth(), 2);

        int frameBufferHeight = calculateScaledFrameBufferSize(client.getWindow().getFramebufferHeight(), scalar);
        int frameBufferWidth = calculateScaledFrameBufferSize(client.getWindow().getFramebufferWidth(), scalar);
        int bufferSize = frameBufferHeight * frameBufferWidth * 16;

        if (stuffer == null) {
            nextByteBuffer = GlAllocationUtils.allocateByteBuffer(bufferSize);
            stuffer = threadPool.submit(new StaticBufferStuffer(nextByteBuffer, minRowHeight, maxRowHeight, minColumnWidth, maxColumnWidth), nextByteBuffer);
        }

        if (tickDelta < prevTickDelta && stuffer.isDone()) {
            if (copier == null || copier.isDone()) {
                if (prevByteBuffer == null || prevByteBuffer.capacity() != bufferSize) {
                    prevByteBuffer = GlAllocationUtils.allocateByteBuffer(bufferSize);
                }

                if (nextByteBuffer == null || nextByteBuffer.capacity() != bufferSize) {
                    nextByteBuffer = GlAllocationUtils.allocateByteBuffer(bufferSize);
                }

                copier = threadPool.submit(new StaticBufferCopier(), prevByteBuffer);
            }
        }

        if (prevByteBuffer != null && prevByteBuffer.capacity() == bufferSize && copier.isDone()) {
            if (stuffer.isDone()) {
                stuffer = threadPool.submit(new StaticBufferStuffer(nextByteBuffer, minRowHeight, maxRowHeight, minColumnWidth, maxColumnWidth), nextByteBuffer);
            }

            preRenderingSetup();
            BufferRenderer.draw(prevByteBuffer, GL11.GL_POINTS, VertexFormats.POSITION_COLOR, prevCount.get());
            postRenderingReset();
        } else if (nextByteBuffer != null && nextByteBuffer.capacity() == bufferSize) {
            preRenderingSetup();
            BufferRenderer.draw(nextByteBuffer, GL11.GL_POINTS, VertexFormats.POSITION_COLOR, nextCount.get());
            postRenderingReset();
        }

        prevTickDelta = tickDelta;
    }

    private static int calculateScaledFrameBufferSize(int bufferSize, int scalar) {
        return Math.floor((double) bufferSize / scalar) < (double) bufferSize / scalar ? bufferSize / scalar + 1 : bufferSize / scalar;
    }

    private static int gcd(int a, int b) {
        if (a == 0) {
            return b;
        } else if (b == 0) {
            return a;
        }

        if (a == b) {
            return a;
        }

        if (a > b) {
            return gcd(a - b, b); // a % b?
        } else {
            return gcd(a, b - a); // b % a?
        }
    }

    // TODO make more efficient
    private static int gcdl(int a, int b, int limit) {
        int gcd = gcd(a, b);

        if (gcd < limit) {
            return gcd;
        }

        int cd = limit;
        while (cd > 1) {
            if (gcd % cd == 0) {
                return cd;
            }

            --cd;
        }

        return 1;
    }

    private static void preRenderingSetup() {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableAlphaTest();

        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(
                0.0,
                (float) calculateScaledFrameBufferSize(client.getWindow().getFramebufferWidth(), scalar),
                (float) calculateScaledFrameBufferSize(client.getWindow().getFramebufferHeight(), scalar),
                0.0,
                1000.0,
                3000.0
        );
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);

        GL11.glPointSize(scalar);
    }

    private static void postRenderingReset() {
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableAlphaTest();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(
                0.0,
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight(),
                0.0,
                1000.0,
                3000.0
        );
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);

        GL11.glPointSize(1);
    }
}
