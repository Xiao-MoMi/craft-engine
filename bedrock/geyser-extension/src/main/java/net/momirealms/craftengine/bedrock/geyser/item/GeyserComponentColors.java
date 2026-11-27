package net.momirealms.craftengine.bedrock.geyser.item;

import org.geysermc.geyser.inventory.item.Potion;
import org.geysermc.geyser.level.EffectType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Fireworks;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectDetails;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectInstance;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.PotionContents;

final class GeyserComponentColors {

    private static final int[] EFFECT_COLORS = new int[Effect.values().length];

    static {
        for (Effect effect : Effect.values()) {
            EFFECT_COLORS[effect.ordinal()] = EffectType.fromJavaEffect(effect).getColor();
        }
    }

    private GeyserComponentColors() {
    }

    static int firework(Fireworks.FireworkExplosion explosion) {
        if (explosion == null || explosion.getColors().length == 0) {
            return -1;
        }
        int[] colors = explosion.getColors();
        if (colors.length == 1) {
            return colors[0] & 0xffffff;
        }
        ColorMix mix = new ColorMix();
        for (int color : colors) {
            mix.add(color, 1);
        }
        return mix.rgb();
    }

    static int potion(PotionContents contents) {
        if (contents == null) {
            return -1;
        }
        if (contents.getCustomColor() != -1) {
            return contents.getCustomColor() & 0xffffff;
        }
        Potion base = Potion.getByJavaId(contents.getPotionId());
        int weight = baseWeight(base);
        if (contents.getCustomEffects().isEmpty()) {
            return weight == 0 ? -1 : base.getJavaColor();
        }

        ColorMix mix = new ColorMix();
        // 神龟药水有两种效果，必须先还原各自的权重；直接混合已取整的药水颜色会产生色差。
        if (base == Potion.TURTLE_MASTER || base == Potion.LONG_TURTLE_MASTER || base == Potion.STRONG_TURTLE_MASTER) {
            int slowness = base == Potion.STRONG_TURTLE_MASTER ? 6 : 4;
            mix.add(EffectType.SLOWNESS.getColor(), slowness);
            mix.add(EffectType.RESISTANCE.getColor(), weight - slowness);
        } else if (weight > 0) {
            mix.add(base.getJavaColor(), weight);
        }
        for (MobEffectInstance effect : contents.getCustomEffects()) {
            MobEffectDetails details = effect.getDetails();
            if (details.isShowParticles()) {
                Effect type = effect.getEffect();
                int color = type == null ? EffectType.NONE.getColor() : EFFECT_COLORS[type.ordinal()];
                mix.add(color, details.getAmplifier() + 1);
            }
        }
        return mix.rgb();
    }

    private static int baseWeight(Potion base) {
        return switch (base) {
            case null -> 0;
            case WATER, MUNDANE, THICK, AWKWARD -> 0;
            case STRONG_LEAPING, STRONG_SWIFTNESS, STRONG_HEALING, STRONG_HARMING, STRONG_POISON, STRONG_REGENERATION, STRONG_STRENGTH -> 2;
            case STRONG_SLOWNESS -> 4;
            case TURTLE_MASTER, LONG_TURTLE_MASTER -> 7;
            case STRONG_TURTLE_MASTER -> 10;
            default -> 1;
        };
    }

    private static final class ColorMix {
        private int red;
        private int green;
        private int blue;
        private int weight;

        private void add(int rgb, int weight) {
            this.red += (rgb >> 16 & 255) * weight;
            this.green += (rgb >> 8 & 255) * weight;
            this.blue += (rgb & 255) * weight;
            this.weight += weight;
        }

        private int rgb() {
            return this.weight == 0 ? -1 : (this.red / this.weight << 16) | (this.green / this.weight << 8) | (this.blue / this.weight);
        }
    }
}
