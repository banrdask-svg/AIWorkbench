package com.aicraft.aiworkbench.visual;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

final class PngWriter {
    private PngWriter() {
    }

    static byte[] solid(int width, int height, byte r, byte g, byte b, byte a) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10});
            chunk(output, "IHDR", ByteBuffer.allocate(13)
                    .putInt(width).putInt(height).put((byte) 8).put((byte) 6).put((byte) 0).put((byte) 0).put((byte) 0).array());
            byte[] raw = new byte[(width * 4 + 1) * height];
            int offset = 0;
            for (int y = 0; y < height; y++) {
                raw[offset++] = 0;
                for (int x = 0; x < width; x++) {
                    raw[offset++] = r;
                    raw[offset++] = g;
                    raw[offset++] = b;
                    raw[offset++] = a;
                }
            }
            Deflater deflater = new Deflater(Deflater.BEST_SPEED);
            deflater.setInput(raw);
            deflater.finish();
            byte[] buffer = new byte[raw.length + 64];
            int length = deflater.deflate(buffer);
            deflater.end();
            chunk(output, "IDAT", java.util.Arrays.copyOf(buffer, length));
            chunk(output, "IEND", new byte[0]);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("PNG write failed", exception);
        }
    }

    private static void chunk(ByteArrayOutputStream output, String type, byte[] data) throws IOException {
        output.write(ByteBuffer.allocate(4).putInt(data.length).array());
        byte[] typeBytes = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        output.write(typeBytes);
        output.write(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        output.write(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }
}
