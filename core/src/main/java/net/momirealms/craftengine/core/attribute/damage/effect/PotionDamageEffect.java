package net.momirealms.craftengine.core.attribute.damage.effect;

import net.momirealms.craftengine.core.attribute.damage.DamageEvent;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.LivingEntity;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.util.Key;

import java.util.Locale;

public final class PotionDamageEffect implements DamageEffect {
    private static final String[] POTION_EFFECT = ConfigKeys.of("potion_effect");
    private static final String[] SHOW_ICON = ConfigKeys.of("show_icon");

    public static final DamageEffectFactory<PotionDamageEffect> FACTORY = section -> new PotionDamageEffect(
            Target.fromConfig(section),
            section.getNonNullIdentifier(POTION_EFFECT),
            section.getNumber("duration", ConfigConstants.CONSTANT_TWENTY),
            section.getNumber("amplifier", ConfigConstants.CONSTANT_ZERO),
            section.getBoolean("ambient"),
            section.getBoolean("particles", true),
            section.getBoolean(SHOW_ICON, true)
    );

    private final Target target;
    private final Key potionEffect;
    private final NumberProvider duration;
    private final NumberProvider amplifier;
    private final boolean ambient;
    private final boolean particles;
    private final boolean showIcon;

    public PotionDamageEffect(Target target, Key potionEffect, NumberProvider duration, NumberProvider amplifier,
                              boolean ambient, boolean particles, boolean showIcon) {
        this.target = target;
        this.potionEffect = potionEffect;
        this.duration = duration;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.particles = particles;
        this.showIcon = showIcon;
    }

    @Override
    public void apply(DamageEvent event) {
        if (!(event.finalDamage() > 0)) {
            return;
        }
        Entity entity = this.target.resolve(event);
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        int duration = this.duration.getInt(event.context());
        if (duration <= 0) {
            return;
        }
        living.addPotionEffect(
                this.potionEffect,
                duration,
                Math.max(0, this.amplifier.getInt(event.context())),
                this.ambient,
                this.particles,
                this.showIcon
        );
    }

    public enum Target {
        VICTIM {
            @Override
            Entity resolve(DamageEvent event) {
                return event.victim();
            }
        },
        ATTACKER {
            @Override
            Entity resolve(DamageEvent event) {
                return event.source().causingEntity();
            }
        },
        DIRECT_ENTITY {
            @Override
            Entity resolve(DamageEvent event) {
                return event.source().directEntity();
            }
        };

        abstract Entity resolve(DamageEvent event);

        static Target fromConfig(ConfigSection section) {
            String value = section.getString("target", "victim");
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "victim", "target" -> VICTIM;
                case "attacker", "causing_entity", "causing-entity", "source" -> ATTACKER;
                case "direct", "direct_entity", "direct-entity" -> DIRECT_ENTITY;
                default -> throw new KnownResourceException(
                        "attribute.damage_effect.unknown_target",
                        section.assemblePath("target"),
                        value
                );
            };
        }
    }
}
