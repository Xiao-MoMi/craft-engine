package net.momirealms.craftengine.proxy.minecraft.world.entity.vehicle.boat;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {"net.minecraft.world.entity.vehicle.boat.Boat", "net.minecraft.world.entity.vehicle.Boat"})
public interface BoatProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.entity.vehicle.boat.Boat", "net.minecraft.world.entity.vehicle.Boat");
}
