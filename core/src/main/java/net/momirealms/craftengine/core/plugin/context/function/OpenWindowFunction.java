package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.gui.GuiType;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class OpenWindowFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final GuiType guiType;
    private final String optionalTitle;

    private OpenWindowFunction(List<Condition<CTX>> predicates,
                               @Nullable PlayerSelector<CTX> selector,
                               GuiType guiType,
                               String optionalTitle) {
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

    public static <CTX extends Context> FunctionFactory<CTX, OpenWindowFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, OpenWindowFunction<CTX>> {
        private static final String[] GUI_TYPE = new String[] {"gui_type", "gui-type"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public OpenWindowFunction<CTX> create(ConfigSection section) {
            String title = section.getString("title");
            return new OpenWindowFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    section.getNonNullEnum(GUI_TYPE, GuiType.class),
                    title == null ? null : AdventureHelper.legacyToMiniMessage(title)
            );
        }
    }
}