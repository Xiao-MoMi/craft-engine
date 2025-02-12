package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.block.PushReaction;
import net.momirealms.craftengine.core.util.Instrument;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MapColor;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockPhysicsEvent;

import java.util.IdentityHashMap;

public class BlockStateUtils {
    public static final IdentityHashMap<Object, Object> CLIENT_SIDE_NOTE_BLOCKS = new IdentityHashMap<>();
    private static int vanillaStateSize;
    private static boolean hasInit;

    public static void init(int size) {
        if (hasInit) {
            throw new IllegalStateException("BlockStateUtils has already been initialized");
        }
        vanillaStateSize = size;
        hasInit = true;
    }

    public static int blockDataToId(BlockData blockData) {
        try {
            Object blockState = Reflections.field$CraftBlockData$data.get(blockData);
            return (int) Reflections.method$IdMapper$getId.invoke(Reflections.instance$BLOCK_STATE_REGISTRY, blockState);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Key getRealBlockId(Block block) {
        BlockData data = block.getBlockData();
        Object blockState = blockDataToBlockState(data);
        return getRealBlockIdFromState(blockState);
    }

    public static Key getRealBlockIdFromState(Object blockState) {
        String id = blockState.toString();
        int first = id.indexOf('{');
        int last = id.indexOf('}');
        if (first != -1 && last != -1 && last > first) {
            String blockId = id.substring(first + 1, last);
            return Key.of(blockId);
        } else {
            throw new IllegalArgumentException("Invalid block ID format: " + id);
        }
    }

    public static Object blockDataToBlockState(BlockData blockData) {
        try {
            return Reflections.field$CraftBlockData$data.get(blockData);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object idToBlockState(int id) {
        try {
            return Reflections.method$IdMapper$byId.invoke(Reflections.instance$BLOCK_STATE_REGISTRY, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static int blockStateToId(Object blockState) {
        try {
            return (int) Reflections.method$IdMapper$getId.invoke(Reflections.instance$BLOCK_STATE_REGISTRY, blockState);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getBlockOwner(Object blockState) {
        try {
            return Reflections.field$StateHolder$owner.get(blockState);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static int physicsEventToId(BlockPhysicsEvent event) throws ReflectiveOperationException {
        Object blockData = Reflections.field$BlockPhysicsEvent$changed.get(event);
        Object blockState = Reflections.field$CraftBlockData$data.get(blockData);
        return (int) Reflections.method$IdMapper$getId.invoke(Reflections.instance$BLOCK_STATE_REGISTRY, blockState);
    }

    public static Object physicsEventToState(BlockPhysicsEvent event) throws ReflectiveOperationException {
        Object blockData = Reflections.field$BlockPhysicsEvent$changed.get(event);
        return Reflections.field$CraftBlockData$data.get(blockData);
    }

    public static void setLightEmission(Object state, int emission) throws ReflectiveOperationException {
        Reflections.field$BlockStateBase$lightEmission.set(state, emission);
    }

    public static int getLightEmission(Object state) throws ReflectiveOperationException {
        return (int) Reflections.field$BlockStateBase$lightEmission.get(state);
    }

    public static void setMapColor(Object state, MapColor color) throws ReflectiveOperationException {
        Object mcMapColor = Reflections.method$MapColor$byId.invoke(null, color.id);
        Reflections.field$BlockStateBase$mapColor.set(state, mcMapColor);
    }

    public static void setInstrument(Object state, Instrument instrument) throws ReflectiveOperationException {
        Object mcInstrument = ((Object[]) Reflections.method$NoteBlockInstrument$values.invoke(null))[instrument.ordinal()];
        Reflections.field$BlockStateBase$instrument.set(state, mcInstrument);
    }

    public static void setHardness(Object state, float hardness) throws ReflectiveOperationException {
        Reflections.field$BlockStateBase$hardness.set(state, hardness);
    }

    public static void setBurnable(Object state, boolean burnable) throws ReflectiveOperationException {
        Reflections.field$BlockStateBase$burnable.set(state, burnable);
    }

    public static void setPushReaction(Object state, PushReaction reaction) throws ReflectiveOperationException {
        Object pushReaction = ((Object[])  Reflections.method$PushReaction$values.invoke(null))[reaction.ordinal()];
        Reflections.field$BlockStateBase$pushReaction.set(state, pushReaction);
    }

    public static void setIsRandomlyTicking(Object state, boolean randomlyTicking) throws ReflectiveOperationException {
        Reflections.field$BlockStateBase$isRandomlyTicking.set(state, randomlyTicking);
    }

    public static void setReplaceable(Object state, boolean replaceable) throws ReflectiveOperationException {
        Reflections.field$BlockStateBase$replaceable.set(state, replaceable);
    }

    public static boolean isReplaceable(Object state) throws ReflectiveOperationException {
        return (boolean) Reflections.field$BlockStateBase$replaceable.get(state);
    }

    public static void setCanOcclude(Object state, boolean canOcclude) throws ReflectiveOperationException {
        Reflections.field$BlockStateBase$canOcclude.set(state, canOcclude);
    }

    public static boolean isOcclude(Object state) throws ReflectiveOperationException {
        return (boolean) Reflections.field$BlockStateBase$canOcclude.get(state);
    }

    public static void setIsRedstoneConductor(Object state, Object predicate) throws ReflectiveOperationException {
        Reflections.field$BlockStateBase$isRedstoneConductor.set(state, predicate);
    }

    public static void setIsSuffocating(Object state, Object predicate) throws ReflectiveOperationException {
        Reflections.field$BlockStateBase$isSuffocating.set(state, predicate);
    }

    public static void setIsViewBlocking(Object state, Object predicate) throws ReflectiveOperationException {
        Reflections.field$BlockStateBase$isViewBlocking.set(state, predicate);
    }

    public static boolean isClientSideNoteBlock(Object state) {
        return CLIENT_SIDE_NOTE_BLOCKS.containsKey(state);
    }

    public static boolean isVanillaBlock(Object state) {
        int id = blockStateToId(state);
        return id >= 0 && id < vanillaStateSize;
    }

    public static boolean isVanillaBlock(int id) {
        return id >= 0 && id < vanillaStateSize;
    }

    public static int vanillaStateSize() {
        return vanillaStateSize;
    }
}
