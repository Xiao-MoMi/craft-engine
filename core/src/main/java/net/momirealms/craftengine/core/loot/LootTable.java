package net.momirealms.craftengine.core.loot;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.entry.LootEntryContainer;
import net.momirealms.craftengine.core.loot.entry.LootEntryContainers;
import net.momirealms.craftengine.core.loot.function.LootFunction;
import net.momirealms.craftengine.core.loot.function.LootFunctions;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class LootTable implements Loot {
    private final List<LootPool> pools;
    private final List<LootFunction> functions;
    private final BiFunction<Item, LootContext, Item> compositeFunction;

    public LootTable(List<LootPool> pools, List<LootFunction> functions) {
        this.pools = pools;
        this.functions = functions;
        this.compositeFunction = LootFunctions.compose(functions);
    }

    private static final String[] BONUS_ROLLS = new String[]{"bonus_rolls", "bonus-rolls"};

    @NotNull
    public static LootTable fromConfig(@NotNull ConfigSection section) {
        List<LootPool> lootPools = section.getList("pools", v -> {
            ConfigSection innerSection = v.getAsSection();
            NumberProvider rolls = innerSection.getValue("rolls", NumberProviders::fromConfig, ConfigConstants.CONSTANT_ONE);
            NumberProvider bonus_rolls = innerSection.getValue(BONUS_ROLLS, NumberProviders::fromConfig, ConfigConstants.CONSTANT_ZERO);
            List<Condition<LootContext>> conditions = innerSection.getList("conditions", CommonConditions::fromConfig);
            List<LootEntryContainer> containers = innerSection.getList("entries", LootEntryContainers::fromConfig);
            List<LootFunction> functions = innerSection.getList("functions", LootFunctions::fromConfig);
            return new LootPool(containers, conditions, functions, rolls, bonus_rolls);
        });
        return new LootTable(lootPools, section.getList("functions", LootFunctions::fromConfig));
    }

    @Override
    public List<Item> getRandomItems(ContextHolder parameters, World world) {
        return this.getRandomItems(parameters, world, null);
    }

    @Override
    public List<Item> getRandomItems(ContextHolder parameters, World world, @Nullable Player player) {
        return this.getRandomItems(new LootContext(world, player, player == null ? 1f : (float) player.luck(), parameters));
    }

    @Override
    public List<Item> getRandomItems(LootContext context) {
        ArrayList<Item> list = new ArrayList<>();
        this.getRandomItems(context, list::add);
        return list;
    }

    @Override
    public void getRandomItems(LootContext context, Consumer<Item> lootConsumer) {
        this.getRandomItemsRaw(context, createFunctionApplier(createStackSplitter(lootConsumer), context));
    }

    private Consumer<Item> createFunctionApplier(Consumer<Item> lootConsumer, LootContext context) {
        return (item -> {
            for (LootFunction function : this.functions) {
                function.apply(item, context);
            }
            lootConsumer.accept(item);
        });
    }

    private Consumer<Item> createStackSplitter(Consumer<Item> consumer) {
        return (item) -> {
            if (item.count() < item.maxStackSize()) {
                consumer.accept(item);
            } else {
                int remaining = item.count();
                while (remaining > 0) {
                    Item splitItem = item.copyWithCount(Math.min(item.maxStackSize(), remaining));
                    remaining -= splitItem.count();
                    consumer.accept(splitItem);
                }
            }
        };
    }

    public void getRandomItemsRaw(LootContext context, Consumer<Item> lootConsumer) {
        Consumer<Item> consumer = LootFunction.decorate(this.compositeFunction, lootConsumer, context);
        for (LootPool pool : this.pools) {
            pool.addRandomItems(consumer, context);
        }
    }
}
