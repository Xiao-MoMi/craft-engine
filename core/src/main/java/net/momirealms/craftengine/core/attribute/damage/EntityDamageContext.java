package net.momirealms.craftengine.core.attribute.damage;

import net.momirealms.craftengine.core.plugin.context.AbstractChainParameterContext;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import org.jetbrains.annotations.NotNull;

public class EntityDamageContext extends AbstractChainParameterContext {
    private final DamageEvent event;

    public EntityDamageContext(DamageEvent event, @NotNull ContextHolder contexts) {
        super(contexts);
        this.event = event;
    }

    @NotNull
    public static EntityDamageContext of(DamageEvent event, @NotNull ContextHolder contexts) {
        return new EntityDamageContext(event, contexts);
    }

    public DamageEvent event() {
        return this.event;
    }
}
