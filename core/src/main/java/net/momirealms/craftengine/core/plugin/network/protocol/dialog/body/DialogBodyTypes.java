package net.momirealms.craftengine.core.plugin.network.protocol.dialog.body;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.Objects;
import java.util.function.Function;

public final class DialogBodyTypes {
    private DialogBodyTypes() {
    }

    public static final DialogBody.Type<ItemBody> ITEM = register(Key.of("item"), ItemBody::read);
    public static final DialogBody.Type<PlainMessageBody> PLAIN_MESSAGE = register(Key.of("plain_message"), PlainMessageBody::read);

    public static DialogBody read(final CompoundTag tag) {
        String type = tag.getString("type");
        DialogBody.Type<? extends DialogBody> bodyType = BuiltInRegistries.DIALOG_BODY_TYPE.getValue(Key.of(type));
        Objects.requireNonNull(bodyType, "Dialog body type not found: " + type);
        return bodyType.read(tag);
    }

    public static <T extends DialogBody> DialogBody.Type<T> register(Key key, Function<CompoundTag, T> function) {
        DialogBody.Type<T> type = new DialogBody.Type<>(key, function);
        ((WritableRegistry<DialogBody.Type<? extends DialogBody>>) BuiltInRegistries.DIALOG_BODY_TYPE)
                .register(ResourceKey.create(Registries.DIALOG_BODY_TYPE.location(), key), type);
        return type;
    }
}
