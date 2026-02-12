package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import com.mojang.datafixers.DSL.TypeReference;

public final class MReferences {
    private MReferences() {}

    public static final TypeReference ITEM_STACK = reference("item_stack");

    public static TypeReference reference(final String name) {
        return new TypeReference() {
            @Override
            public String typeName() {
                return name;
            }

            @Override
            public String toString() {
                return "@" + name;
            }
        };
    }
}
