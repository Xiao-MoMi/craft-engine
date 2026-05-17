package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.advancement.AdvancementType;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ToastFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final String toast;
    private final java.util.function.Function<Player, Item> icon;
    private final AdvancementType advancementType;

    private ToastFunction(List<Condition<CTX>> predicates,
                          @Nullable PlayerSelector<CTX> selector,
                          String toast,
                          java.util.function.Function<Player, Item> icon,
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

    public static <CTX extends Context> FunctionFactory<CTX, ToastFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, ToastFunction<CTX>> {
        private static final String[] ITEM = new String[] {"item", "icon"};
        private static final String[] TOAST = new String[] {"toast", "message"};
        private static final String[] ADVANCEMENT_TYPE = new String[] {"advancement_type", "advancement-type"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public ToastFunction<CTX> create(ConfigSection section) {
            Key item = section.getNonNullIdentifier(ITEM);
            return new ToastFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    AdventureHelper.legacyToMiniMessage(section.getNonNullString(TOAST)),
                    player -> CraftEngine.instance().itemManager().createWrappedItem(item, player),
                    section.getEnum(ADVANCEMENT_TYPE, AdvancementType.class, AdvancementType.GOAL)
            );
        }
    }
}