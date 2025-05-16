package net.momirealms.craftengine.core.item;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Optional;

public class AbstractItem<W extends ItemWrapper<I>, I> implements Item<I> {
    private final ItemFactory<W, I> factory;
    private final W item;

    AbstractItem(ItemFactory<W, I> factory, W item) {
        this.factory = factory;
        this.item = item;
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
    public Optional<Integer> maxDamage() {
        return this.factory.maxDamage(this.item);
    }

    @Override
    public Item<I> dyedColor(Integer data) {
        this.factory.dyedColor(this.item, data);
        return this;
    }

    @Override
    public Optional<Integer> dyedColor() {
        return this.factory.dyedColor(this.item);
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
    public Key id() {
        return this.factory.id(this.item);
    }

    @Override
    public Key vanillaId() {
        return this.factory.vanillaId(this.item);
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
    public Optional<String> customName() {
        return this.factory.customName(this.item);
    }

    @Override
    public Item<I> customName(String displayName) {
        this.factory.customName(this.item, displayName);
        return this;
    }

    @Override
    public Item<I> lore(List<String> lore) {
        this.factory.lore(this.item, lore);
        return this;
    }

    @Override
    public Optional<List<String>> lore() {
        return this.factory.lore(this.item);
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
    public Optional<String> itemName() {
        return this.factory.itemName(this.item);
    }

    @Override
    public Item<I> itemName(String itemName) {
        this.factory.itemName(this.item, itemName);
        return this;
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
    public Item<I> setEnchantments(List<Enchantment> enchantments) {
        this.factory.enchantments(this.item, enchantments);
        return this;
    }

    @Override
    public Item<I> addEnchantment(Enchantment enchantment) {
        this.factory.addEnchantment(this.item, enchantment);
        return this;
    }

    @Override
    public Item<I> setStoredEnchantments(List<Enchantment> enchantments) {
        this.factory.storedEnchantments(this.item, enchantments);
        return this;
    }

    @Override
    public Item<I> addStoredEnchantment(Enchantment enchantment) {
        this.factory.addStoredEnchantment(this.item, enchantment);
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
    public Object getTag(Object... path) {
        return this.factory.getTag(this.item, path);
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
    public void removeComponent(Object type) {
        this.factory.removeComponent(this.item, type);
    }

    @Override
    public Object getComponent(Object type) {
        return this.factory.getComponent(this.item, type);
    }

    @Override
    public Object getJavaTypeComponent(Object type) {
        return this.factory.encodeJava(type, getComponent(type));
    }

    @Override
    public JsonElement getJsonTypeComponent(Object type) {
        return this.factory.encodeJson(type, getComponent(type));
    }

    @Override
    public void setComponent(Object type, Object value) {
        this.factory.setComponent(this.item, type, value);
    }

    @Override
    public void resetComponent(Object type) {
        this.factory.resetComponent(this.item, type);
    }

    @Override
    public I getItem() {
        return this.factory.getItem(this.item);
    }

    @Override
    public I load() {
        return this.factory.load(this.item);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public AbstractItem<W, I> copyWithCount(int count) {
        return new AbstractItem<>(this.factory, (W) this.item.copyWithCount(count));
    }

    @Override
    public boolean is(Key itemTag) {
        return this.factory.is(this.item, itemTag);
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void merge(Item<I> another) {
        this.factory.merge(this.item, (W) ((AbstractItem) another).item);
    }

    @Override
    public byte[] toByteArray() {
        return this.factory.toByteArray(this.item);
    }
}
