package net.momirealms.craftengine.core.pack.bedrock.gui;

import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.util.ClientPngReader;
import net.momirealms.craftengine.core.util.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public final class BedrockGuiGenerator {
    private BedrockGuiGenerator() {
    }

    public static void write(JavaResourcePack resources, Path root) throws IOException {
        BufferedImage background;
        // 六种行数只改变裁剪区域，共用一次读取、解码后的原始容器背景。
        try (InputStream input = resources.openResource("assets/minecraft/textures/gui/container/generic_54.png")) {
            if (input == null) {
                throw new IOException("Missing Java container background generic_54.png");
            }
            background = ClientPngReader.read(input);
        }
        BedrockGuiWriter ui = new BedrockGuiWriter(BedrockTitleLayers.hasImages(root));
        for (int rows = 1; rows <= 6; rows++) {
            BufferedImage image = new BufferedImage(176, 114 + rows * 18, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                drawContainer(background, graphics, rows);
            } finally {
                graphics.dispose();
            }
            String texture = "textures/ce/gui/container_" + rows;
            Path file = root.resolve(texture + ".png");
            ImageUtils.writePng(image, file);
            ui.add(rows, texture);
        }
        ui.write(root);
    }

    private static void drawContainer(BufferedImage image, Graphics2D graphics, int rows) {
        int split = rows * 18 + 17;
        int width = image.getWidth() * 176 / 256;
        int sourceSplit = image.getHeight() * split / 256;
        int sourceBottom = image.getHeight() * 126 / 256;
        int sourceEnd = image.getHeight() * 222 / 256;
        graphics.drawImage(image, 0, 0, 176, split, 0, 0, width, sourceSplit, null);
        graphics.drawImage(image, 0, split, 176, split + 96, 0, sourceBottom, width, sourceEnd, null);
    }
}
