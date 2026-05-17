package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.function.BiFunction;

public final class SlotDisplayTypes {
    private SlotDisplayTypes() {}

    public static final SlotDisplay.Type<EmptySlotDisplay> EMPTY = register(Key.of("empty"), EmptySlotDisplay::read);
    public static final SlotDisplay.Type<AnyFuelDisplay> ANY_FUEL = register(Key.of("any_fuel"), AnyFuelDisplay::read);
    public static final SlotDisplay.Type<WithAnyPotionDisplay> WITH_ANY_POTION = registerIf(Key.of("with_any_potion"), WithAnyPotionDisplay::read, VersionHelper.isOrAbove26_1);
    public static final SlotDisplay.Type<OnlyWithComponentDisplay> ONLY_WITH_COMPONENT = registerIf(Key.of("only_with_component"), OnlyWithComponentDisplay::read, VersionHelper.isOrAbove26_1);
    public static final SlotDisplay.Type<ItemSlotDisplay> ITEM = register(Key.of("item"), ItemSlotDisplay::read);
    public static final SlotDisplay.Type<ItemStackSlotDisplay> ITEM_STACK = register(Key.of("item_stack"), ItemStackSlotDisplay::read);
    public static final SlotDisplay.Type<TagSlotDisplay> TAG = register(Key.of("tag"), TagSlotDisplay::read);
    public static final SlotDisplay.Type<DyedSlotDisplay> DYED = registerIf(Key.of("dyed"), DyedSlotDisplay::read, VersionHelper.isOrAbove26_1);
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

    public static <T extends SlotDisplay> SlotDisplay.Type<T> registerIf(Key key, BiFunction<FriendlyByteBuf, FriendlyByteBuf.Reader<Item>, T> function, boolean register) {
        if (!register) return null;
        SlotDisplay.Type<T> type = new SlotDisplay.Type<>(key, function);
        ((WritableRegistry<SlotDisplay.Type<? extends SlotDisplay>>) BuiltInRegistries.SLOT_DISPLAY_TYPE)
                .register(ResourceKey.create(Registries.SLOT_DISPLAY_TYPE.location(), key), type);
        return type;
    }
}
