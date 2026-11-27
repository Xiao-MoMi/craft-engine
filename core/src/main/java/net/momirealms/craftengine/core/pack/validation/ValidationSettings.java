package net.momirealms.craftengine.core.pack.validation;

import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.MinecraftVersion;

public record ValidationSettings(MinecraftVersion packMinVersion,
                                 MinecraftVersion packMaxVersion,
                                 String overlayFormat,
                                 boolean fixTextureAtlas,
                                 boolean fixMissingTexture,
                                 boolean fixMissingParticleTexture,
                                 boolean fixModelUvOutOfBounds,
                                 boolean fixModelDisplayOutOfBounds,
                                 boolean fixTexturesFormat,
                                 boolean fixRotationAngle) {

    public static ValidationSettings capture() {
        return new ValidationSettings(
                Config.packMinVersion(),
                Config.packMaxVersion(),
                Config.createOverlayFolderName("{version}"),
                Config.fixTextureAtlas(),
                Config.fixMissingTexture(),
                Config.fixMissingParticleTexture(),
                Config.fixModelUvOutOfBounds(),
                Config.fixModelDisplayOutOfBounds(),
                Config.fixTexturesFormat(),
                Config.fixRotationAngle()
        );
    }

    public String createOverlayFolderName(String version) {
        return this.overlayFormat.replace("{version}", version);
    }
}