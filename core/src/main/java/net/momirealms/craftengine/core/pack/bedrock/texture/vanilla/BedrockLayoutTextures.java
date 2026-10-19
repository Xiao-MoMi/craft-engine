package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import net.momirealms.craftengine.core.pack.bedrock.BedrockConversionContext;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.LegacyChatFormatter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

final class BedrockLayoutTextures implements BedrockTextureConverter {

    @Override
    public void convert(BedrockConversionContext context) {
        for (String name : List.of(
                "normal",
                "trapped",
                "ender",
                "copper",
                "copper_exposed",
                "copper_weathered",
                "copper_oxidized"
        )) {
            String target = name.equals("copper") ? "copper_default" : name;
            BedrockVanillaTextures.transform(
                    context,
                    "textures/entity/chest/" + target + ".png",
                    List.of("entity/chest/" + name + ".png"),
                    images -> this.chest(images.getFirst())
            );
            if (!name.equals("ender")) {
                String doubleName = name.equals("normal") ? "double_normal" : (name.equals("trapped") ? "trapped_double" : target + "_double");
                BedrockVanillaTextures.transform(
                        context,
                        "textures/entity/chest/" + doubleName + ".png",
                        List.of("entity/chest/" + name + "_left.png", "entity/chest/" + name + "_right.png"),
                        this::doubleChest
                );
            }
        }
        BedrockVanillaTextures.transform(
                context,
                "textures/entity/zombie/drowned.png",
                List.of("entity/zombie/drowned.png", "entity/zombie/drowned_outer_layer.png"),
                this::drowned
        );
        for (String name : List.of(
                "zombie",
                "husk"
        )) {
            List<String> sources = List.of("entity/zombie/" + name + ".png");
            BedrockVanillaTextures.transform(
                    context,
                    "textures/entity/zombie/" + name + ".png",
                    sources,
                    images -> ImageUtils.crop(images.getFirst(), 0, 0, images.getFirst().getWidth(), images.getFirst().getWidth() / 2f)
            );
            if (name.equals("zombie")) {
                BedrockVanillaTextures.transform(
                        context,
                        "textures/entity/skulls/zombie.png",
                        sources,
                        images -> ImageUtils.crop(images.getFirst(), 0, 0, images.getFirst().getWidth(), images.getFirst().getWidth() / 2f)
                );
            }
        }
        for (String name : List.of(
                "chain",
                "copper_chain",
                "exposed_copper_chain",
                "weathered_copper_chain",
                "oxidized_copper_chain"
        )) {
            String source = name.equals("chain") ? "iron_chain" : name;
            for (int index = 1; index <= 2; index++) {
                int side = index;
                BedrockVanillaTextures.transform(
                        context,
                        "textures/blocks/" + name + index + ".png",
                        List.of("block/" + source + ".png"),
                        images -> this.chain(images.getFirst(), side)
                );
            }
        }
        BedrockVanillaTextures.transform(
                context,
                "textures/blocks/dried_kelp_side_b.png",
                List.of("block/dried_kelp_side.png"),
                images -> ImageUtils.flip(images.getFirst(), true, false)
        );
        for (String part : List.of("top", "bottom")) {
            List<String> sources = List.of("block/tall_seagrass_" + part + ".png");
            BedrockVanillaTextures.transform(
                    context,
                    "textures/blocks/seagrass_doubletall_" + part + "_a.png",
                    sources,
                    List::getFirst
            );
            BedrockVanillaTextures.transform(
                    context,
                    "textures/blocks/seagrass_doubletall_" + part + "_b.png",
                    sources,
                    images -> ImageUtils.flip(images.getFirst(), true, false)
            );
        }
        for (String type : List.of(
                "flow",
                "still"
        )) {
            BedrockVanillaTextures.transform(
                    context,
                    "textures/blocks/water_" + type + "_grey.png",
                    List.of("block/water_" + type + ".png"),
                    images -> ImageUtils.tint(images.getFirst(), 0xffffff)
            );
        }
        for (String color : LegacyChatFormatter.COLORS) {
            BedrockVanillaTextures.transform(
                    context,
                    "textures/items/bundle_" + color + "_open.png",
                    List.of("item/" + color + "_bundle_open_back.png", "item/" + color + "_bundle_open_front.png"),
                    this::bundle
            );
        }
        BedrockVanillaTextures.transform(
                context,
                "textures/items/bundle_open.png",
                List.of("item/bundle_open_back.png", "item/bundle_open_front.png"),
                this::bundle
        );
        for (String color : LegacyChatFormatter.COLORS) {
            List<String> sources = List.of(
                    "block/bed_head_north.png", "block/bed_down.png",
                    "block/" + color + "_bed_head_up.png", "block/" + color + "_bed_head_east.png", "block/" + color + "_bed_head_west.png",
                    "block/" + color + "_bed_foot_up.png", "block/" + color + "_bed_foot_south.png", "block/" + color + "_bed_foot_east.png",
                    "block/" + color + "_bed_foot_west.png"
            );
            String name = color.equals("light_gray") ? "silver" : color;
            BedrockVanillaTextures.transform(
                    context,
                    "textures/entity/bed/" + name + ".png",
                    sources,
                    this::bed
            );
        }
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
        int width = 0;
        int height = 0;
        for (BufferedImage image : images) {
            width = Math.max(width, image.getWidth());
            height = Math.max(height, image.getHeight());
        }
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
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
        graphics.setComposite(AlphaComposite.Src);
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
        // Java 的两半锁扣各宽 1，基岩版使用一个宽 2 的锁扣；逐面拼接，不能直接叠加两张 UV。
        // 与箱体一样，上下表面互换并纵向翻转，四周表面旋转 180 度。
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(rightImage, 2 * factor, 0, factor, factor), false, true), (int) factor, 0, null);
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(leftImage, 2 * factor, 0, factor, factor), false, true), (int) (2 * factor), 0, null);
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(rightImage, factor, 0, factor, factor), false, true), (int) (3 * factor), 0, null);
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(leftImage, factor, 0, factor, factor), false, true), (int) (4 * factor), 0, null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(rightImage, 0, factor, factor, 4 * factor), 180), 0, (int) factor, null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(leftImage, 2 * factor, factor, factor, 4 * factor), 180), (int) (3 * factor), (int) factor, null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(rightImage, 3 * factor, factor, factor, 4 * factor), 180), (int) factor, (int) factor, null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(leftImage, 3 * factor, factor, factor, 4 * factor), 180), (int) (2 * factor), (int) factor, null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(leftImage, factor, factor, factor, 4 * factor), 180), (int) (4 * factor), (int) factor, null);
        graphics.drawImage(ImageUtils.rotate(ImageUtils.crop(rightImage, factor, factor, factor, 4 * factor), 180), (int) (5 * factor), (int) factor, null);
        graphics.dispose();
        return newImage;
    }

    private BufferedImage drowned(List<BufferedImage> images) {
        int width = Math.max(images.getFirst().getWidth(), images.get(1).getWidth());
        BufferedImage fromImage = ImageUtils.resize(images.getFirst(), width, width);
        BufferedImage overlayImage = ImageUtils.resize(images.get(1), width, width);
        float factor = width / 64f;
        BufferedImage newImage = new BufferedImage(width, width, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = newImage.createGraphics();
        // 这里只重排 UV，外层属于独立的几何面，不能与底层混色或继承底层的不透明像素。
        graphics.setComposite(AlphaComposite.Src);
        graphics.drawImage(ImageUtils.crop(fromImage, 0, 0, 32 * factor, 16 * factor), 0, 0, null);
        graphics.drawImage(ImageUtils.crop(fromImage, 16 * factor, 16 * factor, 24 * factor, 16 * factor), (int) (16 * factor), (int) (16 * factor), null);
        graphics.drawImage(ImageUtils.crop(overlayImage, 0, 0, (int) (32 * factor), (int) (16 * factor)), (int) (32 * factor), 0, null);
        graphics.drawImage(ImageUtils.crop(overlayImage, 16 * factor, 16 * factor, 24 * factor, 16 * factor), (int) (16 * factor), (int) (32 * factor), null);
        // 按基岩版 drowned 几何的右臂、左臂、右腿、左腿分别放置底层和外层。
        this.drownedLimb(newImage, fromImage, 40, 16, 0, 16, false);
        this.drownedLimb(newImage, fromImage, 32, 48, 40, 16, true);
        this.drownedLimb(newImage, fromImage, 0, 16, 16, 48, false);
        this.drownedLimb(newImage, fromImage, 16, 48, 32, 48, true);
        this.drownedLimb(newImage, overlayImage, 40, 16, 48, 48, false);
        this.drownedLimb(newImage, overlayImage, 32, 48, 40, 32, true);
        this.drownedLimb(newImage, overlayImage, 0, 16, 0, 48, false);
        this.drownedLimb(newImage, overlayImage, 16, 48, 0, 32, true);
        graphics.dispose();
        return newImage;
    }

    private void drownedLimb(BufferedImage target,
                             BufferedImage source,
                             int sourceX,
                             int sourceY,
                             int targetX,
                             int targetY,
                             boolean mirror) {
        int side = source.getWidth() / 16;
        int sx = sourceX * source.getWidth() / 64;
        int sy = sourceY * source.getWidth() / 64;
        int dx = targetX * source.getWidth() / 64;
        int dy = targetY * source.getWidth() / 64;
        // 基岩版左肢体启用了 mirror：每个面水平翻转，且交换内外侧面，不能翻转整块 UV。
        // 直接搬运 ARGB，避免 Graphics2D 翻转时预乘 alpha 导致半透明像素的 RGB 舍入。
        for (int y = 0; y < 4 * side; y++) {
            for (int x = 0; x < 4 * side; x++) {
                int face = x / side;
                if (y < side && (face == 0 || face == 3)) {
                    continue;
                }
                int targetPixelX = x;
                if (mirror) {
                    int targetFace = y >= side && (face & 1) == 0 ? 2 - face : face;
                    targetPixelX = targetFace * side + side - 1 - x % side;
                }
                target.setRGB(dx + targetPixelX, dy + y, source.getRGB(sx + x, sy + y));
            }
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
