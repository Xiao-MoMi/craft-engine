package net.momirealms.craftengine.core.plugin.text.minimessage;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.AdventureHelper;

public interface FormattedLine {

    Component parse(net.momirealms.craftengine.core.plugin.context.Context context);

    boolean isConstant();

    static FormattedLine create(String line) {
        if (line.equals(AdventureHelper.customMiniMessage().stripTags(line))) {
            return new PreParsedLine(AdventureHelper.miniMessage().deserialize(line));
        } else {
            return new DynamicLine(line);
        }
    }

    class PreParsedLine implements FormattedLine {
        private final Component parsed;

        public PreParsedLine(Component parsed) {
            this.parsed = parsed;
        }

        @Override
        public Component parse(net.momirealms.craftengine.core.plugin.context.Context context) {
            return this.parsed;
        }

        @Override
        public boolean isConstant() {
            return true;
        }
    }

    class DynamicLine implements FormattedLine {
        private final String content;

        public DynamicLine(String content) {
            this.content = content;
        }

        @Override
        public Component parse(net.momirealms.craftengine.core.plugin.context.Context context) {
            return AdventureHelper.deserialize(this.content, context);
        }

        @Override
        public boolean isConstant() {
            return false;
        }
    }
}
