package net.momirealms.craftengine.bukkit.item.factory;

import com.saicone.rtag.RtagItem;
import com.saicone.rtag.data.ComponentType;
import com.saicone.rtag.item.ItemObject;
import net.momirealms.craftengine.bukkit.item.RTagItemWrapper;
import net.momirealms.craftengine.bukkit.util.EnchantmentUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Enchantment;
import net.momirealms.craftengine.core.item.ItemWrapper;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public class ComponentItemFactory extends BukkitItemFactory {

    public ComponentItemFactory(CraftEngine plugin) {
        super(plugin);
    }

    @Override
    public Object encodeJava(Key componentType, @Nullable Object component) {
        return ComponentType.encodeJava(componentType, component).orElse(null);
    }

    @Override
    protected void customModelData(ItemWrapper<ItemStack> item, Integer data) {
        if (data == null) {
            item.removeComponent(ComponentKeys.CUSTOM_MODEL_DATA);
        } else {
            item.setComponent(ComponentKeys.CUSTOM_MODEL_DATA, data);
        }
    }

    @Override
    protected Optional<Integer> customModelData(ItemWrapper<ItemStack> item) {
        if (!item.hasComponent(ComponentKeys.CUSTOM_MODEL_DATA)) return Optional.empty();
        return Optional.ofNullable(
                (Integer) ComponentType.encodeJava(
                        ComponentKeys.CUSTOM_MODEL_DATA,
                        item.getComponent(ComponentKeys.CUSTOM_MODEL_DATA)
                ).orElse(null));
    }

    @Override
    protected void customName(ItemWrapper<ItemStack> item, String json) {
        if (json == null) {
            item.removeComponent(ComponentKeys.CUSTOM_NAME);
        } else {
            item.setComponent(ComponentKeys.CUSTOM_NAME, json);
        }
    }

    @Override
    protected Optional<String> customName(ItemWrapper<ItemStack> item) {
        if (!item.hasComponent(ComponentKeys.CUSTOM_NAME)) return Optional.empty();
        return Optional.ofNullable(
                (String) ComponentType.encodeJava(
                        ComponentKeys.CUSTOM_NAME,
                        item.getComponent(ComponentKeys.CUSTOM_NAME)
                ).orElse(null)
        );
    }

    @Override
    protected void itemName(ItemWrapper<ItemStack> item, String json) {
        if (json == null) {
            item.removeComponent(ComponentKeys.ITEM_NAME);
        } else {
            item.setComponent(ComponentKeys.ITEM_NAME, json);
        }
    }

    @Override
    protected Optional<String> itemName(ItemWrapper<ItemStack> item) {
        if (!item.hasComponent(ComponentKeys.ITEM_NAME)) return Optional.empty();
        return Optional.ofNullable(
                (String) ComponentType.encodeJava(
                        ComponentKeys.ITEM_NAME,
                        item.getComponent(ComponentKeys.ITEM_NAME)
                ).orElse(null)
        );
    }

    @Override
    protected void skull(ItemWrapper<ItemStack> item, String skullData) {
        final Map<String, Object> profile = Map.of(
                "properties", List.of(
                        Map.of(
                                "name", "textures",
                                "value", skullData
                        )
                )
        );
        item.setComponent("minecraft:profile", profile);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Optional<List<String>> lore(ItemWrapper<ItemStack> item) {
        if (!item.hasComponent(ComponentKeys.LORE)) return Optional.empty();
        return Optional.ofNullable(
                (List<String>) ComponentType.encodeJava(
                        ComponentKeys.LORE,
                        item.getComponent(ComponentKeys.LORE)
                ).orElse(null)
        );
    }

    @Override
    protected void lore(ItemWrapper<ItemStack> item, List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            item.removeComponent(ComponentKeys.LORE);
        } else {
            item.setComponent(ComponentKeys.LORE, lore);
        }
    }

    @Override
    protected boolean unbreakable(ItemWrapper<ItemStack> item) {
        return item.hasComponent(ComponentKeys.UNBREAKABLE);
    }

    @Override
    protected void unbreakable(ItemWrapper<ItemStack> item, boolean unbreakable) {
        if (unbreakable) {
            item.removeComponent(ComponentKeys.UNBREAKABLE);
        } else {
            item.setComponent(ComponentKeys.UNBREAKABLE, true);
        }
    }

    @Override
    protected Optional<Boolean> glint(ItemWrapper<ItemStack> item) {
        return Optional.ofNullable((Boolean) item.getComponent(ComponentKeys.ENCHANTMENT_GLINT_OVERRIDE));
    }

    @Override
    protected void glint(ItemWrapper<ItemStack> item, Boolean glint) {
        item.setComponent(ComponentKeys.ENCHANTMENT_GLINT_OVERRIDE, glint);
    }

    @Override
    protected Optional<Integer> damage(ItemWrapper<ItemStack> item) {
        if (!item.hasComponent(ComponentKeys.DAMAGE)) return Optional.empty();
        return Optional.ofNullable(
                (Integer) ComponentType.encodeJava(
                        ComponentKeys.DAMAGE,
                        item.getComponent(ComponentKeys.DAMAGE)
                ).orElse(null)
        );
    }

    @Override
    protected void damage(ItemWrapper<ItemStack> item, Integer damage) {
        if (damage == null) damage = 0;
        item.setComponent(ComponentKeys.DAMAGE, damage);
    }

    @Override
    protected Optional<Integer> maxDamage(ItemWrapper<ItemStack> item) {
        if (!item.hasComponent(ComponentKeys.MAX_DAMAGE)) return Optional.of((int) item.getItem().getType().getMaxDurability());
        return Optional.ofNullable(
                (Integer) ComponentType.encodeJava(
                        ComponentKeys.MAX_DAMAGE,
                        item.getComponent(ComponentKeys.MAX_DAMAGE)
                ).orElse(null)
        );
    }

    @Override
    protected void maxDamage(ItemWrapper<ItemStack> item, Integer damage) {
        if (damage == null) {
            item.removeComponent(ComponentKeys.MAX_DAMAGE);
        } else {
            item.setComponent(ComponentKeys.MAX_DAMAGE, damage);
        }
    }

    @Override
    protected Optional<Enchantment> getEnchantment(ItemWrapper<ItemStack> item, Key key) {
        Object enchant = item.getComponent(ComponentKeys.ENCHANTMENTS);
        try {
            Map<String, Integer> map = EnchantmentUtils.toMap(enchant);
            Integer level = map.get(key.toString());
            if (level == null) return Optional.empty();
            return Optional.of(new Enchantment(key, level));
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to get enchantment " + key, e);
            return Optional.empty();
        }
    }

    @Override
    protected void enchantments(ItemWrapper<ItemStack> item, List<Enchantment> enchantments) {
        Map<String, Integer> enchants = new HashMap<>();
        for (Enchantment enchantment : enchantments) {
            enchants.put(enchantment.id().toString(), enchantment.level());
        }
        item.setComponent(ComponentKeys.ENCHANTMENTS, enchants);
    }

    @Override
    protected void storedEnchantments(ItemWrapper<ItemStack> item, List<Enchantment> enchantments) {
        Map<String, Integer> enchants = new HashMap<>();
        for (Enchantment enchantment : enchantments) {
            enchants.put(enchantment.id().toString(), enchantment.level());
        }
        item.setComponent(ComponentKeys.STORED_ENCHANTMENTS, enchants);
    }

    @Override
    protected void addEnchantment(ItemWrapper<ItemStack> item, Enchantment enchantment) {
        Object enchant = item.getComponent(ComponentKeys.ENCHANTMENTS);
        try {
            Map<String, Integer> map = EnchantmentUtils.toMap(enchant);
            map.put(enchantment.toString(), enchantment.level());
            item.setComponent(ComponentKeys.ENCHANTMENTS, map);
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to add enchantment", e);
        }
    }

    @Override
    protected void addStoredEnchantment(ItemWrapper<ItemStack> item, Enchantment enchantment) {
        Object enchant = item.getComponent(ComponentKeys.STORED_ENCHANTMENTS);
        try {
            Map<String, Integer> map = EnchantmentUtils.toMap(enchant);
            map.put(enchantment.toString(), enchantment.level());
            item.setComponent(ComponentKeys.STORED_ENCHANTMENTS, map);
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to add stored enchantment", e);
        }
    }

    @Override
    protected void itemFlags(ItemWrapper<ItemStack> item, List<String> flags) {
        throw new UnsupportedOperationException("This feature is not available on 1.20.5+");
    }

    @Override
    protected int maxStackSize(ItemWrapper<ItemStack> item) {
        if (!item.hasComponent(ComponentKeys.MAX_STACK_SIZE)) return item.getItem().getType().getMaxStackSize();
        return Optional.ofNullable((Integer) ComponentType.encodeJava(ComponentKeys.MAX_STACK_SIZE, item.getComponent(ComponentKeys.MAX_STACK_SIZE)).orElse(null))
                .orElse(item.getItem().getType().getMaxStackSize());
    }

    @Override
    protected void maxStackSize(ItemWrapper<ItemStack> item, Integer maxStackSize) {
        if (maxStackSize == null) {
            item.removeComponent(ComponentKeys.MAX_STACK_SIZE);
        } else {
            item.setComponent(ComponentKeys.MAX_STACK_SIZE, maxStackSize);
        }
    }

    @Override
    protected void repairCost(ItemWrapper<ItemStack> item, Integer data) {
        if (data == null) {
            item.removeComponent(ComponentKeys.REPAIR_COST);
        } else {
            item.setComponent(ComponentKeys.REPAIR_COST, data);
        }
    }

    @Override
    protected Optional<Integer> repairCost(ItemWrapper<ItemStack> item) {
        if (!item.hasComponent(ComponentKeys.REPAIR_COST)) return Optional.empty();
        return Optional.ofNullable((Integer) ComponentType.encodeJava(ComponentKeys.REPAIR_COST, item.getComponent(ComponentKeys.REPAIR_COST)).orElse(null));
    }

    @Override
    protected ItemWrapper<ItemStack> mergeCopy(ItemWrapper<ItemStack> item1, ItemWrapper<ItemStack> item2) {
        Object itemStack1 = item1.getLiteralObject();
        Object itemStack2 = item2.getLiteralObject();
        try {
            Object itemStack3 = Reflections.method$ItemStack$transmuteCopy.invoke(itemStack1, Reflections.method$ItemStack$getItem.invoke(itemStack2), 1);
            Reflections.method$ItemStack$applyComponents.invoke(itemStack3, Reflections.method$ItemStack$getComponentsPatch.invoke(itemStack2));
            return new RTagItemWrapper(new RtagItem(ItemObject.asCraftMirror(itemStack3)), item2.count());
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to merge item", e);
        }
        return null;
    }

    @Override
    protected void merge(ItemWrapper<ItemStack> item1, ItemWrapper<ItemStack> item2) {
        // load previous changes on nms items
        item1.load();
        Object itemStack1 = item1.getLiteralObject();
        Object itemStack2 = item2.getLiteralObject();
        try {
            Reflections.method$ItemStack$applyComponents.invoke(itemStack1, Reflections.method$ItemStack$getComponentsPatch.invoke(itemStack2));
        } catch (Exception e) {
            plugin.logger().warn("Failed to merge item", e);
        }
    }
}