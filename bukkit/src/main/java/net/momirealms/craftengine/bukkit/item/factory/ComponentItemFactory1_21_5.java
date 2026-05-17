package net.momirealms.craftengine.bukkit.item.factory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.item.ComponentItemWrapper;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.core.attribute.AttributeModifier;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.component.value.JukeboxPlayable;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ComponentItemFactory1_21_5 extends ComponentItemFactory1_21_4 {

    public ComponentItemFactory1_21_5(CraftEngine plugin) {
        super(plugin);
    }

    @Override
    protected void customNameJson(ComponentItemWrapper item, String json) {
        if (json == null) {
            item.resetComponent(DataComponentTypes.CUSTOM_NAME);
        } else {
            item.setSparrowNBTComponent(DataComponentTypes.CUSTOM_NAME, AdventureHelper.componentToNbt(AdventureHelper.jsonToComponent(json)));
        }
    }

    @Override
    protected Optional<String> customNameJson(ComponentItemWrapper item) {
        return item.getComponentAsJson(DataComponentTypes.CUSTOM_NAME).map(it -> GsonHelper.get().toJson(it));
    }

    @Override
    protected void customNameComponent(ComponentItemWrapper item, Component component) {
        if (component == null) {
            item.resetComponent(DataComponentTypes.CUSTOM_NAME);
        } else {
            item.setSparrowNBTComponent(DataComponentTypes.CUSTOM_NAME, AdventureHelper.componentToNbt(component));
        }
    }

    @Override
    protected Optional<Component> customNameComponent(ComponentItemWrapper item) {
        return customNameJson(item).map(AdventureHelper::jsonToComponent);
    }

    @Override
    protected void itemNameJson(ComponentItemWrapper item, String json) {
        if (json == null) {
            item.resetComponent(DataComponentTypes.ITEM_NAME);
        } else {
            item.setSparrowNBTComponent(DataComponentTypes.ITEM_NAME, AdventureHelper.componentToNbt(AdventureHelper.jsonToComponent(json)));
        }
    }

    @Override
    protected void itemNameComponent(ComponentItemWrapper item, Component component) {
        if (component == null) {
            item.resetComponent(DataComponentTypes.ITEM_NAME);
        } else {
            item.setSparrowNBTComponent(DataComponentTypes.ITEM_NAME, AdventureHelper.componentToNbt(component));
        }
    }

    @Override
    protected Optional<String> itemNameJson(ComponentItemWrapper item) {
        return item.getComponentAsJson(DataComponentTypes.ITEM_NAME).map(it -> GsonHelper.get().toJson(it));
    }

    @Override
    protected Optional<List<String>> loreJson(ComponentItemWrapper item) {
        if (!item.hasComponent(DataComponentTypes.LORE)) return Optional.empty();
        Optional<JsonElement> json = item.getComponentAsJson(DataComponentTypes.LORE);
        if (json.isEmpty()) return Optional.empty();
        List<String> lore = new ArrayList<>();
        for (JsonElement jsonElement : (JsonArray) json.get()) {
            lore.add(GsonHelper.get().toJson(jsonElement));
        }
        return Optional.of(lore);
    }

    @Override
    protected void loreComponent(ComponentItemWrapper item, List<Component> lore) {
        if (lore == null || lore.isEmpty()) {
            item.resetComponent(DataComponentTypes.LORE);
        } else {
            List<Tag> loreTags = new ArrayList<>();
            for (Component component : lore) {
                loreTags.add(AdventureHelper.componentToTag(component));
            }
            item.setSparrowNBTComponent(DataComponentTypes.LORE, new ListTag(loreTags));
        }
    }

    @Override
    protected void loreJson(ComponentItemWrapper item, List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            item.resetComponent(DataComponentTypes.LORE);
        } else {
            List<Tag> loreTags = new ArrayList<>();
            for (String json : lore) {
                loreTags.add(AdventureHelper.componentToTag(AdventureHelper.jsonToComponent(json)));
            }
            item.setSparrowNBTComponent(DataComponentTypes.LORE, new ListTag(loreTags));
        }
    }

    @Override
    protected Optional<JukeboxPlayable> jukeboxSong(ComponentItemWrapper item) {
        if (!item.hasComponent(DataComponentTypes.JUKEBOX_PLAYABLE)) return Optional.empty();
        String song = (String) item.getComponentAsJava(DataComponentTypes.JUKEBOX_PLAYABLE).orElse(null);
        if (song == null) return Optional.empty();
        return Optional.of(new JukeboxPlayable(song, true));
    }

    @Override
    protected void jukeboxSong(ComponentItemWrapper item, JukeboxPlayable data) {
        item.setJavaComponent(DataComponentTypes.JUKEBOX_PLAYABLE, data.song());
    }

    @Override
    protected void attributeModifiers(ComponentItemWrapper item, List<AttributeModifier> modifierList) {
        ListTag modifiers = new ListTag();
        for (AttributeModifier modifier : modifierList) {
            CompoundTag modifierTag = new CompoundTag();
            modifierTag.putString("type", modifier.type());
            modifierTag.putString("slot", modifier.slot().name().toLowerCase(Locale.ENGLISH));
            modifierTag.putString("id", modifier.id().toString());
            modifierTag.putDouble("amount", modifier.amount());
            modifierTag.putString("operation", modifier.operation().id());
            AttributeModifier.Display display = modifier.display();
            if (VersionHelper.isOrAbove1_21_6 && display != null) {
                CompoundTag displayTag = new CompoundTag();
                AttributeModifier.Display.Type displayType = display.type();
                displayTag.putString("type", displayType.name().toLowerCase(Locale.ENGLISH));
                if (displayType == AttributeModifier.Display.Type.OVERRIDE) {
                    displayTag.put("value", AdventureHelper.componentToTag(display.value()));
                }
                modifierTag.put("display", displayTag);
            }
            modifiers.add(modifierTag);
        }
        item.setSparrowNBTComponent(DataComponentKeys.ATTRIBUTE_MODIFIERS, modifiers);
    }
}
