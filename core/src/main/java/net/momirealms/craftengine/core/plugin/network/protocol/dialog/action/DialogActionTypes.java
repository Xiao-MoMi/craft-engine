package net.momirealms.craftengine.core.plugin.network.protocol.dialog.action;

import net.momirealms.craftengine.core.plugin.network.protocol.chat.ClickEvent;
import net.momirealms.craftengine.core.plugin.network.protocol.chat.ClickEventTypes;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class DialogActionTypes {
    private DialogActionTypes() {}

    static {
        ClickEventTypes.init();
        for (Map.Entry<ResourceKey<ClickEvent.Type<?>>, ClickEvent.Type<?>> type : BuiltInRegistries.CLICK_EVENT_TYPE.entrySet()) {
            ClickEvent.Type<?> value = type.getValue();
            register(value.id(), StaticAction::read);
        }
        register(Key.of("dynamic/run_command"), DynamicRunCommandAction::read);
        register(Key.of("dynamic/custom"), DynamicCustomAction::read);
    }

    public static DialogAction read(final CompoundTag tag) {
        String type = tag.getString("type");
        DialogAction.Type<? extends DialogAction> actionType = BuiltInRegistries.DIALOG_ACTION_TYPE.getValue(Key.of(type));
        Objects.requireNonNull(actionType, "Dialog action type not found: " + type);
        return actionType.read(tag);
    }

    public static <T extends DialogAction> DialogAction.Type<T> register(Key key, Function<CompoundTag, T> function) {
        DialogAction.Type<T> type = new DialogAction.Type<>(key, function);
        ((WritableRegistry<DialogAction.Type<? extends DialogAction>>) BuiltInRegistries.DIALOG_ACTION_TYPE)
                .register(ResourceKey.create(Registries.DIALOG_ACTION_TYPE.location(), key), type);
        return type;
    }
}
