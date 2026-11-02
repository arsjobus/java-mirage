package com.mirage.icon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IconExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void exportsIconsForNativePackagers() throws Exception {
        IconExporter.export(tempDir);

        byte[] ico = Files.readAllBytes(tempDir.resolve("icon.ico"));
        assertEquals(0, ByteBuffer.wrap(ico).order(ByteOrder.LITTLE_ENDIAN).getShort());
        assertEquals(1, ByteBuffer.wrap(ico).order(ByteOrder.LITTLE_ENDIAN).getShort(2));
        assertEquals(6, ByteBuffer.wrap(ico).order(ByteOrder.LITTLE_ENDIAN).getShort(4));

        byte[] icns = Files.readAllBytes(tempDir.resolve("icon.icns"));
        assertEquals("icns", new String(icns, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
        assertEquals(icns.length, ByteBuffer.wrap(icns).order(ByteOrder.BIG_ENDIAN).getInt(4));

        byte[] png = Files.readAllBytes(tempDir.resolve("icon.png"));
        assertArrayEquals(new byte[]{(byte) 0x89, 'P', 'N', 'G'}, java.util.Arrays.copyOf(png, 4));
    }
}
