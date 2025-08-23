package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.input.SmithingInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CustomSmithingTransformRecipe<T> extends AbstractedFixedResultRecipe<T> {
    public static final Serializer<?> SERIALIZER = new Serializer<>();
    private final Ingredient<T> base;
    private final Ingredient<T> template;
    private final Ingredient<T> addition;
    private final boolean mergeComponents;
    private final List<ItemDataProcessor> processors;

    public CustomSmithingTransformRecipe(Key id,
                                         boolean showNotification,
                                         @Nullable Ingredient<T> template,
                                         @NotNull Ingredient<T> base,
                                         @Nullable Ingredient<T> addition,
                                         CustomRecipeResult<T> result,
                                         List<ItemDataProcessor> processors,
                                         boolean mergeComponents
    ) {
        super(id, showNotification, result);
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

    private boolean checkIngredient(Ingredient<T> ingredient, UniqueIdItem<T> item) {
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
    public @NotNull Key serializerType() {
        return RecipeSerializers.SMITHING_TRANSFORM;
    }

    @Override
    public RecipeType type() {
        return RecipeType.SMITHING;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T assemble(RecipeInput input, ItemBuildContext context) {
        SmithingInput<T> smithingInput = ((SmithingInput<T>) input);
        Item<T> base = smithingInput.base().item();
        T result = this.result(context);
        Item<T> wrappedResult = (Item<T>) CraftEngine.instance().itemManager().wrap(result);
        Item<T> finalResult = wrappedResult;
        if (this.mergeComponents) {
            finalResult = base.mergeCopy(wrappedResult);
        }
        if (this.processors != null) {
            for (ItemDataProcessor processor : this.processors) {
                processor.accept(base, wrappedResult, finalResult);
            }
        }
        return finalResult.getItem();
    }

    @NotNull
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
    public static class Serializer<A> extends AbstractRecipeSerializer<A, CustomSmithingTransformRecipe<A>> {

        @Override
        public CustomSmithingTransformRecipe<A> readMap(Key id, Map<String, Object> arguments) {
            List<String> base = MiscUtils.getAsStringList(arguments.get("base"));
            List<String> template = MiscUtils.getAsStringList(arguments.get("template-type"));
            List<String> addition = MiscUtils.getAsStringList(arguments.get("addition"));
            boolean mergeComponents = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("merge-components", true), "merge-components");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> processors = (List<Map<String, Object>>) arguments.getOrDefault("post-processors", List.of());
            return new CustomSmithingTransformRecipe<>(id,
                    showNotification(arguments),
                    toIngredient(template),
                    ResourceConfigUtils.requireNonNullOrThrow(toIngredient(base), "warning.config.recipe.smithing_transform.missing_base"),
                    toIngredient(addition),
                    parseResult(arguments),
                    ItemDataProcessors.fromMapList(processors),
                    mergeComponents
            );
        }

        @Override
        public CustomSmithingTransformRecipe<A> readJson(Key id, JsonObject json) {
            return new CustomSmithingTransformRecipe<>(id,
                    true, toIngredient(VANILLA_RECIPE_HELPER.singleIngredient(json.get("template"))), Objects.requireNonNull(toIngredient(VANILLA_RECIPE_HELPER.singleIngredient(json.get("base")))), toIngredient(VANILLA_RECIPE_HELPER.singleIngredient(json.get("addition"))), parseResult(VANILLA_RECIPE_HELPER.smithingResult(json.getAsJsonObject("result"))), null, true
            );
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
            ((WritableRegistry<ItemDataProcessor.ProcessorFactory>) BuiltInRegistries.SMITHING_RESULT_PROCESSOR_FACTORY)
                    .register(ResourceKey.create(Registries.SMITHING_RESULT_PROCESSOR_FACTORY.location(), key), factory);
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
                    item3.setExactComponent(component, componentObj);
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
