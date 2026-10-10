package net.momirealms.craftengine.core.block.entity.render;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.world.BlockPos;
import org.jetbrains.annotations.Nullable;

public record BlockEntityRenderOffset(Type type, double horizontal, double vertical) {
    public static final BlockEntityRenderOffset NONE = new BlockEntityRenderOffset(Type.NONE, 0, 0);

    public static BlockEntityRenderOffset fromConfig(@Nullable ConfigSection section) {
        if (section == null) {
            return NONE;
        }
        Type type = section.getEnum("type", Type.class, Type.NONE);
        if (type == Type.NONE) {
            return NONE;
        }
        return new BlockEntityRenderOffset(type,
                section.getValue("horizontal", value -> value.getAsDouble(0), 0.25),
                section.getValue("vertical", value -> value.getAsDouble(0), 0.2));
    }

    public long seed(BlockPos pos) {
        if (this.type == Type.NONE) {
            return 0;
        }
        // Match Minecraft's coordinate hash, including int overflow in the X product.
        // Ignoring Y keeps vertically aligned elements on the same offset.
        long seed = (long) (pos.x() * 3129871) ^ (long) pos.z() * 116129781L;
        seed = seed * seed * 42317861L + seed * 11L;
        return seed >> 16;
    }

    public double x(long seed) {
        return this.type == Type.NONE ? 0 : ((float) (seed & 15L) / 15.0F - 0.5) * 2 * this.horizontal;
    }

    public double y(long seed) {
        return this.type == Type.XYZ ? ((float) (seed >> 4 & 15L) / 15.0F - 1.0) * this.vertical : 0;
    }

    public double z(long seed) {
        return this.type == Type.NONE ? 0 : ((float) (seed >> 8 & 15L) / 15.0F - 0.5) * 2 * this.horizontal;
    }

    public enum Type {
        NONE,
        XZ,
        XYZ
    }
}
