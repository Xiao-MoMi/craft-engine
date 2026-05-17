package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.value.JukeboxPlayable;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;

public final class JukeboxSongProcessor implements ItemProcessor {
    public static final ItemProcessorFactory<JukeboxSongProcessor> FACTORY = new Factory();
    private final JukeboxPlayable song;

    public JukeboxSongProcessor(JukeboxPlayable song) {
        this.song = song;
    }

    public JukeboxPlayable song() {
        return this.song;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        item.jukeboxSong(this.song);
        return item;
    }

    private static class Factory implements ItemProcessorFactory<JukeboxSongProcessor> {

        @Override
        public JukeboxSongProcessor create(ConfigValue value) {
            return new JukeboxSongProcessor(new JukeboxPlayable(value.getAsString(), true));
        }
    }
}
