package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.BedrockConversionContext;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ClientPngReader;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class BedrockAtlasTextures implements BedrockTextureConverter {
    private static final List<Sheet> SHEETS = List.of(
            new Sheet("item/clock_%02d.png", "items/watch_atlas.png", 64, false),
            new Sheet("item/compass_%02d.png", "items/compass_atlas.png", 32, false),
            new Sheet("item/compass_%02d.png", "items/lodestonecompass_atlas.png", 32, false),
            new Sheet("item/recovery_compass_%02d.png", "items/recovery_compass_atlas.png", 32, false)
    );
    private static final JsonObject SMITHING_UI = GsonHelper.readJsonFromResource("/bedrock/atlas/ui/smithing_table_2_screen.json").getAsJsonObject();

    @Override
    public void convert(BedrockConversionContext context) {
        for (Sheet sheet : SHEETS) {
            List<String> inputs = new ArrayList<>(sheet.frames());
            for (int index = 0; index < sheet.frames(); index++) {
                inputs.add(String.format(Locale.ROOT, sheet.source(), index));
            }
            BedrockVanillaTextures.transform(
                    context,
                    "textures/" + sheet.target(),
                    inputs,
                    images -> this.stitch(images, sheet.horizontal())
            );
        }
        this.grid(
                context,
                "textures/map/map_icons.png", 4, 4,
                "/bedrock/atlas/textures/map/map_icons.png",
                "map/decorations/player.png",
                "map/decorations/frame.png",
                "map/decorations/red_marker.png",
                "map/decorations/blue_marker.png",
                "map/decorations/red_x.png",
                "map/decorations/target_point.png",
                "map/decorations/player_off_map.png",
                null,
                null,
                null,
                null,
                null,
                null,
                "map/decorations/player_off_limits.png",
                "map/decorations/woodland_mansion.png",
                "map/decorations/ocean_monument.png"
        );
        this.grid(
                context,
                "textures/environment/moon_phases.png", 4, 2,
                null,
                "environment/celestial/moon/full_moon.png",
                "environment/celestial/moon/waning_gibbous.png",
                "environment/celestial/moon/third_quarter.png",
                "environment/celestial/moon/waning_crescent.png",
                "environment/celestial/moon/new_moon.png",
                "environment/celestial/moon/waxing_crescent.png",
                "environment/celestial/moon/first_quarter.png",
                "environment/celestial/moon/waxing_gibbous.png"
        );
        JsonObject ui = SMITHING_UI.deepCopy();
        boolean uiChanged = this.slots(
                context,
                ui, "smithing_input_image_armors",
                "armors_slot_overlay",
                "helmet",
                "chestplate",
                "leggings",
                "boots"
        );
        uiChanged |= this.slots(
                context,
                ui, "smithing_input_image_armors_and_tools",
                "armors_and_tools_slot_overlay",
                "helmet",
                "sword",
                "chestplate",
                "pickaxe",
                "leggings",
                "axe",
                "boots",
                "hoe",
                "spear",
                "shovel",
                "horse_armor",
                "nautilus_armor"
        );
        uiChanged |= this.slots(
                context,
                ui, "smithing_material_image_all",
                "smithing_material_slot_overlay",
                "ingot",
                "redstone_dust",
                "lapis_lazuli",
                "quartz",
                "diamond",
                "emerald",
                "amethyst_shard"
        );
        uiChanged |= this.slots(
                context,
                ui, "smithing_input_image_templates",
                "templates_slot_overlay",
                "smithing_template_netherite_upgrade",
                "smithing_template_armor_trim"
        );
        if (uiChanged) {
            try {
                Path file = context.root().resolve("ui/smithing_table_2_screen.json");
                Files.createDirectories(file.getParent());
                GsonHelper.writeJsonFile(ui, file);
            } catch (IOException exception) {
                CraftEngine.instance().logger().warn("Could not write Bedrock smithing UI: " + exception.getMessage());
            }
        }
    }

    private boolean slots(BedrockConversionContext context, JsonObject ui, String control, String target, String... names) {
        boolean changed = false;
        for (String name : names) {
            if (context.pack().hasCustomResource(ResourcePackUtils.vanillaTexturePath("gui/sprites/container/slot/" + name + ".png"))) {
                changed = true;
                break;
            }
        }
        if (!changed) {
            return false;
        }
        try {
            List<BufferedImage> inputs = new ArrayList<>(names.length);
            for (String name : names) {
                TextureData data = BedrockVanillaTextures.load(context, "gui/sprites/container/slot/" + name + ".png");
                if (data.animation() != null) {
                    CraftEngine.instance().logger().warn("Exporting first frame of animated slot sprite " + name);
                }
                inputs.add(data.image());
            }
            BufferedImage image = this.stitch(inputs, true);
            if (!BedrockVanillaTextures.writeImage(context, target, "textures/ui/" + target + ".png", image)) {
                return false;
            }
            // UI 的 uv_size 使用像素尺寸，必须与高清图集的每格宽高同步。
            JsonArray size = new JsonArray();
            size.add(image.getWidth() / names.length);
            size.add(image.getHeight());
            ui.getAsJsonObject(control + "@smithing_table_2.smithing_image").add("uv_size", size);
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            CraftEngine.instance().logger().warn("Skipping Bedrock slot atlas " + target + ": " + exception.getMessage());
            return false;
        }
    }

    private void grid(BedrockConversionContext context, String target, int columns, int rows, @Nullable String background, String... paths) {
        List<String> inputs = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        for (int index = 0; index < paths.length; index++) {
            if (paths[index] != null) {
                inputs.add(paths[index]);
                positions.add(index);
            }
        }
        BedrockVanillaTextures.transform(context, target, inputs, images -> {
            int size = images.stream().mapToInt(BufferedImage::getWidth).max().orElseThrow();
            BufferedImage result = new BufferedImage(size * columns, size * rows, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = result.createGraphics();
            graphics.setComposite(AlphaComposite.Src);
            if (background != null) {
                // Java 没有对应资源的格子保留基岩版底图；有对应资源的格子连透明像素一起覆盖。
                try (InputStream input = BedrockAtlasTextures.class.getResourceAsStream(background)) {
                    graphics.drawImage(ClientPngReader.read(input), 0, 0, result.getWidth(), result.getHeight(), null);
                } catch (IOException exception) {
                    graphics.dispose();
                    throw new IllegalArgumentException("Cannot read atlas background " + background, exception);
                }
            }
            for (int index = 0; index < images.size(); index++) {
                int position = positions.get(index);
                graphics.drawImage(images.get(index), position % columns * size, position / columns * size, size, size, null);
            }
            graphics.dispose();
            return result;
        });
    }

    private BufferedImage stitch(List<BufferedImage> images, boolean horizontal) {
        int width = 0;
        int height = 0;
        for (BufferedImage image : images) {
            width = Math.max(width, image.getWidth());
            height = Math.max(height, image.getHeight());
        }
        BufferedImage result = new BufferedImage(horizontal ? width * images.size() : width, horizontal ? height : height * images.size(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        for (int index = 0; index < images.size(); index++) {
            graphics.drawImage(images.get(index), horizontal ? index * width : 0, horizontal ? 0 : index * height, width, height, null);
        }
        graphics.dispose();
        return result;
    }

    private record Sheet(String source, String target, int frames, boolean horizontal) {
    }
}
