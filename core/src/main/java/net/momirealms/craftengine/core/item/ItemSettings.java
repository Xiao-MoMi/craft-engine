package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.entity.ItemDisplayContext;
import net.momirealms.craftengine.core.entity.projectile.ProjectileMeta;
import net.momirealms.craftengine.core.item.modifier.EquippableModifier;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.pack.misc.EquipmentGeneration;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;

public class ItemSettings {
    int fuelTime;
    Set<Key> tags = Set.of();
    @Nullable
    EquipmentGeneration equipment;
    boolean canRepair = true;
    List<AnvilRepairItem> anvilRepairItems = List.of();
    boolean renameable = true;
    boolean canPlaceRelatedVanillaBlock = false;
    ProjectileMeta projectileMeta;
    boolean dyeable = true;

    private ItemSettings() {}

    public <I> List<ItemDataModifier<I>> modifiers() {
        ArrayList<ItemDataModifier<I>> modifiers = new ArrayList<>();
        if (VersionHelper.isOrAbove1_21_2() && this.equipment != null && this.equipment.modernData() != null) modifiers.add(new EquippableModifier<>(this.equipment.modernData()));
        // TODO 1.20 leather armor
        return modifiers;
    }

    public static ItemSettings of() {
        return new ItemSettings();
    }

    public static ItemSettings fromMap(Map<String, Object> map) {
        if (map == null) return ItemSettings.of();
        return applyModifiers(ItemSettings.of(), map);
    }

    public static ItemSettings ofFullCopy(ItemSettings settings) {
        ItemSettings newSettings = of();
        newSettings.fuelTime = settings.fuelTime;
        newSettings.tags = settings.tags;
        newSettings.equipment = settings.equipment;
        newSettings.canRepair = settings.canRepair;
        newSettings.anvilRepairItems = settings.anvilRepairItems;
        newSettings.renameable = settings.renameable;
        newSettings.canPlaceRelatedVanillaBlock = settings.canPlaceRelatedVanillaBlock;
        newSettings.projectileMeta = settings.projectileMeta;
        newSettings.dyeable = settings.dyeable;
        return newSettings;
    }

    public static ItemSettings applyModifiers(ItemSettings settings, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            ItemSettings.Modifier.Factory factory = ItemSettings.Modifiers.FACTORIES.get(entry.getKey());
            if (factory != null) {
                factory.createModifier(entry.getValue()).apply(settings);
            } else {
                throw new LocalizedResourceConfigException("warning.config.item.settings.unknown", entry.getKey());
            }
        }
        return settings;
    }

    public ProjectileMeta projectileMeta() {
        return projectileMeta;
    }

    public boolean canPlaceRelatedVanillaBlock() {
        return canPlaceRelatedVanillaBlock;
    }

    public boolean canRepair() {
        return canRepair;
    }

    public int fuelTime() {
        return fuelTime;
    }

    public boolean renameable() {
        return renameable;
    }

    public Set<Key> tags() {
        return tags;
    }

    public boolean dyeable() {
        return dyeable;
    }

    public List<AnvilRepairItem> repairItems() {
        return anvilRepairItems;
    }

    @Nullable
    public EquipmentGeneration equipment() {
        return equipment;
    }

    public ItemSettings repairItems(List<AnvilRepairItem> items) {
        this.anvilRepairItems = items;
        return this;
    }

    public ItemSettings canRepair(boolean canRepair) {
        this.canRepair = canRepair;
        return this;
    }

    public ItemSettings renameable(boolean renameable) {
        this.renameable = renameable;
        return this;
    }

    public ItemSettings projectileMeta(ProjectileMeta projectileMeta) {
        this.projectileMeta = projectileMeta;
        return this;
    }

    public ItemSettings canPlaceRelatedVanillaBlock(boolean canPlaceRelatedVanillaBlock) {
        this.canPlaceRelatedVanillaBlock = canPlaceRelatedVanillaBlock;
        return this;
    }

    public ItemSettings fuelTime(int fuelTime) {
        this.fuelTime = fuelTime;
        return this;
    }

    public ItemSettings tags(Set<Key> tags) {
        this.tags = tags;
        return this;
    }

    public ItemSettings equipment(EquipmentGeneration equipment) {
        this.equipment = equipment;
        return this;
    }

    public ItemSettings dyeable(boolean bool) {
        this.dyeable = bool;
        return this;
    }

    @FunctionalInterface
    public interface Modifier {

        void apply(ItemSettings settings);

        @FunctionalInterface
        interface Factory {

            ItemSettings.Modifier createModifier(Object value);
        }
    }

    public static class Modifiers {
        private static final Map<String, ItemSettings.Modifier.Factory> FACTORIES = new HashMap<>();

        static {
            registerFactory("repairable", (value -> {
                boolean bool = (boolean) value;
                return settings -> settings.canRepair(bool);
            }));
            registerFactory("renameable", (value -> {
                boolean bool = (boolean) value;
                return settings -> settings.renameable(bool);
            }));
            registerFactory("anvil-repair-item", (value -> {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> materials = (List<Map<String, Object>>) value;
                List<AnvilRepairItem> anvilRepairItemList = new ArrayList<>();
                for (Map<String, Object> material : materials) {
                    int amount = ResourceConfigUtils.getAsInt(material.getOrDefault("amount", 0), "amount");
                    double percent = ResourceConfigUtils.getAsDouble(material.getOrDefault("percent", 0), "percent");
                    anvilRepairItemList.add(new AnvilRepairItem(MiscUtils.getAsStringList(material.get("target")), amount, percent));
                }
                return settings -> settings.repairItems(anvilRepairItemList);
            }));
            registerFactory("fuel-time", (value -> {
                int intValue = ResourceConfigUtils.getAsInt(value, "fuel-time");
                return settings -> settings.fuelTime(intValue);
            }));
            registerFactory("tags", (value -> {
                List<String> tags = MiscUtils.getAsStringList(value);
                return settings -> settings.tags(tags.stream().map(Key::of).collect(Collectors.toSet()));
            }));
            registerFactory("equippable", (value -> {
                Map<String, Object> args = MiscUtils.castToMap(value, false);
                EquipmentData data;
                if (VersionHelper.isOrAbove1_21_2() && args.containsKey("slot")) data = EquipmentData.fromMap(args);
                else data = null;
                EquipmentGeneration equipment = new EquipmentGeneration(
                        EquipmentGeneration.Layer.fromConfig(args.get("humanoid")),
                        EquipmentGeneration.Layer.fromConfig(args.get("humanoid-leggings")),
                        EquipmentGeneration.Layer.fromConfig(args.get("llama-body")),
                        EquipmentGeneration.Layer.fromConfig(args.get("horse-body")),
                        EquipmentGeneration.Layer.fromConfig(args.get("wolf-body")),
                        EquipmentGeneration.Layer.fromConfig(args.get("wings")),
                        data,
                        ResourceConfigUtils.getAsInt(args.getOrDefault("trim", -1), "trim")
                );
                return settings -> settings.equipment(equipment);
            }));
            registerFactory("can-place", (value -> {
                boolean bool = (boolean) value;
                return settings -> settings.canPlaceRelatedVanillaBlock(bool);
            }));
            registerFactory("projectile", (value -> {
                Map<String, Object> args = MiscUtils.castToMap(value, false);
                Key customTridentItemId = Key.of(Objects.requireNonNull(args.get("item"), "'item should not be null'").toString());
                ItemDisplayContext displayType = ItemDisplayContext.valueOf(args.getOrDefault("display-transform", "NONE").toString().toUpperCase(Locale.ENGLISH));
                Vector3f translation = MiscUtils.getAsVector3f(args.getOrDefault("translation", "0"), "translation");
                Vector3f scale = MiscUtils.getAsVector3f(args.getOrDefault("scale", "1"), "scale");
                Quaternionf rotation = MiscUtils.getAsQuaternionf(ResourceConfigUtils.get(args, "rotation-left", "rotation"), "rotation-left");
                String type = args.getOrDefault("type", "none").toString();
                return settings -> settings.projectileMeta(new ProjectileMeta(customTridentItemId, displayType, scale, translation, rotation, type));
            }));
            registerFactory("dyeable", (value -> {
                boolean bool = (boolean) value;
                return settings -> settings.dyeable(bool);
            }));
        }

        private static void registerFactory(String id, ItemSettings.Modifier.Factory factory) {
            FACTORIES.put(id, factory);
        }
    }
}
