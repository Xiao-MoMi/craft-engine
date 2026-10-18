package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTextureRegistry;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class BedrockAtlasTextures implements BedrockTextureConverter {
    private static final List<Sheet> SHEETS = List.of(
            new Sheet("item/clock_%02d.png", "items/watch_atlas.png", 64, false),
            new Sheet("item/compass_%02d.png", "items/compass_atlas.png", 32, false),
            new Sheet("item/recovery_compass_%02d.png", "items/recovery_compass_atlas.png", 32, false),
            new Sheet("particle/gust_%d.png", "particle/gust.png", 12, true),
            new Sheet("particle/small_gust_%d.png", "particle/small_gust.png", 7, true),
            new Sheet("particle/big_smoke_%d.png", "particle/campfire_smoke.png", 12, false),
            new Sheet("particle/sonic_boom_%d.png", "particle/sonic_explosion.png", 16, false),
            new Sheet("particle/soul_%d.png", "particle/soul.png", 11, false),
            new Sheet("particle/sculk_soul_%d.png", "particle/sculk_soul.png", 11, false));

    @Override
    public void convert(JavaResourcePack pack, Path output, BedrockTextureRegistry textures) {
        for (Sheet sheet : SHEETS) {
            List<String> inputs = new ArrayList<>(sheet.frames());
            for (int index = 0; index < sheet.frames(); index++) {
                inputs.add(String.format(Locale.ROOT, sheet.source(), index));
            }
            BedrockVanillaTextures.transform(pack, output, textures, "textures/" + sheet.target(), inputs,
                    images -> this.stitch(images, sheet.horizontal()));
        }
        this.grid(pack, output, textures, "textures/map/map_icons.png", 4, 4,
                "map/decorations/player.png", "map/decorations/frame.png", "map/decorations/red_marker.png", "map/decorations/blue_marker.png",
                "map/decorations/red_x.png", "map/decorations/target_point.png", "map/decorations/player_off_map.png", null,
                null, null, null, null,
                null, "map/decorations/player_off_limits.png", "map/decorations/woodland_mansion.png", "map/decorations/ocean_monument.png");
        this.grid(pack, output, textures, "textures/environment/moon_phases.png", 4, 2,
                "environment/celestial/moon/full_moon.png", "environment/celestial/moon/waning_gibbous.png",
                "environment/celestial/moon/third_quarter.png", "environment/celestial/moon/waning_crescent.png",
                "environment/celestial/moon/new_moon.png", "environment/celestial/moon/waxing_crescent.png",
                "environment/celestial/moon/first_quarter.png", "environment/celestial/moon/waxing_gibbous.png");
        this.slots(pack, output, textures, "armors_slot_overlay", "helmet", "chestplate", "leggings", "boots");
        this.slots(pack, output, textures, "armors_and_tools_slot_overlay", "helmet", "sword", "chestplate", "pickaxe",
                "leggings", "axe", "boots", "hoe", "spear", "shovel", "horse_armor", "nautilus_armor");
        this.slots(pack, output, textures, "smithing_material_slot_overlay", "ingot", "redstone_dust", "lapis_lazuli", "quartz",
                "diamond", "emerald", "amethyst_shard");
        this.slots(pack, output, textures, "templates_slot_overlay", "smithing_template_netherite_upgrade", "smithing_template_armor_trim");
    }

    private void slots(JavaResourcePack pack, Path output, BedrockTextureRegistry textures, String target, String... names) {
        String[] inputs = new String[names.length];
        for (int index = 0; index < names.length; index++) {
            inputs[index] = "gui/sprites/container/slot/" + names[index] + ".png";
        }
        this.grid(pack, output, textures, "textures/ui/" + target + ".png", names.length, 1, inputs);
    }

    private void grid(JavaResourcePack pack, Path output, BedrockTextureRegistry textures, String target, int columns, int rows, String... paths) {
        List<String> inputs = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        for (int index = 0; index < paths.length; index++) {
            if (paths[index] != null) {
                inputs.add(paths[index]);
                positions.add(index);
            }
        }
        BedrockVanillaTextures.transform(pack, output, textures, target, inputs, images -> {
            int size = images.stream().mapToInt(BufferedImage::getWidth).max().orElseThrow();
            BufferedImage result = new BufferedImage(size * columns, size * rows, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = result.createGraphics();
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
        for (int index = 0; index < images.size(); index++) {
            graphics.drawImage(images.get(index), horizontal ? index * width : 0, horizontal ? 0 : index * height, width, height, null);
        }
        graphics.dispose();
        return result;
    }

    private record Sheet(String source, String target, int frames, boolean horizontal) {
    }
}
