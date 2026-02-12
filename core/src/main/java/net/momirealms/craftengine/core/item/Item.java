package net.momirealms.craftengine.core.item;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.attribute.AttributeModifier;
import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.data.Enchantment;
import net.momirealms.craftengine.core.item.data.FireworkExplosion;
import net.momirealms.craftengine.core.item.data.JukeboxPlayable;
import net.momirealms.craftengine.core.item.data.Trim;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.setting.EquipmentData;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
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
 *
 * @param <I> the type of the item implementation
 */
public interface Item<I> {

    ItemType type();

    boolean isEmpty();

    Optional<CustomItem<I>> getCustomItem();

    Optional<List<ItemBehavior>> getItemBehavior();

    boolean isCustomItem();

    boolean isBlockItem();

    @NotNull
    Key id();

    @NotNull
    Key vanillaId();

    @Nullable
    UniqueKey recipeIngredientId();

    Optional<Key> customId();

    Item<I> customId(Key id);

    int count();

    Item<I> count(int amount);

    Item<I> trim(Trim trim);

    Optional<Trim> trim();

    Item<I> customModelData(Integer data);

    Optional<Integer> customModelData();

    Item<I> damage(Integer data);

    Optional<Integer> damage();

    Item<I> repairCost(Integer data);

    Optional<Integer> repairCost();

    Item<I> maxDamage(Integer data);

    int maxDamage();

    Item<I> blockState(Map<String, String> state);

    Optional<Map<String, String>> blockState();

    // todo 考虑部分版本的show in tooltip保留
    Item<I> dyedColor(Color data);

    Optional<Color> dyedColor();

    Item<I> fireworkExplosion(FireworkExplosion explosion);

    Optional<FireworkExplosion> fireworkExplosion();

    Item<I> customNameJson(String displayName);

    Item<I> customNameComponent(Component displayName);

    Optional<String> customNameJson();

    Optional<Component> customNameComponent();

    default Optional<String> hoverNameJson() {
        return customNameJson().or(this::itemNameJson);
    }

    default Optional<Component> hoverNameComponent() {
        return customNameComponent().or(this::itemNameComponent);
    }

    Item<I> itemNameJson(String itemName);

    Item<I> itemNameComponent(Component itemName);

    Optional<String> itemNameJson();

    Optional<Component> itemNameComponent();

    Item<I> itemModel(String itemModel);

    Optional<String> itemModel();

    Item<I> tooltipStyle(String tooltipStyle);

    Optional<String> tooltipStyle();

    Item<I> loreJson(List<String> lore);

    Item<I> loreComponent(List<Component> lore);

    Optional<List<String>> loreJson();

    Optional<List<Component>> loreComponent();

    Item<I> attributeModifiers(List<AttributeModifier> modifiers);

    Optional<JukeboxPlayable> jukeboxSong();

    Item<I> jukeboxSong(JukeboxPlayable song);

    Optional<EquipmentData> equippable();

    Item<I> equippable(EquipmentData equipmentData);

    Item<I> unbreakable(boolean unbreakable);

    boolean unbreakable();

    Item<I> skull(String data);

    Optional<Enchantment> getEnchantment(Key enchantmentId);

    Optional<List<Enchantment>> enchantments();

    Optional<List<Enchantment>> storedEnchantments();

    Item<I> setEnchantments(List<Enchantment> enchantments);

    Item<I> setStoredEnchantments(List<Enchantment> enchantments);

    Item<I> itemFlags(List<String> flags);

    Object getJavaTag(Object... path);

    Tag getTag(Object... path);

    Object getExactTag(Object... path);

    Item<I> setTag(Object value, Object... path);

    boolean hasTag(Object... path);

    boolean removeTag(Object... path);

    boolean hasComponent(Object type);

    boolean hasNonDefaultComponent(Object type);

    void removeComponent(Object type);

    void setExactComponent(Object type, Object value);

    Object getExactComponent(Object type);

    Object getJavaComponent(Object type);

    JsonElement getJsonComponent(Object type);

    Tag getSparrowNBTComponent(Object type);

    Object getNBTComponent(Object type);

    void setComponent(Object type, Object value);

    void setJavaComponent(Object type, Object value);

    void setJsonComponent(Object type, JsonElement value);

    void setNBTComponent(Object type, Tag value);

    void resetComponent(Object type);

    I getItem();

    int maxStackSize();

    Item<I> maxStackSize(int amount);

    Item<I> copyWithCount(int count);

    boolean hasItemTag(Key itemTag);

    Object getLiteralObject();

    Item<I> mergeCopy(Item<?> another);

    Item<I> transmuteCopy(Key another, int count);

    Item<I> unsafeTransmuteCopy(Object another, int count);

    void shrink(int amount);

    void hurtAndBreak(int amount, @Nullable Player player, @Nullable EquipmentSlot slot);

    default Item<I> transmuteCopy(Key another) {
        return transmuteCopy(another, this.count());
    }

    void merge(Item<I> another);

    default Item<I> apply(ItemProcessor modifier, ItemBuildContext context) {
        return modifier.apply(this, context);
    }

    byte[] toByteArray();

    default Item<I> applyDyedColors(List<Color> colors) {
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
