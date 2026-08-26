package net.momirealms.craftengine.core.plugin.ui.state;

import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;

final class MutablePlayerKeyedSignalImpl<T> extends KeyedSignalImpl<UUID, T> implements MutablePlayerKeyedSignal<T> {

    MutablePlayerKeyedSignalImpl(Function<? super UUID, ? extends T> initial, BiPredicate<? super T, ? super T> sameValue) {
        super(initial, sameValue);
        // 通用基类已经完成构造且本类没有额外状态, 注册表可以在这里发布 this.
        PlayerSignalRegistry.track(this);
    }
}

