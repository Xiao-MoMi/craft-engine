package net.momirealms.craftengine.core.item.modifier;

import com.google.common.collect.ImmutableMap;
import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HideTooltipModifier<I> implements ItemDataModifier<I> {
    public static final Map<Key, Integer> TO_LEGACY;
    static {
        ImmutableMap.Builder<Key, Integer> builder = ImmutableMap.builder();
        builder.put(ComponentKeys.ENCHANTMENTS, 1);
        builder.put(ComponentKeys.ATTRIBUTE_MODIFIERS, 2);
        builder.put(ComponentKeys.UNBREAKABLE, 4);
        builder.put(ComponentKeys.CAN_BREAK, 8);
        builder.put(ComponentKeys.CAN_PLACE_ON, 16);
        builder.put(ComponentKeys.STORED_ENCHANTMENTS, 32);
        builder.put(ComponentKeys.POTION_CONTENTS, 32);
        builder.put(ComponentKeys.WRITTEN_BOOK_CONTENT, 32);
        builder.put(ComponentKeys.FIREWORKS, 32);
        builder.put(ComponentKeys.FIREWORK_EXPLOSION, 32);
        builder.put(ComponentKeys.BUNDLE_CONTENTS, 32);
        builder.put(ComponentKeys.MAP_ID, 32);
        builder.put(ComponentKeys.MAP_COLOR, 32);
        builder.put(ComponentKeys.MAP_DECORATIONS, 32);
        builder.put(ComponentKeys.DYED_COLOR, 64);
        builder.put(ComponentKeys.TRIM, 128);
        TO_LEGACY = builder.build();
    }

    private final List<Key> components;
    private final Applier<I> applier;

    public HideTooltipModifier(List<Key> components) {
        this.components = components;
        if (VersionHelper.isOrAbove1_21_5()) {
            this.applier = new ModernApplier<>(components);
        } else if (VersionHelper.isOrAbove1_20_5()) {
            if (components.isEmpty()) {
                this.applier = new DummyApplier<>();
            } else if (components.size() == 1) {
                this.applier = new SemiModernApplier<>(components.getFirst());
            } else {
                List<Applier<I>> appliers = new ArrayList<>();
                for (Key key : components) {
                    appliers.add(new SemiModernApplier<>(key));
                }
                this.applier = new CompoundApplier<>(appliers);
            }
        } else {
            this.applier = new LegacyApplier<>(components);
        }
    }

    public List<Key> components() {
        return components;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        this.applier.apply(item);
        return item;
    }

    @Override
    public Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        if (VersionHelper.isOrAbove1_21_5()) {
            Tag previous = item.getSparrowNBTComponent(ComponentKeys.TOOLTIP_DISPLAY);
            if (previous != null) {
                networkData.put(ComponentKeys.TOOLTIP_DISPLAY.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put(ComponentKeys.TOOLTIP_DISPLAY.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        } else if (VersionHelper.isOrAbove1_20_5()) {
            for (Key component : this.components) {
                Tag previous = item.getSparrowNBTComponent(component);
                if (previous != null) {
                    networkData.put(component.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
                } else {
                    networkData.put(component.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
                }
            }
        } else {
            Tag previous = item.getTag("HideFlags");
            if (previous != null) {
                networkData.put("HideFlags", NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put("HideFlags", NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        }
        return item;
    }

    @Override
    public String name() {
        return "hide-tooltip";
    }

    public interface Applier<I> {

        void apply(Item<I> item);
    }

    public static class DummyApplier<T> implements Applier<T> {

        @Override
        public void apply(Item<T> item) {
        }
    }

    public static class SemiModernApplier<I> implements Applier<I> {
        private final Key component;

        public SemiModernApplier(Key component) {
            this.component = component;
        }

        @Override
        public void apply(Item<I> item) {
            Tag previous = item.getSparrowNBTComponent(this.component);
            if (previous instanceof CompoundTag compoundTag) {
                compoundTag.putBoolean("show_in_tooltip", false);
                item.setNBTComponent(this.component, compoundTag);
            } else {
                CompoundTag compoundTag = new CompoundTag();
                compoundTag.putBoolean("show_in_tooltip", false);
                item.setNBTComponent(this.component, compoundTag);
            }
        }
    }

    public record CompoundApplier<I>(List<Applier<I>> appliers) implements Applier<I> {

        @Override
        public void apply(Item<I> item) {
            for (Applier<I> applier : appliers) {
                applier.apply(item);
            }
        }
    }

    public static class LegacyApplier<W> implements Applier<W> {
        private final int legacyValue;

        public LegacyApplier(List<Key> components) {
            int i = 0;
            for (Key key : components) {
                Integer flag = TO_LEGACY.get(key);
                if (flag != null) {
                    i += flag;
                }
            }
            this.legacyValue = i;
        }

        public int legacyValue() {
            return legacyValue;
        }

        @Override
        public void apply(Item<W> item) {
            Integer previousFlags = (Integer) item.getJavaTag("HideFlags");
            if (previousFlags != null) {
                item.setTag(this.legacyValue | previousFlags, "HideFlags");
            } else {
                item.setTag(this.legacyValue, "HideFlags");
            }
        }
    }

    public static class ModernApplier<W> implements Applier<W> {
        private final List<String> components;

        public ModernApplier(List<Key> components) {
            this.components = components.stream().map(Key::toString).collect(Collectors.toList());
        }

        public List<String> components() {
            return components;
        }

        @Override
        public void apply(Item<W> item) {
            Map<String, Object> data = MiscUtils.castToMap(item.getJavaComponent(ComponentKeys.TOOLTIP_DISPLAY), true);
            if (data == null) {
                item.setJavaComponent(ComponentKeys.TOOLTIP_DISPLAY, Map.of("hidden_components", this.components));
            } else {
                if (data.get("hidden_components") instanceof List<?> list) {
                    List<String> hiddenComponents = list.stream().map(Object::toString).toList();
                    List<String> mergedComponents = Stream.concat(
                            hiddenComponents.stream(),
                            this.components.stream()
                    ).distinct().toList();
                    Map<String, Object> newData = new HashMap<>(data);
                    newData.put("hidden_components", mergedComponents);
                    item.setJavaComponent(ComponentKeys.TOOLTIP_DISPLAY, newData);
                } else {
                    Map<String, Object> newData = new HashMap<>(data);
                    newData.put("hidden_components", this.components);
                    item.setJavaComponent(ComponentKeys.TOOLTIP_DISPLAY, newData);
                }
            }
        }
    }
}
