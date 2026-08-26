package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.CommonFunctions;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.function.Function;

public final class FunctionProcessor implements ItemProcessor {
    public static final ItemProcessorFactory<FunctionProcessor> FACTORY = new Factory();
    private final Function<Context> function;

    public FunctionProcessor(Function<Context> function) {
        this.function = function;
    }

    @Override
    public void apply(ItemBuildContext context) {
        this.function.run(context);
    }

    private static class Factory implements ItemProcessorFactory<FunctionProcessor> {

        @Override
        public FunctionProcessor create(ConfigValue value) {
            return new FunctionProcessor(CommonFunctions.fromConfig(value));
        }
    }
}
