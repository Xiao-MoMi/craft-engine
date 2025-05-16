package net.momirealms.craftengine.core.pack.model.tint;

import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.incendo.cloud.type.Either;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface TintFactory {

    Tint create(Map<String, Object> arguments);

    default Either<Integer, List<Float>> parseTintValue(Object value) {
        if (value instanceof Number i) {
            return Either.ofPrimary(i.intValue());
        } else if (value instanceof List<?> list) {
            if (list.size() == 3) {
                List<String> strList = MiscUtils.getAsStringList(list);
                boolean hasDot = false;
                for (String str : strList) {
                    if (str.contains(".")) {
                        hasDot = true;
                        break;
                    }
                }
                List<Float> fList = new ArrayList<>();
                for (String str : strList) {
                    if (hasDot) {
                        fList.add(Float.parseFloat(str));
                    } else {
                        fList.add(convertToFloat(Integer.parseInt(str)));
                    }
                }
                return Either.ofFallback(fList);
            }
        } else if (value instanceof String s) {
            String[] split = s.split(",");
            if (split.length == 3) {
                List<Float> fList = new ArrayList<>();
                boolean hasDot = s.contains(".");
                for (String string : split) {
                    if (hasDot) {
                        fList.add(Float.parseFloat(string));
                    } else {
                        fList.add(convertToFloat(Integer.parseInt(string)));
                    }
                }
                return Either.ofFallback(fList);
            }
        }
        throw new LocalizedResourceConfigException("warning.config.item.model.tint.invalid_value", value.toString());
    }

    static float convertToFloat(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Tint value out of range: " + value + ". Allowed range is [0,255]");
        }
        return value / 255.0f;
    }
}
