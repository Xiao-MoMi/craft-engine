package net.momirealms.craftengine.core.plugin.network.protocol.chat;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.Objects;
import java.util.function.Function;

public final class ClickEventTypes {
    private ClickEventTypes() {
    }

    public static final ClickEvent.Type<OpenUrlClickEvent> OPEN_URL = register(Key.of("open_url"), OpenUrlClickEvent::read);
    public static final ClickEvent.Type<OpenFileClickEvent> OPEN_FILE = register(Key.of("open_file"), OpenFileClickEvent::read);
    public static final ClickEvent.Type<RunCommandClickEvent> RUN_COMMAND = register(Key.of("run_command"), RunCommandClickEvent::read);
    public static final ClickEvent.Type<SuggestCommandClickEvent> SUGGEST_COMMAND = register(Key.of("suggest_command"), SuggestCommandClickEvent::read);
    public static final ClickEvent.Type<ShowDialogClickEvent> SHOW_DIALOG = register(Key.of("show_dialog"), ShowDialogClickEvent::read);
    public static final ClickEvent.Type<ChangePageClickEvent> CHANGE_PAGE = register(Key.of("change_page"), ChangePageClickEvent::read);
    public static final ClickEvent.Type<CopyToClipboardClickEvent> COPY_TO_CLIPBOARD = register(Key.of("copy_to_clipboard"), CopyToClipboardClickEvent::read);
    public static final ClickEvent.Type<CustomClickEvent> CUSTOM = register(Key.of("custom"), CustomClickEvent::read);

    public static void init() {}

    public static ClickEvent read(final CompoundTag tag) {
        String type = tag.getString("type");
        ClickEvent.Type<? extends ClickEvent> eventType = BuiltInRegistries.CLICK_EVENT_TYPE.getValue(Key.of(type));
        Objects.requireNonNull(eventType, "Click event type not found: " + type);
        return eventType.read(tag);
    }

    public static <T extends ClickEvent> ClickEvent.Type<T> register(Key key, Function<CompoundTag, T> function) {
        ClickEvent.Type<T> type = new ClickEvent.Type<>(key, function);
        ((WritableRegistry<ClickEvent.Type<? extends ClickEvent>>) BuiltInRegistries.CLICK_EVENT_TYPE)
                .register(ResourceKey.create(Registries.CLICK_EVENT_TYPE.location(), key), type);
        return type;
    }
}
