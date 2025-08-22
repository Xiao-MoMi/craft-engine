package net.momirealms.craftengine.core.sound;

import org.jetbrains.annotations.NotNull;

public enum SoundSource {
    MASTER("master"),
    MUSIC("music"),
    RECORD("record"),
    WEATHER("weather"),
    BLOCK("block"),
    HOSTILE("hostile"),
    NEUTRAL("neutral"),
    PLAYER("player"),
    AMBIENT("ambient"),
    VOICE("voice"),
    UI("ui");

    private final String id;

    SoundSource(final String id) {
        this.id = id;
    }

    @NotNull
    public String id() {
        return this.id;
    }
}
