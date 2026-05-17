package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Optional;

public final class ClearItemFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final Key itemId;
    private final NumberProvider count;

    private ClearItemFunction(List<Condition<CTX>> predicates,
                              Key itemId,
                              NumberProvider count) {
        super(predicates);
        this.itemId = itemId;
        this.count = count;
    }

    @Override
    protected void runInternal(CTX ctx) {
        Optional<Player> optionalPlayer = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
        if (optionalPlayer.isEmpty()) {
            return;
        }
        Player player = optionalPlayer.get();
        player.clearOrCountMatchingInventoryItems(itemId, count.getInt(ctx));
    }

    public static <CTX extends Context> FunctionFactory<CTX, ClearItemFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, ClearItemFunction<CTX>> {
        private static final String[] ID = new String[]{"item", "id"};
        private static final String[] COUNT = new String[]{"count", "amount"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public ClearItemFunction<CTX> create(ConfigSection section) {
            return new ClearItemFunction<>(
                    getPredicates(section),
                    section.getNonNullIdentifier(ID),
                    section.getNumber(COUNT, ConfigConstants.CONSTANT_ONE)
            );
        }
    }
}