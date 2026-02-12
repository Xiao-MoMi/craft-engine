package net.momirealms.craftengine.proxy.minecraft.world.item;

import com.mojang.serialization.Codec;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentPatchProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.nbt.CompoundTagProxy;
import net.momirealms.craftengine.proxy.minecraft.network.codec.StreamCodecProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.TagKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EquipmentSlotProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ItemLikeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.pattern.BlockInWorldProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.function.Consumer;

@ReflectionProxy(name = "net.minecraft.world.item.ItemStack")
public interface ItemStackProxy extends DataComponentHolderProxy {
    ItemStackProxy INSTANCE = ASMProxyFactory.create(ItemStackProxy.class);
    Object EMPTY = INSTANCE.getEmpty();

    @ConstructorInvoker
    Object newInstance(@Type(clazz = ItemLikeProxy.class) Object item, int count);

    @FieldGetter(name = "EMPTY", isStatic = true)
    Object getEmpty();

    @FieldGetter(name = "CODEC", isStatic = true, activeIf = "min_version=1.20.5")
    Codec<Object> getCodec();

    @FieldGetter(name = "OPTIONAL_STREAM_CODEC", isStatic = true, activeIf = "min_version=1.20.5")
    Object getOptionalStreamCodec();

    @MethodInvoker(name = "hurtAndBreak", activeIf = "min_version=1.20.5")
    void hurtAndBreak(Object target, int amount, @Type(clazz = LivingEntityProxy.class) Object entity, @Type(clazz = EquipmentSlotProxy.class) Object slot);

    @MethodInvoker(name = "hurtAndBreak", activeIf = "max_version=1.20.4")
    Object hurtAndBreak(Object target, int amount, @Type(clazz = LivingEntityProxy.class) Object entity, Consumer<Object> breakCallback);

    @MethodInvoker(name = "of", isStatic = true, activeIf = "max_version=1.20.4")
    Object of(@Type(clazz = CompoundTagProxy.class) Object nbt);

    @MethodInvoker(name = "is")
    boolean is(Object target, @Type(clazz = TagKeyProxy.class) Object tag);

    @MethodInvoker(name = "getTag", activeIf = "max_version=1.20.4")
    Object getTag(Object target);

    @MethodInvoker(name = "setTag", activeIf = "max_version=1.20.4")
    void setTag(Object target, @Type(clazz = CompoundTagProxy.class) Object nbt);

    @MethodInvoker(name = "save", activeIf = "max_version=1.20.4")
    Object save(Object target, @Type(clazz = CompoundTagProxy.class) Object nbt);

    @MethodInvoker(name = "isEmpty")
    boolean isEmpty(Object target);

    @MethodInvoker(name = "getOrCreateTag", activeIf = "max_version=1.20.4")
    Object getOrCreateTag(Object target);

    @MethodInvoker(name = "set", activeIf = "min_version=1.20.5")
    void set(Object target, @Type(clazz = DataComponentTypeProxy.class) Object type, Object value);

    @MethodInvoker(name = "remove", activeIf = "min_version=1.20.5")
    <T> T remove(Object target, @Type(clazz = DataComponentTypeProxy.class) Object type);

    @MethodInvoker(name = "getItem")
    Object getItem(Object target);

    @MethodInvoker(name = "hasNonDefault", activeIf = "min_version=1.21.4")
    boolean hasNonDefault(Object target, @Type(clazz = DataComponentTypeProxy.class) Object type);

    @MethodInvoker(name = "getComponentsPatch", activeIf = "min_version=1.20.5")
    Object getComponentsPatch(Object target);

    @MethodInvoker(name = "transmuteCopy", activeIf = "min_version=1.20.5")
    Object transmuteCopy(Object target, @Type(clazz = ItemLikeProxy.class) Object item, int count);

    @MethodInvoker(name = "applyComponents", activeIf = "min_version=1.20.5")
    void applyComponents(Object target, @Type(clazz = DataComponentPatchProxy.class) Object changes);

    @MethodInvoker(name = "isCorrectToolForDrops")
    boolean isCorrectToolForDrops(Object target, @Type(clazz = BlockStateProxy.class) Object state);

    @MethodInvoker(name = "canBreakBlockInAdventureMode", activeIf = "min_version=1.20.5")
    boolean canBreakBlockInAdventureMode(Object target, @Type(clazz = BlockInWorldProxy.class) Object block);

    @MethodInvoker(name = "canPlaceOnBlockInAdventureMode", activeIf = "min_version=1.20.5")
    boolean canPlaceOnBlockInAdventureMode(Object target, @Type(clazz = BlockInWorldProxy.class) Object block);

    @MethodInvoker(name = "hasAdventureModeBreakTagForBlock", activeIf = "max_version=1.20.4")
    boolean hasAdventureModeBreakTagForBlock(Object target, @Type(clazz = RegistryProxy.class) Object blockRegistry, @Type(clazz = BlockInWorldProxy.class) Object block);

    @MethodInvoker(name = "hasAdventureModePlaceTagForBlock", activeIf = "max_version=1.20.4")
    boolean hasAdventureModePlaceTagForBlock(Object target, @Type(clazz = RegistryProxy.class) Object blockRegistry, @Type(clazz = BlockInWorldProxy.class) Object block);

    @MethodInvoker(name = "validatedStreamCodec", isStatic = true, activeIf = "min_version=1.20.5")
    Object validatedStreamCodec(@Type(clazz = StreamCodecProxy.class) Object basePacketCodec);
}
