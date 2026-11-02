package com.mirage.icon;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class IconExporter {

    public static final String DEFAULT_DIRECTORY = "packaging/icons";

    private static final int[][] ICNS_TYPES = {
            {16, 0x69637034}, {32, 0x69637035}, {64, 0x69637036},
            {128, 0x69633037}, {256, 0x69633038}, {512, 0x69633039},
            {1024, 0x69633130}
    };

    private IconExporter() {
    }

    static byte[] ico(List<BufferedImage> images) throws IOException {
        List<byte[]> pngs = new ArrayList<>();
        int offset = 6 + 16 * images.size();
        int total = offset;
        for (BufferedImage image : images) {
            if (image.getWidth() > 256 || image.getHeight() > 256) {
                throw new IllegalArgumentException("ICO images must be at most 256 pixels");
            }
            byte[] png = AppIcon.png(image);
            pngs.add(png);
            total += png.length;
        }

        ByteBuffer out = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        out.putShort((short) 0).putShort((short) 1).putShort((short) images.size());
        for (int i = 0; i < images.size(); i++) {
            BufferedImage image = images.get(i);
            out.put((byte) (image.getWidth() == 256 ? 0 : image.getWidth()));
            out.put((byte) (image.getHeight() == 256 ? 0 : image.getHeight()));
            out.put((byte) 0).put((byte) 0);
            out.putShort((short) 1).putShort((short) 32);
            out.putInt(pngs.get(i).length).putInt(offset);
            offset += pngs.get(i).length;
        }
        for (byte[] png : pngs) {
            out.put(png);
        }
        return out.array();
    }

    static byte[] icns(List<BufferedImage> images) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (BufferedImage image : images) {
            int type = icnsType(image);
            byte[] png = AppIcon.png(image);
            ByteBuffer element = ByteBuffer.allocate(8 + png.length).order(ByteOrder.BIG_ENDIAN);
            element.putInt(type).putInt(8 + png.length).put(png);
            body.write(element.array());
        }
        ByteBuffer out = ByteBuffer.allocate(8 + body.size()).order(ByteOrder.BIG_ENDIAN);
        out.putInt(0x69636E73).putInt(8 + body.size()).put(body.toByteArray());
        return out.array();
    }

    private static int icnsType(BufferedImage image) {
        if (image.getWidth() == image.getHeight()) {
            for (int[] entry : ICNS_TYPES) {
                if (entry[0] == image.getWidth()) {
                    return entry[1];
                }
            }
        }
        throw new IllegalArgumentException("ICNS images must be square and have a supported icon size");
    }

    public static void export(Path directory) throws IOException {
        Files.createDirectories(directory);

        List<BufferedImage> windows = new ArrayList<>();
        for (int size : new int[]{16, 32, 48, 64, 128, 256}) {
            windows.add(AppIcon.defaultIcon(size));
        }
        List<BufferedImage> mac = new ArrayList<>();
        for (int[] entry : ICNS_TYPES) {
            mac.add(AppIcon.defaultIcon(entry[0]));
        }

        Files.write(directory.resolve("icon.ico"), ico(windows));
        Files.write(directory.resolve("icon.icns"), icns(mac));
        Files.write(directory.resolve("icon.png"), AppIcon.png(AppIcon.defaultIcon(512)));
    }

    public static void main(String[] args) throws IOException {
        export(Path.of(args.length > 0 ? args[0] : DEFAULT_DIRECTORY));
    }
}
