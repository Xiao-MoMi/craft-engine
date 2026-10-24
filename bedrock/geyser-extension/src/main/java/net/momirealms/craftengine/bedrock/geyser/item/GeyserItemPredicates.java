package net.momirealms.craftengine.bedrock.geyser.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ChargedProjectile;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.predicate.item.ItemConditionPredicate;
import org.geysermc.geyser.api.predicate.item.ItemMatchPredicate;
import org.geysermc.geyser.api.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class GeyserItemPredicates {

    private GeyserItemPredicates() {
    }

    public static String property(JsonObject condition) {
        String property = condition.get(PROPERTY).getAsString();
        return property.startsWith("minecraft:") ? property.substring(10) : property;
    }

    public static MinecraftPredicate<ItemPredicateContext> read(JsonObject condition) {
        String property = property(condition);
        if (condition.has(EXPECTED)) {
            return readBoolean(condition, property);
        }
        if (condition.has(VALUES)) {
            return readSelect(condition, property);
        }
        return readRange(condition, property);
    }

    private static MinecraftPredicate<ItemPredicateContext> readBoolean(JsonObject condition, String property) {
        if (property.equals("selected")) {
            return new Selected(condition.get(EXPECTED).getAsBoolean());
        }
        MinecraftPredicate<ItemPredicateContext> predicate = switch (property) {
            case "broken" -> ItemConditionPredicate.BROKEN;
            case "damaged" -> context -> !context.unbreakable() && context.maxDamage() > 0 && context.damage() > 0;
            case "fishing_rod/cast" -> ItemConditionPredicate.FISHING_ROD_CAST;
            case "has_component" -> {
                if (condition.has("ignore_default") && condition.get("ignore_default").getAsBoolean()) {
                    throw new UnsupportedOperationException("Geyser API cannot distinguish default components: " + condition);
                }
                yield ItemConditionPredicate.hasComponent(Identifier.of(condition.get("component").getAsString()));
            }
            default -> throw unsupported(property);
        };
        return condition.get(EXPECTED).getAsBoolean() ? predicate : predicate.negate();
    }

    private static MinecraftPredicate<ItemPredicateContext> readSelect(JsonObject condition, String property) {
        if (property.equals("context_dimension")) {
            List<String> dimensions = new ArrayList<>();
            for (JsonElement value : condition.getAsJsonArray(VALUES)) {
                dimensions.add(Identifier.of(value.getAsString()).toString());
            }
            return new Dimension(List.copyOf(dimensions), condition.get(NEGATED).getAsBoolean());
        }
        List<MinecraftPredicate<ItemPredicateContext>> predicates = new ArrayList<>();
        for (JsonElement value : condition.getAsJsonArray(VALUES)) {
            predicates.add(switch (property) {
                case "trim_material" -> ItemMatchPredicate.trimMaterial(Identifier.of(value.getAsString()));
                case "charge_type" -> ItemMatchPredicate.chargeType(ChargedProjectile.ChargeType.valueOf(value.getAsString().toUpperCase(Locale.ROOT)));
                default -> throw unsupported(property);
            });
        }
        boolean negated = condition.get(NEGATED).getAsBoolean();
        return context -> {
            for (MinecraftPredicate<ItemPredicateContext> predicate : predicates) {
                if (predicate.test(context)) {
                    return !negated;
                }
            }
            return negated;
        };
    }

    private static MinecraftPredicate<ItemPredicateContext> readRange(JsonObject condition, String property) {
        boolean normalize = !condition.has("normalize") || condition.get("normalize").getAsBoolean();
        ToDoubleFunction<ItemPredicateContext> value = switch (property) {
            case "damage" -> context -> normalize ? (float) context.damage() / Math.max(1, context.maxDamage()) : context.damage();
            case "count" -> context -> normalize ? (float) context.count() / context.maxStackSize() : context.count();
            case "bundle/fullness" -> ItemPredicateContext::bundleFullness;
            default -> throw unsupported(property);
        };
        float scale = condition.get(SCALE).getAsFloat();
        Float min = condition.has(MIN) ? condition.get(MIN).getAsFloat() : null;
        Float max = condition.has(MAX) ? condition.get(MAX).getAsFloat() : null;
        return context -> {
            float scaled = (float) value.applyAsDouble(context) * scale;
            return (min == null || scaled >= min) && (max == null || !(scaled >= max));
        };
    }

    private static UnsupportedOperationException unsupported(String property) {
        return new UnsupportedOperationException(property.equals("custom_model_data")
                ? "custom_model_data branch mapping is not enabled"
                : "Geyser API does not expose the item model property: " + property);
    }

    static boolean requiresSession(MinecraftPredicate<? super ItemPredicateContext> predicate) {
        return predicate instanceof Selected || predicate instanceof Dimension;
    }

    static boolean usesSelection(MinecraftPredicate<? super ItemPredicateContext> predicate) {
        return predicate instanceof Selected;
    }

    static MinecraftPredicate<? super ItemPredicateContext> bind(MinecraftPredicate<? super ItemPredicateContext> predicate,
                                                                Supplier<String> dimension, BooleanSupplier selected) {
        if (predicate instanceof Selected match) {
            return context -> selected.getAsBoolean() == match.expected();
        }
        if (predicate instanceof Dimension match) {
            // context.dimension() 是维度类型；Java context_dimension 使用实际世界 ID。
            return context -> {
                String world = dimension.get();
                return (world != null && match.values().contains(world)) != match.negated();
            };
        }
        return predicate;
    }

    private record Selected(boolean expected) implements MinecraftPredicate<ItemPredicateContext> {
        @Override
        public boolean test(ItemPredicateContext context) {
            return !this.expected;
        }
    }

    private record Dimension(List<String> values, boolean negated) implements MinecraftPredicate<ItemPredicateContext> {
        @Override
        public boolean test(ItemPredicateContext context) {
            throw new IllegalStateException("Dimension predicate requires a Geyser session");
        }
    }
}
