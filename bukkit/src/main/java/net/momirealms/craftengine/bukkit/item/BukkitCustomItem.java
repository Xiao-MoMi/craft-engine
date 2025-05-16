package net.momirealms.craftengine.bukkit.item;

import com.google.common.collect.ImmutableMap;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.MaterialUtils;
import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.plugin.event.EventTrigger;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BukkitCustomItem implements CustomItem<ItemStack> {
    private final Key id;
    private final Key materialKey;
    private final Material material;
    private final ItemDataModifier<ItemStack>[] modifiers;
    private final Map<String, ItemDataModifier<ItemStack>> modifierMap;
    private final ItemDataModifier<ItemStack>[] clientBoundModifiers;
    private final Map<String, ItemDataModifier<ItemStack>> clientBoundModifierMap;
    private final NetworkItemDataProcessor<ItemStack>[] networkItemDataProcessors;
    private final List<ItemBehavior> behaviors;
    private final ItemSettings settings;
    private final EnumMap<EventTrigger, List<Function<PlayerOptionalContext>>> events;

    @SuppressWarnings("unchecked")
    public BukkitCustomItem(Key id,
                            Key materialKey,
                            Material material,
                            List<ItemDataModifier<ItemStack>> modifiers,
                            List<ItemDataModifier<ItemStack>> clientBoundModifiers,
                            List<ItemBehavior> behaviors,
                            ItemSettings settings,
                            EnumMap<EventTrigger, List<Function<PlayerOptionalContext>>> events) {
        this.id = id;
        this.material = material;
        this.materialKey = materialKey;
        this.events = events;
        // unchecked cast
        this.modifiers = modifiers.toArray(new ItemDataModifier[0]);
        // unchecked cast
        this.clientBoundModifiers = clientBoundModifiers.toArray(new ItemDataModifier[0]);
        this.behaviors = List.copyOf(behaviors);
        this.settings = settings;
        ImmutableMap.Builder<String, ItemDataModifier<ItemStack>> modifierMapBuilder = ImmutableMap.builder();
        for (ItemDataModifier<ItemStack> modifier : modifiers) {
            modifierMapBuilder.put(modifier.name(), modifier);
        }
        this.modifierMap = modifierMapBuilder.build();
        ImmutableMap.Builder<String, ItemDataModifier<ItemStack>> clientSideModifierMapBuilder = ImmutableMap.builder();
        List<NetworkItemDataProcessor<ItemStack>> networkItemDataProcessors = new ArrayList<>();
        for (ItemDataModifier<ItemStack> modifier : clientBoundModifiers) {
            String name = modifier.name();
            clientSideModifierMapBuilder.put(name, modifier);
            if (this.modifierMap.containsKey(name)) {
                networkItemDataProcessors.add(NetworkItemDataProcessor.both(this.modifierMap.get(name), modifier));
            } else {
                networkItemDataProcessors.add(NetworkItemDataProcessor.clientOnly(modifier));
            }
        }
        this.clientBoundModifierMap = clientSideModifierMapBuilder.build();
        // unchecked cast
        this.networkItemDataProcessors = networkItemDataProcessors.toArray(new NetworkItemDataProcessor[0]);
    }

    @Override
    public void execute(PlayerOptionalContext context, EventTrigger trigger) {
        for (Function<PlayerOptionalContext> function : Optional.ofNullable(this.events.get(trigger)).orElse(Collections.emptyList())) {
            function.run(context);
        }
    }

    @Override
    public Key id() {
        return this.id;
    }

    @Override
    public Key material() {
        return this.materialKey;
    }

    @Override
    public NetworkItemDataProcessor<ItemStack>[] networkItemDataProcessors() {
        return this.networkItemDataProcessors;
    }

    @Override
    public ItemDataModifier<ItemStack>[] dataModifiers() {
        return this.modifiers;
    }

    @Override
    public Map<String, ItemDataModifier<ItemStack>> dataModifierMap() {
        return this.modifierMap;
    }

    @Override
    public boolean hasClientBoundDataModifier() {
        return this.clientBoundModifiers.length != 0;
    }

    @Override
    public ItemDataModifier<ItemStack>[] clientBoundDataModifiers() {
        return this.clientBoundModifiers;
    }

    @Override
    public Map<String, ItemDataModifier<ItemStack>> clientBoundDataModifierMap() {
        return this.clientBoundModifierMap;
    }

    @Override
    public ItemStack buildItemStack(ItemBuildContext context, int count) {
        ItemStack item = new ItemStack(this.material);
        Item<ItemStack> wrapped = BukkitCraftEngine.instance().itemManager().wrap(item);
        wrapped.count(count);
        for (ItemDataModifier<ItemStack> modifier : this.modifiers) {
            modifier.apply(wrapped, context);
        }
        return wrapped.load();
    }

    @Override
    public ItemSettings settings() {
        return this.settings;
    }

    @Override
    public Item<ItemStack> buildItem(ItemBuildContext context) {
        ItemStack item = new ItemStack(this.material);
        Item<ItemStack> wrapped = BukkitCraftEngine.instance().itemManager().wrap(item);
        for (ItemDataModifier<ItemStack> modifier : dataModifiers()) {
            modifier.apply(wrapped, context);
        }
        return BukkitCraftEngine.instance().itemManager().wrap(wrapped.load());
    }

    @Override
    public @NotNull List<ItemBehavior> behaviors() {
        return this.behaviors;
    }

    public static Builder<ItemStack> builder() {
        return new BuilderImpl();
    }

    public static class BuilderImpl implements Builder<ItemStack> {
        private Key id;
        private Material material;
        private Key materialKey;
        private ItemSettings settings;
        private EnumMap<EventTrigger, List<Function<PlayerOptionalContext>>> events = new EnumMap<>(EventTrigger.class);
        private final List<ItemBehavior> behaviors = new ArrayList<>();
        private final List<ItemDataModifier<ItemStack>> modifiers = new ArrayList<>();
        private final List<ItemDataModifier<ItemStack>> clientBoundModifiers = new ArrayList<>();

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
        public Builder<ItemStack> dataModifier(ItemDataModifier<ItemStack> modifier) {
            this.modifiers.add(modifier);
            return this;
        }

        @Override
        public Builder<ItemStack> dataModifiers(List<ItemDataModifier<ItemStack>> modifiers) {
            this.modifiers.addAll(modifiers);
            return this;
        }

        @Override
        public Builder<ItemStack> clientBoundDataModifier(ItemDataModifier<ItemStack> modifier) {
            this.clientBoundModifiers.add(modifier);
            return this;
        }

        @Override
        public Builder<ItemStack> clientBoundDataModifiers(List<ItemDataModifier<ItemStack>> modifiers) {
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
        public Builder<ItemStack> events(EnumMap<EventTrigger, List<Function<PlayerOptionalContext>>> events) {
            this.events = events;
            return this;
        }

        @Override
        public CustomItem<ItemStack> build() {
            this.modifiers.addAll(this.settings.modifiers());
            return new BukkitCustomItem(this.id, this.materialKey, this.material, this.modifiers, this.clientBoundModifiers, this.behaviors, this.settings, this.events);
        }
    }
}
