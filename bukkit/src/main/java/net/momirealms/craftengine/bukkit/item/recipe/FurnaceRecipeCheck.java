package net.momirealms.craftengine.bukkit.item.recipe;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.core.item.recipe.ConditionalRecipe;
import net.momirealms.craftengine.core.item.recipe.RecipeType;
import net.momirealms.craftengine.core.item.recipe.input.SingleItemInput;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.ContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.CachedRecipeCheckProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeInputProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.entity.BlockEntityProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor1;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;

public final class FurnaceRecipeCheck {
    private final Object nativeCheck;
    private final Object blockEntity;
    private Object lastStack;
    private boolean failed;

    private FurnaceRecipeCheck(Object nativeCheck, Object blockEntity) {
        this.nativeCheck = nativeCheck;
        this.blockEntity = blockEntity;
    }

    static Object wrap(Object nativeCheck, Object blockEntity) {
        return nativeCheck instanceof Cache ? nativeCheck : AdapterClass.CONSTRUCTOR.newInstance(new FurnaceRecipeCheck(nativeCheck, blockEntity));
    }

    public Optional<?> getRecipeFor(Object input, Object level) {
        Object stack = RecipeConversions.item(input, 0);
        if (stack == this.lastStack) {
            if (this.failed) {
                return Optional.empty();
            }
        } else {
            this.lastStack = stack;
            ConditionalRecipe recipe = (ConditionalRecipe) CraftEngine.instance().recipeManager()
                    .recipeByInput(RecipeType.SMELTING, new SingleItemInput(RecipeConversions.wrap(stack)));
            if (recipe == null) {
                this.failed = false;
            } else if (recipe.hasCondition()) {
                long[] playerId = BlockEntityProxy.INSTANCE.getPersistentDataContainer(this.blockEntity)
                        .get(BukkitRecipeManager.FURNACE_LAST_USER, PersistentDataType.LONG_ARRAY);
                if (playerId != null) {
                    Player player = Bukkit.getPlayer(new UUID(playerId[0], playerId[1]));
                    this.failed = player == null || !player.isConnected() || !recipe.canUse(PlayerOptionalContext.of(BukkitAdaptor.adapt(player)));
                    if (this.failed) {
                        return Optional.empty();
                    }
                }
            }
        }
        if (VersionHelper.isOrAbove1_21_2) {
            return CachedRecipeCheckProxy.INSTANCE.getRecipe(this.nativeCheck, input, level);
        }
        if (VersionHelper.isOrAbove1_21) {
            return CachedRecipeCheckProxy.INSTANCE.getRecipe$1_21(this.nativeCheck, input, level);
        }
        return CachedRecipeCheckProxy.INSTANCE.getRecipe$legacy(this.nativeCheck, input, level);
    }

    public void clear() {
        this.lastStack = null;
    }

    private static SConstructor1 generate() {
        Class<?> inputType = VersionHelper.isOrAbove1_21 ? RecipeInputProxy.CLASS : ContainerProxy.CLASS;
        Class<?> levelType = VersionHelper.isOrAbove1_21_2 ? ServerLevelProxy.CLASS : LevelProxy.CLASS;
        Method target = requireNonNull(RecipeGenerator.findMethod(CachedRecipeCheckProxy.CLASS, "getRecipeFor", inputType, levelType));
        try {
            Class<?> generated = new ByteBuddy(ClassFileVersion.JAVA_V21)
                    .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                    .name(FurnaceRecipeCheck.class.getPackageName() + ".InjectedFurnaceCachedCheck")
                    .implement(CachedRecipeCheckProxy.CLASS, Cache.class)
                    .defineField("check", FurnaceRecipeCheck.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                    .defineConstructor(Visibility.PUBLIC).withParameters(FurnaceRecipeCheck.class)
                    .intercept(MethodCall.invoke(Object.class.getConstructor()).andThen(FieldAccessor.ofField("check").setsArgumentAt(0)))
                    .method(is(target)).intercept(MethodCall.invoke(FurnaceRecipeCheck.class.getMethod("getRecipeFor", Object.class, Object.class))
                            .onField("check").withAllArguments())
                    .method(named("clear")).intercept(MethodCall.invoke(FurnaceRecipeCheck.class.getMethod("clear")).onField("check"))
                    .make()
                    .load(FurnaceRecipeCheck.class.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup())).getLoaded();
            return SparrowClass.of(generated)
                    .getSparrowConstructor(ConstructorMatcher.takeArguments(FurnaceRecipeCheck.class)).asm$1();
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException(exception);
        }
    }

    interface Cache {
        void clear();
    }

    private static final class AdapterClass {
        private static final SConstructor1 CONSTRUCTOR = generate();
    }
}
