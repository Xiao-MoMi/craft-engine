package net.momirealms.craftengine.core.plugin.command;

import org.incendo.cloud.parser.flag.CommandFlag;

public final class FlagKeys {
    private FlagKeys() {}

    public static final String SILENT = "silent";
    public static final CommandFlag<Void> SILENT_FLAG = CommandFlag.builder(SILENT).withAliases("s").build();
    public static final String TO_INVENTORY = "to-inventory";
    public static final CommandFlag<Void> TO_INVENTORY_FLAG = CommandFlag.builder(TO_INVENTORY).build();
    public static final String MATCH_TAG = "match-tag";
    public static final CommandFlag<Void> MATCH_TAG_FLAG = CommandFlag.builder(MATCH_TAG).build();
    public static final String CLIENT_SIDE = "client-side";
    public static final CommandFlag<Void> CLIENT_SIDE_FLAG = CommandFlag.builder(CLIENT_SIDE).build();
}
