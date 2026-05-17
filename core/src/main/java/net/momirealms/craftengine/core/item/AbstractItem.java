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
import net.momirealms.craftengine.core.item.setting.value.EquipmentData;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractItem<W extends ItemWrapper> implements Item {
    protected final ItemFactory<W> factory;
    protected final W item;

    protected AbstractItem(ItemFactory<W> factory, W item) {
        this.factory = factory;
        this.item = item;
    }

    protected abstract AbstractItem<W> withSameFactory(W item);

    @Override
    public boolean isEmpty() {
        return this.factory.isEmpty(this.item);
    }

    @Override
    public ItemType type() {
        return this.factory.type(this.item);
    }

    @Override
    public Item itemModel(String data) {
        this.factory.itemModel(this.item, data);
        return this;
    }

    @Override
    public Optional<String> itemModel() {
        return this.factory.itemModel(this.item);
    }

    @Override
    public Item useRemainder(Item item, int count) {
        this.factory.useRemainder(this.item, item, count);
        return this;
    }

    @Override
    public Optional<Item> useRemainder() {
        return this.factory.useRemainder(this.item).map(this::withSameFactory);
    }

    @Override
    public Optional<JukeboxPlayable> jukeboxSong() {
        return this.factory.jukeboxSong(this.item);
    }

    @Override
    public Item jukeboxSong(JukeboxPlayable data) {
        this.factory.jukeboxSong(this.item, data);
        return this;
    }

    @Override
    public Optional<EquipmentData> equippable() {
        return this.factory.equippable(this.item);
    }

    @Override
    public Item equippable(EquipmentData data) {
        this.factory.equippable(this.item, data);
        return this;
    }

    @Override
    public Item tooltipStyle(String data) {
        this.factory.tooltipStyle(this.item, data);
        return this;
    }

    @Override
    public Optional<String> tooltipStyle() {
        return this.factory.tooltipStyle(this.item);
    }

    @Override
    public Item damage(Integer data) {
        this.factory.damage(this.item, data);
        return this;
    }

    @Override
    public Optional<Integer> damage() {
        return this.factory.damage(this.item);
    }

    @Override
    public Item repairCost(Integer data) {
        this.factory.repairCost(this.item, data);
        return this;
    }

    @Override
    public Optional<Integer> repairCost() {
        return this.factory.repairCost(this.item);
    }

    @Override
    public Item maxDamage(Integer data) {
        this.factory.maxDamage(this.item, data);
        return this;
    }

    @Override
    public int maxDamage() {
        return this.factory.maxDamage(this.item);
    }

    @Override
    public Item blockState(Map<String, String> state) {
        this.factory.blockState(this.item, state);
        return this;
    }

    @Override
    public Optional<Map<String, String>> blockState() {
        return this.factory.blockState(this.item);
    }

    @Override
    public Item dyedColor(Color data) {
        this.factory.dyedColor(this.item, data);
        return this;
    }

    @Override
    public Optional<Color> dyedColor() {
        return this.factory.dyedColor(this.item);
    }

    @Override
    public Item fireworkExplosion(FireworkExplosion explosion) {
        this.factory.fireworkExplosion(this.item, explosion);
        return this;
    }

    @Override
    public Optional<FireworkExplosion> fireworkExplosion() {
        return this.factory.fireworkExplosion(this.item);
    }

    @Override
    public Optional<ItemDefinition> getDefinition() {
        return factory.plugin.itemManager().getItemDefinition(id());
    }

    @Override
    public Optional<ItemBehavior> getBehavior() {
        return factory.plugin.itemManager().getItemBehavior(id());
    }

    @Override
    public boolean isCustomItem() {
        Optional<ItemDefinition> itemDefinition = this.factory.plugin.itemManager().getItemDefinition(id());
        return itemDefinition.filter(definition -> !definition.isVanillaItem()).isPresent();
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
    public Optional<Key> customId() {
        return this.factory.customId(this.item);
    }

    @Override
    public Item customId(Key data) {
        this.factory.customId(this.item, data);
        return this;
    }

    @Override
    public int count() {
        return this.item.count();
    }

    @Override
    public Item count(int amount) {
        this.item.count(amount);
        return this;
    }

    @Override
    public Item trim(Trim trim) {
        this.factory.trim(this.item, trim);
        return this;
    }

    @Override
    public Optional<Trim> trim() {
        return this.factory.trim(this.item);
    }

    @Override
    public Item customModelData(Integer data) {
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
    public Item customNameJson(String displayName) {
        this.factory.customNameJson(this.item, displayName);
        return this;
    }

    @Override
    public Optional<Component> customNameComponent() {
        return this.factory.customNameComponent(this.item);
    }

    @Override
    public Item customNameComponent(Component displayName) {
        this.factory.customNameComponent(this.item, displayName);
        return this;
    }

    @Override
    public Item loreJson(List<String> lore) {
        this.factory.loreJson(this.item, lore);
        return this;
    }

    @Override
    public Optional<List<String>> loreJson() {
        return this.factory.loreJson(this.item);
    }

    @Override
    public Item loreComponent(List<Component> lore) {
        this.factory.loreComponent(this.item, lore);
        return this;
    }

    @Override
    public Optional<List<Component>> loreComponent() {
        return this.factory.loreComponent(this.item);
    }

    @Override
    public Item attributeModifiers(List<AttributeModifier> modifiers) {
        this.factory.attributeModifiers(this.item, modifiers);
        return this;
    }

    @Override
    public Item unbreakable(boolean unbreakable) {
        this.factory.unbreakable(this.item, unbreakable);
        return this;
    }

    @Override
    public boolean unbreakable() {
        return this.factory.unbreakable(this.item);
    }

    @Override
    public Item itemNameJson(String itemName) {
        this.factory.itemNameJson(this.item, itemName);
        return this;
    }

    @Override
    public Optional<String> itemNameJson() {
        return this.factory.itemNameJson(this.item);
    }

    @Override
    public Item itemNameComponent(Component itemName) {
        this.factory.itemNameComponent(this.item, itemName);
        return this;
    }

    @Override
    public Optional<Component> itemNameComponent() {
        return this.factory.itemNameComponent(this.item);
    }

    @Override
    public Item skull(String data) {
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
    public Item setEnchantments(List<Enchantment> enchantments) {
        this.factory.enchantments(this.item, enchantments);
        return this;
    }

    @Override
    public Item setStoredEnchantments(List<Enchantment> enchantments) {
        this.factory.storedEnchantments(this.item, enchantments);
        return this;
    }

    @Override
    public Optional<Boolean> glint() {
        return this.factory.glint(this.item);
    }

    @Override
    public Item glint(boolean value) {
        this.factory.glint(this.item, value);
        return this;
    }

    @Override
    public int maxStackSize() {
        return this.factory.maxStackSize(this.item);
    }

    @Override
    public Item maxStackSize(int amount) {
        this.factory.maxStackSize(this.item, amount);
        return this;
    }

    @Override
    public Item itemFlags(List<String> flags) {
        this.factory.itemFlags(this.item, flags);
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
    public Object getTagAsJava(Object... path) {
        return this.factory.getTagAsJava(this.item, path);
    }

    @Override
    public Object getMinecraftTag(Object... path) {
        return this.factory.getMinecraftTag(this.item, path);
    }

    @Override
    public Tag getSparrowTag(Object... path) {
        return this.factory.getSparrowTag(this.item, path);
    }

    @Override
    public JsonElement getTagAsJson(Object... path) {
        return this.factory.getTagAsJson(this.item, path);
    }

    @Override
    public Item setTag(Object value, Object... path) {
        this.factory.setTag(this.item, value, path);
        return this;
    }

    @Override
    public Item setMinecraftTag(Object value, Object... path) {
        this.factory.setMinecraftTag(this.item, value, path);
        return this;
    }

    @Override
    public Item setJsonTag(JsonElement value, Object... path) {
        this.factory.setJsonTag(this.item, value, path);
        return this;
    }

    @Override
    public Item setJavaTag(Object value, Object... path) {
        this.factory.setJavaTag(this.item, value, path);
        return this;
    }

    @Override
    public Item setSparrowTag(Tag value, Object... path) {
        this.factory.setSparrowTag(this.item, value, path);
        return this;
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
    public Object getComponentAsJava(Object type) {
        return this.factory.getComponentAsJava(this.item, type);
    }

    @Override
    public JsonElement getComponentAsJson(Object type) {
        return this.factory.getComponentAsJson(this.item, type);
    }

    @Override
    public Tag getComponentAsSparrowTag(Object type) {
        return this.factory.getComponentAsSparrowTag(this.item, type);
    }

    @Override
    public Object getComponentAsMinecraftTag(Object type) {
        return this.factory.getComponentAsMinecraftTag(this.item, type);
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
    public void setSparrowTagComponent(Object type, Tag value) {
        this.factory.setSparrowTagComponent(this.item, type, value);
    }

    @Override
    public void setMinecraftTagComponent(Object type, Object value) {
        this.factory.setMinecraftTagComponent(this.item, type, value);
    }

    @Override
    public void resetComponent(Object type) {
        this.factory.resetComponent(this.item, type);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public AbstractItem<W> copy() {
        return withSameFactory((W) this.item.copy());
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public AbstractItem<W> copyWithCount(int count) {
        return withSameFactory((W) this.item.copyWithCount(count));
    }

    @Override
    public boolean hasItemTag(Key itemTag) {
        return this.factory.hasItemTag(this.item, itemTag);
    }

    @Override
    public Object minecraftItem() {
        return this.item.minecraftItem();
    }

    @Override
    public Object platformItem() {
        return this.item.platformItem();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public AbstractItem<W> mergeCopy(Item another) {
        return withSameFactory(this.factory.mergeCopy(this.item, (W) ((AbstractItem) another).item));
    }

    @Override
    public AbstractItem<W> transmuteCopy(Key another, int count) {
        return withSameFactory(this.factory.transmuteCopy(this.item, another, count));
    }

    @Override
    public Item unsafeTransmuteCopy(Object another, int count) {
        return withSameFactory(this.factory.unsafeTransmuteCopy(this.item, another, count));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void merge(Item another) {
        this.factory.merge(this.item, (W) ((AbstractItem) another).item);
    }

    @Override
    public byte[] toBytes() {
        return this.factory.toByteArray(this.item);
    }

    @Override
    public CompoundTag toNBT() {
        return this.factory.toNBT(this.item);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean isSimilar(Item another) {
        return this.factory.isSimilar(this.item, (W) ((AbstractItem) another).item);
    }

    @Override
    public void shrink(int amount) {
        this.item.shrink(amount);
    }

    @Override
    public void grow(int amount) {
        this.item.grow(amount);
    }

    @Override
    public void hurtAndBreak(int amount, @NotNull Player player, @Nullable EquipmentSlot slot) {
        this.item.hurtAndBreak(amount, player, slot);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{minecraftItem=" + minecraftItem() + "}";
    }
}
