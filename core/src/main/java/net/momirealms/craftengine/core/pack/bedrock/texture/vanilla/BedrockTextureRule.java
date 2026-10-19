package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import java.util.List;

public record BedrockTextureRule(String source, List<Target> targets) {

    public record Target(String path, List<Tile> tiles) {
    }

    public record Tile(String name, int index, int replicate) {
    }

    public record Tga(String path, boolean keep) {
    }
}
