package net.momirealms.craftengine.core.util.snbt.parse;

import javax.annotation.Nullable;

public interface Rule<S, T> {
    @Nullable
    T parse(ParseState<S> parseState);

    static <S, T> Rule<S, T> fromTerm(Term<S> child, RuleAction<S, T> action) {
        return new WrappedTerm<>(action, child);
    }

    static <S, T> Rule<S, T> fromTerm(Term<S> child, SimpleRuleAction<S, T> action) {
        return new WrappedTerm<>(action, child);
    }

    @FunctionalInterface
    interface RuleAction<S, T> {
        @Nullable
        T run(ParseState<S> parseState);
    }

    @FunctionalInterface
    interface SimpleRuleAction<S, T> extends RuleAction<S, T> {
        T run(Scope scope);

        @Override
        default T run(ParseState<S> parseState) {
            return this.run(parseState.scope());
        }
    }

    record WrappedTerm<S, T>(RuleAction<S, T> action, Term<S> child) implements Rule<S, T> {
        @Nullable
        @Override
        public T parse(ParseState<S> parseState) {
            Scope scope = parseState.scope();
            scope.pushFrame();

            T var3;
            try {
                if (!this.child.parse(parseState, scope, Control.UNBOUND)) {
                    return null;
                }

                var3 = this.action.run(parseState);
            } finally {
                scope.popFrame();
            }

            return var3;
        }
    }
}
