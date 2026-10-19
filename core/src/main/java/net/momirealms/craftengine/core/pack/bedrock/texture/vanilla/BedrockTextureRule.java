package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record BedrockTextureRule(String source, List<Target> targets) {

    public record Target(String path, List<Tile> tiles, @Nullable String firstFrame) {
    }

    public record Tile(String name, int index, int replicate) {
    }

    public record Tga(String path, boolean keep) {
    }
}
