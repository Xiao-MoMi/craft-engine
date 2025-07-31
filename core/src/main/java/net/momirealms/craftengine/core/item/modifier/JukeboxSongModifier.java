package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.item.data.JukeboxPlayable;
import net.momirealms.craftengine.core.util.Key;

public class JukeboxSongModifier<I> implements ItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final JukeboxPlayable song;

    public JukeboxSongModifier(JukeboxPlayable song) {
        this.song = song;
    }

    public JukeboxPlayable song() {
        return song;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.JUKEBOX_PLAYABLE;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        item.jukeboxSong(this.song);
        return item;
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {

        @Override
        public ItemDataModifier<I> create(Object arg) {
            String song = arg.toString();
            return new JukeboxSongModifier<>(new JukeboxPlayable(song, true));
        }
    }
}
