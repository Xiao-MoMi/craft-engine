package net.momirealms.craftengine.bukkit.item.factory;

import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.item.LegacyItemWrapper;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.attribute.AttributeModifier;
import net.momirealms.craftengine.core.item.ItemType;
import net.momirealms.craftengine.core.item.component.value.Enchantment;
import net.momirealms.craftengine.core.item.component.value.FireworkExplosion;
import net.momirealms.craftengine.core.item.component.value.Trim;
import net.momirealms.craftengine.core.item.processor.IdProcessor;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.SkullUtils;
import net.momirealms.craftengine.core.util.UUIDUtils;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.nbt.CompoundTagProxy;
import net.momirealms.craftengine.proxy.minecraft.nbt.StringTagProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class UniversalItemFactory extends BukkitItemFactory<LegacyItemWrapper> {

    public UniversalItemFactory(CraftEngine plugin) {
        super(plugin);
    }

    @Override
    public LegacyItemWrapper wrap(Object item) {
        if (ItemStackProxy.CLASS.isInstance(item)) {
            return new LegacyItemWrapper(item);
        } else if (item instanceof ItemStack itemStack) {
            return new LegacyItemWrapper(itemStack);
        } else {
            throw new IllegalArgumentException("Unsupported item type: " + item.getClass());
        }
    }

    @Override
    protected void setTag(LegacyItemWrapper item, Object value, Object... path) {
        item.setTag(value, path);
    }

    @Override
    protected void setMinecraftTag(LegacyItemWrapper item, Object value, Object[] path) {
        item.setMinecraftTag(value, path);
    }

    @Override
    protected void setSparrowTag(LegacyItemWrapper item, Tag value, Object... path) {
        item.setSparrowTag(value, path);
    }

    @Override
    protected void setJsonTag(LegacyItemWrapper item, JsonElement value, Object... path) {
        item.setJsonTag(value, path);
    }

    @Override
    protected void setJavaTag(LegacyItemWrapper item, Object value, Object... path) {
        item.setJavaTag(value, path);
    }

    @Override
    protected JsonElement getTagAsJson(LegacyItemWrapper item, Object... path) {
        return item.getTagAsJson(path);
    }

    @Override
    protected Object getTagAsJava(LegacyItemWrapper item, Object... path) {
        return item.getTagAsJava(path);
    }

    @Override
    protected Tag getSparrowTag(LegacyItemWrapper item, Object... path) {
        return item.getSparrowTag(path);
    }

    @Override
    protected Object getMinecraftTag(LegacyItemWrapper item, Object... path) {
        return item.getMinecraftTag(path);
    }

    @Override
    protected boolean hasTag(LegacyItemWrapper item, Object... path) {
        return item.hasTag(path);
    }

    @Override
    protected boolean removeTag(LegacyItemWrapper item, Object... path) {
        return item.remove(path);
    }

    @Override
    protected ItemType type(LegacyItemWrapper item) {
        throw new UnsupportedOperationException("1.20.5+");
    }

    @Override
    protected Optional<Key> customId(LegacyItemWrapper item) {
        Object nmsStack = item.minecraftItem();
        Object tag = ItemStackProxy.INSTANCE.getTag(nmsStack);
        if (tag == null) return Optional.empty();
        Object stringTag = CompoundTagProxy.INSTANCE.get(tag, IdProcessor.CRAFT_ENGINE_ID);
        if (stringTag == null) return Optional.empty();
        return Optional.of(Key.of(StringTagProxy.INSTANCE.getData(stringTag)));
    }

    @Override
    protected void customId(LegacyItemWrapper item, Key id) {
        Object nmsStack = item.minecraftItem();
        Object tag = ItemStackProxy.INSTANCE.getOrCreateTag(nmsStack);
        CompoundTagProxy.INSTANCE.putString(tag, IdProcessor.CRAFT_ENGINE_ID, id.asString());
    }

    @Override
    protected void customNameJson(LegacyItemWrapper item, String json) {
        if (json != null) {
            item.setTag(json, "display", "Name");
        } else {
            item.remove("display", "Name");
        }
    }

    @Override
    protected Optional<String> customNameJson(LegacyItemWrapper item) {
        if (!item.hasTag("display", "Name")) return Optional.empty();
        return Optional.of(item.getTagAsJava("display", "Name"));
    }

    @Override
    protected void itemNameJson(LegacyItemWrapper item, String json) {
        customNameJson(item, json);
    }

    @Override
    protected Optional<String> itemNameJson(LegacyItemWrapper item) {
        return customNameJson(item);
    }

    @Override
    protected void customModelData(LegacyItemWrapper item, Integer data) {
        if (data == null) {
            item.remove("CustomModelData");
        } else {
            item.setTag(data, "CustomModelData");
        }
    }

    @Override
    protected Optional<Integer> customModelData(LegacyItemWrapper item) {
        if (!item.hasTag("CustomModelData")) return Optional.empty();
        return Optional.of(item.getTagAsJava("CustomModelData"));
    }

    @Override
    protected void skull(LegacyItemWrapper item, String skullData) {
        if (skullData == null) {
            item.remove("SkullOwner");
        } else {
            UUID id = UUID.nameUUIDFromBytes(SkullUtils.identifierFromBase64(skullData).getBytes(StandardCharsets.UTF_8));
            long most = id.getMostSignificantBits();
            long least = id.getLeastSignificantBits();
            item.setTag(List.of((int) (most >> 32), (int) most, (int) (least >> 32), (int) least), "SkullOwner", "Id");
            item.setTag(
                    List.of(Map.of("Value", skullData)),
                    "SkullOwner", "Properties", "textures"
            );
        }
    }

    @Override
    protected Optional<List<String>> loreJson(LegacyItemWrapper item) {
        if (!item.hasTag("display", "Lore")) return Optional.empty();
        return Optional.of(item.getTagAsJava("display", "Lore"));
    }

    @Override
    protected void loreJson(LegacyItemWrapper item, List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            item.remove("display", "Lore");
        } else {
            item.setTag(lore, "display", "Lore");
        }
    }

    @Override
    protected boolean unbreakable(LegacyItemWrapper item) {
        return Optional.ofNullable((Boolean) item.getTagAsJava("Unbreakable")).orElse(false);
    }

    @Override
    protected void unbreakable(LegacyItemWrapper item, boolean unbreakable) {
        item.setTag(unbreakable, "Unbreakable");
    }

    @Override
    protected Optional<Integer> damage(LegacyItemWrapper item) {
        if (!item.hasTag("Damage")) return Optional.empty();
        return Optional.of(item.getTagAsJava("Damage"));
    }

    @Override
    protected void damage(LegacyItemWrapper item, Integer damage) {
        item.setTag(damage, "Damage");
    }

    @Override
    protected Optional<Color> dyedColor(LegacyItemWrapper item) {
        if (!item.hasTag("display", "color")) return Optional.empty();
        return Optional.of(Color.fromDecimal(item.getTagAsJava("display", "color")));
    }

    @Override
    protected void dyedColor(LegacyItemWrapper item, Color color) {
        if (color == null) {
            item.remove("display", "color");
        } else {
            item.setTag(color.color(), "display", "color");
        }
    }

    @Override
    protected int maxDamage(LegacyItemWrapper item) {
        return item.platformItem().getType().getMaxDurability();
    }

    @Override
    protected void maxDamage(LegacyItemWrapper item, Integer damage) {
    }

    @Override
    protected void enchantments(LegacyItemWrapper item, List<Enchantment> enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            item.remove("Enchantments");
            return;
        }
        ArrayList<Object> tags = new ArrayList<>();
        for (Enchantment enchantment : enchantments) {
            tags.add((Map.of("id", enchantment.id().toString(), "lvl", (short) enchantment.level())));
        }
        item.setTag(tags, "Enchantments");
    }

    @Override
    protected void storedEnchantments(LegacyItemWrapper item, List<Enchantment> enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            item.remove("StoredEnchantments");
            return;
        }
        ArrayList<Object> tags = new ArrayList<>();
        for (Enchantment enchantment : enchantments) {
            tags.add((Map.of("id", enchantment.id().toString(), "lvl", (short) enchantment.level())));
        }
        item.setTag(tags, "StoredEnchantments");
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Optional<Enchantment> getEnchantment(LegacyItemWrapper item, Key key) {
        int level = item.platformItem().getEnchantmentLevel(Objects.requireNonNull(Registry.ENCHANTMENT.get(new NamespacedKey(key.namespace(), key.value()))));
        if (level <= 0) return Optional.empty();
        return Optional.of(new Enchantment(key, level));
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected Optional<List<Enchantment>> enchantments(LegacyItemWrapper item) {
        ListTag enchantmentTag = (ListTag) item.getSparrowTag("Enchantments");
        if (enchantmentTag == null) return Optional.empty();
        List<Enchantment> enchantments = new ArrayList<>();
        for (Tag tag : enchantmentTag) {
            if (tag instanceof CompoundTag enchantTag) {
                enchantments.add(new Enchantment(Key.of(enchantTag.getString("id")), enchantTag.getInt("lvl")));
            }
        }
        return Optional.of(enchantments);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected Optional<List<Enchantment>> storedEnchantments(LegacyItemWrapper item) {
        ListTag enchantmentTag = (ListTag) item.getSparrowTag("StoredEnchantments");
        if (enchantmentTag == null) return Optional.empty();
        List<Enchantment> enchantments = new ArrayList<>();
        for (Tag tag : enchantmentTag) {
            if (tag instanceof CompoundTag enchantTag) {
                enchantments.add(new Enchantment(Key.of(enchantTag.getString("id")), enchantTag.getInt("lvl")));
            }
        }
        return Optional.of(enchantments);
    }

    @Override
    protected void itemFlags(LegacyItemWrapper item, List<String> flags) {
        if (flags == null || flags.isEmpty()) {
            item.remove("HideFlags");
            return;
        }
        int f = 0;
        for (String flag : flags) {
            ItemFlag itemFlag = ItemFlag.valueOf(flag);
            f = f | 1 << itemFlag.ordinal();
        }
        item.setTag(f, "HideFlags");
    }

    @Override
    protected int maxStackSize(LegacyItemWrapper item) {
        return item.platformItem().getType().getMaxStackSize();
    }

    @Override
    protected void maxStackSize(LegacyItemWrapper item, Integer maxStackSize) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
    }

    @Override
    protected void repairCost(LegacyItemWrapper item, Integer data) {
        item.setTag(data, "RepairCost");
    }

    @Override
    protected Optional<Integer> repairCost(LegacyItemWrapper item) {
        if (!item.hasTag("RepairCost")) return Optional.empty();
        return Optional.of(item.getTagAsJava("RepairCost"));
    }

    @Override
    protected void trim(LegacyItemWrapper item, Trim trim) {
        if (trim == null) {
            item.remove("Trim");
            return;
        }
        item.setTag(trim.material().asString(), "Trim", "material");
        item.setTag(trim.pattern().asString(), "Trim", "pattern");
    }

    @Override
    protected Optional<FireworkExplosion> fireworkExplosion(LegacyItemWrapper item) {
        Map<String, Object> explosionObj = item.getTagAsJava("Explosion");
        if (explosionObj == null) return Optional.empty();
        IntArrayList colors = (IntArrayList) explosionObj.get("Colors");
        IntArrayList fadeColors = (IntArrayList) explosionObj.get("FadeColors");
        return Optional.of(
                new FireworkExplosion(
                    FireworkExplosion.Shape.byId((Integer) explosionObj.getOrDefault("Type", 0)),
                        colors == null ? new IntArrayList() : new IntArrayList(colors),
                        fadeColors == null ? new IntArrayList() : new IntArrayList(fadeColors),
                        (boolean) explosionObj.getOrDefault("Trail", false),
                        (boolean) explosionObj.getOrDefault("Flicker", false)
                )
        );
    }

    @Override
    protected void fireworkExplosion(LegacyItemWrapper item, FireworkExplosion explosion) {
        if (explosion == null) {
            item.remove("Explosion");
        } else {
            item.setTag(Map.of(
                    "Type", explosion.shape().id(),
                    "Colors", explosion.colors(),
                    "FadeColors", explosion.fadeColors(),
                    "Trail", explosion.hasTrail(),
                    "Flicker", explosion.hasTwinkle()
            ), "Explosion");
        }
    }

    @Override
    protected Optional<Map<String, String>> blockState(LegacyItemWrapper item) {
        Map<String, String> state = item.getTagAsJava("BlockStateTag");
        if (state == null) return Optional.empty();
        return Optional.of(state);
    }

    @Override
    protected void blockState(LegacyItemWrapper item, Map<String, String> state) {
        item.setTag(state, "BlockStateTag");
    }

    @Override
    protected Optional<Trim> trim(LegacyItemWrapper item) {
        String material = item.getTagAsJava("Trim", "material");
        String pattern = item.getTagAsJava("Trim", "pattern");
        if (material == null || pattern == null) return Optional.empty();
        return Optional.of(new Trim(Key.of(material), Key.of(pattern)));
    }

    @Override
    protected LegacyItemWrapper mergeCopy(LegacyItemWrapper item1, LegacyItemWrapper item2) {
        Object copied = ItemStackProxy.INSTANCE.newInstance(ItemStackProxy.INSTANCE.getItem(item2.minecraftItem()), item2.count());
        Object copiedTag = ItemStackProxy.INSTANCE.getOrCreateTag(copied);
        CompoundTagProxy.INSTANCE.merge(copiedTag, ItemStackProxy.INSTANCE.getOrCreateTag(item1.minecraftItem()));
        CompoundTagProxy.INSTANCE.merge(copiedTag, ItemStackProxy.INSTANCE.getOrCreateTag(item2.minecraftItem()));
        return new LegacyItemWrapper(ItemStackUtils.getBukkitStack(copied));
    }

    @Override
    protected void merge(LegacyItemWrapper item1, LegacyItemWrapper item2) {
        Object item1Tag = ItemStackProxy.INSTANCE.getOrCreateTag(item1.minecraftItem());
        Object item2Tag = ItemStackProxy.INSTANCE.getOrCreateTag(item2.minecraftItem());
        CompoundTagProxy.INSTANCE.merge(item1Tag, item2Tag);
    }

    @Override
    protected LegacyItemWrapper transmuteCopy(LegacyItemWrapper item, Key newItem, int amount) {
        Object copied = ItemStackProxy.INSTANCE.newInstance(RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.ITEM, KeyUtils.toIdentifier(newItem)), amount);
        Object copiedTag = ItemStackProxy.INSTANCE.getOrCreateTag(copied);
        Object thisTag = ItemStackProxy.INSTANCE.getOrCreateTag(item.minecraftItem());
        CompoundTagProxy.INSTANCE.merge(copiedTag, thisTag);
        return new LegacyItemWrapper(ItemStackUtils.getBukkitStack(copied));
    }

    @Override
    protected LegacyItemWrapper unsafeTransmuteCopy(LegacyItemWrapper item, Object newItem, int amount) {
        Object newItemStack = ItemStackProxy.INSTANCE.newInstance(newItem, amount);
        ItemStackProxy.INSTANCE.setTag(newItemStack, CompoundTagProxy.INSTANCE.copy(ItemStackProxy.INSTANCE.getOrCreateTag(item.minecraftItem())));
        return new LegacyItemWrapper(ItemStackUtils.getBukkitStack(newItemStack));
    }

    @Override
    protected void attributeModifiers(LegacyItemWrapper item, List<AttributeModifier> modifiers) {
        ListTag listTag = new ListTag();
        for (AttributeModifier modifier : modifiers) {
            CompoundTag modifierTag = new CompoundTag();
            modifierTag.putString("AttributeName", modifier.type());
            modifierTag.putString("Name", modifier.id().toString());
            modifierTag.putString("Slot", modifier.slot().name().toLowerCase(Locale.ENGLISH));
            modifierTag.putInt("Operation", modifier.operation().ordinal());
            modifierTag.putDouble("Amount", modifier.amount());
            modifierTag.putIntArray("UUID", UUIDUtils.uuidToIntArray(UUID.nameUUIDFromBytes(modifier.id().toString().getBytes(StandardCharsets.UTF_8))));
            listTag.add(modifierTag);
        }
        item.setTag(listTag, "AttributeModifiers");
    }

    @Override
    protected boolean isSimilar(LegacyItemWrapper item1, LegacyItemWrapper item2) {
        return ItemStackProxy.INSTANCE.isSameItemSameTags(item1.minecraftItem(), item2.minecraftItem());
    }
}