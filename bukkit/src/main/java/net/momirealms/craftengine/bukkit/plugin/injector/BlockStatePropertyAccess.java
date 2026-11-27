package net.momirealms.craftengine.bukkit.plugin.injector;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Scheduler;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.momirealms.craftengine.core.block.DelegatingBlockState;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties.PropertyProxy;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class BlockStatePropertyAccess {
    private static final LoadingCache<PropertyPair, Boolean> COMPATIBILITY = Caffeine.newBuilder()
            .scheduler(Scheduler.systemScheduler())
            .expireAfterAccess(Duration.ofMinutes(10))
            .build(PropertyPair::isCompatible);

    private BlockStatePropertyAccess() {
    }

    public static boolean hasProperty(@This DelegatingBlockState holder, @Argument(0) Object property) {
        return compatibleProperty(holder.blockState(), property) != null;
    }

    @RuntimeType
    public static Object getValue(@This DelegatingBlockState holder, @Argument(0) Object property) {
        ImmutableBlockState state = holder.blockState();
        Property<?> custom = compatibleProperty(state, property);
        if (custom == null) {
            return null;
        }
        Object value = state.get(custom);
        Class<?> nativeType = PropertyProxy.INSTANCE.getValueClass(property);
        return custom.valueClass() == nativeType ? value : convertEnum(nativeType, value);
    }

    @RuntimeType
    public static Object setValue(@This DelegatingBlockState holder, @Argument(0) Object property, @Argument(1) Object value) {
        ImmutableBlockState state = holder.blockState();
        Property<?> custom = compatibleProperty(state, property);
        if (custom == null) {
            return holder;
        }
        Object converted = custom.valueClass() == PropertyProxy.INSTANCE.getValueClass(property) ? value : convertEnum(custom.valueClass(), value);
        if (converted == null) {
            return holder;
        }
        try {
            return ImmutableBlockState.with(state, custom, converted).customBlockState().minecraftState();
        } catch (IllegalArgumentException e) {
            return holder;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convertEnum(Class<?> target, Object value) {
        return Enum.valueOf((Class<? extends Enum>) target, ((Enum<?>) value).name());
    }

    private static Property<?> compatibleProperty(ImmutableBlockState state, Object property) {
        if (state == null) {
            return null;
        }
        Property<?> custom = state.owner().value().getProperty(PropertyProxy.INSTANCE.getName(property));
        return custom != null && COMPATIBILITY.get(new PropertyPair(custom, property)) ? custom : null;
    }

    private record PropertyPair(Property<?> custom, Object nativeProperty) {
        private boolean isCompatible() {
            Class<?> nativeType = PropertyProxy.INSTANCE.getValueClass(this.nativeProperty);
            Class<?> customType = this.custom.valueClass();
            if (nativeType != customType && !(nativeType.isEnum() && customType.isEnum())) {
                return false;
            }
            Collection<?> nativeValues = PropertyProxy.INSTANCE.getPossibleValues(this.nativeProperty);
            List<?> customValues = this.custom.possibleValues();
            if (nativeValues.size() != customValues.size()) {
                return false;
            }
            // Before 1.21.2 the native collection has no guaranteed order.
            if (!VersionHelper.isOrAbove1_21_2) {
                Set<String> nativeNames = nativeValues.stream().map(String::valueOf).collect(Collectors.toSet());
                Set<String> customNames = customValues.stream().map(String::valueOf).collect(Collectors.toSet());
                return nativeNames.equals(customNames);
            }
            if (nativeType == customType) {
                return nativeValues.equals(customValues);
            }
            List<?> orderedValues = (List<?>) nativeValues;
            for (int i = 0; i < customValues.size(); i++) {
                if (!orderedValues.get(i).toString().equals(customValues.get(i).toString())) {
                    return false;
                }
            }
            return true;
        }
    }
}
