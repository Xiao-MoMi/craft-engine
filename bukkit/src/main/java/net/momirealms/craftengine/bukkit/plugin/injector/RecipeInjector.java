package net.momirealms.craftengine.bukkit.plugin.injector;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.bukkit.util.ItemTags;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.component.value.FireworkExplosion;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.proxy.minecraft.core.HolderLookupProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryAccessProxy;
import net.momirealms.craftengine.proxy.minecraft.network.codec.StreamCodecProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.world.ContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.CraftingContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.*;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.*;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public final class RecipeInjector {
    public static final Key ARMOR_DYE = Key.of("armor_dye");
    public static final Key REPAIR_ITEM = Key.of("repair_item");
    public static final Key FIREWORK_STAR_FADE = Key.of("firework_star_fade");
    public static Object ARMOR_DYE_RECIPE;  // 1.21.11-
    public static Object FIREWORK_STAR_FADE_RECIPE;  // 1.21.11-
    public static Object REPAIR_ITEM_RECIPE;
    public static Constructor<?> FIREWORK_STAR_FADE_RECIPE_CONSTRUCTOR; // 26.1+

    private RecipeInjector() {}

    public static void init() throws ReflectiveOperationException {
        ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V17);

        ElementMatcher.Junction<MethodDescription> matches = (VersionHelper.isOrAbove1_21 ?
                ElementMatchers.takesArguments(CraftingInputProxy.CLASS, LevelProxy.CLASS) :
                ElementMatchers.takesArguments(CraftingContainerProxy.CLASS, LevelProxy.CLASS)
        ).and(ElementMatchers.returns(boolean.class));
        ElementMatcher.Junction<MethodDescription> assemble = (
                VersionHelper.isOrAbove26_1 ?
                ElementMatchers.takesArguments(CraftingInputProxy.CLASS) :
                VersionHelper.isOrAbove1_21 ?
                ElementMatchers.takesArguments(CraftingInputProxy.CLASS, HolderLookupProxy.ProviderProxy.CLASS) :
                VersionHelper.isOrAbove1_20_5 ?
                ElementMatchers.takesArguments(CraftingContainerProxy.CLASS, HolderLookupProxy.ProviderProxy.CLASS) :
                ElementMatchers.takesArguments(CraftingContainerProxy.CLASS, RegistryAccessProxy.CLASS)
        ).and(ElementMatchers.returns(ItemStackProxy.CLASS));

        if (VersionHelper.isOrAbove26_1) {
            Class<?> clazz$InjectedRepairItemRecipe = byteBuddy
                    .subclass(RepairItemRecipeProxy.CLASS, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                    .name("net.momirealms.craftengine.bukkit.item.recipe.RepairItemRecipe")
                    .defineField("serializer", RecipeSerializerProxy.CLASS, Modifier.PUBLIC)
                    .method(matches)
                    .intercept(MethodDelegation.to(RepairMatchesInterceptor.INSTANCE))
                    .method(ElementMatchers.named("getSerializer"))
                    .intercept(FieldAccessor.ofField("serializer"))
                    .make()
                    .load(RecipeInjector.class.getClassLoader())
                    .getLoaded();
            REPAIR_ITEM_RECIPE = createSpecialRecipe(REPAIR_ITEM, clazz$InjectedRepairItemRecipe);
            clazz$InjectedRepairItemRecipe.getField("serializer")
                    .set(REPAIR_ITEM_RECIPE, RecipeSerializerProxy.INSTANCE.newInstance(MapCodec.unit(REPAIR_ITEM_RECIPE), StreamCodecProxy.INSTANCE.unit(REPAIR_ITEM_RECIPE)));
        } else {
            Class<?> clazz$InjectedRepairItemRecipe = byteBuddy
                    .subclass(RepairItemRecipeProxy.CLASS, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                    .name("net.momirealms.craftengine.bukkit.item.recipe.RepairItemRecipe")
                    // 只修改match逻辑，合并需要在事件里处理，否则无法应用变量
                    .method(matches)
                    .intercept(MethodDelegation.to(RepairMatchesInterceptor.INSTANCE))
                    .make()
                    .load(RecipeInjector.class.getClassLoader())
                    .getLoaded();
            REPAIR_ITEM_RECIPE = createSpecialRecipe(REPAIR_ITEM, clazz$InjectedRepairItemRecipe);
        }

        // 26.1 以上的染色配方直接注册，无需特殊配方
        if (!VersionHelper.isOrAbove26_1) {
            Class<?> clazz$InjectedArmorDyeRecipe = byteBuddy
                    .subclass(ArmorDyeRecipeProxy.CLASS, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                    .name("net.momirealms.craftengine.bukkit.item.recipe.DyeRecipe")
                    .method(matches)
                    .intercept(MethodDelegation.to(DyeMatchesInterceptor.INSTANCE))
                    .method(assemble)
                    .intercept(MethodDelegation.to(DyeAssembleInterceptor.INSTANCE))
                    .make()
                    .load(RecipeInjector.class.getClassLoader())
                    .getLoaded();
            ARMOR_DYE_RECIPE = createSpecialRecipe(ARMOR_DYE, clazz$InjectedArmorDyeRecipe);

            Class<?> clazz$InjectedFireworkStarFadeRecipe = byteBuddy
                    .subclass(FireworkStarFadeRecipeProxy.CLASS)
                    .name("net.momirealms.craftengine.bukkit.item.recipe.FireworkStarFadeRecipe")
                    .method(matches)
                    .intercept(MethodDelegation.to(FireworkStarFadeMatchesInterceptor.INSTANCE))
                    .method(assemble)
                    .intercept(MethodDelegation.to(FireworkStarFadeAssembleInterceptor.INSTANCE))
                    .make()
                    .load(RecipeInjector.class.getClassLoader())
                    .getLoaded();
            FIREWORK_STAR_FADE_RECIPE = createSpecialRecipe(FIREWORK_STAR_FADE, clazz$InjectedFireworkStarFadeRecipe);
        }

        if (VersionHelper.isOrAbove26_1) {
            Class<?> clazz$InjectedFireworkStarFadeRecipe = byteBuddy
                    .subclass(FireworkStarFadeRecipeProxy.CLASS)
                    .name("net.momirealms.craftengine.bukkit.item.recipe.FireworkStarFadeRecipe")
                    .method(matches)
                    .intercept(MethodDelegation.to(FireworkStarFadeMatchesInterceptor.INSTANCE))
                    .method(assemble)
                    .intercept(MethodDelegation.to(FireworkStarFadeAssembleInterceptor.INSTANCE))
                    .make()
                    .load(RecipeInjector.class.getClassLoader())
                    .getLoaded();
            FIREWORK_STAR_FADE_RECIPE_CONSTRUCTOR = SparrowClass.of(clazz$InjectedFireworkStarFadeRecipe)
                    .getConstructor(ConstructorMatcher.takeArguments(IngredientProxy.CLASS, IngredientProxy.CLASS, ItemStackTemplateProxy.CLASS));
        }
    }

    @NotNull
    private static Object createSpecialRecipe(Key id, Class<?> clazz) throws InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {
        if (VersionHelper.isOrAbove26_1) {
            Constructor<?> constructor = ReflectionUtils.getConstructor(clazz);
            assert constructor != null;
            return constructor.newInstance();
        } else if (VersionHelper.isOrAbove1_20_2) {
            Constructor<?> constructor = ReflectionUtils.getConstructor(clazz, CraftingBookCategoryProxy.CLASS);
            assert constructor != null;
            return constructor.newInstance(CraftingBookCategoryProxy.MISC);
        } else {
            Constructor<?> constructor = ReflectionUtils.getConstructor(clazz, IdentifierProxy.CLASS, CraftingBookCategoryProxy.CLASS);
            assert constructor != null;
            return constructor.newInstance(KeyUtils.toIdentifier(id), CraftingBookCategoryProxy.MISC);
        }
    }

    private static final Function<Object, Integer> INGREDIENT_SIZE_GETTER =
            VersionHelper.isOrAbove1_21 ?
                    CraftingInputProxy.INSTANCE::size :
                    ContainerProxy.INSTANCE::getContainerSize;
    private static final BiFunction<Object, Integer, Object> INGREDIENT_GETTER =
            VersionHelper.isOrAbove1_21 ?
                    CraftingInputProxy.INSTANCE::getItem :
                    ContainerProxy.INSTANCE::getItem;

    private static final Function<Object, Boolean> REPAIR_INGREDIENT_COUNT_CHECKER =
            VersionHelper.isOrAbove1_21 ?
                    (input) -> CraftingInputProxy.INSTANCE.ingredientCount(input) != 2 :
                    (container) -> false;

    public static class FireworkStarFadeMatchesInterceptor {
        public static final FireworkStarFadeMatchesInterceptor INSTANCE = new FireworkStarFadeMatchesInterceptor();

        @RuntimeType
        public Object intercept(@Argument(value = 0) Object input) {
            if (DYE_INGREDIENT_COUNT_CHECKER.apply(input)) {
                return false;
            }
            boolean hasDye = false;
            boolean hasFireworkStar = false;
            int size = INGREDIENT_SIZE_GETTER.apply(input);
            for (int i = 0; i < size; i++) {
                Object itemStack = INGREDIENT_GETTER.apply(input, i);
                if (ItemStackProxy.INSTANCE.isEmpty(itemStack)) {
                    continue;
                }
                Item wrapped = BukkitItemManager.instance().wrap(itemStack);
                if (isFireworkDye(wrapped)) {
                    hasDye = true;
                } else {
                    if (!wrapped.id().equals(ItemKeys.FIREWORK_STAR)) {
                        return false;
                    }
                    if (hasFireworkStar) {
                        return false;
                    }
                    hasFireworkStar = true;
                }
            }
            return hasDye && hasFireworkStar;
        }
    }

    public static class FireworkStarFadeAssembleInterceptor {
        public static final FireworkStarFadeAssembleInterceptor INSTANCE = new FireworkStarFadeAssembleInterceptor();

        @RuntimeType
        public Object intercept(@Argument(value = 0) Object input) {
            IntList colors = new IntArrayList();
            Item starItem = null;
            int size = INGREDIENT_SIZE_GETTER.apply(input);
            for (int i = 0; i < size; i++) {
                Object itemStack = INGREDIENT_GETTER.apply(input, i);
                if (ItemStackProxy.INSTANCE.isEmpty(itemStack)) {
                    continue;
                }
                Item wrapped = BukkitItemManager.instance().wrap(itemStack);
                if (isFireworkDye(wrapped)) {
                    Color color = getFireworkColor(wrapped);
                    if (color == null) {
                        return ItemStackProxy.EMPTY;
                    }
                    colors.add(color.color());
                } else if (wrapped.id().equals(ItemKeys.FIREWORK_STAR)) {
                    starItem = wrapped.copyWithCount(1);
                }
            }
            if (starItem == null || colors.isEmpty()) {
                return ItemStackProxy.EMPTY;
            }
            FireworkExplosion explosion = starItem.fireworkExplosion().orElse(FireworkExplosion.DEFAULT);
            starItem.fireworkExplosion(explosion.withFadeColors(colors));
            return starItem.minecraftItem();
        }
    }

    public static class RepairMatchesInterceptor {
        public static final RepairMatchesInterceptor INSTANCE = new RepairMatchesInterceptor();

        @RuntimeType
        public Object intercept(@Argument(value = 0) Object input) {
            if (REPAIR_INGREDIENT_COUNT_CHECKER.apply(input)) {
                return false;
            }
            return getItemsToCombine(input) != null;
        }
    }

    @Nullable
    private static Pair<Item, Item> getItemsToCombine(Object input) {
        Item item1 = null;
        Item item2 = null;
        int size = INGREDIENT_SIZE_GETTER.apply(input);
        for (int i = 0; i < size; i++) {
            Object itemStack = INGREDIENT_GETTER.apply(input, i);
            if (ItemStackProxy.INSTANCE.isEmpty(itemStack)) {
                continue;
            }
            Item wrapped = BukkitItemManager.instance().wrap(itemStack);
            if (item1 == null) {
                item1 = wrapped;
            } else {
                if (item2 != null) {
                    return null;
                }
                item2 = wrapped;
            }
        }
        if (item1 == null || item2 == null) {
            return null;
        }
        if (!canCombine(item1, item2)) {
            return null;
        }
        return new Pair<>(item1, item2);
    }

    private static boolean canCombine(Item input1, Item input2) {
        if (input1.count() != 1 || !isDamageableItem(input1)) return false;
        if (input2.count() != 1 || !isDamageableItem(input2)) return false;
        if (!input1.id().equals(input2.id())) return false;
        Optional<ItemDefinition> customItem = input1.getDefinition();
        return customItem.isEmpty() || customItem.get().settings().repairable().craftingTable() != Tristate.FALSE;
    }

    private static boolean isDamageableItem(Item item) {
        if (VersionHelper.isOrAbove1_20_5) {
            return item.hasComponent(DataComponentTypes.MAX_DAMAGE) && item.hasComponent(DataComponentTypes.DAMAGE);
        } else {
            return ItemProxy.INSTANCE.canBeDepleted(ItemStackProxy.INSTANCE.getItem(item.minecraftItem()));
        }
    }

    private static final Function<Object, Boolean> DYE_INGREDIENT_COUNT_CHECKER =
            VersionHelper.isOrAbove1_21 ?
                    (input) -> CraftingInputProxy.INSTANCE.ingredientCount(input) < 2 :
                    (container) -> false;

    public static class DyeMatchesInterceptor {
        public static final DyeMatchesInterceptor INSTANCE = new DyeMatchesInterceptor();

        @RuntimeType
        public Object intercept(@Argument(value = 0) Object input) {
            if (DYE_INGREDIENT_COUNT_CHECKER.apply(input)) {
                return false;
            }
            int size = INGREDIENT_SIZE_GETTER.apply(input);
            Item itemToDye = null;
            boolean hasDye = false;
            for (int i = 0; i < size; i++) {
                Object itemStack = INGREDIENT_GETTER.apply(input, i);
                if (ItemStackProxy.INSTANCE.isEmpty(itemStack)) {
                    continue;
                }
                Item wrapped = BukkitItemManager.instance().wrap(itemStack);
                if (isDyeable(wrapped)) {
                    if (itemToDye != null) {
                        return false;
                    }
                    itemToDye = wrapped;
                } else {
                    if (!isDye(wrapped)) {
                        return false;
                    }
                    hasDye = true;
                }
            }
            return hasDye && itemToDye != null;
        }
    }

    public static class DyeAssembleInterceptor {
        public static final DyeAssembleInterceptor INSTANCE = new DyeAssembleInterceptor();

        @RuntimeType
        public Object intercept(@Argument(value = 0) Object input) {
            List<Color> colors = new ArrayList<>();
            Item itemToDye = null;
            int size = INGREDIENT_SIZE_GETTER.apply(input);
            for (int i = 0; i < size; i++) {
                Object itemStack = INGREDIENT_GETTER.apply(input, i);
                if (ItemStackProxy.INSTANCE.isEmpty(itemStack)) {
                    continue;
                }
                Item wrapped = BukkitItemManager.instance().wrap(itemStack);
                if (isDyeable(wrapped)) {
                    itemToDye = wrapped.copyWithCount(1);
                } else {
                    Color dyeColor = getDyeColor(wrapped);
                    if (dyeColor != null) {
                        colors.add(dyeColor);
                    } else {
                        return ItemStackProxy.EMPTY;
                    }
                }
            }
            if (itemToDye == null || itemToDye.isEmpty() || colors.isEmpty()) {
                return ItemStackProxy.EMPTY;
            }
            return itemToDye.applyDyedColors(colors).minecraftItem();
        }
    }

    @Nullable
    private static Color getDyeColor(final Item dyeItem) {
        Optional<ItemDefinition> optionalCustomItem = dyeItem.getDefinition();
        if (optionalCustomItem.isPresent()) {
            ItemDefinition itemDefinition = optionalCustomItem.get();
            return Optional.ofNullable(itemDefinition.settings().dyeColor()).orElseGet(() -> getVanillaDyeColor(dyeItem));
        }
        return getVanillaDyeColor(dyeItem);
    }

    @Nullable
    private static Color getFireworkColor(final Item dyeItem) {
        Optional<ItemDefinition> optionalCustomItem = dyeItem.getDefinition();
        if (optionalCustomItem.isPresent()) {
            ItemDefinition itemDefinition = optionalCustomItem.get();
            return Optional.ofNullable(itemDefinition.settings().fireworkColor()).orElseGet(() -> getVanillaFireworkColor(dyeItem));
        }
        return getVanillaFireworkColor(dyeItem);
    }

    private static final Predicate<Item> IS_DYEABLE =
            VersionHelper.isOrAbove1_20_5 ?
                    (item -> item.hasItemTag(ItemTags.DYEABLE)) :
                    (item -> {
                       Object itemLike = ItemStackProxy.INSTANCE.getItem(item.minecraftItem());
                       return DyeableLeatherItemProxy.CLASS.isInstance(itemLike);
                    });

    private static boolean isDyeable(final Item item) {
        Optional<ItemDefinition> optionalCustomItem = item.getDefinition();
        if (optionalCustomItem.isPresent()) {
            ItemDefinition itemDefinition = optionalCustomItem.get();
            if (itemDefinition.settings().dyeable() == Tristate.FALSE) {
                return false;
            }
            if (itemDefinition.settings().dyeable() == Tristate.TRUE) {
                return true;
            }
        }
        return IS_DYEABLE.test(item);
    }

    @SuppressWarnings("PointlessBitwiseExpression")
    @Nullable
    private static Color getVanillaDyeColor(final Item item) {
        Object itemStack = item.minecraftItem();
        Object dyeItem = ItemStackProxy.INSTANCE.getItem(itemStack);
        if (!DyeItemProxy.CLASS.isInstance(dyeItem)) return null;
        Object dyeColor = DyeItemProxy.INSTANCE.getDyeColor(dyeItem);
        int textureDiffuseColor;
        if (VersionHelper.isOrAbove1_21) {
            textureDiffuseColor = DyeColorProxy.INSTANCE.getTextureDiffuseColor(dyeColor);
        } else {
            float[] rgb = DyeColorProxy.INSTANCE.getTextureDiffuseColors(dyeColor);
            int r = (int) (rgb[0] * 255.0F);
            int g = (int) (rgb[1] * 255.0F);
            int b = (int) (rgb[2] * 255.0F);
            textureDiffuseColor = 0 << 24 /*不可省略*/ | r << 16 | g << 8 | b;
        }
        return Color.fromDecimal(textureDiffuseColor);
    }

    @Nullable
    private static Color getVanillaFireworkColor(final Item item) {
        if (VersionHelper.isOrAbove26_1) {
            String colorType = (String) item.getComponentAsJava(DataComponentKeys.DYE);
            if (colorType == null) {
                return null;
            }
            return new Color(DyeColor.valueOf(colorType.toUpperCase(Locale.ROOT)).fireworkColor());
        } else {
            Object itemStack = item.minecraftItem();
            Object dyeItem = ItemStackProxy.INSTANCE.getItem(itemStack);
            if (!DyeItemProxy.CLASS.isInstance(dyeItem)) return null;
            return Color.fromDecimal(DyeColorProxy.INSTANCE.getFireworkColor(DyeItemProxy.INSTANCE.getDyeColor(dyeItem)));
        }
    }

    private static boolean isDye(Item dyeItem) {
        Optional<ItemDefinition> optionalCustomItem = dyeItem.getDefinition();
        if (optionalCustomItem.isPresent()) {
            ItemDefinition itemDefinition = optionalCustomItem.get();
            return itemDefinition.settings().dyeColor() != null || isVanillaDyeItem(dyeItem);
        }
        return isVanillaDyeItem(dyeItem);
    }

    private static boolean isFireworkDye(Item dyeItem) {
        Optional<ItemDefinition> optionalCustomItem = dyeItem.getDefinition();
        if (optionalCustomItem.isPresent()) {
            ItemDefinition itemDefinition = optionalCustomItem.get();
            return itemDefinition.settings().fireworkColor() != null || isVanillaDyeItem(dyeItem);
        }
        return isVanillaDyeItem(dyeItem);
    }

    private static boolean isVanillaDyeItem(Item item) {
        return DyeItemProxy.CLASS.isInstance(ItemStackProxy.INSTANCE.getItem(item.minecraftItem()));
    }
}
