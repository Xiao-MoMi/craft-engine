package net.momirealms.craftengine.core.plugin.gui.category;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigManager;
import net.momirealms.craftengine.core.plugin.gui.*;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.context.ContextHolder;

import java.nio.file.Path;
import java.util.*;

import static java.util.Objects.requireNonNull;

public class ItemBrowserManagerImpl implements ItemBrowserManager {
    private static final Set<String> MOVE_TO_OTHER_INV = Set.of("MOVE_TO_OTHER_INVENTORY", "SHIFT_LEFT", "SHIFT_RIGHT");
    private final CraftEngine plugin;
    private final Map<Key, Category> byId;
    private final TreeSet<Category> ordered;

    public ItemBrowserManagerImpl(CraftEngine plugin) {
        this.plugin = plugin;
        this.byId = new HashMap<>();
        this.ordered = new TreeSet<>();
    }

    @Override
    public void unload() {
        this.byId.clear();
        this.ordered.clear();
    }

    public void delayedLoad() {
        this.ordered.addAll(this.byId.values());
        Constants.load();
    }

    @Override
    public void open(Player player) {
        openItemBrowser(player);
    }

    @Override
    public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
        String name = section.getOrDefault("name", id).toString();
        List<String> members = MiscUtils.getAsStringList(section.getOrDefault("list", List.of()));
        Key icon = Key.of(section.getOrDefault("icon", ItemKeys.STONE).toString());
        if (this.plugin.itemManager().getCustomItem(icon).isEmpty()) {
            icon = ItemKeys.STONE;
        }
        int priority = MiscUtils.getAsInt(section.getOrDefault("priority", 0));
        Category category = new Category(id, name, icon, members.stream().map(Key::of).distinct().toList(), priority);
        if (this.byId.containsKey(id)) {
            this.byId.get(id).merge(category);
        } else {
            this.byId.put(id, category);
        }
    }

    @Override
    public TreeSet<Category> categories() {
        return ordered;
    }

    @Override
    public Optional<Category> byId(Key key) {
        return Optional.ofNullable(this.byId.get(key));
    }

    public void openItemBrowser(Player player) {
        GuiLayout layout = new GuiLayout(
                "AAAAAAAAA",
                "AAAAAAAAA",
                "AAAAAAAAA",
                "AAAAAAAAA",
                "AAAAAAAAA",
                " <     > "
        )
        .addIngredient('A', Ingredient.paged())
        .addIngredient('>', GuiElement.paged((element) -> {
                    Key next = element.gui().hasNextPage() ? Constants.BROWSER_NEXT_PAGE_AVAILABLE : Constants.BROWSER_NEXT_PAGE_BLOCK;
                    return this.plugin.itemManager().getCustomItem(next)
                            .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                                    .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(element.gui().currentPage()))
                                    .withParameter(GuiParameters.MAX_PAGE, String.valueOf(element.gui().maxPages()))
                                    .build())))
                            .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + next));
                }, true)
        )
        .addIngredient('<', GuiElement.paged((element) -> {
                    Key previous = element.gui().hasPreviousPage() ? Constants.BROWSER_PREVIOUS_PAGE_AVAILABLE : Constants.BROWSER_PREVIOUS_PAGE_BLOCK;
                    return this.plugin.itemManager().getCustomItem(previous)
                            .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                                    .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(element.gui().currentPage()))
                                    .withParameter(GuiParameters.MAX_PAGE, String.valueOf(element.gui().maxPages()))
                                    .build())))
                            .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + previous));
                }, false)
        );

        List<ItemWithAction> iconList = this.ordered.stream().map(it -> {
            Item<?> item = this.plugin.itemManager().createWrappedItem(it.icon(), player);
            if (item == null) {
                this.plugin.logger().warn("Can't not find item " + it.icon() + " for category icon");
                return null;
            }
            item.displayName(AdventureHelper.miniMessageToJson(it.displayName()));
            item.lore(List.of());
            item.load();
            return new ItemWithAction(item, (element, click) -> {
                click.cancel();
                openCategoryPage(click.clicker(), it.id(), element.gui());
            });
        }).filter(Objects::nonNull).toList();

        PagedGui.builder()
                .addIngredients(iconList)
                .layout(layout)
                .inventoryClickConsumer(c -> {
                    if (MOVE_TO_OTHER_INV.contains(c.type())) {
                        c.cancel();
                    }
                })
                .build()
                .title(AdventureHelper.miniMessage().deserialize(Constants.BROWSER_TITLE, ItemBuildContext.of(player, ContextHolder.EMPTY).tagResolvers()))
                .refresh()
                .open(player);
    }

    public void openCategoryPage(Player player, Key categoryId, Gui parentGui) {
        GuiLayout layout = new GuiLayout(
                "AAAAAAAAA",
                "AAAAAAAAA",
                "AAAAAAAAA",
                "AAAAAAAAA",
                "AAAAAAAAA",
                " <  =  > "
        )
        .addIngredient('A', Ingredient.paged())
        .addIngredient('=', GuiElement.constant(this.plugin.itemManager().getCustomItem(Constants.CATEGORY_BACK)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.EMPTY)))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + Constants.CATEGORY_BACK)),
                ((element, click) -> {
                    click.cancel();
                    parentGui.open(player);
                }))
        )
        .addIngredient('>', GuiElement.paged((element) -> {
                    Key next = element.gui().hasNextPage() ? Constants.CATEGORY_NEXT_PAGE_AVAILABLE : Constants.CATEGORY_NEXT_PAGE_BLOCK;
                    return this.plugin.itemManager().getCustomItem(next)
                            .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                                    .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(element.gui().currentPage()))
                                    .withParameter(GuiParameters.MAX_PAGE, String.valueOf(element.gui().maxPages()))
                                    .build())))
                            .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + next));
                }, true)
        )
        .addIngredient('<', GuiElement.paged((element) -> {
                    Key previous = element.gui().hasPreviousPage() ? Constants.CATEGORY_PREVIOUS_PAGE_AVAILABLE : Constants.CATEGORY_PREVIOUS_PAGE_BLOCK;
                    return this.plugin.itemManager().getCustomItem(previous)
                            .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                                    .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(element.gui().currentPage()))
                                    .withParameter(GuiParameters.MAX_PAGE, String.valueOf(element.gui().maxPages()))
                                    .build())))
                            .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + previous));
                }, false)
        );

        Optional<Category> optionalCategory = byId(categoryId);
        if (optionalCategory.isEmpty()) {
            this.plugin.logger().warn("Can't find category " + categoryId);
            return;
        }

        Category category = optionalCategory.get();

        List<ItemWithAction> itemList = category.members().stream().map(it -> {
            Item<?> item = this.plugin.itemManager().createWrappedItem(it, player);
            if (item == null) return null;
            return new ItemWithAction(item, (element, click) -> {
                click.cancel();
                openRecipePage(click.clicker(), it, element.gui());
            });
        }).filter(Objects::nonNull).toList();

        PagedGui.builder()
                .addIngredients(itemList)
                .layout(layout)
                .build()
                .title(AdventureHelper.miniMessage().deserialize(Constants.CATEGORY_TITLE, ItemBuildContext.of(player, ContextHolder.EMPTY).tagResolvers()))
                .refresh()
                .open(player);
    }

    public void openRecipePage(Player player, Key result, Gui parentGui) {

    }

    static class Constants {
        public static String BROWSER_TITLE;
        public static Key BROWSER_NEXT_PAGE_AVAILABLE;
        public static Key BROWSER_NEXT_PAGE_BLOCK;
        public static Key BROWSER_PREVIOUS_PAGE_AVAILABLE;
        public static Key BROWSER_PREVIOUS_PAGE_BLOCK;

        public static String CATEGORY_TITLE;
        public static Key CATEGORY_BACK;
        public static Key CATEGORY_NEXT_PAGE_AVAILABLE;
        public static Key CATEGORY_NEXT_PAGE_BLOCK;
        public static Key CATEGORY_PREVIOUS_PAGE_AVAILABLE;
        public static Key CATEGORY_PREVIOUS_PAGE_BLOCK;

        public static void load() {
            Section section = ConfigManager.instance().settings().getSection("gui.browser");
            if (section == null) return;
            BROWSER_TITLE = getOrThrow(section, "main.title");
            BROWSER_NEXT_PAGE_AVAILABLE = Key.of(getOrThrow(section, "main.page-navigation.next.available"));
            BROWSER_NEXT_PAGE_BLOCK = Key.of(getOrThrow(section, "main.page-navigation.next.not-available"));
            BROWSER_PREVIOUS_PAGE_AVAILABLE = Key.of(getOrThrow(section, "main.page-navigation.previous.available"));
            BROWSER_PREVIOUS_PAGE_BLOCK = Key.of(getOrThrow(section, "main.page-navigation.previous.not-available"));

            CATEGORY_TITLE = getOrThrow(section, "category.title");
            CATEGORY_BACK = Key.of(getOrThrow(section, "category.page-navigation.return"));
            CATEGORY_NEXT_PAGE_AVAILABLE = Key.of(getOrThrow(section, "category.page-navigation.next.available"));
            CATEGORY_NEXT_PAGE_BLOCK = Key.of(getOrThrow(section, "category.page-navigation.next.not-available"));
            CATEGORY_PREVIOUS_PAGE_AVAILABLE = Key.of(getOrThrow(section, "category.page-navigation.previous.available"));
            CATEGORY_PREVIOUS_PAGE_BLOCK = Key.of(getOrThrow(section, "category.page-navigation.previous.not-available"));
        }

        private static String getOrThrow(Section section, String route) {
            return requireNonNull(section.getString(route), "gui.browser." + route);
        }
    }
}
