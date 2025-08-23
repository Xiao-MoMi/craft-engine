package net.momirealms.craftengine.bukkit.plugin.injector;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.ComponentTypes;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.ItemTags;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.data.FireworkExplosion;
import net.momirealms.craftengine.core.util.*;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public final class RecipeInjector {
    private static Class<?> clazz$InjectedArmorDyeRecipe;
    private static Class<?> clazz$InjectedRepairItemRecipe;
    private static Class<?> clazz$InjectedFireworkStarFadeRecipe;

    public static void init() {
        ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V17);

        ElementMatcher.Junction<MethodDescription> matches = (VersionHelper.isOrAbove1_21() ?
                ElementMatchers.takesArguments(CoreReflections.clazz$CraftingInput, CoreReflections.clazz$Level) :
                ElementMatchers.takesArguments(CoreReflections.clazz$CraftingContainer, CoreReflections.clazz$Level)
        ).and(ElementMatchers.returns(boolean.class));
        ElementMatcher.Junction<MethodDescription> assemble = (
                VersionHelper.isOrAbove1_21() ?
                        ElementMatchers.takesArguments(CoreReflections.clazz$CraftingInput, CoreReflections.clazz$HolderLookup$Provider) :
                        VersionHelper.isOrAbove1_20_5() ?
                                ElementMatchers.takesArguments(CoreReflections.clazz$CraftingContainer, CoreReflections.clazz$HolderLookup$Provider) :
                                ElementMatchers.takesArguments(CoreReflections.clazz$CraftingContainer, CoreReflections.clazz$RegistryAccess)
        ).and(ElementMatchers.returns(CoreReflections.clazz$ItemStack));

        clazz$InjectedArmorDyeRecipe = byteBuddy
                .subclass(CoreReflections.clazz$ArmorDyeRecipe, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name("net.momirealms.craftengine.bukkit.item.recipe.ArmorDyeRecipe")
                .method(matches)
                .intercept(MethodDelegation.to(DyeMatchesInterceptor.INSTANCE))
                .method(assemble)
                .intercept(MethodDelegation.to(DyeAssembleInterceptor.INSTANCE))
                .make()
                .load(RecipeInjector.class.getClassLoader())
                .getLoaded();

        clazz$InjectedFireworkStarFadeRecipe = byteBuddy
                .subclass(CoreReflections.clazz$FireworkStarFadeRecipe)
                .name("net.momirealms.craftengine.bukkit.item.recipe.FireworkStarFadeRecipe")
                .method(matches)
                .intercept(MethodDelegation.to(FireworkStarFadeMatchesInterceptor.INSTANCE))
                .method(assemble)
                .intercept(MethodDelegation.to(FireworkStarFadeAssembleInterceptor.INSTANCE))
                .make()
                .load(RecipeInjector.class.getClassLoader())
                .getLoaded();

        clazz$InjectedRepairItemRecipe = byteBuddy
                .subclass(CoreReflections.clazz$RepairItemRecipe, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name("net.momirealms.craftengine.bukkit.item.recipe.RepairItemRecipe")
                // 只修改match逻辑，合并需要在事件里处理，否则无法应用变量
                .method(matches)
                .intercept(MethodDelegation.to(RepairMatchesInterceptor.INSTANCE))
                .make()
                .load(RecipeInjector.class.getClassLoader())
                .getLoaded();
    }

    public static Object createRepairItemRecipe(Key id) throws ReflectiveOperationException {
        return createSpecialRecipe(id, clazz$InjectedRepairItemRecipe);
    }

    public static Object createCustomDyeRecipe(Key id) throws ReflectiveOperationException {
        return createSpecialRecipe(id, clazz$InjectedArmorDyeRecipe);
    }

    public static Object createFireworkStarFadeRecipe(Key id) throws ReflectiveOperationException {
        return createSpecialRecipe(id, clazz$InjectedFireworkStarFadeRecipe);
    }

    @NotNull
    private static Object createSpecialRecipe(Key id, Class<?> clazz$InjectedRepairItemRecipe) throws InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {
        if (VersionHelper.isOrAbove1_20_2()) {
            Constructor<?> constructor = ReflectionUtils.getConstructor(clazz$InjectedRepairItemRecipe, CoreReflections.clazz$CraftingBookCategory);
            return constructor.newInstance(CoreReflections.instance$CraftingBookCategory$MISC);
        } else {
            Constructor<?> constructor = ReflectionUtils.getConstructor(clazz$InjectedRepairItemRecipe, CoreReflections.clazz$ResourceLocation, CoreReflections.clazz$CraftingBookCategory);
            return constructor.newInstance(KeyUtils.toResourceLocation(id), CoreReflections.instance$CraftingBookCategory$MISC);
        }
    }

    private static final Function<Object, Integer> INGREDIENT_SIZE_GETTER =
            VersionHelper.isOrAbove1_21() ?
                    FastNMS.INSTANCE::method$CraftingInput$size :
                    FastNMS.INSTANCE::method$Container$getContainerSize;
    private static final BiFunction<Object, Integer, Object> INGREDIENT_GETTER =
            VersionHelper.isOrAbove1_21() ?
                    FastNMS.INSTANCE::method$CraftingInput$getItem :
                    FastNMS.INSTANCE::method$Container$getItem;

    private static final Function<Object, Boolean> REPAIR_INGREDIENT_COUNT_CHECKER =
            VersionHelper.isOrAbove1_21() ?
                    (input) -> FastNMS.INSTANCE.method$CraftingInput$ingredientCount(input) != 2 :
                    (container) -> false;

    public static class FireworkStarFadeMatchesInterceptor {
        public static final FireworkStarFadeMatchesInterceptor INSTANCE = new FireworkStarFadeMatchesInterceptor();

        @RuntimeType
        public Object intercept(@AllArguments Object[] args) {
            Object input = args[0];
            if (DYE_INGREDIENT_COUNT_CHECKER.apply(input)) {
                return false;
            }
            boolean hasDye = false;
            boolean hasFireworkStar = false;
            int size = INGREDIENT_SIZE_GETTER.apply(input);
            for (int i = 0; i < size; i++) {
                Object itemStack = INGREDIENT_GETTER.apply(input, i);
                if (FastNMS.INSTANCE.method$ItemStack$isEmpty(itemStack)) {
                    continue;
                }
                Item<ItemStack> wrapped = BukkitItemManager.instance().wrap(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(itemStack));
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
        public Object intercept(@AllArguments Object[] args) {
            IntList colors = new IntArrayList();
            Item<ItemStack> starItem = null;
            Object input = args[0];
            int size = INGREDIENT_SIZE_GETTER.apply(input);
            for (int i = 0; i < size; i++) {
                Object itemStack = INGREDIENT_GETTER.apply(input, i);
                if (FastNMS.INSTANCE.method$ItemStack$isEmpty(itemStack)) {
                    continue;
                }
                Item<ItemStack> wrapped = BukkitItemManager.instance().wrap(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(itemStack));
                if (isFireworkDye(wrapped)) {
                    Color color = getFireworkColor(wrapped);
                    if (color == null) {
                        return CoreReflections.instance$ItemStack$EMPTY;
                    }
                    colors.add(color.color());
                } else if (wrapped.id().equals(ItemKeys.FIREWORK_STAR)) {
                    starItem = wrapped.copyWithCount(1);
                }
            }
            if (starItem == null || colors.isEmpty()) {
                return CoreReflections.instance$ItemStack$EMPTY;
            }
            FireworkExplosion explosion = starItem.fireworkExplosion().orElse(FireworkExplosion.DEFAULT);
            starItem.fireworkExplosion(explosion.withFadeColors(colors));
            return starItem.getLiteralObject();
        }
    }

    public static class RepairMatchesInterceptor {
        public static final RepairMatchesInterceptor INSTANCE = new RepairMatchesInterceptor();

        @RuntimeType
        public Object intercept(@AllArguments Object[] args) {
            Object input = args[0];
            if (REPAIR_INGREDIENT_COUNT_CHECKER.apply(input)) {
                return false;
            }
            return getItemsToCombine(input) != null;
        }
    }

    @Nullable
    private static Pair<Item<ItemStack>, Item<ItemStack>> getItemsToCombine(Object input) {
        Item<ItemStack> item1 = null;
        Item<ItemStack> item2 = null;
        int size = INGREDIENT_SIZE_GETTER.apply(input);
        for (int i = 0; i < size; i++) {
            Object itemStack = INGREDIENT_GETTER.apply(input, i);
            if (FastNMS.INSTANCE.method$ItemStack$isEmpty(itemStack)) {
                continue;
            }
            Item<ItemStack> wrapped = BukkitItemManager.instance().wrap(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(itemStack));
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

    private static boolean canCombine(Item<ItemStack> input1, Item<ItemStack> input2) {
        if (input1.count() != 1 || !isDamageableItem(input1)) return false;
        if (input2.count() != 1 || !isDamageableItem(input2)) return false;
        if (!input1.id().equals(input2.id())) return false;
        Optional<CustomItem<ItemStack>> customItem = input1.getCustomItem();
        return customItem.isEmpty() || customItem.get().settings().canRepair() != Tristate.FALSE;
    }

    private static boolean isDamageableItem(Item<ItemStack> item) {
        if (VersionHelper.isOrAbove1_20_5()) {
            return item.hasComponent(ComponentTypes.MAX_DAMAGE) && item.hasComponent(ComponentTypes.DAMAGE);
        } else {
            return FastNMS.INSTANCE.method$Item$canBeDepleted(FastNMS.INSTANCE.method$ItemStack$getItem(item.getLiteralObject()));
        }
    }

    private static final Function<Object, Boolean> DYE_INGREDIENT_COUNT_CHECKER =
            VersionHelper.isOrAbove1_21() ?
                    (input) -> FastNMS.INSTANCE.method$CraftingInput$ingredientCount(input) < 2 :
                    (container) -> false;

    public static class DyeMatchesInterceptor {
        public static final DyeMatchesInterceptor INSTANCE = new DyeMatchesInterceptor();

        @RuntimeType
        public Object intercept(@AllArguments Object[] args) {
            Object input = args[0];
            if (DYE_INGREDIENT_COUNT_CHECKER.apply(input)) {
                return false;
            }
            int size = INGREDIENT_SIZE_GETTER.apply(input);
            Item<ItemStack> itemToDye = null;
            boolean hasDye = false;
            for (int i = 0; i < size; i++) {
                Object itemStack = INGREDIENT_GETTER.apply(input, i);
                if (FastNMS.INSTANCE.method$ItemStack$isEmpty(itemStack)) {
                    continue;
                }
                Item<ItemStack> wrapped = BukkitItemManager.instance().wrap(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(itemStack));
                if (isDyeable(wrapped)) {
                    if (itemToDye != null) {
                        return false;
                    }
                    itemToDye = wrapped;
                } else {
                    if (!isArmorDye(wrapped)) {
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
        public Object intercept(@AllArguments Object[] args) {
            List<Color> colors = new ArrayList<>();
            Item<ItemStack> itemToDye = null;
            Object input = args[0];
            int size = INGREDIENT_SIZE_GETTER.apply(input);
            for (int i = 0; i < size; i++) {
                Object itemStack = INGREDIENT_GETTER.apply(input, i);
                if (FastNMS.INSTANCE.method$ItemStack$isEmpty(itemStack)) {
                    continue;
                }
                Item<ItemStack> wrapped = BukkitItemManager.instance().wrap(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(itemStack));
                if (isDyeable(wrapped)) {
                    itemToDye = wrapped.copyWithCount(1);
                } else {
                    Color dyeColor = getDyeColor(wrapped);
                    if (dyeColor != null) {
                        colors.add(dyeColor);
                    } else {
                        return CoreReflections.instance$ItemStack$EMPTY;
                    }
                }
            }
            if (itemToDye == null || itemToDye.isEmpty() || colors.isEmpty()) {
                return CoreReflections.instance$ItemStack$EMPTY;
            }
            return itemToDye.applyDyedColors(colors).getLiteralObject();
        }
    }

    @Nullable
    private static Color getDyeColor(final Item<ItemStack> dyeItem) {
        Optional<CustomItem<ItemStack>> optionalCustomItem = dyeItem.getCustomItem();
        if (optionalCustomItem.isPresent()) {
            CustomItem<ItemStack> customItem = optionalCustomItem.get();
            return Optional.ofNullable(customItem.settings().dyeColor()).orElseGet(() -> getVanillaDyeColor(dyeItem));
        }
        return getVanillaDyeColor(dyeItem);
    }

    @Nullable
    private static Color getFireworkColor(final Item<ItemStack> dyeItem) {
        Optional<CustomItem<ItemStack>> optionalCustomItem = dyeItem.getCustomItem();
        if (optionalCustomItem.isPresent()) {
            CustomItem<ItemStack> customItem = optionalCustomItem.get();
            return Optional.ofNullable(customItem.settings().fireworkColor()).orElseGet(() -> getVanillaFireworkColor(dyeItem));
        }
        return getVanillaFireworkColor(dyeItem);
    }

    private static final Predicate<Item<ItemStack>> IS_DYEABLE =
            VersionHelper.isOrAbove1_20_5() ?
                    (item -> item.hasItemTag(ItemTags.DYEABLE)) :
                    (item -> {
                       Object itemLike = FastNMS.INSTANCE.method$ItemStack$getItem(item.getLiteralObject());
                       return CoreReflections.clazz$DyeableLeatherItem.isInstance(itemLike);
                    });

    private static boolean isDyeable(final Item<ItemStack> item) {
        Optional<CustomItem<ItemStack>> optionalCustomItem = item.getCustomItem();
        if (optionalCustomItem.isPresent()) {
            CustomItem<ItemStack> customItem = optionalCustomItem.get();
            if (customItem.settings().dyeable() == Tristate.FALSE) {
                return false;
            }
            if (customItem.settings().dyeable() == Tristate.TRUE) {
                return true;
            }
        }
        return IS_DYEABLE.test(item);
    }

    @Nullable
    private static Color getVanillaDyeColor(final Item<ItemStack> item) {
        Object itemStack = item.getLiteralObject();
        Object dyeItem = FastNMS.INSTANCE.method$ItemStack$getItem(itemStack);
        if (!CoreReflections.clazz$DyeItem.isInstance(dyeItem)) return null;
        return Color.fromDecimal(FastNMS.INSTANCE.method$DyeColor$getTextureDiffuseColor(FastNMS.INSTANCE.method$DyeItem$getDyeColor(dyeItem)));
    }

    @Nullable
    private static Color getVanillaFireworkColor(final Item<ItemStack> item) {
        Object itemStack = item.getLiteralObject();
        Object dyeItem = FastNMS.INSTANCE.method$ItemStack$getItem(itemStack);
        if (!CoreReflections.clazz$DyeItem.isInstance(dyeItem)) return null;
        return Color.fromDecimal(FastNMS.INSTANCE.method$DyeColor$getFireworkColor(FastNMS.INSTANCE.method$DyeItem$getDyeColor(dyeItem)));
    }

    private static boolean isArmorDye(Item<ItemStack> dyeItem) {
        Optional<CustomItem<ItemStack>> optionalCustomItem = dyeItem.getCustomItem();
        if (optionalCustomItem.isPresent()) {
            CustomItem<ItemStack> customItem = optionalCustomItem.get();
            return customItem.settings().dyeColor() != null || isVanillaDyeItem(dyeItem);
        }
        return isVanillaDyeItem(dyeItem);
    }

    private static boolean isFireworkDye(Item<ItemStack> dyeItem) {
        Optional<CustomItem<ItemStack>> optionalCustomItem = dyeItem.getCustomItem();
        if (optionalCustomItem.isPresent()) {
            CustomItem<ItemStack> customItem = optionalCustomItem.get();
            return customItem.settings().fireworkColor() != null || isVanillaDyeItem(dyeItem);
        }
        return isVanillaDyeItem(dyeItem);
    }

    private static boolean isVanillaDyeItem(Item<ItemStack> item) {
        return CoreReflections.clazz$DyeItem.isInstance(FastNMS.INSTANCE.method$ItemStack$getItem(item.getLiteralObject()));
    }
}
