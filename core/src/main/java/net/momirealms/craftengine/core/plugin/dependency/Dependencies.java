package net.momirealms.craftengine.core.plugin.dependency;

import net.momirealms.craftengine.core.plugin.dependency.relocation.Relocation;

import java.util.List;

public final class Dependencies {
    private Dependencies() {}

    public static final Dependency ASM = Dependency.of("asm", "org.ow2.asm", "asm")
            .build();

    public static final Dependency ASM_COMMONS = Dependency.of("asm-commons", "org.ow2.asm", "asm-commons")
            .versionKey("asm")
            .build();

    public static final Dependency ASM_UTIL = Dependency.of("asm-util", "org.ow2.asm", "asm-util")
            .versionKey("asm")
            .build();

    public static final Dependency JAR_RELOCATOR = Dependency.of("jar-relocator", "me.lucko", "jar-relocator")
            .build();

    public static final Dependency CRAFT_ENGINE_BUKKIT_PROXY = Dependency.of("craft-engine-bukkit-proxy", "net.momirealms", "craft-engine-bukkit-proxy")
            .visibility(DependencyVisibility.PUBLIC)
            .jarInJarPath("proxy.jarinjar")
            .build();

    public static final Dependency GEANTY_REF = Dependency.of("geantyref", "io{}leangen{}geantyref", "geantyref")
            .relocations(
                    Relocation.of("geantyref", "io{}leangen{}geantyref")
            )
            .build();

    public static final Dependency CLOUD_CORE = Dependency.of("cloud-core", "org{}incendo", "cloud-core")
            .relocations(
                    Relocation.of("cloud", "org{}incendo{}cloud"),
                    Relocation.of("geantyref", "io{}leangen{}geantyref")
            )
            .build();

    public static final Dependency CLOUD_SERVICES = Dependency.of("cloud-services", "org{}incendo", "cloud-services")
            .versionKey("cloud-core")
            .relocations(
                    Relocation.of("cloud", "org{}incendo{}cloud"),
                    Relocation.of("geantyref", "io{}leangen{}geantyref")
            )
            .build();

    public static final Dependency CLOUD_BRIGADIER = Dependency.of("cloud-brigadier", "org{}incendo", "cloud-brigadier")
            .versionKey("cloud-platform")
            .relocations(
                    Relocation.of("cloud", "org{}incendo{}cloud"),
                    Relocation.of("geantyref", "io{}leangen{}geantyref")
            )
            .build();

    public static final Dependency CLOUD_BUKKIT = Dependency.of("cloud-bukkit", "org{}incendo", "cloud-bukkit")
            .versionKey("cloud-platform")
            .relocations(
                    Relocation.of("cloud", "org{}incendo{}cloud"),
                    Relocation.of("geantyref", "io{}leangen{}geantyref"),
                    Relocation.of("adventure", "net{}kyori{}adventure")
            )
            .build();

    public static final Dependency CLOUD_PAPER = Dependency.of("cloud-paper", "org{}incendo", "cloud-paper")
            .versionKey("cloud-platform")
            .relocations(
                    Relocation.of("cloud", "org{}incendo{}cloud"),
                    Relocation.of("geantyref", "io{}leangen{}geantyref"),
                    Relocation.of("adventure", "net{}kyori{}adventure"),
                    Relocation.of("option", "net{}kyori{}option")
            )
            .build();

    public static final Dependency CLOUD_MINECRAFT_EXTRAS = Dependency.of("cloud-minecraft-extras", "org{}incendo", "cloud-minecraft-extras")
            .versionKey("cloud-platform")
            .relocations(
                    Relocation.of("cloud", "org{}incendo{}cloud"),
                    Relocation.of("geantyref", "io{}leangen{}geantyref"),
                    Relocation.of("adventure", "net{}kyori{}adventure"),
                    Relocation.of("option", "net{}kyori{}option")
            )
            .build();

    public static final Dependency BOOSTED_YAML = Dependency.of("boosted-yaml", "dev{}dejvokep", "boosted-yaml")
            .relocations(
                    Relocation.of("boostedyaml", "dev{}dejvokep{}boostedyaml")
            )
            .build();

    public static final Dependency BSTATS_BASE = Dependency.of("bstats-base", "org{}bstats", "bstats-base")
            .relocations(
                    Relocation.of("bstats", "org{}bstats")
            )
            .build();

    public static final Dependency BSTATS_BUKKIT = Dependency.of("bstats-bukkit", "org{}bstats", "bstats-bukkit")
            .versionKey("bstats-base")
            .relocations(
                    Relocation.of("bstats", "org{}bstats")
            )
            .build();

    public static final Dependency GSON = Dependency.of("gson", "com.google.code.gson", "gson")
            .build();

    public static final Dependency CAFFEINE = Dependency.of("caffeine", "com{}github{}ben-manes{}caffeine", "caffeine")
            .relocations(
                    Relocation.of("caffeine", "com{}github{}benmanes{}caffeine")
            )
            .build();

    public static final Dependency ZSTD = Dependency.of("zstd-jni", "com.github.luben", "zstd-jni")
            .build();

    public static final Dependency COMMONS_LANG3 = Dependency.of("commons-lang3", "org{}apache{}commons", "commons-lang3")
            .relocations(
                    Relocation.of("commons", "org{}apache{}commons")
            )
            .build();

    public static final Dependency COMMONS_IO = Dependency.of("commons-io", "commons-io", "commons-io")
            .relocations(
                    Relocation.of("commons", "org{}apache{}commons")
            )
            .build();

    public static final Dependency BYTE_BUDDY = Dependency.of("byte-buddy", "net{}bytebuddy", "byte-buddy")
            .relocations(
                    Relocation.of("bytebuddy", "net{}bytebuddy")
            )
            .build();

    public static final Dependency BYTE_BUDDY_AGENT = Dependency.of("byte-buddy-agent", "net{}bytebuddy", "byte-buddy-agent")
            .versionKey("byte-buddy")
            .relocations(
                    Relocation.of("bytebuddy", "net{}bytebuddy")
            )
            .build();

    public static final Dependency SNAKE_YAML = Dependency.of("snakeyaml-engine", "org{}snakeyaml", "snakeyaml-engine")
            .relocations(
                    Relocation.of("snakeyaml", "org{}snakeyaml")
            )
            .build();

    public static final Dependency OPTION = Dependency.of("option", "net{}kyori", "option")
            .relocations(
                    Relocation.of("option", "net{}kyori{}option"),
                    Relocation.of("adventure", "net{}kyori{}adventure")
            )
            .visibility(DependencyVisibility.PUBLIC)
            .build();

    public static final Dependency ADVENTURE_API = Dependency.of("adventure-api", "net{}kyori", "adventure-api")
            .relocations(
                    Relocation.of("option", "net{}kyori{}option"),
                    Relocation.of("adventure", "net{}kyori{}adventure")
            )
            .visibility(DependencyVisibility.PUBLIC)
            .build();

    public static final Dependency ADVENTURE_NBT = Dependency.of("adventure-nbt", "net{}kyori", "adventure-nbt")
            .versionKey("adventure-api")
            .relocations(
                    Relocation.of("option", "net{}kyori{}option"),
                    Relocation.of("adventure", "net{}kyori{}adventure")
            )
            .visibility(DependencyVisibility.PUBLIC)
            .build();

    public static final Dependency ADVENTURE_KEY = Dependency.of("adventure-key", "net{}kyori", "adventure-key")
            .versionKey("adventure-api")
            .relocations(
                    Relocation.of("option", "net{}kyori{}option"),
                    Relocation.of("adventure", "net{}kyori{}adventure")
            )
            .visibility(DependencyVisibility.PUBLIC)
            .build();

    public static final Dependency TEXT_SERIALIZER_COMMONS = Dependency.of("adventure-text-serializer-commons", "net{}kyori", "adventure-text-serializer-commons")
            .versionKey("adventure-api")
            .relocations(
                    Relocation.of("option", "net{}kyori{}option"),
                    Relocation.of("adventure", "net{}kyori{}adventure")
            )
            .visibility(DependencyVisibility.PUBLIC)
            .build();

    public static final Dependency TEXT_SERIALIZER_GSON = Dependency.of("adventure-text-serializer-gson", "net{}kyori", "adventure-text-serializer-gson")
            .versionKey("adventure-api")
            .relocations(
                    Relocation.of("option", "net{}kyori{}option"),
                    Relocation.of("adventure", "net{}kyori{}adventure")
            )
            .visibility(DependencyVisibility.PUBLIC)
            .build();

    public static final Dependency TEXT_SERIALIZER_GSON_LEGACY = Dependency.of("adventure-text-serializer-json-legacy-impl", "net{}kyori", "adventure-text-serializer-json-legacy-impl")
            .versionKey("adventure-api")
            .relocations(
                    Relocation.of("option", "net{}kyori{}option"),
                    Relocation.of("adventure", "net{}kyori{}adventure")
            )
            .visibility(DependencyVisibility.PUBLIC)
            .build();

    public static final Dependency TEXT_SERIALIZER_LEGACY = Dependency.of("adventure-text-serializer-legacy", "net{}kyori", "adventure-text-serializer-legacy")
            .versionKey("adventure-api")
            .relocations(
                    Relocation.of("option", "net{}kyori{}option"),
                    Relocation.of("adventure", "net{}kyori{}adventure")
            )
            .visibility(DependencyVisibility.PUBLIC)
            .build();

    public static final Dependency TEXT_SERIALIZER_JSON = Dependency.of("adventure-text-serializer-json", "net{}kyori", "adventure-text-serializer-json")
            .versionKey("adventure-api")
            .relocations(
                    Relocation.of("option", "net{}kyori{}option"),
                    Relocation.of("adventure", "net{}kyori{}adventure")
            )
            .visibility(DependencyVisibility.PUBLIC)
            .build();

    public static final Dependency AHO_CORASICK = Dependency.of("ahocorasick", "org{}ahocorasick", "ahocorasick")
            .relocations(
                    Relocation.of("ahocorasick", "org{}ahocorasick")
            )
            .build();

    public static final Dependency LZ4 = Dependency.of("lz4", "at{}yawk{}lz4", "lz4-java")
            .relocations(
                    Relocation.of("jpountz", "net{}jpountz")
            )
            .build();

    public static final Dependency GRAALJS_POLYGLOT = Dependency.of("graaljs-polyglot", "org.graalvm.polyglot", "polyglot")
            .versionKey("graaljs")
            .build();
    public static final Dependency GRAALJS_JS_LANGUAGE = Dependency.of("graaljs-js-language", "org.graalvm.js", "js-language")
            .versionKey("graaljs")
            .build();
    public static final Dependency GRAALJS_TRUFFLE_RUNTIME = Dependency.of("graaljs-truffle-runtime", "org.graalvm.truffle", "truffle-runtime")
            .versionKey("graaljs")
            .build();
    // Community Edition
    public static final Dependency GRAALJS_TRUFFLE_COMPILER = Dependency.of("graaljs-truffle-compiler", "org.graalvm.truffle", "truffle-compiler")
            .versionKey("graaljs")
            .build();
    public static final Dependency GRAALJS_TRUFFLE_API = Dependency.of("graaljs-truffle-api", "org.graalvm.truffle", "truffle-api")
            .versionKey("graaljs")
            .build();
    public static final Dependency GRAALJS_REGEX = Dependency.of("graaljs-regex", "org.graalvm.regex", "regex")
            .versionKey("graaljs")
            .build();
    public static final Dependency GRAALJS_COLLECTIONS = Dependency.of("graaljs-collections", "org.graalvm.sdk", "collections")
            .versionKey("graaljs")
            .build();
    public static final Dependency GRAALJS_NATIVEIMAGE = Dependency.of("graaljs-nativeimage", "org.graalvm.sdk", "nativeimage")
            .versionKey("graaljs")
            .build();
    public static final Dependency GRAALJS_ICU4J = Dependency.of("graaljs-icu4j", "org.graalvm.shadowed", "icu4j")
            .versionKey("graaljs")
            .build();
    public static final Dependency NASHORN_CORE = Dependency.of("nashorn", "org.openjdk.nashorn", "nashorn-core")
            .build();

    public static final Dependency JIMFS = Dependency.of("jimfs", "com{}google{}jimfs", "jimfs")
            .relocations(
                    Relocation.of("jimfs", "com{}google{}common{}jimfs")
            )
            .build();

    public static final Dependency BUCKET_4_J = Dependency.of("bucket4j", "com{}bucket4j", "bucket4j_jdk17-core")
            .relocations(
                    Relocation.of("bucket4j", "io{}github{}bucket4j")
            )
            .build();

    public static final Dependency NETTY_HTTP = Dependency.of("netty-codec-http", "io{}netty", "netty-codec-http")
            .relocations(
                    Relocation.of("netty{}handler{}codec{}http", "io{}netty{}handler{}codec{}http"),
                    Relocation.of("netty{}handler{}codec{}rtsp", "io{}netty{}handler{}codec{}rtsp"),
                    Relocation.of("netty{}handler{}codec{}spdy", "io{}netty{}handler{}codec{}spdy")
            )
            .build();

    public static final Dependency NETTY_HTTP2 = Dependency.of("netty-codec-http2", "io{}netty", "netty-codec-http2")
            .relocations(
                    Relocation.of("netty{}handler{}codec{}http2", "io{}netty{}handler{}codec{}http2")
            )
            .build();

    public static final Dependency REACTIVE_STREAMS = Dependency.of("reactive-streams", "org{}reactivestreams", "reactive-streams")
            .relocations(
                    Relocation.of("reactivestreams", "org{}reactivestreams"),
                    Relocation.of("netty{}handler{}codec{}http2", "io{}netty{}handler{}codec{}http2"),
                    Relocation.of("netty{}handler{}codec{}http", "io{}netty{}handler{}codec{}http"),
                    Relocation.of("netty{}handler{}codec{}rtsp", "io{}netty{}handler{}codec{}rtsp"),
                    Relocation.of("netty{}handler{}codec{}spdy", "io{}netty{}handler{}codec{}spdy")
            )
            .build();

    private static final List<Relocation> AWS_RELOCATIONS = List.of(
            Relocation.of("awssdk", "software{}amazon{}awssdk"),
            Relocation.of("reactivestreams", "org{}reactivestreams"),
            Relocation.of("netty{}handler{}codec{}http2", "io{}netty{}handler{}codec{}http2"),
            Relocation.of("netty{}handler{}codec{}http", "io{}netty{}handler{}codec{}http"),
            Relocation.of("netty{}handler{}codec{}rtsp", "io{}netty{}handler{}codec{}rtsp"),
            Relocation.of("netty{}handler{}codec{}spdy", "io{}netty{}handler{}codec{}spdy")
    );

    public static final Dependency AMAZON_AWSSDK_S3 = Dependency.of("amazon-sdk-s3", "software{}amazon{}awssdk", "s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_NETTY_NIO_CLIENT = Dependency.of("amazon-sdk-netty-nio-client", "software{}amazon{}awssdk", "netty-nio-client")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_SDK_CORE = Dependency.of("amazon-sdk-core", "software{}amazon{}awssdk", "sdk-core")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_AUTH = Dependency.of("amazon-sdk-auth", "software{}amazon{}awssdk", "auth")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_REGIONS = Dependency.of("amazon-sdk-regions", "software{}amazon{}awssdk", "regions")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_IDENTITY_SPI = Dependency.of("amazon-sdk-identity-spi", "software{}amazon{}awssdk", "identity-spi")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_HTTP_CLIENT_SPI = Dependency.of("amazon-sdk-http-client-spi", "software{}amazon{}awssdk", "http-client-spi")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_PROTOCOL_CORE = Dependency.of("amazon-sdk-protocol-core", "software{}amazon{}awssdk", "protocol-core")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_AWS_XML_PROTOCOL = Dependency.of("amazon-sdk-aws-xml-protocol", "software{}amazon{}awssdk", "aws-xml-protocol")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_JSON_UTILS = Dependency.of("amazon-sdk-json-utils", "software{}amazon{}awssdk", "json-utils")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_AWS_CORE = Dependency.of("amazon-sdk-aws-core", "software{}amazon{}awssdk", "aws-core")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_UTILS = Dependency.of("amazon-sdk-utils", "software{}amazon{}awssdk", "utils")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_ANNOTATIONS = Dependency.of("amazon-sdk-annotations", "software{}amazon{}awssdk", "annotations")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_CRT_CORE = Dependency.of("amazon-sdk-crt-core", "software{}amazon{}awssdk", "crt-core")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_CHECKSUMS = Dependency.of("amazon-sdk-checksums", "software{}amazon{}awssdk", "checksums")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_CHECKSUMS_SPI = Dependency.of("amazon-sdk-checksums-spi", "software{}amazon{}awssdk", "checksums-spi")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_RETRIES = Dependency.of("amazon-sdk-retries", "software{}amazon{}awssdk", "retries")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_RETRIES_SPI = Dependency.of("amazon-sdk-retries-spi", "software{}amazon{}awssdk", "retries-spi")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_ENDPOINTS_SPI = Dependency.of("amazon-sdk-endpoints-spi", "software{}amazon{}awssdk", "endpoints-spi")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_ARNS = Dependency.of("amazon-sdk-arns", "software{}amazon{}awssdk", "arns")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_AWS_QUERY_PROTOCOL = Dependency.of("amazon-sdk-aws-query-protocol", "software{}amazon{}awssdk", "aws-query-protocol")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_HTTP_AUTH = Dependency.of("amazon-sdk-http-auth", "software{}amazon{}awssdk", "http-auth")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_HTTP_AUTH_SPI = Dependency.of("amazon-sdk-http-auth-spi", "software{}amazon{}awssdk", "http-auth-spi")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_HTTP_AUTH_AWS = Dependency.of("amazon-sdk-http-auth-aws", "software{}amazon{}awssdk", "http-auth-aws")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_HTTP_AUTH_AWS_EVENTSTREAM = Dependency.of("amazon-sdk-http-auth-aws-eventstream", "software{}amazon{}awssdk", "http-auth-aws-eventstream")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_METRICS_SPI = Dependency.of("amazon-sdk-metrics-spi", "software{}amazon{}awssdk", "metrics-spi")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_THIRD_PARTY_JACKSON_CORE = Dependency.of("amazon-sdk-third-party-jackson-core", "software{}amazon{}awssdk", "third-party-jackson-core")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_AWSSDK_PROFILES = Dependency.of("amazon-sdk-profiles", "software{}amazon{}awssdk", "profiles")
            .versionKey("amazon-sdk-s3")
            .relocations(AWS_RELOCATIONS)
            .build();

    public static final Dependency AMAZON_EVENTSTREAM = Dependency.of("amazon-sdk-eventstream", "software{}amazon{}eventstream", "eventstream")
            .relocations(
                    Relocation.of("eventstream", "software{}amazon{}eventstream"),
                    Relocation.of("reactivestreams", "org{}reactivestreams"),
                    Relocation.of("netty{}handler{}codec{}http2", "io{}netty{}handler{}codec{}http2"),
                    Relocation.of("netty{}handler{}codec{}http", "io{}netty{}handler{}codec{}http"),
                    Relocation.of("netty{}handler{}codec{}rtsp", "io{}netty{}handler{}codec{}rtsp"),
                    Relocation.of("netty{}handler{}codec{}spdy", "io{}netty{}handler{}codec{}spdy")
            )
            .build();
}
