package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTextureRegistry;
import net.momirealms.craftengine.core.util.ImageUtils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

final class BedrockBedTextures implements BedrockTextureConverter {
    private static final List<String> COLORS = List.of("black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray", "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow");

    @Override
    public void convert(JavaResourcePack pack, Path output, BedrockTextureRegistry textures) {
        for (String color : COLORS) {
            List<String> sources = List.of(
                    "block/bed_head_north.png", "block/bed_down.png",
                    "block/" + color + "_bed_head_up.png", "block/" + color + "_bed_head_east.png", "block/" + color + "_bed_head_west.png",
                    "block/" + color + "_bed_foot_up.png", "block/" + color + "_bed_foot_south.png", "block/" + color + "_bed_foot_east.png",
                    "block/" + color + "_bed_foot_west.png");
            String name = color.equals("light_gray") ? "silver" : color;
            BedrockVanillaTextures.transform(pack, output, textures, "textures/entity/bed/" + name + ".png", sources, this::bed);
        }
    }

    private BufferedImage bed(List<BufferedImage> images) {
        int width = images.stream().mapToInt(BufferedImage::getWidth).max().orElseThrow();
        float scale = width / 16f;
        BufferedImage bedHeadNorthImage = ImageUtils.resize(images.get(0), width, width);
        BufferedImage bedDownImage = ImageUtils.resize(images.get(1), width, width);
        BufferedImage bedHeadUpImage = ImageUtils.resize(images.get(2), width, width);
        BufferedImage bedHeadEastImage = ImageUtils.resize(images.get(3), width, width);
        BufferedImage bedHeadWestImage = ImageUtils.resize(images.get(4), width, width);
        BufferedImage bedFootUpImage = ImageUtils.resize(images.get(5), width, width);
        BufferedImage bedFootSouthImage = ImageUtils.resize(images.get(6), width, width);
        BufferedImage bedFootEastImage = ImageUtils.resize(images.get(7), width, width);
        BufferedImage bedFootWestImage = ImageUtils.resize(images.get(8), width, width);
        BufferedImage bedDownFace = ImageUtils.rotate(bedDownImage, 180);
        BufferedImage newBedImage = new BufferedImage(width * 4, width * 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = newBedImage.createGraphics();
        // Rearrange the Java mattress and leg faces into the fixed Bedrock bed UV layout.
        graphics.drawImage(ImageUtils.rotate(this.mattressFace(bedHeadNorthImage, scale), 180), (int) (6 * scale), 0, null);
        graphics.drawImage(ImageUtils.rotate(this.mattressFace(bedHeadEastImage, scale), 90), (int) (22 * scale), (int) (6 * scale), null);
        graphics.drawImage(ImageUtils.rotate(this.mattressFace(bedHeadWestImage, scale), 270), 0, (int) (6 * scale), null);
        graphics.drawImage(bedHeadUpImage, (int) (6 * scale), (int) (6 * scale), null);
        graphics.drawImage(bedDownFace, (int) (28 * scale), (int) (6 * scale), null);

        graphics.drawImage(ImageUtils.flip(this.mattressFace(bedFootSouthImage, scale), false, true), (int) (22 * scale), 0, null);
        graphics.drawImage(ImageUtils.rotate(this.mattressFace(bedFootEastImage, scale), 90), (int) (22 * scale), (int) (22 * scale), null);
        graphics.drawImage(ImageUtils.rotate(this.mattressFace(bedFootWestImage, scale), 270), 0, (int) (22 * scale), null);
        graphics.drawImage(bedFootUpImage, (int) (6 * scale), (int) (22 * scale), null);
        graphics.drawImage(bedDownFace, (int) (28 * scale), (int) (22 * scale), null);

        BufferedImage headWestLegEnd = ImageUtils.rotate(this.legFace(bedHeadWestImage, scale, 6), 180);
        BufferedImage headEastLegEnd = ImageUtils.rotate(this.legFace(bedHeadEastImage, scale, 7), 180);
        BufferedImage footWestLegEnd = ImageUtils.rotate(this.legFace(bedFootWestImage, scale, 7), 180);
        BufferedImage footEastLegEnd = ImageUtils.rotate(this.legFace(bedFootEastImage, scale, 6), 180);

        graphics.drawImage(ImageUtils.flip(this.legFace(bedHeadWestImage, scale, 0), false, true), (int) (3 * scale), (int) (38 * scale), null);
        graphics.drawImage(ImageUtils.flip(this.legFace(bedHeadWestImage, scale, 3), false, true), (int) (6 * scale), (int) (38 * scale), null);
        graphics.drawImage(ImageUtils.rotate(this.legFace(bedHeadNorthImage, scale, 13), 90), 0, (int) (41 * scale), null);
        graphics.drawImage(ImageUtils.rotate(this.legFace(bedHeadNorthImage, scale, 10), 90), (int) (6 * scale), (int) (41 * scale), null);
        graphics.drawImage(headWestLegEnd, (int) (3 * scale), (int) (41 * scale), null);
        graphics.drawImage(headWestLegEnd, (int) (9 * scale), (int) (41 * scale), null);

        graphics.drawImage(ImageUtils.flip(this.legFace(bedHeadNorthImage, scale, 0), true, false), (int) (15 * scale), (int) (38 * scale), null);
        graphics.drawImage(ImageUtils.flip(this.legFace(bedHeadEastImage, scale, 10), false, true), (int) (18 * scale), (int) (38 * scale), null);
        graphics.drawImage(ImageUtils.rotate(this.legFace(bedHeadNorthImage, scale, 3), 270), (int) (12 * scale), (int) (41 * scale), null);
        graphics.drawImage(ImageUtils.rotate(this.legFace(bedHeadEastImage, scale, 13), 90), (int) (18 * scale), (int) (41 * scale), null);
        graphics.drawImage(headEastLegEnd, (int) (15 * scale), (int) (41 * scale), null);
        graphics.drawImage(headEastLegEnd, (int) (21 * scale), (int) (41 * scale), null);

        graphics.drawImage(ImageUtils.rotate(this.legFace(bedFootWestImage, scale, 10), 180), (int) (3 * scale), (int) (44 * scale), null);
        graphics.drawImage(ImageUtils.flip(this.legFace(bedFootWestImage, scale, 13), true, false), (int) (6 * scale), (int) (44 * scale), null);
        graphics.drawImage(ImageUtils.rotate(this.legFace(bedFootSouthImage, scale, 0), 90), 0, (int) (47 * scale), null);
        graphics.drawImage(ImageUtils.rotate(this.legFace(bedFootSouthImage, scale, 3), 90), (int) (6 * scale), (int) (47 * scale), null);
        graphics.drawImage(footWestLegEnd, (int) (3 * scale), (int) (47 * scale), null);
        graphics.drawImage(footWestLegEnd, (int) (9 * scale), (int) (47 * scale), null);

        graphics.drawImage(ImageUtils.rotate(this.legFace(bedFootEastImage, scale, 3), 180), (int) (15 * scale), (int) (44 * scale), null);
        graphics.drawImage(ImageUtils.flip(this.legFace(bedFootEastImage, scale, 0), true, false), (int) (18 * scale), (int) (44 * scale), null);
        graphics.drawImage(ImageUtils.rotate(this.legFace(bedFootSouthImage, scale, 10), 270), (int) (12 * scale), (int) (47 * scale), null);
        graphics.drawImage(ImageUtils.rotate(this.legFace(bedFootSouthImage, scale, 13), 270), (int) (18 * scale), (int) (47 * scale), null);
        graphics.drawImage(footEastLegEnd, (int) (15 * scale), (int) (47 * scale), null);
        graphics.drawImage(footEastLegEnd, (int) (21 * scale), (int) (47 * scale), null);
        graphics.dispose();
        return newBedImage;
    }

    private BufferedImage mattressFace(BufferedImage side, float scale) {
        return ImageUtils.crop(side, 0, 7 * scale, 16 * scale, 6 * scale);
    }

    private BufferedImage legFace(BufferedImage side, float scale, int sourceX) {
        return ImageUtils.crop(side, sourceX * scale, 13 * scale, 3 * scale, 3 * scale);
    }
}
