package net.momirealms.craftengine.core.plugin.gui.category;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.recipe.*;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.gui.Ingredient;
import net.momirealms.craftengine.core.plugin.gui.*;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.context.ContextHolder;

import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("DuplicatedCode")
public class ItemBrowserManagerImpl implements ItemBrowserManager {
    private static final Set<String> MOVE_TO_OTHER_INV = Set.of("SHIFT_LEFT", "SHIFT_RIGHT");
    private static final Set<String> LEFT_CLICK = Set.of("LEFT", "SHIFT_LEFT");
    private static final Set<String> RIGHT_CLICK = Set.of("RIGHT", "SHIFT_RIGHT");
    private static final Set<String> MIDDLE_CLICK = Set.of("MIDDLE");
    private static final Set<String> DOUBLE_CLICK = Set.of("DOUBLE_CLICK");
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
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                openCategoryPage(click.clicker(), it.id(), element.gui());
            });
        }).filter(Objects::nonNull).toList();

        PagedGui.builder()
                .addIngredients(iconList)
                .layout(layout)
                .inventoryClickConsumer(c -> {
                    if (MOVE_TO_OTHER_INV.contains(c.type()) || DOUBLE_CLICK.contains(c.type())) {
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
                    player.playSound(Constants.SOUND_RETURN_PAGE, 0.25f, 1);
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
            return new ItemWithAction(item, (e, c) -> {
                c.cancel();
                if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                    Item<?> newItem = this.plugin.itemManager().createWrappedItem(e.item().id(), player);
                    newItem.count(newItem.maxStackSize());
                    c.setItemOnCursor(newItem);
                    return;
                }
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().getRecipeByResult(it);
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), it, e.gui(), inRecipes, 0, 0);
                } else {
                    openNoRecipePage(player, it, e.gui());
                }
            });
        }).filter(Objects::nonNull).toList();

        PagedGui.builder()
                .addIngredients(itemList)
                .layout(layout)
                .inventoryClickConsumer(c -> {
                    if (MOVE_TO_OTHER_INV.contains(c.type()) || DOUBLE_CLICK.contains(c.type())) {
                        c.cancel();
                    }
                })
                .build()
                .title(AdventureHelper.miniMessage().deserialize(Constants.CATEGORY_TITLE, ItemBuildContext.of(player, ContextHolder.EMPTY).tagResolvers()))
                .refresh()
                .open(player);
    }

    public void openNoRecipePage(Player player, Key result, Gui parentGui) {
        GuiLayout layout = new GuiLayout(
                "         ",
                "         ",
                "    X    ",
                "    ^    ",
                "         ",
                "    =    "
        )
        .addIngredient('X', GuiElement.constant(this.plugin.itemManager().createWrappedItem(result, player), (e, c) -> {
            c.cancel();
            if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(result, player);
                item.count(item.maxStackSize());
                c.setItemOnCursor(item);
            }
        }))
        .addIngredient('^', player.hasPermission(GET_ITEM_PERMISSION) ? GuiElement.constant(this.plugin.itemManager().createWrappedItem(Constants.RECIPE_GET_ITEM, player), (e, c) -> {
            c.cancel();
            player.playSound(Constants.SOUND_PICK_ITEM);
            if (LEFT_CLICK.contains(c.type())) {
                player.giveItem(this.plugin.itemManager().createWrappedItem(result, player));
            } else if (RIGHT_CLICK.contains(c.type())) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(result, player);
                player.giveItem(item.count(item.maxStackSize()));
            }
        }) : GuiElement.EMPTY)
        .addIngredient('=', GuiElement.constant(this.plugin.itemManager().getCustomItem(Constants.RECIPE_BACK)
                        .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.EMPTY)))
                        .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + Constants.RECIPE_BACK)),
                ((element, click) -> {
                    click.cancel();
                    player.playSound(Constants.SOUND_RETURN_PAGE, 0.25f, 1);
                    parentGui.open(player);
                }))
        );

        BasicGui.builder()
                .layout(layout)
                .inventoryClickConsumer(c -> {
                    if (MOVE_TO_OTHER_INV.contains(c.type()) || DOUBLE_CLICK.contains(c.type())) {
                        c.cancel();
                    }
                })
                .build()
                .title(AdventureHelper.miniMessage().deserialize(Constants.RECIPE_NONE_TITLE, ItemBuildContext.of(player, ContextHolder.EMPTY).tagResolvers()))
                .refresh()
                .open(player);
    }

    public void openRecipePage(Player player, Key result, Gui parentGui, List<Recipe<Object>> recipes, int index, int depth) {
        if (index >= recipes.size()) return;
        if (depth > MAX_RECIPE_DEPTH) return;
        Recipe<Object> recipe = recipes.get(index);
        Key recipeType = recipe.type();
        if (recipeType == RecipeTypes.SHAPELESS || recipeType == RecipeTypes.SHAPED) {
            openCraftingRecipePage(player, result, (CraftingTableRecipe<Object>) recipe, parentGui, recipes, index, depth);
            return;
        }
        if (recipeType == RecipeTypes.BLASTING || recipeType == RecipeTypes.CAMPFIRE_COOKING || recipeType == RecipeTypes.SMOKING || recipeType == RecipeTypes.SMELTING) {
            openCookingRecipePage(player, result, (CookingRecipe<Object>) recipe, parentGui, recipes, index, depth);
            return;
        }
        if (recipeType == RecipeTypes.STONE_CUTTING) {
            openStoneCuttingRecipePage(player, result, (CustomStoneCuttingRecipe<Object>) recipe, parentGui, recipes, index, depth);
            return;
        }
    }

    public void openStoneCuttingRecipePage(Player player, Key result, CustomStoneCuttingRecipe<Object> recipe, Gui parentGui, List<Recipe<Object>> recipes, int index, int depth) {
        Key previous = index > 0 ? Constants.RECIPE_PREVIOUS_PAGE_AVAILABLE : Constants.RECIPE_PREVIOUS_PAGE_BLOCK;
        Key next = index + 1 < recipes.size() ? Constants.RECIPE_NEXT_PAGE_AVAILABLE : Constants.RECIPE_NEXT_PAGE_BLOCK;

        List<Item<?>> ingredients = new ArrayList<>();
        net.momirealms.craftengine.core.item.recipe.Ingredient<Object> ingredient = recipe.ingredient();
        for (Holder<Key> in : ingredient.items()) {
            ingredients.add(this.plugin.itemManager().createWrappedItem(in.value(), player));
        }
        GuiLayout layout = new GuiLayout(
                "         ",
                "         ",
                "  A   X  ",
                "      ^  ",
                "         ",
                " <  =  > "
        )
        .addIngredient('X', GuiElement.constant(this.plugin.itemManager().createWrappedItem(result, player).count(recipe.result().count()), (e, c) -> {
            c.cancel();
            if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(result, player);
                item.count(item.maxStackSize());
                c.setItemOnCursor(item);
            }
        }))
        .addIngredient('^', player.hasPermission(GET_ITEM_PERMISSION) ? GuiElement.constant(this.plugin.itemManager().createWrappedItem(Constants.RECIPE_GET_ITEM, player), (e, c) -> {
            c.cancel();
            player.playSound(Constants.SOUND_PICK_ITEM);
            if (LEFT_CLICK.contains(c.type())) {
                player.giveItem(this.plugin.itemManager().createWrappedItem(result, player));
            } else if (RIGHT_CLICK.contains(c.type())) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(result, player);
                player.giveItem(item.count(item.maxStackSize()));
            }
        }) : GuiElement.EMPTY)
        .addIngredient('A', GuiElement.recipeIngredient(ingredients, (e, c) -> {
            c.cancel();
            if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(e.item().id(), player);
                item.count(item.maxStackSize());
                c.setItemOnCursor(item);
                return;
            }
            List<Recipe<Object>> inRecipes = plugin.recipeManager().getRecipeByResult(e.item().id());
            if (!inRecipes.isEmpty()) {
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                openRecipePage(player, e.item().id(), e.gui(), inRecipes, 0, depth + 1);
            }
        }))
        .addIngredient('=', GuiElement.constant(this.plugin.itemManager().getCustomItem(Constants.RECIPE_BACK)
                        .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.EMPTY)))
                        .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + Constants.RECIPE_BACK)),
                ((element, click) -> {
                    click.cancel();
                    player.playSound(Constants.SOUND_RETURN_PAGE, 0.25f, 1);
                    parentGui.open(player);
                }))
        )
        .addIngredient('>', GuiElement.constant(this.plugin.itemManager()
                .getCustomItem(next)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(index + 1))
                        .withParameter(GuiParameters.MAX_PAGE, String.valueOf(recipes.size()))
                        .build())))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + next)), (e, c) -> {
            c.cancel();
            if (index + 1 < recipes.size()) {
                player.playSound(Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                openRecipePage(player, result, parentGui, recipes, index + 1, depth);
            }
        }))
        .addIngredient('<', GuiElement.constant(this.plugin.itemManager()
                .getCustomItem(previous)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(index + 1))
                        .withParameter(GuiParameters.MAX_PAGE, String.valueOf(recipes.size()))
                        .build())))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + previous)), (e, c) -> {
            c.cancel();
            if (index > 0) {
                player.playSound(Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                openRecipePage(player, result, parentGui, recipes, index - 1, depth);
            }
        }));

        BasicGui.builder()
                .layout(layout)
                .inventoryClickConsumer(c -> {
                    if (MOVE_TO_OTHER_INV.contains(c.type()) || DOUBLE_CLICK.contains(c.type())) {
                        c.cancel();
                    }
                })
                .build()
                .title(AdventureHelper.miniMessage().deserialize(Constants.RECIPE_STONECUTTING_TITLE, ItemBuildContext.of(player, ContextHolder.EMPTY).tagResolvers()))
                .refresh()
                .open(player);
    }

    public void openCookingRecipePage(Player player, Key result, CookingRecipe<Object> recipe, Gui parentGui, List<Recipe<Object>> recipes, int index, int depth) {
        Key previous = index > 0 ? Constants.RECIPE_PREVIOUS_PAGE_AVAILABLE : Constants.RECIPE_PREVIOUS_PAGE_BLOCK;
        Key next = index + 1 < recipes.size() ? Constants.RECIPE_NEXT_PAGE_AVAILABLE : Constants.RECIPE_NEXT_PAGE_BLOCK;

        List<Item<?>> ingredients = new ArrayList<>();
        net.momirealms.craftengine.core.item.recipe.Ingredient<Object> ingredient = recipe.ingredient();
        for (Holder<Key> in : ingredient.items()) {
            ingredients.add(this.plugin.itemManager().createWrappedItem(in.value(), player));
        }
        GuiLayout layout = new GuiLayout(
                "         ",
                "         ",
                "  A   X  ",
                "  ?   ^  ",
                "         ",
                " <  =  > "
        )
        .addIngredient('X', GuiElement.constant(this.plugin.itemManager().createWrappedItem(result, player).count(recipe.result().count()), (e, c) -> {
            c.cancel();
            if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(result, player);
                item.count(item.maxStackSize());
                c.setItemOnCursor(item);
            }
        }))
        .addIngredient('?', GuiElement.constant(this.plugin.itemManager().getCustomItem(Constants.RECIPE_COOKING_INFO)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.COOKING_TIME, String.valueOf(recipe.cookingTime()))
                        .withParameter(GuiParameters.COOKING_EXPERIENCE, String.valueOf(recipe.experience()))
                        .build())))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + Constants.RECIPE_COOKING_INFO)), (e, c) -> c.cancel()))
        .addIngredient('^', player.hasPermission(GET_ITEM_PERMISSION) ? GuiElement.constant(this.plugin.itemManager().createWrappedItem(Constants.RECIPE_GET_ITEM, player), (e, c) -> {
            c.cancel();
            player.playSound(Constants.SOUND_PICK_ITEM);
            if (LEFT_CLICK.contains(c.type())) {
                player.giveItem(this.plugin.itemManager().createWrappedItem(result, player));
            } else if (RIGHT_CLICK.contains(c.type())) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(result, player);
                player.giveItem(item.count(item.maxStackSize()));
            }
        }) : GuiElement.EMPTY)
        .addIngredient('A', GuiElement.recipeIngredient(ingredients, (e, c) -> {
            c.cancel();
            if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(e.item().id(), player);
                item.count(item.maxStackSize());
                c.setItemOnCursor(item);
                return;
            }
            List<Recipe<Object>> inRecipes = plugin.recipeManager().getRecipeByResult(e.item().id());
            if (!inRecipes.isEmpty()) {
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                openRecipePage(player, e.item().id(), e.gui(), inRecipes, 0, depth + 1);
            }
        }))
        .addIngredient('=', GuiElement.constant(this.plugin.itemManager().getCustomItem(Constants.RECIPE_BACK)
                        .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.EMPTY)))
                        .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + Constants.RECIPE_BACK)),
                ((element, click) -> {
                    click.cancel();
                    player.playSound(Constants.SOUND_RETURN_PAGE, 0.25f, 1);
                    parentGui.open(player);
                }))
        )
        .addIngredient('>', GuiElement.constant(this.plugin.itemManager()
                .getCustomItem(next)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(index + 1))
                        .withParameter(GuiParameters.MAX_PAGE, String.valueOf(recipes.size()))
                        .build())))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + next)), (e, c) -> {
            c.cancel();
            if (index + 1 < recipes.size()) {
                player.playSound(Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                openRecipePage(player, result, parentGui, recipes, index + 1, depth);
            }
        }))
        .addIngredient('<', GuiElement.constant(this.plugin.itemManager()
                .getCustomItem(previous)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(index + 1))
                        .withParameter(GuiParameters.MAX_PAGE, String.valueOf(recipes.size()))
                        .build())))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + previous)), (e, c) -> {
            c.cancel();
            if (index > 0) {
                player.playSound(Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                openRecipePage(player, result, parentGui, recipes, index - 1, depth);
            }
        }));

        String title;
        if (recipe.type() == RecipeTypes.SMELTING) {
            title = Constants.RECIPE_SMELTING_TITLE;
        } else if (recipe.type() == RecipeTypes.BLASTING) {
            title = Constants.RECIPE_BLASTING_TITLE;
        } else if (recipe.type() == RecipeTypes.CAMPFIRE_COOKING) {
            title = Constants.RECIPE_CAMPFIRE_TITLE;
        } else {
            title = Constants.RECIPE_SMOKING_TITLE;
        }

        BasicGui.builder()
                .layout(layout)
                .inventoryClickConsumer(c -> {
                    if (MOVE_TO_OTHER_INV.contains(c.type()) || DOUBLE_CLICK.contains(c.type())) {
                        c.cancel();
                    }
                })
                .build()
                .title(AdventureHelper.miniMessage().deserialize(title, ItemBuildContext.of(player, ContextHolder.EMPTY).tagResolvers()))
                .refresh()
                .open(player);
    }

    public void openCraftingRecipePage(Player player, Key result, CraftingTableRecipe<Object> recipe, Gui parentGui, List<Recipe<Object>> recipes, int index, int depth) {
        Key previous = index > 0 ? Constants.RECIPE_PREVIOUS_PAGE_AVAILABLE : Constants.RECIPE_PREVIOUS_PAGE_BLOCK;
        Key next = index + 1 < recipes.size() ? Constants.RECIPE_NEXT_PAGE_AVAILABLE : Constants.RECIPE_NEXT_PAGE_BLOCK;

        GuiLayout layout = new GuiLayout(
                "         ",
                " ABC     ",
                " DEF   X ",
                " GHI   ^ ",
                "         ",
                " <  =  > "
        )
        .addIngredient('X', GuiElement.constant(this.plugin.itemManager().createWrappedItem(result, player).count(recipe.result().count()), (e, c) -> {
            c.cancel();
            if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(result, player);
                item.count(item.maxStackSize());
                c.setItemOnCursor(item);
            }
        }))
        .addIngredient('^', player.hasPermission(GET_ITEM_PERMISSION) ? GuiElement.constant(this.plugin.itemManager().createWrappedItem(Constants.RECIPE_GET_ITEM, player), (e, c) -> {
            c.cancel();
            player.playSound(Constants.SOUND_PICK_ITEM);
            if (LEFT_CLICK.contains(c.type())) {
                player.giveItem(this.plugin.itemManager().createWrappedItem(result, player));
            } else if (RIGHT_CLICK.contains(c.type())) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(result, player);
                player.giveItem(item.count(item.maxStackSize()));
            }
        }) : GuiElement.EMPTY)
        .addIngredient('=', GuiElement.constant(this.plugin.itemManager().getCustomItem(Constants.RECIPE_BACK)
                        .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.EMPTY)))
                        .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + Constants.RECIPE_BACK)),
                ((element, click) -> {
                    click.cancel();
                    player.playSound(Constants.SOUND_RETURN_PAGE, 0.25f, 1);
                    parentGui.open(player);
                }))
        )
        .addIngredient('>', GuiElement.constant(this.plugin.itemManager()
                .getCustomItem(next)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(index + 1))
                        .withParameter(GuiParameters.MAX_PAGE, String.valueOf(recipes.size()))
                        .build())))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + next)), (e, c) -> {
            c.cancel();
            if (index + 1 < recipes.size()) {
                player.playSound(Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                openRecipePage(player, result, parentGui, recipes, index + 1, depth);
            }
        }))
        .addIngredient('<', GuiElement.constant(this.plugin.itemManager()
                .getCustomItem(previous)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(index + 1))
                        .withParameter(GuiParameters.MAX_PAGE, String.valueOf(recipes.size()))
                        .build())))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + previous)), (e, c) -> {
            c.cancel();
            if (index > 0) {
                player.playSound(Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                openRecipePage(player, result, parentGui, recipes, index - 1, depth);
            }
        }));

        char start = 'A';
        if (recipe.type() == RecipeTypes.SHAPED) {
            String[] pattern = ((CustomShapedRecipe<Object>) recipe).pattern().pattern();
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    char currentChar = (char) (start + x + y * 3);
                    if (x < pattern[0].length() && y < pattern.length) {
                        char ingredientChar = pattern[y].charAt(x);
                        net.momirealms.craftengine.core.item.recipe.Ingredient<Object> ingredient = ((CustomShapedRecipe<Object>) recipe).pattern().ingredients().get(ingredientChar);
                        if (ingredient == null) {
                            layout.addIngredient(currentChar, Ingredient.EMPTY);
                        } else {
                            List<Item<?>> ingredients = new ArrayList<>();
                            for (Holder<Key> in : ingredient.items()) {
                                ingredients.add(this.plugin.itemManager().createWrappedItem(in.value(), player));
                            }
                            layout.addIngredient(currentChar, GuiElement.recipeIngredient(ingredients, (e, c) -> {
                                c.cancel();
                                if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                                    Item<?> item = this.plugin.itemManager().createWrappedItem(e.item().id(), player);
                                    item.count(item.maxStackSize());
                                    c.setItemOnCursor(item);
                                    return;
                                }
                                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().getRecipeByResult(e.item().id());
                                if (!inRecipes.isEmpty()) {
                                    player.playSound(Constants.SOUND_CLICK_BUTTON);
                                    openRecipePage(player, e.item().id(), e.gui(), inRecipes, 0, depth + 1);
                                }
                            }));
                        }
                    } else {
                        layout.addIngredient(currentChar, Ingredient.EMPTY);
                    }
                }
            }
        } else {
            List<net.momirealms.craftengine.core.item.recipe.Ingredient<Object>> ingredients = ((CustomShapelessRecipe<Object>) recipe).ingredients();
            int i = 0;
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    char currentChar = (char) (start + x + y * 3);
                    if (i < ingredients.size()) {
                        List<Item<?>> ingredientItems = new ArrayList<>();
                        for (Holder<Key> in : ingredients.get(i).items()) {
                            ingredientItems.add(this.plugin.itemManager().createWrappedItem(in.value(), player));
                        }
                        layout.addIngredient(currentChar, GuiElement.recipeIngredient(ingredientItems, (e, c) -> {
                            c.cancel();
                            if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                                Item<?> item = this.plugin.itemManager().createWrappedItem(e.item().id(), player);
                                item.count(item.maxStackSize());
                                c.setItemOnCursor(item);
                                return;
                            }
                            List<Recipe<Object>> inRecipes = this.plugin.recipeManager().getRecipeByResult(e.item().id());
                            if (!inRecipes.isEmpty()) {
                                player.playSound(Constants.SOUND_CLICK_BUTTON);
                                openRecipePage(player, e.item().id(), e.gui(), inRecipes, 0, depth + 1);
                            }
                        }));
                    } else {
                        layout.addIngredient(currentChar, Ingredient.EMPTY);
                    }
                    i++;
                }
            }
        }

        BasicGui.builder()
                .layout(layout)
                .inventoryClickConsumer(c -> {
                    if (MOVE_TO_OTHER_INV.contains(c.type()) || DOUBLE_CLICK.contains(c.type())) {
                        c.cancel();
                    }
                })
                .build()
                .title(AdventureHelper.miniMessage().deserialize(Constants.RECIPE_CRAFTING_TITLE, ItemBuildContext.of(player, ContextHolder.EMPTY).tagResolvers()))
                .refresh()
                .open(player);
    }
}
