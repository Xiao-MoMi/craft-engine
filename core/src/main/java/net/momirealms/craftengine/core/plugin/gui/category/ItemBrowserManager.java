package net.momirealms.craftengine.core.plugin.gui.category;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.recipe.Recipe;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.gui.Gui;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import static java.util.Objects.requireNonNull;

public interface ItemBrowserManager extends Manageable {
    int MAX_RECIPE_DEPTH = 16;
    String GET_ITEM_PERMISSION = "craftengine.browser.get_item";

    ConfigParser parser();

    void addExternalCategoryMember(Key item, List<Key> category);

    void open(Player player);

    void openRecipePage(Player player, Gui parentGui, List<Recipe<Object>> recipes, int index, int depth, boolean canOpenNoRecipePage);

    void openNoRecipePage(Player player, Key result, Gui parentGui, int depth);

    TreeSet<Category> categories();

    Optional<Category> byId(Key key);

    class Constants {
        public static String BROWSER_TITLE;
        public static Key BROWSER_NEXT_PAGE_AVAILABLE;
        public static Key BROWSER_NEXT_PAGE_BLOCK;
        public static Key BROWSER_PREVIOUS_PAGE_AVAILABLE;
        public static Key BROWSER_PREVIOUS_PAGE_BLOCK;

        public static String CATEGORY_TITLE;
        public static Key CATEGORY_BACK;
        public static Key CATEGORY_EXIT;
        public static Key CATEGORY_NEXT_PAGE_AVAILABLE;
        public static Key CATEGORY_NEXT_PAGE_BLOCK;
        public static Key CATEGORY_PREVIOUS_PAGE_AVAILABLE;
        public static Key CATEGORY_PREVIOUS_PAGE_BLOCK;

        public static String RECIPE_NONE_TITLE;
        public static String RECIPE_BLASTING_TITLE;
        public static String RECIPE_SMELTING_TITLE;
        public static String RECIPE_SMOKING_TITLE;
        public static String RECIPE_CAMPFIRE_TITLE;
        public static String RECIPE_CRAFTING_TITLE;
        public static String RECIPE_STONECUTTING_TITLE;
        public static String RECIPE_SMITHING_TRANSFORM_TITLE;
        public static Key RECIPE_BACK;
        public static Key RECIPE_EXIT;
        public static Key RECIPE_NEXT_PAGE_AVAILABLE;
        public static Key RECIPE_NEXT_PAGE_BLOCK;
        public static Key RECIPE_PREVIOUS_PAGE_AVAILABLE;
        public static Key RECIPE_PREVIOUS_PAGE_BLOCK;
        public static Key RECIPE_GET_ITEM;
        public static Key RECIPE_COOKING_INFO;

        public static Key SOUND_CHANGE_PAGE;
        public static Key SOUND_RETURN_PAGE;
        public static Key SOUND_PICK_ITEM;
        public static Key SOUND_CLICK_BUTTON;

        public static void load() {
            Section section = Config.instance().settings().getSection("gui.browser");
            if (section == null) return;
            BROWSER_TITLE = getOrThrow(section, "main.title");
            BROWSER_NEXT_PAGE_AVAILABLE = Key.of(getOrThrow(section, "main.page-navigation.next.available"));
            BROWSER_NEXT_PAGE_BLOCK = Key.of(getOrThrow(section, "main.page-navigation.next.not-available"));
            BROWSER_PREVIOUS_PAGE_AVAILABLE = Key.of(getOrThrow(section, "main.page-navigation.previous.available"));
            BROWSER_PREVIOUS_PAGE_BLOCK = Key.of(getOrThrow(section, "main.page-navigation.previous.not-available"));

            CATEGORY_TITLE = getOrThrow(section, "category.title");
            CATEGORY_BACK = Key.of(getOrThrow(section, "category.page-navigation.return"));
            CATEGORY_EXIT = Key.of(getOrThrow(section, "category.page-navigation.exit"));
            CATEGORY_NEXT_PAGE_AVAILABLE = Key.of(getOrThrow(section, "category.page-navigation.next.available"));
            CATEGORY_NEXT_PAGE_BLOCK = Key.of(getOrThrow(section, "category.page-navigation.next.not-available"));
            CATEGORY_PREVIOUS_PAGE_AVAILABLE = Key.of(getOrThrow(section, "category.page-navigation.previous.available"));
            CATEGORY_PREVIOUS_PAGE_BLOCK = Key.of(getOrThrow(section, "category.page-navigation.previous.not-available"));

            RECIPE_NONE_TITLE = getOrThrow(section, "recipe.none.title");
            RECIPE_BLASTING_TITLE = getOrThrow(section, "recipe.blasting.title");
            RECIPE_SMELTING_TITLE = getOrThrow(section, "recipe.smelting.title");
            RECIPE_SMOKING_TITLE = getOrThrow(section, "recipe.smoking.title");
            RECIPE_CAMPFIRE_TITLE = getOrThrow(section, "recipe.campfire-cooking.title");
            RECIPE_CRAFTING_TITLE = getOrThrow(section, "recipe.crafting.title");
            RECIPE_STONECUTTING_TITLE = getOrThrow(section, "recipe.stonecutting.title");
            RECIPE_SMITHING_TRANSFORM_TITLE = getOrThrow(section, "recipe.smithing-transform.title");
            RECIPE_BACK = Key.of(getOrThrow(section, "recipe.page-navigation.return"));
            RECIPE_EXIT = Key.of(getOrThrow(section, "recipe.page-navigation.exit"));
            RECIPE_NEXT_PAGE_AVAILABLE = Key.of(getOrThrow(section, "recipe.page-navigation.next.available"));
            RECIPE_NEXT_PAGE_BLOCK = Key.of(getOrThrow(section, "recipe.page-navigation.next.not-available"));
            RECIPE_PREVIOUS_PAGE_AVAILABLE = Key.of(getOrThrow(section, "recipe.page-navigation.previous.available"));
            RECIPE_PREVIOUS_PAGE_BLOCK = Key.of(getOrThrow(section, "recipe.page-navigation.previous.not-available"));
            RECIPE_GET_ITEM = Key.of(getOrThrow(section, "recipe.get-item-icon"));
            RECIPE_COOKING_INFO = Key.of(getOrThrow(section, "recipe.cooking-information-icon"));

            SOUND_CHANGE_PAGE = Key.of(getOrThrow(section, "sounds.change-page"));
            SOUND_RETURN_PAGE = Key.of(getOrThrow(section, "sounds.return-page"));
            SOUND_PICK_ITEM = Key.of(getOrThrow(section, "sounds.pick-item"));
            SOUND_CLICK_BUTTON = Key.of(getOrThrow(section, "sounds.click-button"));
        }

        private static String getOrThrow(Section section, String route) {
            return requireNonNull(section.getString(route), "gui.browser." + route);
        }
    }
}
