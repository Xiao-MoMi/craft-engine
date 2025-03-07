package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.item.modifier.*;
import net.momirealms.craftengine.core.pack.model.generator.AbstractModelGenerator;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.TypeUtils;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.*;
import java.util.function.Function;

public abstract class AbstractItemManager<I> extends AbstractModelGenerator implements ItemManager<I> {
    protected final Map<String, Function<Object, ItemModifier<I>>> dataFunctions = new HashMap<>();
    protected final Map<Key, CustomItem<I>> customItems = new HashMap<>();

    private void registerDataFunction(Function<Object, ItemModifier<I>> function, String... alias) {
        for (String a : alias) {
            dataFunctions.put(a, function);
        }
    }

    @Override
    public void unload() {
        this.customItems.clear();
        super.clearModelsToGenerate();
    }

    @Override
    public Optional<CustomItem<I>> getCustomItem(Key key) {
        return Optional.ofNullable(this.customItems.get(key));
    }

    public AbstractItemManager(CraftEngine plugin) {
        super(plugin);
        this.registerFunctions();
    }

    private void registerFunctions() {
        registerDataFunction((obj) -> {
            String name = obj.toString();
            return new DisplayNameModifier<>(name);
        }, "name", "display-name", "custom-name");
        registerDataFunction((obj) -> {
            List<String> name = MiscUtils.getAsStringList(obj);
            return new LoreModifier<>(name);
        }, "lore", "display-lore", "description");
        registerDataFunction((obj) -> {
            Map<String, Object> data = MiscUtils.castToMap(obj, false);
            return new TagsModifier<>(data);
        }, "tags", "tag", "nbt");
        registerDataFunction((obj) -> {
            boolean value = TypeUtils.checkType(obj, Boolean.class);
            return new UnbreakableModifier<>(value);
        }, "unbreakable");
        registerDataFunction((obj) -> {
            Map<String, Object> data = MiscUtils.castToMap(obj, false);
            List<Enchantment> enchantments = new ArrayList<>();
            for (Map.Entry<String, Object> e : data.entrySet()) {
                if (e.getValue() instanceof Number number) {
                    enchantments.add(new Enchantment(Key.of(e.getKey()), number.intValue()));
                }
            }
            return new EnchantmentModifier<>(enchantments);
        }, "enchantment", "enchantments", "enchant");
        if (VersionHelper.isVersionNewerThan1_20_5()) {
            registerDataFunction((obj) -> {
                String name = obj.toString();
                return new ItemNameModifier<>(name);
            }, "item-name");
        }
        if (VersionHelper.isVersionNewerThan1_20_5()) {
            registerDataFunction((obj) -> {
                Map<String, Object> data = MiscUtils.castToMap(obj, false);
                return new ComponentModifier<>(data);
            }, "components", "component");
        }
        if (VersionHelper.isVersionNewerThan1_21()) {
            registerDataFunction((obj) -> {
                String song = obj.toString();
                return new JukeboxSongModifier<>(Key.of(song));
            }, "jukebox-playable");
        }
        if (VersionHelper.isVersionNewerThan1_21_2()) {
            registerDataFunction((obj) -> {
                String id = obj.toString();
                return new TooltipStyleModifier<>(Key.of(id));
            }, "tooltip-style");
        }
    }
}
