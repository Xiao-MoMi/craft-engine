package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.Blocks")
public interface BlocksProxy {
    BlocksProxy INSTANCE = ASMProxyFactory.create(BlocksProxy.class);
    Object COBWEB = INSTANCE.getCobweb();
    Object AIR = INSTANCE.getAir();
    Object STONE = INSTANCE.getStone();
    Object FIRE = INSTANCE.getFire();
    Object ICE = INSTANCE.getIce();
    Object SHORT_GRASS = INSTANCE.getShortGrass();
    Object COMPOSTER = INSTANCE.getComposter();
    Object SNOW = INSTANCE.getSnow();
    Object WATER = INSTANCE.getWater();
    Object TNT = INSTANCE.getTnt();
    Object BARRIER = INSTANCE.getBarrier();
    Object CARVED_PUMPKIN = INSTANCE.getCarvedPumpkin();
    Object JACK_O_LANTERN = INSTANCE.getJackOLantern();
    Object MELON = INSTANCE.getMelon();
    Object PUMPKIN = INSTANCE.getPumpkin();
    Object AIR$defaultState = BlockProxy.INSTANCE.getDefaultBlockState(AIR);
    Object STONE$defaultState = BlockProxy.INSTANCE.getDefaultBlockState(STONE);
    Object WATER$defaultState = BlockProxy.INSTANCE.getDefaultBlockState(WATER);
    Object TNT$defaultState = BlockProxy.INSTANCE.getDefaultBlockState(TNT);

    @FieldGetter(name = "COBWEB", isStatic = true)
    Object getCobweb();

    @FieldGetter(name = "AIR", isStatic = true)
    Object getAir();

    @FieldGetter(name = "STONE", isStatic = true)
    Object getStone();

    @FieldGetter(name = "FIRE", isStatic = true)
    Object getFire();

    @FieldGetter(name = "ICE", isStatic = true)
    Object getIce();

    @FieldGetter(name = {"SHORT_GRASS", "GRASS"}, isStatic = true)
    Object getShortGrass();

    @FieldGetter(name = "COMPOSTER", isStatic = true)
    Object getComposter();

    @FieldGetter(name = "SNOW", isStatic = true)
    Object getSnow();

    @FieldGetter(name = "WATER", isStatic = true)
    Object getWater();

    @FieldGetter(name = "TNT", isStatic = true)
    Object getTnt();

    @FieldGetter(name = "BARRIER", isStatic = true)
    Object getBarrier();

    @FieldGetter(name = "CARVED_PUMPKIN", isStatic = true)
    Object getCarvedPumpkin();

    @FieldGetter(name = "JACK_O_LANTERN", isStatic = true)
    Object getJackOLantern();

    @FieldGetter(name = "MELON", isStatic = true)
    Object getMelon();

    @FieldGetter(name = "PUMPKIN", isStatic = true)
    Object getPumpkin();

}
