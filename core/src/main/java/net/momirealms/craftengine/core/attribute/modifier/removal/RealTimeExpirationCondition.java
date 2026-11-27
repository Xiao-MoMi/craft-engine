package net.momirealms.craftengine.core.attribute.modifier.removal;

public record RealTimeExpirationCondition(long expiresAtEpochMillis) implements RemovalCondition {

    @Override
    public boolean test(RemovalContext context) {
        return context.currentTimeMillis() >= this.expiresAtEpochMillis;
    }

    @Override
    public long nextCheckTick(RemovalContext context) {
        if (this.test(context)) {
            return context.currentTick();
        }
        long remainingMillis = this.expiresAtEpochMillis - context.currentTimeMillis();
        long delayTicks = Math.clamp(Math.ceilDiv(remainingMillis, 50L), 1L, 200L);
        return context.currentTick() + delayTicks;
    }
}
