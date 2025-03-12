package net.momirealms.craftengine.core.plugin.dependency;

import net.momirealms.craftengine.core.plugin.dependency.relocation.Relocation;

import java.util.Collections;
import java.util.List;

public class Dependencies {

    public static final Dependency ASM = new Dependency(
            "asm",
            "org.ow2.asm",
            "asm",
            "asm",
            Collections.emptyList()
    );
    public static final Dependency ASM_COMMONS = new Dependency(
            "asm-commons",
            "org.ow2.asm",
            "asm-commons",
            "asm-commons",
            Collections.emptyList()
    );
    public static final Dependency JAR_RELOCATOR = new Dependency(
            "jar-relocator",
            "me.lucko",
            "jar-relocator",
            "jar-relocator",
            Collections.emptyList()
    );
    public static final Dependency NETTY_HTTP = new Dependency(
            "netty-codec-http",
            "io.netty",
            "netty-codec-http",
            "netty-codec-http",
            Collections.emptyList()
    );
    public static final Dependency GEANTY_REF = new Dependency(
            "geantyref",
            "io{}leangen{}geantyref",
            "geantyref",
            "geantyref",
            List.of(Relocation.of("geantyref", "io{}leangen{}geantyref"))
    );
    public static final Dependency CLOUD_CORE = new Dependency(
            "cloud-core",
            "org{}incendo",
            "cloud-core",
            "cloud-core",
            List.of(Relocation.of("cloud", "org{}incendo{}cloud"),
                    Relocation.of("geantyref", "io{}leangen{}geantyref"))
    );
    public static final Dependency CLOUD_BRIGADIER = new Dependency(
            "cloud-brigadier",
            "org{}incendo",
            "cloud-brigadier",
            "cloud-brigadier",
            List.of(Relocation.of("cloud", "org{}incendo{}cloud"),
                    Relocation.of("geantyref", "io{}leangen{}geantyref"))
    );
    public static final Dependency CLOUD_SERVICES = new Dependency(
            "cloud-services",
            "org{}incendo",
            "cloud-services",
            "cloud-services",
            List.of(Relocation.of("cloud", "org{}incendo{}cloud"),
                    Relocation.of("geantyref", "io{}leangen{}geantyref"))
    );
    public static final Dependency CLOUD_BUKKIT = new Dependency(
            "cloud-bukkit",
            "org{}incendo",
            "cloud-bukkit",
            "cloud-bukkit",
            List.of(Relocation.of("cloud", "org{}incendo{}cloud"),
                    Relocation.of("geantyref", "io{}leangen{}geantyref"),
                    Relocation.of("adventure", "net{}kyori{}adventure"),
                    Relocation.of("examination", "net{}kyori{}examination"),
                    Relocation.of("option", "net{}kyori{}option"))
    );
    public static final Dependency CLOUD_PAPER = new Dependency(
            "cloud-paper",
            "org{}incendo",
            "cloud-paper",
            "cloud-paper",
            List.of(Relocation.of("cloud", "org{}incendo{}cloud"),
                    Relocation.of("geantyref", "io{}leangen{}geantyref"),
                    Relocation.of("adventure", "net{}kyori{}adventure"),
                    Relocation.of("examination", "net{}kyori{}examination"),
                    Relocation.of("option", "net{}kyori{}option"))
    );
    public static final Dependency CLOUD_MINECRAFT_EXTRAS = new Dependency(
            "cloud-minecraft-extras",
            "org{}incendo",
            "cloud-minecraft-extras",
            "cloud-minecraft-extras",
            List.of(Relocation.of("cloud", "org{}incendo{}cloud"),
                    Relocation.of("geantyref", "io{}leangen{}geantyref"),
                    Relocation.of("adventure", "net{}kyori{}adventure"),
                    Relocation.of("examination", "net{}kyori{}examination"),
                    Relocation.of("option", "net{}kyori{}option"))
    );
    public static final Dependency BOOSTED_YAML = new Dependency(
            "boosted-yaml",
            "dev{}dejvokep",
            "boosted-yaml",
            "boosted-yaml",
            List.of(Relocation.of("boostedyaml", "dev{}dejvokep{}boostedyaml"))
    );
    public static final Dependency BSTATS_BASE = new Dependency(
            "bstats-base",
            "org{}bstats",
            "bstats-base",
            "bstats-base",
            List.of(Relocation.of("bstats", "org{}bstats"))
    );
    public static final Dependency BSTATS_BUKKIT = new Dependency(
            "bstats-bukkit",
            "org{}bstats",
            "bstats-bukkit",
            "bstats-bukkit",
            List.of(Relocation.of("bstats", "org{}bstats"))
    ) {
          @Override
          public String getVersion() {
              return Dependencies.BSTATS_BASE.getVersion();
          }
    };
    public static final Dependency GSON = new Dependency(
            "gson",
            "com.google.code.gson",
            "gson",
            "gson",
            Collections.emptyList()
    );
    public static final Dependency CAFFEINE = new Dependency(
            "caffeine",
            "com{}github{}ben-manes{}caffeine",
            "caffeine",
            "caffeine",
            List.of(Relocation.of("caffeine", "com{}github{}benmanes{}caffeine"))
    );
    public static final Dependency ZSTD = new Dependency(
            "zstd-jni",
            "com.github.luben",
            "zstd-jni",
            "zstd-jni",
            Collections.emptyList()
    );
    public static final Dependency SLF4J_API = new Dependency(
            "slf4j-api",
            "org.slf4j",
            "slf4j-api",
            "slf4j-api",
            Collections.emptyList()
    );
    public static final Dependency SLF4J_SIMPLE = new Dependency(
            "slf4j-simple",
            "org.slf4j",
            "slf4j-simple",
            "slf4j-simple",
            Collections.emptyList()
    ) {
        @Override
        public String getVersion() {
            return Dependencies.SLF4J_API.getVersion();
        }
    };
    public static final Dependency COMMONS_IO = new Dependency(
            "commons-io",
            "commons-io",
            "commons-io",
            "commons-io",
            List.of(Relocation.of("commons", "org{}apache{}commons"))
    );
    public static final Dependency BYTE_BUDDY = new Dependency(
            "byte-buddy",
            "net{}bytebuddy",
            "byte-buddy",
            "byte-buddy",
            List.of(Relocation.of("bytebuddy", "net{}bytebuddy"))
    );
    public static final Dependency SNAKE_YAML = new Dependency(
            "snake-yaml",
            "org{}yaml",
            "snakeyaml",
            "snakeyaml",
            List.of(Relocation.of("snakeyaml", "org{}yaml{}snakeyaml"))
    );
    public static final Dependency MINIMESSAGE = new Dependency(
            "adventure-text-minimessage",
            "net{}kyori",
            "adventure-text-minimessage",
            "adventure-text-minimessage",
            List.of(Relocation.of("adventure", "net{}kyori{}adventure"))
    );
    public static final Dependency TEXT_SERIALIZER_GSON = new Dependency(
            "adventure-text-serializer-gson",
            "net{}kyori",
            "adventure-text-serializer-gson",
            "adventure-text-serializer-gson",
            List.of(Relocation.of("adventure", "net{}kyori{}adventure"))
    );
    public static final Dependency TEXT_SERIALIZER_JSON = new Dependency(
            "adventure-text-serializer-json",
            "net{}kyori",
            "adventure-text-serializer-json",
            "adventure-text-serializer-json",
            List.of(Relocation.of("adventure", "net{}kyori{}adventure"))
    );
}