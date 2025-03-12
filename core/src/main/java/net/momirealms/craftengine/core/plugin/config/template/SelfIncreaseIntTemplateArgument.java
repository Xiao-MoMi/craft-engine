package net.momirealms.craftengine.core.plugin.config.template;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.Map;

public class SelfIncreaseIntTemplateArgument implements TemplateArgument {
    public static final Factory FACTORY = new Factory();
    private final int min;
    private final int max;
    private int current;

    public SelfIncreaseIntTemplateArgument(int min, int max) {
        this.min = min;
        this.max = max;
        this.current = min;
    }

    @Override
    public String get() {
        String value = String.valueOf(current);
        if (current < max) current += 1;
        return value;
    }

    @Override
    public Key type() {
        return TemplateArguments.SELF_INCREASE_INT;
    }

    public int current() {
        return current;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public static class Factory implements TemplateArgumentFactory {
        @Override
        public TemplateArgument create(Map<String, Object> arguments) {
            int from = MiscUtils.getAsInt(arguments.get("from"));
            int to = MiscUtils.getAsInt(arguments.get("to"));
            if (from > to) throw new IllegalArgumentException("from > to");
            return new SelfIncreaseIntTemplateArgument(from, to);
        }
    }
}
