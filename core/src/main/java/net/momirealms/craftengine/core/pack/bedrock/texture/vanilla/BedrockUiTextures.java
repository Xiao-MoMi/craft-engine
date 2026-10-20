package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.BedrockConversionContext;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaTextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ClientPngReader;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.GuiSpriteUtils;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.UnaryOperator;

final class BedrockUiTextures implements BedrockTextureConverter {

    @Override
    public void convert(BedrockConversionContext context) {
        this.hotbar(context);
        this.writeSprite(
                context,
                "gui/sprites/hud/hotbar_selection.png",
                "textures/ui/selected_hotbar_slot.png",
                24, 23,
                this::selection
        );
        BedrockVanillaTextures.transform(context, "textures/ui/title.png", List.of("gui/title/minecraft.png"), images -> {
            BufferedImage image = images.getFirst();
            return ImageUtils.resize(ImageUtils.crop(image, 0, 0, image.getWidth(), (float) (image.getHeight() * 44) / 64), 1937, 333);
        });
        for (int index = 0; index < 4; index++) {
            this.writeSprite(
                    context,
                    "gui/sprites/hud/locator_bar_dot/default_" + index + ".png",
                    "textures/ui/locator_bar_dot_" + index + ".png",
                    9, 9,
                    image -> {
                        int scale = image.getWidth() / 9;
                        return ImageUtils.crop(image, scale, scale, 7 * scale, 7 * scale);
                    }
            );
        }
        for (String direction : List.of("up", "down")) {
            this.writeSprite(
                    context,
                    "gui/sprites/hud/locator_bar_arrow_" + direction + ".png",
                    "textures/ui/locator_arrow_" + direction + ".png",
                    7, 5,
                    image -> {
                        int scale = image.getWidth() / 7;
                        return ImageUtils.crop(image, 0, direction.equals("up") ? scale : 0, image.getWidth(), 4 * scale);
                    }
            );
        }
        this.experience(context, "background", "experiencebarempty");
        this.experience(context, "progress", "experiencebarfull");
    }

    private void hotbar(BedrockConversionContext context) {
        String path = "gui/sprites/hud/hotbar.png";
        try {
            BufferedImage image = this.sprite(context, path, 182, 22);
            if (image == null) {
                return;
            }
            int scale = image.getHeight() / 22;
            for (int index = 0; index < 9; index++) {
                BedrockVanillaTextures.writeImage(
                        context,
                        path,
                        "textures/ui/hotbar_" + index + ".png",
                        ImageUtils.crop(image, (index * 20 + 1) * scale, 0, 20 * scale, image.getHeight())
                );
            }
            BedrockVanillaTextures.writeImage(
                    context,
                    path,
                    "textures/ui/hotbar_start_cap.png",
                    ImageUtils.crop(image, 0, 0, scale, image.getHeight())
            );
            BedrockVanillaTextures.writeImage(
                    context,
                    path,
                    "textures/ui/hotbar_end_cap.png",
                    ImageUtils.crop(image, image.getWidth() - scale, 0, scale, image.getHeight())
            );
        } catch (IOException | IllegalArgumentException | ArithmeticException exception) {
            CraftEngine.instance().logger().warn("Skipping Bedrock vanilla texture " + path + ": " + exception.getMessage());
        }
    }

    private BufferedImage selection(BufferedImage image) {
        int size = image.getWidth();
        int border = size / 24;
        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        try {
            // 保留 Java 的 24×23 内容，仅用翻转的顶边补齐底部，不拉伸或叠加透明度。
            graphics.drawImage(image, 0, 0, null);
            graphics.drawImage(ImageUtils.flip(ImageUtils.crop(image, 0, 0, size, border), false, true), 0, size - border, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    @Nullable
    private BufferedImage sprite(BedrockConversionContext context, String source, int width, int height) throws IOException {
        String path = ResourcePackUtils.vanillaTexturePath(source);
        if (!context.pack().hasCustomResource(path) && !context.pack().hasCustomResource(path + ".mcmeta")) {
            return null;
        }
        JsonObject metadata = context.pack().readResourceMetadata(path);
        try (InputStream input = context.pack().openResource(path)) {
            if (input == null) {
                throw new IOException("Missing texture " + path);
            }
            TextureData data = JavaTextureAnimation.read(ClientPngReader.read(input), metadata,
                    context.pack().version().isAtOrAbove(MinecraftVersion.V1_21_11));
            if (data.animation() != null) {
                CraftEngine.instance().logger().warn("Exporting first frame of Bedrock UI texture " + source + ": UI animations are not supported");
            }
            return GuiSpriteUtils.render(data.image(), metadata, width, height);
        }
    }

    private void writeSprite(BedrockConversionContext context, String source, String target, int width, int height,
                             UnaryOperator<BufferedImage> transform) {
        try {
            BufferedImage image = this.sprite(context, source, width, height);
            if (image != null) {
                BedrockVanillaTextures.writeImage(context, source, target, transform.apply(image));
            }
        } catch (IOException | IllegalArgumentException | ArithmeticException exception) {
            CraftEngine.instance().logger().warn("Skipping Bedrock vanilla texture " + source + ": " + exception.getMessage());
        }
    }

    private void experience(BedrockConversionContext context, String source, String target) {
        String path = "gui/sprites/hud/experience_bar_" + source + ".png";
        try {
            BufferedImage image = this.sprite(context, path, 182, 5);
            if (image == null || !BedrockVanillaTextures.writeImage(context, path, "textures/ui/" + target + ".png", image)) {
                return;
            }
            JsonObject definition = new JsonObject();
            JsonArray size = new JsonArray();
            size.add(182);
            size.add(5);
            definition.add("base_size", size);
            JsonArray slices = new JsonArray();
            slices.add(1);
            slices.add(0);
            slices.add(1);
            slices.add(0);
            definition.add("nineslice_size", slices);
            GsonHelper.writeJsonFile(definition, context.root().resolve("textures/ui/" + target + ".json"));
            if (source.equals("background")) {
                BedrockVanillaTextures.writeImage(context, path, "textures/ui/experiencenub.png", new BufferedImage(11, 5, BufferedImage.TYPE_INT_ARGB));
            }
        } catch (IOException | IllegalArgumentException | ArithmeticException exception) {
            CraftEngine.instance().logger().warn("Skipping Bedrock vanilla texture " + path + ": " + exception.getMessage());
        }
    }
}
