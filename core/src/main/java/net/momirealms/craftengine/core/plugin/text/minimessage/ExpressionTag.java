package net.momirealms.craftengine.core.plugin.text.minimessage;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class ExpressionTag implements TagResolver {
    public static final TagResolver INSTANCE = new ExpressionTag();

    private ExpressionTag() {}

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
        if (!has(name)) {
            return null;
        }

        String format = arguments.popOr("No format provided").toString();
        String expr = arguments.popOr("No expression provided").toString();

        Component resultComponent = ctx.deserialize(expr);
        String resultString = AdventureHelper.plainTextContent(resultComponent);
        Expression expression = new Expression(resultString);

        try {
            Number numberValue = expression.evaluate().getNumberValue();
            DecimalFormat df = new DecimalFormat(format);
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            df.setDecimalFormatSymbols(symbols);
            String formatted = df.format(numberValue);
            return Tag.selfClosingInserting(Component.text(formatted));
        } catch (IllegalArgumentException e) {
            throw ctx.newException("Invalid number format: " + format, arguments);
        } catch (EvaluationException | ParseException e) {
            throw ctx.newException("Invalid expression: " + e.getMessage(), arguments);
        }
    }

    @Override
    public boolean has(@NotNull String name) {
        return "expr".equals(name);
    }
}