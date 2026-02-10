package net.momirealms.craftengine.bukkit.item;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistryOps;
import net.momirealms.craftengine.bukkit.util.EquipmentSlotUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.ItemType;
import net.momirealms.craftengine.core.item.ItemWrapper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentMapProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class ComponentItemWrapper implements ItemWrapper<ItemStack> {
    private final ItemStack item;
    private final Object handle;
    private ItemType itemType;

    public ComponentItemWrapper(final Object handle) {
        this.handle = handle;
        this.item = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(handle);
    }

    public ComponentItemWrapper(final ItemStack item) {
        this.item = ItemStackUtils.ensureCraftItemStack(item);
        this.handle = FastNMS.INSTANCE.field$CraftItemStack$handle(this.item);
    }

    public ComponentItemWrapper(final ItemStack item, int count) {
        this.item = ItemStackUtils.ensureCraftItemStack(item);
        this.item.setAmount(count);
        this.handle = FastNMS.INSTANCE.field$CraftItemStack$handle(this.item);
    }

    public ItemType itemType() {
        if (this.itemType == null) {
            this.itemType = new ComponentItemType(ItemStackProxy.INSTANCE.getItem(this.getLiteralObject()));
        }
        return this.itemType;
    }

    public void removeComponent(Object type) {
        ItemStackProxy.INSTANCE.remove(this.getLiteralObject(), ensureDataComponentType(type));
    }

    public void resetComponent(Object type) {
        Object item = ItemStackProxy.INSTANCE.getItem(this.getLiteralObject());
        Object componentMap = ItemProxy.INSTANCE.components(item);
        Object componentType = ensureDataComponentType(type);
        Object defaultComponent;
        if (VersionHelper.isOrAbove1_21_5()) {
            defaultComponent = DataComponentGetterProxy.INSTANCE.get(componentMap, componentType);
        } else {
            defaultComponent = DataComponentMapProxy.INSTANCE.get(componentMap, componentType);
        }
        ItemStackProxy.INSTANCE.set(this.getLiteralObject(), componentType, defaultComponent);
    }

    public void setComponent(Object type, final Object value) {
        if (value instanceof JsonElement jsonElement) {
            setJsonComponent(type, jsonElement);
        } else if (CoreReflections.clazz$Tag.isInstance(value)) {
            setNBTComponent(type, value);
        } else if (value instanceof Tag tag) {
            setSparrowNBTComponent(type, tag);
        } else {
            setJavaComponent(type, value);
        }
    }

    public Object getExactComponent(Object type) {
        return ItemStackProxy.INSTANCE.get(getLiteralObject(), ensureDataComponentType(type));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getJavaComponent(Object type) {
        return (Optional<T>) getComponentInternal(type, MRegistryOps.JAVA);
    }

    public Optional<JsonElement> getJsonComponent(Object type) {
        return getComponentInternal(type, MRegistryOps.JSON);
    }

    public Optional<Object> getNBTComponent(Object type) {
        return getComponentInternal(type, MRegistryOps.NBT);
    }

    public Optional<Tag> getSparrowNBTComponent(Object type) {
        return getComponentInternal(type, MRegistryOps.SPARROW_NBT).map(Tag::copy);
    }

    private <T> Optional<T> getComponentInternal(Object type, DynamicOps<T> ops) {
        Object componentType = ensureDataComponentType(type);
        Codec<T> codec = DataComponentTypeProxy.INSTANCE.codec(componentType);
        try {
            T componentData = ItemStackProxy.INSTANCE.get(getLiteralObject(), componentType);
            if (componentData == null) return Optional.empty();
            DataResult<T> result = codec.encodeStart(ops, componentData);
            return result.result();
        } catch (Throwable t) {
            throw new RuntimeException("Cannot read component " + type.toString(), t);
        }
    }

    public boolean hasComponent(Object type) {
        return ItemStackProxy.INSTANCE.has(getLiteralObject(), ensureDataComponentType(type));
    }

    public boolean hasNonDefaultComponent(Object type) {
        if (VersionHelper.isOrAbove1_21_4()) {
            return ItemStackProxy.INSTANCE.hasNonDefault(getLiteralObject(), ensureDataComponentType(type));
        } else {
            Object item = ItemStackProxy.INSTANCE.getItem(this.getLiteralObject());
            Object componentMap = ItemProxy.INSTANCE.components(item);
            Object componentType = ensureDataComponentType(type);
            Object defaultComponent;
            if (VersionHelper.isOrAbove1_21_5()) {
                defaultComponent = DataComponentGetterProxy.INSTANCE.get(componentMap, componentType);
            } else {
                defaultComponent = DataComponentMapProxy.INSTANCE.get(componentMap, componentType);
            }
            return !Objects.equals(defaultComponent, getExactComponent(componentType));
        }
    }

    public void setExactComponent(Object type, final Object value) {
        ItemStackProxy.INSTANCE.set(this.getLiteralObject(), ensureDataComponentType(type), value);
    }

    public void setJavaComponent(Object type, Object value) {
        setComponentInternal(type, MRegistryOps.JAVA, value);
    }

    public void setJsonComponent(Object type, JsonElement value) {
        setComponentInternal(type, MRegistryOps.JSON, value);
    }

    public void setNBTComponent(Object type, Object value) {
        setComponentInternal(type, MRegistryOps.NBT, value);
    }

    public void setSparrowNBTComponent(Object type, Tag value) {
        setComponentInternal(type, MRegistryOps.SPARROW_NBT, value);
    }

    private <T> void setComponentInternal(Object type, DynamicOps<T> ops, T value) {
        if (value == null) return;
        Object componentType = ensureDataComponentType(type);
        if (componentType == null) {
            return;
        }
        Codec<T> codec = DataComponentTypeProxy.INSTANCE.codec(componentType);
        try {
            DataResult<T> result = codec.parse(ops, value);
            if (result.isError()) {
                throw new IllegalArgumentException(result.toString());
            }
            result.result().ifPresent(it -> ItemStackProxy.INSTANCE.set(this.getLiteralObject(), componentType, it));
        } catch (Throwable t) {
            throw new RuntimeException("Cannot parse component " + type.toString(), t);
        }
    }

    private Object ensureDataComponentType(Object type) {
        if (!DataComponentTypeProxy.CLASS.isInstance(type)) {
            Key key = Key.of(type.toString());
            return RegistryUtils.getRegistryValue(MBuiltInRegistries.DATA_COMPONENT_TYPE, KeyUtils.toIdentifier(key));
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
    public Object getLiteralObject() {
        return this.handle;
    }

    @Override
    public int count() {
        return this.item.getAmount();
    }

    @Override
    public void count(int amount) {
        this.item.setAmount(Math.max(amount, 0));
    }

    @Override
    public void shrink(int amount) {
        count(count() - amount);
    }

    @Override
    public void grow(int amount) {
        count(count() + amount);
    }

    @Override
    public void hurtAndBreak(int amount, @Nullable Player player, @Nullable EquipmentSlot slot) {
        if (player == null) {
            if (this.hurt(amount)) {
                this.shrink(1);
                this.setJavaComponent(DataComponentTypes.DAMAGE, 0);
            }
            return;
        }
        ItemStackUtils.hurtAndBreak(
                this.handle,
                amount,
                player.serverPlayer(),
                slot != null ? EquipmentSlotUtils.toNMSEquipmentSlot(slot) : null
        );
    }

    private boolean hurt(int amount) {
        if (!this.hasComponent(DataComponentTypes.MAX_DAMAGE) || this.hasComponent(DataComponentTypes.UNBREAKABLE) || !this.hasComponent(DataComponentTypes.DAMAGE)) return false;
        if (amount > 0) {
            int level = this.item.getEnchantmentLevel(Enchantment.UNBREAKING);
            int ignoredDamage = 0;
            for (int i = 0; level > 0 && i < amount; ++i) {
                if (RandomUtils.generateRandomInt(0, level + 1) > 0) ++ignoredDamage;
            }
            amount -= ignoredDamage;
            if (amount <= 0) return false;
        }
        Optional<Integer> optionalDamage = this.getJavaComponent(DataComponentTypes.DAMAGE);
        int damage = optionalDamage.orElse(0) + amount;
        this.setJavaComponent(DataComponentTypes.DAMAGE, damage);
        Optional<Integer> optionalMaxDamage = this.getJavaComponent(DataComponentTypes.MAX_DAMAGE);
        return damage >= optionalMaxDamage.orElseGet(() -> (int) this.item.getType().getMaxDurability());
    }
}
