package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.InteractionHandProxy;
import net.momirealms.craftengine.proxy.minecraft.world.effect.MobEffectInstanceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.effect.MobEffectProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.entity.LivingEntity")
public interface LivingEntityProxy extends EntityProxy {
    LivingEntityProxy INSTANCE = ASMProxyFactory.create(LivingEntityProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.entity.LivingEntity");

    @MethodInvoker(name = "drop", activeIf = "min_version=26.3 && has_patch=paper")
    Object drop(Object target, @Type(clazz = ItemStackProxy.class) Object droppedItem, boolean thrownFromHand, @Type(clazz = net.momirealms.craftengine.proxy.minecraft.util.PredictionProxy.class) Object prediction, boolean randomly, boolean callEvent, @org.jetbrains.annotations.Nullable java.util.function.Consumer<org.bukkit.entity.Item> entityOperation);

    @FieldGetter(name = "lastDamageSource")
    Object getLastDamageSource(Object target);

    /**
     * @return 1.21.5+ {@code EntityReference<Player>}</br>1.21.5- {@code Player}
     */
    @FieldGetter(name = "lastHurtByPlayer")
    Object getLastHurtByPlayerField(Object target);

    @MethodInvoker(name = "getLocalBoundsForPose")
    Object getLocalBoundsForPose(Object target, @Type(clazz = PoseProxy.class) Object pos);

    @MethodInvoker(name = "getAttribute", activeIf = "max_version=1.20.4")
    Object getAttribute$legacy(Object target, @Type(clazz = AttributeProxy.class) Object attribute);

    @MethodInvoker(name = "getAttribute", activeIf = "min_version=1.20.5")
    Object getAttribute(Object target, @Type(clazz = HolderProxy.class) Object attribute);

    @MethodInvoker(name = "getEffect", activeIf = "max_version=1.20.4")
    Object getEffect$legacy(Object target, @Type(clazz = MobEffectProxy.class) Object effect);

    @MethodInvoker(name = "getEffect", activeIf = "min_version=1.20.5")
    Object getEffect(Object target, @Type(clazz = HolderProxy.class) Object effect);

    @MethodInvoker(name = "getScale")
    float getScale(Object target);

    @MethodInvoker(name = "broadcastBreakEvent", activeIf = "max_version=1.20.6")
    void broadcastBreakEvent(Object target, @Type(clazz = EquipmentSlotProxy.class) Object slot);

    @MethodInvoker(name = "startUsingItem")
    void startUsingItem(Object target, @Type(clazz = InteractionHandProxy.class) Object hand);

    @MethodInvoker(name = "getLastHurtByPlayer", activeIf = "min_version=1.21.5")
    Object getLastHurtByPlayer(Object target);

    @MethodInvoker(name = "swing", activeIf = "max_version=26.2")
    default void swing(Object target, @Type(clazz = InteractionHandProxy.class) Object hand, boolean updateSelf) {
        swing(target, hand, net.momirealms.craftengine.proxy.minecraft.world.item.component.SwingAnimationProxy.DEFAULT, updateSelf);
    }

    @MethodInvoker(name = "swing", activeIf = "min_version=26.3")
    boolean swing(Object target, @Type(clazz = InteractionHandProxy.class) Object hand, @Type(clazz = net.momirealms.craftengine.proxy.minecraft.world.item.component.SwingAnimationProxy.class) Object animation, boolean updateSelf);

    @MethodInvoker(name = "addEffect")
    boolean addEffect(Object target, @Type(clazz = MobEffectInstanceProxy.class) Object effect);

    @MethodInvoker(name = "removeEffect", activeIf = "min_version=1.20.5")
    boolean removeEffect(Object target, @Type(clazz = HolderProxy.class) Object effect);

    @MethodInvoker(name = "removeEffect", activeIf = "max_version=1.20.4")
    boolean removeEffect$legacy(Object target, @Type(clazz = MobEffectProxy.class) Object effect);

    @MethodInvoker(name = "removeAllEffects")
    boolean removeAllEffects(Object target);

    @MethodInvoker(name = "getItemBySlot")
    Object getItemBySlot(Object target, @Type(clazz = EquipmentSlotProxy.class) Object slot);

    @MethodInvoker(name = "setItemSlot")
    void setItemSlot(Object target, @Type(clazz = EquipmentSlotProxy.class) Object slot, @Type(clazz = ItemStackProxy.class) Object item);
    @MethodInvoker(name = "drop", activeIf = "min_version=26.3")
    Object drop(Object target, @Type(clazz = ItemStackProxy.class) Object droppedItem, boolean dropAround, @Type(clazz = net.momirealms.craftengine.proxy.minecraft.util.PredictionProxy.class) Object prediction);


}
