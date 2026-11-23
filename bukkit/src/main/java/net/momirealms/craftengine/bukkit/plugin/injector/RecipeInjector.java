package net.momirealms.craftengine.bukkit.plugin.injector;

import com.mojang.serialization.MapCodec;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.HolderLookupProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryAccessProxy;
import net.momirealms.craftengine.proxy.minecraft.network.codec.StreamCodecProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.CraftingContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackTemplateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.*;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static java.util.Objects.requireNonNull;

public final class RecipeInjector {
    public static final Key ARMOR_DYE = Key.of("armor_dye");
    public static final Key REPAIR_ITEM = Key.of("repair_item");
    public static final Key FIREWORK_STAR_FADE = Key.of("firework_star_fade");
    public static Object ARMOR_DYE_RECIPE;  // 1.21.11-
    public static Object FIREWORK_STAR_FADE_RECIPE;  // 1.21.11-
    public static Object REPAIR_ITEM_RECIPE;
    public static Constructor<?> FIREWORK_STAR_FADE_RECIPE_CONSTRUCTOR; // 26.1+

    private RecipeInjector() {
    }

    public static void init() throws ReflectiveOperationException {
        ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V21);
        Class<?> input = VersionHelper.isOrAbove1_21 ? CraftingInputProxy.CLASS : CraftingContainerProxy.CLASS;
        ElementMatcher.Junction<MethodDescription> matches = ElementMatchers.takesArguments(input, LevelProxy.CLASS)
                .and(ElementMatchers.returns(boolean.class));
        ElementMatcher.Junction<MethodDescription> assemble = (VersionHelper.isOrAbove26_1
                ? ElementMatchers.takesArguments(input)
                : ElementMatchers.takesArguments(input, VersionHelper.isOrAbove1_20_5 ? HolderLookupProxy.ProviderProxy.CLASS : RegistryAccessProxy.CLASS))
                .and(ElementMatchers.returns(ItemStackProxy.CLASS));

        DynamicType.Builder<?> repair = byteBuddy.subclass(RepairItemRecipeProxy.CLASS, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name("net.momirealms.craftengine.bukkit.item.recipe.RepairItemRecipe")
                .method(matches).intercept(delegate("matchesRepair"));
        if (VersionHelper.isOrAbove26_1) {
            repair = repair.defineField("serializer", RecipeSerializerProxy.CLASS, Modifier.PUBLIC)
                    .method(ElementMatchers.named("getSerializer")).intercept(FieldAccessor.ofField("serializer"));
        }
        Class<?> repairType = load(repair);
        REPAIR_ITEM_RECIPE = createSpecialRecipe(REPAIR_ITEM, repairType);
        if (VersionHelper.isOrAbove26_1) {
            repairType.getField("serializer").set(REPAIR_ITEM_RECIPE,
                    RecipeSerializerProxy.INSTANCE.newInstance(MapCodec.unit(REPAIR_ITEM_RECIPE), StreamCodecProxy.INSTANCE.unit(REPAIR_ITEM_RECIPE)));
        } else {
            Class<?> dyeType = load(byteBuddy.subclass(ArmorDyeRecipeProxy.CLASS, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                    .name("net.momirealms.craftengine.bukkit.item.recipe.DyeRecipe")
                    .method(matches).intercept(delegate("matchesDye"))
                    .method(assemble).intercept(delegate("assembleDye")));
            ARMOR_DYE_RECIPE = createSpecialRecipe(ARMOR_DYE, dyeType);
        }

        Class<?> fadeType = load(byteBuddy.subclass(FireworkStarFadeRecipeProxy.CLASS)
                .name("net.momirealms.craftengine.bukkit.item.recipe.FireworkStarFadeRecipe")
                .method(matches).intercept(delegate("matchesFireworkFade"))
                .method(assemble).intercept(delegate("assembleFireworkFade")));
        if (VersionHelper.isOrAbove26_1) {
            FIREWORK_STAR_FADE_RECIPE_CONSTRUCTOR = requireNonNull(SparrowClass.of(fadeType)
                    .getConstructor(ConstructorMatcher.takeArguments(IngredientProxy.CLASS, IngredientProxy.CLASS, ItemStackTemplateProxy.CLASS)));
        } else {
            FIREWORK_STAR_FADE_RECIPE = createSpecialRecipe(FIREWORK_STAR_FADE, fadeType);
        }
    }

    private static MethodDelegation delegate(String method) {
        return MethodDelegation.withDefaultConfiguration().filter(ElementMatchers.named(method)).to(SpecialRecipeMethods.class);
    }

    private static Class<?> load(DynamicType.Builder<?> builder) {
        return builder.make().load(RecipeInjector.class.getClassLoader()).getLoaded();
    }

    private static Object createSpecialRecipe(Key id, Class<?> type) throws ReflectiveOperationException {
        if (VersionHelper.isOrAbove26_1) {
            return type.getConstructor().newInstance();
        }
        if (VersionHelper.isOrAbove1_20_2) {
            return type.getConstructor(CraftingBookCategoryProxy.CLASS).newInstance(CraftingBookCategoryProxy.MISC);
        }
        return type.getConstructor(IdentifierProxy.CLASS, CraftingBookCategoryProxy.CLASS)
                .newInstance(KeyUtils.toIdentifier(id), CraftingBookCategoryProxy.MISC);
    }
}
