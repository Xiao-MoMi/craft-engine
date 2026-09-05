package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

final class MergingSignal<T> extends AbstractSignal<Long> {
    private final AbstractSignal<? extends Collection<? extends T>> sources;
    private final Function<? super T, ? extends Signal<?>> signalOf;
    private final Object mergeLock = new Object();

    @Nullable private volatile Aligned aligned;
    private volatile long version;
    private long notifiedVersion;
    @Nullable private Subscription sourcesUpstream;
    private Subscription @Nullable [] memberUpstream;

    MergingSignal(@NotNull AbstractSignal<? extends Collection<? extends T>> sources, @NotNull Function<? super T, ? extends Signal<?>> signalOf) {
        this.sources = sources;
        this.signalOf = signalOf;
    }

    @Override
    @NotNull
    public Long get() {
        return this.version();
    }

    @Override
    long version() {
        Aligned current = this.aligned;
        if (current != null
                && current.sourcesVersion() == this.sources.version()
                && current.memberVersionSum() == versionSumOf(current.members())) {
            return this.version;
        }

        Subscription[] previous;
        synchronized (this.mergeLock) {
            previous = this.alignLocked();
        }
        closeAll(previous);
        return this.version;
    }

    // 将当前成员与集合内容对齐, 返回需要在锁外关闭的上一批转发凭证.
    private Subscription @Nullable [] alignLocked() {
        long sourcesVersion = this.sources.version();
        Aligned current = this.aligned;
        AbstractSignal<?>[] members = current != null && current.sourcesVersion() == sourcesVersion
                ? current.members()
                : this.currentMembers();

        if (current != null && Arrays.equals(current.members(), members)) {
            long sum = versionSumOf(members);
            if (sum != current.memberVersionSum()) {
                this.version++;
            }
            this.aligned = new Aligned(members, sourcesVersion, sum);
            return null;
        }

        Subscription[] previous = this.memberUpstream;
        Subscription[] attached = null;
        if (previous != null) {
            attached = this.linkAll(members, this::onUpstreamDirty);
        }
        long sum;
        try {
            sum = versionSumOf(members);
        } catch (RuntimeException | Error exception) {
            closeAll(attached);
            throw exception;
        }
        if (attached != null) {
            this.memberUpstream = attached;
        }
        this.version++;
        this.aligned = new Aligned(members, sourcesVersion, sum);
        return previous;
    }

    private AbstractSignal<?>[] currentMembers() {
        Collection<? extends T> elements = this.sources.get();
        AbstractSignal<?>[] members = new AbstractSignal<?>[elements.size()];
        int index = 0;
        for (T element : elements) {
            members[index++] = require(this.signalOf.apply(element));
        }
        return members;
    }

    private void onUpstreamDirty() {
        Subscription[] previous;
        boolean shouldNotify = false;
        synchronized (this.mergeLock) {
            previous = this.alignLocked();
            if (this.version > this.notifiedVersion) {
                this.notifiedVersion = this.version;
                shouldNotify = true;
            }
        }
        closeAll(previous);
        if (shouldNotify) {
            this.notifyDirty();
        }
    }

    @Override
    protected void onActive() {
        Subscription[] discarded = null;
        synchronized (this.mergeLock) {
            this.sourcesUpstream = this.linkTo(this.sources, this::onUpstreamDirty);
            try {
                this.alignLocked();
                Aligned current = this.aligned;
                assert current != null;
                this.memberUpstream = this.linkAll(current.members(), this::onUpstreamDirty);
                // 再次对齐, 收进建立转发期间发生的成员变化.
                discarded = this.alignLocked();
            } catch (RuntimeException | Error exception) {
                closeAll(this.memberUpstream);
                this.memberUpstream = null;
                this.sourcesUpstream.close();
                this.sourcesUpstream = null;
                throw exception;
            }
            this.notifiedVersion = this.version;
        }
        closeAll(discarded);
    }

    @Override
    protected void onInactive() {
        Subscription previousSources;
        Subscription[] previousMembers;
        synchronized (this.mergeLock) {
            previousSources = this.sourcesUpstream;
            previousMembers = this.memberUpstream;
            this.sourcesUpstream = null;
            this.memberUpstream = null;
        }
        previousSources.close();
        closeAll(previousMembers);
    }

    private static long versionSumOf(AbstractSignal<?>[] members) {
        long sum = 0L;
        for (int index = 0; index < members.length; index++) {
            sum += members[index].version();
        }
        return sum;
    }

    private record Aligned(AbstractSignal<?>[] members, long sourcesVersion, long memberVersionSum) {
    }
}
