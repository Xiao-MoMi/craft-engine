package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.entity.LivingEntity;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class EquipmentCondition<CTX extends Context> implements Condition<CTX> {
    private final EquipmentSlot slot;
    private final Set<String> ids;
    private final boolean regexMatch;
    private final Set<Key> tags;

    private EquipmentCondition(EquipmentSlot slot, Set<String> ids, boolean regexMatch, Set<Key> tags) {
        this.slot = slot;
        this.ids = ids;
        this.regexMatch = regexMatch;
        this.tags = tags;
    }

    @Override
    public boolean test(CTX ctx) {
        Optional<LivingEntity> optionalEntity = DirectContextParameters.getOptionalLivingEntity(ctx);
        if (optionalEntity.isEmpty()) {
            return false;
        }
        LivingEntity entity = optionalEntity.get();
        Item item = entity.getItemByEquipmentSlot(this.slot);
        if (!this.ids.isEmpty() && MiscUtils.matchRegex(item.id().asString(), this.ids, this.regexMatch)) {
            return true;
        }
        if (!this.tags.isEmpty()) {
            for (Key tag : this.tags) {
                if (item.hasPluginTag(tag)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static <CTX extends Context> ConditionFactory<CTX, EquipmentCondition<CTX>> factory() {
        return new Factory<>();
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, EquipmentCondition<CTX>> {
        private static final String[] ID = ConfigKeys.of("id|item(s)");
        private static final String[] TAG = ConfigKeys.of("tag(s)");

        @Override
        public EquipmentCondition<CTX> create(ConfigSection section) {
            EquipmentSlot slot = EquipmentSlot.byId(section.getNonEmptyString("slot"));
            Set<String> ids = new HashSet<>(section.getStringList(ID));
            boolean regexMatch = section.getBoolean("regex");
            Set<Key> tags = new HashSet<>(section.getList(TAG, ConfigValue::getAsIdentifier));
            return new EquipmentCondition<>(slot, ids, regexMatch, tags);
        }
    }
}
