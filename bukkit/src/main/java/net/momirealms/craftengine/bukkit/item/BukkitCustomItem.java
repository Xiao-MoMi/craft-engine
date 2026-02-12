package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.updater.ItemUpdateConfig;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class BukkitCustomItem extends AbstractCustomItem<ItemStack> {
    private final Object item;
    private final Object clientItem;

    public BukkitCustomItem(boolean isVanillaItem, UniqueKey id, Object item, Object clientItem, Key materialKey, Key clientBoundMaterialKey,
                            List<ItemBehavior> behaviors,
                            List<ItemProcessor> modifiers, List<ItemProcessor> clientBoundModifiers,
                            ItemSettings settings,
                            Map<EventTrigger, List<Function<Context>>> events,
                            ItemUpdateConfig updater) {
        super(isVanillaItem, id, materialKey, clientBoundMaterialKey, behaviors, modifiers, clientBoundModifiers, settings, events, updater);
        this.item = item;
        this.clientItem = clientItem;
    }

    @Override
    public ItemStack buildItemStack(ItemBuildContext context, int count) {
        ItemStack item = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(FastNMS.INSTANCE.constructor$ItemStack(this.item, count));
        Item<ItemStack> wrapped = BukkitCraftEngine.instance().itemManager().wrap(item);
        for (ItemProcessor modifier : this.modifiers) {
            modifier.apply(wrapped, context);
        }
        return wrapped.getItem();
    }

    @Override
    public Item<ItemStack> buildItem(ItemBuildContext context, int count) {
        ItemStack item = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(FastNMS.INSTANCE.constructor$ItemStack(this.item, count));
        Item<ItemStack> wrapped = BukkitCraftEngine.instance().itemManager().wrap(item);
        for (ItemProcessor modifier : dataModifiers()) {
            modifier.apply(wrapped, context);
        }
        return wrapped;
    }

    public Object clientItem() {
        return this.clientItem;
    }

    public Object item() {
        return this.item;
    }

    public boolean hasClientboundMaterial() {
        return this.clientItem != this.item;
    }

    public static Builder<ItemStack> builder(Object item, Object clientBoundItem) {
        return new BuilderImpl(item, clientBoundItem);
    }

    public static class BuilderImpl implements Builder<ItemStack> {
        private boolean isVanillaItem;
        private UniqueKey id;
        private Key itemKey;
        private final Object item;
        private Key clientBoundItemKey;
        private final Object clientBoundItem;
        private final Map<EventTrigger, List<Function<Context>>> events = new EnumMap<>(EventTrigger.class);
        private final List<ItemBehavior> behaviors = new ArrayList<>(4);
        private final List<ItemProcessor> modifiers = new ArrayList<>(4);
        private final List<ItemProcessor> clientBoundModifiers = new ArrayList<>(4);
        private ItemSettings settings;
        private ItemUpdateConfig updater;

        public BuilderImpl(Object item, Object clientBoundItem) {
            this.item = item;
            this.clientBoundItem = clientBoundItem;
        }

        @Override
        public Builder<ItemStack> isVanillaItem(boolean is) {
            this.isVanillaItem = is;
            return this;
        }

        @Override
        public Builder<ItemStack> id(UniqueKey id) {
            this.id = id;
            return this;
        }

        @Override
        public Builder<ItemStack> clientBoundMaterial(Key clientBoundMaterial) {
            this.clientBoundItemKey = clientBoundMaterial;
            return this;
        }

        @Override
        public Builder<ItemStack> material(Key material) {
            this.itemKey = material;
            return this;
        }

        @Override
        public Builder<ItemStack> dataModifier(ItemProcessor modifier) {
            this.modifiers.add(modifier);
            return this;
        }

        @Override
        public Builder<ItemStack> dataModifiers(List<ItemProcessor> modifiers) {
            this.modifiers.addAll(modifiers);
            return this;
        }

        @Override
        public Builder<ItemStack> clientBoundDataModifier(ItemProcessor modifier) {
            this.clientBoundModifiers.add(modifier);
            return this;
        }

        @Override
        public Builder<ItemStack> clientBoundDataModifiers(List<ItemProcessor> modifiers) {
            this.clientBoundModifiers.addAll(modifiers);
            return null;
        }

        @Override
        public Builder<ItemStack> behavior(ItemBehavior behavior) {
            this.behaviors.add(behavior);
            return this;
        }

        @Override
        public Builder<ItemStack> behaviors(List<ItemBehavior> behaviors) {
            this.behaviors.addAll(behaviors);
            return this;
        }

        @Override
        public Builder<ItemStack> settings(ItemSettings settings) {
            this.settings = settings;
            return this;
        }

        @Override
        public Builder<ItemStack> events(Map<EventTrigger, List<Function<Context>>> events) {
            this.events.putAll(events);
            return this;
        }

        @Override
        public Builder<ItemStack> updater(ItemUpdateConfig updater) {
            this.updater = updater;
            return this;
        }

        @Override
        public CustomItem<ItemStack> build() {
            this.modifiers.addAll(this.settings.processors());
            this.clientBoundModifiers.addAll(this.settings.clientBoundProcessors());
            return new BukkitCustomItem(this.isVanillaItem, this.id, this.item, this.clientBoundItem, this.itemKey, this.clientBoundItemKey, List.copyOf(this.behaviors),
                    List.copyOf(this.modifiers), List.copyOf(this.clientBoundModifiers), this.settings, this.events, updater);
        }
    }
}
