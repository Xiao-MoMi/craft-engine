package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Optional;

public class AbstractItem<W extends ItemWrapper<I>, I> implements Item<I> {
    private final ItemFactory<?, W, I> factory;
    private final ItemWrapper<I> item;

    AbstractItem(ItemFactory<?, W, I> factory, ItemWrapper<I> item) {
        this.factory = factory;
        this.item = item;
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
    public Item<I> maxDamage(Integer data) {
        this.factory.maxDamage(this.item, data);
        return this;
    }

    @Override
    public Optional<Integer> maxDamage() {
        return this.factory.maxDamage(this.item);
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
    public int count() {
        return this.item.count();
    }

    @Override
    public Item<I> count(int amount) {
        this.item.count(amount);
        return this;
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
    public Optional<String> displayName() {
        return this.factory.displayName(this.item);
    }

    @Override
    public Optional<String> itemName() {
        return this.factory.itemName(this.item);
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
    public Item<I> displayName(String displayName) {
        this.factory.displayName(this.item, displayName);
        return this;
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
    public boolean hasComponent(String type) {
        return this.factory.hasComponent(this.item, type);
    }

    @Override
    public void removeComponent(String type) {
        this.factory.removeComponent(this.item, type);
    }

    @Override
    public Object getComponent(String type) {
        return this.factory.getComponent(this.item, type);
    }

    @Override
    public void setComponent(String type, Object value) {
        this.factory.setComponent(this.item, type, value);
    }

    @Override
    public I getItem() {
        return this.factory.getItem(this.item);
    }

    @Override
    public I load() {
        return this.factory.load(this.item);
    }

    @Override
    public I loadCopy() {
        return this.factory.loadCopy(this.item);
    }

    @Override
    public void update() {
        this.factory.update(this.item);
    }

    @Override
    public Item<I> copyWithCount(int count) {
        return new AbstractItem<>(this.factory, this.item.copyWithCount(count));
    }

    @Override
    public boolean is(Key itemTag) {
        return this.factory.is(this.item, itemTag);
    }

    @Override
    public Object getLiteralObject() {
        return this.item.getLiteralObject();
    }
}
