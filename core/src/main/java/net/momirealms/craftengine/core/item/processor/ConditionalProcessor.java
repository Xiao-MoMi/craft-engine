package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemData;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class ConditionalProcessor implements NetworkItemProcessor {
    public static final ItemProcessorFactory<ConditionalProcessor> FACTORY = new Factory();
    private final Predicate<Context> condition;
    private final ItemProcessor[] modifiers;
    private final NetworkItemProcessor[] networkModifiers;

    public ConditionalProcessor(Predicate<Context> condition, ItemProcessor[] modifiers) {
        this.modifiers = modifiers;
        this.condition = condition;
        NetworkItemProcessor[] networkModifiers = new NetworkItemProcessor[modifiers.length];
        for (int i = 0; i < modifiers.length; i++) {
            if (!(modifiers[i] instanceof NetworkItemProcessor networkProcessor)) {
                networkModifiers = null;
                break;
            }
            networkModifiers[i] = networkProcessor;
        }
        this.networkModifiers = networkModifiers;
    }

    @Override
    public void apply(ItemBuildContext context) {
        Item item = context.item();
        if (this.condition.test(context)) {
            for (ItemProcessor m : this.modifiers) {
                Item applied = item.apply(m, context);
                if (applied != item) {
                    context.setItem(applied);
                    item = applied;
                }
            }
        }
    }

    @Override
    public boolean applyNetworkItem(NetworkItemBuildContext context, @Nullable NetworkItemData networkData) {
        if (this.networkModifiers == null) {
            throw new IllegalStateException("Conditional processor contains a processor that does not support network items");
        }
        if (!this.condition.test(context)) {
            return false;
        }
        boolean changed = false;
        for (NetworkItemProcessor modifier : this.networkModifiers) {
            if (!modifier.shouldSkip(context.source())) {
                changed |= modifier.applyNetworkItem(context, networkData);
            }
        }
        return changed;
    }

    @Nullable
    ItemProcessor unsupportedNetworkProcessor() {
        for (ItemProcessor modifier : this.modifiers) {
            if (!(modifier instanceof NetworkItemProcessor)) {
                return modifier;
            }
            if (modifier instanceof ConditionalProcessor conditional) {
                ItemProcessor unsupported = conditional.unsupportedNetworkProcessor();
                if (unsupported != null) {
                    return unsupported;
                }
            }
        }
        return null;
    }

    private static class Factory implements ItemProcessorFactory<ConditionalProcessor> {

        @Override
        public ConditionalProcessor create(ConfigValue value) {
            ConfigSection section = value.getAsSection();
            List<Condition<Context>> conditions = section.getList(ConfigKeys.of("condition(s)"), CommonConditions::fromConfig);
            List<ItemProcessor> modifiers = new ArrayList<>();
            ItemProcessors.collectProcessors(section.getNonNullSection("data"), modifiers::add);
            return new ConditionalProcessor(MiscUtils.allOf(conditions), modifiers.toArray(new ItemProcessor[0]));
        }
    }
}
