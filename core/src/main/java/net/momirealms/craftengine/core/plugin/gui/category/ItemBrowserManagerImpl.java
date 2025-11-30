package net.momirealms.craftengine.core.plugin.gui.category;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.recipe.*;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.config.IdSectionConfigParser;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.condition.AllOfCondition;
import net.momirealms.craftengine.core.plugin.context.event.EventConditions;
import net.momirealms.craftengine.core.plugin.gui.*;
import net.momirealms.craftengine.core.plugin.gui.Ingredient;
import net.momirealms.craftengine.core.util.*;

import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("DuplicatedCode")
public class ItemBrowserManagerImpl implements ItemBrowserManager {
    private static final String SHIFT_LEFT = "SHIFT_LEFT";
    private static final String SHIFT_RIGHT = "SHIFT_RIGHT";
    private static final Set<String> MOVE_TO_OTHER_INV = Set.of("SHIFT_LEFT", "SHIFT_RIGHT");
    private static final Set<String> LEFT_CLICK = Set.of("LEFT", SHIFT_LEFT);
    private static final Set<String> RIGHT_CLICK = Set.of("RIGHT", SHIFT_RIGHT);
    private static final Set<String> MIDDLE_CLICK = Set.of("MIDDLE");
    private static final Set<String> DOUBLE_CLICK = Set.of("DOUBLE_CLICK");
    private final CraftEngine plugin;
    private final Map<Key, Category> byId;
    private final TreeSet<Category> categoryOnMainPage;
    private final Map<Key, List<Key>> externalMembers;
    private final CategoryParser categoryParser;

    public ItemBrowserManagerImpl(CraftEngine plugin) {
        this.plugin = plugin;
        this.byId = new HashMap<>();
        this.externalMembers = new HashMap<>();
        this.categoryOnMainPage = new TreeSet<>();
        this.categoryParser = new CategoryParser();
    }

    @Override
    public void unload() {
        this.byId.clear();
        this.categoryOnMainPage.clear();
        this.externalMembers.clear();
    }

    @Override
    public void delayedLoad() {
        for (Map.Entry<Key, List<Key>> entry : this.externalMembers.entrySet()) {
            Key item = entry.getKey();
            for (Key categoryId : entry.getValue()) {
                Optional.ofNullable(this.byId.get(categoryId)).ifPresent(category -> {
                    category.addMember(item.toString());
                });
            }
        }
        for (Category category : this.byId.values()) {
            if (!category.hidden()) {
                this.categoryOnMainPage.add(category);
            }
        }
        Constants.load();
    }

    @Override
    public ConfigParser parser() {
        return this.categoryParser;
    }

    @Override
    public void addExternalCategoryMember(Key item, List<Key> category) {
        List<Key> categories = this.externalMembers.computeIfAbsent(item, k -> new ArrayList<>());
        categories.addAll(category);
    }

    @Override
    public void open(Player player) {
        openItemBrowser(player);
    }

    @Override
    public TreeSet<Category> categories() {
        return categoryOnMainPage;
    }

    @Override
    public Optional<Category> byId(Key key) {
        return Optional.ofNullable(this.byId.get(key));
    }

    public class CategoryParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"categories", "category"};

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int loadingSequence() {
            return LoadingSequence.CATEGORY;
        }

        @Override
        public void parseSection(Pack pack, Path path, String node, Key id, Map<String, Object> section) {
            String name = section.getOrDefault("name", id).toString();
            List<String> members = MiscUtils.getAsStringList(section.getOrDefault("list", List.of()));
            Key icon = Key.of(section.getOrDefault("icon", ItemKeys.STONE).toString());
            int priority = ResourceConfigUtils.getAsInt(section.getOrDefault("priority", 0), "priority");
            List<String> lore = MiscUtils.getAsStringList(section.getOrDefault("lore", List.of()));
            boolean hidden = ResourceConfigUtils.getAsBoolean(section.getOrDefault("hidden", false), "hidden");
            List<Condition<Context>> conditionList = ResourceConfigUtils.parseConfigAsList(ResourceConfigUtils.get(section, "conditions", "condition"), EventConditions::fromMap);
            Condition<Context> conditions = conditionList.isEmpty() ? null : conditionList.size() == 1 ? conditionList.getFirst() : new AllOfCondition<>(conditionList);
            Category category = new Category(id, name, lore, icon, new ArrayList<>(members), priority, hidden, conditions);
            if (ItemBrowserManagerImpl.this.byId.containsKey(id)) {
                ItemBrowserManagerImpl.this.byId.get(id).merge(category);
            } else {
                ItemBrowserManagerImpl.this.byId.put(id, category);
            }
        }
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
                            )))
                            .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + next));
                }, true)
        )
        .addIngredient('<', GuiElement.paged((element) -> {
                    Key previous = element.gui().hasPreviousPage() ? Constants.BROWSER_PREVIOUS_PAGE_AVAILABLE : Constants.BROWSER_PREVIOUS_PAGE_BLOCK;
                    return this.plugin.itemManager().getCustomItem(previous)
                            .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                                    .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(element.gui().currentPage()))
                                    .withParameter(GuiParameters.MAX_PAGE, String.valueOf(element.gui().maxPages()))
                            )))
                            .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + previous));
                }, false)
        );

        List<ItemWithAction> iconList = this.categoryOnMainPage.stream().map(it -> {
            Condition<Context> condition = it.condition();
            if (condition != null && !condition.test(PlayerOptionalContext.of(player))) {
                return null;
            }
            Item<?> item = this.plugin.itemManager().createWrappedItem(it.icon(), player);
            if (ItemUtils.isEmpty(item)) {
                this.plugin.logger().warn("Can't not find item " + it.icon() + " for category icon");
                return null;
            }
            item.customNameJson(AdventureHelper.componentToJson(AdventureHelper.miniMessage().deserialize(it.displayName(), ItemBuildContext.EMPTY_RESOLVERS)));
            item.loreJson(it.displayLore().stream().map(lore -> AdventureHelper.componentToJson(AdventureHelper.miniMessage().deserialize(lore, ItemBuildContext.EMPTY_RESOLVERS))).toList());
            return new ItemWithAction(item, (element, click) -> {
                click.cancel();
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                openCategoryPage(click.clicker(), it.id(), element.gui(), true);
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
                .title(AdventureHelper.miniMessage().deserialize(Constants.BROWSER_TITLE, PlayerOptionalContext.of(player).tagResolvers()))
                .refresh()
                .open(player);
    }

    public void openCategoryPage(Player player, Key categoryId, Gui parentGui, boolean canOpenNoRecipePage) {
        GuiLayout layout = new GuiLayout(
                "AAAAAAAAA",
                "AAAAAAAAA",
                "AAAAAAAAA",
                "AAAAAAAAA",
                "AAAAAAAAA",
                " <  =  > "
        )
        .addIngredient('A', Ingredient.paged())
        .addIngredient('=', GuiElement.constant(this.plugin.itemManager().getCustomItem(parentGui != null ? Constants.CATEGORY_BACK : Constants.CATEGORY_EXIT)
                .map(it -> it.buildItem(ItemBuildContext.of(player)))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + (parentGui != null ? Constants.CATEGORY_BACK : Constants.CATEGORY_EXIT))),
                ((element, click) -> {
                    click.cancel();
                    player.playSound(Constants.SOUND_RETURN_PAGE, 0.25f, 1);
                    if (parentGui != null) {
                        parentGui.open(player);
                    } else {
                        player.closeInventory();
                    }
                }))
        )
        .addIngredient('>', GuiElement.paged((element) -> {
                    Key next = element.gui().hasNextPage() ? Constants.CATEGORY_NEXT_PAGE_AVAILABLE : Constants.CATEGORY_NEXT_PAGE_BLOCK;
                    return this.plugin.itemManager().getCustomItem(next)
                            .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                                    .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(element.gui().currentPage()))
                                    .withParameter(GuiParameters.MAX_PAGE, String.valueOf(element.gui().maxPages()))
                            )))
                            .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + next));
                }, true)
        )
        .addIngredient('<', GuiElement.paged((element) -> {
                    Key previous = element.gui().hasPreviousPage() ? Constants.CATEGORY_PREVIOUS_PAGE_AVAILABLE : Constants.CATEGORY_PREVIOUS_PAGE_BLOCK;
                    return this.plugin.itemManager().getCustomItem(previous)
                            .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                                    .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(element.gui().currentPage()))
                                    .withParameter(GuiParameters.MAX_PAGE, String.valueOf(element.gui().maxPages()))
                            )))
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
            if (it.charAt(0) == '#') {
                String subCategoryId = it.substring(1);
                Category subCategory = this.byId.get(Key.of(subCategoryId));
                Item<?> item;
                if (subCategory == null) {
                    item = Objects.requireNonNull(this.plugin.itemManager().createWrappedItem(ItemKeys.BARRIER, player));
                    item.customNameJson(AdventureHelper.componentToJson(Component.text(subCategoryId).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
                } else {
                    Condition<Context> condition = subCategory.condition();
                    if (condition != null && !condition.test(PlayerOptionalContext.of(player))) {
                        return null;
                    }
                    item = this.plugin.itemManager().createWrappedItem(subCategory.icon(), player);
                    if (ItemUtils.isEmpty(item)) {
                        if (!subCategory.icon().equals(ItemKeys.AIR)) {
                            item = Objects.requireNonNull(this.plugin.itemManager().createWrappedItem(ItemKeys.BARRIER, player));
                            item.customNameJson(AdventureHelper.componentToJson(AdventureHelper.miniMessage().deserialize(subCategory.displayName(), ItemBuildContext.EMPTY_RESOLVERS)));
                            item.loreJson(subCategory.displayLore().stream().map(lore -> AdventureHelper.componentToJson(AdventureHelper.miniMessage().deserialize(lore, ItemBuildContext.EMPTY_RESOLVERS))).toList());
                        }
                    } else {
                        item.customNameJson(AdventureHelper.componentToJson(AdventureHelper.miniMessage().deserialize(subCategory.displayName(), ItemBuildContext.EMPTY_RESOLVERS)));
                        item.loreJson(subCategory.displayLore().stream().map(lore -> AdventureHelper.componentToJson(AdventureHelper.miniMessage().deserialize(lore, ItemBuildContext.EMPTY_RESOLVERS))).toList());
                    }
                }
                return new ItemWithAction(item, (element, click) -> {
                    click.cancel();
                    player.playSound(Constants.SOUND_CLICK_BUTTON);
                    if (subCategory == null) return;
                    openCategoryPage(click.clicker(), subCategory.id(), element.gui(), canOpenNoRecipePage);
                });
            } else {
                Key itemId = Key.of(it);
                Item<?> item = this.plugin.itemManager().createWrappedItem(itemId, player);
                boolean canGoFurther;
                if (ItemUtils.isEmpty(item)) {
                    if (!itemId.equals(ItemKeys.AIR)) {
                        item = this.plugin.itemManager().createWrappedItem(ItemKeys.BARRIER, player);
                        item.customNameJson(AdventureHelper.componentToJson(Component.text(it).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE).color(NamedTextColor.RED)));
                    }
                    canGoFurther = false;
                } else {
                    canGoFurther = true;
                }
                return new ItemWithAction(item, (e, c) -> {
                    c.cancel();
                    Item<?> eItem = e.item();
                    if (!canGoFurther) {
                        return;
                    }
                    if (player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION)) {
                        if (MIDDLE_CLICK.contains(c.type()) && c.itemOnCursor() == null) {
                            Item<?> newItem = this.plugin.itemManager().createWrappedItem(eItem.id(), player);
                            newItem.count(newItem.maxStackSize());
                            c.setItemOnCursor(newItem);
                            return;
                        }
                        if (SHIFT_LEFT.equals(c.type())) {
                            player.giveItem(this.plugin.itemManager().createWrappedItem(eItem.id(), player));
                            return;
                        } else if (SHIFT_RIGHT.equals(c.type())) {
                            Item<?> newItem = this.plugin.itemManager().createWrappedItem(eItem.id(), player);
                            newItem.count(newItem.maxStackSize());
                            player.giveItem(newItem);
                            return;
                        }
                    }
                    if (LEFT_CLICK.contains(c.type())) {
                        List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByResult(itemId);
                        player.playSound(Constants.SOUND_CLICK_BUTTON);
                        if (!inRecipes.isEmpty()) {
                            openRecipePage(c.clicker(), e.gui(), inRecipes, 0, 0, canOpenNoRecipePage);
                        } else if (canOpenNoRecipePage) {
                            openNoRecipePage(player, itemId, e.gui(), 0);
                        }
                    } else if (RIGHT_CLICK.contains(c.type())) {
                        List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByIngredient(itemId);
                        player.playSound(Constants.SOUND_CLICK_BUTTON);
                        if (!inRecipes.isEmpty()) {
                            openRecipePage(c.clicker(), e.gui(), inRecipes, 0, 0, canOpenNoRecipePage);
                        }
                    }
                });
            }
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
                .title(AdventureHelper.miniMessage().deserialize(Constants.CATEGORY_TITLE, PlayerOptionalContext.of(player).tagResolvers()))
                .refresh()
                .open(player);
    }

    @Override
    public void openNoRecipePage(Player player, Key result, Gui parentGui, int depth) {
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
            } else if (RIGHT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByIngredient(result);
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, true);
                }
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
        .addIngredient('=', GuiElement.constant(this.plugin.itemManager().getCustomItem(parentGui != null ? Constants.RECIPE_BACK : Constants.RECIPE_EXIT)
                        .map(it -> it.buildItem(ItemBuildContext.of(player)))
                        .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + (parentGui != null ? Constants.RECIPE_BACK : Constants.RECIPE_EXIT))),
                ((element, click) -> {
                    click.cancel();
                    player.playSound(Constants.SOUND_RETURN_PAGE, 0.25f, 1);
                    if (parentGui != null) {
                        parentGui.open(player);
                    } else {
                        player.closeInventory();
                    }
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
                .title(AdventureHelper.miniMessage().deserialize(Constants.RECIPE_NONE_TITLE, PlayerOptionalContext.of(player).tagResolvers()))
                .refresh()
                .open(player);
    }

    @Override
    public void openRecipePage(Player player, Gui parentGui, List<Recipe<Object>> recipes, int index, int depth, boolean canOpenNoRecipePage) {
        if (index >= recipes.size()) return;
        if (depth > MAX_RECIPE_DEPTH) return;
        Recipe<Object> recipe = recipes.get(index);
        Key recipeType = recipe.serializerType();
        if (recipeType == RecipeSerializers.SHAPELESS || recipeType == RecipeSerializers.SHAPED) {
            openCraftingRecipePage(player, (CustomCraftingTableRecipe<Object>) recipe, parentGui, recipes, index, depth, canOpenNoRecipePage);
            return;
        }
        if (recipeType == RecipeSerializers.BLASTING || recipeType == RecipeSerializers.CAMPFIRE_COOKING || recipeType == RecipeSerializers.SMOKING || recipeType == RecipeSerializers.SMELTING) {
            openCookingRecipePage(player, (CustomCookingRecipe<Object>) recipe, parentGui, recipes, index, depth, canOpenNoRecipePage);
            return;
        }
        if (recipeType == RecipeSerializers.STONECUTTING) {
            openStoneCuttingRecipePage(player, (CustomStoneCuttingRecipe<Object>) recipe, parentGui, recipes, index, depth, canOpenNoRecipePage);
            return;
        }
        if (recipeType == RecipeSerializers.SMITHING_TRANSFORM) {
            openSmithingTransformRecipePage(player, (CustomSmithingTransformRecipe<Object>) recipe, parentGui, recipes, index, depth, canOpenNoRecipePage);
            return;
        }
        if (recipeType == RecipeSerializers.BREWING) {
            openBrewingRecipePage(player, (CustomBrewingRecipe<Object>) recipe, parentGui, recipes, index, depth, canOpenNoRecipePage);
            return;
        }
    }

    public void openBrewingRecipePage(Player player, CustomBrewingRecipe<Object> recipe, Gui parentGui, List<Recipe<Object>> recipes, int index, int depth, boolean canOpenNoRecipePage) {
        Key previous = index > 0 ? Constants.RECIPE_PREVIOUS_PAGE_AVAILABLE : Constants.RECIPE_PREVIOUS_PAGE_BLOCK;
        Key next = index + 1 < recipes.size() ? Constants.RECIPE_NEXT_PAGE_AVAILABLE : Constants.RECIPE_NEXT_PAGE_BLOCK;
        Key result = recipe.result().item().id();

        List<Item<?>> ingredients = new ArrayList<>();
        net.momirealms.craftengine.core.item.recipe.Ingredient<Object> ingredient = recipe.ingredient();
        for (UniqueKey in : ingredient.items()) {
            ingredients.add(this.plugin.itemManager().createWrappedItem(in.key(), player));
        }

        List<Item<?>> containers = new ArrayList<>();
        net.momirealms.craftengine.core.item.recipe.Ingredient<Object> container = recipe.container();
        for (UniqueKey in : container.items()) {
            containers.add(this.plugin.itemManager().createWrappedItem(in.key(), player));
        }

        GuiLayout layout = new GuiLayout(
                "         ",
                "   A     ",
                "         ",
                "   B X   ",
                "     ^   ",
                " <  =  > "
        )
        .addIngredient('X', GuiElement.constant(this.plugin.itemManager().createWrappedItem(result, player).count(recipe.result().count()), (e, c) -> {
            c.cancel();
            if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(result, player);
                item.count(item.maxStackSize());
                c.setItemOnCursor(item);
                return;
            }
            if (LEFT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByResult(result);
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                } else if (canOpenNoRecipePage) {
                    openNoRecipePage(player, result, e.gui(), 0);
                }
            } else if (RIGHT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByIngredient(result);
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                }
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
        .addIngredient('=', GuiElement.constant(this.plugin.itemManager().getCustomItem(parentGui != null ? Constants.RECIPE_BACK : Constants.RECIPE_EXIT)
                        .map(it -> it.buildItem(ItemBuildContext.of(player)))
                        .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + (parentGui != null ? Constants.RECIPE_BACK : Constants.RECIPE_EXIT))),
                ((element, click) -> {
                    click.cancel();
                    player.playSound(Constants.SOUND_RETURN_PAGE, 0.25f, 1);
                    if (parentGui != null) {
                        parentGui.open(player);
                    } else {
                        player.closeInventory();
                    }
                }))
        )
        .addIngredient('>', GuiElement.constant(this.plugin.itemManager()
                .getCustomItem(next)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(index + 1))
                        .withParameter(GuiParameters.MAX_PAGE, String.valueOf(recipes.size()))
                )))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + next)), (e, c) -> {
            c.cancel();
            if (index + 1 < recipes.size()) {
                player.playSound(Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                openRecipePage(player, parentGui, recipes, index + 1, depth, canOpenNoRecipePage);
            }
        }))
        .addIngredient('<', GuiElement.constant(this.plugin.itemManager()
                .getCustomItem(previous)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(index + 1))
                        .withParameter(GuiParameters.MAX_PAGE, String.valueOf(recipes.size()))
                )))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + previous)), (e, c) -> {
            c.cancel();
            if (index > 0) {
                player.playSound(Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                openRecipePage(player, parentGui, recipes, index - 1, depth, canOpenNoRecipePage);
            }
        }))
        .addIngredient('A', GuiElement.recipeIngredient(ingredients, (e, c) -> {
            c.cancel();
            if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(e.item().id(), player);
                item.count(item.maxStackSize());
                c.setItemOnCursor(item);
                return;
            }
            if (LEFT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByResult(e.item().id());
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                } else if (canOpenNoRecipePage) {
                    openNoRecipePage(player, e.item().id(), e.gui(), 0);
                }
            } else if (RIGHT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByIngredient(e.item().id());
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                }
            }
        }))
        .addIngredient('B', GuiElement.recipeIngredient(containers, (e, c) -> {
            c.cancel();
            if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(e.item().id(), player);
                item.count(item.maxStackSize());
                c.setItemOnCursor(item);
                return;
            }
            if (LEFT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByResult(e.item().id());
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                } else if (canOpenNoRecipePage) {
                    openNoRecipePage(player, e.item().id(), e.gui(), 0);
                }
            } else if (RIGHT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByIngredient(e.item().id());
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                }
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
                .title(AdventureHelper.miniMessage().deserialize(Constants.RECIPE_BREWING_TITLE, PlayerOptionalContext.of(player).tagResolvers()))
                .refresh()
                .open(player);
    }

    public void openSmithingTransformRecipePage(Player player, CustomSmithingTransformRecipe<Object> recipe, Gui parentGui, List<Recipe<Object>> recipes, int index, int depth, boolean canOpenNoRecipePage) {
        Key previous = index > 0 ? Constants.RECIPE_PREVIOUS_PAGE_AVAILABLE : Constants.RECIPE_PREVIOUS_PAGE_BLOCK;
        Key next = index + 1 < recipes.size() ? Constants.RECIPE_NEXT_PAGE_AVAILABLE : Constants.RECIPE_NEXT_PAGE_BLOCK;
        Key result = recipe.result().item().id();
        GuiLayout layout = new GuiLayout(
                "         ",
                "         ",
                " ABC  X  ",
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
                return;
            }
            if (LEFT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByResult(result);
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                } else if (canOpenNoRecipePage) {
                    openNoRecipePage(player, result, e.gui(), 0);
                }
            } else if (RIGHT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByIngredient(result);
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                }
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
        .addIngredient('=', GuiElement.constant(this.plugin.itemManager().getCustomItem(parentGui != null ? Constants.RECIPE_BACK : Constants.RECIPE_EXIT)
                        .map(it -> it.buildItem(ItemBuildContext.of(player)))
                        .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + (parentGui != null ? Constants.RECIPE_BACK : Constants.RECIPE_EXIT))),
                ((element, click) -> {
                    click.cancel();
                    player.playSound(Constants.SOUND_RETURN_PAGE, 0.25f, 1);
                    if (parentGui != null) {
                        parentGui.open(player);
                    } else {
                        player.closeInventory();
                    }
                }))
        )
        .addIngredient('>', GuiElement.constant(this.plugin.itemManager()
                .getCustomItem(next)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(index + 1))
                        .withParameter(GuiParameters.MAX_PAGE, String.valueOf(recipes.size()))
                )))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + next)), (e, c) -> {
            c.cancel();
            if (index + 1 < recipes.size()) {
                player.playSound(Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                openRecipePage(player, parentGui, recipes, index + 1, depth, canOpenNoRecipePage);
            }
        }))
        .addIngredient('<', GuiElement.constant(this.plugin.itemManager()
                .getCustomItem(previous)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(index + 1))
                        .withParameter(GuiParameters.MAX_PAGE, String.valueOf(recipes.size()))
                )))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + previous)), (e, c) -> {
            c.cancel();
            if (index > 0) {
                player.playSound(Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                openRecipePage(player, parentGui, recipes, index - 1, depth, canOpenNoRecipePage);
            }
        }));

        List<Item<?>> templates = new ArrayList<>();
        Optional.ofNullable(recipe.template()).ifPresent(it -> {
            for (UniqueKey in : it.items()) {
                templates.add(this.plugin.itemManager().createWrappedItem(in.key(), player));
            }
        });
        layout.addIngredient('A', templates.isEmpty() ? GuiElement.EMPTY : GuiElement.recipeIngredient(templates, (e, c) -> {
            c.cancel();
            if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(e.item().id(), player);
                item.count(item.maxStackSize());
                c.setItemOnCursor(item);
                return;
            }
            if (LEFT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByResult(e.item().id());
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                } else if (canOpenNoRecipePage) {
                    openNoRecipePage(player, e.item().id(), e.gui(), 0);
                }
            } else if (RIGHT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByIngredient(e.item().id());
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                }
            }
        }));

        List<Item<?>> bases = new ArrayList<>();
        Optional.ofNullable(recipe.base()).ifPresent(it -> {
            for (UniqueKey in : it.items()) {
                bases.add(this.plugin.itemManager().createWrappedItem(in.key(), player));
            }
        });
        layout.addIngredient('B', bases.isEmpty() ? GuiElement.EMPTY : GuiElement.recipeIngredient(bases, (e, c) -> {
            c.cancel();
            if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(e.item().id(), player);
                item.count(item.maxStackSize());
                c.setItemOnCursor(item);
                return;
            }
            if (LEFT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByResult(e.item().id());
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                } else if (canOpenNoRecipePage) {
                    openNoRecipePage(player, e.item().id(), e.gui(), 0);
                }
            } else if (RIGHT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByIngredient(e.item().id());
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                }
            }
        }));

        List<Item<?>> additions = new ArrayList<>();
        Optional.ofNullable(recipe.addition()).ifPresent(it -> {
            for (UniqueKey in : it.items()) {
                additions.add(this.plugin.itemManager().createWrappedItem(in.key(), player));
            }
        });
        layout.addIngredient('C', additions.isEmpty() ? GuiElement.EMPTY : GuiElement.recipeIngredient(additions, (e, c) -> {
            c.cancel();
            if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(e.item().id(), player);
                item.count(item.maxStackSize());
                c.setItemOnCursor(item);
                return;
            }
            if (LEFT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByResult(e.item().id());
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                } else if (canOpenNoRecipePage) {
                    openNoRecipePage(player, e.item().id(), e.gui(), 0);
                }
            } else if (RIGHT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByIngredient(e.item().id());
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                }
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
                .title(AdventureHelper.miniMessage().deserialize(Constants.RECIPE_SMITHING_TRANSFORM_TITLE, PlayerOptionalContext.of(player).tagResolvers()))
                .refresh()
                .open(player);
    }

    public void openStoneCuttingRecipePage(Player player, CustomStoneCuttingRecipe<Object> recipe, Gui parentGui, List<Recipe<Object>> recipes, int index, int depth, boolean canOpenNoRecipePage) {
        Key previous = index > 0 ? Constants.RECIPE_PREVIOUS_PAGE_AVAILABLE : Constants.RECIPE_PREVIOUS_PAGE_BLOCK;
        Key next = index + 1 < recipes.size() ? Constants.RECIPE_NEXT_PAGE_AVAILABLE : Constants.RECIPE_NEXT_PAGE_BLOCK;
        Key result = recipe.result().item().id();

        List<Item<?>> ingredients = new ArrayList<>();
        net.momirealms.craftengine.core.item.recipe.Ingredient<Object> ingredient = recipe.ingredient();
        for (UniqueKey in : ingredient.items()) {
            ingredients.add(this.plugin.itemManager().createWrappedItem(in.key(), player));
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
                return;
            }
            if (LEFT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByResult(result);
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                } else if (canOpenNoRecipePage) {
                    openNoRecipePage(player, result, e.gui(), 0);
                }
            } else if (RIGHT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByIngredient(result);
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                }
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
            if (LEFT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByResult(e.item().id());
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                } else if (canOpenNoRecipePage) {
                    openNoRecipePage(player, e.item().id(), e.gui(), 0);
                }
            } else if (RIGHT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByIngredient(e.item().id());
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                }
            }
        }))
        .addIngredient('=', GuiElement.constant(this.plugin.itemManager().getCustomItem(parentGui != null ? Constants.RECIPE_BACK : Constants.RECIPE_EXIT)
                        .map(it -> it.buildItem(ItemBuildContext.of(player)))
                        .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + (parentGui != null ? Constants.RECIPE_BACK : Constants.RECIPE_EXIT))),
                ((element, click) -> {
                    click.cancel();
                    player.playSound(Constants.SOUND_RETURN_PAGE, 0.25f, 1);
                    if (parentGui != null) {
                        parentGui.open(player);
                    } else {
                        player.closeInventory();
                    }
                }))
        )
        .addIngredient('>', GuiElement.constant(this.plugin.itemManager()
                .getCustomItem(next)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(index + 1))
                        .withParameter(GuiParameters.MAX_PAGE, String.valueOf(recipes.size()))
                )))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + next)), (e, c) -> {
            c.cancel();
            if (index + 1 < recipes.size()) {
                player.playSound(Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                openRecipePage(player, parentGui, recipes, index + 1, depth, canOpenNoRecipePage);
            }
        }))
        .addIngredient('<', GuiElement.constant(this.plugin.itemManager()
                .getCustomItem(previous)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(index + 1))
                        .withParameter(GuiParameters.MAX_PAGE, String.valueOf(recipes.size()))
                )))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + previous)), (e, c) -> {
            c.cancel();
            if (index > 0) {
                player.playSound(Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                openRecipePage(player, parentGui, recipes, index - 1, depth, canOpenNoRecipePage);
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
                .title(AdventureHelper.miniMessage().deserialize(Constants.RECIPE_STONECUTTING_TITLE, PlayerOptionalContext.of(player).tagResolvers()))
                .refresh()
                .open(player);
    }

    public void openCookingRecipePage(Player player, CustomCookingRecipe<Object> recipe, Gui parentGui, List<Recipe<Object>> recipes, int index, int depth, boolean canOpenNoRecipePage) {
        Key previous = index > 0 ? Constants.RECIPE_PREVIOUS_PAGE_AVAILABLE : Constants.RECIPE_PREVIOUS_PAGE_BLOCK;
        Key next = index + 1 < recipes.size() ? Constants.RECIPE_NEXT_PAGE_AVAILABLE : Constants.RECIPE_NEXT_PAGE_BLOCK;
        Key result = recipe.result().item().id();

        List<Item<?>> ingredients = new ArrayList<>();
        net.momirealms.craftengine.core.item.recipe.Ingredient<Object> ingredient = recipe.ingredient();
        for (UniqueKey in : ingredient.items()) {
            ingredients.add(this.plugin.itemManager().createWrappedItem(in.key(), player));
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
                return;
            }
            if (LEFT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByResult(result);
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                } else if (canOpenNoRecipePage) {
                    openNoRecipePage(player, result, e.gui(), 0);
                }
            } else if (RIGHT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByIngredient(result);
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                }
            }
        }))
        .addIngredient('?', GuiElement.constant(this.plugin.itemManager().getCustomItem(Constants.RECIPE_COOKING_INFO)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.COOKING_TIME, String.valueOf(recipe.cookingTime()))
                        .withParameter(GuiParameters.COOKING_EXPERIENCE, String.valueOf(recipe.experience()))
                )))
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
            if (LEFT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByResult(e.item().id());
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                } else if (canOpenNoRecipePage) {
                    openNoRecipePage(player, e.item().id(), e.gui(), 0);
                }
            } else if (RIGHT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByIngredient(e.item().id());
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                }
            }
        }))
        .addIngredient('=', GuiElement.constant(this.plugin.itemManager().getCustomItem(parentGui != null ? Constants.RECIPE_BACK : Constants.RECIPE_EXIT)
                        .map(it -> it.buildItem(ItemBuildContext.of(player)))
                        .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + (parentGui != null ? Constants.RECIPE_BACK : Constants.RECIPE_EXIT))),
                ((element, click) -> {
                    click.cancel();
                    player.playSound(Constants.SOUND_RETURN_PAGE, 0.25f, 1);
                    if (parentGui != null) {
                        parentGui.open(player);
                    } else {
                        player.closeInventory();
                    }
                }))
        )
        .addIngredient('>', GuiElement.constant(this.plugin.itemManager()
                .getCustomItem(next)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(index + 1))
                        .withParameter(GuiParameters.MAX_PAGE, String.valueOf(recipes.size()))
                )))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + next)), (e, c) -> {
            c.cancel();
            if (index + 1 < recipes.size()) {
                player.playSound(Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                openRecipePage(player, parentGui, recipes, index + 1, depth, canOpenNoRecipePage);
            }
        }))
        .addIngredient('<', GuiElement.constant(this.plugin.itemManager()
                .getCustomItem(previous)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(index + 1))
                        .withParameter(GuiParameters.MAX_PAGE, String.valueOf(recipes.size()))
                )))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + previous)), (e, c) -> {
            c.cancel();
            if (index > 0) {
                player.playSound(Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                openRecipePage(player, parentGui, recipes, index - 1, depth, canOpenNoRecipePage);
            }
        }));

        String title;
        if (recipe.serializerType() == RecipeSerializers.SMELTING) {
            title = Constants.RECIPE_SMELTING_TITLE;
        } else if (recipe.serializerType() == RecipeSerializers.BLASTING) {
            title = Constants.RECIPE_BLASTING_TITLE;
        } else if (recipe.serializerType() == RecipeSerializers.CAMPFIRE_COOKING) {
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
                .title(AdventureHelper.miniMessage().deserialize(title, PlayerOptionalContext.of(player).tagResolvers()))
                .refresh()
                .open(player);
    }

    public void openCraftingRecipePage(Player player, CustomCraftingTableRecipe<Object> recipe, Gui parentGui, List<Recipe<Object>> recipes, int index, int depth, boolean canOpenNoRecipePage) {
        Key previous = index > 0 ? Constants.RECIPE_PREVIOUS_PAGE_AVAILABLE : Constants.RECIPE_PREVIOUS_PAGE_BLOCK;
        Key next = index + 1 < recipes.size() ? Constants.RECIPE_NEXT_PAGE_AVAILABLE : Constants.RECIPE_NEXT_PAGE_BLOCK;
        Key result = recipe.result().item().id();

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
                Item<?> item = this.plugin.itemManager().createWrappedItem(recipe.result().item().id(), player);
                item.count(item.maxStackSize());
                c.setItemOnCursor(item);
                return;
            }
            if (LEFT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByResult(result);
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                } else if (canOpenNoRecipePage) {
                    openNoRecipePage(player, result, e.gui(), 0);
                }
            } else if (RIGHT_CLICK.contains(c.type())) {
                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByIngredient(result);
                if (inRecipes == recipes) return;
                player.playSound(Constants.SOUND_CLICK_BUTTON);
                if (!inRecipes.isEmpty()) {
                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                }
            }
        }))
        .addIngredient('^', player.hasPermission(GET_ITEM_PERMISSION) ? GuiElement.constant(this.plugin.itemManager().createWrappedItem(Constants.RECIPE_GET_ITEM, player), (e, c) -> {
            c.cancel();
            player.playSound(Constants.SOUND_PICK_ITEM);
            if (LEFT_CLICK.contains(c.type())) {
                player.giveItem(this.plugin.itemManager().createWrappedItem(recipe.result().item().id(), player));
            } else if (RIGHT_CLICK.contains(c.type())) {
                Item<?> item = this.plugin.itemManager().createWrappedItem(recipe.result().item().id(), player);
                player.giveItem(item.count(item.maxStackSize()));
            }
        }) : GuiElement.EMPTY)
        .addIngredient('=', GuiElement.constant(this.plugin.itemManager().getCustomItem(parentGui != null ? Constants.RECIPE_BACK : Constants.RECIPE_EXIT)
                        .map(it -> it.buildItem(ItemBuildContext.of(player)))
                        .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + (parentGui != null ? Constants.RECIPE_BACK : Constants.RECIPE_EXIT))),
                ((element, click) -> {
                    click.cancel();
                    player.playSound(Constants.SOUND_RETURN_PAGE, 0.25f, 1);
                    if (parentGui != null) {
                        parentGui.open(player);
                    } else {
                        player.closeInventory();
                    }
                }))
        )
        .addIngredient('>', GuiElement.constant(this.plugin.itemManager()
                .getCustomItem(next)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(index + 1))
                        .withParameter(GuiParameters.MAX_PAGE, String.valueOf(recipes.size()))
                )))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + next)), (e, c) -> {
            c.cancel();
            if (index + 1 < recipes.size()) {
                player.playSound(Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                openRecipePage(player, parentGui, recipes, index + 1, depth, canOpenNoRecipePage);
            }
        }))
        .addIngredient('<', GuiElement.constant(this.plugin.itemManager()
                .getCustomItem(previous)
                .map(it -> it.buildItem(ItemBuildContext.of(player, ContextHolder.builder()
                        .withParameter(GuiParameters.CURRENT_PAGE, String.valueOf(index + 1))
                        .withParameter(GuiParameters.MAX_PAGE, String.valueOf(recipes.size()))
                )))
                .orElseThrow(() -> new GuiElementMissingException("Can't find gui element " + previous)), (e, c) -> {
            c.cancel();
            if (index > 0) {
                player.playSound(Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                openRecipePage(player, parentGui, recipes, index - 1, depth, canOpenNoRecipePage);
            }
        }));

        char start = 'A';
        if (recipe.serializerType() == RecipeSerializers.SHAPED) {
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
                            for (UniqueKey in : ingredient.items()) {
                                ingredients.add(this.plugin.itemManager().createWrappedItem(in.key(), player));
                            }
                            layout.addIngredient(currentChar, GuiElement.recipeIngredient(ingredients, (e, c) -> {
                                c.cancel();
                                if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                                    Item<?> item = this.plugin.itemManager().createWrappedItem(e.item().id(), player);
                                    item.count(item.maxStackSize());
                                    c.setItemOnCursor(item);
                                    return;
                                }
                                if (LEFT_CLICK.contains(c.type())) {
                                    List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByResult(e.item().id());
                                    if (inRecipes == recipes) return;
                                    player.playSound(Constants.SOUND_CLICK_BUTTON);
                                    if (!inRecipes.isEmpty()) {
                                        openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                                    } else if (canOpenNoRecipePage) {
                                        openNoRecipePage(player, e.item().id(), e.gui(), 0);
                                    }
                                } else if (RIGHT_CLICK.contains(c.type())) {
                                    List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByIngredient(e.item().id());
                                    if (inRecipes == recipes) return;
                                    player.playSound(Constants.SOUND_CLICK_BUTTON);
                                    if (!inRecipes.isEmpty()) {
                                        openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                                    }
                                }
                            }));
                        }
                    } else {
                        layout.addIngredient(currentChar, Ingredient.EMPTY);
                    }
                }
            }
        } else {
            List<net.momirealms.craftengine.core.item.recipe.Ingredient<Object>> ingredients = recipe.ingredientsInUse();
            int i = 0;
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    char currentChar = (char) (start + x + y * 3);
                    if (i < ingredients.size()) {
                        List<Item<?>> ingredientItems = new ArrayList<>();
                        for (UniqueKey in : ingredients.get(i).items()) {
                            ingredientItems.add(this.plugin.itemManager().createWrappedItem(in.key(), player));
                        }
                        layout.addIngredient(currentChar, GuiElement.recipeIngredient(ingredientItems, (e, c) -> {
                            c.cancel();
                            if (MIDDLE_CLICK.contains(c.type()) && player.isCreativeMode() && player.hasPermission(GET_ITEM_PERMISSION) && c.itemOnCursor() == null) {
                                Item<?> item = this.plugin.itemManager().createWrappedItem(e.item().id(), player);
                                item.count(item.maxStackSize());
                                c.setItemOnCursor(item);
                                return;
                            }
                            if (LEFT_CLICK.contains(c.type())) {
                                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByResult(e.item().id());
                                if (inRecipes == recipes) return;
                                player.playSound(Constants.SOUND_CLICK_BUTTON);
                                if (!inRecipes.isEmpty()) {
                                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                                } else if (canOpenNoRecipePage) {
                                    openNoRecipePage(player, e.item().id(), e.gui(), 0);
                                }
                            } else if (RIGHT_CLICK.contains(c.type())) {
                                List<Recipe<Object>> inRecipes = this.plugin.recipeManager().recipeByIngredient(e.item().id());
                                if (inRecipes == recipes) return;
                                player.playSound(Constants.SOUND_CLICK_BUTTON);
                                if (!inRecipes.isEmpty()) {
                                    openRecipePage(c.clicker(), e.gui(), inRecipes, 0, depth + 1, canOpenNoRecipePage);
                                }
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
                .title(AdventureHelper.miniMessage().deserialize(Constants.RECIPE_CRAFTING_TITLE, PlayerOptionalContext.of(player).tagResolvers()))
                .refresh()
                .open(player);
    }
}
