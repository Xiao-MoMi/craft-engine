package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.plugin.reflection.ReflectionInitException;
import net.momirealms.craftengine.bukkit.util.BukkitReflectionUtils;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;

public final class CoreReflections {
    private CoreReflections() {}

    public static final Class<?> clazz$RandomSource = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("util.RandomSource"))
    );

    public static final Class<?> clazz$ResourceLocation = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    List.of("resources.MinecraftKey"),
                    List.of("resources.ResourceLocation", "resources.Identifier")
            )
    );

    public static final Class<?> clazz$ResourceKey = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("resources.ResourceKey"))
    );

    public static final Class<?> clazz$SoundEvent = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "sounds.SoundEffect",
                    "sounds.SoundEvent"
            )
    );

    public static final Constructor<?> constructor$SoundEvent = requireNonNull(
            VersionHelper.isOrAbove1_21_2() ?
                    ReflectionUtils.getConstructor(clazz$SoundEvent, clazz$ResourceLocation, Optional.class) :
                    ReflectionUtils.getDeclaredConstructor(clazz$SoundEvent, clazz$ResourceLocation, float.class, boolean.class)
    );

    public static final Class<?> clazz$SoundSource = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "sounds.SoundCategory",
                    "sounds.SoundSource"
            )
    );

    public static final Class<?> clazz$Component = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.chat.IChatBaseComponent",
                    "network.chat.Component"
            )
    );

    public static final Class<?> clazz$HolderLookup$Provider = BukkitReflectionUtils.findReobfOrMojmapClass(
            VersionHelper.isOrAbove1_20_5() ? "core.HolderLookup$a" : "core.HolderLookup$b",
            "core.HolderLookup$Provider"
    );

    public static final Class<?> clazz$Holder = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("core.Holder"))
    );

    public static final Class<?> clazz$Holder$Reference = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.Holder$c",
                    "core.Holder$Reference"
            )
    );

    public static final Class<?> clazz$RegistryAccess = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.IRegistryCustom",
                    "core.RegistryAccess"
            )
    );

    public static final Class<?> clazz$Registries = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("core.registries.Registries"))
    );

    public static final Class<?> clazz$BuiltInRegistries = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("core.registries.BuiltInRegistries"))
    );

    public static final Class<?> clazz$BlockPos = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.BlockPosition",
                    "core.BlockPos"
            )
    );

    public static final Class<?> clazz$IdMapper = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.RegistryBlockID",
                    "core.IdMapper"
            )
    );

    public static final Class<?> clazz$IdMap = requireNonNull(
            clazz$IdMapper.getInterfaces()[0]
    );

    public static final Method method$IdMap$size = requireNonNull(
            ReflectionUtils.getMethod(clazz$IdMap, int.class)
    );

    public static final Method method$IdMapper$size = requireNonNull(
            ReflectionUtils.getMethod(clazz$IdMapper, int.class)
    );

    public static final Class<?> clazz$Direction = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.EnumDirection",
                    "core.Direction"
            )
    );

    public static final Class<?> clazz$ParticleType = requireNonNull(
            Optional.of(Objects.requireNonNull(BukkitReflectionUtils.findReobfOrMojmapClass("core.particles.Particle", "core.particles.ParticleType"))).map(it -> {
                if (it.getSuperclass() != Object.class) {
                    return it.getSuperclass();
                }
                return it;
            }).orElseThrow()
    );

    public static final Class<?> clazz$ParticleTypes = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.particles.Particles",
                    "core.particles.ParticleTypes"
            )
    );

    public static final Class<?> clazz$BlockParticleOption = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "core.particles.ParticleParamBlock",
                    "core.particles.BlockParticleOption"
            )
    );

    public static final Class<?> clazz$NonNullList = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("core.NonNullList"))
    );

    public static final Class<?> clazz$DataComponentType = ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass("core.component.DataComponentType")
    );

    // 1.21.5+
    public static final Class<?> clazz$DataComponentPredicate$Type = MiscUtils.requireNonNullIf(ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass("core.component.predicates.DataComponentPredicate$Type")
    ), VersionHelper.isOrAbove1_21_5());

    public static final Class<?> clazz$Tag = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "nbt.NBTBase",
                    "nbt.Tag"
            )
    );

    public static final Class<?> clazz$EntityDataSerializers = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.syncher.DataWatcherRegistry",
                    "network.syncher.EntityDataSerializers"
            )
    );
    
    public static final Class<?> clazz$FriendlyByteBuf = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "network.PacketDataSerializer",
                    "network.FriendlyByteBuf"
            )
    );

    public static final Class<?> clazz$RegistryFriendlyByteBuf =
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("network.RegistryFriendlyByteBuf")
            );

    public static final Class<?> clazz$LevelReader = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.IWorldReader",
                    "world.level.LevelReader"
            )
    );

    public static final Class<?> clazz$DimensionType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.dimension.DimensionManager",
                    "world.level.dimension.DimensionType"
            )
    );

    public static final Class<?> clazz$EntityType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.EntityTypes",
                    "world.entity.EntityType"
            )
    );

    public static final Class<?> clazz$VoxelShape = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.phys.shapes.VoxelShape"))
    );

    public static final Class<?> clazz$Vec3 = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.phys.Vec3D",
                    "world.phys.Vec3"
            )
    );

    public static final Class<?> clazz$AttributeModifier = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.entity.ai.attributes.AttributeModifier")
            )
    );

    public static final Class<?> clazz$AttributeModifier$Operation = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.entity.ai.attributes.AttributeModifier$Operation"))
    );

    public static final Constructor<?> constructor$AttributeModifier = requireNonNull(
            !VersionHelper.isOrAbove1_20_5() ? ReflectionUtils.getConstructor(clazz$AttributeModifier, String.class, double.class, clazz$AttributeModifier$Operation) :
                    (!VersionHelper.isOrAbove1_21() ? ReflectionUtils.getConstructor(clazz$AttributeModifier, UUID.class, String.class, double.class, clazz$AttributeModifier$Operation) :
                            (ReflectionUtils.getConstructor(clazz$AttributeModifier, clazz$ResourceLocation, double.class, clazz$AttributeModifier$Operation)))
    );

    public static final Class<?> clazz$Attribute = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.ai.attributes.AttributeBase",
                    "world.entity.ai.attributes.Attribute"
            )
    );

    public static final Class<?> clazz$Biome = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.biome.BiomeBase",
                    "world.level.biome.Biome"
            )
    );

    public static final Class<?> clazz$BlockState = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.IBlockData",
                    "world.level.block.state.BlockState"
            )
    );

    public static final Class<?> clazz$Block = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.level.block.Block"))
    );

    public static final Class<?> clazz$LevelAccessor = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.GeneratorAccess",
                    "world.level.LevelAccessor"
            )
    );

    public static final Class<?> clazz$PalettedContainer = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.chunk.DataPaletteBlock",
                    "world.level.chunk.PalettedContainer"
            )
    );

    public static final Class<?> clazz$LevelChunk = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.chunk.Chunk",
                    "world.level.chunk.LevelChunk"
            )
    );

    public static final Class<?> clazz$LevelChunkSection = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.chunk.ChunkSection",
                    "world.level.chunk.LevelChunkSection"
            )
    );

    public static final Method method$LevelChunkSection$setBlockState = requireNonNull(
            ReflectionUtils.getMethod(clazz$LevelChunkSection, clazz$BlockState, int.class, int.class, int.class, clazz$BlockState, boolean.class)
    );

    public static final Class<?> clazz$BlockBehaviour$Properties = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.BlockBase$Info",
                    "world.level.block.state.BlockBehaviour$Properties"
            )
    );

    public static final Class<?> clazz$BlockBehaviour = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.BlockBase",
                    "world.level.block.state.BlockBehaviour"
            )
    );

    public static final Class<?> clazz$MobEffect = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.effect.MobEffectList"),  // 这里paper会自动获取到NM.world.effect.MobEffect
                    BukkitReflectionUtils.assembleMCClass("world.effect.MobEffect") // paper柠檬酸了
            )
    );

    public static final Class<?> clazz$MobEffectInstance = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.effect.MobEffect",
                    "world.effect.MobEffectInstance"
            )
    );

    public static final Class<?> clazz$Item = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.item.Item"))
    );

    public static final Class<?> clazz$FluidState = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.material.Fluid",
                    "world.level.material.FluidState"
            )
    );

    public static final Class<?> clazz$Fluid = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.material.FluidType"),  // 这里paper会自动获取到NM.world.level.material.Fluid
                    BukkitReflectionUtils.assembleMCClass("world.level.material.Fluid") // paper柠檬酸了
            )
    );

    public static final Class<?> clazz$RecipeType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.Recipes",
                    "world.item.crafting.RecipeType"
            )
    );

    public static final Class<?> clazz$WorldGenLevel = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.GeneratorAccessSeed",
                    "world.level.WorldGenLevel"
            )
    );

    public static final Class<?> clazz$ChunkGenerator = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.chunk.ChunkGenerator")
            )
    );

    public static final Class<?> clazz$ConfiguredFeature = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.levelgen.feature.WorldGenFeatureConfigured",
                    "world.level.levelgen.feature.ConfiguredFeature"
            )
    );

    public static final Class<?> clazz$PlacedFeature = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.level.levelgen.placement.PlacedFeature"))
    );

    public static final Constructor<?> constructor$PlacedFeature = requireNonNull(
            ReflectionUtils.getConstructor(clazz$PlacedFeature, clazz$Holder, List.class)
    );

    // 1.21+
    public static final Class<?> clazz$JukeboxSong = ReflectionUtils.getClazz(
            BukkitReflectionUtils.assembleMCClass("world.item.JukeboxSong")
    );

    public static final Class<?> clazz$StateDefinition$Factory = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.BlockStateList$b",
                    "world.level.block.state.StateDefinition$Factory"
            )
    );

    public static final Class<?> clazz$ServerLevel = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "server.level.WorldServer",
                    "server.level.ServerLevel"
            )
    );

    public static final Class<?> clazz$Explosion = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.level.Explosion"))
    );

    public static final Class<?> clazz$BlockStateBase = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.BlockBase$BlockData",
                    "world.level.block.state.BlockBehaviour$BlockStateBase"
            )
    );

    public static final Class<?> clazz$BlockGetter = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.IBlockAccess",
                    "world.level.BlockGetter"
            )
    );

    public static final Class<?> clazz$StateHolder = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.IBlockDataHolder",
                    "world.level.block.state.StateHolder"
            )
    );

    public static final Class<?> clazz$CollisionContext = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.phys.shapes.VoxelShapeCollision",
                    "world.phys.shapes.CollisionContext"
            )
    );

    public static final Class<?> clazz$PathComputationType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.pathfinder.PathMode",
                    "world.level.pathfinder.PathComputationType"
            )
    );

    public static final Method method$BlockBehaviour$isPathFindable = requireNonNull(
            VersionHelper.isOrAbove1_20_5() ?
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, boolean.class, clazz$BlockState, clazz$PathComputationType) :
            ReflectionUtils.getMethod(clazz$BlockBehaviour, boolean.class, clazz$BlockState, clazz$BlockGetter, clazz$BlockPos, clazz$PathComputationType)
    );

    public static final Method method$BlockBehaviour$getShape = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, clazz$VoxelShape, new String[]{"getShape", "a"}, clazz$BlockState, clazz$BlockGetter, clazz$BlockPos, clazz$CollisionContext)
    );

    public static final Method method$BlockBehaviour$getCollisionShape = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, clazz$VoxelShape, new String[]{"getCollisionShape", VersionHelper.isOrAbove1_20_3() ? "b" : "c"}, clazz$BlockState, clazz$BlockGetter, clazz$BlockPos, clazz$CollisionContext)
    );

    public static final Method method$BlockBehaviour$getBlockSupportShape = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, clazz$VoxelShape, new String[]{"getBlockSupportShape", "b_"}, clazz$BlockState, clazz$BlockGetter, clazz$BlockPos)
    );

    public static final Class<?> clazz$LevelLightEngine = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.level.lighting.LevelLightEngine"))
    );

    public static final Class<?> clazz$Player = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.player.EntityHuman",
                    "world.entity.player.Player"
            )
    );

    public static final Class<?> clazz$Entity = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.entity.Entity"))
    );

    public static final Class<?> clazz$Level = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.World",
                    "world.level.Level"
            )
    );

    public static final Method method$BlockBehaviour$hasAnalogOutputSignal = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, boolean.class, new String[]{"hasAnalogOutputSignal",
            VersionHelper.isOrAbove1_20_5() ? "c_" : "d_"}, clazz$BlockState)
    );

    public static final Method method$BlockBehaviour$getAnalogOutputSignal = requireNonNull(
            VersionHelper.isOrAbove1_21_9()
                    ? ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, int.class, new String[]{"getAnalogOutputSignal", "a"}, clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$Direction)
                    : ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, int.class, new String[]{"getAnalogOutputSignal", "a"}, clazz$BlockState, clazz$Level, clazz$BlockPos)
    );

    public static final Class<?> clazz$InteractionHand = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.EnumHand",
                    "world.InteractionHand"
            )
    );

    public static final Class<?> clazz$ItemStack = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.item.ItemStack"))
    );

    // 1.21.3+
    public static final Class<?> clazz$ScheduledTickAccess =
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.level.ScheduledTickAccess"));

    public static final Method method$BlockBehaviour$updateShape = requireNonNull(
            VersionHelper.isOrAbove1_21_2() ?
                    ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, clazz$BlockState, clazz$BlockState, clazz$LevelReader, clazz$ScheduledTickAccess, clazz$BlockPos, clazz$Direction, clazz$BlockPos, clazz$BlockState, clazz$RandomSource) :
                    ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, clazz$BlockState, clazz$BlockState, clazz$Direction, clazz$BlockState, clazz$LevelAccessor, clazz$BlockPos, clazz$BlockPos)
    );

    public static final Method method$BlockBehaviour$canSurvive = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, boolean.class, clazz$BlockState, clazz$LevelReader, clazz$BlockPos)
    );

    public static final Method method$Block$stepOn = requireNonNull(
            ReflectionUtils.getMethod(clazz$Block, void.class, new String[] {"stepOn", "a"}, clazz$Level, clazz$BlockPos, clazz$BlockState, clazz$Entity)
    );

    public static final Method method$BlockBehaviour$onExplosionHit = MiscUtils.requireNonNullIf(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, clazz$BlockState, VersionHelper.isOrAbove1_21_2() ? clazz$ServerLevel : clazz$Level, clazz$BlockPos, clazz$Explosion, BiConsumer.class),
            VersionHelper.isOrAbove1_21()
    );

    public static final Class<?> clazz$Fallable = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.level.block.Fallable"))
    );

    public static final Class<?> clazz$FallingBlockEntity = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.item.EntityFallingBlock",
                    "world.entity.item.FallingBlockEntity"
            )
    );

    public static final Method method$Fallable$onLand = requireNonNull(
            ReflectionUtils.getMethod(clazz$Fallable, void.class, clazz$Level, clazz$BlockPos, clazz$BlockState, clazz$BlockState, clazz$FallingBlockEntity)
    );

    public static final Method method$Fallable$onBrokenAfterFall = requireNonNull(
            ReflectionUtils.getMethod(clazz$Fallable, void.class, clazz$Level, clazz$BlockPos, clazz$FallingBlockEntity)
    );

    public static final Class<?> clazz$LeavesBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.BlockLeaves",
                    "world.level.block.LeavesBlock"
            )
    );

    public static final Class<?> clazz$Property = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.state.properties.IBlockState",
                    "world.level.block.state.properties.Property"
            )
    );

    public static final Method method$StateHolder$hasProperty = requireNonNull(
            ReflectionUtils.getMethod(clazz$StateHolder, boolean.class, clazz$Property)
    );

    public static final Method method$StateHolder$getValue = requireNonNull(
            ReflectionUtils.getMethod(clazz$StateHolder, Object.class, new String[] {"getValue", "c"}, clazz$Property)
    );

    public static final Method method$StateHolder$setValue = requireNonNull(
            ReflectionUtils.getMethod(clazz$StateHolder, Object.class, new String[] {"setValue", VersionHelper.isOrAbove1_21_2() ? "b" : "a"}, clazz$Property, Comparable.class)
    );

    public static final Method method$BlockStateBase$updateNeighbourShapes = requireNonNull(
            ReflectionUtils.getMethod(
                    // flags   // depth
                    clazz$BlockStateBase, void.class, clazz$LevelAccessor, clazz$BlockPos, int.class, int.class
            )
    );

    public static final Field field$LevelChunkSection$states = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$LevelChunkSection, clazz$PalettedContainer, 0)
    );

    public static final Method method$FluidState$createLegacyBlock = requireNonNull(
            ReflectionUtils.getMethod(clazz$FluidState, clazz$BlockState)
    );

    public static final Class<?> clazz$RecipeManager = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.CraftingManager",
                    "world.item.crafting.RecipeManager"
            )
    );

    public static final Method method$RecipeManager$finalizeRecipeLoading =
            ReflectionUtils.getMethod(clazz$RecipeManager, new String[]{"finalizeRecipeLoading"});

    public static final Class<?> clazz$FeatureFlagSet = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.flag.FeatureFlagSet"))
    );

    public static final Field field$RecipeManager$featureflagset =
            ReflectionUtils.getDeclaredField(clazz$RecipeManager, clazz$FeatureFlagSet, 0);

    public static final Class<?> clazz$Ingredient = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.RecipeItemStack",
                    "world.item.crafting.Ingredient"
            )
    );

    public static final Field field$Ingredient$itemStacks = requireNonNull(
            ReflectionUtils.getDeclaredField(
                    clazz$Ingredient,
                    VersionHelper.isOrAbove1_21_4() ? Set.class : VersionHelper.isOrAbove1_21_2() ? List.class : clazz$ItemStack.arrayType(),
                    VersionHelper.isOrAbove1_21_4() ? 0 : VersionHelper.isOrAbove1_21_2() ? 1 : 0
            )
    );

    public static final MethodHandle methodHandle$RecipeManager$finalizeRecipeLoading;
    public static final MethodHandle methodHandle$RecipeManager$featureflagsetGetter;
    public static final MethodHandle methodHandle$RecipeManager$featureflagsetSetter;
    public static final MethodHandle methodHandle$Ingredient$itemStacksSetter;

    static {
        try {
            if (method$RecipeManager$finalizeRecipeLoading != null) {
                methodHandle$RecipeManager$finalizeRecipeLoading = ReflectionUtils.unreflectMethod(method$RecipeManager$finalizeRecipeLoading)
                        .asType(MethodType.methodType(void.class, Object.class));
            } else {
                methodHandle$RecipeManager$finalizeRecipeLoading = null;
            }
            if (field$RecipeManager$featureflagset != null) {
                methodHandle$RecipeManager$featureflagsetGetter = ReflectionUtils.unreflectGetter(field$RecipeManager$featureflagset)
                        .asType(MethodType.methodType(Object.class, Object.class));
                methodHandle$RecipeManager$featureflagsetSetter = ReflectionUtils.unreflectSetter(field$RecipeManager$featureflagset)
                        .asType(MethodType.methodType(void.class, Object.class, Object.class));
            } else {
                methodHandle$RecipeManager$featureflagsetGetter = null;
                methodHandle$RecipeManager$featureflagsetSetter = null;
            }
            methodHandle$Ingredient$itemStacksSetter = ReflectionUtils.unreflectSetter(field$Ingredient$itemStacks)
                    .asType(MethodType.methodType(void.class, Object.class, VersionHelper.isOrAbove1_21_4() ? Set.class : VersionHelper.isOrAbove1_21_2() ? List.class : Object.class));
        } catch (Exception e) {
            throw new ReflectionInitException("Failed to initialize CoreReflections", e);
        }
    }

    public static final Class<?> clazz$ShapedRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.ShapedRecipes",
                    "world.item.crafting.ShapedRecipe"
            )
    );

    // 1.20.3+
    public static final Class<?> clazz$ShapedRecipePattern =
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.item.crafting.ShapedRecipePattern"));

    // 1.20.1-1.20.2
    public static final Field field$1_20_1$ShapedRecipe$recipeItems =
            ReflectionUtils.getDeclaredField(clazz$ShapedRecipe, clazz$NonNullList, 0);

    // 1.20.3+
    public static final Field field$1_20_3$ShapedRecipe$pattern=
            ReflectionUtils.getDeclaredField(clazz$ShapedRecipe, clazz$ShapedRecipePattern, 0);

    // 1.20.3-1.21.1
    public static final Field field$ShapedRecipePattern$ingredients1_20_3 = Optional.ofNullable(clazz$ShapedRecipePattern)
            .map(it -> ReflectionUtils.getDeclaredField(it, clazz$NonNullList, 0))
            .orElse(null);

    // 1.21.2+
    public static final Field field$ShapedRecipePattern$ingredients1_21_2 = Optional.ofNullable(clazz$ShapedRecipePattern)
            .map(it -> ReflectionUtils.getDeclaredField(it, List.class, 0))
            .orElse(null);

    // 1.20.1-1.21.1
    public static final Field field$Ingredient$values =
            ReflectionUtils.getInstanceDeclaredField(clazz$Ingredient, 0);

    public static final Class<?> clazz$ShapelessRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.ShapelessRecipes",
                    "world.item.crafting.ShapelessRecipe"
            )
    );

    public static final Class<?> clazz$PlacementInfo =
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.item.crafting.PlacementInfo"));

    // 1.21.2+
    public static final Field field$ShapelessRecipe$placementInfo = Optional.ofNullable(clazz$PlacementInfo)
            .map(it -> ReflectionUtils.getDeclaredField(clazz$ShapelessRecipe, it, 0))
            .orElse(null);

    public static final Field field$ShapedRecipe$placementInfo = Optional.ofNullable(clazz$PlacementInfo)
            .map(it -> ReflectionUtils.getDeclaredField(clazz$ShapedRecipe, it, 0))
            .orElse(null);

    public static final Field field$ShapelessRecipe$ingredients =
            Optional.ofNullable(ReflectionUtils.getDeclaredField(clazz$ShapelessRecipe, List.class, 0))
                    .orElse(ReflectionUtils.getDeclaredField(clazz$ShapelessRecipe, clazz$NonNullList, 0));

    public static final MethodHandle methodHandle$1_20_1$ShapedRecipe$recipeItemsGetter;
    public static final MethodHandle methodHandle$1_20_3$ShapedRecipe$patternGetter;
    public static final MethodHandle methodHandle$ShapedRecipePattern$ingredients1_20_3Getter;
    public static final MethodHandle methodHandle$ShapedRecipePattern$ingredients1_21_2Getter;
    public static final MethodHandle methodHandle$Ingredient$valuesGetter;
    public static final MethodHandle methodHandle$ShapelessRecipe$placementInfoSetter;
    public static final MethodHandle methodHandle$ShapedRecipe$placementInfoSetter;
    public static final MethodHandle methodHandle$ShapelessRecipe$ingredientsGetter;

    static {
        try {
            if (field$1_20_1$ShapedRecipe$recipeItems != null) {
                methodHandle$1_20_1$ShapedRecipe$recipeItemsGetter = ReflectionUtils.unreflectGetter(field$1_20_1$ShapedRecipe$recipeItems)
                        .asType(MethodType.methodType(List.class, Object.class));
            } else {
                methodHandle$1_20_1$ShapedRecipe$recipeItemsGetter = null;
            }
            if (field$1_20_3$ShapedRecipe$pattern != null) {
                methodHandle$1_20_3$ShapedRecipe$patternGetter = ReflectionUtils.unreflectGetter(field$1_20_3$ShapedRecipe$pattern)
                        .asType(MethodType.methodType(Object.class, Object.class));
            } else {
                methodHandle$1_20_3$ShapedRecipe$patternGetter = null;
            }
            if (field$ShapedRecipePattern$ingredients1_20_3 != null) {
                methodHandle$ShapedRecipePattern$ingredients1_20_3Getter = ReflectionUtils.unreflectGetter(field$ShapedRecipePattern$ingredients1_20_3)
                        .asType(MethodType.methodType(List.class, Object.class));
            } else {
                methodHandle$ShapedRecipePattern$ingredients1_20_3Getter = null;
            }
            if (field$ShapedRecipePattern$ingredients1_21_2 != null) {
                methodHandle$ShapedRecipePattern$ingredients1_21_2Getter = ReflectionUtils.unreflectGetter(field$ShapedRecipePattern$ingredients1_21_2)
                        .asType(MethodType.methodType(List.class, Object.class));
            } else {
                methodHandle$ShapedRecipePattern$ingredients1_21_2Getter = null;
            }
            if (field$Ingredient$values != null) {
                methodHandle$Ingredient$valuesGetter = ReflectionUtils.unreflectGetter(field$Ingredient$values)
                        .asType(MethodType.methodType(Object[].class, Object.class));
            } else {
                methodHandle$Ingredient$valuesGetter = null;
            }
            if (field$ShapelessRecipe$placementInfo != null) {
                methodHandle$ShapelessRecipe$placementInfoSetter = ReflectionUtils.unreflectSetter(field$ShapelessRecipe$placementInfo)
                        .asType(MethodType.methodType(void.class, Object.class, Object.class));
            } else {
                methodHandle$ShapelessRecipe$placementInfoSetter = null;
            }
            if (field$ShapedRecipe$placementInfo != null) {
                methodHandle$ShapedRecipe$placementInfoSetter = ReflectionUtils.unreflectSetter(field$ShapedRecipe$placementInfo)
                        .asType(MethodType.methodType(void.class, Object.class, Object.class));
            } else {
                methodHandle$ShapedRecipe$placementInfoSetter = null;
            }
            if (field$ShapelessRecipe$ingredients != null) {
                methodHandle$ShapelessRecipe$ingredientsGetter = ReflectionUtils.unreflectGetter(field$ShapelessRecipe$ingredients)
                        .asType(MethodType.methodType(List.class, Object.class));
            } else {
                methodHandle$ShapelessRecipe$ingredientsGetter = null;
            }
        } catch (Exception e) {
            throw new ReflectionInitException("Failed to initialize CoreReflections", e);
        }
    }

    // require ResourceLocation for 1.20.1-1.21.1
    // require ResourceKey for 1.21.2+
    public static final Method method$RecipeManager$byKey;

    static {
        Method method$RecipeManager$byKey0 = null;
        if (VersionHelper.isOrAbove1_21_2()) {
            for (Method method : clazz$RecipeManager.getMethods()) {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == clazz$ResourceKey) {
                    if (method.getReturnType() == Optional.class && method.getGenericReturnType() instanceof ParameterizedType type) {
                        Type[] actualTypeArguments = type.getActualTypeArguments();
                        if (actualTypeArguments.length == 1) {
                            method$RecipeManager$byKey0 = method;
                        }
                    }
                }
            }
        } else if (VersionHelper.isOrAbove1_20_2()) {
            for (Method method : clazz$RecipeManager.getMethods()) {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == clazz$ResourceLocation) {
                    if (method.getReturnType() == Optional.class && method.getGenericReturnType() instanceof ParameterizedType type) {
                        Type[] actualTypeArguments = type.getActualTypeArguments();
                        if (actualTypeArguments.length == 1) {
                            method$RecipeManager$byKey0 = method;
                        }
                    }
                }
            }
        } else {
            for (Method method : clazz$RecipeManager.getMethods()) {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == clazz$ResourceLocation) {
                    if (method.getReturnType() == Optional.class) {
                        method$RecipeManager$byKey0 = method;
                    }
                }
            }
        }
        method$RecipeManager$byKey = requireNonNull(method$RecipeManager$byKey0);
    }

    public static final Class<?> clazz$Recipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.IRecipe",
                    "world.item.crafting.Recipe"
            )
    );

    public static final Class<?> clazz$LivingEntity = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.EntityLiving",
                    "world.entity.LivingEntity"
            )
    );

    // for 1.20.1-1.21.1
    public static final Class<?> clazz$AbstractCookingRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.RecipeCooking",
                    "world.item.crafting.AbstractCookingRecipe"
            )
    );

    // for 1.20.1-1.21.1
    public static final Field field$AbstractCookingRecipe$input =
            ReflectionUtils.getDeclaredField(clazz$AbstractCookingRecipe, clazz$Ingredient, 0);

    // for 1.21.2+
    public static final Class<?> clazz$SingleItemRecipe =
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.RecipeSingleItem",
                    "world.item.crafting.SingleItemRecipe"
            );

    // for 1.21.2+
    public static final Field field$SingleItemRecipe$input =
            Optional.ofNullable(clazz$SingleItemRecipe)
                    .map(it -> ReflectionUtils.getDeclaredField(it, clazz$Ingredient, 0))
                    .orElse(null);

    public static final MethodHandle methodHandle$AbstractCookingRecipe$inputGetter;
    public static final MethodHandle methodHandle$SingleItemRecipe$inputGetter;

    static {
        try {
            if (field$AbstractCookingRecipe$input != null) {
                methodHandle$AbstractCookingRecipe$inputGetter = ReflectionUtils.unreflectGetter(field$AbstractCookingRecipe$input)
                        .asType(MethodType.methodType(Object.class, Object.class));
            } else {
                methodHandle$AbstractCookingRecipe$inputGetter = null;
            }
            if (field$SingleItemRecipe$input != null) {
                methodHandle$SingleItemRecipe$inputGetter = ReflectionUtils.unreflectGetter(field$SingleItemRecipe$input)
                        .asType(MethodType.methodType(Object.class, Object.class));
            } else {
                methodHandle$SingleItemRecipe$inputGetter = null;
            }
        } catch (Exception e) {
            throw new ReflectionInitException("Failed to initialize methodHandle$SingleItemRecipe$inputGetter", e);
        }
    }

    public static final Method method$LevelReader$getMaxLocalRawBrightness = requireNonNull(
            ReflectionUtils.getMethod(clazz$LevelReader, int.class, clazz$BlockPos)
    );

    public static final Method method$ConfiguredFeature$place = requireNonNull(
            ReflectionUtils.getMethod(clazz$ConfiguredFeature, boolean.class, clazz$WorldGenLevel, clazz$ChunkGenerator, clazz$RandomSource, clazz$BlockPos)
    );

    public static final Method method$PlacedFeature$place = requireNonNull(
            ReflectionUtils.getMethod(clazz$PlacedFeature, boolean.class, clazz$WorldGenLevel, clazz$ChunkGenerator, clazz$RandomSource, clazz$BlockPos)
    );

    public static final Class<?> clazz$BonemealableBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.IBlockFragilePlantElement",
                    "world.level.block.BonemealableBlock"
            )
    );

    public static final Class<?> clazz$WorldlyContainerHolder = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.IInventoryHolder",
                    "world.WorldlyContainerHolder"
            )
    );

    public static final Method method$BonemealableBlock$isValidBonemealTarget = requireNonNull(
            VersionHelper.isOrAbove1_20_2() ?
                    ReflectionUtils.getInstanceMethod(clazz$BonemealableBlock, boolean.class, clazz$LevelReader, clazz$BlockPos, clazz$BlockState) :
                    ReflectionUtils.getInstanceMethod(clazz$BonemealableBlock, boolean.class, clazz$LevelReader, clazz$BlockPos, clazz$BlockState, boolean.class)
    );

    public static final Class<?> clazz$WorldlyContainer = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.IWorldInventory",
                    "world.WorldlyContainer"
            )
    );

    public static final Method method$WorldlyContainerHolder$getContainer = requireNonNull(
            ReflectionUtils.getMethod(clazz$WorldlyContainerHolder, clazz$WorldlyContainer, clazz$BlockState, clazz$LevelAccessor, clazz$BlockPos)
    );
    public static final Method method$BonemealableBlock$isBonemealSuccess = requireNonNull(
            ReflectionUtils.getMethod(clazz$BonemealableBlock, boolean.class, clazz$Level, clazz$RandomSource, clazz$BlockPos, clazz$BlockState)
    );
    
    public static final Method method$PalettedContainer$getAndSet = Objects.requireNonNull(
            ReflectionUtils.getMethod(clazz$PalettedContainer, Object.class, new String[] {"a", "getAndSet"}, int.class, int.class, int.class, Object.class)
    );

    public static final Method method$PalettedContainer$getAndSetUnchecked = Objects.requireNonNull(
            ReflectionUtils.getMethod(clazz$PalettedContainer, Object.class, new String[] {"b", "getAndSetUnchecked"}, int.class, int.class, int.class, Object.class)
    );

    public static final Class<?> clazz$MenuType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.inventory.Containers",
                    "world.inventory.MenuType"
            )
    );

    public static final Class<?> clazz$AbstractContainerMenu = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.inventory.Container",
                    "world.inventory.AbstractContainerMenu"
            )
    );

    public static final Field field$Player$containerMenu = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$Player, clazz$AbstractContainerMenu, 0)
    );

    public static final Field field$AbstractContainerMenu$containerId = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$AbstractContainerMenu, int.class, 1)
    );

    public static final Field field$AbstractContainerMenu$menuType = requireNonNull(
            ReflectionUtils.getDeclaredField(clazz$AbstractContainerMenu, clazz$MenuType, 0)
    );

    public static final Method method$AbstractContainerMenu$broadcastFullState = requireNonNull(
            ReflectionUtils.getMethod(clazz$AbstractContainerMenu, void.class, new String[]{ "broadcastFullState", "e" })
    );

    public static final Field field$AbstractContainerMenu$checkReachable = requireNonNull(
            ReflectionUtils.getDeclaredFieldBackwards(clazz$AbstractContainerMenu, boolean.class, 0)
    );

    public static final Constructor<?> constructor$JukeboxSong = Optional.ofNullable(clazz$JukeboxSong)
            .map(it -> ReflectionUtils.getConstructor(it, clazz$Holder, clazz$Component, float.class, int.class))
            .orElse(null);

    public static final Class<?> clazz$RepairItemRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.RecipeRepair",
                    "world.item.crafting.RepairItemRecipe"
            )
    );

    public static final Class<?> clazz$ArmorDyeRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.RecipeArmorDye",
                    "world.item.crafting.ArmorDyeRecipe"
            )
    );

    public static final Class<?> clazz$FireworkStarFadeRecipe = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.RecipeFireworksFade",
                    "world.item.crafting.FireworkStarFadeRecipe"
            )
    );

    public static final Method method$ItemStack$getItem = requireNonNull(
            ReflectionUtils.getMethod(clazz$ItemStack, clazz$Item)
    );

    public static final Class<?> clazz$BlockHitResult = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.phys.MovingObjectPositionBlock",
                    "world.phys.BlockHitResult"
            )
    );

    public static final Class<?> clazz$ClipContext$Fluid = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.RayTrace$FluidCollisionOption",
                    "world.level.ClipContext$Fluid"
            )
    );

    public static final Method method$ClipContext$Fluid$values = requireNonNull(
            ReflectionUtils.getStaticMethod(clazz$ClipContext$Fluid, clazz$ClipContext$Fluid.arrayType())
    );

    public static final Object instance$ClipContext$Fluid$NONE;
    public static final Object instance$ClipContext$Fluid$SOURCE_ONLY;
    public static final Object instance$ClipContext$Fluid$ANY;

    static {
        try {
            Object[] values = (Object[]) method$ClipContext$Fluid$values.invoke(null);
            instance$ClipContext$Fluid$NONE = values[0];
            instance$ClipContext$Fluid$SOURCE_ONLY = values[1];
            instance$ClipContext$Fluid$ANY = values[2];
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Method method$Item$getPlayerPOVHitResult = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$Item, clazz$BlockHitResult, clazz$Level, clazz$Player, clazz$ClipContext$Fluid)
    );

    public static final Class<?> clazz$SimpleWaterloggedBlock = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.IBlockWaterlogged",
                    "world.level.block.SimpleWaterloggedBlock"
            )
    );

    public static final Method method$SimpleWaterloggedBlock$canPlaceLiquid = requireNonNull(
            VersionHelper.isOrAbove1_21_5()
                    ? ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, boolean.class, clazz$LivingEntity, clazz$BlockGetter, clazz$BlockPos, clazz$BlockState, clazz$Fluid)
                    : VersionHelper.isOrAbove1_20_2()
                    ? ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, boolean.class, clazz$Player, clazz$BlockGetter, clazz$BlockPos, clazz$BlockState, clazz$Fluid)
                    : ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, boolean.class, clazz$BlockGetter, clazz$BlockPos, clazz$BlockState, clazz$Fluid)
    );

    public static final Method method$SimpleWaterloggedBlock$placeLiquid = requireNonNull(
            ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, boolean.class, clazz$LevelAccessor, clazz$BlockPos, clazz$BlockState, clazz$FluidState)
    );

    public static final Method method$SimpleWaterloggedBlock$pickupBlock = requireNonNull(
            VersionHelper.isOrAbove1_21_5()
                    ? ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, clazz$ItemStack, clazz$LivingEntity, clazz$LevelAccessor, clazz$BlockPos, clazz$BlockState)
                    : VersionHelper.isOrAbove1_20_2()
                    ? ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, clazz$ItemStack, clazz$Player, clazz$LevelAccessor, clazz$BlockPos, clazz$BlockState)
                    : ReflectionUtils.getMethod(clazz$SimpleWaterloggedBlock, clazz$ItemStack, clazz$LevelAccessor, clazz$BlockPos, clazz$BlockState)
    );

    public static final Method method$Fluid$defaultFluidState = requireNonNull(
            ReflectionUtils.getMethod(clazz$Fluid, clazz$FluidState, 0)
    );

    public static final Class<?> clazz$SupportType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.EnumBlockSupport",
                    "world.level.block.SupportType"
            )
    );

    public static final Method method$BlockStateBase$isFaceSturdy = requireNonNull(
            ReflectionUtils.getMethod(clazz$BlockStateBase, boolean.class, clazz$BlockGetter, clazz$BlockPos, clazz$Direction, clazz$SupportType)
    );

    public static final Class<?> clazz$Rotation = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.EnumBlockRotation",
                    "world.level.block.Rotation"
            )
    );

    public static final Class<?> clazz$Mirror = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.block.EnumBlockMirror",
                    "world.level.block.Mirror"
            )
    );

    public static final Method method$BlockBehaviour$rotate = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, clazz$BlockState, clazz$BlockState, clazz$Rotation)
    );

    public static final Method method$BlockBehaviour$mirror = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, clazz$BlockState, clazz$BlockState, clazz$Mirror)
    );

    public static final Class<?> clazz$Orientation =
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.redstone.Orientation",
                    "world.level.redstone.Orientation"
            );

    public static final Method method$BlockBehaviour$neighborChanged = requireNonNull(
            VersionHelper.isOrAbove1_21_2() ?
                    ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$Block, clazz$Orientation, boolean.class) :
                    Optional.ofNullable(ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$Block, clazz$BlockPos, boolean.class))
                            .orElse(ReflectionUtils.getMethod(clazz$BlockBehaviour, void.class, clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$Block, clazz$BlockPos, boolean.class))
    );

    // 1.20.2+
    // 从 1.21.2+ 才有 particleStatus
    public static final Class<?> clazz$ClientInformation =
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("server.level.ClientInformation"));

    public static final Method method$BonemealableBlock$performBonemeal = requireNonNull(
            ReflectionUtils.getMethod(clazz$BonemealableBlock, void.class, clazz$ServerLevel, clazz$RandomSource, clazz$BlockPos, clazz$BlockState)
    );
    
    public static final Method method$BlockBehaviour$tick = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"tick", "a"}, clazz$BlockState, clazz$ServerLevel, clazz$BlockPos, clazz$RandomSource)
    );

    public static final Method method$BlockBehaviour$randomTick = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"randomTick", "b"}, clazz$BlockState, clazz$ServerLevel, clazz$BlockPos, clazz$RandomSource)
    );

    public static final Method method$BlockBehaviour$onPlace = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"onPlace", VersionHelper.isOrAbove1_21_5() ? "a" : "b"},
                    clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$BlockState, boolean.class)
    );

    public static final Class<?> clazz$InsideBlockEffectApplier = BukkitReflectionUtils.findReobfOrMojmapClass(
            "world.entity.InsideBlockEffectApplier",
            "world.entity.InsideBlockEffectApplier"
    );

    public static final Method method$BlockBehaviour$entityInside = requireNonNull(
            VersionHelper.isOrAbove1_21_10()
                    ? ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"entityInside", "a"}, clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$Entity, clazz$InsideBlockEffectApplier, boolean.class)
                    : VersionHelper.isOrAbove1_21_5()
                        ? ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"entityInside", "a"}, clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$Entity, clazz$InsideBlockEffectApplier)
                        : ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"entityInside", "a"}, clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$Entity)
    );

    // 1.21.5+
    public static final Method method$BlockBehaviour$affectNeighborsAfterRemoval = ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"affectNeighborsAfterRemoval", "a"}, clazz$BlockState, clazz$ServerLevel, clazz$BlockPos, boolean.class);

    public static final Method method$BlockBehaviour$getSignal = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, int.class, new String[]{"getSignal", "a"}, clazz$BlockState, clazz$BlockGetter, clazz$BlockPos, clazz$Direction)
    );

    public static final Method method$BlockBehaviour$getDirectSignal = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, int.class, new String[]{"getDirectSignal", "b"}, clazz$BlockState, clazz$BlockGetter, clazz$BlockPos, clazz$Direction)
    );

    public static final Method method$BlockBehaviour$isSignalSource = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, boolean.class, new String[]{
                    "isSignalSource",
                    !VersionHelper.isOrAbove1_20_5() ? "f_" : // 1.20.1-1.20.4
                    !VersionHelper.isOrAbove1_21_2() ? "e_" /* 1.20.5-1.21.1 */ : "f_" // 1.21.2+
            }, clazz$BlockState)
    );

    public static final Class<?> clazz$LootParams$Builder = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.storage.loot.LootParams$a",
                    "world.level.storage.loot.LootParams$Builder"
            )
    );

    public static final Method method$BlockStateBase$getDrops = requireNonNull(
            ReflectionUtils.getMethod(clazz$BlockStateBase, List.class, clazz$LootParams$Builder)
    );

    public static final Class<?> clazz$TrimPattern = requireNonNull(
            VersionHelper.isOrAbove1_21_2() ?
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.equipment.trim.TrimPattern",
                    "world.item.equipment.trim.TrimPattern"
            ) :
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.armortrim.TrimPattern",
                    "world.item.armortrim.TrimPattern"
            )
    );

    public static final Class<?> clazz$TrimMaterial = requireNonNull(
            VersionHelper.isOrAbove1_21_2() ?
                    BukkitReflectionUtils.findReobfOrMojmapClass(
                            "world.item.equipment.trim.TrimMaterial",
                            "world.item.equipment.trim.TrimMaterial"
                    ) :
                    BukkitReflectionUtils.findReobfOrMojmapClass(
                            "world.item.armortrim.TrimMaterial",
                            "world.item.armortrim.TrimMaterial"
                    )
    );

    public static final Method method$Block$playerWillDestroy = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    clazz$Block,
                    VersionHelper.isOrAbove1_20_3() ? clazz$BlockState : void.class,
                    clazz$Level, clazz$BlockPos, clazz$BlockState, clazz$Player
            )
    );

    public static final Class<?> clazz$BlockItem = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.ItemBlock",
                    "world.item.BlockItem"
            )
    );

    public static final Method method$BlockBehaviour$spawnAfterBreak = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, clazz$BlockState, clazz$ServerLevel, clazz$BlockPos, clazz$ItemStack, boolean.class)
    );

    // 1.20~1.21.4
    public static final Method method$BlockBehaviour$onRemove = MiscUtils.requireNonNullIf(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"a", "onRemove"}, clazz$BlockState, clazz$Level, clazz$BlockPos, clazz$BlockState, boolean.class),
            !VersionHelper.isOrAbove1_21_5()
    );

    public static final Class<?> clazz$DyeItem = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.ItemDye",
                    "world.item.DyeItem"
            )
    );

    public static final Class<?> clazz$CraftingBookCategory = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.CraftingBookCategory",
                    "world.item.crafting.CraftingBookCategory"
            )
    );

    public static final Class<?> clazz$CraftingInput = MiscUtils.requireNonNullIf(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.crafting.CraftingInput",
                    "world.item.crafting.CraftingInput"
            ), VersionHelper.isOrAbove1_21()
    );

    public static final Class<?> clazz$CraftingContainer = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.inventory.InventoryCrafting",
                    "world.inventory.CraftingContainer"
            )
    );

    public static final Class<?> clazz$DyeableLeatherItem = MiscUtils.requireNonNullIf(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.IDyeable",
                    "world.item.DyeableLeatherItem"
            ), !VersionHelper.isOrAbove1_20_5()
    );

    public static final Class<?> clazz$LootPoolEntryType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.storage.loot.entries.LootEntryType",
                    "world.level.storage.loot.entries.LootPoolEntryType"
            )
    );

    public static final Method method$Block$fallOn = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$Block, void.class, clazz$Level, clazz$BlockState, clazz$BlockPos, clazz$Entity, VersionHelper.isOrAbove1_21_5() ? double.class : float.class)
    );

    public static final Method method$Block$updateEntityMovementAfterFallOn = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$Block, void.class, clazz$BlockGetter, clazz$Entity)
    );

    public static final Class<?> clazz$GameEvent = requireNonNull(
            ReflectionUtils.getClazz(
                    BukkitReflectionUtils.assembleMCClass("world.level.gameevent.GameEvent")
            )
    );

    public static final Method method$BlockStateBase$isBlock = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockStateBase, boolean.class, new String[]{"is", "a"}, clazz$Block)
    );

    public static final Class<?> clazz$Projectile = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.entity.projectile.IProjectile",
                    "world.entity.projectile.Projectile"
            )
    );

    public static final Method method$BlockBehaviour$onProjectileHit = requireNonNull(
            ReflectionUtils.getDeclaredMethod(clazz$BlockBehaviour, void.class, new String[]{"onProjectileHit", "a"}, clazz$Level, clazz$BlockState, clazz$BlockHitResult, clazz$Projectile)
    );

    public static final Method method$Block$setPlacedBy = requireNonNull(
            ReflectionUtils.getMethod(clazz$Block, void.class, clazz$Level, clazz$BlockPos, clazz$BlockState, clazz$LivingEntity, clazz$ItemStack)
    );

    public static final Class<?> clazz$ItemCost = MiscUtils.requireNonNullIf(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.item.trading.ItemCost",
                    "world.item.trading.ItemCost"
            ), VersionHelper.isOrAbove1_20_5()
    );

    public static final Class<?> clazz$BlockEntityType = requireNonNull(BukkitReflectionUtils.findReobfOrMojmapClass(
            "world.level.block.entity.TileEntityTypes",
            "world.level.block.entity.BlockEntityType"
    ));

    public static final Class<?> clazz$BlockStateProviderType = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProviders",
                    "world.level.levelgen.feature.stateproviders.BlockStateProviderType"
            )
    );

    public static final Class<?> clazz$Feature = requireNonNull(
            BukkitReflectionUtils.findReobfOrMojmapClass(
                    "world.level.levelgen.feature.WorldGenerator",
                    "world.level.levelgen.feature.Feature"
            )
    );
}
