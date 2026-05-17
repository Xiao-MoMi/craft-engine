package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.Loot;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.List;
import java.util.Optional;

public final class DropLootFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final Loot loot;
    private final boolean toInv;

    private DropLootFunction(List<Condition<CTX>> predicates,
                             NumberProvider x,
                             NumberProvider y,
                             NumberProvider z,
                             Loot loot,
                             boolean toInv) {
        super(predicates);
        this.x = x;
        this.y = y;
        this.z = z;
        this.loot = loot;
        this.toInv = toInv;
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<WorldPosition> optionalWorldPosition = ctx.getOptionalParameter(DirectContextParameters.POSITION);
        if (optionalWorldPosition.isPresent()) {
            World world = optionalWorldPosition.get().world();
            WorldPosition position = new WorldPosition(world, x.getDouble(ctx), y.getDouble(ctx), z.getDouble(ctx));
            Player player = ctx.getOptionalParameter(DirectContextParameters.PLAYER).orElse(null);
            List<? extends Item> items = loot.getRandomItems(ctx.contexts(), world, player);
            if (this.toInv && player != null) {
                for (Item item : items) {
                    player.giveItem(item, true);
                }
            } else {
                for (Item item : items) {
                    world.dropItemNaturally(position, item);
                }
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, DropLootFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, DropLootFunction<CTX>> {
        private static final String[] LOOT = new String[] {"loot", "loots"};
        private static final String[] TO_INVENTORY = new String[] {"to_inventory", "to-inventory"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public DropLootFunction<CTX> create(ConfigSection section) {
            return new DropLootFunction<>(
                    getPredicates(section),
                    section.getNumber("x", ConfigConstants.POSITION_X),
                    section.getNumber("y", ConfigConstants.POSITION_Y),
                    section.getNumber("z", ConfigConstants.POSITION_Z),
                    section.getValue(LOOT, ConfigValue::getAsLoot),
                    section.getBoolean(TO_INVENTORY)
            );
        }
    }
}