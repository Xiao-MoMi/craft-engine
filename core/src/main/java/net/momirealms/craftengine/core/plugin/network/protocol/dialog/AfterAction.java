package net.momirealms.craftengine.core.plugin.network.protocol.dialog;

import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;

public enum AfterAction {
    CLOSE("close"),
    NONE("none"),
    WAIT_FOR_RESPONSE("wait_for_response");

    private final String id;

    AfterAction(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public static AfterAction read(Tag tag) {
        if (tag instanceof StringTag stringTag) {
            return switch (stringTag.getAsString()) {
                case "close" -> CLOSE;
                case "none" -> NONE;
                case "wait_for_response" -> WAIT_FOR_RESPONSE;
                case null, default -> CLOSE;
            };
        }
        return CLOSE;
    }
}
