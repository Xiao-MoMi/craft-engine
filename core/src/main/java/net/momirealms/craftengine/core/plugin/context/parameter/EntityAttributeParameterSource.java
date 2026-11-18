package net.momirealms.craftengine.core.plugin.context.parameter;

import net.momirealms.craftengine.core.attribute.Attribute;
import net.momirealms.craftengine.core.attribute.AttributeManager;
import net.momirealms.craftengine.core.attribute.vanilla.VanillaAttributeInstance;
import net.momirealms.craftengine.core.entity.LivingEntity;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.ChainParameterSource;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Optional;

final class EntityAttributeParameterSource implements ChainParameterSource {
    private final LivingEntity entity;

    EntityAttributeParameterSource(LivingEntity entity) {
        this.entity = entity;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getParameter(ContextKey<T> key) {
        String name = key.node();
        if (name.indexOf(':') > 0) {
            Key attributeId = Key.of(name);
            VanillaAttributeInstance vanillaAttribute = this.entity.getVanillaAttribute(attributeId);
            if (vanillaAttribute != null) {
                return Optional.of((T) Double.valueOf(vanillaAttribute.getValue()));
            }
            AttributeManager manager = CraftEngine.instance().attributeManager();
            return manager.getAttribute(attributeId).map(attribute -> (T) Double.valueOf(manager.getAttributeValue(this.entity, attribute)));
        } else {
            AttributeManager manager = CraftEngine.instance().attributeManager();
            List<Attribute> matches = manager.getAttributesByValue(name);
            if (matches.size() > 1) {
                return Optional.empty();
            }
            if (!matches.isEmpty()) {
                return Optional.of((T) Double.valueOf(manager.getAttributeValue(this.entity, matches.getFirst())));
            }
            VanillaAttributeInstance vanillaAttribute = this.entity.getVanillaAttribute(Key.minecraft(name));
            return vanillaAttribute == null ? Optional.empty() : Optional.of((T) Double.valueOf(vanillaAttribute.getValue()));
        }
    }
}
