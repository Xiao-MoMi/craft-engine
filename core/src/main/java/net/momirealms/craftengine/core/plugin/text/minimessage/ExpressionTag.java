package net.momirealms.craftengine.core.plugin.text.minimessage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.expression.ContextExpression;
import net.momirealms.craftengine.core.plugin.context.text.StringTag;
import net.momirealms.craftengine.core.util.FastDecimalFormat;
import net.momirealms.sparrow.message.ParsingException;
import net.momirealms.sparrow.message.tag.Tag;
import net.momirealms.sparrow.message.tag.resolver.ArgumentQueue;
import net.momirealms.sparrow.message.tag.resolver.StaticTagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class ExpressionTag extends StaticTagResolver implements StringTag {
    public static final ExpressionTag INSTANCE = new ExpressionTag();
    public static final Cache<String, ContextExpression<Context>> CACHE = Caffeine.newBuilder()
            .maximumSize(256)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();
    public static final Cache<String, FastDecimalFormat> FORMAT_CACHE = Caffeine.newBuilder()
            .maximumSize(64)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    private ExpressionTag() {
        super("expr");
    }

    public static void clearCaches() {
        CACHE.invalidateAll();
        FORMAT_CACHE.invalidateAll();
    }

    @Override
    public Tag resolve(
            @NotNull String name,
            @NotNull ArgumentQueue arguments,
            @NotNull net.momirealms.sparrow.message.Context ctx
    ) throws ParsingException {
        String format = arguments.popOr("No format provided").toString();
        String expr = arguments.popOr("No expression provided").toString();

        ContextExpression<Context> compiled = CACHE.get(expr, ContextExpression::compile);
        Context context = ctx.target() instanceof Context target ? target : null;
        if (format.equals("bool")) {
            final boolean value;
            try {
                value = compiled.test(context);
            } catch (final RuntimeException e) {
                throw ctx.newException("Invalid expression: " + expr, e, arguments);
            }
            return Tag.selfClosingInserting(Component.text(Boolean.toString(value)));
        }
        final double numberValue;
        try {
            numberValue = compiled.evaluate(context);
        } catch (final RuntimeException e) {
            throw ctx.newException("Invalid expression: " + expr, e, arguments);
        }
        final FastDecimalFormat df;
        try {
            df = FORMAT_CACHE.get(format, FastDecimalFormat::new);
        } catch (final IllegalArgumentException e) {
            throw ctx.newException("Invalid number format: " + format, arguments);
        }
        return Tag.selfClosingInserting(Component.text(df.format(numberValue)));
    }

    @Override
    public String resolve(String[] args, net.momirealms.craftengine.core.plugin.context.Context context) {
        String format = StringTag.requireArg(args, 0, "No format provided");
        String expr = StringTag.requireArg(args, 1, "No expression provided");
        ContextExpression<Context> compiled = CACHE.get(expr, ContextExpression::compile);
        if (format.equals("bool")) {
            return Boolean.toString(compiled.test(context));
        }
        return FORMAT_CACHE.get(format, FastDecimalFormat::new).format(compiled.evaluate(context));
    }

    @Override
    public StringTag precompile(String[] args) {
        final String format = StringTag.requireArg(args, 0, "No format provided");
        final String rawExpression = StringTag.requireArg(args, 1, "No expression provided");
        final ContextExpression<Context> compiled = CACHE.get(rawExpression, ContextExpression::compile);
        if (format.equals("bool")) {
            return (boundArgs, context) -> compiled.test(context);
        }
        final FastDecimalFormat df = FORMAT_CACHE.get(format, FastDecimalFormat::new);
        return (boundArgs, context) -> df.format(compiled.evaluate(context));
    }
}
