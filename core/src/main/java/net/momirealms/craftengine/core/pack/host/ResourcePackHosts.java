package net.momirealms.craftengine.core.pack.host;

import net.momirealms.craftengine.core.pack.host.impl.*;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public class ResourcePackHosts {
    public static final Key NONE = Key.of("craftengine:none");
    public static final Key SELF = Key.of("craftengine:self");
    public static final Key EXTERNAL = Key.of("craftengine:external");
    public static final Key LOBFILE = Key.of("craftengine:lobfile");
    public static final Key S3 = Key.of("craftengine:s3");
    public static final Key ALIST = Key.of("craftengine:alist");
    public static final Key DROPBOX = Key.of("craftengine:dropbox");
    public static final Key ONEDRIVE = Key.of("craftengine:onedrive");
    public static final Key GITLAB = Key.of("craftengine:gitlab");

    static {
        register(NONE, NoneHost.FACTORY);
        register(SELF, SelfHost.FACTORY);
        register(EXTERNAL, ExternalHost.FACTORY);
        register(LOBFILE, LobFileHost.FACTORY);
        register(S3, S3HostFactory.INSTANCE);
        register(ALIST, AlistHost.FACTORY);
        register(DROPBOX, DropboxHost.FACTORY);
        register(ONEDRIVE, OneDriveHost.FACTORY);
        register(GITLAB, GitLabHost.FACTORY);
    }

    public static void register(Key key, ResourcePackHostFactory factory) {
        ((WritableRegistry<ResourcePackHostFactory>) BuiltInRegistries.RESOURCE_PACK_HOST_FACTORY)
                .register(ResourceKey.create(Registries.RESOURCE_PACK_HOST_FACTORY.location(), key), factory);
    }

    public static ResourcePackHost fromMap(Map<String, Object> map) {
        String type = (String) map.get("type");
        if (type == null) {
            throw new LocalizedException("warning.config.host.missing_type");
        }
        Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
        ResourcePackHostFactory factory = BuiltInRegistries.RESOURCE_PACK_HOST_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedException("warning.config.host.invalid_type", type);
        }
        return factory.create(map);
    }
}
