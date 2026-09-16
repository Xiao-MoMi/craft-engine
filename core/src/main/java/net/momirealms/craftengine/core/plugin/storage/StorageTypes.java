package net.momirealms.craftengine.core.plugin.storage;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public final class StorageTypes {
    public static final StorageType<FileStorage> JSON = register(Key.ce("json"), FileStorage.FACTORY);
    public static final StorageType<SqlStorage> SQLITE = register(Key.ce("sqlite"), new SqlStorageFactory(SqlDialect.SQLITE));
    public static final StorageType<SqlStorage> H2 = register(Key.ce("h2"), new SqlStorageFactory(SqlDialect.H2));
    public static final StorageType<SqlStorage> MYSQL = register(Key.ce("mysql"), new SqlStorageFactory(SqlDialect.MYSQL));
    public static final StorageType<SqlStorage> MARIADB = register(Key.ce("mariadb"), new SqlStorageFactory(SqlDialect.MARIADB));
    public static final StorageType<SqlStorage> POSTGRESQL = register(Key.ce("postgresql"), new SqlStorageFactory(SqlDialect.POSTGRESQL));
    public static final StorageType<MongoStorage> MONGODB = register(Key.ce("mongodb"), MongoStorageFactory.INSTANCE);

    private StorageTypes() {}

    public static <T extends Storage> StorageType<T> register(Key key, StorageFactory<T> factory) {
        StorageType<T> type = new StorageType<>(key, factory);
        ((WritableRegistry<StorageType<? extends Storage>>) BuiltInRegistries.STORAGE_TYPE).register(ResourceKey.create(Registries.STORAGE_TYPE.location(), key), type);
        return type;
    }

    public static Storage fromConfig(ConfigSection section) throws Exception {
        Key key = Key.ce(section.getString("type", "sqlite"));
        StorageType<? extends Storage> type = BuiltInRegistries.STORAGE_TYPE.getValue(key);
        if (type == null) {
            throw new KnownResourceException("storage.unknown_type", section.assemblePath("type"), key.asString());
        }
        return type.factory().create(section);
    }
}
