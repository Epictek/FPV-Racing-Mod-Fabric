package dev.lazurite.fpvracing.client.renderer;

import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.GlAllocationUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class StaticRenderer2 {
    final int BYTES_PER_POINT = Float.BYTES * 3 + Byte.BYTES * 4;

    private final ExecutorService threadPool = Executors.newFixedThreadPool(2);

    private Future<ByteBuffer> stuffer;
    private Future<ByteBuffer> copier;

    private volatile ByteBuffer prevByteBuffer;
    private volatile ByteBuffer nextByteBuffer;

    private float prevTickDelta;

    private int desiredPseudopixelSize = 1;
    private boolean forceScaling = true;

    private int minRowHeight = 1;
    private int maxRowHeight = 1;
    private int minColumnWidth = 1;
    private int maxColumnWidth = 1;

    public void setDesiredPseudopixelSize(int desiredPseudopixelSize) {
        if (desiredPseudopixelSize >= 1) {
            this.desiredPseudopixelSize = desiredPseudopixelSize;
        }
    }

    public int getDesiredPseudopixelSize() {
        return desiredPseudopixelSize;
    }

    public void setForceScaling(boolean forceScaling) {
        this.forceScaling = forceScaling;
    }

    public boolean getForceScaling() {
        return forceScaling;
    }

    public void setMinRowHeight(int minRowHeight) {
        if (minRowHeight >= 1) {
            this.minRowHeight = minRowHeight;

            if (getMaxRowHeight() == 1) {
                setMaxRowHeight(getMinRowHeight());
            }
        }
    }

    public int getMinRowHeight() {
        return minRowHeight;
    }

    public void setMaxRowHeight(int maxRowHeight) {
        if (maxRowHeight >= 1) {
            this.maxRowHeight = maxRowHeight;
        }
    }

    public int getMaxRowHeight() {
        return maxRowHeight;
    }

    public void setMinColumnWidth(int minColumnWidth) {
        if (minColumnWidth >= 1) {
            this.minColumnWidth = minColumnWidth;

            if (getMaxColumnWidth() == 1) {
                setMaxColumnWidth(getMinColumnWidth());
            }
        }
    }

    public int getMinColumnWidth() {
        return minColumnWidth;
    }

    public void setMaxColumnWidth(int maxColumnWidth) {
        if (maxColumnWidth >= 1) {
            this.maxColumnWidth = maxColumnWidth;
        }
    }

    public int getMaxColumnWidth() {
        return maxColumnWidth;
    }

    public int calculateScaledHeight(int height, int width) {
        return (int) Math.ceil((double) height / calculateScalar(height, width));
    }

    public int calculateScaledWidth(int height, int width) {
        return (int) Math.ceil((double) width / calculateScalar(height, width));
    }

    public int calculateScalar(int height, int width) {
        int[] pointSizeRange = new int[2];
        GL11.glGetIntegerv(GL11.GL_POINT_SIZE_RANGE, pointSizeRange);
        int pointSize = Math.min(desiredPseudopixelSize, pointSizeRange[1]);

        if (getForceScaling()) {
            return gcdl(height, width, pointSize);
        } else {
            return pointSize;
        }
    }

    public synchronized void render(int height, int width, float tickDelta) {
        int scaledHeight = calculateScaledHeight(height, width);
        int scaledWidth = calculateScaledWidth(height, width);

        int bufferSize = scaledHeight * scaledWidth * BYTES_PER_POINT;

        if (stuffer == null) {
            nextByteBuffer = GlAllocationUtils.allocateByteBuffer(bufferSize);
            stuffer = threadPool.submit(new Stuffer(scaledHeight, scaledWidth), nextByteBuffer);
        }

        if (tickDelta < prevTickDelta && stuffer.isDone()) {
            if (copier == null || copier.isDone()) {
                if (prevByteBuffer == null || prevByteBuffer.capacity() < bufferSize) {
                    prevByteBuffer = GlAllocationUtils.allocateByteBuffer(bufferSize);
                }

                if (nextByteBuffer == null || nextByteBuffer.capacity() < bufferSize) {
                    nextByteBuffer = GlAllocationUtils.allocateByteBuffer(bufferSize);
                }

                copier = threadPool.submit(new Copier(), prevByteBuffer);
            }
        }

        if (prevByteBuffer != null && prevByteBuffer.capacity() >= bufferSize && copier.isDone()) {
            if (stuffer.isDone()) {
                stuffer = threadPool.submit(new Stuffer(scaledHeight, scaledWidth), nextByteBuffer);
            }

            BufferRenderer.draw(prevByteBuffer, GL11.GL_POINTS, VertexFormats.POSITION_COLOR, scaledHeight * scaledWidth);
        } else if (nextByteBuffer != null && nextByteBuffer.capacity() >= bufferSize) {
            BufferRenderer.draw(nextByteBuffer, GL11.GL_POINTS, VertexFormats.POSITION_COLOR, scaledHeight * scaledWidth);
        }

        prevTickDelta = tickDelta;
    }

    // Unsafe. Doesn't handle negative or zero numbers.
    private static int gcd(int a, int b) {
        if (a == b) {
            return a;
        } else if (a > b) {
            return gcd(a - b, b);
        } else {
            return gcd(a, b - a);
        }
    }

    // Unsafe. Doesn't handle negative or zero numbers.
    private static int gcdl(int a, int b, int limit) {
        int gcd = gcd(a, b);

        if (gcd < limit) {
            return gcd;
        } else {
            int cd = limit;
            while (cd > 1) {
                if (gcd % cd == 0) {
                    return cd;
                }

                --cd;
            }

            return 1;
        }
    }

    private class Stuffer implements Runnable {
        int scaledHeight;
        int scaledWidth;

        private Stuffer(int scaledHeight, int scaledWidth) {
            this.scaledHeight = scaledHeight;
            this.scaledWidth = scaledWidth;
        }

        @Override
        public synchronized void run() {
            Random random = new Random();

            int elementOffset = 0;
            int rowCounter = 0;

            byte color;

            float rowHeight = 0.0f;
            float columnWidth;

            byte[] lastRowColors = new byte[scaledWidth];
            float[] lastRowColumnWidths = new float[scaledWidth];

            nextByteBuffer.clear();

            for (float currentHeight = 0.5f; currentHeight <= scaledHeight; ++currentHeight) {
                if (rowHeight == 0.0f || rowCounter >= rowHeight) {
                    rowCounter = 0;
                    rowHeight = Math.round(random.nextFloat() * (maxRowHeight - minRowHeight) + minRowHeight);
                }

                float currentWidth = 0.5f;

                while (currentWidth <= scaledWidth) {
                    if (rowCounter != 0) {
                        color = lastRowColors[(int) currentWidth];
                        columnWidth = lastRowColumnWidths[(int) currentWidth];
                    } else {
                        color = (byte) random.nextInt(256);

                        if (maxColumnWidth <= (float) scaledWidth - currentWidth) {
                            columnWidth = random.nextFloat() * (maxColumnWidth - minColumnWidth) + minColumnWidth;
                        } else if (minColumnWidth <= ((float) scaledWidth - currentWidth)) {
                            columnWidth = random.nextFloat() * ((float) scaledWidth - currentWidth - minColumnWidth) + minColumnWidth;
                        } else {
                            columnWidth = (float) scaledWidth - currentWidth;
                        }

                        lastRowColors[(int) currentWidth] = color;
                        lastRowColumnWidths[(int) currentWidth] = columnWidth;
                    }

                    float rowSectionWidth = currentWidth;

                    for (; rowSectionWidth < currentWidth + columnWidth; ++rowSectionWidth) {
                        nextByteBuffer.putFloat(elementOffset, rowSectionWidth);
                        nextByteBuffer.putFloat(elementOffset + Float.BYTES, currentHeight);
                        nextByteBuffer.putFloat(elementOffset + Float.BYTES * 2, -90.0f);

                        nextByteBuffer.put(elementOffset + Float.BYTES * 3, color);
                        nextByteBuffer.put(elementOffset + Float.BYTES * 3 + Byte.BYTES, color);
                        nextByteBuffer.put(elementOffset + Float.BYTES * 3 + Byte.BYTES * 2, color);
                        nextByteBuffer.put(elementOffset + Float.BYTES * 3 + Byte.BYTES * 3, (byte) 255);

                        elementOffset += BYTES_PER_POINT;
                    }

                    currentWidth = rowSectionWidth;
                }

                ++rowCounter;
            }
        }
    }

    private class Copier implements Runnable {
        @Override
        public synchronized void run() {
            prevByteBuffer.put(nextByteBuffer);
        }
    }
}
