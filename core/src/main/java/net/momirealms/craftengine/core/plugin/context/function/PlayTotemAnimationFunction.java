package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class PlayTotemAnimationFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final Key item;
    @Nullable
    private final Key sound;
    private final NumberProvider volume;
    private final NumberProvider pitch;
    private final boolean silent;

    private PlayTotemAnimationFunction(List<Condition<CTX>> predicates,
                                       PlayerSelector<CTX> selector,
                                       Key item,
                                       @Nullable Key sound,
                                       NumberProvider volume,
                                       NumberProvider pitch,
                                       boolean silent) {
        super(predicates);
        this.selector = selector;
        this.item = item;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.silent = silent;
    }

    @Override
    protected void runInternal(CTX ctx) {
        ItemDefinition itemDefinition = CraftEngine.instance().itemManager().getItemDefinition(this.item).orElse(null);
        if (itemDefinition == null) {
            return;
        }
        SoundData soundData = null;
        if (this.sound != null) {
            soundData = SoundData.of(
                    this.sound,
                    SoundData.SoundValue.fixed(Math.max(this.volume.getFloat(ctx), 0f)),
                    SoundData.SoundValue.fixed(MiscUtils.clamp(this.pitch.getFloat(ctx), 0f, 2f))
            );
        }
        for (Player player : this.selector.get(ctx)) {
            Item buildItem = itemDefinition.buildItem(player);
            if (VersionHelper.isOrAbove1_21_2) {
                buildItem.setJavaComponent(DataComponentKeys.DEATH_PROTECTION, Map.of());
            }
            player.sendTotemAnimation(buildItem, soundData, this.silent);
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, PlayTotemAnimationFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, PlayTotemAnimationFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public PlayTotemAnimationFunction<CTX> create(ConfigSection section) {
            return new PlayTotemAnimationFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    section.getNonNullIdentifier("item"),
                    section.getIdentifier("sound"),
                    section.getNumber("volume", ConfigConstants.CONSTANT_ONE),
                    section.getNumber("pitch", ConfigConstants.CONSTANT_ONE),
                    section.getBoolean("silent")
            );
        }
    }
}