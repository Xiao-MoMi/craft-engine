package net.momirealms.craftengine.bukkit.plugin.injector;

import com.mojang.datafixers.util.Pair;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.recipe.BukkitRecipeManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRecipeTypes;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.recipe.CustomCookingRecipe;
import net.momirealms.craftengine.core.item.recipe.OptimizedIDItem;
import net.momirealms.craftengine.core.item.recipe.RecipeTypes;
import net.momirealms.craftengine.core.item.recipe.input.SingleItemInput;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class RecipeInjector {
    private static Class<?> clazz$InjectedCacheChecker;
    
    public static void init() {
        ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V17);
        clazz$InjectedCacheChecker = byteBuddy
                .subclass(Object.class, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name("net.momirealms.craftengine.bukkit.entity.InjectedCacheChecker")
                .implement(CoreReflections.clazz$RecipeManager$CachedCheck)
                .implement(InjectedCacheCheck.class)

                .defineField("recipeType", Object.class, Visibility.PUBLIC)
                .method(ElementMatchers.named("recipeType"))
                .intercept(FieldAccessor.ofField("recipeType"))

                .defineField("customRecipeType", Key.class, Visibility.PUBLIC)
                .method(ElementMatchers.named("customRecipeType"))
                .intercept(FieldAccessor.ofField("customRecipeType"))

                .defineField("lastRecipe", Object.class, Visibility.PUBLIC)
                .method(ElementMatchers.named("lastRecipe"))
                .intercept(FieldAccessor.ofField("lastRecipe"))

                .defineField("lastCustomRecipe", Key.class, Visibility.PUBLIC)
                .method(ElementMatchers.named("lastCustomRecipe"))
                .intercept(FieldAccessor.ofField("lastCustomRecipe"))

                .method(ElementMatchers.named("getRecipeFor").or(ElementMatchers.named("a")))
                .intercept(MethodDelegation.to(
                        VersionHelper.isOrAbove1_21_2() ?
                                GetRecipeForMethodInterceptor1_21_2.INSTANCE :
                                (VersionHelper.isOrAbove1_21() ?
                                        GetRecipeForMethodInterceptor1_21.INSTANCE :
                                        VersionHelper.isOrAbove1_20_5() ?
                                                GetRecipeForMethodInterceptor1_20_5.INSTANCE :
                                                GetRecipeForMethodInterceptor1_20.INSTANCE)
                ))
                .make()
                .load(RecipeInjector.class.getClassLoader())
                .getLoaded();
    }

    public static void injectCookingBlockEntity(Object entity) throws ReflectiveOperationException {
        if (CoreReflections.clazz$AbstractFurnaceBlockEntity.isInstance(entity)) {
            Object quickCheck = CoreReflections.field$AbstractFurnaceBlockEntity$quickCheck.get(entity);
            if (clazz$InjectedCacheChecker.isInstance(quickCheck)) return; // already injected
            Object recipeType = FastNMS.INSTANCE.field$AbstractFurnaceBlockEntity$recipeType(entity);
            InjectedCacheCheck injectedChecker = (InjectedCacheCheck) ReflectionUtils.UNSAFE.allocateInstance(clazz$InjectedCacheChecker);
            if (recipeType == MRecipeTypes.SMELTING) {
                injectedChecker.customRecipeType(RecipeTypes.SMELTING);
                injectedChecker.recipeType(MRecipeTypes.SMELTING);
            } else if (recipeType == MRecipeTypes.BLASTING) {
                injectedChecker.customRecipeType(RecipeTypes.BLASTING);
                injectedChecker.recipeType(MRecipeTypes.BLASTING);
            } else if (recipeType == MRecipeTypes.SMOKING) {
                injectedChecker.customRecipeType(RecipeTypes.SMOKING);
                injectedChecker.recipeType(MRecipeTypes.SMOKING);
            } else {
                throw new IllegalStateException("RecipeType " + recipeType + " not supported");
            }
            CoreReflections.field$AbstractFurnaceBlockEntity$quickCheck.set(entity, injectedChecker);
        } else if (!VersionHelper.isOrAbove1_21_2() && CoreReflections.clazz$CampfireBlockEntity.isInstance(entity)) {
            Object quickCheck = CoreReflections.field$CampfireBlockEntity$quickCheck.get(entity);
            if (clazz$InjectedCacheChecker.isInstance(quickCheck)) return; // already injected
            InjectedCacheCheck injectedChecker = (InjectedCacheCheck) ReflectionUtils.UNSAFE.allocateInstance(clazz$InjectedCacheChecker);
            injectedChecker.customRecipeType(RecipeTypes.CAMPFIRE_COOKING);
            injectedChecker.recipeType(MRecipeTypes.CAMPFIRE_COOKING);
            CoreReflections.field$CampfireBlockEntity$quickCheck.set(entity, injectedChecker);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public static class GetRecipeForMethodInterceptor1_20 {
        public static final GetRecipeForMethodInterceptor1_20 INSTANCE = new GetRecipeForMethodInterceptor1_20();

        @SuppressWarnings("unchecked")
        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args) {
            InjectedCacheCheck injectedCacheCheck = (InjectedCacheCheck) thisObj;
            Object lastRecipeResourceLocation = injectedCacheCheck.lastRecipe();
            Optional<Pair<Object, Object>> optionalRecipe = FastNMS.INSTANCE.method$RecipeManager$getRecipeFor(BukkitRecipeManager.nmsRecipeManager(), injectedCacheCheck.recipeType(), args[0], args[1], lastRecipeResourceLocation);
            if (optionalRecipe.isEmpty()) {
                return Optional.empty();
            }

            Pair<Object, Object> resourceLocationAndRecipe = optionalRecipe.get();
            Object rawRecipeResourceLocation = resourceLocationAndRecipe.getFirst();
            Key rawRecipeKey = Key.of(rawRecipeResourceLocation.toString());
            BukkitRecipeManager recipeManager = BukkitRecipeManager.instance();

            boolean isCustom = recipeManager.isCustomRecipe(rawRecipeKey);
            if (!isCustom) {
                injectedCacheCheck.lastRecipe(rawRecipeResourceLocation);
                return Optional.of(resourceLocationAndRecipe.getSecond());
            }

            ItemStack itemStack = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(
                    injectedCacheCheck.recipeType() == MRecipeTypes.CAMPFIRE_COOKING ?
                            FastNMS.INSTANCE.field$SimpleContainer$items(args[0]).getFirst() :
                            FastNMS.INSTANCE.field$AbstractFurnaceBlockEntity$getItem(args[0], 0)
            );

            Item<ItemStack> wrappedItem = BukkitItemManager.instance().wrap(itemStack);
            SingleItemInput<ItemStack> input = new SingleItemInput<>(new OptimizedIDItem<>(wrappedItem.recipeIngredientId(), itemStack));
            CustomCookingRecipe<ItemStack> ceRecipe = (CustomCookingRecipe<ItemStack>) recipeManager.recipeByInput(injectedCacheCheck.customRecipeType(), input, injectedCacheCheck.lastCustomRecipe());
            if (ceRecipe == null) {
                return Optional.empty();
            }

            injectedCacheCheck.lastCustomRecipe(ceRecipe.id());
            if (!ceRecipe.id().equals(rawRecipeKey)) {
                injectedCacheCheck.lastRecipe(KeyUtils.toResourceLocation(ceRecipe.id()));
            }
            return Optional.ofNullable(recipeManager.nmsRecipeHolderByRecipe(ceRecipe));
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public static class GetRecipeForMethodInterceptor1_20_5 {
        public static final GetRecipeForMethodInterceptor1_20_5 INSTANCE = new GetRecipeForMethodInterceptor1_20_5();

        @SuppressWarnings("unchecked")
        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args) {
            InjectedCacheCheck injectedCacheCheck = (InjectedCacheCheck) thisObj;
            Object lastRecipeResourceLocation = injectedCacheCheck.lastRecipe();
            Optional<Object> optionalRecipe = (Optional<Object>) FastNMS.INSTANCE.method$RecipeManager$getRecipeFor(BukkitRecipeManager.nmsRecipeManager(), injectedCacheCheck.recipeType(), args[0], args[1], lastRecipeResourceLocation);
            if (optionalRecipe.isEmpty()) {
                return Optional.empty();
            }

            Object rawRecipeHolder = optionalRecipe.get();
            Object rawRecipeResourceLocation = FastNMS.INSTANCE.field$RecipeHolder$id(rawRecipeHolder);
            Key rawRecipeKey = Key.of(rawRecipeResourceLocation.toString());

            BukkitRecipeManager recipeManager = BukkitRecipeManager.instance();
            ItemStack itemStack = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(
                    injectedCacheCheck.recipeType() == MRecipeTypes.CAMPFIRE_COOKING ?
                            FastNMS.INSTANCE.field$SimpleContainer$items(args[0]).getFirst() :
                            FastNMS.INSTANCE.field$AbstractFurnaceBlockEntity$getItem(args[0], 0)
            );

            boolean isCustom = recipeManager.isCustomRecipe(rawRecipeKey);
            if (!isCustom) {
                injectedCacheCheck.lastRecipe(rawRecipeResourceLocation);
                return optionalRecipe;
            }

            Item<ItemStack> wrappedItem = BukkitItemManager.instance().wrap(itemStack);
            SingleItemInput<ItemStack> input = new SingleItemInput<>(new OptimizedIDItem<>(wrappedItem.recipeIngredientId(), itemStack));
            CustomCookingRecipe<ItemStack> ceRecipe = (CustomCookingRecipe<ItemStack>) recipeManager.recipeByInput(injectedCacheCheck.customRecipeType(), input, injectedCacheCheck.lastCustomRecipe());
            if (ceRecipe == null) {
                return Optional.empty();
            }

            injectedCacheCheck.lastCustomRecipe(ceRecipe.id());
            if (!ceRecipe.id().equals(rawRecipeKey)) {
                injectedCacheCheck.lastRecipe(KeyUtils.toResourceLocation(ceRecipe.id()));
            }
            return Optional.ofNullable(recipeManager.nmsRecipeHolderByRecipe(ceRecipe));
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public static class GetRecipeForMethodInterceptor1_21 {
        public static final GetRecipeForMethodInterceptor1_21 INSTANCE = new GetRecipeForMethodInterceptor1_21();

        @SuppressWarnings("unchecked")
        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args) {
            InjectedCacheCheck injectedCacheCheck = (InjectedCacheCheck) thisObj;
            Object lastRecipeResourceLocation = injectedCacheCheck.lastRecipe();
            Optional<Object> optionalRecipe = (Optional<Object>) FastNMS.INSTANCE.method$RecipeManager$getRecipeFor(BukkitRecipeManager.nmsRecipeManager(), injectedCacheCheck.recipeType(), args[0], args[1], lastRecipeResourceLocation);
            if (optionalRecipe.isEmpty()) {
                return Optional.empty();
            }

            Object rawRecipeHolder = optionalRecipe.get();
            Object rawRecipeResourceLocation = FastNMS.INSTANCE.field$RecipeHolder$id(rawRecipeHolder);
            Key rawRecipeKey = Key.of(rawRecipeResourceLocation.toString());

            BukkitRecipeManager recipeManager = BukkitRecipeManager.instance();
            boolean isCustom = recipeManager.isCustomRecipe(rawRecipeKey);
            if (!isCustom) {
                injectedCacheCheck.lastRecipe(rawRecipeResourceLocation);
                return optionalRecipe;
            }

            ItemStack itemStack = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(FastNMS.INSTANCE.field$SingleRecipeInput$item(args[0]));
            Item<ItemStack> wrappedItem = BukkitItemManager.instance().wrap(itemStack);
            SingleItemInput<ItemStack> input = new SingleItemInput<>(new OptimizedIDItem<>(wrappedItem.recipeIngredientId(), itemStack));
            CustomCookingRecipe<ItemStack> ceRecipe = (CustomCookingRecipe<ItemStack>) recipeManager.recipeByInput(injectedCacheCheck.customRecipeType(), input, injectedCacheCheck.lastCustomRecipe());
            if (ceRecipe == null) {
                return Optional.empty();
            }

            injectedCacheCheck.lastCustomRecipe(ceRecipe.id());
            if (!ceRecipe.id().equals(rawRecipeKey)) {
                injectedCacheCheck.lastRecipe(KeyUtils.toResourceLocation(ceRecipe.id()));
            }
            return Optional.ofNullable(recipeManager.nmsRecipeHolderByRecipe(ceRecipe));
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public static class GetRecipeForMethodInterceptor1_21_2 {
        public static final GetRecipeForMethodInterceptor1_21_2 INSTANCE = new GetRecipeForMethodInterceptor1_21_2();

        @SuppressWarnings("unchecked")
        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args) {
            InjectedCacheCheck injectedCacheCheck = (InjectedCacheCheck) thisObj;
            Object lastRecipeResourceKey = injectedCacheCheck.lastRecipe();
            Optional<Object> optionalRecipe = (Optional<Object>) FastNMS.INSTANCE.method$RecipeManager$getRecipeFor(BukkitRecipeManager.nmsRecipeManager(), injectedCacheCheck.recipeType(), args[0], args[1], lastRecipeResourceKey);
            if (optionalRecipe.isEmpty()) {
                return Optional.empty();
            }

            // 获取配方的基础信息
            Object recipeHolder = optionalRecipe.get();
            Object rawRecipeResourceKey = FastNMS.INSTANCE.field$RecipeHolder$id(recipeHolder);
            Object rawRecipeResourceLocation = FastNMS.INSTANCE.field$ResourceKey$location(rawRecipeResourceKey);
            Key rawRecipeKey = Key.of(rawRecipeResourceLocation.toString());

            BukkitRecipeManager recipeManager = BukkitRecipeManager.instance();
            // 来自其他插件注册的自定义配方
            boolean isCustom = recipeManager.isCustomRecipe(rawRecipeKey);
            if (!isCustom) {
                injectedCacheCheck.lastRecipe(rawRecipeResourceKey);
                return optionalRecipe;
            }

            // 获取唯一内存地址id
            ItemStack itemStack = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(FastNMS.INSTANCE.field$SingleRecipeInput$item(args[0]));
            Item<ItemStack> wrappedItem = BukkitItemManager.instance().wrap(itemStack);
            SingleItemInput<ItemStack> input = new SingleItemInput<>(new OptimizedIDItem<>(wrappedItem.recipeIngredientId(), itemStack));
            CustomCookingRecipe<ItemStack> ceRecipe = (CustomCookingRecipe<ItemStack>) recipeManager.recipeByInput(injectedCacheCheck.customRecipeType(), input, injectedCacheCheck.lastCustomRecipe());
            // 这个ce配方并不存在，那么应该返回空
            if (ceRecipe == null) {
                return Optional.empty();
            }

            // 记录上一次使用的配方(ce)
            injectedCacheCheck.lastCustomRecipe(ceRecipe.id());
            // 更新上一次使用的配方(nms)
            if (!ceRecipe.id().equals(rawRecipeKey)) {
                injectedCacheCheck.lastRecipe(FastNMS.INSTANCE.method$ResourceKey$create(MRegistries.RECIPE, KeyUtils.toResourceLocation(ceRecipe.id())));
            }
            return Optional.ofNullable(recipeManager.nmsRecipeHolderByRecipe(ceRecipe));
        }
    }
}
