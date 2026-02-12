package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public enum FurnitureDebugStickState {
    VARIANT(f -> f.currentVariant().name(), new VariantHandler()),
    X_0_1(f -> String.format("%.2f", f.position().x), new XPositionHandler(0.1)),
    X_0_0_1(f -> String.format("%.2f", f.position().x), new XPositionHandler(0.01)),
    Y_0_1(f -> String.format("%.2f", f.position().y), new YPositionHandler(0.1)),
    Y_0_0_1(f -> String.format("%.2f", f.position().y), new YPositionHandler(0.01)),
    Z_0_1(f -> String.format("%.2f", f.position().z), new ZPositionHandler(0.1)),
    Z_0_0_1(f -> String.format("%.2f", f.position().z), new ZPositionHandler(0.01)),
    ROTATION_90(f -> String.format("%.2f", f.position().yRot()), new RotateHandler(90)),
    ROTATION_45(f -> String.format("%.2f", f.position().yRot()), new RotateHandler(45)),
    ROTATION_22_5(f -> String.format("%.2f", f.position().yRot()), new RotateHandler(22.5f)),
    ROTATION_15(f -> String.format("%.2f", f.position().yRot()), new RotateHandler(15)),
    ROTATION_5(f -> String.format("%.2f", f.position().yRot()), new RotateHandler(5)),
    ROTATION_1(f -> String.format("%.2f", f.position().yRot()), new RotateHandler(1)),
    ROTATION_0_5(f -> String.format("%.2f", f.position().yRot()), new RotateHandler(0.5f));

    public static final FurnitureDebugStickState[] VALUES = values();
    private final Function<Furniture, String> formatter;
    private final DebugStickHandler handler;

    FurnitureDebugStickState(Function<Furniture, String> formatter, DebugStickHandler handler) {
        this.formatter = formatter;
        this.handler = handler;
    }

    public DebugStickHandler handler() {
        return handler;
    }

    public FurnitureDebugStickState next() {
        int ordinal = this.ordinal();
        if (ordinal == VALUES.length - 1) {
            return VALUES[0]; // 循环到第一个
        }
        return VALUES[ordinal + 1];
    }

    public FurnitureDebugStickState previous() {
        int ordinal = this.ordinal();
        if (ordinal == 0) {
            return VALUES[VALUES.length - 1]; // 循环到最后一个
        }
        return VALUES[ordinal - 1];
    }

    public String format(Furniture furniture) {
        return this.formatter.apply(furniture);
    }

    public interface DebugStickHandler {

        void onInteract(boolean isSecondaryUsage, Furniture furniture, BiConsumer<String, String> changed, Runnable noChange);
    }

    public static class VariantHandler implements DebugStickHandler {

        @Override
        public void onInteract(boolean isSecondaryUsage, Furniture furniture, BiConsumer<String, String> changed, Runnable noChange) {
            List<String> variants = new ArrayList<>(furniture.config.variants().keySet());
            if (variants.size() == 1) {
                noChange.run();
            } else {
                String variantName = furniture.currentVariant().name();
                int index;
                if (isSecondaryUsage) {
                    index = variants.indexOf(variantName) - 1;
                    if (index < 0) {
                        index = variants.size() - 1;
                    }
                } else {
                    index = variants.indexOf(variantName) + 1;
                    if (index >= variants.size()) {
                        index = 0;
                    }
                }
                if (furniture.setVariant(variants.get(index), true)) {
                    changed.accept("variant", variants.get(index));
                }
            }
        }
    }

    public static class RotateHandler implements DebugStickHandler {
        private final float angle;

        public RotateHandler(float angle) {
            this.angle = angle;
        }

        @Override
        public void onInteract(boolean isSecondaryUsage, Furniture furniture, BiConsumer<String, String> changed, Runnable noChange) {
            WorldPosition position = furniture.position();
            float yaw = isSecondaryUsage ? position.yRot() - angle : position.yRot() + angle;
            furniture.moveTo(new WorldPosition(position.world, position.x, position.y, position.z, position.xRot, yaw), true).thenAccept(success -> {
                if (success) {
                    changed.accept("rotation", String.format("%.2f", yaw));
                }
            });
        }
    }

    public static class XPositionHandler implements DebugStickHandler {
        private final double delta;

        public XPositionHandler(double delta) {
            this.delta = delta;
        }

        @Override
        public void onInteract(boolean isSecondaryUsage, Furniture furniture, BiConsumer<String, String> changed, Runnable noChange) {
            WorldPosition position = furniture.position();
            double x = isSecondaryUsage ? position.x - delta : position.x + delta;
            furniture.moveTo(new WorldPosition(position.world, x, position.y, position.z, position.xRot, position.yRot), true).thenAccept(success -> {
                if (success) {
                    changed.accept("x", String.format("%.2f", x));
                }
            });
        }
    }

    public static class YPositionHandler implements DebugStickHandler {
        private final double delta;

        public YPositionHandler(double delta) {
            this.delta = delta;
        }

        @Override
        public void onInteract(boolean isSecondaryUsage, Furniture furniture, BiConsumer<String, String> changed, Runnable noChange) {
            WorldPosition position = furniture.position();
            double y = isSecondaryUsage ? position.y - delta : position.y + delta;
            furniture.moveTo(new WorldPosition(position.world, position.x, y, position.z, position.xRot, position.yRot), true).thenAccept(success -> {
                if (success) {
                    changed.accept("y", String.format("%.2f", y));
                }
            });
        }
    }

    public static class ZPositionHandler implements DebugStickHandler {
        private final double delta;

        public ZPositionHandler(double delta) {
            this.delta = delta;
        }

        @Override
        public void onInteract(boolean isSecondaryUsage, Furniture furniture, BiConsumer<String, String> changed, Runnable noChange) {
            WorldPosition position = furniture.position();
            double z = isSecondaryUsage ? position.z - delta : position.z + delta;
            furniture.moveTo(new WorldPosition(position.world, position.x, position.y, z, position.xRot, position.yRot), true).thenAccept(success -> {
                if (success) {
                    changed.accept("z", String.format("%.2f", z));
                }
            });
        }
    }
}