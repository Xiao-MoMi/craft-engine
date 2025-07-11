package net.momirealms.craftengine.core.advancement.network;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public class CriterionProgress {
    @Nullable
    private Instant obtainedTimestamp;

    public CriterionProgress() {
    }

    public CriterionProgress(@Nullable Instant obtainedTimestamp) {
        this.obtainedTimestamp = obtainedTimestamp;
    }

    public boolean isDone() {
        return this.obtainedTimestamp != null;
    }

    public void grant() {
        this.obtainedTimestamp = Instant.now();
    }

    public void revoke() {
        this.obtainedTimestamp = null;
    }

    public @Nullable Instant obtainedTimestamp() {
        return obtainedTimestamp;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeNullable(this.obtainedTimestamp, FriendlyByteBuf::writeInstant);
    }

    public static CriterionProgress read(FriendlyByteBuf buf) {
        return new CriterionProgress(buf.readNullable(FriendlyByteBuf::readInstant));
    }
}
