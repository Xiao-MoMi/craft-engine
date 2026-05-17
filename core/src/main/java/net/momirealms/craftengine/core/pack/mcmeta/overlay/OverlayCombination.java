package net.momirealms.craftengine.core.pack.mcmeta.overlay;

import net.momirealms.craftengine.core.pack.mcmeta.Overlay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class OverlayCombination {
    private final List<VersionBasedEvent> versionBasedEvents;
    private final List<Overlay> currentOverlays;
    private int cursor;
    private int version;

    public OverlayCombination(List<Overlay> overlays, int minVersion, int maxVersion) {
        this.versionBasedEvents = new ArrayList<>();
        this.currentOverlays = new ArrayList<>();
        this.version = minVersion;
        this.cursor = 0;

        Map<Integer, List<Event>> eventsByVersion = new TreeMap<>();
        eventsByVersion.computeIfAbsent(minVersion, k -> new ArrayList<>());
        eventsByVersion.computeIfAbsent(maxVersion + 1, k -> new ArrayList<>());
        for (Overlay overlay : overlays) {
            if (overlay.minVersion().major() > maxVersion) {
                continue;
            }
            if (overlay.maxVersion().major() < minVersion) {
                continue;
            }

            // 取最小中的较大值
            int join = Math.max(overlay.minVersion().major(), minVersion);
            // 去最大中的较小值
            int leave = Math.min(overlay.maxVersion().major(), maxVersion);
            List<Event> joinEvents = eventsByVersion.computeIfAbsent(join, k -> new ArrayList<>());
            joinEvents.add(new Event(overlay, Operation.JOIN));
            List<Event> leaveEvents = eventsByVersion.computeIfAbsent(leave + 1, k -> new ArrayList<>());
            leaveEvents.add(new Event(overlay, Operation.LEAVE));
        }

        for (Map.Entry<Integer, List<Event>> entry : eventsByVersion.entrySet()) {
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
        if (next.min > next.max) {
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
        Segment segment = new Segment(this.version, events.version - 1, Set.copyOf(this.currentOverlays));
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

    public record Segment(int min, int max, Set<Overlay> overlays) {

        @Override
        public @NotNull String toString() {
            return "OverlaySegment{" +
                    "min=" + this.min +
                    ", max=" + this.max +
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

    record VersionBasedEvent(int version, List<Event> events) {

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
