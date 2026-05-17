package net.momirealms.craftengine.core.plugin.network.protocol.dialog.input;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.Objects;
import java.util.function.Function;

public final class DialogInputControlTypes {
    private DialogInputControlTypes() {
    }

    public static final DialogInputControl.Type<BooleanInputControl> BOOLEAN = register(Key.of("boolean"), BooleanInputControl::read);
    public static final DialogInputControl.Type<NumberRangeInputControl> NUMBER_RANGE = register(Key.of("number_range"), NumberRangeInputControl::read);
    public static final DialogInputControl.Type<SingleOptionInputControl> SINGLE_OPTION = register(Key.of("single_option"), SingleOptionInputControl::read);
    public static final DialogInputControl.Type<TextInputControl> TEXT = register(Key.of("text"), TextInputControl::read);

    public static DialogInputControl read(CompoundTag tag) {
        String type = tag.getString("type");
        DialogInputControl.Type<? extends DialogInputControl> controlType = BuiltInRegistries.DIALOG_INPUT_CONTROL_TYPE.getValue(Key.of(type));
        Objects.requireNonNull(controlType, "Unknown dialog input control type " + type);
        return controlType.read(tag);
    }

    public static <T extends DialogInputControl> DialogInputControl.Type<T> register(Key key, Function<CompoundTag, T> function) {
        DialogInputControl.Type<T> type = new DialogInputControl.Type<>(key, function);
        ((WritableRegistry<DialogInputControl.Type<? extends DialogInputControl>>) BuiltInRegistries.DIALOG_INPUT_CONTROL_TYPE)
                .register(ResourceKey.create(Registries.DIALOG_INPUT_CONTROL_TYPE.location(), key), type);
        return type;
    }
}
