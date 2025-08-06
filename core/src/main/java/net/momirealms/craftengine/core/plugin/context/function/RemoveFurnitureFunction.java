package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RemoveFurnitureFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final boolean dropLoot;
    private final boolean playSound;

    public RemoveFurnitureFunction(boolean dropLoot, boolean playSound, List<Condition<CTX>> predicates) {
        super(predicates);
        this.dropLoot = dropLoot;
        this.playSound = playSound;
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<WorldPosition> optionalWorldPosition = ctx.getOptionalParameter(DirectContextParameters.POSITION);
        if (optionalWorldPosition.isPresent()) {
            // Search for furniture in the context
            Optional<Furniture> optionalFurniture = ctx.getOptionalParameter(DirectContextParameters.FURNITURE);
            if (optionalFurniture.isPresent()) {
                Furniture furniture = optionalFurniture.get();
                if (furniture.isValid()) {
                    furniture.destroy();
                    // TODO: Implement logic to drop loot and play sounds
                    // using this.dropLoot and this.playSound when necessary
                }
            }
        }
    }

    @Override
    public Key type() {
        return CommonFunctions.REMOVE_FURNITURE;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            boolean dropLoot = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("drop-loot", true), "drop-loot");
            boolean playSound = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("play-sound", true), "play-sound");
            return new RemoveFurnitureFunction<>(dropLoot, playSound, getPredicates(arguments));
        }
    }
}
