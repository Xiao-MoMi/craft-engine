package net.momirealms.craftengine.core.sound.song;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.Key;

public record JukeboxSong(Key sound, Component description, float lengthInSeconds, int comparatorOutput, float range) {
}
