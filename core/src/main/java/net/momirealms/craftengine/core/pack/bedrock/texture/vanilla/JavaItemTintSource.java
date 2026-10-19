package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.tint.ConstantTint;
import net.momirealms.craftengine.core.pack.model.definition.tint.GrassTint;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.util.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

final class JavaItemTintSource {
    private static final String GRASS_COLORMAP = "assets/minecraft/textures/colormap/grass.png";
    private final JavaResourcePack pack;
    private int[] grassColormap;

    JavaItemTintSource(JavaResourcePack pack) {
        this.pack = pack;
    }

    TintResult read(Key key, int tintIndex) throws IOException {
        ItemModel definition = this.pack.readItemDefinition(key);
        if (definition == null) {
            return null;
        }
        if (!(definition instanceof BaseItemModel model)) {
            throw new IOException("Expected a static item model for " + key);
        }
        // 是否覆写过了
        boolean changed = this.pack.hasCustomResource(ResourcePackUtils.itemDefinitionPath(key));
        // 没有对应的着色源时，不改变贴图颜色
        if (tintIndex < 0 || tintIndex >= model.tints().size()) {
            return new TintResult(new Color(0xffffff), changed);
        }
        Tint tint = model.tints().get(tintIndex);
        if (tint instanceof ConstantTint constant) {
            int color = constant.value().map(value -> value & 0xffffff, value -> ((int) (value.get(0) * 255) << 16) | ((int) (value.get(1) * 255) << 8) | (int) (value.get(2) * 255));
            return new TintResult(new Color(color), changed);
        }
        if (tint instanceof GrassTint grass) {
            double temperature = grass.temperature();
            double downfall = grass.downfall();
            if (!(temperature >= 0 && temperature <= 1 && downfall >= 0 && downfall <= 1)) {
                throw new IOException("Grass tint temperature and downfall must be between 0 and 1");
            }
            if (this.grassColormap == null) {
                BufferedImage colormap;
                try (InputStream input = this.pack.openResource(GRASS_COLORMAP)) {
                    colormap = input == null ? null : ClientPngReader.read(input);
                }
                if (colormap == null || colormap.getWidth() != 256 || colormap.getHeight() != 256) {
                    throw new IOException("Expected a 256x256 grass colormap");
                }
                this.grassColormap = PngOptimizer.readPixels(colormap);
            }
            downfall *= temperature;
            int x = (int) ((1 - temperature) * 255);
            int y = (int) ((1 - downfall) * 255);
            return new TintResult(new Color(this.grassColormap[(y << 8) | x] & 0xffffff), changed || this.pack.hasCustomResource(GRASS_COLORMAP));
        }
        throw new IOException("Unsupported item tint for " + key + " at index " + tintIndex + ": " + tint.get().get("type").getAsString());
    }

    record TintResult(Color color, boolean changed) {
    }
}
