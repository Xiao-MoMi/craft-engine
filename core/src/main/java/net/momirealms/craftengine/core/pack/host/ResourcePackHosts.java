package net.momirealms.craftengine.core.pack.host;

import net.momirealms.craftengine.core.pack.host.impl.*;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public final class ResourcePackHosts {
    public static final ResourcePackHostType<NoneHost> NONE = register(Key.ce("none"), NoneHost.FACTORY);
    public static final ResourcePackHostType<SelfHost> SELF = register(Key.ce("self"), SelfHost.FACTORY);
    public static final ResourcePackHostType<ExternalHost> EXTERNAL = register(Key.ce("external"), ExternalHost.FACTORY);
    public static final ResourcePackHostType<LobFileHost> LOBFILE = register(Key.ce("lobfile"), LobFileHost.FACTORY);
    public static final ResourcePackHostType<S3Host> S3 = register(Key.ce("s3"), S3HostFactory.INSTANCE);
    public static final ResourcePackHostType<AlistHost> ALIST = register(Key.ce("alist"), AlistHost.FACTORY);
    public static final ResourcePackHostType<DropboxHost> DROPBOX = register(Key.ce("dropbox"), DropboxHost.FACTORY);
    public static final ResourcePackHostType<OneDriveHost> ONEDRIVE = register(Key.ce("onedrive"), OneDriveHost.FACTORY);
    public static final ResourcePackHostType<GitLabHost> GITLAB = register(Key.ce("gitlab"), GitLabHost.FACTORY);

    private ResourcePackHosts() {}

    public static <T extends ResourcePackHost> ResourcePackHostType<T> register(Key key, ResourcePackHostFactory<T> factory) {
        ResourcePackHostType<T> type = new ResourcePackHostType<>(key, factory);
        ((WritableRegistry<ResourcePackHostType<? extends ResourcePackHost>>) BuiltInRegistries.RESOURCE_PACK_HOST_TYPE)
                .register(ResourceKey.create(Registries.RESOURCE_PACK_HOST_TYPE.location(), key), type);
        return type;
    }

    public static ResourcePackHost fromConfig(ConfigSection section) {
        Key type = section.getNonNullIdentifier("type");
        ResourcePackHostType<? extends ResourcePackHost> hostType = BuiltInRegistries.RESOURCE_PACK_HOST_TYPE.getValue(type);
        if (hostType == null) {
            throw new LocalizedException("warning.config.host.invalid_type", type.asString());
        }
        return hostType.factory().create(section);
    }
}
