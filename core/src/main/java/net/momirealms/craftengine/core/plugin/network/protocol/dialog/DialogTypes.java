package net.momirealms.craftengine.core.plugin.network.protocol.dialog;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.Objects;
import java.util.function.Function;

public final class DialogTypes {
    private DialogTypes() {
    }

    public static final Dialog.Type<NoticeDialog> NOTICE = register(Key.of("notice"), NoticeDialog::read);
    public static final Dialog.Type<ServerLinksDialog> SERVER_LINKS = register(Key.of("server_links"), ServerLinksDialog::read);
    public static final Dialog.Type<DialogListDialog> DIALOG_LIST = register(Key.of("dialog_list"), DialogListDialog::read);
    public static final Dialog.Type<MultiActionDialog> MULTI_ACTION = register(Key.of("multi_action"), MultiActionDialog::read);
    public static final Dialog.Type<ConfirmationDialog> CONFIRMATION = register(Key.of("confirmation"), ConfirmationDialog::read);

    public static Dialog read(CompoundTag tag) {
        String type = tag.getString("type");
        Dialog.Type<? extends Dialog> dialogType = BuiltInRegistries.DIALOG_TYPE.getValue(Key.of(type));
        Objects.requireNonNull(dialogType, "Unknown dialog type " + type);
        return dialogType.read(tag);
    }

    public static <T extends Dialog> Dialog.Type<T> register(Key key, Function<CompoundTag, T> function) {
        Dialog.Type<T> type = new Dialog.Type<>(key, function);
        ((WritableRegistry<Dialog.Type<? extends Dialog>>) BuiltInRegistries.DIALOG_TYPE)
                .register(ResourceKey.create(Registries.DIALOG_TYPE.location(), key), type);
        return type;
    }
}
