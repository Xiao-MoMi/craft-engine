package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import org.jetbrains.annotations.Nullable;

import java.util.List;

record BedrockTextureRule(String source, List<Target> targets) {

    record Target(String path, List<Tile> tiles, @Nullable String firstFrame) {
    }

    record Tile(String name, int index, int replicate) {
    }

    record Tga(String path, boolean keep) {
    }
}
