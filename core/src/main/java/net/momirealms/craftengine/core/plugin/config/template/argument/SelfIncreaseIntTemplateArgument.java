package net.momirealms.craftengine.core.plugin.config.template.argument;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.util.Map;

public final class SelfIncreaseIntTemplateArgument implements TemplateArgument {
    public static final TemplateArgumentFactory<SelfIncreaseIntTemplateArgument> FACTORY = new Factory();
    private final int min;
    private final int max;
    private int current;
    private final int step;
    private final int stepInterval;
    private int callCount;

    public SelfIncreaseIntTemplateArgument(int min, int max, int step, int stepInterval) {
        this.min = min;
        this.max = max;
        this.current = min;
        this.step = step;
        this.stepInterval = stepInterval;
        this.callCount = 0;
    }

    @Override
    public String get(String node, Map<String, TemplateArgument> arguments) {
        String value = String.valueOf(this.current);
        this.callCount++;
        if (this.stepInterval <= 0 || this.callCount % this.stepInterval == 0) {
            if (this.current + this.step <= this.max) {
                this.current += this.step;
            } else {
                this.current = this.max;
            }
        }
        return value;
    }

    public int min() {
        return this.min;
    }

    public int max() {
        return this.max;
    }

    public int current() {
        return this.current;
    }

    public int step() {
        return this.step;
    }

    public int stepInterval() {
        return this.stepInterval;
    }

    public int callCount() {
        return this.callCount;
    }

    private static class Factory implements TemplateArgumentFactory<SelfIncreaseIntTemplateArgument> {
        private static final String[] STEP_INTERVAL = new String[]{"step_interval", "step-interval"};

        @Override
        public SelfIncreaseIntTemplateArgument create(ConfigSection section) {
            return new SelfIncreaseIntTemplateArgument(
                    section.getNonNullInt("from"),
                    section.getNonNullInt("to"),
                    section.getInt("step", 1),
                    section.getInt(STEP_INTERVAL, 1)
            );
        }
    }
}
