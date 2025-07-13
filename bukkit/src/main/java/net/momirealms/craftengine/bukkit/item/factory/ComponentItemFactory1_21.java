package net.momirealms.craftengine.bukkit.item.factory;

import net.momirealms.craftengine.bukkit.item.ComponentItemWrapper;
import net.momirealms.craftengine.bukkit.item.ComponentTypes;
import net.momirealms.craftengine.core.item.data.JukeboxPlayable;
import net.momirealms.craftengine.core.plugin.CraftEngine;

import java.util.Map;
import java.util.Optional;

public class ComponentItemFactory1_21 extends ComponentItemFactory1_20_5 {

    public ComponentItemFactory1_21(CraftEngine plugin) {
        super(plugin);
    }

    @Override
    protected Optional<JukeboxPlayable> jukeboxSong(ComponentItemWrapper item) {
        Optional<Map<String, Object>> map = item.getJavaComponent(ComponentTypes.JUKEBOX_PLAYABLE);
        return map.map(song -> new JukeboxPlayable(
                (String) song.get("song"),
                (boolean) song.getOrDefault("show_in_tooltip", true))
        );
    }

    @Override
    protected void jukeboxSong(ComponentItemWrapper item, JukeboxPlayable data) {
        item.setJavaComponent(ComponentTypes.JUKEBOX_PLAYABLE, Map.of(
                "song", data.song(),
                "show_in_tooltip", true
        ));
    }
}
