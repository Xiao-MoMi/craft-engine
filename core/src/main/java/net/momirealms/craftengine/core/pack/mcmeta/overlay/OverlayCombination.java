package net.momirealms.craftengine.core.pack.mcmeta.overlay;

import net.momirealms.craftengine.core.pack.mcmeta.Overlay;
import net.momirealms.craftengine.core.pack.mcmeta.PackVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class OverlayCombination {
    private final List<VersionBasedEvent> versionBasedEvents;
    private final List<Overlay> currentOverlays;
    private int cursor;
    private long version;

    public OverlayCombination(List<Overlay> overlays, int minVersion, int maxVersion) {
        this(overlays, new PackVersion(minVersion), new PackVersion(maxVersion, Integer.MAX_VALUE));
    }

    public OverlayCombination(List<Overlay> overlays, PackVersion minVersion, PackVersion maxVersion) {
        this(overlays, minVersion, maxVersion, List.of());
    }

    public OverlayCombination(List<Overlay> overlays, PackVersion minVersion, PackVersion maxVersion, List<PackVersion> boundaries) {
        this.versionBasedEvents = new ArrayList<>();
        this.currentOverlays = new ArrayList<>();
        this.version = encode(minVersion);
        this.cursor = 0;

        long min = encode(minVersion);
        long max = encode(maxVersion);
        Map<Long, List<Event>> eventsByVersion = new TreeMap<>();
        eventsByVersion.computeIfAbsent(min, k -> new ArrayList<>());
        eventsByVersion.computeIfAbsent(max + 1, k -> new ArrayList<>());
        // 额外边界只切分版本区间，不改变当前生效的 overlay。
        for (PackVersion boundary : boundaries) {
            long point = encode(boundary);
            if (point > min && point <= max) {
                eventsByVersion.computeIfAbsent(point, k -> new ArrayList<>());
            }
        }
        for (Overlay overlay : overlays) {
            if (overlay.minVersion().isAbove(maxVersion)) {
                continue;
            }
            if (overlay.maxVersion().isBelow(minVersion)) {
                continue;
            }

            // 取最小中的较大值
            long join = Math.max(encode(overlay.minVersion()), min);
            // 去最大中的较小值
            long leave = Math.min(encode(overlay.maxVersion()), max);
            List<Event> joinEvents = eventsByVersion.computeIfAbsent(join, k -> new ArrayList<>());
            joinEvents.add(new Event(overlay, Operation.JOIN));
            List<Event> leaveEvents = eventsByVersion.computeIfAbsent(leave + 1, k -> new ArrayList<>());
            leaveEvents.add(new Event(overlay, Operation.LEAVE));
        }

        for (Map.Entry<Long, List<Event>> entry : eventsByVersion.entrySet()) {
            this.versionBasedEvents.add(new VersionBasedEvent(entry.getKey(), entry.getValue()));
        }
    }

    public boolean hasNext() {
        return this.cursor < this.versionBasedEvents.size();
    }

    @Nullable
    public OverlayCombination.Segment nextSegment() {
        Segment next = next();
        if (next == null) {
            return null;
        }
        // 第一次100%有问题
        if (next.minVersion.isAbove(next.maxVersion)) {
            return next();
        }
        return next;
    }

    @Nullable
    private OverlayCombination.Segment next() {
        // 已经没有事件里
        if (this.cursor >= this.versionBasedEvents.size()) {
            return null;
        }
        // 获取事件
        VersionBasedEvent events = this.versionBasedEvents.get(this.cursor++);
        // 将上一个版本和上次记录的版本打为一个overlay返回
        Segment segment = new Segment(decode(this.version), decode(events.version - 1), Set.copyOf(this.currentOverlays));
        this.version = events.version;
        // 变更当前成员
        for (Event event : events.events) {
            if (event.operation() == Operation.LEAVE) {
                this.currentOverlays.remove(event.overlay);
            } else if (event.operation() == Operation.JOIN) {
                this.currentOverlays.add(event.overlay);
            }
        }

        return segment;
    }

    // Pack formats are ordered pairs of nonnegative ints. Keep minor boundaries during the sweep.
    private static long encode(PackVersion version) {
        return ((long) version.major() << 31) + version.minor();
    }

    private static PackVersion decode(long version) {
        return new PackVersion((int) (version >> 31), (int) (version & Integer.MAX_VALUE));
    }

    public record Segment(PackVersion minVersion, PackVersion maxVersion, Set<Overlay> overlays) {
        public Segment(int min, int max, Set<Overlay> overlays) {
            this(new PackVersion(min), new PackVersion(max), overlays);
        }

        public int min() {
            return this.minVersion.major();
        }

        public int max() {
            return this.maxVersion.major();
        }

        @Override
        public @NotNull String toString() {
            return "OverlaySegment{" +
                    "min=" + this.minVersion.asString() +
                    ", max=" + this.maxVersion.asString() +
                    ", overlays=" + this.overlays +
                    '}';
        }
    }

    record Event(Overlay overlay, Operation operation) {

        @Override
        public @NotNull String toString() {
            return "Event{" +
                    "overlay=" + this.overlay +
                    ", operation=" + this.operation +
                    '}';
        }
    }

    record VersionBasedEvent(long version, List<Event> events) {

        @Override
        public @NotNull String toString() {
            return "VersionBasedEvent{" +
                    "version=" + this.version +
                    ", events=" + this.events +
                    '}';
        }
    }

    enum Operation {
        JOIN, LEAVE
    }
}
