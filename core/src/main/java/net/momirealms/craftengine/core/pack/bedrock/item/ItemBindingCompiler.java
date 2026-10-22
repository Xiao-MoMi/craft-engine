package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.model.definition.*;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ItemBindingCompiler {

    private ItemBindingCompiler() {
    }

    public static Map<Key, ItemDefinitionBinding> compile(JavaResourcePack pack) throws IOException {
        Map<Key, ItemDefinitionBinding> bindings = new LinkedHashMap<>();
        for (Map.Entry<Key, JsonObject> entry : pack.itemDefinitions().entrySet()) {
            bindings.put(entry.getKey(), compile(entry.getKey(), entry.getValue()));
        }
        return bindings;
    }

    public static ItemDefinitionBinding compile(Key definitionId, JsonObject json) throws IOException {
        ModernItemModel definition;
        try {
            definition = ModernItemModel.fromJson(json);
        } catch (IllegalArgumentException | IllegalStateException | NullPointerException exception) {
            throw new IOException("Failed to parse item definition " + definitionId + " at " + ResourcePackUtils.itemDefinitionPath(definitionId) + ": " + exception.getMessage(), exception);
        }
        Builder builder = new Builder();
        builder.visit(definition.model(), "$.model");
        return new ItemDefinitionBinding(
                definitionId,
                definition.handAnimationOnSwap(),
                definition.oversizedInGui(),
                definition.swapAnimationScale(),
                builder.rules
        );
    }

    private static final class Builder {
        private final List<ItemBindingCondition> conditions = new ObjectArrayList<>();
        private final List<Transformation> transformations = new ObjectArrayList<>();
        private final List<ItemBindingRule> rules = new ObjectArrayList<>();

        private void visit(ItemModel model, String path) {
            Transformation transformation = switch (model) {
                case BaseItemModel base -> base.transformation();
                case ConditionItemModel condition -> condition.transformation();
                case SelectItemModel select -> select.transformation();
                case RangeDispatchItemModel range -> range.transformation();
                case CompositeItemModel composite -> composite.transformation();
                case SpecialItemModel special -> special.transformation();
                default -> null;
            };
            if (transformation != null) {
                this.transformations.add(transformation);
            }
            // 条件和变换在当前组合层内累积；组合的外层条件和变换保留在组合规则上。
            switch (model) {
                case BaseItemModel base -> this.emit(new ItemRenderTarget.Model(base.path(), base.tints()), path);
                case SpecialItemModel special -> this.emit(new ItemRenderTarget.Special(special.base(), special.specialModel()), path);
                case EmptyItemModel ignored -> this.emit(ItemRenderTarget.Builtin.EMPTY, path);
                case BundleSelectedItemModel ignored -> this.emit(ItemRenderTarget.Builtin.BUNDLE_SELECTED_ITEM, path);
                case ConditionItemModel condition -> {
                    this.branch(new ItemBindingCondition.BooleanMatch(condition.property(), true), condition.onTrue(), path + ".on_true");
                    this.branch(new ItemBindingCondition.BooleanMatch(condition.property(), false), condition.onFalse(), path + ".on_false");
                }
                case SelectItemModel select -> this.select(select, path);
                case RangeDispatchItemModel range -> this.range(range, path);
                case CompositeItemModel composite -> {
                    Builder children = new Builder();
                    for (int i = 0; i < composite.models().size(); i++) {
                        children.visit(composite.models().get(i), path + ".models[" + i + "]");
                    }
                    this.emit(new ItemRenderTarget.Composite(children.rules), path);
                }
                default ->
                        throw new IllegalArgumentException("Unsupported item model at " + path + ": " + model.getClass().getName());
            }
            if (transformation != null) {
                this.transformations.removeLast();
            }
        }

        private void select(SelectItemModel select, String path) {
            List<JsonElement> allValues = new ObjectArrayList<>();
            int index = 0;
            for (Map.Entry<Either<JsonElement, List<JsonElement>>, ItemModel> entry : select.whenMap().entrySet()) {
                List<JsonElement> values = entry.getKey().map(List::of, list -> list);
                allValues.addAll(values);
                this.branch(
                        new ItemBindingCondition.SelectMatch(select.property(), values, false),
                        entry.getValue(),
                        path + ".cases[" + index++ + "].model"
                );
            }
            this.branch(
                    new ItemBindingCondition.SelectMatch(select.property(), allValues, true),
                    select.fallBack(),
                    path + ".fallback"
            );
        }

        private void range(RangeDispatchItemModel range, String path) {
            List<IndexedEntry> entries = new ObjectArrayList<>(range.entries().size());
            for (int i = 0; i < range.entries().size(); i++) {
                entries.add(new IndexedEntry(i, range.entries().get(i)));
            }
            entries.sort(Comparator.comparingDouble(entry -> entry.entry().threshold()));
            Float firstThreshold = entries.isEmpty() ? null : entries.getFirst().entry().threshold();
            this.branch(new ItemBindingCondition.NumericRange(
                            range.property(),
                            range.scale(),
                            null,
                            firstThreshold
                    ),
                    range.fallBack(),
                    path + ".fallback"
            );
            for (int i = 0; i < entries.size(); i++) {
                IndexedEntry indexed = entries.get(i);
                float threshold = indexed.entry().threshold();
                Float nextThreshold = i + 1 < entries.size() ? entries.get(i + 1).entry().threshold() : null;
                // A later equal threshold wins; only the last entry has a non-empty interval.
                if (nextThreshold != null && threshold == nextThreshold) {
                    continue;
                }
                this.branch(new ItemBindingCondition.NumericRange(
                                range.property(),
                                range.scale(),
                                threshold,
                                nextThreshold
                        ),
                        indexed.entry().model(),
                        path + ".entries[" + indexed.index() + "].model"
                );
            }
        }

        private void branch(ItemBindingCondition condition, ItemModel model, String path) {
            this.conditions.add(condition);
            if (model == null) {
                this.emit(ItemRenderTarget.Builtin.MISSING, path);
            } else {
                this.visit(model, path);
            }
            this.conditions.removeLast();
        }

        private void emit(ItemRenderTarget target, String path) {
            this.rules.add(new ItemBindingRule(this.conditions, target, this.transformations, path));
        }
    }

    private record IndexedEntry(int index, RangeDispatchItemModel.Entry entry) {
    }
}
