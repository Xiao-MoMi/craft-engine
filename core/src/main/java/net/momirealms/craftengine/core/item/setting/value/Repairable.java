package net.momirealms.craftengine.core.item.setting.value;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Tristate;

public record Repairable(Tristate craftingTable, Tristate anvilRepair, Tristate anvilCombine) {
    public static final Repairable UNDEFINED = new Repairable(Tristate.UNDEFINED, Tristate.UNDEFINED, Tristate.UNDEFINED);
    public static final Repairable TRUE = new Repairable(Tristate.TRUE, Tristate.TRUE, Tristate.TRUE);
    public static final Repairable FALSE = new Repairable(Tristate.FALSE, Tristate.FALSE, Tristate.FALSE);

    private static final String[] CRAFTING_TABLE = new String[] {"crafting_table", "crafting-table"};
    private static final String[] ANVIL_REPAIR = new String[] {"anvil_repair", "anvil-repair"};
    private static final String[] ANVIL_COMBINE = new String[] {"anvil_combine", "anvil-combine"};

    public static Repairable fromConfig(ConfigSection section) {
        Tristate craftingTable = section.getValue(CRAFTING_TABLE, it -> Tristate.of(it.getAsBoolean()), Tristate.UNDEFINED);
        Tristate anvilRepair = section.getValue(ANVIL_REPAIR, it -> Tristate.of(it.getAsBoolean()), Tristate.UNDEFINED);
        Tristate anvilCombine = section.getValue(ANVIL_COMBINE, it -> Tristate.of(it.getAsBoolean()), Tristate.UNDEFINED);
        return new Repairable(craftingTable, anvilRepair, anvilCombine);
    }
}
