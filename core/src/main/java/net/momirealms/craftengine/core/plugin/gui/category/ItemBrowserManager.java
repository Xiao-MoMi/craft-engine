package net.momirealms.craftengine.core.plugin.gui.category;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import net.momirealms.craftengine.core.util.Key;

import java.util.Optional;
import java.util.TreeSet;

public interface ItemBrowserManager extends Reloadable, ConfigSectionParser {
    String CONFIG_SECTION_NAME = "categories";
    int MAX_RECIPE_DEPTH = 16;
    Key BUTTON_SOUND = Key.of("minecraft:ui.hud.bubble_pop");
    Key PAGE_SOUND = Key.of("minecraft:ui.loom.take_result");
    Key RETURN_SOUND = Key.of("minecraft:ui.button.click");
    Key PICKUP_SOUND = Key.of("minecraft:entity.item.pickup");
    String GET_ITEM_PERMISSION = "craftengine.browser.get_item";

    default String sectionId() {
        return CONFIG_SECTION_NAME;
    }

    default int loadingSequence() {
        return LoadingSequence.CATEGORY;
    }

    void delayedLoad();

    void open(Player player);

    TreeSet<Category> categories();

    Optional<Category> byId(Key key);
}
