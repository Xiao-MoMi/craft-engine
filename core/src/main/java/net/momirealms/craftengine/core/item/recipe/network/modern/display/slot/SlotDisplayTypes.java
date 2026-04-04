package net.momirealms.craftengine.core.item.recipe.network.modern.display.slot;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.function.BiFunction;

public final class SlotDisplayTypes {
    private SlotDisplayTypes() {}

    public static final SlotDisplay.Type<EmptySlotDisplay> EMPTY = register(Key.of("empty"), EmptySlotDisplay::read);
    public static final SlotDisplay.Type<AnyFuelDisplay> ANY_FUEL = register(Key.of("any_fuel"), AnyFuelDisplay::read);
    // todo: with_any_potion
    // todo: only_with_component
    public static final SlotDisplay.Type<ItemSlotDisplay> ITEM = register(Key.of("item"), ItemSlotDisplay::read);
    public static final SlotDisplay.Type<ItemStackSlotDisplay> ITEM_STACK = register(Key.of("item_stack"), ItemStackSlotDisplay::read);
    public static final SlotDisplay.Type<TagSlotDisplay> TAG = register(Key.of("tag"), TagSlotDisplay::read);
    // todo: dyed
    public static final SlotDisplay.Type<SmithingTrimDemoSlotDisplay> SMITHING_TRIM = register(Key.of("smithing_trim"), SmithingTrimDemoSlotDisplay::read);
    public static final SlotDisplay.Type<WithRemainderSlotDisplay> WITH_REMAINDER = register(Key.of("with_remainder"), WithRemainderSlotDisplay::read);
    public static final SlotDisplay.Type<CompositeSlotDisplay> COMPOSITE = register(Key.of("composite"), CompositeSlotDisplay::read);

    public static void init() {
    }

    public static <T extends SlotDisplay> SlotDisplay.Type<T> register(Key key, BiFunction<FriendlyByteBuf, FriendlyByteBuf.Reader<Item>, T> function) {
        SlotDisplay.Type<T> type = new SlotDisplay.Type<>(key, function);
        ((WritableRegistry<SlotDisplay.Type<? extends SlotDisplay>>) BuiltInRegistries.SLOT_DISPLAY_TYPE)
                .register(ResourceKey.create(Registries.SLOT_DISPLAY_TYPE.location(), key), type);
        return type;
    }
}
