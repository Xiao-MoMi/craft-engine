package net.momirealms.craftengine.core.item;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.attribute.AttributeModifier;
import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.component.value.Enchantment;
import net.momirealms.craftengine.core.item.component.value.FireworkExplosion;
import net.momirealms.craftengine.core.item.component.value.JukeboxPlayable;
import net.momirealms.craftengine.core.item.component.value.Trim;
import net.momirealms.craftengine.core.item.customdata.CustomDataSerializer;
import net.momirealms.craftengine.core.item.customdata.CustomDataSerializers;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.setting.value.EquipmentData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface representing an item.
 * This interface provides methods for managing item properties such as custom model data,
 * damage, display name, lore, enchantments, and tags.
 */
public interface Item {

    static Item empty() {
        return CraftEngine.instance().itemManager().emptyItem();
    }

    static Item fromNBT(final CompoundTag tag) {
        return CraftEngine.instance().itemManager().fromNBT(tag);
    }

    static Item fromBytes(final byte[] bytes) {
        return CraftEngine.instance().itemManager().fromBytes(bytes);
    }

    Object minecraftItem();

    default Object platformItem() {
        return minecraftItem();
    }

    ItemType type();

    boolean isEmpty();

    Optional<ItemDefinition> getDefinition();

    Optional<ItemBehavior> getBehavior();

    boolean isCustomItem();

    boolean isBlockItem();

    @NotNull
    Key id();

    @NotNull
    Key vanillaId();

    Optional<Key> customId();

    Item customId(Key id);

    int count();

    Item count(int amount);

    Item trim(Trim trim);

    Optional<Trim> trim();

    Item customModelData(Integer data);

    Optional<Integer> customModelData();

    Item damage(Integer data);

    Optional<Integer> damage();

    Item repairCost(Integer data);

    Optional<Integer> repairCost();

    Item maxDamage(Integer data);

    int maxDamage();

    Item blockState(Map<String, String> state);

    Optional<Map<String, String>> blockState();

    Item dyedColor(Color data);

    Optional<Color> dyedColor();

    Item fireworkExplosion(FireworkExplosion explosion);

    Optional<FireworkExplosion> fireworkExplosion();

    Item customNameJson(String displayName);

    Item customNameComponent(Component displayName);

    Optional<String> customNameJson();

    Optional<Component> customNameComponent();

    default Optional<String> hoverNameJson() {
        return customNameJson().or(this::itemNameJson);
    }

    default Optional<Component> hoverNameComponent() {
        return customNameComponent().or(this::itemNameComponent);
    }

    Item itemNameJson(String itemName);

    Item itemNameComponent(Component itemName);

    Optional<String> itemNameJson();

    Optional<Component> itemNameComponent();

    Item itemModel(String itemModel);

    Optional<String> itemModel();

    Item useRemainder(Item item, int count);

    Optional<Item> useRemainder();

    Item tooltipStyle(String tooltipStyle);

    Optional<String> tooltipStyle();

    Item loreJson(List<String> lore);

    Item loreComponent(List<Component> lore);

    Optional<List<String>> loreJson();

    Optional<List<Component>> loreComponent();

    Item attributeModifiers(List<AttributeModifier> modifiers);

    Optional<JukeboxPlayable> jukeboxSong();

    Item jukeboxSong(JukeboxPlayable song);

    Optional<EquipmentData> equippable();

    Item equippable(EquipmentData equipmentData);

    Item unbreakable(boolean unbreakable);

    boolean unbreakable();

    Item skull(String data);

    Optional<Enchantment> getEnchantment(Key enchantmentId);

    Optional<List<Enchantment>> enchantments();

    Optional<List<Enchantment>> storedEnchantments();

    Item setEnchantments(List<Enchantment> enchantments);

    Item setStoredEnchantments(List<Enchantment> enchantments);

    Optional<Boolean> glint();

    Item glint(boolean value);

    Item itemFlags(List<String> flags);

    Tag getSparrowTag(Object... path);

    Object getMinecraftTag(Object... path);

    JsonElement getTagAsJson(Object... path);

    Object getTagAsJava(Object... path);

    Item setTag(Object value, Object... path);

    Item setSparrowTag(Tag value, Object... path);

    Item setJavaTag(Object value, Object... path);

    Item setMinecraftTag(Object value, Object... path);

    Item setJsonTag(JsonElement value, Object... path);

    @SuppressWarnings({"rawtypes", "unchecked"})
    default void setCustomData(Object value, Object... path) {
        CustomDataSerializer serializer = CustomDataSerializers.getSerializer(value.getClass());
        if (serializer == null) {
            throw new IllegalArgumentException("Custom data serializer not supported: " + value.getClass());
        }
        Tag tag = serializer.serialize(value);
        setSparrowTag(tag, path);
    }

    @Nullable
    default <T> T getCustomData(Class<T> clazz, Object... path) {
        CustomDataSerializer<T> serializer = CustomDataSerializers.getSerializer(clazz);
        if (serializer == null) {
            throw new IllegalArgumentException("Custom data serializer not supported: " + clazz);
        }
        Tag sparrowTag = getSparrowTag(path);
        if (sparrowTag == null) {
            return null;
        }
        return serializer.deserialize(sparrowTag);
    }

    boolean hasTag(Object... path);

    boolean removeTag(Object... path);

    boolean hasComponent(Object type);

    boolean hasNonDefaultComponent(Object type);

    void removeComponent(Object type);

    Object getExactComponent(Object type);

    Object getComponentAsJava(Object type);

    JsonElement getComponentAsJson(Object type);

    Tag getComponentAsSparrowTag(Object type);

    Object getComponentAsMinecraftTag(Object type);

    void setComponent(Object type, Object value);

    void setExactComponent(Object type, Object value);

    void setJavaComponent(Object type, Object value);

    void setJsonComponent(Object type, JsonElement value);

    void setSparrowTagComponent(Object type, Tag value);

    void setMinecraftTagComponent(Object type, Object value);

    void resetComponent(Object type);

    int maxStackSize();

    Item maxStackSize(int amount);

    Item copy();

    Item copyWithCount(int count);

    boolean hasItemTag(Key itemTag);

    Item mergeCopy(Item another);

    Item transmuteCopy(Key another, int count);

    Item unsafeTransmuteCopy(Object another, int count);

    void shrink(int amount);

    void grow(int amount);

    void hurtAndBreak(int amount, @NotNull Player player, @Nullable EquipmentSlot slot);

    default Item transmuteCopy(Key another) {
        return transmuteCopy(another, this.count());
    }

    void merge(Item another);

    default Item apply(ItemProcessor modifier, ItemBuildContext context) {
        return modifier.apply(this, context);
    }

    byte[] toBytes();

    CompoundTag toNBT();

    boolean isSimilar(Item another);

    default Item applyDyedColors(List<Color> colors) {
        int totalRed = 0;
        int totalGreen = 0;
        int totalBlue = 0;
        int totalMaxComponent = 0;
        int colorCount = 0;
        Optional<Color> existingColor = dyedColor();
        existingColor.ifPresent(colors::add);
        for (Color color : colors) {
            int dyeRed = color.r();
            int dyeGreen = color.g();
            int dyeBlue = color.b();
            totalMaxComponent += Math.max(dyeRed, Math.max(dyeGreen, dyeBlue));
            totalRed += dyeRed;
            totalGreen += dyeGreen;
            totalBlue += dyeBlue;
            ++colorCount;
        }
        int avgRed = totalRed / colorCount;
        int avgGreen = totalGreen / colorCount;
        int avgBlue = totalBlue / colorCount;
        float avgMaxComponent = (float) totalMaxComponent / (float)colorCount;
        float currentMaxComponent = (float) Math.max(avgRed, Math.max(avgGreen, avgBlue));
        avgRed = (int) ((float) avgRed * avgMaxComponent / currentMaxComponent);
        avgGreen = (int) ((float) avgGreen * avgMaxComponent / currentMaxComponent);
        avgBlue = (int) ((float) avgBlue * avgMaxComponent / currentMaxComponent);
        Color finalColor = new Color(0, avgRed, avgGreen, avgBlue);
        return dyedColor(finalColor);
    }
}
