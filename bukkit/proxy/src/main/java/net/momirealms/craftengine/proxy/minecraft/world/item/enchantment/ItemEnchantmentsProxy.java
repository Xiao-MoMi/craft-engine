package net.momirealms.craftengine.proxy.minecraft.world.item.enchantment;

import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.enchantment.ItemEnchantments", activeIf = "min_version=1.20.5")
public interface ItemEnchantmentsProxy {
    ItemEnchantmentsProxy INSTANCE = ASMProxyFactory.create(ItemEnchantmentsProxy.class);

    @FieldGetter(name = "enchantments")
    Object2IntAVLTreeMap<Object> getEnchantments(Object target);
}
