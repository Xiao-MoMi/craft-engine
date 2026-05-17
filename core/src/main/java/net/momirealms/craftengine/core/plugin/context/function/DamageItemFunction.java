package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class DamageItemFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final NumberProvider amount;
    private final EquipmentSlot slot;

    private DamageItemFunction(List<Condition<CTX>> predicates, NumberProvider amount, @Nullable EquipmentSlot slot) {
        super(predicates);
        this.amount = amount;
        this.slot = slot;
    }

    @Override
    protected void runInternal(CTX ctx) {
        Player player = ctx.getOptionalParameter(DirectContextParameters.PLAYER).orElse(null);
        if (player == null) return;
        Item item = ctx.getOptionalParameter(DirectContextParameters.ITEM_IN_HAND).orElse(null);
        InteractionHand hand = ctx.getOptionalParameter(DirectContextParameters.HAND).orElse(null);
        if (item == null && hand != null) {
            item = player.getItemInHand(hand);
        } else if (item == null) {
            return;
        }
        EquipmentSlot slot;
        if (this.slot != null) {
            slot = this.slot;
        } else {
            slot = hand == null ? null : hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        }
        item.hurtAndBreak(amount.getInt(ctx), player, slot);
    }

    public static <CTX extends Context> FunctionFactory<CTX, DamageItemFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, DamageItemFunction<CTX>> {
        private static final String[] AMOUNT = new String[] {"amount", "damage"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public DamageItemFunction<CTX> create(ConfigSection section) {
            return new DamageItemFunction<>(
                    getPredicates(section),
                    section.getNumber(AMOUNT, ConfigConstants.CONSTANT_ONE),
                    section.getEnum("slot", EquipmentSlot.class)
            );
        }
    }
}