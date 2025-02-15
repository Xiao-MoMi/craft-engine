package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.MaterialUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemSettings;
import net.momirealms.craftengine.core.item.behavior.EmptyItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.modifier.ItemModifier;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BukkitCustomItem implements CustomItem<ItemStack> {
    private final Key id;
    private final Key materialKey;
    private final Material material;
    private final List<ItemModifier<ItemStack>> modifiers;
    private final ItemBehavior behavior;
    private final ItemSettings settings;

    public BukkitCustomItem(Key id, Key materialKey, Material material, List<ItemModifier<ItemStack>> modifiers, ItemBehavior behavior, ItemSettings settings) {
        this.id = id;
        this.material = material;
        this.modifiers = modifiers;
        this.behavior = behavior;
        this.materialKey = materialKey;
        this.settings = settings;
    }

    @Override
    public Key id() {
        return id;
    }

    @Override
    public Key material() {
        return materialKey;
    }

    @Override
    public List<ItemModifier<ItemStack>> modifiers() {
        return modifiers;
    }

    @Override
    public ItemStack buildItemStack(Player player, int count) {
        ItemStack item = new ItemStack(material);
        if (this.modifiers.isEmpty()) {
            return item;
        }
        Item<ItemStack> wrapped = BukkitCraftEngine.instance().itemManager().wrap(item);
        wrapped.count(count);
        for (ItemModifier<ItemStack> modifier : this.modifiers) {
            modifier.apply(wrapped, player);
        }
        return wrapped.load();
    }

    @Override
    public ItemSettings settings() {
        return settings;
    }

    @Override
    public Item<ItemStack> buildItem(Player player) {
        ItemStack item = new ItemStack(material);
        Item<ItemStack> wrapped = BukkitCraftEngine.instance().itemManager().wrap(item);
        for (ItemModifier<ItemStack> modifier : modifiers()) {
            modifier.apply(wrapped, player);
        }
        return wrapped;
    }

    @Override
    public @NotNull ItemBehavior behavior() {
        return this.behavior;
    }

    public static Builder<ItemStack> builder() {
        return new BuilderImpl();
    }

    public static class BuilderImpl implements Builder<ItemStack> {
        private Key id;
        private Material material;
        private Key materialKey;
        private ItemBehavior behavior = EmptyItemBehavior.INSTANCE;
        private ItemSettings settings = ItemSettings.of();
        private final List<ItemModifier<ItemStack>> modifiers = new ArrayList<>();

        @Override
        public Builder<ItemStack> id(Key id) {
            this.id = id;
            return this;
        }

        @Override
        public Builder<ItemStack> material(Key material) {
            this.materialKey = material;
            this.material = MaterialUtils.getMaterial(material.value());
            return this;
        }

        @Override
        public Builder<ItemStack> modifier(ItemModifier<ItemStack> modifier) {
            this.modifiers.add(modifier);
            return this;
        }

        @Override
        public Builder<ItemStack> modifiers(List<ItemModifier<ItemStack>> list) {
            this.modifiers.addAll(list);
            return this;
        }

        @Override
        public Builder<ItemStack> behavior(ItemBehavior behavior) {
            this.behavior=  behavior;
            return this;
        }

        @Override
        public Builder<ItemStack> settings(ItemSettings settings) {
            this.settings = settings;
            return this;
        }

        @Override
        public CustomItem<ItemStack> build() {
            return new BukkitCustomItem(id, materialKey, material, modifiers, behavior, settings);
        }
    }
}
