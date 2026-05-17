package net.momirealms.craftengine.core.pack.host.impl;

import net.momirealms.craftengine.core.pack.host.ResourcePackHostFactory;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.dependency.Dependencies;

import java.util.List;

public final class S3HostFactory implements ResourcePackHostFactory<S3Host> {
    public static final ResourcePackHostFactory<S3Host> INSTANCE = new S3HostFactory();

    @Override
    public S3Host create(ConfigSection section) {
        CraftEngine.instance().dependencyManager().loadDependencies(
                List.of(
                        Dependencies.NETTY_HTTP2,
                        Dependencies.REACTIVE_STREAMS,
                        Dependencies.AMAZON_AWSSDK_S3,
                        Dependencies.AMAZON_AWSSDK_NETTY_NIO_CLIENT,
                        Dependencies.AMAZON_AWSSDK_SDK_CORE,
                        Dependencies.AMAZON_AWSSDK_AUTH,
                        Dependencies.AMAZON_AWSSDK_REGIONS,
                        Dependencies.AMAZON_AWSSDK_IDENTITY_SPI,
                        Dependencies.AMAZON_AWSSDK_HTTP_CLIENT_SPI,
                        Dependencies.AMAZON_AWSSDK_PROTOCOL_CORE,
                        Dependencies.AMAZON_AWSSDK_AWS_XML_PROTOCOL,
                        Dependencies.AMAZON_AWSSDK_JSON_UTILS,
                        Dependencies.AMAZON_AWSSDK_AWS_CORE,
                        Dependencies.AMAZON_AWSSDK_UTILS,
                        Dependencies.AMAZON_AWSSDK_ANNOTATIONS,
                        Dependencies.AMAZON_AWSSDK_CRT_CORE,
                        Dependencies.AMAZON_AWSSDK_CHECKSUMS,
                        Dependencies.AMAZON_EVENTSTREAM,
                        Dependencies.AMAZON_AWSSDK_PROFILES,
                        Dependencies.AMAZON_AWSSDK_RETRIES,
                        Dependencies.AMAZON_AWSSDK_ENDPOINTS_SPI,
                        Dependencies.AMAZON_AWSSDK_ARNS,
                        Dependencies.AMAZON_AWSSDK_AWS_QUERY_PROTOCOL,
                        Dependencies.AMAZON_AWSSDK_HTTP_AUTH_AWS,
                        Dependencies.AMAZON_AWSSDK_HTTP_AUTH_SPI,
                        Dependencies.AMAZON_AWSSDK_HTTP_AUTH,
                        Dependencies.AMAZON_AWSSDK_HTTP_AUTH_AWS_EVENTSTREAM,
                        Dependencies.AMAZON_AWSSDK_CHECKSUMS_SPI,
                        Dependencies.AMAZON_AWSSDK_RETRIES_SPI,
                        Dependencies.AMAZON_AWSSDK_METRICS_SPI,
                        Dependencies.AMAZON_AWSSDK_THIRD_PARTY_JACKSON_CORE
                )
        );
        return S3Host.FACTORY.create(section);
    }
}
