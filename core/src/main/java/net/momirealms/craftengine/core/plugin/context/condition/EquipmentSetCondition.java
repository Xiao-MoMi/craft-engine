package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.entity.LivingEntity;
import net.momirealms.craftengine.core.entity.LivingEntityHolder;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;

public final class EquipmentSetCondition<CTX extends Context> implements Condition<CTX> {
    private final Key set;
    private final int min;
    private final int max;

    private EquipmentSetCondition(Key set, int min, int max) {
        this.set = set;
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(CTX ctx) {
        LivingEntity entity = resolveLivingEntity(ctx);
        if (entity == null) {
            return false;
        }
        LivingEntityHolder holder = CraftEngine.instance().entityManager().getEntityHolder(entity.uuid());
        return holder != null && matches(holder.equipments.countSetPieces(this.set));
    }

    boolean matches(int pieces) {
        return pieces >= this.min && pieces <= this.max;
    }

    private static LivingEntity resolveLivingEntity(Context ctx) {
        return ctx.getOptionalParameter(DirectContextParameters.ENTITY)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .orElse(null);
    }

    public static <CTX extends Context> ConditionFactory<CTX, EquipmentSetCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, EquipmentSetCondition<CTX>> {
        private static final String[] SET = ConfigKeys.of("set|id");
        private static final String[] COUNT = ConfigKeys.of("count|amount");
        private static final String[] MIN = ConfigKeys.of("min|min_count");
        private static final String[] MAX = ConfigKeys.of("max|max_count");

        @Override
        public EquipmentSetCondition<CTX> create(ConfigSection section) {
            Key set = section.getNonNullIdentifier(SET);
            ConfigValue countValue = section.getValue(COUNT);
            if (countValue != null) {
                int count = countValue.getAsInt(0);
                return new EquipmentSetCondition<>(set, count, count);
            }

            int min = section.getValue(MIN, value -> value.getAsInt(0), 1);
            ConfigValue maxValue = section.getValue(MAX);
            int max = maxValue == null ? Integer.MAX_VALUE : maxValue.getAsInt(0);
            if (max < min) {
                throw new KnownResourceException(
                        "number.no_less_than",
                        maxValue.path(),
                        Integer.toString(max),
                        Integer.toString(min)
                );
            }
            return new EquipmentSetCondition<>(set, min, max);
        }
    }
}
