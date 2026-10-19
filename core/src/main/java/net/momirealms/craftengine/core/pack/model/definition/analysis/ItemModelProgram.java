package net.momirealms.craftengine.core.pack.model.definition.analysis;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.BundleSelectedItemModel;
import net.momirealms.craftengine.core.pack.model.definition.CompositeItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ConditionItemModel;
import net.momirealms.craftengine.core.pack.model.definition.EmptyItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ModernItemModel;
import net.momirealms.craftengine.core.pack.model.definition.RangeDispatchItemModel;
import net.momirealms.craftengine.core.pack.model.definition.SelectItemModel;
import net.momirealms.craftengine.core.pack.model.definition.SpecialItemModel;
import net.momirealms.craftengine.core.pack.model.definition.Transformation;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public record ItemModelProgram(ModernItemModel definition, ItemModelNode root) {

    public static ItemModelProgram compile(ModernItemModel definition) {
        return new ItemModelProgram(definition, compile(definition.model(), "$.model"));
    }

    private static ItemModelNode compile(ItemModel model, String path) {
        if (model == null) {
            return new ItemModelNode.Missing(path);
        }
        if (model instanceof BaseItemModel base) {
            return new ItemModelNode.Model(path, base);
        }
        if (model instanceof SpecialItemModel special) {
            return new ItemModelNode.Special(path, special);
        }
        if (model instanceof EmptyItemModel) {
            return new ItemModelNode.Empty(path);
        }
        if (model instanceof BundleSelectedItemModel) {
            return new ItemModelNode.BundleSelected(path);
        }
        if (model instanceof CompositeItemModel composite) {
            List<ItemModelNode> children = new ArrayList<>(composite.models().size());
            for (int i = 0; i < composite.models().size(); i++) {
                children.add(compile(composite.models().get(i), path + ".models[" + i + ']'));
            }
            return new ItemModelNode.Composite(path, children, composite.transformation());
        }
        if (model instanceof ConditionItemModel condition) {
            return new ItemModelNode.Condition(path, condition.property(), compile(condition.onTrue(), path + ".on_true"),
                    compile(condition.onFalse(), path + ".on_false"), condition.transformation());
        }
        if (model instanceof SelectItemModel select) {
            if (select.whenMap().isEmpty()) {
                throw new IllegalArgumentException("Empty select cases at " + path);
            }
            Map<JsonElement, ItemModelNode> cases = new LinkedHashMap<>();
            int index = 0;
            for (Map.Entry<Either<JsonElement, List<JsonElement>>, ItemModel> entry : select.whenMap().entrySet()) {
                ItemModelNode child = compile(entry.getValue(), path + ".cases[" + index++ + "].model");
                List<JsonElement> values = entry.getKey().map(List::of, list -> list);
                if (values.isEmpty()) {
                    throw new IllegalArgumentException("Empty select case at " + child.path());
                }
                for (JsonElement value : values) {
                    if (cases.putIfAbsent(value.deepCopy(), child) != null) {
                        throw new IllegalArgumentException("Duplicate select case value at " + child.path() + ": " + value);
                    }
                }
            }
            return new ItemModelNode.Select(path, select.property(), cases,
                    compile(select.fallBack(), path + ".fallback"), select.transformation());
        }
        if (model instanceof RangeDispatchItemModel range) {
            List<ItemModelNode.Entry> entries = new ArrayList<>(range.entries().size());
            for (int i = 0; i < range.entries().size(); i++) {
                RangeDispatchItemModel.Entry entry = range.entries().get(i);
                entries.add(new ItemModelNode.Entry(entry.threshold(), compile(entry.model(), path + ".entries[" + i + "].model")));
            }
            // Stable sorting preserves equal thresholds, including Java's linear/binary-search distinction.
            entries.sort(Comparator.comparingDouble(ItemModelNode.Entry::threshold));
            return new ItemModelNode.Range(path, range.property(), range.scale(), entries,
                    compile(range.fallBack(), path + ".fallback"), range.transformation());
        }
        throw new IllegalArgumentException("Unknown Java item model at " + path + ": " + model.getClass().getName());
    }

    public List<Layer> evaluate(ItemModelContext context) {
        List<Layer> layers = new ArrayList<>();
        evaluate(this.root, context, List.of(), layers);
        return List.copyOf(layers);
    }

    private static void evaluate(ItemModelNode node, ItemModelContext context, List<Transformation> inherited, List<Layer> layers) {
        if (node instanceof ItemModelNode.Empty) {
            return;
        }
        if (node instanceof ItemModelNode.BundleSelected) {
            // Java renders the selected item's own definition and does not inherit the bundle's transformation.
            layers.add(new Layer(node, List.of()));
            return;
        }
        List<Transformation> transforms = inherited;
        if (node.transformation() != null) {
            transforms = new ArrayList<>(inherited.size() + 1);
            transforms.addAll(inherited);
            transforms.add(node.transformation());
        }
        if (node instanceof ItemModelNode.Composite composite) {
            for (ItemModelNode child : composite.children()) {
                evaluate(child, context, transforms, layers);
            }
        } else if (node instanceof ItemModelNode.Condition condition) {
            evaluate(context.condition(condition.property()) ? condition.onTrue() : condition.onFalse(), context, transforms, layers);
        } else if (node instanceof ItemModelNode.Select select) {
            JsonElement value = context.select(select.property());
            ItemModelNode selected = value == null ? select.fallback() : select.cases().getOrDefault(value, select.fallback());
            evaluate(selected, context, transforms, layers);
        } else if (node instanceof ItemModelNode.Range range) {
            evaluate(range.select(context.number(range.property())), context, transforms, layers);
        } else {
            layers.add(new Layer(node, transforms));
        }
    }

    public void visit(Consumer<ItemModelNode> visitor) {
        visit(this.root, visitor);
    }

    private static void visit(ItemModelNode node, Consumer<ItemModelNode> visitor) {
        visitor.accept(node);
        if (node instanceof ItemModelNode.Composite composite) {
            for (ItemModelNode child : composite.children()) {
                visit(child, visitor);
            }
        } else if (node instanceof ItemModelNode.Condition condition) {
            visit(condition.onTrue(), visitor);
            visit(condition.onFalse(), visitor);
        } else if (node instanceof ItemModelNode.Select select) {
            for (ItemModelNode child : select.cases().values()) {
                visit(child, visitor);
            }
            visit(select.fallback(), visitor);
        } else if (node instanceof ItemModelNode.Range range) {
            for (ItemModelNode.Entry entry : range.entries()) {
                visit(entry.model(), visitor);
            }
            visit(range.fallback(), visitor);
        }
    }

    public record Layer(ItemModelNode source, List<Transformation> transformations) {

        public Layer {
            transformations = List.copyOf(transformations);
        }

        public Matrix4f matrix() {
            Matrix4f matrix = new Matrix4f();
            for (Transformation transformation : this.transformations) {
                matrix.mul(transformation.toMatrix());
            }
            return matrix;
        }
    }
}
