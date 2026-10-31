package net.momirealms.craftengine.bedrock;

import java.nio.file.Path;
import java.util.List;

public final class BedrockPackFiles {
    public static final String DATA_EXTENSION = ".cedata";
    public static final List<String> SERVER_FILES = List.of(
            RegistrationPlanKind.ITEM.file(),
            RegistrationPlanKind.BLOCK.file(),
            BedrockPackLayout.TRANSLATIONS,
            BedrockPackLayout.GLYPHS
    );

    private BedrockPackFiles() {
    }

    public static Path data(Path pack, Path directory) {
        String name = pack.getFileName().toString();
        int extension = name.lastIndexOf('.');
        return directory.resolve((extension < 0 ? name : name.substring(0, extension)) + DATA_EXTENSION);
    }
}
