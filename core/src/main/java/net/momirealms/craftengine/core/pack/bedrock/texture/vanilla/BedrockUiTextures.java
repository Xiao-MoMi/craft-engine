package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTextureRegistry;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ImageUtils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

final class BedrockUiTextures implements BedrockTextureConverter {

    @Override
    public void convert(JavaResourcePack pack, Path output, BedrockTextureRegistry textures) {
        List<String> hotbar = List.of("gui/sprites/hud/hotbar.png");
        for (int index = 0; index < 9; index++) {
            int slot = index;
            BedrockVanillaTextures.transform(pack, output, textures, "textures/ui/hotbar_" + index + ".png", hotbar, images -> {
                BufferedImage image = images.getFirst();
                float scale = image.getHeight() / 22f;
                return ImageUtils.crop(image, (slot * 20 + 1) * scale, 0, 20 * scale, image.getHeight());
            });
        }
        BedrockVanillaTextures.transform(pack, output, textures, "textures/ui/hotbar_start_cap.png", hotbar, images -> {
            BufferedImage image = images.getFirst();
            return ImageUtils.crop(image, 0, 0, image.getHeight() / 22f, image.getHeight());
        });
        BedrockVanillaTextures.transform(pack, output, textures, "textures/ui/hotbar_end_cap.png", hotbar, images -> {
            BufferedImage image = images.getFirst();
            float width = image.getHeight() / 22f;
            return ImageUtils.crop(image, image.getWidth() - width, 0, width, image.getHeight());
        });
        BedrockVanillaTextures.transform(pack, output, textures, "textures/ui/selected_hotbar_slot.png",
                List.of("gui/sprites/hud/hotbar_selection.png"), images -> this.selection(images.getFirst()));
        BedrockVanillaTextures.transform(pack, output, textures, "textures/ui/title.png", List.of("gui/title/minecraft.png"), images -> {
            BufferedImage image = images.getFirst();
            return ImageUtils.resize(ImageUtils.crop(image, 0, 0, image.getWidth(), image.getHeight() - (int) (image.getHeight() / 256f * 79)), 1937, 333);
        });
        for (int index = 0; index < 4; index++) {
            BedrockVanillaTextures.transform(pack, output, textures, "textures/ui/locator_bar_dot_" + index + ".png",
                    List.of("gui/sprites/hud/locator_bar_dot/default_" + index + ".png"), images -> {
                        BufferedImage image = images.getFirst();
                        float scale = image.getWidth() / 9f;
                        return ImageUtils.crop(image, scale, scale, 7 * scale, 7 * scale);
                    });
        }
        for (String direction : List.of("up", "down")) {
            BedrockVanillaTextures.transform(pack, output, textures, "textures/ui/locator_arrow_" + direction + ".png",
                    List.of("gui/sprites/hud/locator_bar_arrow_" + direction + ".png"), images -> {
                        BufferedImage image = images.getFirst();
                        float scale = image.getWidth() / 7f;
                        return ImageUtils.crop(image, 0, direction.equals("up") ? scale : 0, image.getWidth(), 4 * scale);
                    });
        }
        this.experience(pack, output, textures, "background", "experiencebarempty");
        this.experience(pack, output, textures, "progress", "experiencebarfull");
    }

    private BufferedImage selection(BufferedImage image) {
        int size = image.getWidth();
        int border = Math.max(1, size / 24);
        BufferedImage result = ImageUtils.resize(image, size, size);
        Graphics2D graphics = result.createGraphics();
        graphics.drawImage(ImageUtils.flip(ImageUtils.crop(image, 0, 0, size, border), false, true), 0, size - border, null);
        graphics.dispose();
        return result;
    }

    private void experience(JavaResourcePack pack, Path output, BedrockTextureRegistry textures, String source, String target) {
        String path = "gui/sprites/hud/experience_bar_" + source + ".png";
        if (!pack.hasCustomResource(BedrockVanillaTextures.resource(path))) {
            return;
        }
        try {
            BufferedImage image = BedrockVanillaTextures.load(pack, path).image();
            BedrockVanillaTextures.writeImage(output, textures, path, "textures/ui/" + target + ".png", image);
            JsonObject definition = new JsonObject();
            JsonArray size = new JsonArray();
            size.add(image.getWidth());
            size.add(image.getHeight());
            definition.add("base_size", size);
            JsonArray slices = new JsonArray();
            slices.add(1);
            slices.add(0);
            slices.add(1);
            slices.add(0);
            definition.add("nineslice_size", slices);
            GsonHelper.writeJsonFile(definition, output.resolve("textures/ui/" + target + ".json"));
            if (source.equals("background")) {
                BedrockVanillaTextures.writeImage(output, textures, path, "textures/ui/experiencenub.png", new BufferedImage(11, 5, BufferedImage.TYPE_INT_ARGB));
            }
        } catch (IOException | IllegalArgumentException exception) {
            BedrockVanillaTextures.warn(path, exception);
        }
    }
}
