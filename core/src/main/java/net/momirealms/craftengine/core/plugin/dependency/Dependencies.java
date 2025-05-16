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

    public static final Dependency TEXT_SERIALIZER_GSON_LEGACY = new Dependency(
            "adventure-text-serializer-json-legacy-impl",
            "net{}kyori",
            "adventure-text-serializer-json-legacy-impl",
            "adventure-text-serializer-json-legacy-impl",
            List.of(Relocation.of("adventure", "net{}kyori{}adventure"))
    );

    public static final Dependency TEXT_SERIALIZER_JSON = new Dependency(
            "adventure-text-serializer-json",
            "net{}kyori",
            "adventure-text-serializer-json",
            "adventure-text-serializer-json",
            List.of(Relocation.of("adventure", "net{}kyori{}adventure"))
    );

    public static final Dependency AHO_CORASICK = new Dependency(
            "ahocorasick",
            "org{}ahocorasick",
            "ahocorasick",
            "aho-corasick",
            List.of(Relocation.of("ahocorasick", "org{}ahocorasick"))
    );

    public static final Dependency LZ4 = new Dependency(
            "lz4",
            "org{}lz4",
            "lz4-java",
            "lz4-java",
            List.of(Relocation.of("jpountz", "net{}jpountz"))
    );

    public static final Dependency EVALEX = new Dependency(
            "evalex",
            "com{}ezylang",
            "EvalEx",
            "evalex",
            List.of(Relocation.of("evalex", "com{}ezylang{}evalex"))
    );

    public static final Dependency NETTY_HTTP = new Dependency(
            "netty-codec-http",
            "io{}netty",
            "netty-codec-http",
            "netty-codec-http",
            Collections.emptyList()
    );

    public static final Dependency NETTY_HTTP2 = new Dependency(
            "netty-codec-http2",
            "io{}netty",
            "netty-codec-http2",
            "netty-codec-http2",
            Collections.emptyList()
    );

    public static final Dependency REACTIVE_STREAMS = new Dependency(
            "reactive-streams",
            "org{}reactivestreams",
            "reactive-streams",
            "reactive-streams",
            List.of(Relocation.of("reactivestreams", "org{}reactivestreams"))
    );

    public static final Dependency JIMFS = new Dependency(
            "jimfs",
            "com{}google{}jimfs",
            "jimfs",
            "jimfs",
            List.of(Relocation.of("jimfs", "com{}google{}common{}jimfs"))
    );

    public static final Dependency COMMONS_IMAGING = new Dependency(
            "commons-imaging",
            "org{}apache{}commons",
            "commons-imaging",
            "commons-imaging",
            List.of(Relocation.of("imaging", "org{}apache{}commons{}imaging"))
    );

    public static final Dependency AMAZON_AWSSDK_S3 = new Dependency(
            "amazon-sdk-s3",
            "software{}amazon{}awssdk",
            "s3",
            "amazon-s3",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    );

    public static final Dependency AMAZON_AWSSDK_NETTY_NIO_CLIENT = new Dependency(
            "amazon-sdk-netty-nio-client",
            "software{}amazon{}awssdk",
            "netty-nio-client",
            "amazon-netty-nio-client",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_SDK_CORE = new Dependency(
            "amazon-sdk-core",
            "software{}amazon{}awssdk",
            "sdk-core",
            "amazon-sdk-core",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_AUTH = new Dependency(
            "amazon-sdk-auth",
            "software{}amazon{}awssdk",
            "auth",
            "amazon-auth",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_REGIONS = new Dependency(
            "amazon-sdk-regions",
            "software{}amazon{}awssdk",
            "regions",
            "amazon-regions",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_IDENTITY_SPI = new Dependency(
            "amazon-sdk-identity-spi",
            "software{}amazon{}awssdk",
            "identity-spi",
            "amazon-identity-spi",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_HTTP_CLIENT_SPI = new Dependency(
            "amazon-sdk-http-client-spi",
            "software{}amazon{}awssdk",
            "http-client-spi",
            "amazon-http-client-spi",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_PROTOCOL_CORE = new Dependency(
            "amazon-sdk-protocol-core",
            "software{}amazon{}awssdk",
            "protocol-core",
            "amazon-protocol-core",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_AWS_XML_PROTOCOL = new Dependency(
            "amazon-sdk-aws-xml-protocol",
            "software{}amazon{}awssdk",
            "aws-xml-protocol",
            "amazon-aws-xml-protocol",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_JSON_UTILS = new Dependency(
            "amazon-sdk-json-utils",
            "software{}amazon{}awssdk",
            "json-utils",
            "amazon-json-utils",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_AWS_CORE = new Dependency(
            "amazon-sdk-aws-core",
            "software{}amazon{}awssdk",
            "aws-core",
            "amazon-aws-core",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_UTILS = new Dependency(
            "amazon-sdk-utils",
            "software{}amazon{}awssdk",
            "utils",
            "amazon-utils",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_ANNOTATIONS = new Dependency(
            "amazon-sdk-annotations",
            "software{}amazon{}awssdk",
            "annotations",
            "amazon-annotations",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_CRT_CORE = new Dependency(
            "amazon-sdk-crt-core",
            "software{}amazon{}awssdk",
            "crt-core",
            "amazon-crt-core",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_CHECKSUMS = new Dependency(
            "amazon-sdk-checksums",
            "software{}amazon{}awssdk",
            "checksums",
            "amazon-checksums",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_EVENTSTREAM = new Dependency(
            "amazon-sdk-eventstream",
            "software{}amazon{}eventstream",
            "eventstream",
            "amazon-eventstream",
            List.of(
                    Relocation.of("eventstream", "software{}amazon{}eventstream"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    );

    public static final Dependency AMAZON_AWSSDK_PROFILES = new Dependency(
            "amazon-sdk-profiles",
            "software{}amazon{}awssdk",
            "profiles",
            "amazon-profiles",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_RETRIES = new Dependency(
            "amazon-sdk-retries",
            "software{}amazon{}awssdk",
            "retries",
            "amazon-retries",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_ENDPOINTS_SPI = new Dependency(
            "amazon-sdk-endpoints-spi",
            "software{}amazon{}awssdk",
            "endpoints-spi",
            "amazon-endpoints-spi",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_ARNS = new Dependency(
            "amazon-sdk-arns",
            "software{}amazon{}awssdk",
            "arns",
            "amazon-arns",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_AWS_QUERY_PROTOCOL = new Dependency(
            "amazon-sdk-aws-query-protocol",
            "software{}amazon{}awssdk",
            "aws-query-protocol",
            "amazon-aws-query-protocol",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_HTTP_AUTH_AWS = new Dependency(
            "amazon-sdk-http-auth-aws",
            "software{}amazon{}awssdk",
            "http-auth-aws",
            "amazon-http-auth-aws",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_HTTP_AUTH_SPI = new Dependency(
            "amazon-sdk-http-auth-spi",
            "software{}amazon{}awssdk",
            "http-auth-spi",
            "amazon-http-auth-spi",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_HTTP_AUTH = new Dependency(
            "amazon-sdk-http-auth",
            "software{}amazon{}awssdk",
            "http-auth",
            "amazon-http-auth",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_HTTP_AUTH_AWS_EVENTSTREAM = new Dependency(
            "amazon-sdk-http-auth-aws-eventstream",
            "software{}amazon{}awssdk",
            "http-auth-aws-eventstream",
            "amazon-http-auth-aws-eventstream",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_CHECKSUMS_SPI = new Dependency(
            "amazon-sdk-checksums-spi",
            "software{}amazon{}awssdk",
            "checksums-spi",
            "amazon-checksums-spi",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_RETRIES_SPI = new Dependency(
            "amazon-sdk-retries-spi",
            "software{}amazon{}awssdk",
            "retries-spi",
            "amazon-retries-spi",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_METRICS_SPI = new Dependency(
            "amazon-sdk-metrics-spi",
            "software{}amazon{}awssdk",
            "metrics-spi",
            "amazon-metrics-spi",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };

    public static final Dependency AMAZON_AWSSDK_THIRD_PARTY_JACKSON_CORE = new Dependency(
            "amazon-sdk-third-party-jackson-core",
            "software{}amazon{}awssdk",
            "third-party-jackson-core",
            "amazon-third-party-jackson-core",
            List.of(
                    Relocation.of("awssdk", "software{}amazon{}awssdk"),
                    Relocation.of("reactivestreams", "org{}reactivestreams")
            )
    ) {
        @Override
        public String getVersion() {
            return AMAZON_AWSSDK_S3.getVersion();
        }
    };
}