package net.momirealms.craftengine.core.item.setting;

import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.processor.NetworkItemProcessor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class CustomItemSettingType<T> {
    private static final List<CustomItemSettingType<?>> TYPES = new ArrayList<>();
    private static volatile CustomItemSettingType<?>[] frozenTypes;
    private final int id;
    @Nullable
    private final BiConsumer<T, Consumer<ItemProcessor>> dataProcessor;
    @Nullable
    private final BiConsumer<T, Consumer<NetworkItemProcessor>> clientBoundDataProcessor;

    public CustomItemSettingType(@Nullable BiConsumer<T, Consumer<ItemProcessor>> dataProcessor,
                                 @Nullable BiConsumer<T, Consumer<NetworkItemProcessor>> clientBoundDataProcessor) {
        this.dataProcessor = dataProcessor;
        this.clientBoundDataProcessor = clientBoundDataProcessor;
        synchronized (CustomItemSettingType.class) {
            if (frozenTypes != null) {
                throw new IllegalStateException("Custom item setting types must be registered before item configuration loading");
            }
            this.id = TYPES.size();
            TYPES.add(this);
        }
    }

    public int id() {
        return this.id;
    }

    @Nullable
    public BiConsumer<T, Consumer<ItemProcessor>> dataProcessor() {
        return this.dataProcessor;
    }

    @Nullable
    public BiConsumer<T, Consumer<NetworkItemProcessor>> clientBoundDataProcessor() {
        return this.clientBoundDataProcessor;
    }

    public static synchronized void freeze() {
        if (frozenTypes == null) {
            frozenTypes = TYPES.toArray(new CustomItemSettingType<?>[0]);
            TYPES.clear();
        }
    }

    static Object[] newDataArray() {
        CustomItemSettingType<?>[] types = frozenTypes;
        if (types == null) {
            throw new IllegalStateException("Custom item setting types must be frozen before creating item settings");
        }
        return new Object[types.length];
    }

    static CustomItemSettingType<?> byId(int id) {
        return frozenTypes[id];
    }

    public static <T> CustomItemSettingType<T> simple() {
        return new CustomItemSettingType<>(null, null);
    }

    public static <T> CustomItemSettingType<T> newType(@Nullable BiConsumer<T, Consumer<ItemProcessor>> dataProcessor, @Nullable BiConsumer<T, Consumer<NetworkItemProcessor>> clientBoundDataProcessor) {
        return new CustomItemSettingType<>(dataProcessor, clientBoundDataProcessor);
    }
}
