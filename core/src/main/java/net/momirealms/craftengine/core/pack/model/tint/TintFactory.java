package net.momirealms.craftengine.core.pack.model.tint;

import org.incendo.cloud.type.Either;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface TintFactory {

    Tint create(Map<String, Object> arguments);

    default Either<Integer, List<Integer>> parseTintValue(Object value) {
        if (value instanceof Number i) {
            return Either.ofPrimary(i.intValue());
        } else if (value instanceof List<?> list) {
            if (list.size() != 3) {
                throw new IllegalArgumentException("Invalid tint value list size: " + list.size() + " which is expected to be 3");
            }
            List<Integer> intList = new ArrayList<>();
            for (Object o : list) {
                intList.add(Integer.parseInt(o.toString()));
            }
            return Either.ofFallback(intList);
        } else if (value instanceof String s) {
            String[] split = s.split(",");
            if (split.length != 3) {
                throw new IllegalArgumentException("Invalid tint value list size: " + split.length + " which is expected to be 3");
            }
            List<Integer> intList = new ArrayList<>();
            for (String string : split) {
                intList.add(Integer.parseInt(string));
            }
            return Either.ofFallback(intList);
        }
        throw new IllegalArgumentException("Invalid tint value: " + value);
    }
}
