package net.momirealms.craftengine.core.plugin.ui.state;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BiPredicate;
import java.util.function.Function;

final class PlayerKeyedSignalImpl<T> extends AsyncKeyedSignalImpl<UUID, T> implements PlayerKeyedSignal<T> {

    PlayerKeyedSignalImpl(T placeholder, Executor executor, Function<? super UUID, ? extends T> loader, BiPredicate<? super T, ? super T> sameValue, @Nullable AsyncSignalImpl.Polling polling) {
        super(placeholder, executor, loader, sameValue, polling);
        // 通用基类已经完成构造且本类没有额外状态, 注册表可以在这里发布 this.
        PlayerSignalRegistry.track(this);
    }
}

