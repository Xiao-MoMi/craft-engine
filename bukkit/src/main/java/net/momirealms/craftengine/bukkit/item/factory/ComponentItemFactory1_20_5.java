package net.momirealms.craftengine.bukkit.item.factory;

import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.item.ComponentItemWrapper;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistryOps;
import net.momirealms.craftengine.bukkit.util.EnchantmentUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.attribute.AttributeModifier;
import net.momirealms.craftengine.core.item.DataComponentKeys;
import net.momirealms.craftengine.core.item.ItemType;
import net.momirealms.craftengine.core.item.data.Enchantment;
import net.momirealms.craftengine.core.item.data.FireworkExplosion;
import net.momirealms.craftengine.core.item.data.Trim;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class ComponentItemFactory1_20_5 extends BukkitItemFactory<ComponentItemWrapper> {

    public ComponentItemFactory1_20_5(CraftEngine plugin) {
        super(plugin);
    }

    @Override
    protected ItemType type(ComponentItemWrapper item) {
        return item.itemType();
    }

    @Override
    protected void customId(ComponentItemWrapper item, Key id) {
        FastNMS.INSTANCE.setCustomItemId(item.getLiteralObject(), id.toString());
    }

    @Override
    protected Optional<Key> customId(ComponentItemWrapper item) {
        return Optional.ofNullable(FastNMS.INSTANCE.getCustomItemId(item.getLiteralObject())).map(Key::of);
    }

    @Override
    protected ComponentItemWrapper wrapInternal(ItemStack item) {
        return new ComponentItemWrapper(item);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object getJavaTag(ComponentItemWrapper item, Object... path) {
        Map<String, Object> rootMap = (Map<String, Object>) item.getJavaComponent(DataComponentTypes.CUSTOM_DATA).orElse(null);
        if (rootMap == null) return null;
        Object currentObj = rootMap;
        for (int i = 0; i < path.length; i++) {
            Object pathSegment = path[i];
            if (pathSegment == null) return null;
            currentObj = ((Map<String, Object>) currentObj).get(pathSegment.toString());
            if (currentObj == null) return null;
            if (i == path.length - 1) {
                return currentObj;
            }
            if (!(currentObj instanceof Map)) {
                return null;
            }
        }
        return currentObj;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected Object getExactTag(ComponentItemWrapper item, Object... path) {
        Object customData = getExactComponent(item, DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return null;
        Object currentTag = FastNMS.INSTANCE.method$CustomData$getUnsafe(customData);
        for (int i = 0; i < path.length; i++) {
            Object pathSegment = path[i];
            if (pathSegment == null) return null;
            currentTag = FastNMS.INSTANCE.method$CompoundTag$get(currentTag, path[i].toString());
            if (currentTag == null) return null;
            if (i == path.length - 1) {
                return currentTag;
            }
            if (!CoreReflections.clazz$CompoundTag.isInstance(currentTag)) {
                return null;
            }
        }
        return null;
    }

    @Override
    protected Tag getTag(ComponentItemWrapper item, Object... path) {
        CompoundTag rootTag = (CompoundTag) item.getSparrowNBTComponent(DataComponentTypes.CUSTOM_DATA).orElse(null);
        if (rootTag == null) return null;
        Tag currentTag = rootTag;
        for (int i = 0; i < path.length; i++) {
            Object pathSegment = path[i];
            if (pathSegment == null) return null;
            CompoundTag t = (CompoundTag) currentTag;
            currentTag = t.get(pathSegment.toString());
            if (currentTag == null) return null;
            if (i == path.length - 1) {
                return currentTag;
            }
            if (!(currentTag instanceof CompoundTag)) {
                return null;
            }
        }
        return currentTag;
    }

    @Override
    protected void setTag(ComponentItemWrapper item, Object value, Object... path) {
        Tag valueTag;
        if (value instanceof Tag tag) {
            valueTag = tag;
        } else if (value instanceof JsonElement je) {
            valueTag = MRegistryOps.JSON.convertTo(MRegistryOps.SPARROW_NBT, je);
        } else if (CoreReflections.clazz$Tag.isInstance(value)) {
            valueTag = MRegistryOps.NBT.convertTo(MRegistryOps.SPARROW_NBT, value);
        } else {
            assert MRegistryOps.JAVA != null;
            valueTag = MRegistryOps.JAVA.convertTo(MRegistryOps.SPARROW_NBT, value);
        }

        CompoundTag rootTag = (CompoundTag) item.getSparrowNBTComponent(DataComponentTypes.CUSTOM_DATA).orElseGet(CompoundTag::new);

        if (path == null || path.length == 0) {
            if (valueTag instanceof CompoundTag) {
                rootTag = (CompoundTag) valueTag;
            } else {
                throw new IllegalArgumentException("Cannot set non-CompoundTag as root without path");
            }
        } else {
            CompoundTag currentTag = rootTag;
            for (int i = 0; i < path.length - 1; i++) {
                Object pathSegment = path[i];
                if (pathSegment == null) throw new NullPointerException("Path segment cannot be null");

                String key = pathSegment.toString();
                Tag nextTag = currentTag.get(key);

                if (!(nextTag instanceof CompoundTag)) {
                    nextTag = new CompoundTag();
                    currentTag.put(key, nextTag);
                }
                currentTag = (CompoundTag) nextTag;
            }

            String finalKey = path[path.length - 1].toString();
            currentTag.put(finalKey, valueTag);
        }

        item.setSparrowNBTComponent(DataComponentTypes.CUSTOM_DATA, rootTag);
    }

    @Override
    protected boolean hasTag(ComponentItemWrapper item, Object... path) {
        return getTag(item, path) != null;
    }

    @Override
    protected boolean removeTag(ComponentItemWrapper item, Object... path) {
        CompoundTag rootTag = (CompoundTag) item.getSparrowNBTComponent(DataComponentTypes.CUSTOM_DATA).orElse(null);
        if (rootTag == null || path == null || path.length == 0) return false;

        if (path.length == 1) {
            String key = path[0].toString();
            if (rootTag.containsKey(key)) {
                rootTag.remove(key);
                item.setSparrowNBTComponent(DataComponentTypes.CUSTOM_DATA, rootTag);
                return true;
            }
            return false;
        }

        CompoundTag parentTag = rootTag;
        for (int i = 0; i < path.length - 1; i++) {
            Object pathSegment = path[i];
            if (pathSegment == null) return false;

            String key = pathSegment.toString();
            Tag childTag = parentTag.get(key);

            if (!(childTag instanceof CompoundTag)) {
                return false;
            }
            parentTag = (CompoundTag) childTag;
        }

        String finalKey = path[path.length - 1].toString();
        if (parentTag.containsKey(finalKey)) {
            parentTag.remove(finalKey);
            item.setSparrowNBTComponent(DataComponentTypes.CUSTOM_DATA, rootTag);
            return true;
        }
        return false;
    }

    @Override
    protected Optional<String> tooltipStyle(ComponentItemWrapper item) {
        throw new UnsupportedOperationException("This feature is not available on 1.21.2+");
    }

    @Override
    protected void tooltipStyle(ComponentItemWrapper item, String data) {
        throw new UnsupportedOperationException("This feature is not available on 1.21.2+");
    }

    @Override
    protected void setComponent(ComponentItemWrapper item, Object type, Object value) {
        item.setComponent(type, value);
    }

    @Override
    protected void setJavaComponent(ComponentItemWrapper item, Object type, Object value) {
        item.setJavaComponent(type, value);
    }

    @Override
    protected void setJsonComponent(ComponentItemWrapper item, Object type, JsonElement value) {
        item.setJsonComponent(type, value);
    }

    @Override
    protected void setNBTComponent(ComponentItemWrapper item, Object type, Tag value) {
        item.setSparrowNBTComponent(type, value);
    }

    @Override
    protected void resetComponent(ComponentItemWrapper item, Object type) {
        item.resetComponent(type);
    }

    @Override
    protected Object getExactComponent(ComponentItemWrapper item, Object type) {
        return item.getExactComponent(type);
    }

    @Override
    protected void setExactComponent(ComponentItemWrapper item, Object type, Object value) {
        item.setExactComponent(type, value);
    }

    @Override
    protected Object getJavaComponent(ComponentItemWrapper item, Object type) {
        return item.getJavaComponent(type).orElse(null);
    }

    @Override
    protected JsonElement getJsonComponent(ComponentItemWrapper item, Object type) {
        return item.getJsonComponent(type).orElse(null);
    }

    @Override
    public Object getNBTComponent(ComponentItemWrapper item, Object type) {
        return item.getNBTComponent(type).orElse(null);
    }

    @Override
    protected Tag getSparrowNBTComponent(ComponentItemWrapper item, Object type) {
        return item.getSparrowNBTComponent(type).orElse(null);
    }

    @Override
    protected boolean hasComponent(ComponentItemWrapper item, Object type) {
        return item.hasComponent(type);
    }

    @Override
    protected boolean hasNonDefaultComponent(ComponentItemWrapper item, Object type) {
        return item.hasNonDefaultComponent(type);
    }

    @Override
    protected void removeComponent(ComponentItemWrapper item, Object type) {
        item.removeComponent(type);
    }

    @Override
    protected void customModelData(ComponentItemWrapper item, Integer data) {
        if (data == null) {
            item.resetComponent(DataComponentTypes.CUSTOM_MODEL_DATA);
        } else {
            item.setJavaComponent(DataComponentTypes.CUSTOM_MODEL_DATA, data);
        }
    }

    @Override
    protected Optional<Integer> customModelData(ComponentItemWrapper item) {
        return item.getJavaComponent(DataComponentTypes.CUSTOM_MODEL_DATA);
    }

    @Override
    protected void customNameJson(ComponentItemWrapper item, String json) {
        if (json == null) {
            item.resetComponent(DataComponentTypes.CUSTOM_NAME);
        } else {
            item.setJavaComponent(DataComponentTypes.CUSTOM_NAME, json);
        }
    }

    @Override
    protected Optional<String> customNameJson(ComponentItemWrapper item) {
        return item.getJavaComponent(DataComponentTypes.CUSTOM_NAME);
    }

    @Override
    protected void itemNameJson(ComponentItemWrapper item, String json) {
        if (json == null) {
            item.resetComponent(DataComponentTypes.ITEM_NAME);
        } else {
            item.setJavaComponent(DataComponentTypes.ITEM_NAME, json);
        }
    }

    @Override
    protected Optional<String> itemNameJson(ComponentItemWrapper item) {
        return item.getJavaComponent(DataComponentTypes.ITEM_NAME);
    }

    @Override
    protected void skull(ComponentItemWrapper item, String skullData) {
        if (skullData == null) {
            item.resetComponent(DataComponentTypes.PROFILE);
        } else {
            Map<String, Object> profile = Map.of("properties", List.of(Map.of("name", "textures", "value", skullData)));
            item.setJavaComponent(DataComponentTypes.PROFILE, profile);
        }
    }

    @Override
    protected Optional<List<String>> loreJson(ComponentItemWrapper item) {
        return item.getJavaComponent(DataComponentTypes.LORE);
    }

    @Override
    protected void loreJson(ComponentItemWrapper item, List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            item.resetComponent(DataComponentTypes.LORE);
        } else {
            item.setJavaComponent(DataComponentTypes.LORE, lore);
        }
    }

    @Override
    protected boolean unbreakable(ComponentItemWrapper item) {
        return item.hasComponent(DataComponentTypes.UNBREAKABLE);
    }

    @Override
    protected void unbreakable(ComponentItemWrapper item, boolean unbreakable) {
        if (unbreakable) {
            item.setJavaComponent(DataComponentTypes.UNBREAKABLE, Map.of());
        } else {
            item.resetComponent(DataComponentTypes.UNBREAKABLE);
        }
    }

    @Override
    protected Optional<Boolean> glint(ComponentItemWrapper item) {
        return Optional.ofNullable((Boolean) item.getExactComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE));
    }

    @Override
    protected void glint(ComponentItemWrapper item, Boolean glint) {
        if (glint == null) {
            item.resetComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
        } else {
            item.setJavaComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, glint);
        }
    }

    @Override
    protected Optional<Integer> damage(ComponentItemWrapper item) {
        return item.getJavaComponent(DataComponentTypes.DAMAGE);
    }

    @Override
    protected void damage(ComponentItemWrapper item, Integer damage) {
        if (damage == null) {
            item.resetComponent(DataComponentTypes.DAMAGE);
        } else {
            item.setJavaComponent(DataComponentTypes.DAMAGE, damage);
        }
    }

    @Override
    protected Optional<Color> dyedColor(ComponentItemWrapper item) {
        if (!item.hasComponent(DataComponentTypes.DYED_COLOR)) return Optional.empty();
        Object javaObj = getJavaComponent(item, DataComponentTypes.DYED_COLOR);
        if (javaObj instanceof Integer integer) {
            return Optional.of(Color.fromDecimal(integer));
        } else if (javaObj instanceof Map<?, ?> map) {
            return Optional.of(Color.fromDecimal((int) map.get("rgb")));
        }
        return Optional.empty();
    }

    @Override
    protected void dyedColor(ComponentItemWrapper item, Color color) {
        if (color == null) {
            item.resetComponent(DataComponentTypes.DYED_COLOR);
        } else {
            item.setJavaComponent(DataComponentTypes.DYED_COLOR, color.color());
        }
    }

    @Override
    protected int maxDamage(ComponentItemWrapper item) {
        Optional<Integer> damage = item.getJavaComponent(DataComponentTypes.MAX_DAMAGE);
        return damage.orElseGet(() -> (int) item.getItem().getType().getMaxDurability());
    }

    @Override
    protected void maxDamage(ComponentItemWrapper item, Integer damage) {
        if (damage == null) {
            item.resetComponent(DataComponentTypes.MAX_DAMAGE);
        } else {
            item.setJavaComponent(DataComponentTypes.MAX_DAMAGE, damage);
        }
    }

    @Override
    protected Optional<Enchantment> getEnchantment(ComponentItemWrapper item, Key key) {
        Object enchant = item.getExactComponent(DataComponentTypes.ENCHANTMENTS);
        if (enchant == null) return Optional.empty();
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
    protected void enchantments(ComponentItemWrapper item, List<Enchantment> enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            item.resetComponent(DataComponentTypes.ENCHANTMENTS);
        } else {
            Map<String, Integer> enchants = new HashMap<>();
            for (Enchantment enchantment : enchantments) {
                enchants.put(enchantment.id().toString(), enchantment.level());
            }
            item.setJavaComponent(DataComponentTypes.ENCHANTMENTS, enchants);
        }
    }

    @Override
    protected Optional<List<Enchantment>> enchantments(ComponentItemWrapper item) {
        Object exactEnchantments = item.getExactComponent(DataComponentTypes.ENCHANTMENTS);
        if (exactEnchantments == null) return Optional.empty();
        try {
            return Optional.of(EnchantmentUtils.toList(exactEnchantments));
        } catch (ReflectiveOperationException e) {
            this.plugin.logger().warn("Failed to get enchantments " + exactEnchantments, e);
            return Optional.empty();
        }
    }

    @Override
    protected Optional<List<Enchantment>> storedEnchantments(ComponentItemWrapper item) {
        Object exactEnchantments = item.getExactComponent(DataComponentTypes.STORED_ENCHANTMENTS);
        if (exactEnchantments == null) return Optional.empty();
        try {
            return Optional.of(EnchantmentUtils.toList(exactEnchantments));
        } catch (ReflectiveOperationException e) {
            this.plugin.logger().warn("Failed to get stored enchantments " + exactEnchantments, e);
            return Optional.empty();
        }
    }

    @Override
    protected void storedEnchantments(ComponentItemWrapper item, List<Enchantment> enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            item.resetComponent(DataComponentTypes.STORED_ENCHANTMENTS);
        } else {
            Map<String, Integer> enchants = new HashMap<>();
            for (Enchantment enchantment : enchantments) {
                enchants.put(enchantment.id().toString(), enchantment.level());
            }
            item.setJavaComponent(DataComponentTypes.STORED_ENCHANTMENTS, enchants);
        }
    }

    @Override
    protected void itemFlags(ComponentItemWrapper item, List<String> flags) {
        throw new UnsupportedOperationException("This feature is not available on 1.20.5+");
    }

    @Override
    protected int maxStackSize(ComponentItemWrapper item) {
        Optional<Integer> stackSize = item.getJavaComponent(DataComponentTypes.MAX_STACK_SIZE);
        return stackSize.orElseGet(() -> item.getItem().getType().getMaxStackSize());
    }

    @Override
    protected void maxStackSize(ComponentItemWrapper item, Integer maxStackSize) {
        if (maxStackSize == null) {
            item.resetComponent(DataComponentTypes.MAX_STACK_SIZE);
        } else {
            item.setJavaComponent(DataComponentTypes.MAX_STACK_SIZE, maxStackSize);
        }
    }

    @Override
    protected void repairCost(ComponentItemWrapper item, Integer data) {
        if (data == null) {
            item.resetComponent(DataComponentTypes.REPAIR_COST);
        } else {
            item.setJavaComponent(DataComponentTypes.REPAIR_COST, data);
        }
    }

    @Override
    protected Optional<Integer> repairCost(ComponentItemWrapper item) {
        return item.getJavaComponent(DataComponentTypes.REPAIR_COST);
    }

    @Override
    protected void trim(ComponentItemWrapper item, Trim trim) {
        if (trim == null) {
            item.resetComponent(DataComponentTypes.TRIM);
        } else {
            try {
                item.setJavaComponent(DataComponentTypes.TRIM, Map.of(
                        "pattern", trim.pattern().asString(),
                        "material", trim.material().asString()
                ));
            } catch (Exception e) {
                // 预防未启用基于纹饰盔甲时，锁链甲可能产生的网络问题（由用户配置决定）
                if (!trim.material().equals(Key.of("minecraft", "custom")) && !trim.pattern().equals(Key.of("minecraft", "chainmail"))) {
                    this.plugin.logger().warn("Failed to apply trim " + trim, e);
                }
            }
        }
    }

    @Override
    protected Optional<Trim> trim(ComponentItemWrapper item) {
        Optional<Object> trim = item.getJavaComponent(DataComponentTypes.TRIM);
        if (trim.isEmpty()) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        Map<String, String> trimMap = (Map<String, String>) trim.get();
        return Optional.of(new Trim(Key.of(trimMap.get("pattern")), Key.of(trimMap.get("material"))));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Optional<FireworkExplosion> fireworkExplosion(ComponentItemWrapper item) {
        Optional<Object> optionalExplosion = item.getJavaComponent(DataComponentTypes.FIREWORK_EXPLOSION);
        if (optionalExplosion.isEmpty()) return Optional.empty();
        Map<String, Object> explosions = MiscUtils.castToMap(optionalExplosion.get(), false);
        FireworkExplosion.Shape shape = Optional.ofNullable(FireworkExplosion.Shape.byName((String) explosions.get("shape"))).orElse(FireworkExplosion.Shape.SMALL_BALL);
        boolean hasTrail = (boolean) explosions.getOrDefault("has_trail", false);
        boolean hasTwinkler = (boolean) explosions.getOrDefault("has_twinkle", false);
        List<Integer> colors = (List<Integer>) Optional.ofNullable(explosions.get("colors")).orElse(new IntArrayList());
        List<Integer> fadeColors = (List<Integer>) Optional.ofNullable(explosions.get("fade_colors")).orElse(new IntArrayList());
        return Optional.of(new FireworkExplosion(
                shape,
                new IntArrayList(colors),
                new IntArrayList(fadeColors),
                hasTrail,
                hasTwinkler
        ));
    }

    @Override
    protected void fireworkExplosion(ComponentItemWrapper item, FireworkExplosion explosion) {
        if (explosion == null) {
            item.resetComponent(DataComponentTypes.FIREWORK_EXPLOSION);
        } else {
            item.setJavaComponent(DataComponentTypes.FIREWORK_EXPLOSION, Map.of(
                    "shape", explosion.shape().getName(),
                    "has_trail", explosion.hasTrail(),
                    "has_twinkle", explosion.hasTwinkle(),
                    "colors", explosion.colors(),
                    "fade_colors", explosion.fadeColors()
            ));
        }
    }

    @Override
    protected ComponentItemWrapper mergeCopy(ComponentItemWrapper item1, ComponentItemWrapper item2) {
        Object itemStack1 = item1.getLiteralObject();
        Object itemStack2 = item2.getLiteralObject();
        Object itemStack3 = FastNMS.INSTANCE.method$ItemStack$transmuteCopy(itemStack1, FastNMS.INSTANCE.method$ItemStack$getItem(itemStack2), item2.count());
        FastNMS.INSTANCE.method$ItemStack$applyComponents(itemStack3, FastNMS.INSTANCE.method$ItemStack$getComponentsPatch(itemStack2));
        return new ComponentItemWrapper(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(itemStack3));
    }

    @Override
    protected void merge(ComponentItemWrapper item1, ComponentItemWrapper item2) {
        Object itemStack1 = item1.getLiteralObject();
        Object itemStack2 = item2.getLiteralObject();
        try {
            FastNMS.INSTANCE.method$ItemStack$applyComponents(itemStack1, FastNMS.INSTANCE.method$ItemStack$getComponentsPatch(itemStack2));
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to merge item", e);
        }
    }

    @Override
    protected ComponentItemWrapper transmuteCopy(ComponentItemWrapper item, Key newItem, int amount) {
        Object itemStack1 = item.getLiteralObject();
        Object itemStack2 = FastNMS.INSTANCE.method$ItemStack$transmuteCopy(itemStack1, FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.ITEM, KeyUtils.toResourceLocation(newItem)), amount);
        return new ComponentItemWrapper(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(itemStack2));
    }

    @Override
    protected ComponentItemWrapper unsafeTransmuteCopy(ComponentItemWrapper item, Object newItem, int amount) {
        Object itemStack1 = item.getLiteralObject();
        Object itemStack2 = FastNMS.INSTANCE.method$ItemStack$transmuteCopy(itemStack1, newItem, amount);
        return new ComponentItemWrapper(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(itemStack2));
    }

    @Override
    protected void attributeModifiers(ComponentItemWrapper item, List<AttributeModifier> modifierList) {
        CompoundTag compoundTag = (CompoundTag) item.getSparrowNBTComponent(DataComponentKeys.ATTRIBUTE_MODIFIERS).orElseGet(CompoundTag::new);
        ListTag modifiers = new ListTag();
        compoundTag.put("modifiers", modifiers);
        for (AttributeModifier modifier : modifierList) {
            CompoundTag modifierTag = new CompoundTag();
            modifierTag.putString("type", modifier.type());
            modifierTag.putString("slot", modifier.slot().name().toLowerCase(Locale.ENGLISH));
            if (VersionHelper.isOrAbove1_21()) {
                modifierTag.putString("id", modifier.id().toString());
            } else {
                modifierTag.putIntArray("uuid", UUIDUtils.uuidToIntArray(UUID.nameUUIDFromBytes(modifier.id().toString().getBytes(StandardCharsets.UTF_8))));
                modifierTag.putString("name", modifier.id().toString());
            }
            modifierTag.putDouble("amount", modifier.amount());
            modifierTag.putString("operation", modifier.operation().id());
            modifiers.add(modifierTag);
        }
        item.setSparrowNBTComponent(DataComponentKeys.ATTRIBUTE_MODIFIERS, compoundTag);
    }

    @Override
    protected Optional<Map<String, String>> blockState(ComponentItemWrapper item) {
        return item.getJavaComponent(DataComponentTypes.BLOCK_STATE);
    }

    @Override
    protected void blockState(ComponentItemWrapper item, Map<String, String> state) {
        item.setJavaComponent(DataComponentTypes.BLOCK_STATE, state);
    }
}