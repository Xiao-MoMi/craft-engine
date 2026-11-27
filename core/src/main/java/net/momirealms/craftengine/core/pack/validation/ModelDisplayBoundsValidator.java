package net.momirealms.craftengine.core.pack.validation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.model.ModelJsonKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ModelDisplayBoundsValidator {

    private ModelDisplayBoundsValidator() {
    }

    public static List<Problem> validate(JsonObject model) {
        if (!(model.get(ModelJsonKeys.DISPLAY) instanceof JsonObject display)) {
            return List.of();
        }
        List<Problem> problems = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : display.entrySet()) {
            if (entry.getValue() instanceof JsonObject transform) {
                validateVector(transform, entry.getKey(), ModelJsonKeys.TRANSLATION, 80, problems);
                validateVector(transform, entry.getKey(), ModelJsonKeys.SCALE, 4, problems);
            }
        }
        return problems;
    }

    private static void validateVector(JsonObject transform, String position, String field, int limit, List<Problem> problems) {
        if (!(transform.get(field) instanceof JsonArray array) || array.size() != 3) {
            return;
        }
        boolean outOfBounds = false;
        for (JsonElement value : array) {
            if (!(value instanceof JsonPrimitive primitive) || !primitive.isNumber() || !Double.isFinite(primitive.getAsDouble())) {
                return;
            }
            double number = primitive.getAsDouble();
            outOfBounds |= number < -limit || number > limit;
        }
        if (outOfBounds) {
            problems.add(new Problem(position, field, array.toString(), limit));
        }
    }

    public static int fix(JsonObject model, List<Problem> problems) {
        if (!(model.get(ModelJsonKeys.DISPLAY) instanceof JsonObject display)) {
            return 0;
        }
        int fixed = 0;
        for (Problem problem : problems) {
            if (!(display.get(problem.position()) instanceof JsonObject transform) || !(transform.get(problem.field()) instanceof JsonArray array) || array.size() != 3) {
                continue;
            }
            boolean changed = false;
            for (int i = 0; i < 3; i++) {
                double value = array.get(i).getAsDouble();
                if (value < -problem.limit() || value > problem.limit()) {
                    array.set(i, new JsonPrimitive(Math.clamp(value, -problem.limit(), problem.limit())));
                    changed = true;
                }
            }
            if (changed) {
                fixed++;
            }
        }
        return fixed;
    }

    public record Problem(String position, String field, String vector, int limit) {
    }
}
