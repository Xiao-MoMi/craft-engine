package net.momirealms.craftengine.core.attribute.modifier.removal;

public record TickExpirationCondition(long remainingTicks) implements RemovalCondition {

    public TickExpirationCondition {
        if (remainingTicks < 0) {
            throw new IllegalArgumentException("Remaining ticks must not be negative");
        }
    }

    @Override
    public boolean test(RemovalContext context) {
        return context.currentTick() >= Math.addExact(context.boundTick(), this.remainingTicks);
    }

    @Override
    public long nextCheckTick(RemovalContext context) {
        return Math.max(context.currentTick(), Math.addExact(context.boundTick(), this.remainingTicks));
    }
}
