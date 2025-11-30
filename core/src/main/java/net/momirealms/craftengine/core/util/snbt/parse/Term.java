package net.momirealms.craftengine.core.util.snbt.parse;

import java.util.ArrayList;
import java.util.List;

public interface Term<S> {
    boolean parse(ParseState<S> parseState, Scope scope, Control control);

    static <S, T> Term<S> marker(Atom<T> name, T value) {
        return new Marker<>(name, value);
    }

    @SafeVarargs
    static <S> Term<S> sequence(Term<S>... elements) {
        return new Sequence<>(elements);
    }

    @SafeVarargs
    static <S> Term<S> alternative(Term<S>... elements) {
        return new Alternative<>(elements);
    }

    static <S> Term<S> optional(Term<S> term) {
        return new Maybe<>(term);
    }

    static <S, T> Term<S> repeated(NamedRule<S, T> element, Atom<List<T>> listName) {
        return repeated(element, listName, 0);
    }

    static <S, T> Term<S> repeated(NamedRule<S, T> element, Atom<List<T>> listName, int minRepetitions) {
        return new Repeated<>(element, listName, minRepetitions);
    }

    static <S, T> Term<S> repeatedWithTrailingSeparator(NamedRule<S, T> element, Atom<List<T>> listName, Term<S> seperator) {
        return repeatedWithTrailingSeparator(element, listName, seperator, 0);
    }

    static <S, T> Term<S> repeatedWithTrailingSeparator(NamedRule<S, T> element, Atom<List<T>> listName, Term<S> seperator, int minRepetitions) {
        return new RepeatedWithSeparator<>(element, listName, seperator, minRepetitions, true);
    }

    static <S> Term<S> positiveLookahead(Term<S> term) {
        return new LookAhead<>(term, true);
    }

    static <S> Term<S> cut() {
        return new Term<>() {
            @Override
            public boolean parse(ParseState<S> parseState, Scope scope, Control control) {
                control.cut();
                return true;
            }

            @Override
            public String toString() {
                return "↑";
            }
        };
    }

    static <S> Term<S> empty() {
        return new Term<>() {
            @Override
            public boolean parse(ParseState<S> parseState, Scope scope, Control control) {
                return true;
            }

            @Override
            public String toString() {
                return "ε";
            }
        };
    }

    static <S> Term<S> fail(final Object reason) {
        return new Term<>() {
            @Override
            public boolean parse(ParseState<S> parseState, Scope scope, Control control) {
                parseState.errorCollector().store(parseState.mark(), reason);
                return false;
            }

            @Override
            public String toString() {
                return "fail";
            }
        };
    }

    record Alternative<S>(Term<S>[] elements) implements Term<S> {
        @Override
        public boolean parse(ParseState<S> parseState, Scope scope, Control control) {
            Control control1 = parseState.acquireControl();

            try {
                int i = parseState.mark();
                scope.splitFrame();

                for (Term<S> term : this.elements) {
                    if (term.parse(parseState, scope, control1)) {
                        scope.mergeFrame();
                        return true;
                    }

                    scope.clearFrameValues();
                    parseState.restore(i);
                    if (control1.hasCut()) {
                        break;
                    }
                }

                scope.popFrame();
                return false;
            } finally {
                parseState.releaseControl();
            }
        }
    }

    record LookAhead<S>(Term<S> term, boolean positive) implements Term<S> {
        @Override
        public boolean parse(ParseState<S> parseState, Scope scope, Control control) {
            int i = parseState.mark();
            boolean flag = this.term.parse(parseState.silent(), scope, control);
            parseState.restore(i);
            return this.positive == flag;
        }
    }

    record Marker<S, T>(Atom<T> name, T value) implements Term<S> {
        @Override
        public boolean parse(ParseState<S> parseState, Scope scope, Control control) {
            scope.put(this.name, this.value);
            return true;
        }
    }

    record Maybe<S>(Term<S> term) implements Term<S> {
        @Override
        public boolean parse(ParseState<S> parseState, Scope scope, Control control) {
            int i = parseState.mark();
            if (!this.term.parse(parseState, scope, control)) {
                parseState.restore(i);
            }

            return true;
        }
    }

    record Repeated<S, T>(NamedRule<S, T> element, Atom<List<T>> listName, int minRepetitions) implements Term<S> {
        @Override
        public boolean parse(ParseState<S> parseState, Scope scope, Control control) {
            int i = parseState.mark();
            List<T> list = new ArrayList<>(this.minRepetitions);

            while (true) {
                int i1 = parseState.mark();
                T object = parseState.parse(this.element);
                if (object == null) {
                    parseState.restore(i1);
                    if (list.size() < this.minRepetitions) {
                        parseState.restore(i);
                        return false;
                    } else {
                        scope.put(this.listName, list);
                        return true;
                    }
                }

                list.add(object);
            }
        }
    }

    record RepeatedWithSeparator<S, T>(
        NamedRule<S, T> element, Atom<List<T>> listName, Term<S> separator, int minRepetitions, boolean allowTrailingSeparator
    ) implements Term<S> {
        @Override
        public boolean parse(ParseState<S> parseState, Scope scope, Control control) {
            int i = parseState.mark();
            List<T> list = new ArrayList<>(this.minRepetitions);
            boolean flag = true;

            while (true) {
                int i1 = parseState.mark();
                if (!flag && !this.separator.parse(parseState, scope, control)) {
                    parseState.restore(i1);
                    break;
                }

                int i2 = parseState.mark();
                T object = parseState.parse(this.element);
                if (object == null) {
                    if (flag) {
                        parseState.restore(i2);
                    } else {
                        if (!this.allowTrailingSeparator) {
                            parseState.restore(i);
                            return false;
                        }

                        parseState.restore(i2);
                    }
                    break;
                }

                list.add(object);
                flag = false;
            }

            if (list.size() < this.minRepetitions) {
                parseState.restore(i);
                return false;
            } else {
                scope.put(this.listName, list);
                return true;
            }
        }
    }

    record Sequence<S>(Term<S>[] elements) implements Term<S> {
        @Override
        public boolean parse(ParseState<S> parseState, Scope scope, Control control) {
            int i = parseState.mark();

            for (Term<S> term : this.elements) {
                if (!term.parse(parseState, scope, control)) {
                    parseState.restore(i);
                    return false;
                }
            }

            return true;
        }
    }
}
