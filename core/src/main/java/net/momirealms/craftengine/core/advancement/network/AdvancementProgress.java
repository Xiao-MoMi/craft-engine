package net.momirealms.craftengine.core.advancement.network;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;

public class AdvancementProgress {
    private final Map<String, CriterionProgress> progress;

    public AdvancementProgress(Map<String, CriterionProgress> progress) {
        this.progress = progress;
    }

    public AdvancementProgress() {
        this.progress = new HashMap<>();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeMap(this.progress, FriendlyByteBuf::writeUtf, ((byteBuf, criterionProgress) -> criterionProgress.write(buf)));
    }

    public static AdvancementProgress read(FriendlyByteBuf buf) {
        Map<String, CriterionProgress> progress = buf.readMap(FriendlyByteBuf::readUtf, CriterionProgress::read);
        return new AdvancementProgress(progress);
    }
}
