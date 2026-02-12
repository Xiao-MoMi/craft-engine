package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectors;
import net.momirealms.craftengine.core.plugin.gui.GuiType;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.EnumUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class OpenWindowFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final GuiType guiType;
    private final String optionalTitle;

    public OpenWindowFunction(List<Condition<CTX>> predicates, @Nullable PlayerSelector<CTX> selector, GuiType guiType, String optionalTitle) {
        super(predicates);
        this.selector = selector;
        this.guiType = guiType;
        this.optionalTitle = optionalTitle;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> {
                CraftEngine.instance().guiManager().openInventory(it, this.guiType);
                if (this.optionalTitle != null) {
                    CraftEngine.instance().guiManager().updateInventoryTitle(it, AdventureHelper.miniMessage().deserialize(this.optionalTitle, ctx.tagResolvers()));
                }
            });
        } else {
            for (Player viewer : this.selector.get(ctx)) {
                CraftEngine.instance().guiManager().openInventory(viewer, this.guiType);
                if (this.optionalTitle != null) {
                    RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(viewer));
                    CraftEngine.instance().guiManager().updateInventoryTitle(viewer, AdventureHelper.miniMessage().deserialize(this.optionalTitle, relationalContext.tagResolvers()));
                }
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, OpenWindowFunction<CTX>> factory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, OpenWindowFunction<CTX>> {

        public Factory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public OpenWindowFunction<CTX> create(Map<String, Object> arguments) {
            String title = Optional.ofNullable(arguments.get("title")).map(String::valueOf).orElse(null);
            String rawType = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("gui-type"), "warning.config.function.open_window.missing_gui_type");
            try {
                GuiType type = GuiType.valueOf(rawType.toUpperCase(Locale.ENGLISH));
                return new OpenWindowFunction<>(getPredicates(arguments), PlayerSelectors.fromObject(arguments.get("target"), conditionFactory()), type, title == null ? null : AdventureHelper.legacyToMiniMessage(title));
            } catch (IllegalArgumentException e) {
                throw new LocalizedResourceConfigException("warning.config.function.open_window.invalid_gui_type", e, rawType, EnumUtils.toString(GuiType.values()));
            }
        }
    }
}