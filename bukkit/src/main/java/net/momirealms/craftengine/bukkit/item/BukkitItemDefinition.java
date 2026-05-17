package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.AbstractItemDefinition;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.setting.ItemSettings;
import net.momirealms.craftengine.core.item.updater.ItemUpdateConfig;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class BukkitItemDefinition extends AbstractItemDefinition {
    private final Object item;
    private final Object clientItem;

    public BukkitItemDefinition(boolean isVanillaItem, UniqueKey id, Object item, Object clientItem, Key materialKey, Key clientBoundMaterialKey,
                                ItemBehavior behavior,
                                List<ItemProcessor> modifiers, List<ItemProcessor> clientBoundModifiers,
                                ItemSettings settings,
                                Map<EventTrigger, List<Function<Context>>> events,
                                ItemUpdateConfig updater) {
        super(isVanillaItem, id, materialKey, clientBoundMaterialKey, behavior, modifiers, clientBoundModifiers, settings, events, updater);
        this.item = item;
        this.clientItem = clientItem;
    }

    @Override
    public BukkitItem buildItem(ItemBuildContext context, int count) {
        ItemStack item = ItemStackUtils.getBukkitStack(ItemStackProxy.INSTANCE.newInstance(this.item, count));
        BukkitItem wrapped = BukkitItemManager.instance().wrap(item);
        for (ItemProcessor modifier : processors()) {
            modifier.apply(wrapped, context);
        }
        return wrapped;
    }

    public ItemStack buildBukkitItem(org.bukkit.entity.Player player) {
        return buildItem(ItemBuildContext.of(BukkitAdaptor.adapt(player)), 1).getBukkitItem();
    }

    public ItemStack buildBukkitItem(Player player) {
        return buildItem(ItemBuildContext.of(player), 1).getBukkitItem();
    }

    public ItemStack buildBukkitItem(ItemBuildContext context, int count) {
        return buildItem(context, count).getBukkitItem();
    }

    public ItemStack buildBukkitItem(ItemBuildContext context) {
        return buildItem(context, 1).getBukkitItem();
    }

    public ItemStack buildBukkitItem() {
        return buildItem(ItemBuildContext.empty(), 1).getBukkitItem();
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

    public static Builder builder(Object item, Object clientBoundItem) {
        return new BuilderImpl(item, clientBoundItem);
    }

    public static class BuilderImpl implements Builder {
        private boolean isVanillaItem;
        private UniqueKey id;
        private Key itemKey;
        private final Object item;
        private Key clientBoundItemKey;
        private final Object clientBoundItem;
        private final Map<EventTrigger, List<Function<Context>>> events = new EnumMap<>(EventTrigger.class);
        private ItemBehavior behavior;
        private final List<ItemProcessor> processors = new ArrayList<>(4);
        private final List<ItemProcessor> clientBoundProcessors = new ArrayList<>(4);
        private ItemSettings settings;
        private ItemUpdateConfig updater;

        public BuilderImpl(Object item, Object clientBoundItem) {
            this.item = item;
            this.clientBoundItem = clientBoundItem;
        }

        @Override
        public Builder isVanillaItem(boolean is) {
            this.isVanillaItem = is;
            return this;
        }

        @Override
        public Builder id(UniqueKey id) {
            this.id = id;
            return this;
        }

        @Override
        public Builder clientBoundMaterial(Key clientBoundMaterial) {
            this.clientBoundItemKey = clientBoundMaterial;
            return this;
        }

        @Override
        public Builder material(Key material) {
            this.itemKey = material;
            return this;
        }

        @Override
        public Builder dataProcessor(ItemProcessor modifier) {
            this.processors.add(modifier);
            return this;
        }

        @Override
        public Builder dataProcessors(List<ItemProcessor> modifiers) {
            this.processors.addAll(modifiers);
            return this;
        }

        @Override
        public Builder clientBoundProcessor(ItemProcessor modifier) {
            this.clientBoundProcessors.add(modifier);
            return this;
        }

        @Override
        public Builder clientBoundProcessors(List<ItemProcessor> modifiers) {
            this.clientBoundProcessors.addAll(modifiers);
            return null;
        }

        @Override
        public Builder behavior(ItemBehavior behavior) {
            this.behavior = behavior;
            return this;
        }

        @Override
        public Builder settings(ItemSettings settings) {
            this.settings = settings;
            return this;
        }

        @Override
        public Builder events(Map<EventTrigger, List<Function<Context>>> events) {
            this.events.putAll(events);
            return this;
        }

        @Override
        public Builder updater(ItemUpdateConfig updater) {
            this.updater = updater;
            return this;
        }

        @Override
        public ItemDefinition build() {
            this.processors.addAll(this.settings.processors());
            this.clientBoundProcessors.addAll(this.settings.clientBoundProcessors());
            return new BukkitItemDefinition(this.isVanillaItem, this.id, this.item, this.clientBoundItem, this.itemKey, this.clientBoundItemKey, this.behavior,
                    List.copyOf(this.processors), List.copyOf(this.clientBoundProcessors), this.settings, this.events, updater);
        }
    }
}
