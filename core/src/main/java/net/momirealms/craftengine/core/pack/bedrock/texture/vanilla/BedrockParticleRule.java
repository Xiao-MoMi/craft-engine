package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import org.jetbrains.annotations.Nullable;

import java.util.List;

record BedrockParticleRule(String source,
                          Selection selection,
                          int padding,
                          List<BedrockParticleDefinition> targets,
                          @Nullable String unsupportedReason) {

    static BedrockParticleRule of(String source, Selection selection, BedrockParticleDefinition... targets) {
        return of(source, selection, 0, targets);
    }

    static BedrockParticleRule of(String source, Selection selection, int padding, BedrockParticleDefinition... targets) {
        return new BedrockParticleRule(source, selection, padding, List.of(targets), null);
    }

    static BedrockParticleRule unsupported(String source, Selection selection, String reason) {
        return new BedrockParticleRule(source, selection, 0, List.of(), reason);
    }

    enum Selection {
        RANDOM,
        AGE,
        WAKE,
        QUARTER;

        String frame(int count) {
            if (count == 1) {
                return "0";
            }
            return switch (this) {
                case RANDOM -> "math.min(" + (count - 1) + ", math.floor(variable.particle_random_4 * " + count + "))";
                case QUARTER -> "0";
                // Java SpriteSet.get(age, lifetime) 使用 age * (帧数 - 1) / lifetime，按 tick 取整。
                case AGE -> "math.clamp(math.floor(math.floor(variable.particle_age * 20) * " + (count - 1) + " / math.max(1, math.floor(variable.particle_lifetime * 20))), 0, " + (count - 1) + ")";
                // WakeParticle 使用剩余寿命决定循环位置，不是整段寿命播放一遍。
                case WAKE -> "math.floor(math.mod(math.max(0, 60 - math.floor((variable.particle_lifetime - variable.particle_age) * 20)), 4) * " + (count - 1) + " / 4)";
            };
        }
    }
}
