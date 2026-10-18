package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTextureRegistry;
import net.momirealms.craftengine.core.util.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

final class BedrockLayoutTextures implements BedrockTextureConverter {
    private static final List<String> CHESTS = List.of("normal", "trapped", "christmas", "ender", "copper", "copper_exposed", "copper_weathered", "copper_oxidized");
    private static final List<String> COLORS = List.of("black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray", "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow");

    @Override
    public void convert(JavaResourcePack pack, Path output, BedrockTextureRegistry textures) {
        for (String name : CHESTS) {
            String target = name.equals("copper") ? "copper_default" : name;
            BedrockVanillaTextures.transform(pack, output, textures, "textures/entity/chest/" + target + ".png", List.of("entity/chest/" + name + ".png"), images -> this.chest(images.getFirst()));
            if (!name.equals("ender")) {
                String doubleName = name.equals("normal") ? "double_normal" : (name.equals("trapped") ? "trapped_double" : target + "_double");
                BedrockVanillaTextures.transform(pack, output, textures, "textures/entity/chest/" + doubleName + ".png", List.of("entity/chest/" + name + "_left.png", "entity/chest/" + name + "_right.png"), this::doubleChest);
            }
        }
        BedrockVanillaTextures.transform(pack, output, textures, "textures/entity/zombie/drowned.png", List.of("entity/zombie/drowned.png", "entity/zombie/drowned_outer_layer.png"), this::drowned);
        for (String name : List.of("zombie", "husk")) {
            List<String> sources = List.of("entity/zombie/" + name + ".png");
            BedrockVanillaTextures.transform(pack, output, textures, "textures/entity/zombie/" + name + ".png", sources, images -> ImageUtils.crop(images.getFirst(), 0, 0, images.getFirst().getWidth(), images.getFirst().getWidth() / 2f));
            if (name.equals("zombie")) {
                BedrockVanillaTextures.transform(pack, output, textures, "textures/entity/skulls/zombie.png", sources, images -> ImageUtils.crop(images.getFirst(), 0, 0, images.getFirst().getWidth(), images.getFirst().getWidth() / 2f));
            }
        }
        for (String name : List.of("chain", "copper_chain", "exposed_copper_chain", "weathered_copper_chain", "oxidized_copper_chain")) {
            for (int index = 1; index <= 2; index++) {
                int side = index;
                BedrockVanillaTextures.transform(pack, output, textures, "textures/blocks/" + name + index + ".png", List.of("block/" + name + ".png"), images -> this.chain(images.getFirst(), side));
            }
        }
        BedrockVanillaTextures.transform(pack, output, textures, "textures/blocks/dried_kelp_side_b.png", List.of("block/dried_kelp_side.png"), images -> ImageUtils.flip(images.getFirst(), true, false));
        for (String part : List.of("top", "bottom")) {
            List<String> sources = List.of("block/tall_seagrass_" + part + ".png");
            BedrockVanillaTextures.transform(pack, output, textures, "textures/blocks/seagrass_doubletall_" + part + "_a.png", sources, List::getFirst);
            BedrockVanillaTextures.transform(pack, output, textures, "textures/blocks/seagrass_doubletall_" + part + "_b.png", sources, images -> ImageUtils.flip(images.getFirst(), true, false));
        }
        for (String type : List.of("flow", "still")) {
            BedrockVanillaTextures.transform(pack, output, textures, "textures/blocks/water_" + type + "_grey.png", List.of("block/water_" + type + ".png"), images -> ImageUtils.tint(images.getFirst(), 0xffffff));
        }
        for (String color : COLORS) {
            BedrockVanillaTextures.transform(pack, output, textures, "textures/items/bundle_" + color + "_open.png", List.of("item/" + color + "_bundle_open_back.png", "item/" + color + "_bundle_open_front.png"), this::bundle);
        }
        BedrockVanillaTextures.transform(pack, output, textures, "textures/items/bundle_open.png", List.of("item/bundle_open_back.png", "item/bundle_open_front.png"), this::bundle);
    }

    private BufferedImage chain(BufferedImage image, int side) {
        float scale = image.getWidth() / 16f;
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.drawImage(ImageUtils.crop(image, side == 1 ? 3 * scale : 0, 0, (side == 1 ? 6 : 3) * scale, image.getHeight()), 0, 0, null);
        graphics.dispose();
        return result;
    }

    private BufferedImage bundle(List<BufferedImage> images) {
        BufferedImage first = images.getFirst();
        BufferedImage result = new BufferedImage(first.getWidth(), first.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        for (BufferedImage image : images) {
            graphics.drawImage(image, 0, 0, result.getWidth(), result.getHeight(), null);
        }
        graphics.dispose();
        return result;
    }

    private BufferedImage chest(BufferedImage source) {
        BufferedImage chestImage = source;
        float factor = chestImage.getWidth() / 64f;
        BufferedImage newChestImage = new BufferedImage(chestImage.getWidth(), chestImage.getWidth(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = newChestImage.createGraphics();
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(chestImage, 0, (14 * factor), (14 * factor), (5 * factor)), 180), 0, (int) (14 * factor), null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(chestImage, 0, (33 * factor), (14 * factor), (10 * factor)), 180), 0, (int) (33 * factor), null);
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(chestImage, (28 * factor), 0, (14 * factor), (14 * factor)), false, true), (int) (14 * factor), 0, null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(chestImage, (42 * factor), (14 * factor), (14 * factor), (5 * factor)), 180), (int) (14 * factor), (int) (14 * factor), null);
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(chestImage, (28 * factor), (19 * factor), (14 * factor), (14 * factor)), false, true), (int) (14 * factor), (int) (19 * factor), null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(chestImage, (42 * factor), (33 * factor), (14 * factor), (10 * factor)), 180), (int) (14 * factor), (int) (33 * factor), null);
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(chestImage, (14 * factor), 0, (14 * factor), (14 * factor)), false, true), (int) (28 * factor), 0, null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(chestImage, (28 * factor), (14 * factor), (14 * factor), (5 * factor)), 180), (int) (28 * factor), (int) (14 * factor), null);
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(chestImage, (14 * factor), (19 * factor), (14 * factor), (14 * factor)), false, true), (int) (28 * factor), (int) (19 * factor), null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(chestImage, (28 * factor), (33 * factor), (14 * factor), (10 * factor)), 180), (int) (28 * factor), (int) (33 * factor), null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(chestImage, (14 * factor), (14 * factor), (14 * factor), (5 * factor)), 180), (int) (42 * factor), (int) (14 * factor), null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(chestImage, (14 * factor), (33 * factor), (14 * factor), (10 * factor)), 180), (int) (42 * factor), (int) (33 * factor), null);
        graphics.drawImage(ImageUtils.crop(chestImage, 0, 0, (6 * factor), (6 * factor)), 0, 0, null);
        graphics.dispose();
        return newChestImage;
    }

    private BufferedImage doubleChest(List<BufferedImage> images) {
        int width = Math.max(images.get(0).getWidth(), images.get(1).getWidth());
        BufferedImage leftImage = ImageUtils.resize(images.get(0), width, width);
        BufferedImage rightImage = ImageUtils.resize(images.get(1), width, width);
        float factor = width / 64f;
        BufferedImage newImage = new BufferedImage(width * 2, width, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = newImage.createGraphics();
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(rightImage, 0, (14 * factor), (14 * factor), (5 * factor)), 180), 0, (int) (14 * factor), null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(leftImage, (29 * factor), (14 * factor), (14 * factor), (5 * factor)), 180), (int) (44 * factor), (int) (14 * factor), null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(rightImage, 0, (33 * factor), (14 * factor), (10 * factor)), 180), 0, (int) (33 * factor), null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(leftImage, (29 * factor), (33 * factor), (14 * factor), (10 * factor)), 180), (int) (44 * factor), (int) (33 * factor), null);
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(rightImage, (29 * factor), 0, (15 * factor), (14 * factor)), false, true), (int) (14 * factor), 0, null);
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(leftImage, (29 * factor), 0, (15 * factor), (14 * factor)), false, true), (int) (29 * factor), 0, null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(rightImage, (43 * factor), (14 * factor), (15 * factor), (5 * factor)), 180), (int) (14 * factor), (int) (14 * factor), null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(leftImage, (43 * factor), (14 * factor), (15 * factor), (5 * factor)), 180), (int) (29 * factor), (int) (14 * factor), null);
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(rightImage, (29 * factor), (19 * factor), (15 * factor), (14 * factor)), false, true), (int) (14 * factor), (int) (19 * factor), null);
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(leftImage, (29 * factor), (19 * factor), (15 * factor), (14 * factor)), false, true), (int) (29 * factor), (int) (19 * factor), null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(rightImage, (43 * factor), (33 * factor), (15 * factor), (10 * factor)), 180), (int) (14 * factor), (int) (33 * factor), null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(leftImage, (43 * factor), (33 * factor), (15 * factor), (10 * factor)), 180), (int) (29 * factor), (int) (33 * factor), null);
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(rightImage, (14 * factor), 0, (15 * factor), (14 * factor)), false, true), (int) (44 * factor), 0, null);
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(leftImage, (14 * factor), 0, (15 * factor), (14 * factor)), false, true), (int) (59 * factor), 0, null);
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(rightImage, (14 * factor), (19 * factor), (15 * factor), (14 * factor)), false, true), (int) (44 * factor), (int) (19 * factor), null);
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(leftImage, (14 * factor), (19 * factor), (15 * factor), (14 * factor)), false, true), (int) (59 * factor), (int) (19 * factor), null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(leftImage, (14 * factor), (14 * factor), (15 * factor), (5 * factor)), 180), (int) (58 * factor), (int) (14 * factor), null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(rightImage, (14 * factor), (14 * factor), (15 * factor), (5 * factor)), 180), (int) (73 * factor), (int) (14 * factor), null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(leftImage, (14 * factor), (33 * factor), (15 * factor), (10 * factor)), 180), (int) (58 * factor), (int) (33 * factor), null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(rightImage, (14 * factor), (33 * factor), (15 * factor), (10 * factor)), 180), (int) (73 * factor), (int) (33 * factor), null);
        graphics.drawImage(ImageUtils.crop(leftImage, 0, 0, (6 * factor), (6 * factor)), 0, 0, null);
        graphics.drawImage(ImageUtils.crop(rightImage, 0, 0, (6 * factor), (6 * factor)), 0, 0, null);
        graphics.dispose();
        return newImage;
    }

    private BufferedImage drowned(List<BufferedImage> images) {
        BufferedImage fromImage = images.getFirst();
        BufferedImage overlayImage = ImageUtils.resize(images.get(1), fromImage.getWidth(), fromImage.getHeight());
        float factor = fromImage.getWidth() / 64f;
        BufferedImage newImage = new BufferedImage(fromImage.getWidth(), fromImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = newImage.createGraphics();
        graphics.drawImage(fromImage, 0, 0, null);
        graphics.drawImage(ImageUtils.crop(overlayImage, 0, 0, (int) (32 * factor), (int) (16 * factor)), (int) (32 * factor), 0, null);
        graphics.drawImage(ImageUtils.crop(overlayImage, 0, (int) (16 * factor), (int) (64 * factor), (int) (16 * factor)), 0, (int) (32 * factor), null);
        graphics.drawImage(ImageUtils.crop(overlayImage, (int) (16 * factor), (int) (48 * factor), (int) (16 * factor), (int) (16 * factor)), 0, (int) (48 * factor), null);
        graphics.drawImage(ImageUtils.crop(overlayImage, (int) (32 * factor), (int) (48 * factor), (int) (16 * factor), (int) (16 * factor)), (int) (48 * factor), (int) (48 * factor), null);
        graphics.dispose();
        return newImage;
    }
}
