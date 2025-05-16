package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.util.Key;

public final class CommonConditions {
    private CommonConditions() {}

    public static final Key EMPTY = Key.of("craftengine:empty");
    public static final Key ALL_OF = Key.of("craftengine:all_of");
    public static final Key ANY_OF = Key.of("craftengine:any_of");
    public static final Key INVERTED = Key.of("craftengine:inverted");
    public static final Key MATCH_ITEM = Key.of("craftengine:match_item");
    public static final Key MATCH_BLOCK_PROPERTY = Key.from("craftengine:match_block_property");
    public static final Key TABLE_BONUS = Key.from("craftengine:table_bonus");
    public static final Key SURVIVES_EXPLOSION = Key.from("craftengine:survives_explosion");
    public static final Key RANDOM = Key.from("craftengine:random");
    public static final Key ENCHANTMENT = Key.from("craftengine:enchantment");
    public static final Key FALLING_BLOCK = Key.from("craftengine:falling_block");
    public static final Key DISTANCE = Key.from("craftengine:distance");
    public static final Key PERMISSION = Key.from("craftengine:permission");
    public static final Key EQUALS = Key.from("craftengine:equals");
    public static final Key EXPRESSION = Key.from("craftengine:expression");
}
