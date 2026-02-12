package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistryOps;
import net.momirealms.craftengine.bukkit.util.EquipmentSlotUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.ItemType;
import net.momirealms.craftengine.core.item.ItemWrapper;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class LegacyItemWrapper implements ItemWrapper<ItemStack> {
    private final Object nmsStack;
    private final ItemStack itemStack;
    private ItemType itemType;

    public LegacyItemWrapper(ItemStack item) {
        this.itemStack = ItemStackUtils.ensureCraftItemStack(item);
        this.nmsStack = FastNMS.INSTANCE.field$CraftItemStack$handle(this.itemStack);
    }

    public ItemType itemType() {
        if (this.itemType == null) {
            this.itemType = new ComponentItemType(FastNMS.INSTANCE.method$ItemStack$getItem(this.getLiteralObject()));
        }
        return this.itemType;
    }

    public boolean setTag(Object value, Object... path) {
        Object finalNMSTag;
        if (value instanceof Tag tag) {
            finalNMSTag = MRegistryOps.SPARROW_NBT.convertTo(MRegistryOps.NBT, tag);
        } else if (CoreReflections.clazz$Tag.isInstance(value)) {
            finalNMSTag = value;
        } else {
            finalNMSTag = MRegistryOps.JAVA.convertTo(MRegistryOps.NBT, value);
        }

        if (path == null || path.length == 0) {
            if (CoreReflections.clazz$CompoundTag.isInstance(finalNMSTag)) {
                FastNMS.INSTANCE.method$ItemStack$setTag(this.nmsStack, finalNMSTag);
                return true;
            }
            return false;
        }

        Object currentTag = FastNMS.INSTANCE.field$ItemStack$getOrCreateTag(this.nmsStack);

        for (int i = 0; i < path.length - 1; i++) {
            Object pathSegment = path[i];
            if (pathSegment == null) return false;
            Object childTag = FastNMS.INSTANCE.method$CompoundTag$get(currentTag, pathSegment.toString());
            if (!CoreReflections.clazz$CompoundTag.isInstance(childTag)) {
                childTag = FastNMS.INSTANCE.constructor$CompoundTag();
                FastNMS.INSTANCE.method$CompoundTag$put(currentTag, pathSegment.toString(), childTag);
            }
            currentTag = childTag;
        }

        String finalKey = path[path.length - 1].toString();
        FastNMS.INSTANCE.method$CompoundTag$put(currentTag, finalKey, finalNMSTag);
        return true;
    }

    @SuppressWarnings("unchecked")
    public <V> V getJavaTag(Object... path) {
        Object tag = getExactTag(path);
        if (tag == null) return null;
        return (V) MRegistryOps.NBT.convertTo(MRegistryOps.JAVA, tag);
    }

    public Tag getNBTTag(Object... path) {
        Object tag = getExactTag(path);
        if (tag == null) return null;
        return MRegistryOps.NBT.convertTo(MRegistryOps.SPARROW_NBT, tag);
    }

    public int count() {
        return getItem().getAmount();
    }

    public void count(int amount) {
        if (amount < 0) amount = 0;
        getItem().setAmount(amount);
    }

    @SuppressWarnings("DuplicatedCode")
    public Object getExactTag(Object... path) {
        Object compoundTag = FastNMS.INSTANCE.method$ItemStack$getTag(this.nmsStack);
        if (compoundTag == null) return null;
        Object currentTag = compoundTag;
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

    public boolean remove(Object... path) {
        Object compoundTag = FastNMS.INSTANCE.method$ItemStack$getTag(this.nmsStack);
        if (compoundTag == null || path == null || path.length == 0) return false;

        if (path.length == 1) {
            String key = path[0].toString();
            if (FastNMS.INSTANCE.method$CompoundTag$get(compoundTag, key) != null) {
                FastNMS.INSTANCE.method$CompoundTag$remove(compoundTag, key);
                return true;
            }
        }

        Object currentTag = compoundTag;
        for (int i = 0; i < path.length - 1; i++) {
            Object pathSegment = path[i];
            if (pathSegment == null) return false;
            currentTag = FastNMS.INSTANCE.method$CompoundTag$get(currentTag, path[i].toString());
            if (!CoreReflections.clazz$CompoundTag.isInstance(currentTag)) {
                return false;
            }
        }

        String finalKey = path[path.length - 1].toString();
        if (FastNMS.INSTANCE.method$CompoundTag$get(currentTag, finalKey) != null) {
            FastNMS.INSTANCE.method$CompoundTag$remove(currentTag, finalKey);
            return true;
        }
        return false;
    }

    public boolean hasTag(Object... path) {
        return getExactTag(path) != null;
    }

    @Override
    public ItemStack getItem() {
        return this.itemStack;
    }

    @Override
    public Object getLiteralObject() {
        return this.nmsStack;
    }

    @Override
    public ItemWrapper<ItemStack> copyWithCount(int count) {
        ItemStack copied = this.itemStack.clone();
        copied.setAmount(count);
        return new LegacyItemWrapper(copied);
    }

    @Override
    public void shrink(int amount) {
        this.count(count() - amount);
    }

    @Override
    public void hurtAndBreak(int amount, @Nullable Player player, @Nullable EquipmentSlot slot) {
        if (player == null) {
            if (this.hurt(amount)) {
                this.shrink(1);
                this.setTag(0, "Damage");
            }
            return;
        }
        FastNMS.INSTANCE.method$ItemStack$hurtAndBreak(
                this.nmsStack,
                amount,
                player.serverPlayer(),
                slot != null ? EquipmentSlotUtils.toNMSEquipmentSlot(slot) : null
        );
    }

    private boolean hurt(int amount) {
        if (ItemStackUtils.isEmpty(itemStack) || itemStack.getType().getMaxDurability() <= 0 || !hasTag("Unbreakable") || (boolean) getJavaTag("Unbreakable")) return false;
        if (amount > 0) {
            int level = this.itemStack.getEnchantmentLevel(Enchantment.UNBREAKING);
            int ignoredDamage = 0;
            for (int i = 0; level > 0 && i < amount; ++i) {
                if (RandomUtils.generateRandomInt(0, level + 1) > 0) ++ignoredDamage;
            }
            amount -= ignoredDamage;
            if (amount <= 0) return false;
        }
        int damage = this.hasTag("Damage") ? this.getJavaTag("Damage") : 0;
        damage += amount;
        this.setTag(damage, "Damage");
        return damage >= this.itemStack.getType().getMaxDurability();
    }
}