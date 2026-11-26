package net.momirealms.craftengine.bedrock.geyser.title;

import io.netty.util.concurrent.EventExecutor;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketEvent;
import net.momirealms.craftengine.bedrock.text.TitleFrame;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetTitlePacket;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class TitleSession {
    private static final long TICK = TimeUnit.MILLISECONDS.toNanos(50);
    private static final Timings DEFAULT = new Timings(10, 70, 20);
    private static final Timings DISPLAY = new Timings(0, Integer.MAX_VALUE / 50, 0);
    private static final Timings HIDDEN = new Timings(0, 0, 0);
    private final EventExecutor loop;
    private final LongSupplier clock;
    private Timings timings = DEFAULT;
    private TitleFrame.Content title;
    private TitleFrame.Content subtitle = TitleFrame.EMPTY;
    private int titleCapacity;
    private int subtitleCapacity;
    private int lastAlpha = -1;
    private TitleFrame frame;
    private long started;
    private long deadline;
    private ScheduledFuture<?> timer;
    private long wakeAt;
    private Consumer<BedrockPacket> sender;

    public TitleSession(EventExecutor loop) {
        this(loop, System::nanoTime);
    }

    TitleSession(EventExecutor loop, LongSupplier clock) {
        this.loop = loop;
        this.clock = clock;
    }

    public void onTitle(GeyserPacketEvent<SetTitlePacket> event) {
        SetTitlePacket.Type type = event.packet().getType();
        if (type == SetTitlePacket.Type.ACTIONBAR || type == SetTitlePacket.Type.ACTIONBAR_JSON) {
            return;
        }
        event.setCancelled(this.accept(event.packet(), event::writeBefore, event::deferredWriter));
    }

    boolean accept(SetTitlePacket packet, Consumer<BedrockPacket> write, Supplier<Consumer<BedrockPacket>> deferred) {
        long now = this.clock.getAsLong();
        if (this.title != null && now - this.deadline >= 0) {
            this.finish(write);
        }
        switch (packet.getType()) {
            case TIMES -> {
                this.timings = this.timings.update(packet);
                if (this.title != null) {
                    this.deadline = now + this.timings.duration();
                    if (this.sender != null) {
                        this.update(now, false, write);
                        return true;
                    }
                }
            }
            case TITLE, SUBTITLE -> {
                return this.onText(packet, now, write, deferred);
            }
            case CLEAR, RESET -> {
                if (packet.getType() == SetTitlePacket.Type.RESET) {
                    this.timings = DEFAULT;
                }
                boolean managed = this.sender != null;
                this.finish(write, packet.getType());
                return managed;
            }
            case TITLE_JSON, SUBTITLE_JSON -> {
                this.finish(write);
            }
            default -> {
            }
        }
        return false;
    }

    private boolean onText(SetTitlePacket packet, long now, Consumer<BedrockPacket> write, Supplier<Consumer<BedrockPacket>> deferred) {
        boolean changed = this.updateText(packet, now);
        if (this.title == null) {
            return this.subtitle.layered();
        }
        if (this.sender != null) {
            this.update(now, changed, write);
            return true;
        }
        if (!this.title.layered() && !this.subtitle.layered()) {
            return false;
        }

        this.sender = deferred.get();
        write.accept(DISPLAY.packet());
        // Prime the frame before opening the display. A zero-duration title can finish during the update.
        this.update(now, true, write);
        if (this.sender != null) {
            write.accept(packet(SetTitlePacket.Type.TITLE, TitleFrame.DISPLAY));
        }
        return true;
    }

    private boolean updateText(SetTitlePacket packet, long now) {
        String text = packet.getText();
        if (packet.getType() == SetTitlePacket.Type.SUBTITLE) {
            if (text.equals(this.subtitle.source())) {
                return false;
            }
            this.subtitle = TitleFrame.content(text);
            return true;
        }

        this.deadline = now + this.timings.duration();
        if (this.title == null) {
            this.started = now;
        } else if (text.equals(this.title.source())) {
            return false;
        }
        this.title = TitleFrame.content(text);
        return true;
    }

    private void update(long now, boolean changed, Consumer<BedrockPacket> write) {
        long remaining = this.deadline - now;
        if (remaining <= 0) {
            this.finish(write);
            return;
        }
        long elapsed = now - this.started;
        long fadeIn = this.timings.fadeIn * TICK;
        long fadeOut = this.timings.fadeOut * TICK;
        int alpha = 255;
        if (elapsed < fadeIn) {
            alpha = (int) (elapsed * 255.0 / fadeIn);
        }
        if (remaining < fadeOut) {
            alpha = Math.min(alpha, (int) (remaining * 255.0 / fadeOut));
        }
        if (changed) {
            if (this.frame == null) {
                this.frame = new TitleFrame();
            }
            this.titleCapacity = Math.max(this.titleCapacity, this.title.size());
            this.subtitleCapacity = Math.max(this.subtitleCapacity, this.subtitle.size());
            this.frame.setContent(this.titleCapacity, this.subtitleCapacity, this.title, this.subtitle);
        }
        if (changed || this.lastAlpha != alpha) {
            write.accept(packet(SetTitlePacket.Type.SUBTITLE, this.frame.encode(alpha)));
            this.lastAlpha = alpha;
        }
        // Stay asleep while fully opaque. Renewals can reuse an earlier wakeup.
        long delay = elapsed < fadeIn || remaining <= fadeOut ? Math.min(TICK, remaining) : remaining - fadeOut;
        long next = now + delay;
        if (this.timer == null || next - this.wakeAt < 0) {
            if (this.timer != null) {
                this.timer.cancel(false);
            }
            this.wakeAt = next;
            this.timer = this.loop.schedule(this::onTimer, delay, TimeUnit.NANOSECONDS);
        }
    }

    private void onTimer() {
        this.timer = null;
        this.update(this.clock.getAsLong(), false, this.sender);
    }

    private void finish(Consumer<BedrockPacket> write) {
        this.finish(write, SetTitlePacket.Type.CLEAR);
    }

    private void finish(Consumer<BedrockPacket> write, SetTitlePacket.Type clear) {
        if (this.sender != null) {
            if (this.frame != null) {
                write.accept(packet(SetTitlePacket.Type.SUBTITLE, this.frame.encode(0)));
            }
            write.accept(packet(clear, ""));
            // CLEAR can leave the HUD bound to its last visible frame. Publish blank text after it, with no native animation.
            write.accept(HIDDEN.packet());
            write.accept(packet(SetTitlePacket.Type.SUBTITLE, " "));
            write.accept(packet(SetTitlePacket.Type.TITLE, " "));
            write.accept(this.timings.packet());
        }
        this.close();
    }

    public void onDimensionChange(GeyserPacketEvent<?> event) {
        this.finish(event::writeBefore);
    }

    public void close() {
        if (this.timer != null) {
            this.timer.cancel(false);
            this.timer = null;
        }
        this.sender = null;
        this.title = null;
        this.subtitle = TitleFrame.EMPTY;
        this.titleCapacity = 0;
        this.subtitleCapacity = 0;
        this.lastAlpha = -1;
        this.frame = null;
    }

    static SetTitlePacket packet(SetTitlePacket.Type type, String text) {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(type);
        packet.setText(text);
        packet.setXuid("");
        packet.setPlatformOnlineId("");
        return packet;
    }

    private record Timings(int fadeIn, int stay, int fadeOut) {
        private Timings update(SetTitlePacket packet) {
            int in = packet.getFadeInTime() < 0 ? this.fadeIn : packet.getFadeInTime();
            int hold = packet.getStayTime() < 0 ? this.stay : packet.getStayTime();
            int out = packet.getFadeOutTime() < 0 ? this.fadeOut : packet.getFadeOutTime();
            return this.fadeIn == in && this.stay == hold && this.fadeOut == out ? this : new Timings(in, hold, out);
        }

        private long duration() {
            return ((long) this.fadeIn + this.stay + this.fadeOut) * TICK;
        }

        private SetTitlePacket packet() {
            SetTitlePacket packet = TitleSession.packet(SetTitlePacket.Type.TIMES, "");
            packet.setFadeInTime(this.fadeIn);
            packet.setStayTime(this.stay);
            packet.setFadeOutTime(this.fadeOut);
            return packet;
        }
    }
}
