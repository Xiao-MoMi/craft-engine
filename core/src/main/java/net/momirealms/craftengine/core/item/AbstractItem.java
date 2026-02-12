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

public class AbstractItem<W extends ItemWrapper<I>, I> implements Item<I> {
    private final ItemFactory<W, I> factory;
    private final W item;

    AbstractItem(ItemFactory<W, I> factory, W item) {
        this.factory = factory;
        this.item = item;
    }

    @Override
    public boolean isEmpty() {
        return this.factory.isEmpty(this.item);
    }

    @Override
    public ItemType type() {
        return this.factory.type(this.item);
    }

    @Override
    public Item<I> itemModel(String data) {
        this.factory.itemModel(this.item, data);
        return this;
    }

    @Override
    public Optional<String> itemModel() {
        return this.factory.itemModel(this.item);
    }

    @Override
    public Optional<JukeboxPlayable> jukeboxSong() {
        return this.factory.jukeboxSong(this.item);
    }

    @Override
    public Item<I> jukeboxSong(JukeboxPlayable data) {
        this.factory.jukeboxSong(this.item, data);
        return this;
    }

    @Override
    public Optional<EquipmentData> equippable() {
        return this.factory.equippable(this.item);
    }

    @Override
    public Item<I> equippable(EquipmentData data) {
        this.factory.equippable(this.item, data);
        return this;
    }

    @Override
    public Item<I> tooltipStyle(String data) {
        this.factory.tooltipStyle(this.item, data);
        return this;
    }

    @Override
    public Optional<String> tooltipStyle() {
        return this.factory.tooltipStyle(this.item);
    }

    @Override
    public Item<I> damage(Integer data) {
        this.factory.damage(this.item, data);
        return this;
    }

    @Override
    public Optional<Integer> damage() {
        return this.factory.damage(this.item);
    }

    @Override
    public Item<I> repairCost(Integer data) {
        this.factory.repairCost(this.item, data);
        return this;
    }

    @Override
    public Optional<Integer> repairCost() {
        return this.factory.repairCost(this.item);
    }

    @Override
    public Item<I> maxDamage(Integer data) {
        this.factory.maxDamage(this.item, data);
        return this;
    }

    @Override
    public int maxDamage() {
        return this.factory.maxDamage(this.item);
    }

    @Override
    public Item<I> blockState(Map<String, String> state) {
        this.factory.blockState(this.item, state);
        return this;
    }

    @Override
    public Optional<Map<String, String>> blockState() {
        return this.factory.blockState(this.item);
    }

    @Override
    public Item<I> dyedColor(Color data) {
        this.factory.dyedColor(this.item, data);
        return this;
    }

    @Override
    public Optional<Color> dyedColor() {
        return this.factory.dyedColor(this.item);
    }

    @Override
    public Item<I> fireworkExplosion(FireworkExplosion explosion) {
        this.factory.fireworkExplosion(this.item, explosion);
        return this;
    }

    @Override
    public Optional<FireworkExplosion> fireworkExplosion() {
        return this.factory.fireworkExplosion(this.item);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<CustomItem<I>> getCustomItem() {
        return ((ItemManager<I>) factory.plugin.itemManager()).getCustomItem(id());
    }

    @Override
    public Optional<List<ItemBehavior>> getItemBehavior() {
        return factory.plugin.itemManager().getItemBehavior(id());
    }

    @Override
    public boolean isCustomItem() {
        return factory.plugin.itemManager().getCustomItem(id()).isPresent();
    }

    @Override
    public boolean isBlockItem() {
        return factory.isBlockItem(item);
    }

    @Override
    public @NotNull Key id() {
        return this.factory.id(this.item);
    }

    @Override
    public @NotNull Key vanillaId() {
        return this.factory.vanillaId(this.item);
    }

    @Override
    public @Nullable UniqueKey recipeIngredientId() {
        return this.factory.recipeIngredientID(this.item);
    }

    @Override
    public Optional<Key> customId() {
        return this.factory.customId(this.item);
    }

    @Override
    public Item<I> customId(Key data) {
        this.factory.customId(this.item, data);
        return this;
    }

    @Override
    public int count() {
        return this.item.count();
    }

    @Override
    public Item<I> count(int amount) {
        this.item.count(amount);
        return this;
    }

    @Override
    public Item<I> trim(Trim trim) {
        this.factory.trim(this.item, trim);
        return this;
    }

    @Override
    public Optional<Trim> trim() {
        return this.factory.trim(this.item);
    }

    @Override
    public Item<I> customModelData(Integer data) {
        this.factory.customModelData(this.item, data);
        return this;
    }

    @Override
    public Optional<Integer> customModelData() {
        return this.factory.customModelData(this.item);
    }

    @Override
    public Optional<String> customNameJson() {
        return this.factory.customNameJson(this.item);
    }

    @Override
    public Item<I> customNameJson(String displayName) {
        this.factory.customNameJson(this.item, displayName);
        return this;
    }

    @Override
    public Optional<Component> customNameComponent() {
        return this.factory.customNameComponent(this.item);
    }

    @Override
    public Item<I> customNameComponent(Component displayName) {
        this.factory.customNameComponent(this.item, displayName);
        return this;
    }

    @Override
    public Item<I> loreJson(List<String> lore) {
        this.factory.loreJson(this.item, lore);
        return this;
    }

    @Override
    public Optional<List<String>> loreJson() {
        return this.factory.loreJson(this.item);
    }

    @Override
    public Item<I> loreComponent(List<Component> lore) {
        this.factory.loreComponent(this.item, lore);
        return this;
    }

    @Override
    public Optional<List<Component>> loreComponent() {
        return this.factory.loreComponent(this.item);
    }

    @Override
    public Item<I> attributeModifiers(List<AttributeModifier> modifiers) {
        this.factory.attributeModifiers(this.item, modifiers);
        return this;
    }

    @Override
    public Item<I> unbreakable(boolean unbreakable) {
        this.factory.unbreakable(this.item, unbreakable);
        return this;
    }

    @Override
    public boolean unbreakable() {
        return this.factory.unbreakable(this.item);
    }

    @Override
    public Item<I> itemNameJson(String itemName) {
        this.factory.itemNameJson(this.item, itemName);
        return this;
    }

    @Override
    public Optional<String> itemNameJson() {
        return this.factory.itemNameJson(this.item);
    }

    @Override
    public Item<I> itemNameComponent(Component itemName) {
        this.factory.itemNameComponent(this.item, itemName);
        return this;
    }

    @Override
    public Optional<Component> itemNameComponent() {
        return this.factory.itemNameComponent(this.item);
    }

    @Override
    public Item<I> skull(String data) {
        this.factory.skull(this.item, data);
        return this;
    }

    @Override
    public Optional<Enchantment> getEnchantment(Key enchantmentId) {
        return this.factory.getEnchantment(this.item, enchantmentId);
    }

    @Override
    public Optional<List<Enchantment>> enchantments() {
        return this.factory.enchantments(this.item);
    }

    @Override
    public Optional<List<Enchantment>> storedEnchantments() {
        return this.factory.storedEnchantments(this.item);
    }

    @Override
    public Item<I> setEnchantments(List<Enchantment> enchantments) {
        this.factory.enchantments(this.item, enchantments);
        return this;
    }

    @Override
    public Item<I> setStoredEnchantments(List<Enchantment> enchantments) {
        this.factory.storedEnchantments(this.item, enchantments);
        return this;
    }

    @Override
    public int maxStackSize() {
        return this.factory.maxStackSize(this.item);
    }

    @Override
    public Item<I> maxStackSize(int amount) {
        this.factory.maxStackSize(this.item, amount);
        return this;
    }

    @Override
    public Item<I> itemFlags(List<String> flags) {
        this.factory.itemFlags(this.item, flags);
        return this;
    }

    @Override
    public Object getJavaTag(Object... path) {
        return this.factory.getJavaTag(this.item, path);
    }

    @Override
    public Tag getTag(Object... path) {
        return this.factory.getTag(this.item, path);
    }

    @Override
    public Object getExactTag(Object... path) {
        return this.factory.getExactTag(this.item, path);
    }

    @Override
    public Item<I> setTag(Object value, Object... path) {
        this.factory.setTag(this.item, value, path);
        return this;
    }

    @Override
    public boolean hasTag(Object... path) {
        return this.factory.hasTag(this.item, path);
    }

    @Override
    public boolean removeTag(Object... path) {
        return this.factory.removeTag(this.item, path);
    }

    @Override
    public boolean hasComponent(Object type) {
        return this.factory.hasComponent(this.item, type);
    }

    @Override
    public boolean hasNonDefaultComponent(Object type) {
        return this.factory.hasNonDefaultComponent(this.item, type);
    }

    @Override
    public void removeComponent(Object type) {
        this.factory.removeComponent(this.item, type);
    }

    @Override
    public Object getExactComponent(Object type) {
        return this.factory.getExactComponent(this.item, type);
    }

    @Override
    public void setExactComponent(Object type, Object value) {
        this.factory.setExactComponent(this.item, type, value);
    }

    @Override
    public Object getJavaComponent(Object type) {
        return this.factory.getJavaComponent(this.item, type);
    }

    @Override
    public JsonElement getJsonComponent(Object type) {
        return this.factory.getJsonComponent(this.item, type);
    }

    @Override
    public Tag getSparrowNBTComponent(Object type) {
        return this.factory.getSparrowNBTComponent(this.item, type);
    }

    @Override
    public Object getNBTComponent(Object type) {
        return this.factory.getNBTComponent(this.item, type);
    }

    @Override
    public void setComponent(Object type, Object value) {
        this.factory.setComponent(this.item, type, value);
    }

    @Override
    public void setJavaComponent(Object type, Object value) {
        this.factory.setJavaComponent(this.item, type, value);
    }

    @Override
    public void setJsonComponent(Object type, JsonElement value) {
        this.factory.setJsonComponent(this.item, type, value);
    }

    @Override
    public void setNBTComponent(Object type, Tag value) {
        this.factory.setNBTComponent(this.item, type, value);
    }

    @Override
    public void resetComponent(Object type) {
        this.factory.resetComponent(this.item, type);
    }

    @Override
    public I getItem() {
        return this.factory.getItem(this.item);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public AbstractItem<W, I> copyWithCount(int count) {
        return new AbstractItem<>(this.factory, (W) this.item.copyWithCount(count));
    }

    @Override
    public boolean hasItemTag(Key itemTag) {
        return this.factory.hasItemTag(this.item, itemTag);
    }

    @Override
    public Object getLiteralObject() {
        return this.item.getLiteralObject();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public AbstractItem<W, I> mergeCopy(Item<?> another) {
        return new AbstractItem<>(this.factory, this.factory.mergeCopy(this.item, (W) ((AbstractItem) another).item));
    }

    @Override
    public AbstractItem<W, I> transmuteCopy(Key another, int count) {
        return new AbstractItem<>(this.factory, this.factory.transmuteCopy(this.item, another, count));
    }

    @Override
    public Item<I> unsafeTransmuteCopy(Object another, int count) {
        return new AbstractItem<>(this.factory, this.factory.unsafeTransmuteCopy(this.item, another, count));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void merge(Item<I> another) {
        this.factory.merge(this.item, (W) ((AbstractItem) another).item);
    }

    @Override
    public byte[] toByteArray() {
        return this.factory.toByteArray(this.item);
    }

    @Override
    public void shrink(int amount) {
        this.item.shrink(amount);
    }

    @Override
    public void hurtAndBreak(int amount, @Nullable Player player, @Nullable EquipmentSlot slot) {
        this.item.hurtAndBreak(amount, player, slot);
    }
}
