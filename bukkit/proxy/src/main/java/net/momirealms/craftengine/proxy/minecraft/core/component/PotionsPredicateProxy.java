package net.momirealms.craftengine.proxy.minecraft.core.component;

import com.mojang.serialization.Codec;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.IngredientProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.core.component.predicates.PotionsPredicate", activeIf = "min_version=26.3")
public interface PotionsPredicateProxy {
    PotionsPredicateProxy INSTANCE = ASMProxyFactory.create(PotionsPredicateProxy.class);

    @FieldGetter(name = "CODEC", isStatic = true)
    Codec<Object> codec();

    @MethodInvoker(name = "potions")
    Optional<Iterable<Object>> potions(Object target);

    @ReflectionProxy(name = "net.minecraft.core.component.predicates.DataComponentPredicate", activeIf = "min_version=26.3")
    interface Predicate {
        Predicate INSTANCE = ASMProxyFactory.create(Predicate.class);

        @MethodInvoker(name = "matches")
        boolean matches(Object target, @Type(clazz = DataComponentGetterProxy.class) Object components);
    }

    @ReflectionProxy(name = "net.minecraft.world.item.alchemy.PotionContents", activeIf = "min_version=26.3")
    interface Contents {
        Contents INSTANCE = ASMProxyFactory.create(Contents.class);

        @ConstructorInvoker
        Object newInstance(@Type(clazz = HolderProxy.class) Object potion);
    }

    @ReflectionProxy(name = "net.minecraft.world.item.crafting.PotionIngredient", activeIf = "min_version=26.3")
    interface PotionIngredient {
        PotionIngredient INSTANCE = ASMProxyFactory.create(PotionIngredient.class);

        @ConstructorInvoker
        Object newInstance(@Type(clazz = IngredientProxy.class) Object ingredient, Optional<?> predicates);
    }
}
