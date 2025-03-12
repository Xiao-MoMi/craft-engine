package net.momirealms.craftengine.core.plugin.command;

import org.incendo.cloud.parser.flag.CommandFlag;

public final class FlagKeys {
    public static final String SILENT = "silent";
    public static final CommandFlag<Void> SILENT_FLAG = CommandFlag.builder("silent").withAliases("s").build();
    public static final String TO_INVENTORY = "to-inventory";
    public static final CommandFlag<Void> TO_INVENTORY_FLAG = CommandFlag.builder("to-inventory").build();
}
