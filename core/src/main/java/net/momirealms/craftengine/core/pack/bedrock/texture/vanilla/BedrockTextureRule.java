package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import org.jetbrains.annotations.Nullable;

import java.util.List;

record BedrockTextureRule(String source, List<Target> targets) {

    record Target(String path, List<Tile> tiles, @Nullable String firstFrame) {

        String animationPath() {
            return this.firstFrame == null ? this.path.replace(".png", "_anim.png") : this.path;
        }

        String firstFramePath() {
            return this.firstFrame == null ? this.path : this.firstFrame;
        }
    }

    record Tile(String name, int index, int replicate) {
    }

    record Tga(String path, boolean keep) {
    }
}
