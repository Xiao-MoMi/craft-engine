package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.Map;

public class JukeboxSongModifier<I> implements ItemModifier<I> {
    private final Key song;

    public JukeboxSongModifier(Key song) {
        this.song = song;
    }

    @Override
    public String name() {
        return "jukebox-playable";
    }

    @Override
    public void apply(Item<I> item, ItemBuildContext context) {
        if (VersionHelper.isVersionNewerThan1_21_5()) {
            item.setComponent(ComponentKeys.JUKEBOX_PLAYABLE, song.toString());
        } else {
            item.setComponent(ComponentKeys.JUKEBOX_PLAYABLE, Map.of(
                    "song", song.toString(),
                    "show_in_tooltip", true
            ));
        }
    }
}
