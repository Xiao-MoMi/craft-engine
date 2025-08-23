package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.World;
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
        ctx.getOptionalParameter(DirectContextParameters.FURNITURE).ifPresent(furniture -> removeFurniture(ctx, furniture, dropLoot, playSound));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void removeFurniture(Context ctx, Furniture furniture, boolean dropLoot, boolean playSound) {
        if (!furniture.isValid()) return;
        WorldPosition position = furniture.position();
        World world = position.world();
        furniture.destroy();
        LootTable lootTable = furniture.config().lootTable();
        if (dropLoot && lootTable != null) {
            ContextHolder.Builder builder = ContextHolder.builder()
                    .withParameter(DirectContextParameters.POSITION, position)
                    .withParameter(DirectContextParameters.FURNITURE, furniture)
                    .withOptionalParameter(DirectContextParameters.FURNITURE_ITEM, furniture.extraData().item().orElse(null));
            Optional<Player> optionalPlayer = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
            Player player = optionalPlayer.orElse(null);
            if (player != null) {
                Item<?> itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                builder.withParameter(DirectContextParameters.PLAYER, player)
                        .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand.isEmpty() ? null : itemInHand);
            }
            List<Item<?>> items = lootTable.getRandomItems(builder.build(), world, player);
            for (Item<?> item : items) {
                world.dropItemNaturally(position, item);
            }
        }
        if (playSound) {
            SoundData breakSound = furniture.config().settings().sounds().breakSound();
            world.playSound(position, breakSound.id(), breakSound.volume().get(), breakSound.pitch().get(), SoundSource.BLOCK);
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
