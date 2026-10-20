package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public record ItemBehaviorDescription(Use use,
                                      Swing attack,
                                      Swing interact,
                                      Map<Key, Component> components) {

    public ItemBehaviorDescription {
        components = Map.copyOf(components);
    }

    public enum UseAnimation {
        NONE,
        EAT,
        DRINK,
        BLOCK,
        BOW,
        TRIDENT,
        CROSSBOW,
        SPYGLASS,
        TOOT_HORN,
        BRUSH,
        BUNDLE,
        SPEAR;

        public static UseAnimation fromNative(String name, MinecraftVersion version) {
            if (name.equals("SPEAR") && version.isBelow(MinecraftVersion.V1_21_11)) {
                return TRIDENT;
            }
            return valueOf(name);
        }
    }

    public enum SwingAnimation {
        NONE,
        WHACK,
        STAB
    }

    public enum ComponentSource {
        DEFAULT,
        OVERRIDE,
        REMOVED,
        ABSENT
    }

    public record Use(UseAnimation animation,
                      Ticks maximumDuration,
                      @Nullable Ticks chargeDuration,
                      boolean nativeUseOnRelease) {
    }

    public record Swing(SwingAnimation animation, int baseDurationTicks) {
        public static final Swing LEGACY = new Swing(SwingAnimation.WHACK, 6);
    }

    public record Ticks(int value) {
    }

    public record Component(ComponentSource source, @Nullable JsonElement value) {

        public static Component of(@Nullable JsonElement effective, @Nullable JsonElement defaultValue) {
            ComponentSource source;
            if (Objects.equals(effective, defaultValue)) {
                source = effective == null ? ComponentSource.ABSENT : ComponentSource.DEFAULT;
            } else {
                source = effective == null ? ComponentSource.REMOVED : ComponentSource.OVERRIDE;
            }
            return new Component(source, effective);
        }
    }
}
