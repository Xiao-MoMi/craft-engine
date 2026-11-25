package net.momirealms.craftengine.bedrock;

import java.nio.file.Path;
import java.util.List;

public final class BedrockPackFiles {
    public static final String DATA_EXTENSION = ".cedata";
    public static final String CLIENT_PACK = "client.mcpack";
    public static final List<String> SERVER_FILES = List.of(
            RegistrationPlanKind.ITEM.file(),
            RegistrationPlanKind.BLOCK.file(),
            BedrockPackLayout.MODEL_DISPLAYS,
            BedrockPackLayout.ELYTRA_VARIANTS,
            BedrockPackLayout.NATIVE_MODELS,
            BedrockPackLayout.TRANSLATIONS,
            BedrockPackLayout.SCOREBOARD,
            BedrockPackLayout.TITLE,
            BedrockPackLayout.BOSS_BARS,
            BedrockPackLayout.FONTS
    );

    private BedrockPackFiles() {
    }

    public static Path data(Path pack, Path directory) {
        return directory.resolve(baseName(pack) + DATA_EXTENSION);
    }

    private static String baseName(Path file) {
        String name = file.getFileName().toString();
        int extension = name.lastIndexOf('.');
        return extension < 0 ? name : name.substring(0, extension);
    }
}
