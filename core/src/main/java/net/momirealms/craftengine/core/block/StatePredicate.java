package net.momirealms.craftengine.core.block;

public class StatePredicate {
    private static Object TRUE;
    private static Object FALSE;

    public static void init(Object alwaysTrue, Object alwaysFalse) {
        TRUE = alwaysTrue;
        FALSE = alwaysFalse;
    }

    public static Object alwaysTrue() {
        return TRUE;
    }

    public static Object alwaysFalse() {
        return FALSE;
    }
}
