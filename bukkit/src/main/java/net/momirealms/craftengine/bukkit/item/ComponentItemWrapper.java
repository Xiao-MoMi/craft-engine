package net.momirealms.craftengine.bukkit.item;

import com.google.gson.JsonElement;
import com.saicone.rtag.data.ComponentType;
import com.saicone.rtag.tag.TagBase;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.item.ItemWrapper;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("UnstableApiUsage")
public class ComponentItemWrapper implements ItemWrapper<ItemStack> {
    private final ItemStack item;

    public ComponentItemWrapper(final ItemStack item) {
        this.item = FastNMS.INSTANCE.ensureCraftItemStack(item);
    }

    public ComponentItemWrapper(final ItemStack item, int count) {
        this.item = FastNMS.INSTANCE.ensureCraftItemStack(item);
        this.item.setAmount(count);
    }

    public void removeComponent(Object type) {
        FastNMS.INSTANCE.removeComponent(this.getLiteralObject(), ensureDataComponentType(type));
    }

    public void resetComponent(Object type) {
        FastNMS.INSTANCE.resetComponent(this.getLiteralObject(), ensureDataComponentType(type));
    }

    public void setComponent(Object type, final Object value) {
        if (value instanceof JsonElement jsonElement) {
            setJsonComponent(type, jsonElement);
        } else if (TagBase.isTag(value)) {
            setNBTComponent(type, value);
        } else {
            setJavaComponent(type, value);
        }
    }

    public Object getComponent(Object type) {
        return FastNMS.INSTANCE.getComponent(getLiteralObject(), ensureDataComponentType(type));
    }

    public boolean hasComponent(Object type) {
        return FastNMS.INSTANCE.hasComponent(getLiteralObject(), ensureDataComponentType(type));
    }

    public void setJavaComponent(Object type, Object value) {
        ComponentType.parseJava(type, value).ifPresent(it -> FastNMS.INSTANCE.setComponent(this.getLiteralObject(), ensureDataComponentType(type), it));
    }

    public void setJsonComponent(Object type, JsonElement value) {
        ComponentType.parseJson(type, value).ifPresent(it -> FastNMS.INSTANCE.setComponent(this.getLiteralObject(), ensureDataComponentType(type), it));
    }

    public void setNBTComponent(Object type, Object value) {
        ComponentType.parseNbt(type, value).ifPresent(it -> FastNMS.INSTANCE.setComponent(this.getLiteralObject(), ensureDataComponentType(type), it));
    }

    private Object ensureDataComponentType(Object type) {
        assert Reflections.clazz$DataComponentType != null;
        if (!Reflections.clazz$DataComponentType.isInstance(type)) {
            Key key = Key.of(type.toString());
            return FastNMS.INSTANCE.getComponentType(key.namespace(), key.value());
        }
        return type;
    }

    @Override
    public ItemWrapper<ItemStack> copyWithCount(int count) {
        ItemStack copied = this.item.clone();
        copied.setAmount(count);
        return new ComponentItemWrapper(copied);
    }

    @Override
    public ItemStack getItem() {
        return this.item;
    }

    @Override
    public ItemStack load() {
        return this.item;
    }

    @Override
    public Object getLiteralObject() {
        return FastNMS.INSTANCE.field$CraftItemStack$handle(this.item);
    }

    @Override
    public int count() {
        return this.item.getAmount();
    }

    @Override
    public void count(int amount) {
        this.item.setAmount(amount);
    }
}
