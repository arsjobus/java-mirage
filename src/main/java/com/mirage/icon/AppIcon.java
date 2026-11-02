package com.mirage.icon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class AppIcon {

    public static final int[] SIZES = {16, 32, 48, 64, 128, 256, 512, 1024};

    private AppIcon() {
    }

    public static BufferedImage defaultIcon(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Icon size must be positive");
        }

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.scale(size / 128.0, size / 128.0);

            RoundRectangle2D tile = new RoundRectangle2D.Double(4, 4, 120, 120, 30, 30);
            g.setPaint(new java.awt.GradientPaint(8, 8, new Color(0x30235F), 116, 120, new Color(0x111B3D)));
            g.fill(tile);

            g.setColor(new Color(0xFFD66B));
            g.fill(new Ellipse2D.Double(76, 24, 28, 28));

            g.setColor(new Color(0x8B6BCE));
            Path2D farDune = new Path2D.Double();
            farDune.moveTo(9, 77);
            farDune.lineTo(34, 57);
            farDune.lineTo(52, 68);
            farDune.lineTo(76, 48);
            farDune.lineTo(119, 77);
            farDune.closePath();
            g.fill(farDune);

            g.setColor(new Color(0xF29C68));
            Path2D nearDune = new Path2D.Double();
            nearDune.moveTo(9, 83);
            nearDune.lineTo(36, 69);
            nearDune.lineTo(57, 78);
            nearDune.lineTo(85, 63);
            nearDune.lineTo(119, 81);
            nearDune.lineTo(119, 105);
            nearDune.lineTo(9, 105);
            nearDune.closePath();
            g.fill(nearDune);

            g.setColor(new Color(0x55D4C0));
            g.fill(new Ellipse2D.Double(45, 91, 39, 9));
            g.setColor(new Color(0xB6F4DC));
            g.fill(new Ellipse2D.Double(52, 92, 24, 3));

            g.setColor(new Color(0xFFE9A8));
            g.fillRect(22, 38, 4, 4);
            g.fillRect(30, 30, 3, 3);
            g.fillRect(40, 42, 3, 3);
        } finally {
            g.dispose();
        }
        return image;
    }

    public static byte[] png(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", out)) {
            throw new IOException("No PNG writer is available");
        }
        return out.toByteArray();
    }
}
