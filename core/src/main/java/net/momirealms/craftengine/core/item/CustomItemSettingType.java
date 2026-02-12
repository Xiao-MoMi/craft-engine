package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public record CustomItemSettingType<T>(@Nullable BiConsumer<T, Consumer<ItemProcessor>> dataProcessor, @Nullable BiConsumer<T, Consumer<ItemProcessor>> clientBoundDataProcessor) {

    public static <T> CustomItemSettingType<T> simple() {
        return new CustomItemSettingType<>((a, b) -> {}, (a, b) -> {});
    }

    public static <T> CustomItemSettingType<T> newType(@Nullable BiConsumer<T, Consumer<ItemProcessor>> dataProcessor, @Nullable BiConsumer<T, Consumer<ItemProcessor>> clientBoundDataProcessor) {
        return new CustomItemSettingType<>(dataProcessor, clientBoundDataProcessor);
    }
}
