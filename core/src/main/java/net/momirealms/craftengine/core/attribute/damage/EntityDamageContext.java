package net.momirealms.craftengine.core.attribute.damage;

import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.AbstractChainParameterContext;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import org.jetbrains.annotations.NotNull;

public class EntityDamageContext extends AbstractChainParameterContext {
    private final DamageEvent event;

    public EntityDamageContext(DamageEvent event, @NotNull ContextHolder contexts) {
        super(contexts);
        this.event = event;
    }

    @NotNull
    public static EntityDamageContext of(DamageEvent event, @NotNull ContextHolder.Builder contexts) {
        Entity entity = event.source().causingEntity();
        if (entity != null) {
            contexts.withParameter(DirectContextParameters.CAUSING_ENTITY, entity);
            if (entity instanceof Player player) {
                contexts.withParameter(DirectContextParameters.PLAYER, player);
            }
        }
        Entity directEntity = event.source().directEntity();
        if (directEntity != null) {
            contexts.withParameter(DirectContextParameters.DIRECT_ENTITY, directEntity);
        }
        Entity victim = event.victim();
        contexts.withParameter(DirectContextParameters.ENTITY, victim);
        contexts.withParameter(DirectContextParameters.POSITION, victim.position());
        double baseDamage = event.damage();
        contexts.withParameter(DirectContextParameters.ORIGINAL_DAMAGE, baseDamage);
        contexts.withParameter(DirectContextParameters.DAMAGE, baseDamage);
        contexts.withParameter(DirectContextParameters.IS_CRITICAL, event.source().isCritical());
        contexts.withParameter(DirectContextParameters.IS_SWEEP, event.isSweepAttack());
        contexts.withParameter(DirectContextParameters.IS_ATTACK_READY, event.isAttackReady());
        contexts.withParameter(DirectContextParameters.ATTACK_STRENGTH, event.attackStrength());
        contexts.withParameter(DirectContextParameters.SHOOT_FORCE, event.shootForce());
        return new EntityDamageContext(event, contexts.build());
    }

    public DamageEvent event() {
        return this.event;
    }
}
