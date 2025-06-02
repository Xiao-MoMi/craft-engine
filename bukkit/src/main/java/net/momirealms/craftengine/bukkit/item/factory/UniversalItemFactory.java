package net.momirealms.craftengine.bukkit.item.factory;

import com.saicone.rtag.RtagItem;
import com.saicone.rtag.item.ItemObject;
import com.saicone.rtag.tag.TagBase;
import com.saicone.rtag.tag.TagCompound;
import com.saicone.rtag.tag.TagList;
import net.momirealms.craftengine.bukkit.item.LegacyItemWrapper;
import net.momirealms.craftengine.core.item.Enchantment;
import net.momirealms.craftengine.core.item.Trim;
import net.momirealms.craftengine.core.item.modifier.IdModifier;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.SkullUtils;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class UniversalItemFactory extends BukkitItemFactory<LegacyItemWrapper> {

    public UniversalItemFactory(CraftEngine plugin) {
        super(plugin);
    }

    @Override
    protected LegacyItemWrapper wrapInternal(ItemStack item) {
        return new LegacyItemWrapper(new RtagItem(item), item.getAmount());
    }

    @Override
    protected void setTag(LegacyItemWrapper item, Object value, Object... path) {
        item.setTag(value, path);
    }

    @Override
    protected Object getJavaTag(LegacyItemWrapper item, Object... path) {
        return item.getJavaTag(path);
    }

    @Override
    protected Tag getNBTTag(LegacyItemWrapper item, Object... path) {
        return item.getNBTTag(path);
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
    protected Optional<Key> customId(LegacyItemWrapper item) {
        Object id = item.getJavaTag(IdModifier.CRAFT_ENGINE_ID);
        if (id == null) return Optional.empty();
        return Optional.of(Key.of(id.toString()));
    }

    @Override
    protected void customId(LegacyItemWrapper item, Key id) {
        item.setTag(id.toString(), IdModifier.CRAFT_ENGINE_ID);
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
        return Optional.of(item.getJavaTag("display", "Name"));
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
        return Optional.of(item.getJavaTag("CustomModelData"));
    }

    @Override
    protected void skull(LegacyItemWrapper item, String skullData) {
        if (skullData == null) {
            item.remove("SkullOwner");
        } else {
            item.setTag(UUID.nameUUIDFromBytes(SkullUtils.identifierFromBase64(skullData).getBytes(StandardCharsets.UTF_8)), "SkullOwner", "Id");
            item.setTag(
                    List.of(Map.of("Value", skullData)),
                    "SkullOwner", "Properties", "textures"
            );
        }
    }

    @Override
    protected Optional<List<String>> loreJson(LegacyItemWrapper item) {
        if (!item.hasTag("display", "Lore")) return Optional.empty();
        return Optional.of(item.getJavaTag("display", "Lore"));
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
        return Optional.ofNullable((Boolean) item.getJavaTag("Unbreakable")).orElse(false);
    }

    @Override
    protected void unbreakable(LegacyItemWrapper item, boolean unbreakable) {
        item.setTag(unbreakable, "Unbreakable");
    }

    @Override
    protected Optional<Integer> damage(LegacyItemWrapper item) {
        if (!item.hasTag("Damage")) return Optional.empty();
        return Optional.of(item.getJavaTag("Damage"));
    }

    @Override
    protected void damage(LegacyItemWrapper item, Integer damage) {
        item.setTag(damage, "Damage");
    }

    @Override
    protected Optional<Integer> dyedColor(LegacyItemWrapper item) {
        if (!item.hasTag("display", "color")) return Optional.empty();
        return Optional.of(item.getJavaTag("display", "color"));
    }

    @Override
    protected void dyedColor(LegacyItemWrapper item, Integer color) {
        if (color == null) {
            item.remove("display", "color");
        } else {
            item.setTag(color, "display", "color");
        }
    }

    @Override
    protected int maxDamage(LegacyItemWrapper item) {
        return item.getItem().getType().getMaxDurability();
    }

    @Override
    protected void maxDamage(LegacyItemWrapper item, Integer damage) {
        throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
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

    @Override
    protected void addEnchantment(LegacyItemWrapper item, Enchantment enchantment) {
        Object enchantments = item.getExactTag("Enchantments");
        if (enchantments != null) {
            for (Object enchant : TagList.getValue(enchantments)) {
                if (TagBase.getValue(TagCompound.get(enchant, "id")).equals(enchant.toString())) {
                    TagCompound.set(enchant, "lvl", TagBase.newTag(enchantment.level()));
                    return;
                }
            }
            item.add(Map.of("id", enchantment.id().toString(), "lvl", (short) enchantment.level()), "Enchantments");
        } else {
            item.setTag(List.of(Map.of("id", enchantment.id().toString(), "lvl", (short) enchantment.level())), "Enchantments");
        }
    }

    @Override
    protected void addStoredEnchantment(LegacyItemWrapper item, Enchantment enchantment) {
        Object enchantments = item.getExactTag("StoredEnchantments");
        if (enchantments != null) {
            for (Object enchant : TagList.getValue(enchantments)) {
                if (TagBase.getValue(TagCompound.get(enchant, "id")).equals(enchant.toString())) {
                    TagCompound.set(enchant, "lvl", TagBase.newTag(enchantment.level()));
                    return;
                }
            }
            item.add(Map.of("id", enchantment.id().toString(), "lvl", (short) enchantment.level()), "StoredEnchantments");
        } else {
            item.setTag(List.of(Map.of("id", enchantment.id().toString(), "lvl", (short) enchantment.level())), "StoredEnchantments");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Optional<Enchantment> getEnchantment(LegacyItemWrapper item, Key key) {
        int level = item.getItem().getEnchantmentLevel(Objects.requireNonNull(Registry.ENCHANTMENT.get(new NamespacedKey(key.namespace(), key.value()))));
        if (level <= 0) return Optional.empty();
        return Optional.of(new Enchantment(key, level));
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
        return item.getItem().getType().getMaxStackSize();
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
        return Optional.of(item.getJavaTag("RepairCost"));
    }

    @Override
    protected void trim(LegacyItemWrapper item, Trim trim) {
        if (trim == null) {
            item.remove("Trim");
            return;
        }
        item.setTag(trim.material(), "Trim", "material");
        item.setTag(trim.pattern(), "Trim", "pattern");
    }

    @Override
    protected Optional<Trim> trim(LegacyItemWrapper item) {
        String material = item.getJavaTag("Trim", "material");
        String pattern = item.getJavaTag("Trim", "pattern");
        if (material == null || pattern == null) return Optional.empty();
        return Optional.of(new Trim(material, pattern));
    }

    @Override
    protected LegacyItemWrapper mergeCopy(LegacyItemWrapper item1, LegacyItemWrapper item2) {
        Object itemStack = ItemObject.copy(item2.getLiteralObject());
        ItemObject.setCustomDataTag(itemStack, TagCompound.clone(ItemObject.getCustomDataTag(item1.getLiteralObject())));
        // one more step than vanilla
        TagCompound.merge(ItemObject.getCustomDataTag(itemStack), ItemObject.getCustomDataTag(item2.getLiteralObject()), true, true);
        return new LegacyItemWrapper(new RtagItem(ItemObject.asCraftMirror(itemStack)), item2.count());
    }

    @Override
    protected void merge(LegacyItemWrapper item1, LegacyItemWrapper item2) {
        // load previous changes on nms items
        item1.load();
        TagCompound.merge(ItemObject.getCustomDataTag(item1.getLiteralObject()), ItemObject.getCustomDataTag(item2.getLiteralObject()), true, true);
        // update wrapped item
        item1.update();
    }
}