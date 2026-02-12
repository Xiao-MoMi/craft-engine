package net.momirealms.craftengine.bukkit.plugin.reflection.bukkit;

import net.momirealms.craftengine.bukkit.plugin.reflection.ReflectionInitException;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BukkitReflectionUtils;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class CraftBukkitReflections {
    private CraftBukkitReflections() {}

    public static final Class<?> clazz$CraftChatMessage = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("util.CraftChatMessage"))
    );

    public static final Method method$CraftChatMessage$fromJSON = requireNonNull(
            ReflectionUtils.getMethod(clazz$CraftChatMessage, new String[]{"fromJSON"}, String.class)
    );

    public static final Class<?> clazz$CraftRegistry = ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleCBClass("CraftRegistry")
    );

    public static final Class<?> clazz$CraftPlayer = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("entity.CraftPlayer"))
    );

    public static final Class<?> clazz$CraftWorld = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("CraftWorld"))
    );

    public static final Class<?> clazz$CraftBlock = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("block.CraftBlock"))
    );

    public static final Class<?> clazz$CraftEventFactory = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("event.CraftEventFactory"))
    );

    public static final Class<?> clazz$CraftBlockStates = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("block.CraftBlockStates"))
    );

    public static final Class<?> clazz$CraftBlockState = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("block.CraftBlockState"))
    );

    public static final Method method$CraftBlockStates$getBlockState = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftBlockStates, clazz$CraftBlockState, CoreReflections.clazz$LevelAccessor, CoreReflections.clazz$BlockPos)
    );

    public static final Method method$CraftBlockState$getHandle = requireNonNull(
            ReflectionUtils.getMethod(clazz$CraftBlockState, CoreReflections.clazz$BlockState)
    );

    public static final Class<?> clazz$CraftBlockData = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("block.data.CraftBlockData"))
    );

    public static final Field field$CraftBlockData$data = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$CraftBlockData, CoreReflections.clazz$BlockState, 0)
    );

    public static final Method method$CraftBlockData$createData = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftBlockData, clazz$CraftBlockData, new String[]{"createData"}, CoreReflections.clazz$BlockState)
    );

    public static final Method method$CraftBlockData$fromData = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftBlockData, clazz$CraftBlockData, new String[]{"fromData"}, CoreReflections.clazz$BlockState)
    );

    public static final Field field$BlockPhysicsEvent$changed = requireNonNull(
            ReflectionUtils.getDeclaredField(BlockPhysicsEvent.class, BlockData.class, 0)
    );

    public static final Class<?> clazz$CraftChunk = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("CraftChunk"))
    );

    public static final Field field$CraftChunk$worldServer = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$CraftChunk, CoreReflections.clazz$ServerLevel, 0)
    );

    public static final Method method$CraftBlock$setTypeAndData = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftBlock, boolean.class, CoreReflections.clazz$LevelAccessor, CoreReflections.clazz$BlockPos, CoreReflections.clazz$BlockState, CoreReflections.clazz$BlockState, boolean.class)
    );

    public static final Class<?> clazz$CraftMagicNumbers = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("util.CraftMagicNumbers"))
    );

    public static final Field field$CraftMagicNumbers$BLOCK_MATERIAL = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$CraftMagicNumbers, "BLOCK_MATERIAL")
    );

    public static final Class<?> clazz$CraftItemStack = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftItemStack"))
    );

    public static final Method method$CraftItemStack$asCraftMirror = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftItemStack, clazz$CraftItemStack, new String[]{"asCraftMirror"}, CoreReflections.clazz$ItemStack)
    );

    public static final Class<?> clazz$Registry$SimpleRegistry = requireNonNull(
            ReflectionUtils.getClazz("org.bukkit.Registry$SimpleRegistry")
    );

    public static final Field field$Registry$SimpleRegistry$map = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Registry$SimpleRegistry, Map.class, 0)
    );

    public static final Method method$CraftEventFactory$callBlockPlaceEvent = requireNonNull(
            VersionHelper.isOrAbove1_21_5()
                    ? ReflectionUtils.getStaticMethod(clazz$CraftEventFactory, BlockPlaceEvent.class, CoreReflections.clazz$ServerLevel, CoreReflections.clazz$Player, CoreReflections.clazz$InteractionHand, BlockState.class, CoreReflections.clazz$BlockPos)
                    : ReflectionUtils.getStaticMethod(clazz$CraftEventFactory, BlockPlaceEvent.class, CoreReflections.clazz$ServerLevel, CoreReflections.clazz$Player, CoreReflections.clazz$InteractionHand, BlockState.class, int.class, int.class, int.class)
    );

    public static final Class<?> clazz$CraftEntity = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("entity.CraftEntity"))
    );

    public static final Field field$CraftEntity$entity = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$CraftEntity, CoreReflections.clazz$Entity, 0)
    );

    public static final Class<?> clazz$InventoryView = requireNonNull(
            ReflectionUtils.getClazz("org.bukkit.inventory.InventoryView")
    );

    public static final Method method$InventoryView$getPlayer = requireNonNull(
            ReflectionUtils.getMethod(clazz$InventoryView, HumanEntity.class)
    );

    public static final Class<?> clazz$CraftRecipe = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftRecipe"))
    );

    public static final Method method$CraftRecipe$addToCraftingManager = requireNonNull(
            ReflectionUtils.getMethod(clazz$CraftRecipe, new String[]{"addToCraftingManager"})
    );

    public static final Method method$CraftRecipe$toMinecraft = Optional.of(clazz$CraftRecipe)
            .map(it -> ReflectionUtils.getStaticMethod(it, CoreReflections.clazz$ResourceKey, NamespacedKey.class))
            .orElse(null);

    public static final Class<?> clazz$CraftShapedRecipe = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftShapedRecipe"))
    );

    public static final Method method$CraftShapedRecipe$fromBukkitRecipe = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftShapedRecipe, clazz$CraftShapedRecipe, ShapedRecipe.class)
    );

    public static final Class<?> clazz$CraftShapelessRecipe = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftShapelessRecipe"))
    );

    public static final Method method$CraftShapelessRecipe$fromBukkitRecipe = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftShapelessRecipe, clazz$CraftShapelessRecipe, ShapelessRecipe.class)
    );

    public static final Class<?> clazz$CraftSmithingTransformRecipe = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftSmithingTransformRecipe"))
    );

    public static final Method method$CraftSmithingTransformRecipe$fromBukkitRecipe = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftSmithingTransformRecipe, clazz$CraftSmithingTransformRecipe, SmithingTransformRecipe.class)
    );

    public static final Class<?> clazz$CraftInventoryPlayer = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftInventoryPlayer"))
    );

    public static final Class<?> clazz$CraftInventory = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftInventory"))
    );

    public static final Method method$CraftInventoryPlayer$getInventory = requireNonNull(
            ReflectionUtils.getMethod(clazz$CraftInventoryPlayer, CoreReflections.clazz$Inventory, new String[]{ "getInventory" })
    );

    public static final Class<?> clazz$CraftServer = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("CraftServer"))
    );

    public static final Field field$CraftServer$playerList = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$CraftServer, CoreReflections.clazz$DedicatedPlayerList, 0)
    );

    public static final MethodHandle methodHandle$CraftServer$playerListGetter;

    static {
        try {
            methodHandle$CraftServer$playerListGetter = ReflectionUtils.unreflectGetter(field$CraftServer$playerList)
                    .asType(MethodType.methodType(Object.class, Server.class));
        } catch (Exception e) {
            throw new ReflectionInitException("Failed to initialize methodHandle$CraftServer$playerList", e);
        }
    }

    public static final Class<?> clazz$CraftInventoryCrafting = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftInventoryCrafting"))
    );

    public static final Field field$CraftInventoryCrafting$resultInventory = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$CraftInventoryCrafting, CoreReflections.clazz$Container, 0)
    );

    public static final Class<?> clazz$CraftResultInventory = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftResultInventory"))
    );

    public static final Field field$CraftResultInventory$resultInventory = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$CraftResultInventory, CoreReflections.clazz$Container, 0)
    );

    public static final Class<?> clazz$CraftFurnaceRecipe = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftFurnaceRecipe"))
    );

    public static final Method method$CraftFurnaceRecipe$fromBukkitRecipe = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftFurnaceRecipe, clazz$CraftFurnaceRecipe, FurnaceRecipe.class)
    );

    public static final Class<?> clazz$CraftBlastingRecipe = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftBlastingRecipe"))
    );

    public static final Method method$CraftBlastingRecipe$fromBukkitRecipe = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftBlastingRecipe, clazz$CraftBlastingRecipe, BlastingRecipe.class)
    );

    public static final Class<?> clazz$CraftSmokingRecipe = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftSmokingRecipe"))
    );

    public static final Method method$CraftSmokingRecipe$fromBukkitRecipe = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftSmokingRecipe, clazz$CraftSmokingRecipe, SmokingRecipe.class)
    );

    public static final Class<?> clazz$CraftCampfireRecipe = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftCampfireRecipe"))
    );

    public static final Method method$CraftCampfireRecipe$fromBukkitRecipe = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftCampfireRecipe, clazz$CraftCampfireRecipe, CampfireRecipe.class)
    );

    public static final Class<?> clazz$CraftStonecuttingRecipe = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftStonecuttingRecipe"))
    );

    public static final Method method$CraftStonecuttingRecipe$fromBukkitRecipe = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftStonecuttingRecipe, clazz$CraftStonecuttingRecipe, StonecuttingRecipe.class)
    );

    public static final Class<?> clazz$CraftBlockEntityState = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("block.CraftBlockEntityState"))
    );

    public static final Field field$CraftBlockEntityState$tileEntity = requireNonNull(
            ReflectionUtils.getInstanceDeclaredField(clazz$CraftBlockEntityState, 0)
    );

    public static final Method method$CraftInventory$getInventory = requireNonNull(
            ReflectionUtils.getMethod(clazz$CraftInventory, CoreReflections.clazz$Container, new String[]{ "getInventory" })
    );

    public static final Class<?> clazz$CraftContainer = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftContainer"))
    );

    public static final Constructor<?> constructor$CraftContainer = requireNonNull(
            ReflectionUtils.getConstructor(clazz$CraftContainer, Inventory.class, CoreReflections.clazz$Player, int.class)
    );

    public static final Method method$CraftContainer$getNotchInventoryType = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftContainer, CoreReflections.clazz$MenuType, Inventory.class)
    );

    public static final Class<?> clazz$CraftComplexRecipe = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftComplexRecipe"))
    );

    public static final Field field$CraftComplexRecipe$recipe = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$CraftComplexRecipe, CoreReflections.clazz$CustomRecipe, 0)
    );

    public static final Class<?> clazz$CraftInventoryAnvil = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftInventoryAnvil"))
    );

    // 1.21+
    public static final Class<?> clazz$CraftInventoryView =
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftInventoryView"));

    // 1.21+
    public static final Field field$CraftInventoryView$container = Optional.ofNullable(clazz$CraftInventoryView)
            .map(it -> ReflectionUtils.getDeclaredField(it, 0)).orElse(null);

    // 1.20-1.20.6
    public static final Field field$CraftInventoryAnvil$menu =
            ReflectionUtils.getDeclaredField(clazz$CraftInventoryAnvil, CoreReflections.clazz$AnvilMenu, 0);

    public static final Method method$CraftRecipe$toIngredient = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftRecipe, CoreReflections.clazz$Ingredient, RecipeChoice.class, boolean.class)
    );

    public static final Method method$CraftEventFactory$handleBlockFormEvent = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftEventFactory, boolean.class, new String[] { "handleBlockFormEvent" }, CoreReflections.clazz$Level, CoreReflections.clazz$BlockPos, CoreReflections.clazz$BlockState, int.class)
    );

    public static final Method method$CraftEventFactory$handleBlockGrowEvent = requireNonNull(
            VersionHelper.isOrAbove1_21_5() ?
                    ReflectionUtils.getStaticMethod(clazz$CraftEventFactory, boolean.class, CoreReflections.clazz$Level, CoreReflections.clazz$BlockPos, CoreReflections.clazz$BlockState, int.class) :
                    ReflectionUtils.getStaticMethod(clazz$CraftEventFactory, boolean.class, CoreReflections.clazz$Level, CoreReflections.clazz$BlockPos, CoreReflections.clazz$BlockState)
    );

    public static final Class<?> clazz$CraftShulker = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("entity.CraftShulker"))
    );

    public static final Method method$CraftShulker$getHandle = requireNonNull(
            ReflectionUtils.getMethod(clazz$CraftShulker, CoreReflections.clazz$Shulker, 0)
    );

    public static final Method method$CraftEntity$getHandle = requireNonNull(
            ReflectionUtils.getMethod(clazz$CraftEntity, CoreReflections.clazz$Entity, 0)
    );

    public static final Class<?> clazz$SignChangeEvent = requireNonNull(
            ReflectionUtils.getClazz("org.bukkit.event.block.SignChangeEvent")
    );

    public static final Class<?> clazz$BookMeta = requireNonNull(
            ReflectionUtils.getClazz("org.bukkit.inventory.meta.BookMeta")
    );

    public static final Method method$CraftPlayer$setSimplifyContainerDesyncCheck =
            ReflectionUtils.getMethod(clazz$CraftPlayer, new String[]{"setSimplifyContainerDesyncCheck"}, boolean.class);

    public static final Field field$CraftBlockStates$FACTORIES = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$CraftBlockStates, "FACTORIES")
    );

    public static final Class<?> clazz$CraftBlockStates$BlockEntityStateFactory = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("block.CraftBlockStates$BlockEntityStateFactory"))
    );

    public static final Method method$Level$getCraftWorld = requireNonNull(
            ReflectionUtils.getMethod(CoreReflections.clazz$Level, clazz$CraftWorld)
    );

    public static final Class<?> clazz$MinecraftInventory = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftInventoryCustom$MinecraftInventory"))
    );

    public static final Class<?> clazz$CraftMerchant = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftMerchant"))
    );

    public static final Method method$CraftMerchant$getMerchant = requireNonNull(
            ReflectionUtils.getMethod(clazz$CraftMerchant, CoreReflections.clazz$Merchant)
    );

    public static final Class<?> clazz$CraftMerchantCustom = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftMerchantCustom"))
    );

    public static final Class<?> clazz$CraftMerchantCustom$MinecraftMerchant = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("inventory.CraftMerchantCustom$MinecraftMerchant"))
    );

    public static final Field field$MinecraftMerchant$title = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$CraftMerchantCustom$MinecraftMerchant, CoreReflections.clazz$Component, 0)
    );

    public static final Class<?> clazz$CraftTeam = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleCBClass("scoreboard.CraftTeam"))
    );

    public static final Field field$CraftTeam$team = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$CraftTeam, CoreReflections.clazz$PlayerTeam, 0)
    );

    public static final Method method$CraftEventFactory$callInventoryOpenEvent = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$CraftEventFactory, CoreReflections.clazz$AbstractContainerMenu, CoreReflections.clazz$ServerPlayer, CoreReflections.clazz$AbstractContainerMenu)
    );
}
