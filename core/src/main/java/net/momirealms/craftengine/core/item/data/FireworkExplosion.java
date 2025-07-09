package net.momirealms.craftengine.core.item.data;

import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public record FireworkExplosion(Shape shape, IntList colors, IntList fadeColors, boolean hasTrail, boolean hasTwinkle) {

    public enum Shape {
        SMALL_BALL(0, "small_ball"),
        LARGE_BALL(1, "large_ball"),
        STAR(2, "star"),
        CREEPER(3, "creeper"),
        BURST(4, "burst");

        private final int id;
        private final String name;

        Shape(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int id() {
            return id;
        }

        @NotNull
        public String getName() {
            return name;
        }

        private static final Shape[] BY_ID = values();
        private static final Map<String, Shape> BY_NAME = new HashMap<>();

        static {
            for (Shape shape : BY_ID) {
                BY_NAME.put(shape.getName(), shape);
                BY_NAME.put(shape.name, shape);
            }
        }

        public static Shape byName(String name) {
            return BY_NAME.get(name);
        }

        public static Shape byId(int id) {
            return BY_ID[id];
        }
    }
}
