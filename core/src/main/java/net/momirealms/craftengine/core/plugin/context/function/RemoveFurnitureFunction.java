package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.Loot;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.List;
import java.util.Optional;

public final class RemoveFurnitureFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final boolean dropLoot;
    private final boolean playSound;

    private RemoveFurnitureFunction(List<Condition<CTX>> predicates,
                                    boolean playSound,
                                    boolean dropLoot) {
        super(predicates);
        this.dropLoot = dropLoot;
        this.playSound = playSound;
    }

    @Override
    public void runInternal(CTX ctx) {
        ctx.getOptionalParameter(DirectContextParameters.FURNITURE).ifPresent(furniture -> removeFurniture(ctx, furniture, dropLoot, playSound));
    }

    public static void removeFurniture(Context ctx, Furniture furniture, boolean dropLoot, boolean playSound) {
        if (!furniture.isValid()) return;
        WorldPosition position = furniture.position();
        World world = position.world();
        furniture.destroy();
        Loot loot = furniture.config.loot();
        if (dropLoot && loot != null) {
            ContextHolder.Builder builder = ContextHolder.builder()
                    .withParameter(DirectContextParameters.POSITION, position)
                    .withParameter(DirectContextParameters.FURNITURE, furniture)
                    .withOptionalParameter(DirectContextParameters.FURNITURE_ITEM, furniture.persistentData.item().orElse(null));
            Optional<Player> optionalPlayer = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
            Player player = optionalPlayer.orElse(null);
            if (player != null) {
                Item itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                builder.withParameter(DirectContextParameters.PLAYER, player)
                        .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand.isEmpty() ? null : itemInHand);
            }
            List<Item> items = loot.getRandomItems(builder.build(), world, player);
            for (Item item : items) {
                world.dropItemNaturally(position, item);
            }
        }
        if (playSound) {
            SoundData breakSound = furniture.config().settings().sounds().breakSound();
            world.playSound(position, breakSound.id(), breakSound.volume().get(), breakSound.pitch().get(), SoundSource.BLOCK);
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, RemoveFurnitureFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, RemoveFurnitureFunction<CTX>> {
        private static final String[] PLAY_SOUND = new String[] {"play_sound", "play-sound"};
        private static final String[] DROP_LOOT = new String[] {"drop_loot", "drop-loot"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public RemoveFurnitureFunction<CTX> create(ConfigSection section) {
            return new RemoveFurnitureFunction<>(
                    getPredicates(section),
                    section.getBoolean(PLAY_SOUND, true),
                    section.getBoolean(DROP_LOOT, true)
            );
        }
    }
}