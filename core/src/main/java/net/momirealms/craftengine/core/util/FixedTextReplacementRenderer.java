package net.momirealms.craftengine.core.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FixedTextReplacementRenderer implements ComponentRenderer<FixedTextReplacementRenderer.State> {
    static final FixedTextReplacementRenderer INSTANCE = new FixedTextReplacementRenderer();

    private FixedTextReplacementRenderer() {
    }

    @Override
    public @NotNull Component render(final Component component, final @NotNull State state) {
        final List<Component> oldChildren = component.children();
        final int oldChildrenSize = oldChildren.size();
        Style oldStyle = component.style();
        List<Component> children = null;
        Component modified = component;

        if (component instanceof TextComponent tc) {
            final String content = tc.content();
            // 当前节点完成全部匹配后才递归，因此本次渲染可以复用同一个 Matcher。
            final Matcher matcher = state.matcher.reset(content);
            int replacedUntil = 0;
            boolean firstMatch = true;
            while (matcher.find()) {
                if (matcher.start() == 0) {
                    if (matcher.end() == content.length()) {
                        final Component replacement = state.replacement.apply(matcher)
                                .style(s -> s.merge(component.style(), Style.Merge.Strategy.IF_ABSENT_ON_TARGET));
                        if (oldChildrenSize == 0) {
                            modified = replacement;
                            if (replacement.style().hoverEvent() != null) {
                                oldStyle = oldStyle.hoverEvent(null);
                            }
                        } else {
                            modified = Component.text("", component.style());
                            if (children == null) {
                                children = new ArrayList<>(oldChildrenSize + 1 + replacement.children().size());
                            }
                            children.add(replacement);
                        }
                    } else {
                        modified = Component.text("", component.style());
                        final Component child = state.replacement.apply(matcher);
                        if (children == null) {
                            children = new ArrayList<>(oldChildrenSize + 1);
                        }
                        children.add(child);
                    }
                } else {
                    if (children == null) {
                        children = new ArrayList<>(oldChildrenSize + 2);
                    }
                    if (firstMatch) {
                        modified = tc.content(content.substring(0, matcher.start()));
                    } else if (replacedUntil < matcher.start()) {
                        children.add(Component.text(content.substring(replacedUntil, matcher.start())));
                    }
                    children.add(state.replacement.apply(matcher));
                }
                firstMatch = false;
                replacedUntil = matcher.end();
            }
            if (replacedUntil > 0 && replacedUntil < content.length()) {
                if (children == null) {
                    children = new ArrayList<>(oldChildrenSize);
                }
                children.add(Component.text(content.substring(replacedUntil)));
            }
        } else if (modified instanceof TranslatableComponent translatable) {
            final List<TranslationArgument> args = translatable.arguments();
            List<TranslationArgument> newArgs = null;
            for (int i = 0, size = args.size(); i < size; i++) {
                final TranslationArgument original = args.get(i);
                TranslationArgument replaced = original;
                if (original.value() instanceof Component argument) {
                    Component rendered = this.render(argument, state);
                    if (rendered != argument) {
                        replaced = TranslationArgument.component(rendered);
                    }
                }
                if (replaced != original) {
                    if (newArgs == null) {
                        newArgs = new ArrayList<>(size);
                        for (int j = 0; j < i; j++) {
                            newArgs.add(args.get(j));
                        }
                    }
                }
                if (newArgs != null) {
                    newArgs.add(replaced);
                }
            }
            if (newArgs != null) {
                modified = translatable.arguments(newArgs);
            }
        }

        final HoverEvent<?> event = oldStyle.hoverEvent();
        if (event != null) {
            final HoverEvent<?> rendered = event.withRenderedValue(this, state);
            if (event != rendered) {
                modified = modified.style(s -> s.hoverEvent(rendered));
            }
        }

        boolean first = true;
        for (int i = 0; i < oldChildrenSize; i++) {
            final Component child = oldChildren.get(i);
            final Component replaced = this.render(child, state);
            if (replaced != child) {
                if (children == null) {
                    children = new ArrayList<>(oldChildrenSize);
                }
                if (first) {
                    for (int j = 0; j < i; j++) {
                        children.add(oldChildren.get(j));
                    }
                }
                first = false;
            }
            if (children != null) {
                children.add(replaced);
                first = false;
            }
        }

        if (children != null) {
            return modified.children(children);
        }
        return modified;
    }

    record State(Matcher matcher, Function<MatchResult, Component> replacement) {

        State(Pattern pattern, Function<MatchResult, Component> replacement) {
            this(pattern.matcher(""), replacement);
        }
    }
}
