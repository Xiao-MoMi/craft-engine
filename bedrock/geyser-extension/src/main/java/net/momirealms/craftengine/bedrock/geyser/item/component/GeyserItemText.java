package net.momirealms.craftengine.bedrock.geyser.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.GsonComponentSerializerProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.MessageTranslatorProxy;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class GeyserItemText {
    @Nullable
    private final Object displayName;
    private final List<Object> lore;

    private GeyserItemText(@Nullable Object displayName, List<Object> lore) {
        this.displayName = displayName;
        this.lore = lore;
    }

    @Nullable
    public static GeyserItemText read(@Nullable JsonObject components) {
        if (components == null) {
            return null;
        }
        JsonElement name = components.get("minecraft:custom_name");
        if (name == null || name.isJsonNull()) {
            name = components.get("minecraft:item_name");
        }
        Object displayName = name == null || name.isJsonNull() ? null : GsonComponentSerializerProxy.deserialize(name.toString());
        List<Object> lore = readLore(components.get("minecraft:lore"));
        if (displayName == null && lore.isEmpty()) {
            return null;
        }
        return new GeyserItemText(displayName, lore);
    }

    private static List<Object> readLore(@Nullable JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return List.of();
        }
        JsonArray lines = value.getAsJsonArray();
        List<Object> lore = new ArrayList<>(lines.size());
        for (JsonElement line : lines) {
            lore.add(GsonComponentSerializerProxy.deserialize(line.toString()));
        }
        return lore;
    }

    @Nullable
    String displayName(String locale) {
        return this.displayName == null ? null : MessageTranslatorProxy.INSTANCE.convertMessageRaw(this.displayName, locale);
    }

    boolean hasLore() {
        return !this.lore.isEmpty();
    }

    List<String> lore(String locale) {
        List<String> lines = new ArrayList<>(this.lore.size());
        for (Object line : this.lore) {
            lines.add(MessageTranslatorProxy.INSTANCE.convertMessageRaw(line, locale));
        }
        return lines;
    }
}
