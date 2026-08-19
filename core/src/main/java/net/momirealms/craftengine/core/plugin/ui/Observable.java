package net.momirealms.craftengine.core.plugin.ui;

public interface Observable<T> {

    /**
     * 为此来源订阅观察者, 每次调用都会创建独立的订阅关系, 即使同一观察者被重复提供也是如此.
     * <p><strong>订阅由 Observable 保活</strong>, 订阅一直持续到显式关闭或来源本身被回收.
     *
     * @param observer 要通知的观察者
     * @return 订阅凭证, 用于显式退订.
     */
    Subscription subscribe(Observer<? super T> observer);
}
