package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.Nullable;

public record ItemBehaviorDescription(Use use,
                                      Swing attack,
                                      Swing interact,
                                      @Nullable Kinetic kinetic) {

    public ItemBehaviorDescription(Use use, Swing attack, Swing interact) {
        this(use, attack, interact, null);
    }

    public record Kinetic(int delayTicks,
                          float forwardMovement,
                          @Nullable KineticCondition dismount,
                          @Nullable KineticCondition knockback,
                          @Nullable KineticCondition damage) {
    }

    public record KineticCondition(int durationTicks, float minimumSpeed, float minimumRelativeSpeed) {
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
}
