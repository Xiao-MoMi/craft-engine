package net.momirealms.craftengine.bukkit.item.factory;

import com.google.gson.JsonElement;
import com.saicone.rtag.item.ItemTagStream;
import net.momirealms.craftengine.bukkit.util.ItemTags;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.item.EquipmentData;
import net.momirealms.craftengine.core.item.ItemFactory;
import net.momirealms.craftengine.core.item.ItemWrapper;
import net.momirealms.craftengine.core.item.JukeboxPlayable;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public abstract class BukkitItemFactory<W extends ItemWrapper<ItemStack>> extends ItemFactory<W, ItemStack> {

    protected BukkitItemFactory(CraftEngine plugin) {
        super(plugin);
    }

    public static BukkitItemFactory<? extends ItemWrapper<ItemStack>> create(CraftEngine plugin) {
        Objects.requireNonNull(plugin, "plugin");
        switch (plugin.serverVersion()) {
            case "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4" -> {
                return new UniversalItemFactory(plugin);
            }
            case "1.20.5", "1.20.6"-> {
                return new ComponentItemFactory1_20_5(plugin);
            }
            case "1.21", "1.21.1" -> {
                return new ComponentItemFactory1_21(plugin);
            }
            case "1.21.2", "1.21.3" -> {
                return new ComponentItemFactory1_21_2(plugin);
            }
            case "1.21.4" -> {
                return new ComponentItemFactory1_21_4(plugin);
            }
            case "1.21.5", "1.21.6", "1.22", "1.22.1" -> {
                return new ComponentItemFactory1_21_5(plugin);
            }
            default -> throw new IllegalStateException("Unsupported server version: " + plugin.serverVersion());
        }
    }

    @Override
    protected byte[] toByteArray(W item) {
        return ItemTagStream.INSTANCE.toBytes(item.getItem());
    }

    @Override
    protected boolean isBlockItem(W item) {
        // todo 这个 isBlockItem 他考虑组件了吗???
        return item.getItem().getType().isBlock();
    }

    @Override
    protected Key vanillaId(W item) {
        return Key.of(item.getItem().getType().getKey().asString());
    }

    @Override
    protected Key id(W item) {
        return customId(item).orElse(vanillaId(item));
    }

    @Override
    protected ItemStack load(W item) {
        return item.load();
    }

    @Override
    protected ItemStack getItem(W item) {
        return item.getItem();
    }

    @Override
    protected boolean is(W item, Key itemTag) {
        Object literalObject = item.getLiteralObject();
        Object tag = ItemTags.getOrCreate(itemTag);
        try {
            return (boolean) Reflections.method$ItemStack$isTag.invoke(literalObject, tag);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    @Override
    protected JsonElement encodeJson(Object type, Object component) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    public Object encodeJava(Object componentType, @Nullable Object component) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected void resetComponent(W item, Object type) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected void setComponent(W item, Object type, Object value) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected Object getComponent(W item, Object type) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected boolean hasComponent(W item, Object type) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected void removeComponent(W item, Object type) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected Optional<String> tooltipStyle(W item) {
        throw new UnsupportedOperationException("This feature is only available on 1.21.2+");
    }

    @Override
    protected void tooltipStyle(W item, String data) {
        throw new UnsupportedOperationException("This feature is only available on 1.21.2+");
    }

    @Override
    protected Optional<JukeboxPlayable> jukeboxSong(W item) {
        throw new UnsupportedOperationException("This feature is only available on 1.21+");
    }

    @Override
    protected void jukeboxSong(W item, JukeboxPlayable data) {
        throw new UnsupportedOperationException("This feature is only available on 1.21+");
    }

    @Override
    protected Optional<Boolean> glint(W item) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected void glint(W item, Boolean glint) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected Optional<String> itemModel(W item) {
        throw new UnsupportedOperationException("This feature is only available on 1.21.2+");
    }

    @Override
    protected void itemModel(W item, String data) {
        throw new UnsupportedOperationException("This feature is only available on 1.21.2+");
    }

    @Override
    protected Optional<EquipmentData> equippable(W item) {
        throw new UnsupportedOperationException("This feature is only available on 1.21.2+");
    }

    @Override
    protected void equippable(W item, EquipmentData data) {
        throw new UnsupportedOperationException("This feature is only available on 1.21.2+");
    }
}
