package net.momirealms.craftengine.proxy.minecraft.world.level.block.state;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.level.block.state.BlockBehaviour")
public interface BlockBehaviourProxy {
    BlockBehaviourProxy INSTANCE = ASMProxyFactory.create(BlockBehaviourProxy.class);

    @FieldGetter(name = "hasCollision")
    boolean hasCollision(Object target);

    @FieldSetter(name = "hasCollision")
    void setHasCollision(Object target, boolean hasCollision);

    @FieldGetter(name = "isRandomlyTicking")
    boolean isRandomlyTicking(Object target);

    @FieldSetter(name = "isRandomlyTicking")
    void setIsRandomlyTicking(Object target, boolean isRandomlyTicking);

    @FieldGetter(name = "explosionResistance")
    float getExplosionResistance(Object target);

    @FieldSetter(name = "explosionResistance")
    void setExplosionResistance(Object target, float resistance);

    @FieldGetter(name = "soundType")
    Object getSoundType(Object target);

    @FieldSetter(name = "soundType")
    void setSoundType(Object target, Object soundType);

    @FieldGetter(name = "friction")
    float getFriction(Object target);

    @FieldSetter(name = "friction")
    void setFriction(Object target, float friction);

    @FieldGetter(name = "speedFactor")
    float getSpeedFactor(Object target);

    @FieldSetter(name = "speedFactor")
    void setSpeedFactor(Object target, float speedFactor);

    @FieldGetter(name = "jumpFactor")
    float getJumpFactor(Object target);

    @FieldSetter(name = "jumpFactor")
    void setJumpFactor(Object target, float jumpFactor);

    @FieldGetter(name = "descriptionId", activeIf = "min_version=1.21.2")
    String getDescriptionId(Object target);

    @FieldSetter(name = "descriptionId", activeIf = "min_version=1.21.2")
    void setDescriptionId(Object target, String descriptionId);

    @ReflectionProxy(name = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase")
    interface BlockStateBaseProxy extends StateHolderProxy {
        BlockStateBaseProxy INSTANCE = ASMProxyFactory.create(BlockStateBaseProxy.class);

        @FieldGetter(name = "lightEmission")
        int getLightEmission(Object target);

        @FieldSetter(name = "lightEmission")
        void setLightEmission(Object target, int lightEmission);

        @FieldGetter(name = "useShapeForLightOcclusion")
        boolean isUseShapeForLightOcclusion(Object target);

        @FieldSetter(name = "useShapeForLightOcclusion")
        void setUseShapeForLightOcclusion(Object target, boolean useShapeForLightOcclusion);

        @FieldGetter(name = "isAir")
        boolean isAir(Object target);

        @FieldSetter(name = "isAir")
        void setIsAir(Object target, boolean isAir);

        @FieldGetter(name = "ignitedByLava")
        boolean isIgnitedByLava(Object target);

        @FieldSetter(name = "ignitedByLava")
        void setIgnitedByLava(Object target, boolean ignitedByLava);

        @FieldGetter(name = "liquid")
        boolean isLiquid(Object target);

        @FieldSetter(name = "liquid")
        void setLiquid(Object target, boolean liquid);

        @FieldGetter(name = "legacySolid")
        boolean isLegacySolid(Object target);

        @FieldSetter(name = "legacySolid")
        void setLegacySolid(Object target, boolean legacySolid);

        @FieldGetter(name = "pushReaction")
        Object getPushReaction(Object target);

        @FieldSetter(name = "pushReaction")
        void setPushReaction(Object target, Object pushReaction);

        @FieldGetter(name = "mapColor")
        Object getMapColor(Object target);

        @FieldSetter(name = "mapColor")
        void setMapColor(Object target, Object mapColor);

        @FieldGetter(name = "destroySpeed")
        float getDestroySpeed(Object target);

        @FieldSetter(name = "destroySpeed")
        void setDestroySpeed(Object target, float destroySpeed);

        @FieldGetter(name = "requiresCorrectToolForDrops")
        boolean isRequiresCorrectToolForDrops(Object target);

        @FieldSetter(name = "requiresCorrectToolForDrops")
        void setRequiresCorrectToolForDrops(Object target, boolean requiresCorrectToolForDrops);

        @FieldGetter(name = "canOcclude")
        boolean isCanOcclude(Object target);

        @FieldSetter(name = "canOcclude")
        void setCanOcclude(Object target, boolean canOcclude);

        @FieldGetter(name = "isRedstoneConductor")
        Object getIsRedstoneConductor(Object target);

        @FieldSetter(name = "isRedstoneConductor")
        void setIsRedstoneConductor(Object target, Object isRedstoneConductor);

        @FieldGetter(name = "isSuffocating")
        Object getIsSuffocating(Object target);

        @FieldSetter(name = "isSuffocating")
        void setIsSuffocating(Object target, Object isSuffocating);

        @FieldGetter(name = "isViewBlocking")
        Object getIsViewBlocking(Object target);

        @FieldSetter(name = "isViewBlocking")
        void setIsViewBlocking(Object target, Object isViewBlocking);

        @FieldGetter(name = "hasPostProcess")
        Object getHasPostProcess(Object target);

        @FieldSetter(name = "hasPostProcess")
        void setHasPostProcess(Object target, Object hasPostProcess);

        @FieldGetter(name = "emissiveRendering")
        Object getEmissiveRendering(Object target);

        @FieldSetter(name = "emissiveRendering")
        void setEmissiveRendering(Object target, Object emissiveRendering);

        @FieldGetter(name = "offsetFunction")
        Object getOffsetFunction(Object target);

        @FieldSetter(name = "offsetFunction")
        void setOffsetFunction(Object target, Object offsetFunction);

        @FieldGetter(name = "spawnTerrainParticles")
        boolean isSpawnTerrainParticles(Object target);

        @FieldSetter(name = "spawnTerrainParticles")
        void setSpawnTerrainParticles(Object target, boolean spawnTerrainParticles);

        @FieldGetter(name = "instrument")
        Object getInstrument(Object target);

        @FieldSetter(name = "instrument")
        void setInstrument(Object target, Object instrument);

        @FieldGetter(name = "replaceable")
        boolean isReplaceable(Object target);

        @FieldSetter(name = "replaceable")
        void setReplaceable(Object target, boolean replaceable);

        @FieldGetter(name = "fluidState")
        Object getFluidState(Object target);

        @FieldSetter(name = "fluidState")
        void setFluidState(Object target, Object fluidState);

        @FieldGetter(name = "isRandomlyTicking")
        boolean isRandomlyTicking(Object target);

        @FieldSetter(name = "isRandomlyTicking")
        void setIsRandomlyTicking(Object target, boolean isRandomlyTicking);

        @FieldGetter(name = "solidRender", activeIf = "min_version=1.21.2")
        boolean isSolidRender(Object target);

        @FieldSetter(name = "solidRender", activeIf = "min_version=1.21.2")
        void setSolidRender(Object target, boolean solidRender);

        @FieldGetter(name = "occlusionShape", activeIf = "min_version=1.21.2")
        Object getOcclusionShape(Object target);

        @FieldSetter(name = "occlusionShape", activeIf = "min_version=1.21.2")
        void setOcclusionShape(Object target, Object occlusionShape);

        @FieldGetter(name = "propagatesSkylightDown", activeIf = "min_version=1.21.2")
        boolean isPropagatesSkylightDown(Object target);

        @FieldSetter(name = "propagatesSkylightDown", activeIf = "min_version=1.21.2")
        void setPropagatesSkylightDown(Object target, boolean propagatesSkylightDown);

        @FieldGetter(name = "lightBlock", activeIf = "min_version=1.21.2")
        int getLightBlock$0(Object target);

        @FieldSetter(name = "lightBlock", activeIf = "min_version=1.21.2")
        void setLightBlock(Object target, int lightBlock);

        @FieldGetter(name = "occludesFullBlock")
        boolean isOccludesFullBlock(Object target);

        @FieldSetter(name = "occludesFullBlock")
        void setOccludesFullBlock(Object target, boolean occludesFullBlock);

        @FieldGetter(name = "emptyCollisionShape")
        boolean isEmptyCollisionShape(Object target);

        @FieldSetter(name = "emptyCollisionShape")
        void setEmptyCollisionShape(Object target, boolean emptyCollisionShape);

        @FieldGetter(name = "shapeExceedsCube")
        boolean isShapeExceedsCube(Object target);

        @FieldSetter(name = "shapeExceedsCube")
        void setShapeExceedsCube(Object target, boolean shapeExceedsCube);

        @FieldGetter(name = "cache")
        Object getCache(Object target);

        @FieldSetter(name = "cache")
        void setCache(Object target, Object cache);

        @FieldGetter(name = "conditionallyFullOpaque", activeIf = "max_version=1.21.1")
        boolean isConditionallyFullOpaque(Object target);

        @FieldSetter(name = "conditionallyFullOpaque", activeIf = "max_version=1.21.1")
        void setConditionallyFullOpaque(Object target, boolean conditionallyFullOpaque);

        @FieldGetter(name = "opacityIfCached", activeIf = "max_version=1.21.1")
        int getOpacityIfCached(Object target);

        @FieldSetter(name = "opacityIfCached", activeIf = "max_version=1.21.1")
        void setOpacityIfCached(Object target, int opacityIfCached);

        @MethodInvoker(name = "initCache")
        void initCache(Object target);

        @MethodInvoker(name = "getLightBlock", activeIf = "min_version=1.21.2")
        int getLightBlock$1(Object target);

        @MethodInvoker(name = "getLightBlock", activeIf = "max_version=1.21.1")
        int getLightBlock(Object target, @Type(clazz = BlockGetterProxy.class) Object world, @Type(clazz = BlockPosProxy.class) Object pos);

        @MethodInvoker(name = "randomTick")
        void randomTick(Object target, @Type(clazz = ServerLevelProxy.class) Object world, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = RandomSourceProxy.class) Object random);

        @MethodInvoker(name = "getSoundType")
        Object getSoundType(Object target);

        @ReflectionProxy(name = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase$Cache")
        interface CacheProxy {
            CacheProxy INSTANCE = ASMProxyFactory.create(CacheProxy.class);

            @FieldGetter(name = "lightBlock", activeIf = "max_version=1.21.1")
            int getLightBlock(Object target);

            @FieldSetter(name = "lightBlock", activeIf = "max_version=1.21.1")
            void setLightBlock(Object target, int lightBlock);

            @FieldGetter(name = "propagatesSkylightDown", activeIf = "max_version=1.21.1")
            boolean propagatesSkylightDown(Object target);

            @FieldSetter(name = "propagatesSkylightDown", activeIf = "max_version=1.21.1")
            void setPropagatesSkylightDown(Object target, boolean propagatesSkylightDown);
        }
    }

    @ReflectionProxy(name = "net.minecraft.world.level.block.state.BlockBehaviour$Properties")
    interface PropertiesProxy {
        PropertiesProxy INSTANCE = ASMProxyFactory.create(PropertiesProxy.class);

        @MethodInvoker(name = "of", isStatic = true)
        Object of();

        @FieldSetter(name = "id", activeIf = "min_version=1.21.2")
        void setId(Object target, Object id);
    }
}
