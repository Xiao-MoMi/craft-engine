package net.momirealms.craftengine.core.pack.bedrock.font;

import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.ClientPngReader;
import net.momirealms.craftengine.core.util.Key;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class JavaFontSources {
    final JavaResourcePack pack;
    final Map<Key, List<JavaResourcePack.FontResource>> fonts;
    private final Map<String, JavaUnihexFont> unihexFonts = new HashMap<>();
    final Map<Key, BufferedImage> images = new HashMap<>();

    JavaFontSources(JavaResourcePack pack) throws IOException {
        this.pack = pack;
        this.fonts = pack.fonts();
    }

    JavaUnihexFont unihex(String path) throws IOException {
        JavaUnihexFont source = this.unihexFonts.get(path);
        if (source == null) {
            try (InputStream input = this.pack.openResource(path)) {
                if (input == null) {
                    throw new IOException("Missing unihex resource " + path);
                }
                source = new JavaUnihexFont(input);
            }
            this.unihexFonts.put(path, source);
        }
        return source;
    }

    BufferedImage image(Key texture, String path) throws IOException {
        BufferedImage image = this.images.get(texture);
        if (image == null) {
            try (InputStream input = this.pack.openResource(path)) {
                if (input != null) {
                    image = ClientPngReader.read(input);
                }
            } catch (IOException exception) {
                throw new IOException(path + ": " + exception.getMessage(), exception);
            }
            if (image == null) {
                throw new IOException(TranslationManager.message("resource_pack.bedrock.font.bitmap", path));
            }
            this.images.put(texture, image);
        }
        return image;
    }
}
