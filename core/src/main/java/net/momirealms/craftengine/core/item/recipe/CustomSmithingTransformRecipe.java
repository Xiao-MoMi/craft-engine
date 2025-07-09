package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.input.SmithingInput;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CustomSmithingTransformRecipe<T> implements Recipe<T> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final Key id;
    private final CustomRecipeResult<T> result;
    private final Ingredient<T> base;
    private final Ingredient<T> template;
    private final Ingredient<T> addition;
    private final boolean mergeComponents;
    private final List<ItemDataProcessor> processors;

    public CustomSmithingTransformRecipe(Key id,
                                         @Nullable Ingredient<T> base,
                                         @Nullable Ingredient<T> template,
                                         @Nullable Ingredient<T> addition,
                                         CustomRecipeResult<T> result,
                                         boolean mergeComponents,
                                         List<ItemDataProcessor> processors
    ) {
        this.id = id;
        this.result = result;
        this.base = base;
        this.template = template;
        this.addition = addition;
        this.processors = processors;
        this.mergeComponents = mergeComponents;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(RecipeInput input) {
        SmithingInput<T> smithingInput = (SmithingInput<T>) input;
        return checkIngredient(this.base, smithingInput.base())
                && checkIngredient(this.template, smithingInput.template())
                && checkIngredient(this.addition, smithingInput.addition());
    }

    private boolean checkIngredient(Ingredient<T> ingredient, OptimizedIDItem<T> item) {
        if (ingredient != null) {
            if (item == null || item.isEmpty()) {
                return false;
            }
            return ingredient.test(item);
        } else {
            return item == null || item.isEmpty();
        }
    }

    @Override
    public List<Ingredient<T>> ingredientsInUse() {
        List<Ingredient<T>> ingredients = new ArrayList<>();
        ingredients.add(this.base);
        if (this.template != null) {
            ingredients.add(this.template);
        }
        if (this.addition != null) {
            ingredients.add(this.addition);
        }
        return ingredients;
    }

    @Override
    public @NotNull Key type() {
        return RecipeTypes.SMITHING_TRANSFORM;
    }

    @Override
    public Key id() {
        return this.id;
    }

    @Override
    public T result(ItemBuildContext context) {
        return this.result.buildItemStack(context);
    }

    @SuppressWarnings("unchecked")
    public T assemble(ItemBuildContext context, Item<T> base) {
        T result = this.result(context);
        Item<T> wrappedResult = (Item<T>) CraftEngine.instance().itemManager().wrap(result);
        Item<T> finalResult = wrappedResult;
        if (this.mergeComponents) {
            finalResult = base.mergeCopy(wrappedResult);
        }
        for (ItemDataProcessor processor : this.processors) {
            processor.accept(base, wrappedResult, finalResult);
        }
        return finalResult.getItem();
    }

    @Override
    public CustomRecipeResult<T> result() {
        return this.result;
    }

    @Nullable
    public Ingredient<T> base() {
        return this.base;
    }

    @Nullable
    public Ingredient<T> template() {
        return template;
    }

    @Nullable
    public Ingredient<T> addition() {
        return addition;
    }

    @SuppressWarnings({"DuplicatedCode"})
    public static class Factory<A> implements RecipeFactory<A> {

        @Override
        public Recipe<A> create(Key id, Map<String, Object> arguments) {
            List<String> base = MiscUtils.getAsStringList(arguments.get("base"));
            List<String> addition = MiscUtils.getAsStringList(arguments.get("addition"));
            List<String> template = MiscUtils.getAsStringList(arguments.get("template-type"));
            boolean mergeComponents = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("merge-components", true), "merge-components");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> processors = (List<Map<String, Object>>) arguments.getOrDefault("post-processors", List.of());
            return new CustomSmithingTransformRecipe<>(
                    id,
                    toIngredient(base), toIngredient(template),toIngredient(addition), parseResult(arguments),
                    mergeComponents,
                    ItemDataProcessors.fromMapList(processors)
            );
        }

        private Ingredient<A> toIngredient(List<String> items) {
            Set<Holder<Key>> holders = new HashSet<>();
            for (String item : items) {
                if (item.charAt(0) == '#') {
                    holders.addAll(CraftEngine.instance().itemManager().tagToItems(Key.of(item.substring(1))));
                } else {
                    holders.add(BuiltInRegistries.OPTIMIZED_ITEM_ID.get(Key.of(item)).orElseThrow(
                            () -> new LocalizedResourceConfigException("warning.config.recipe.invalid_item", item)));
                }
            }
            return holders.isEmpty() ? null : Ingredient.of(holders);
        }
    }

    public static class ItemDataProcessors {
        public static final Key KEEP_COMPONENTS = Key.of("craftengine:keep_components");
        public static final Key KEEP_TAGS = Key.of("craftengine:keep_tags");

        static {
            if (VersionHelper.isOrAbove1_20_5()) {
                register(KEEP_COMPONENTS, KeepComponents.FACTORY);
            } else {
                register(KEEP_TAGS, KeepTags.FACTORY);
            }
        }

        public static List<ItemDataProcessor> fromMapList(List<Map<String, Object>> mapList) {
            if (mapList == null || mapList.isEmpty()) return List.of();
            List<ItemDataProcessor> functions = new ArrayList<>();
            for (Map<String, Object> map : mapList) {
                functions.add(fromMap(map));
            }
            return functions;
        }

        public static ItemDataProcessor fromMap(Map<String, Object> map) {
            String type = (String) map.get("type");
            if (type == null) {
                throw new LocalizedResourceConfigException("warning.config.recipe.smithing_transform.post_processor.missing_type");
            }
            Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
            ItemDataProcessor.ProcessorFactory factory = BuiltInRegistries.SMITHING_RESULT_PROCESSOR_FACTORY.getValue(key);
            if (factory == null) {
                throw new LocalizedResourceConfigException("warning.config.recipe.smithing_transform.post_processor.invalid_type", type);
            }
            return factory.create(map);
        }

        public static void register(Key key, ItemDataProcessor.ProcessorFactory factory) {
            Holder.Reference<ItemDataProcessor.ProcessorFactory> holder = ((WritableRegistry<ItemDataProcessor.ProcessorFactory>) BuiltInRegistries.SMITHING_RESULT_PROCESSOR_FACTORY)
                    .registerForHolder(new ResourceKey<>(Registries.SMITHING_RESULT_PROCESSOR_FACTORY.location(), key));
            holder.bindValue(factory);
        }
    }

    public interface ItemDataProcessor extends TriConsumer<Item<?>, Item<?>, Item<?>> {

        Key type();

        interface ProcessorFactory {
            ItemDataProcessor create(Map<String, Object> arguments);
        }
    }

    public static class KeepComponents implements ItemDataProcessor {
        public static final Factory FACTORY = new Factory();
        private final List<Key> components;

        public KeepComponents(List<Key> components) {
            this.components = components;
        }

        @Override
        public void accept(Item<?> item1, Item<?> item2, Item<?> item3) {
            for (Key component : this.components) {
                Object componentObj = item1.getExactComponent(component);
                if (componentObj != null) {
                    item3.setComponent(component, componentObj);
                }
            }
        }

        @Override
        public Key type() {
            return ItemDataProcessors.KEEP_COMPONENTS;
        }

        public static class Factory implements ProcessorFactory {

            @Override
            public ItemDataProcessor create(Map<String, Object> arguments) {
                Object componentsObj = arguments.get("components");
                if (componentsObj == null) {
                    throw new LocalizedResourceConfigException("warning.config.recipe.smithing_transform.post_processor.keep_component.missing_components");
                }
                List<String> components = MiscUtils.getAsStringList(componentsObj);
                return new KeepComponents(components.stream().map(Key::of).toList());
            }
        }
    }

    public static class KeepTags implements ItemDataProcessor {
        public static final Factory FACTORY = new Factory();
        private final List<String[]> tags;

        public KeepTags(List<String[]> tags) {
            this.tags = tags;
        }

        @Override
        public void accept(Item<?> item1, Item<?> item2, Item<?> item3) {
            for (String[] tag : this.tags) {
                Object tagObj = item1.getJavaTag((Object[]) tag);
                if (tagObj != null) {
                    item3.setTag(tagObj, (Object[]) tag);
                }
            }
        }

        @Override
        public Key type() {
            return ItemDataProcessors.KEEP_TAGS;
        }

        public static class Factory implements ProcessorFactory {

            @Override
            public ItemDataProcessor create(Map<String, Object> arguments) {
                Object tagsObj = arguments.get("tags");
                if (tagsObj == null) {
                    throw new LocalizedResourceConfigException("warning.config.recipe.smithing_transform.post_processor.keep_component.missing_tags");
                }
                List<String> tags = MiscUtils.getAsStringList(tagsObj);
                return new KeepTags(tags.stream().map(it -> it.split("\\.")).toList());
            }
        }
    }
}
