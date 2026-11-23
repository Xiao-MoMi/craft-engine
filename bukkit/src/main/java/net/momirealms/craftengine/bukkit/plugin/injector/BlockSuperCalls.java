package net.momirealms.craftengine.bukkit.plugin.injector;

public interface BlockSuperCalls {

    Object superGetShape(Object state, Object level, Object pos, Object context);

    Object superGetCollisionShape(Object state, Object level, Object pos, Object context);

    Object superGetSupportShape(Object state, Object level, Object pos);

    Object superPlayerWillDestroy(Object level, Object pos, Object state, Object player);

    interface Explosion {
        void superOnExplosionHit(Object state, Object level, Object pos, Object explosion, Object drops);
    }
}
