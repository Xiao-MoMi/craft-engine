package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.advancement.AdvancementType;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectors;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.EnumUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ToastFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final String toast;
    private final java.util.function.Function<Player, Item<?>> icon;
    private final AdvancementType advancementType;

    public ToastFunction(List<Condition<CTX>> predicates,
                         @Nullable PlayerSelector<CTX> selector,
                         String toast,
                         java.util.function.Function<Player, Item<?>> icon,
                         AdvancementType advancementType) {
        super(predicates);
        this.selector = selector;
        this.toast = toast;
        this.icon = icon;
        this.advancementType = advancementType;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> it.sendToast(AdventureHelper.miniMessage().deserialize(this.toast, ctx.tagResolvers()), this.icon.apply(it), this.advancementType));
        } else {
            for (Player viewer : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(viewer));
                viewer.sendToast(AdventureHelper.miniMessage().deserialize(this.toast, relationalContext.tagResolvers()), this.icon.apply(viewer), this.advancementType);
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, ToastFunction<CTX>> factory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, ToastFunction<CTX>> {

        public Factory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public ToastFunction<CTX> create(Map<String, Object> arguments) {
            AdvancementType advancementType;
            String advancementName = arguments.getOrDefault("advancement-type", "goal").toString();
            try {
                advancementType = AdvancementType.valueOf(advancementName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new LocalizedResourceConfigException("warning.config.function.toast.invalid_advancement_type", advancementName, EnumUtils.toString(AdvancementType.values()));
            }
            String toast = AdventureHelper.legacyToMiniMessage(ResourceConfigUtils.requireNonEmptyStringOrThrow(ResourceConfigUtils.get(arguments, "toast", "message"), "warning.config.function.toast.missing_toast"));
            Key item = Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(ResourceConfigUtils.get(arguments, "item", "icon"), "warning.config.function.toast.missing_icon"));
            return new ToastFunction<>(
                    getPredicates(arguments),
                    PlayerSelectors.fromObject(arguments.get("target"), conditionFactory()),
                    toast,
                    player -> CraftEngine.instance().itemManager().createWrappedItem(item, player),
                    advancementType
            );
        }
    }
}