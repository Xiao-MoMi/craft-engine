package net.momirealms.craftengine.core.plugin.gui.category.source;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class RegexCategorySource implements CategorySource {
    private final List<Pattern> patterns;

    private RegexCategorySource(List<Pattern> patterns) {
        this.patterns = List.copyOf(patterns);
    }

    static RegexCategorySource fromConfig(ConfigSection section) {
        List<String> expressions = section.getNonNullStringList("regex");
        List<Pattern> patterns = new ArrayList<>(expressions.size());
        for (int i = 0; i < expressions.size(); i++) {
            String expression = expressions.get(i);
            try {
                patterns.add(Pattern.compile(expression));
            } catch (PatternSyntaxException e) {
                throw new KnownResourceException(
                        "category.source.invalid_regex",
                        section.assemblePath("regex", i),
                        e,
                        expression
                );
            }
        }
        return new RegexCategorySource(patterns);
    }

    @Override
    public List<String> resolve(CategorySourceContext context) {
        List<String> resolved = new ArrayList<>();
        for (Pattern pattern : this.patterns) {
            for (Key itemId : context.itemIds()) {
                if (pattern.matcher(itemId.asString()).matches()) {
                    resolved.add(itemId.asString());
                }
            }
        }
        return resolved;
    }
}
