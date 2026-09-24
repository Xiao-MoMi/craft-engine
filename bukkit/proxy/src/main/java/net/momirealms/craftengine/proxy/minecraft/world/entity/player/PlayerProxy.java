package net.momirealms.craftengine.proxy.minecraft.world.entity.player;

import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.entity.player.Player")
public interface PlayerProxy extends LivingEntityProxy {
    PlayerProxy INSTANCE = ASMProxyFactory.create(PlayerProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.entity.player.Player");

    @FieldGetter(name = "containerMenu")
    Object getContainerMenu(Object target);

    @FieldSetter(name = "containerMenu")
    void setContainerMenu(Object target, Object containerMenu);

    @FieldGetter(name = "inventoryMenu")
    Object getInventoryMenu(Object target);

    @FieldSetter(name = "inventoryMenu")
    void setInventoryMenu(Object target, Object inventoryMenu);

    @FieldGetter(name = "inventory")
    Object getInventory(Object target);

    @FieldSetter(name = "inventory")
    void setInventory(Object target, Object inventory);

    @FieldGetter(name = "abilities")
    Object getAbilities(Object target);

    @FieldSetter(name = "abilities")
    void setAbilities(Object target, Object abilities);

    @MethodInvoker(name = "drop", activeIf = "max_version=1.20.2")
    Object drop(Object target, @Type(clazz = ItemStackProxy.class) Object droppedItem, boolean dropAround, boolean traceItem, boolean callEvent);

    @MethodInvoker(name = "drop", activeIf = "max_version=26.2")
    default Object drop(Object target, @Type(clazz = ItemStackProxy.class) Object droppedItem, boolean retainOwnership) {
        return drop(target, droppedItem, retainOwnership, net.momirealms.craftengine.proxy.minecraft.util.PredictionProxy.SERVER_ONLY);
    }

    @MethodInvoker(name = "getCooldowns")
    Object getCooldowns(Object target);

    @MethodInvoker(name = "getXpNeededForNextLevel")
    int getXpNeededForNextLevel(Object target);

    @MethodInvoker(name = "getAttackStrengthScale")
    float getAttackStrengthScale(Object target, float adjustTicks);
}
