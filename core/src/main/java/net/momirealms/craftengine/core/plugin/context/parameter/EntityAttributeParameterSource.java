package net.momirealms.craftengine.core.plugin.context.parameter;

import net.momirealms.craftengine.core.attribute.Attribute;
import net.momirealms.craftengine.core.attribute.AttributeManager;
import net.momirealms.craftengine.core.attribute.vanilla.VanillaAttributeInstance;
import net.momirealms.craftengine.core.entity.LivingEntity;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.ChainParameterSource;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

final class EntityAttributeParameterSource implements ChainParameterSource {
    private final LivingEntity entity;

    EntityAttributeParameterSource(LivingEntity entity) {
        this.entity = entity;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getParameter(ContextKey<T> key) {
        Key attributeId = Key.of(key.node());
        VanillaAttributeInstance vanillaAttribute = this.entity.getVanillaAttribute(attributeId);
        if (vanillaAttribute != null) {
            return Optional.of((T) Double.valueOf(vanillaAttribute.getValue()));
        }
        AttributeManager manager = CraftEngine.instance().attributeManager();
        Optional<Attribute> customAttribute = manager.getAttribute(attributeId);
        return customAttribute.map(attribute -> (T) Double.valueOf(manager.getAttributeValue(this.entity, attribute)));
    }
}
