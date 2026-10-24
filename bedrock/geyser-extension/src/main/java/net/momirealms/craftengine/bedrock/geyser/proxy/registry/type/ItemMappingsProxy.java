package net.momirealms.craftengine.bedrock.geyser.proxy.registry.type;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;

@ReflectionProxy(name = "org.geysermc.geyser.registry.type.ItemMappings")
public interface ItemMappingsProxy {
    ItemMappingsProxy INSTANCE = ASMProxyFactory.create(ItemMappingsProxy.class);

    @MethodInvoker(name = "getLightBlocks")
    Object lightBlocks(Object target);

    @MethodInvoker(name = "getItemDefinitions")
    Object itemDefinitions(Object target);

    @MethodInvoker(name = "getCustomIdMappings")
    Object customIdMappings(Object target);

    @MethodInvoker(name = "getNonVanillaCustomItemIds")
    Object nonVanillaCustomItemIds(Object target);

    @MethodInvoker(name = "getCustomBlockItemDefinitions")
    Object customBlockItemDefinitions(Object target);

    default ItemMappings copy(ItemMappings source, ItemMapping[] items) {
        // 仅复制当前连接的映射数组；协议注册表继续共享，fastutil 类型交给 proxy 适配重定位。
        ItemMappings.ItemMappingsBuilder builder = ItemMappings.builder().items(items)
                .lodestoneCompass(source.getLodestoneCompass()).creativeItemGroups(source.getCreativeItemGroups())
                .creativeItems(source.getCreativeItems()).storedItems(source.getStoredItems())
                .javaOnlyItems(source.getJavaOnlyItems()).buckets(source.getBuckets()).boats(source.getBoats())
                .zeroBlockDefinitionRuntimeId(source.getZeroBlockDefinitionRuntimeId());
        Builder.INSTANCE.lightBlocks(builder, this.lightBlocks(source));
        Builder.INSTANCE.itemDefinitions(builder, this.itemDefinitions(source));
        Builder.INSTANCE.customIdMappings(builder, this.customIdMappings(source));
        Builder.INSTANCE.nonVanillaCustomItemIds(builder, this.nonVanillaCustomItemIds(source));
        Builder.INSTANCE.customBlockItemDefinitions(builder, this.customBlockItemDefinitions(source));
        return builder.build();
    }

    @ReflectionProxy(name = "org.geysermc.geyser.registry.type.ItemMappings$ItemMappingsBuilder")
    interface Builder {
        Builder INSTANCE = ASMProxyFactory.create(Builder.class);

        @FieldSetter(name = "lightBlocks")
        void lightBlocks(Object target, Object value);

        @FieldSetter(name = "itemDefinitions")
        void itemDefinitions(Object target, Object value);

        @FieldSetter(name = "customIdMappings")
        void customIdMappings(Object target, Object value);

        @FieldSetter(name = "nonVanillaCustomItemIds")
        void nonVanillaCustomItemIds(Object target, Object value);

        @FieldSetter(name = "customBlockItemDefinitions")
        void customBlockItemDefinitions(Object target, Object value);
    }
}
